package com.batchprompt.jobs.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.jobs.client.FileClient;
import com.batchprompt.jobs.client.PromptClient;
import com.batchprompt.jobs.dto.FileRecordDto;
import com.batchprompt.jobs.dto.JobTaskMessage;
import com.batchprompt.jobs.dto.PromptDto;
import com.batchprompt.jobs.model.JobTask;
import com.batchprompt.jobs.repository.JobTaskRepository;
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
            PromptDto promptDto = promptClient.getPrompt(message.getPromptUuid(), message.getAuthToken());
            if (promptDto == null) {
                throw new Exception("Prompt not found: " + message.getPromptUuid());
            }
            
            // Get file record
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
            
            String responseText = chatModel.generateResponse(
                    promptDto.getPromptText(),
                    message.getModelName(),
                    recordData,
                    outputSchema
            );
            
            // Step 4: If successful, update status to Completed in a separate transaction
            completeTask(jobTaskUuid, responseText);
            
            log.info("Job task {} completed successfully", jobTaskUuid);
        } catch (Exception e) {
            // Step 5: If failed, update status to Failed in a separate transaction
            log.error("Error processing job task: {}", jobTaskUuid, e);
            
            failTask(jobTaskUuid, e.getMessage());
        } finally {
            // Step 6 & 7: Update the job status based on the status of all its tasks
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
        
        // Update the status of the job task to Processing and set beginTimestamp
        jobTask.setStatus(JobTask.Status.Processing);
        jobTask.setBeginTimestamp(LocalDateTime.now());
        return jobTaskRepository.save(jobTask);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeTask(UUID jobTaskUuid, String responseText) {
        JobTask jobTask = jobTaskRepository.findById(jobTaskUuid).orElse(null);
        if (jobTask == null) {
            log.error("Job task not found when completing task: {}", jobTaskUuid);
            return;
        }
        
        jobTask.setStatus(JobTask.Status.Completed);
        jobTask.setResponseText(responseText);
        jobTask.setEndTimestamp(LocalDateTime.now());
        jobTaskRepository.save(jobTask);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failTask(UUID jobTaskUuid, String errorMessage) {
        JobTask jobTask = jobTaskRepository.findById(jobTaskUuid).orElse(null);
        if (jobTask == null) {
            log.error("Job task not found when failing task: {}", jobTaskUuid);
            return;
        }
        
        jobTask.setStatus(JobTask.Status.Failed);
        jobTask.setEndTimestamp(LocalDateTime.now());
        jobTask.setErrorMessage(errorMessage);
        jobTaskRepository.save(jobTask);
    }
}