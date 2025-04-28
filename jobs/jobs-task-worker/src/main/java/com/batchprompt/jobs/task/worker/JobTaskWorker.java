package com.batchprompt.jobs.task.worker;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.files.client.FileClient;
import com.batchprompt.files.model.dto.FileRecordDto;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.JobTaskRepository;
import com.batchprompt.jobs.core.service.ChatModel;
import com.batchprompt.jobs.core.service.JobService;
import com.batchprompt.jobs.core.service.ModelService;
import com.batchprompt.jobs.model.TaskStatus;
import com.batchprompt.jobs.model.dto.JobTaskMessage;
import com.batchprompt.prompts.client.PromptClient;
import com.batchprompt.prompts.model.dto.PromptDto;
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

    @RabbitListener(queues = "${rabbitmq.queue.jobs.name}")
    public void processJobTask(JobTaskMessage message) {
        UUID jobTaskUuid = message.getJobTaskUuid();
        UUID jobUuid = message.getJobUuid();
        
        log.info("Processing job task: {}", jobTaskUuid);
        
        JobTask jobTask = null;
        
        try {
            // Use a separate transaction to get and update the job task status to Processing
            jobTask = updateTaskToProcessing(jobTaskUuid);
            if (jobTask == null) {
                return; // Already logged in the updateTaskToProcessing method
            }
            
            // Step 2: Get the ChatModel for the model name. If job status is Submitted, update it to Processing
            ChatModel chatModel = modelService.getModel(message.getModelName());
            if (chatModel == null) {
                throw new Exception("Model not found: " + message.getModelName());
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
            if (promptDto.getOutputSchema() != null && !promptDto.getOutputSchema().isBlank()) {
                outputSchema = objectMapper.readTree(promptDto.getOutputSchema());
            }

            String promptText = replacePlaceholders(promptDto.getPromptText(), recordData);
            
            String responseText = chatModel.generateResponse(
                    promptText,
                    message.getModelName(),
                    outputSchema
            );
            
            // Step 4: If successful, update status to Completed in a separate transaction
            completeTask(jobTaskUuid, responseText);
            
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
    public void completeTask(UUID jobTaskUuid, String responseText) {
        JobTask jobTask = jobTaskRepository.findById(jobTaskUuid).orElse(null);
        if (jobTask == null) {
            log.error("Job task not found: {}", jobTaskUuid);
            return;
        }
        
        jobTask.setStatus(TaskStatus.COMPLETED);
        jobTask.setResponseText(responseText);
        jobTask.setEndTimestamp(LocalDateTime.now());
        
        jobTaskRepository.save(jobTask);
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