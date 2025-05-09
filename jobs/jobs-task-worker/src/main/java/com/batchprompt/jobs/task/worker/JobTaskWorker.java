package com.batchprompt.jobs.task.worker;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.files.client.FileClient;
import com.batchprompt.files.model.dto.FileRecordDto;
import com.batchprompt.jobs.core.model.ChatModelResponse;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.JobTaskRepository;
import com.batchprompt.jobs.core.service.ChatModel;
import com.batchprompt.jobs.core.service.JobCreditService;
import com.batchprompt.jobs.core.service.JobPricingService;
import com.batchprompt.jobs.core.service.JobService;
import com.batchprompt.jobs.core.service.ModelService;
import com.batchprompt.jobs.core.service.TokenEstimator;
import com.batchprompt.jobs.model.TaskStatus;
import com.batchprompt.jobs.model.dto.JobTaskMessage;
import com.batchprompt.prompts.client.PromptClient;
import com.batchprompt.prompts.model.dto.PromptDto;
import com.batchprompt.users.client.AccountClient;
import com.batchprompt.users.model.dto.AccountCreditTransactionDto;
import com.batchprompt.users.model.dto.AccountDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobTaskWorker {

    private final JobTaskRepository jobTaskRepository;
    private final JobService jobService;
    private final ModelService modelService;
    private final FileClient fileClient;
    private final PromptClient promptClient;
    private final ObjectMapper objectMapper;
    private final JobPricingService jobPricingService;
    private final JobCreditService jobCreditService;
    private final AccountClient accountClient;

    // This method will be called by the listener configurations created in JobTaskListenerConfig
    public void processJobTask(JobTaskMessage message) {
        UUID jobTaskUuid = message.getJobTaskUuid();
        UUID jobUuid = message.getJobUuid();
        
        log.info("Processing job task: {} for model: {}", jobTaskUuid, message.getModelId());
        
        JobTask jobTask = null;
        
        try {
            // Use a separate transaction to get and update the job task status to Processing
            jobTask = updateTaskToProcessing(jobTaskUuid);
            if (jobTask == null) {
                return; // Already logged in the updateTaskToProcessing method
            }

            // Check if the user has sufficient credits before proceeding
            if (!jobCreditService.checkUserHasSufficientCredits(message.getUserId())) {
                // Mark the task as insufficient credits and update the job status
                updateTaskToInsufficientCredits(jobTaskUuid);
                jobService.updateJobStatus(jobUuid);
                log.warn("Task {} marked as INSUFFICIENT_CREDITS for user: {}", jobTaskUuid, message.getUserId());
                return;
            }
            
            // Step 2: Get the ChatModel for the model id. If job status is Submitted, update it to Processing
            ChatModel chatModel = modelService.getChatModel(message.getModelId());
            if (chatModel == null) {
                throw new Exception("Model not found: " + message.getModelId());
            }
            
            // Update job status if needed
            jobService.updateJobStatus(jobUuid);
            
            // Step 3: Use the ChatModel to generate the response
            // Get prompt text
            // Use user auth token if available, otherwise service-to-service authentication will be used
            PromptDto promptDto = promptClient.getPrompt(message.getPromptUuid(), message.getAuthToken());
            if (promptDto == null) {
                throw new Exception("Prompt not found: " + message.getPromptUuid());
            }
            
            // Get file record
            // Use user auth token if available, otherwise service-to-service authentication will be used
            FileRecordDto recordDto = fileClient.getFileRecord(message.getFileRecordUuid(), message.getAuthToken());
            if (recordDto == null) {
                throw new Exception("File record not found: " + message.getFileRecordUuid());
            }
            
            // Convert record data to JsonNode
            JsonNode recordData = recordDto.getRecord();
            
            // Convert output schema to JsonNode if it exists
            JsonNode outputSchema = null;
            if (promptDto.getResponseJsonSchema() != null && !promptDto.getResponseJsonSchema().isBlank()) {
                outputSchema = objectMapper.readTree(promptDto.getResponseJsonSchema());
            }

            String promptText = replacePlaceholders(promptDto.getPromptText(), recordData);
            
            // Estimate token count before sending to model
            int estimatedPromptTokens = TokenEstimator.estimateTokenCount(promptText, message.getModelId(), modelService);
            updateEstimatedTokens(jobTaskUuid, estimatedPromptTokens);
            
            // Use the new chat model response method to get response with token counts
            ChatModelResponse chatResponse = chatModel.generateChatResponse(
                    promptText,
                    outputSchema,
                    message.getMaxTokens(),
                    message.getTemperature()
            );
            
            // Step 4: If successful, update status to Completed in a separate transaction
            completeTaskWithTokens(jobTask, message.getUserId(), chatResponse);
            
            log.info("Job task {} completed successfully", jobTaskUuid);
        } catch (Exception e) {
            // Step 5: If failed, update status to Failed in a separate transaction
            log.error("Error processing job task: {}", jobTaskUuid, e);
            if (jobTask != null) {
                failTask(jobTaskUuid, e.getMessage());
            }
        } {
            // Step 6: Update job status to Completed or Failed
            jobService.updateJobStatus(jobUuid);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobTask updateTaskToProcessing(UUID jobTaskUuid) {
        JobTask jobTask = jobTaskRepository.findById(jobTaskUuid).orElse(null);
        if (jobTask == null) {
            log.error("Job task not found: {}", jobTaskUuid);
            return null;
        }
        
        // If already completed or failed, don't process again
        if (jobTask.getStatus() == TaskStatus.COMPLETED || jobTask.getStatus() == TaskStatus.FAILED) {
            log.info("Job task {} already in final state: {}", jobTaskUuid, jobTask.getStatus());
            return null;
        }
        
        jobTask.setStatus(TaskStatus.PROCESSING);
        jobTask.setBeginTimestamp(LocalDateTime.now());
        
        return jobTaskRepository.save(jobTask);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEstimatedTokens(UUID jobTaskUuid, int estimatedPromptTokens) {
        JobTask jobTask = jobTaskRepository.findById(jobTaskUuid).orElse(null);
        if (jobTask == null) {
            log.error("Job task not found: {}", jobTaskUuid);
            return;
        }
        
        jobTask.setEstimatedPromptTokens(estimatedPromptTokens);
        jobTaskRepository.save(jobTask);
        
        log.debug("Job task {} estimated prompt tokens: {}", jobTaskUuid, estimatedPromptTokens);
    }
    
    private void completeTaskWithTokens(JobTask jobTask, String userId, ChatModelResponse chatResponse) {
        UUID jobTaskUuid = jobTask.getJobTaskUuid();        
        jobTask.setStatus(TaskStatus.COMPLETED);
        jobTask.setResponseText(chatResponse.getResponseText());
        jobTask.setEndTimestamp(LocalDateTime.now());
        
        // Set token usage information
        jobTask.setPromptTokens(chatResponse.getPromptTokens());
        jobTask.setCompletionTokens(chatResponse.getCompletionTokens());
        jobTask.setTotalTokens(chatResponse.getTotalTokens());
        
        // Calculate cost using the pricing service
        Double calculatedCost = jobPricingService.calculateCost(jobTask);
        if (calculatedCost != null) {
            jobTask.setCalculatedCostUsd(calculatedCost);
            log.info("Job task {} cost calculated: ${}", jobTaskUuid, calculatedCost);
            
            // Credit usage is calculated automatically within the job pricing service
            if (jobTask.getCreditUsage() != null) {
                log.info("Job task {} credit usage calculated: {} credits", 
                        jobTaskUuid, jobTask.getCreditUsage());
            }
        } else {
            log.warn("Could not calculate cost for job task {}", jobTaskUuid);
        }
        
        // Save the task with calculated cost and credit usage
        jobTaskRepository.save(jobTask);
        
        // Update job-level credit usage by summing all completed tasks
        try {
            jobCreditService.updateJobCreditUsage(jobTask.getJobUuid());
        } catch (Exception e) {
            log.error("Error updating job credit usage for job {}: {}", 
                    jobTask.getJobUuid(), e.getMessage(), e);
        }
        
        // Debit the credits used from the user's account (if credit usage is calculated)
        if (jobTask.getCreditUsage() != null && userId != null) {
            try {
                
                // Create a transaction DTO to debit credits (negative amount represents debit)
                AccountCreditTransactionDto transactionDto = new AccountCreditTransactionDto();
                transactionDto.setChangeAmount(-1 * jobTask.getCreditUsage()); // Negative amount for debit
                transactionDto.setReason("Task credits used: " + jobTask.getJobTaskUuid());
                transactionDto.setReferenceId(jobTask.getJobTaskUuid().toString());
                
                // Get the first account for the user
                List<AccountDto> accounts = jobCreditService.getUserAccounts(userId);
                
                if (accounts != null && !accounts.isEmpty()) {
                    UUID accountUuid = accounts.get(0).getAccountUuid();
                    
                    // Call the account client to debit credits
                    accountClient.addCredits(accountUuid, transactionDto);
                    log.info("Debited {} credits from account {} for task {}", 
                            jobTask.getCreditUsage(), accountUuid, jobTaskUuid);
                } else {
                    log.error("No accounts found for user {}", userId);
                }
            } catch (Exception e) {
                // Don't fail the task if debiting fails, just log the error
                log.error("Error debiting credits for task {}: {}", jobTaskUuid, e.getMessage(), e);
            }
        }
        
        if (chatResponse.getTotalTokens() != null) {
            log.info("Job task {} completed with {} tokens (prompt: {}, completion: {})", 
                jobTaskUuid, 
                chatResponse.getTotalTokens(),
                chatResponse.getPromptTokens(),
                chatResponse.getCompletionTokens());
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failTask(UUID jobTaskUuid, String errorMessage) {
        JobTask jobTask = jobTaskRepository.findById(jobTaskUuid).orElse(null);
        if (jobTask == null) {
            log.error("Job task not found: {}", jobTaskUuid);
            return;
        }
        
        jobTask.setStatus(TaskStatus.FAILED);
        jobTask.setErrorMessage(errorMessage);
        jobTask.setEndTimestamp(LocalDateTime.now());
        
        jobTaskRepository.save(jobTask);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTaskToInsufficientCredits(UUID jobTaskUuid) {
        JobTask jobTask = jobTaskRepository.findById(jobTaskUuid).orElse(null);
        if (jobTask == null) {
            log.error("Job task not found: {}", jobTaskUuid);
            return;
        }
        
        jobTask.setStatus(TaskStatus.INSUFFICIENT_CREDITS);
        jobTask.setErrorMessage("Insufficient credits available to process this task");
        jobTask.setEndTimestamp(LocalDateTime.now());
        
        jobTaskRepository.save(jobTask);
        log.info("Job task {} marked as INSUFFICIENT_CREDITS", jobTaskUuid);
    }

    private String replacePlaceholders(String promptText, JsonNode recordData) {
        // Find all of the placeholders in the promt text (e.g., {{fieldName}})
        // For each one, try to find a matching field in the record data.
        //
        // Matching is done as follows:
        // 1. exact name
        // 2. trim both sides and compare
        // 3. trim both sides, convert to lowercase and compare
        // 4. trim both sides, convert to lowercase and replace spaces with underscores

        // First, find all placeholders in the prompt text
        String regex = "\\{\\{\\s*([^\\s]+)\\s*\\}\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(promptText);
        while (matcher.find()) {
            String placeholder = matcher.group(0);
            String placeholderName = matcher.group(1).trim();
            String fieldValue = null;

            fieldValue = findMatchingFieldValue(placeholderName, recordData, s -> s);

            if (fieldValue == null) {
                fieldValue = findMatchingFieldValue(placeholderName, recordData, s-> s.trim());
            }

            if (fieldValue == null) {
                fieldValue = findMatchingFieldValue(placeholderName, recordData, s -> s.trim().toLowerCase());
            }

            if (fieldValue == null) {
                fieldValue = findMatchingFieldValue(placeholderName, recordData, s -> s.trim().toLowerCase().replace(" ", "_"));
            }

            if (fieldValue == null) {
               log.warn("Field not found in record data: {}", placeholderName);
               fieldValue = "";
            }

            promptText = promptText.replace(placeholder, fieldValue);
        }
        return promptText;
    }

    private String findMatchingFieldValue(String placeholderName, JsonNode recordData, java.util.function.Function<String, String> transform) {
        String transformedPlaceholderName = placeholderName != null ? transform.apply(placeholderName) : null;

        for (var property : recordData.properties()) {
            String transformedPropertyName = transform.apply(property.getKey());
            if (transformedPropertyName.equals(transformedPlaceholderName)) {
                return property.getValue().asText();
            }
        }
        return null;
    }
}