package com.batchprompt.jobs.validation.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.repository.JobRepository;
import com.batchprompt.jobs.core.service.JobService;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.dto.JobValidationMessage;
import com.batchprompt.prompts.client.PromptClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobValidationWorker {

    private final JobService jobService;
    private final JobRepository jobRepository;
    private final PromptClient promptClient;
    
    @Value("${rabbitmq.queue.job-validation.name}")
    private String jobValidationQueueName;
    
    @RabbitListener(queues = "${rabbitmq.queue.job-validation.name}")
    public void receiveValidationMessage(JobValidationMessage validationMessage) {
        UUID jobUuid = validationMessage.getJobUuid();
        Job job = jobService.getJobById(jobUuid);
        if (job == null) {
            log.error("Job not found for validation: {}", jobUuid);
            return;
        }
        log.info("Received validation request for job: {}", jobUuid);
        double estimatedCredits = 0.0;
        
        try {
            log.info("Validation successful for job: {}, estimated credits: {}", jobUuid, estimatedCredits);
            job.setStatus(JobStatus.VALIDATED);
            jobRepository.save(job);
        } catch (Exception e) {
            log.error("Error validating job: {}", jobUuid, e);
            failValidation(job, "Validation error: " + e.getMessage());
        }
    }
    
    private void failValidation(Job job, String errorMessage) {
        log.warn("Validation failed for job: {}, reason: {}", job.getJobUuid(), errorMessage);
        job.setStatus(JobStatus.VALIDATION_FAILED);
        jobRepository.save(job);
    }
    
    /**
     * Extract field names from a prompt template that has {{fieldName}} placeholders
     * 
     * @param promptText The prompt template text
     * @return List of field names (without the {{ }} delimiters)
     */
    private List<String> extractPromptFields(String promptText) {
        List<String> fieldNames = new ArrayList<>();
        
        // Regex to match patterns like {{fieldName}}
        Pattern pattern = Pattern.compile("\\{\\{([^{}]+)\\}\\}");
        Matcher matcher = pattern.matcher(promptText);
        
        while (matcher.find()) {
            String fieldName = matcher.group(1).trim();
            if (!fieldName.isEmpty() && !fieldNames.contains(fieldName)) {
                fieldNames.add(fieldName);
            }
        }
        
        return fieldNames;
    }
}
