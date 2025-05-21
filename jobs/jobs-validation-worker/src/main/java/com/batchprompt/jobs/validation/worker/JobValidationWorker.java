package com.batchprompt.jobs.validation.worker;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.batchprompt.files.client.FileClient;
import com.batchprompt.files.model.dto.FileRecordDto;
import com.batchprompt.jobs.core.exception.JobSubmissionException;
import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.model.JobValidationResultMessage;
import com.batchprompt.jobs.core.repository.JobRepository;
import com.batchprompt.jobs.core.repository.JobTaskRepository;
import com.batchprompt.jobs.core.repository.JobValidationResultMessageRepository;
import com.batchprompt.jobs.core.service.CreditCalculationService;
import com.batchprompt.jobs.core.service.JobNotificationService;
import com.batchprompt.jobs.core.service.JobPricingService;
import com.batchprompt.jobs.core.service.JobService;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.TaskStatus;
import com.batchprompt.jobs.model.dto.JobValidationMessage;
import com.batchprompt.prompts.client.PromptClient;
import com.batchprompt.prompts.model.dto.PromptDto;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobValidationWorker {

    private final JobService jobService;
    private final JobRepository jobRepository;
    private final FileClient fileClient;
    private final PromptClient promptClient;
    private final JobTaskRepository jobTaskRepository;
    private final JobPricingService jobPricingService;
    private final CreditCalculationService creditCalculationService;
    private final JobValidationResultMessageRepository jobValidationResultMessageRepository;
    private final JobNotificationService jobNotificationService;
    
    @Value("${rabbitmq.queue.job-validation.name}")
    private String jobValidationQueueName;
    
    @RabbitListener(queues = "${rabbitmq.queue.job-validation.name}")
    public void processJobValidationMessage(JobValidationMessage validationMessage) {
        UUID jobUuid = validationMessage.getJobUuid();
        Job job = jobService.getJobById(jobUuid);
        if (job == null) {
            log.error("Job not found for validation: {}", jobUuid);
            return;
        }
        log.info("Received validation request for job: {}", jobUuid);

        boolean valid = buildJobTasks(job);
        
        try {
            log.info("Validation successful for job: {}, estimated credits: {}", jobUuid, job.getCreditEstimate());
            job.setStatus(valid ? JobStatus.VALIDATED : JobStatus.VALIDATION_FAILED);
            jobRepository.save(job);
        } catch (Exception e) {
            log.error("Error validating job: {}", jobUuid, e);
            failValidation(job, "Validation error: " + e.getMessage());
        }
        jobNotificationService.sendJobUpdateNotification(job);
    }

    private boolean buildJobTasks(Job job) {

        PromptDto promptDto = promptClient.getPrompt(job.getPromptUuid(), null);

        int startRecordNumber = job.getStartRecordNumber() != null ? job.getStartRecordNumber() : 1;
        Integer maxRecords = job.getMaxRecords();
        
        // Get file record count to determine how many records to process
        int pageSize = 100; // Process records in batches of 100
        int startPage = (startRecordNumber - 1) / pageSize; // Calculate the starting page based on startRecordNumber
        
        // Retrieve first page to get total count and start processing
        Page<FileRecordDto> recordsPage = fileClient.getFileRecordsPaginated(
            job.getFileUuid(), 
            startPage, 
            pageSize, 
            "recordNumber", 
            "asc", 
            null
        );
        
        if (recordsPage == null || recordsPage.getTotalElements() == 0) {
            throw new JobSubmissionException("No records found for file: " + job.getFileUuid());
        }
        
        // Calculate total records to process based on maxRecords and starting position
        long totalRecords = recordsPage.getTotalElements();
        long recordsToProcess = maxRecords != null 
            ? Math.min(maxRecords, totalRecords - (startRecordNumber - 1)) 
            : (totalRecords - (startRecordNumber - 1));

        log.info("Processing {} records from file {} starting at record {}", recordsToProcess, job.getFileUuid(), startRecordNumber);

            
        if (recordsToProcess <= 0) {
            throw new JobSubmissionException("No records to process based on startRecordNumber: " + startRecordNumber);
        }        
        // Placeholder for building job tasks
        // This method should be implemented to create job tasks based on the job details

        double totalEstimatedCost = 0.0;
        double totalEstimatedCredits = 0.0;
        int failedCount = 0;
        int taskCount = 0;

        while (recordsPage.getNumberOfElements() > 0) {
            // Check if we have enough records to process
            if (recordsToProcess <= 0) {
                break;
            }
            
            // Create job tasks for the first batch
            for (int i = 0; i < recordsPage.getNumberOfElements(); i++) {
                // Check if we've hit the max records limit
                if (maxRecords != null && taskCount >= maxRecords) {
                    break;
                }
                
                FileRecordDto record = recordsPage.getContent().get(i);
                // Skip records with record numbers less than startRecordNumber
                if (record.getRecordNumber() < startRecordNumber) {
                    continue;
                }
                
                UUID jobTaskUuid = UUID.randomUUID();
                
                String promptText = createPromptText(job, promptDto, record);
                if (promptText == null) {
                    failedCount++;
                    continue;
                }
                int estimatedPromptTokens = estimateInputTokens(promptText);
                int estimatedCompletionTokens = 2000; // Placeholder for completion tokens
                int estimatedThinkingTokens = 0; // Placeholder for thinking tokens

                // Estimate the cost
                double estimatedCost = jobPricingService.calculateCost(
                    job.getModelId(), 
                    estimatedPromptTokens, 
                    estimatedCompletionTokens, 
                    estimatedThinkingTokens
                );

                double estimatedCredits = creditCalculationService.calculateCreditUsage(
                    job.getModelId(), 
                    estimatedCost, 
                    LocalDateTime.now()
                );

                totalEstimatedCost += estimatedCost;
                totalEstimatedCredits += estimatedCredits;

                JobTask task = JobTask.builder()
                        .jobTaskUuid(jobTaskUuid)
                        .jobUuid(job.getJobUuid())
                        .fileRecordUuid(record.getFileRecordUuid())
                        .recordNumber(record.getRecordNumber())
                        .promptText(promptText)
                        .estimatedPromptTokens(estimatedPromptTokens)
                        .estimatedCompletionTokens(estimatedCompletionTokens)
                        .estimatedThinkingTokens(estimatedThinkingTokens)
                        .costEstimate(estimatedCost)
                        .creditEstimate(estimatedCredits)
                        .modelId(job.getModelId())
                        .status(TaskStatus.SUBMITTED)
                        .build();
                        
                jobTaskRepository.save(task);

                taskCount++;
            }
            
            // Check if we need to process more pages
            if (recordsToProcess > recordsPage.getNumberOfElements()) {
                recordsToProcess -= recordsPage.getNumberOfElements();
                startPage++;
                
                // If we've already processed maxRecords, no need to fetch more pages
                if (maxRecords != null && taskCount >= maxRecords) {
                    break;
                }
                
                // Get the next page of records
                recordsPage = fileClient.getFileRecordsPaginated(
                    job.getFileUuid(), 
                    startPage, 
                    pageSize, 
                    "recordNumber", 
                    "asc", 
                    null
                );
            } else {
                break;
            }
        }
        // Update job with total estimated cost and credits
        job.setCostEstimate(totalEstimatedCost);
        job.setCreditEstimate(totalEstimatedCredits);
        job.setTaskCount(taskCount);
        log.info("Job tasks created for job: {}, total estimated cost: {}, total estimated credits: {}", 
            job.getJobUuid(), totalEstimatedCost, totalEstimatedCredits);
            
        return failedCount == 0;
    }

    private int estimateInputTokens(String promptText) {
        // Placeholder for estimating input tokens
        // This method should be implemented to estimate the number of tokens in the prompt text
        return promptText.length() / 4; // Example: 1 token = 4 characters
    }

    private String createPromptText(Job job, PromptDto promptDto, FileRecordDto recordDto) {
        // Replace placeholders in the prompt template with actual record values
        String promptText = promptDto.getPromptText();

        // Convert record data to JsonNode
        JsonNode recordData = recordDto.getRecord();

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
               addValidationResultMessage(job, recordDto.getRecordNumber(), placeholderName, "Field not found in record data: " + placeholderName);
               return null;
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
    
    private void failValidation(Job job, String errorMessage) {
        log.warn("Validation failed for job: {}, reason: {}", job.getJobUuid(), errorMessage);
        job.setStatus(JobStatus.VALIDATION_FAILED);
        jobRepository.save(job);
    }

    private void addValidationResultMessage(Job job, int recordNumber, String fieldName, String message) {
        JobValidationResultMessage validationMessage = JobValidationResultMessage.builder()
                .jobValidationMessageUuid(UUID.randomUUID())
                .jobUuid(job.getJobUuid())
                .recordNumber(recordNumber)
                .fieldName(fieldName)
                .message(message)
                .build();
        jobValidationResultMessageRepository.save(validationMessage);
        log.info("Validation message added for job: {}, field: {}, message: {}", job.getJobUuid(), fieldName, message);
    }

}
