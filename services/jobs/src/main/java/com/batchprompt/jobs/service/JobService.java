package com.batchprompt.jobs.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.jobs.client.FileClient;
import com.batchprompt.jobs.client.PromptClient;
import com.batchprompt.jobs.dto.FileDto;
import com.batchprompt.jobs.dto.FileRecordDto;
import com.batchprompt.jobs.dto.PromptDto;
import com.batchprompt.jobs.exception.JobSubmissionException;
import com.batchprompt.jobs.model.Job;
import com.batchprompt.jobs.model.JobTask;
import com.batchprompt.jobs.repository.JobRepository;
import com.batchprompt.jobs.repository.JobTaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobTaskRepository jobTaskRepository;
    private final FileClient fileClient;
    private final PromptClient promptClient;
    private final ModelService modelService;
    
    /**
     * Get all jobs
     * 
     * @return List of all jobs
     */
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }
    
    /**
     * Get a job by its UUID
     * 
     * @param jobUuid The UUID of the job to retrieve
     * @return The job if found, or null if not found
     */
    public Job getJobById(UUID jobUuid) {
        return jobRepository.findById(jobUuid).orElse(null);
    }
    
    /**
     * Get all jobs for a specific user
     * 
     * @param userId The user ID to retrieve jobs for
     * @return List of jobs for the user
     */
    public List<Job> getJobsByUserId(String userId) {
        return jobRepository.findByUserId(userId);
    }
    
    /**
     * Get all tasks for a specific job
     * 
     * @param jobUuid The UUID of the job to retrieve tasks for
     * @return List of tasks for the job
     */
    public List<JobTask> getTasksByJobId(UUID jobUuid) {
        return jobTaskRepository.findByJobUuid(jobUuid);
    }
    
    /**
     * Submit a new job for processing
     * 
     * @param fileUuid The UUID of the file to process
     * @param promptUuid The UUID of the prompt to use
     * @param modelName The name of the model to use
     * @param userId The user ID submitting the job
     * @param authToken The authentication token for calling other services
     * @return The created job
     * @throws JobSubmissionException If there's an error during job submission
     */
    @Transactional
    public Job submitJob(UUID fileUuid, UUID promptUuid, String modelName, String userId, String authToken) {
        // Validate model
        if (!modelService.isModelSupported(modelName)) {
            throw new JobSubmissionException("Unsupported model: " + modelName);
        }
        
        // Validate file exists and belongs to the user
        FileDto file = fileClient.getFile(fileUuid, authToken);
        if (file == null) {
            throw new JobSubmissionException("File not found: " + fileUuid);
        }
        
        if (!file.getUserId().equals(userId)) {
            throw new JobSubmissionException("File " + fileUuid + " does not belong to user " + userId);
        }
        
        // Validate file is ready
        if (!"Ready".equals(file.getStatus())) {
            throw new JobSubmissionException("File is not ready for processing. Current status: " + file.getStatus());
        }
        
        // Validate prompt exists and belongs to the user
        PromptDto prompt = promptClient.getPrompt(promptUuid, authToken);
        if (prompt == null) {
            throw new JobSubmissionException("Prompt not found: " + promptUuid);
        }
        
        if (!prompt.getUserId().equals(userId)) {
            throw new JobSubmissionException("Prompt " + promptUuid + " does not belong to user " + userId);
        }
        
        // Get file records
        List<FileRecordDto> fileRecords = fileClient.getFileRecords(fileUuid, authToken);
        if (fileRecords == null || fileRecords.isEmpty()) {
            throw new JobSubmissionException("No records found for file: " + fileUuid);
        }
        
        // Create job
        UUID jobUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        Job job = Job.builder()
                .jobUuid(jobUuid)
                .userId(userId)
                .fileUuid(fileUuid)
                .promptUuid(promptUuid)
                .modelName(modelName)
                .status(Job.Status.Submitted)
                .taskCount(fileRecords.size())
                .completedTaskCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
                
        jobRepository.save(job);
        
        // Create job tasks
        for (FileRecordDto record : fileRecords) {
            JobTask task = JobTask.builder()
                    .jobTaskUuid(UUID.randomUUID())
                    .jobUuid(jobUuid)
                    .fileRecordUuid(record.getFileRecordUuid())
                    .modelName(modelName)
                    .status(JobTask.Status.Submitted)
                    .build();
                    
            jobTaskRepository.save(task);
        }
        
        log.info("Job {} submitted for file {} and prompt {}", jobUuid, fileUuid, promptUuid);
        
        // TODO: Send message to message queue for processing
        
        return job;
    }
}