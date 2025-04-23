package com.batchprompt.jobs.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.batchprompt.jobs.client.FileClient;
import com.batchprompt.jobs.client.PromptClient;
import com.batchprompt.jobs.dto.FileDto;
import com.batchprompt.jobs.dto.FileRecordDto;
import com.batchprompt.jobs.dto.JobTaskMessage;
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
    private final MessageProducer messageProducer;
    
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
                .status(Job.Status.SUBMITTED)
                .taskCount(fileRecords.size())
                .completedTaskCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
                
        jobRepository.save(job);
        
        // Store messages to be sent after transaction commits
        final List<JobTaskMessage> messagesToSend = new ArrayList<>();
        
        // Create job tasks
        for (FileRecordDto record : fileRecords) {
            UUID jobTaskUuid = UUID.randomUUID();
            
            JobTask task = JobTask.builder()
                    .jobTaskUuid(jobTaskUuid)
                    .jobUuid(jobUuid)
                    .fileRecordUuid(record.getFileRecordUuid())
                    .modelName(modelName)
                    .status(JobTask.Status.Submitted)
                    .build();
                    
            jobTaskRepository.save(task);
            
            // Create the message but don't send it yet
            JobTaskMessage message = JobTaskMessage.builder()
                    .jobTaskUuid(jobTaskUuid)
                    .jobUuid(jobUuid)
                    .fileRecordUuid(record.getFileRecordUuid())
                    .modelName(modelName)
                    .promptUuid(promptUuid)
                    .userId(userId)
                    .authToken(authToken)
                    .build();
                    
            messagesToSend.add(message);
        }
        
        // Register a callback to be executed after the transaction is successfully committed
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                // Now that the transaction has committed, send all messages
                for (JobTaskMessage message : messagesToSend) {
                    messageProducer.sendJobTask(message);
                }
                log.info("Sent {} job task messages for job {} after transaction commit", messagesToSend.size(), jobUuid);
            }
        });
        
        log.info("Job {} submitted for file {} and prompt {}", jobUuid, fileUuid, promptUuid);
        
        return job;
    }
    
    /**
     * Update job status based on the status of its tasks
     * 
     * @param jobUuid The UUID of the job to update
     */
    @Transactional
    public void updateJobStatus(UUID jobUuid) {
        Job job = jobRepository.findById(jobUuid).orElse(null);
        if (job == null) {
            log.error("Job not found: {}", jobUuid);
            return;
        }
        
        List<JobTask> tasks = jobTaskRepository.findByJobUuid(jobUuid);
        if (tasks.isEmpty()) {
            log.error("No tasks found for job: {}", jobUuid);
            return;
        }
        
        int completedCount = 0;
        int failedCount = 0;
        
        for (JobTask task : tasks) {
            if (task.getStatus() == JobTask.Status.Completed) {
                completedCount++;
            } else if (task.getStatus() == JobTask.Status.Failed) {
                failedCount++;
            }
        }
        
        job.setCompletedTaskCount(completedCount + failedCount);
        
        // Update job status based on task status
        if (completedCount + failedCount == tasks.size()) {
            if (failedCount > 0 && completedCount > 0) {
                job.setStatus(Job.Status.COMPLETED_WITH_ERRORS);
            } else if (failedCount > 0) {
                job.setStatus(Job.Status.FAILED);
            } else {
                job.setStatus(Job.Status.COMPLETED);
            }
        } else if (job.getStatus() == Job.Status.SUBMITTED) {
            job.setStatus(Job.Status.PROCESSING);
        }
        
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
        
        log.info("Updated job {} status to {}", jobUuid, job.getStatus());
    }
}