package com.batchprompt.jobs.core.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.batchprompt.files.client.FileClient;
import com.batchprompt.files.model.FileStatus;
import com.batchprompt.files.model.dto.FileDto;
import com.batchprompt.files.model.dto.FileRecordDto;
import com.batchprompt.jobs.core.exception.JobSubmissionException;
import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.JobRepository;
import com.batchprompt.jobs.core.repository.JobTaskRepository;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.TaskStatus;
import com.batchprompt.jobs.model.dto.JobOutputMessage;
import com.batchprompt.jobs.model.dto.JobTaskMessage;
import com.batchprompt.prompts.client.PromptClient;
import com.batchprompt.prompts.model.dto.PromptDto;
import com.batchprompt.jobs.core.model.JobOutputField;
import com.batchprompt.jobs.core.repository.JobOutputFieldRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobTaskRepository jobTaskRepository;
    private final JobOutputFieldRepository jobOutputFieldRepository;
    private final FileClient fileClient;
    private final PromptClient promptClient;
    private final ModelService modelService;
    private final MessageProducer messageProducer;
    private final ObjectProvider<JobService> selfProvider;

    
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
     * Get paginated jobs for a specific user with sorting
     * 
     * @param userId The user ID to retrieve jobs for
     * @param pageable Pageable object containing pagination and sorting information
     * @return Page of jobs for the user
     */
    public Page<Job> getJobsByUserIdPaginated(String userId, Pageable pageable) {
        return jobRepository.findByUserId(userId, pageable);
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
     * Submit a new job for processing with specific output fields
     * 
     * @param fileUuid The UUID of the file to process
     * @param promptUuid The UUID of the prompt to use
     * @param modelName The name of the model to use
     * @param outputFieldUuids List of field UUIDs to include in the output (can be null for all fields)
     * @param userId The user ID submitting the job
     * @param authToken The authentication token for calling other services
     * @return The created job
     * @throws JobSubmissionException If there's an error during job submission
     */
    @Transactional
    public Job submitJob(UUID fileUuid, UUID promptUuid, String modelName, List<UUID> outputFieldUuids, String userId, String authToken) {
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
        if (file.getStatus() != FileStatus.READY) {
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
                .fileName(file.getFileName())
                .promptUuid(promptUuid)
                .modelName(modelName)
                .status(JobStatus.SUBMITTED)
                .taskCount(fileRecords.size())
                .completedTaskCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
                
        jobRepository.save(job);
        
        // Log the outputFieldUuids parameter to check if it's null or empty
        log.info("Job {} submitted with outputFieldUuids: {}", jobUuid, outputFieldUuids);
        
        // Save selected output fields if provided
        if (outputFieldUuids != null && !outputFieldUuids.isEmpty()) {
            int fieldOrder = 0;
            for (UUID fieldUuid : outputFieldUuids) {
                JobOutputField outputField = JobOutputField.builder()
                        .jobOutputFieldUuid(UUID.randomUUID())
                        .job(job)
                        .fieldOrder(fieldOrder++)
                        .fieldUuid(fieldUuid)
                        .build();
                jobOutputFieldRepository.save(outputField);
            }
            log.info("Saved {} output fields for job {}", outputFieldUuids.size(), jobUuid);
        } else {
            log.info("No output fields specified for job {}, will include all fields", jobUuid);
        }
        
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
                    .status(TaskStatus.SUBMITTED)
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
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
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
     * Submit a new job for processing with all fields included
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
        return submitJob(fileUuid, promptUuid, modelName, null, userId, authToken);
    }
    
    /**
     * Update job status based on the status of its tasks
     * 
     * @param jobUuid The UUID of the job to update
     */

    public void updateJobStatus(UUID jobUuid) {
        final int MAX_RETRIES = 10;
        int retryCount = 0;
        while (true) {
            try {
                
                /*
                 * Need a separate transaction for each retry. In order to do that, we need to
                 * call updateJobStatusNoRetry() from self to ensure the spring proxy is used
                 * and the transaction is created correctly.
                 */
                var self = selfProvider.getIfAvailable();
                if (self == null) {
                    throw new IllegalStateException("Self provider is not available");
                }
                self.updateJobStatusNoRetry(jobUuid);
                break; // Exit loop if successful
            } catch (ObjectOptimisticLockingFailureException e) {
                // Handle optimistic locking exception - this happens when another thread has updated the job
                if (retryCount < MAX_RETRIES) {
                    log.info("Optimistic locking conflict detected for job {}, retrying update ({}/{})", 
                        jobUuid, retryCount + 1, MAX_RETRIES);
                    
                    // Small delay before retry to reduce contention
                    try {
                        Thread.sleep(50 * (retryCount + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    
                    retryCount++;
                } else {
                    log.error("Failed to update job {} status after {} retries due to concurrent modifications", 
                        jobUuid, MAX_RETRIES);
                    throw e;
                }
            }
        }    
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobStatusNoRetry(UUID jobUuid) {
        
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
            if (task.getStatus() == TaskStatus.COMPLETED) {
                completedCount++;
            } else if (task.getStatus() == TaskStatus.FAILED) {
                failedCount++;
            }
        }
            
        // We only update the count if it has changed
        boolean countsChanged = job.getCompletedTaskCount() != (completedCount + failedCount); 
        if (countsChanged) {
            job.setCompletedTaskCount(completedCount + failedCount);
        }
        
        JobStatus newStatus = null;
        
        // Update job status based on task status
        if (completedCount + failedCount == tasks.size()) {
            // All tasks are completed or failed, update to PENDING_OUTPUT
            newStatus = JobStatus.PENDING_OUTPUT;
        } else if (job.getStatus() == JobStatus.SUBMITTED) {
            // First task started processing
            newStatus = JobStatus.PROCESSING;
        }
        
        // Only save if there are actual changes to make
        if (countsChanged || (newStatus != null && job.getStatus() != newStatus)) {
            if (newStatus != null) {
                job.setStatus(newStatus);
            }
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // If all tasks completed, and we're changing status to PENDING_OUTPUT
            if (newStatus == JobStatus.PENDING_OUTPUT) {
                log.info("All tasks completed for job {}. Setting status to {} and queueing for output processing", 
                            jobUuid, newStatus);
                            
                // Queue the job for output processing after the transaction is committed
                final UUID finalJobUuid = job.getJobUuid();
                final String userId = job.getUserId();
                final boolean hasErrors = failedCount > 0;
                
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // Send job output processing message
                        JobOutputMessage outputMessage = JobOutputMessage.builder()
                                .jobUuid(finalJobUuid)
                                .userId(userId)
                                .hasErrors(hasErrors)
                                .build();
                        
                        messageProducer.sendJobOutput(outputMessage);
                        log.info("Sent job output message for job {} after transaction commit", finalJobUuid);
                    }
                });
            } else if (countsChanged || newStatus != null) {
                log.info("Updated job {} status to {}, completed tasks: {}/{}", 
                    jobUuid, job.getStatus(), job.getCompletedTaskCount(), tasks.size());
            }
        }

    }
}