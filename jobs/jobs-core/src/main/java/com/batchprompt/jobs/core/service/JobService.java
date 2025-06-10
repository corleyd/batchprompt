package com.batchprompt.jobs.core.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
import com.batchprompt.jobs.core.exception.JobSubmissionException;
import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobOutputField;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.JobOutputFieldRepository;
import com.batchprompt.jobs.core.repository.JobRepository;
import com.batchprompt.jobs.core.repository.JobTaskRepository;
import com.batchprompt.jobs.core.repository.dto.TaskStatusCount;
import com.batchprompt.jobs.core.specification.JobSpecification;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.TaskStatus;
import com.batchprompt.jobs.model.dto.JobDefinitionDto;
import com.batchprompt.jobs.model.dto.JobOutputMessage;
import com.batchprompt.jobs.model.dto.JobTaskMessage;
import com.batchprompt.jobs.model.dto.JobValidationMessage;
import com.batchprompt.prompts.client.PromptClient;
import com.batchprompt.prompts.model.dto.PromptDto;
import com.batchprompt.prompts.model.dto.PromptJobInfoDto;

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
    private final JobNotificationService jobNotificationService;
    
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
     * Get paginated jobs with optional filtering by userId, modelId, and status
     * 
     * @param userId Optional user ID to filter by
     * @param modelId Optional model id to filter by
     * @param status Optional job status to filter by
     * @param pageable Pageable object containing pagination and sorting information
     * @return Page of filtered jobs
     */
    public Page<Job> getJobsPaginated(String userId, String modelId, UUID promptUuid, JobStatus status, Pageable pageable) {
        return jobRepository.findAll(JobSpecification.withFilters(userId, modelId, promptUuid, status), pageable);
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
     * Get paginated tasks for a specific job with sorting
     * 
     * @param jobUuid The UUID of the job to retrieve tasks for
     * @param pageable Pageable object containing pagination and sorting information
     * @return Page of tasks for the job
     */
    public Page<JobTask> getTasksByJobIdPaginated(UUID jobUuid, Pageable pageable) {
        return jobTaskRepository.findByJobUuid(jobUuid, pageable);
    }
    
    /**
     * Create a new job based on the provided job submit for validation
     * 
     * @param jobDefinitionDto The job submission data transfer object
     * @param userId The user ID submitting the job
     * @param authToken The authentication token for calling other services
     * @return The created job
     * @throws JobSubmissionException If there's an error during job submission
     */
    @Transactional
    public Job validateJob(JobDefinitionDto jobDefinitionDto, String userId, String authToken) {
        // Validate model
        if (!modelService.isModelSupported(jobDefinitionDto.getModelId())) {
            throw new JobSubmissionException("Unsupported model: " + jobDefinitionDto.getModelId());
        }
        
        // Validate file exists and belongs to the user
        FileDto file = fileClient.getFile(jobDefinitionDto.getFileUuid(), authToken);
        if (file == null) {
            throw new JobSubmissionException("File not found: " + jobDefinitionDto.getFileUuid());
        }
        
        if (!file.getUserId().equals(userId)) {
            throw new JobSubmissionException("File " + jobDefinitionDto.getFileUuid() + " does not belong to user " + userId);
        }
        
        // Validate file is ready
        if (file.getStatus() != FileStatus.READY) {
            throw new JobSubmissionException("File is not ready for processing. Current status: " + file.getStatus());
        }
        
        // Validate prompt exists and belongs to the user
        PromptDto prompt = promptClient.getPrompt(jobDefinitionDto.getPromptUuid(), authToken);
        if (prompt == null) {
            throw new JobSubmissionException("Prompt not found: " + jobDefinitionDto.getPromptUuid());
        }
        
        if (!prompt.getUserId().equals(userId)) {
            throw new JobSubmissionException("Prompt " + jobDefinitionDto.getPromptUuid() + " does not belong to user " + userId);
        }
        
        // Create job
        UUID jobUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        // Determine record range parameters
      
        Job job = Job.builder()
                .jobUuid(jobUuid)
                .userId(userId)
                .fileUuid(jobDefinitionDto.getFileUuid())
                .fileName(file.getFileName())
                .promptUuid(jobDefinitionDto.getPromptUuid())
                .modelId(jobDefinitionDto.getModelId())
                .status(JobStatus.PENDING_VALIDATION)
                .completedTaskCount(0)
                .maxTokens(jobDefinitionDto.getMaxTokens())
                .temperature(jobDefinitionDto.getTemperature())
                .maxRecords(jobDefinitionDto.getMaxRecords())
                .startRecordNumber(jobDefinitionDto.getStartRecordNumber())
                .createdAt(now)
                .updatedAt(now)
                .build();
                
        jobRepository.save(job);
        
        // Log the outputFieldUuids parameter to check if it's null or empty
        log.info("Job {} submitted with outputFieldUuids: {}", jobUuid, jobDefinitionDto.getOutputFieldUuids());
        
        // Save selected output fields if provided
        if (jobDefinitionDto.getOutputFieldUuids() != null && !jobDefinitionDto.getOutputFieldUuids().isEmpty()) {
            int fieldOrder = 0;
            for (UUID fieldUuid : jobDefinitionDto.getOutputFieldUuids()) {
                JobOutputField outputField = JobOutputField.builder()
                        .jobOutputFieldUuid(UUID.randomUUID())
                        .job(job)
                        .fieldOrder(fieldOrder++)
                        .fieldUuid(fieldUuid)
                        .build();
                jobOutputFieldRepository.save(outputField);
            }
            log.info("Saved {} output fields for job {}", jobDefinitionDto.getOutputFieldUuids().size(), jobUuid);
        } else {
            log.info("No output fields specified for job {}, will include all fields", jobUuid);
        }


        /*
         * The job tasks are created in the job validation step. This is so that we don't have to 
         * query the file twice - once to build the tasks and later to validate the job.
         */

        JobValidationMessage validationMessage = JobValidationMessage.builder()
                .jobUuid(job.getJobUuid())
                .build();

                // Register a callback to be executed after the transaction is successfully committed
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Send the validation message
                messageProducer.sendJobValidation(validationMessage);
                log.info("Sent job validation message for job {} after transaction commit", job.getJobUuid());
            }
        });
        
        log.info("Job {} submitted for file {} and prompt {}", jobUuid, jobDefinitionDto.getFileUuid(), jobDefinitionDto.getPromptUuid());
        
        return job;
    }

    /**
     * Submit a job for processing
     * 
     * @param jobUuid The job to be submitted
     */

    @Transactional
    public Job submitJob(UUID jobUuid) {
        Job job = jobRepository.findById(jobUuid).orElse(null);
        if (job == null) {
            throw new JobSubmissionException("Job not found: " + jobUuid);
        }
        
        // Check if the job is already submitted
        if (job.getStatus() != JobStatus.VALIDATED) {
            throw new JobSubmissionException("Job is not in VALIDATED status: " + job.getStatus());
        }
        
        // Retrieve the tasks for the job
        List<JobTask> tasks = jobTaskRepository.findByJobUuid(jobUuid);

        // Update the job status to SUBMITTED
        job.setStatus(JobStatus.SUBMITTED);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
        jobNotificationService.sendJobUpdateNotification(job);
        
        // Submit all tasks (no status filter)
        int tasksSubmitted = submitJobTasks(job, tasks, null);

        PromptJobInfoDto promptJobInfo = PromptJobInfoDto.builder()
                .jobRunCountIncrement(1)
                .lastJobRunTimestamp(job.getUpdatedAt())
                .build();

        try {
            promptClient.updateJobInfo(job.getPromptUuid(), promptJobInfo, null);
        } catch (Exception e) {
            log.error("Failed to update job info for prompt {}: {}", job.getPromptUuid(), e.getMessage());
            // We can still continue processing the job even if updating prompt info fails
        }
        
        log.info("Job {} submitted for processing with {} tasks", jobUuid, tasksSubmitted);
        return job;
    }

    /**
     * Cancel a job
     * 
     * @param jobUuid The UUID of the job to cancel
     * @return The updated job
     */
    @Transactional
    public Job cancelJob(UUID jobUuid) {

        Job job = jobRepository.findById(jobUuid).orElseThrow(() -> new JobSubmissionException("Job not found: " + jobUuid));

        switch (job.getStatus()) {
            case SUBMITTED:
            case PROCESSING:
            case VALIDATING:
            case PENDING_VALIDATION:
            case INSUFFICIENT_CREDITS:
                // These statuses can be cancelled
                break;

            case CANCELLED:
            case COMPLETED:
            case FAILED:
                // Already in a terminal state, cannot cancel
                throw new JobSubmissionException("Job is already in terminal state: " + job.getStatus());

            default:
                // Other statuses cannot be cancelled
                throw new JobSubmissionException("Job cannot be cancelled from status: " + job.getStatus());
        }
        
        // Update the job status to CANCELLED
        job = updateJobStatus(jobUuid, JobStatus.CANCELLED, null);
        
        log.info("Job {} cancelled", jobUuid);
        return job;
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
                    
                    // Exponential backoff to reduce contention
                    try {
                        // Base delay of 50ms with exponential increase and some randomness
                        long delay = (long) (50 * Math.pow(1.5, retryCount) + Math.random() * 50);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Thread interrupted during retry delay for job {}", jobUuid);
                        return; // Exit on interruption
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
        
        // Get the total number of tasks for this job
        Long totalTaskCount = jobTaskRepository.countByJobUuid(jobUuid);
        if (totalTaskCount == null || totalTaskCount == 0) {
            log.error("No tasks found for job: {}", jobUuid);
            return;
        }
        
        // Get the counts by status in a single query instead of loading all tasks
        List<TaskStatusCount> statusCounts = jobTaskRepository.countTasksByStatus(jobUuid);
        
        int completedCount = 0;
        int failedCount = 0;
        int insufficientCreditsCount = 0;
        double creditsUsed = 0.0;
        // Process the counts from the query results
        for (TaskStatusCount statusCount : statusCounts) {
            if (statusCount.getStatus() == TaskStatus.COMPLETED) {
                completedCount = statusCount.getCount().intValue();
            } else if (statusCount.getStatus() == TaskStatus.FAILED) {
                failedCount = statusCount.getCount().intValue();
            } else if (statusCount.getStatus() == TaskStatus.INSUFFICIENT_CREDITS) {
                insufficientCreditsCount = statusCount.getCount().intValue();
            }
            if (statusCount.getCreditsUsed() != null) {
                creditsUsed += statusCount.getCreditsUsed();
            }
        }
            
        // We only update the count if it has changed
        int completedTaskCount = completedCount + failedCount;
        boolean countsChanged = job.getCompletedTaskCount() != completedTaskCount; 
        if (countsChanged) {
            job.setCompletedTaskCount(completedTaskCount);
        }

        boolean creditsChanged = false;
        if (job.getCreditUsage() == null) {
            job.setCreditUsage(creditsUsed);
            creditsChanged = true;
        } else if (job.getCreditUsage() != creditsUsed) {
            job.setCreditUsage(creditsUsed);
            creditsChanged = true;
        }        
        
        // Re-read job status to ensure we're working with the most current state
        // This helps prevent race conditions where another process changed the status
        // since we loaded the job
        JobStatus currentStatus = job.getStatus();
        
        JobStatus newStatus = null;
        
        // Only change status if in a legal state for automatic transitions
        // This prevents overriding manual status changes like CANCELLED
        boolean isEligibleForAutoUpdate = 
            currentStatus == JobStatus.SUBMITTED || 
            currentStatus == JobStatus.PROCESSING || 
            currentStatus == JobStatus.VALIDATING || 
            currentStatus == JobStatus.PENDING_VALIDATION;
            
        if (isEligibleForAutoUpdate) {
            // Check for insufficient credits first - if any task has insufficient credits, mark the whole job
            if (insufficientCreditsCount > 0) {
                newStatus = JobStatus.INSUFFICIENT_CREDITS;
            }
            // If no insufficient credits but all tasks are done, mark as pending output
            else if (completedTaskCount == totalTaskCount) {
                // All tasks are completed or failed, update to PENDING_OUTPUT
                newStatus = JobStatus.PENDING_OUTPUT;
            } else if (currentStatus == JobStatus.SUBMITTED) {
                // First task started processing
                newStatus = JobStatus.PROCESSING;
            }
        }
        
        // Only save if there are actual changes to make
        if (countsChanged || creditsChanged || (newStatus != null && job.getStatus() != newStatus)) {
            if (newStatus != null) {
                job.setStatus(newStatus);
            }
            job.setUpdatedAt(LocalDateTime.now());
            
            try {
                job = jobRepository.save(job);
                jobNotificationService.sendJobUpdateNotification(job);
                
                // If all tasks completed and we're not in INSUFFICIENT_CREDITS, queue for output processing
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
                } else if (newStatus == JobStatus.INSUFFICIENT_CREDITS) {
                    log.warn("Job {} marked as INSUFFICIENT_CREDITS ({} tasks with insufficient credits)", 
                            jobUuid, insufficientCreditsCount);
                } else if (countsChanged || newStatus != null) {
                    log.info("Updated job {} status to {}, completed tasks: {}/{}", 
                        jobUuid, job.getStatus(), job.getCompletedTaskCount(), totalTaskCount);
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                // Explicitly throw this to be handled by the retry mechanism in updateJobStatus
                log.debug("Optimistic locking failure while saving job {}", jobUuid);
                throw e;
            }
        } else {
            log.debug("No changes to save for job {}", jobUuid);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job updateJobStatus(UUID jobUuid, JobStatus status, Consumer<Job> jobMutator) {
        Job job = jobRepository.findById(jobUuid).orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobUuid));
        job.setStatus(status);
        job.setUpdatedAt(LocalDateTime.now());
        if (jobMutator != null) {
            jobMutator.accept(job);
        }
        job = jobRepository.save(job);
        jobNotificationService.sendJobUpdateNotification(job);
        return job;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job updateJobStatus(UUID jobUuid, JobStatus status) {
        return updateJobStatus(jobUuid, status, null);
    }

    public void failJob(UUID jobUuid, String errorMessage) {
        Job job = updateJobStatus(jobUuid, JobStatus.FAILED, j -> {
            j.setErrorMessage(errorMessage);
        });
        log.error("Job {} failed: {}", job.getJobUuid(), errorMessage);

    }

    /**
     * Check if a file or prompt has any active jobs (not in terminal states)
     * 
     * @param fileUuid The UUID of the file to check (can be null if checking by prompt)
     * @param promptUuid The UUID of the prompt to check (can be null if checking by file)
     * @return true if there are active jobs for this file or prompt
     */
    public boolean hasActiveJobs(UUID fileUuid, UUID promptUuid) {
        List<Job> jobs;
        
        if (fileUuid != null) {
            jobs = jobRepository.findByFileUuid(fileUuid);
        } else if (promptUuid != null) {
            jobs = jobRepository.findByPromptUuid(promptUuid);
        } else {
            throw new IllegalArgumentException("Either fileUuid or promptUuid must be provided");
        }
        
        // Check if any jobs are not in terminal states
        return jobs.stream().anyMatch(job -> {
            JobStatus status = job.getStatus();
            return status != JobStatus.COMPLETED && 
                   status != JobStatus.FAILED && 
                   status != JobStatus.CANCELLED &&
                   status != JobStatus.COMPLETED_WITH_ERRORS;
        });
    }
    
    /**
     * Check if a file has any active jobs (not in terminal states)
     * 
     * @param fileUuid The UUID of the file to check
     * @return true if there are active jobs for this file
     */
    public boolean hasActiveJobs(UUID fileUuid) {
        return hasActiveJobs(fileUuid, null);
    }

    /**
     * Delete a job and all its associated tasks
     * 
     * @param jobUuid The UUID of the job to delete
     */
    @Transactional
    public void deleteJob(UUID jobUuid) {
        Job job = jobRepository.findById(jobUuid).orElseThrow(() -> new JobSubmissionException("Job not found: " + jobUuid));
        
        // Check if the job can be deleted - only allow deletion of completed, failed, or cancelled jobs
        if (job.getStatus() == JobStatus.PROCESSING || 
            job.getStatus() == JobStatus.SUBMITTED || 
            job.getStatus() == JobStatus.VALIDATING || 
            job.getStatus() == JobStatus.PENDING_VALIDATION) {
            throw new JobSubmissionException("Cannot delete job in status: " + job.getStatus() + ". Only completed, failed, or cancelled jobs can be deleted.");
        }
        
        // Delete all job tasks first (due to foreign key constraints)
        jobTaskRepository.deleteByJobUuid(jobUuid);
        
        // Delete all job output fields
        jobOutputFieldRepository.deleteByJob(job);
        
        // Delete the job
        jobRepository.delete(job);
        
        log.info("Job {} and all associated tasks deleted", jobUuid);
    }

    /**
     * Continue a job that was previously cancelled or has insufficient credits
     * 
     * @param jobUuid The UUID of the job to continue
     * @return The updated job
     */
    @Transactional
    public Job continueJob(UUID jobUuid) {
        Job job = jobRepository.findById(jobUuid).orElse(null);
        if (job == null) {
            throw new JobSubmissionException("Job not found: " + jobUuid);
        }
        
        // Check if the job is in a state that can be continued
        if (job.getStatus() != JobStatus.CANCELLED && job.getStatus() != JobStatus.INSUFFICIENT_CREDITS) {
            throw new JobSubmissionException("Job is not in CANCELLED or INSUFFICIENT_CREDITS status: " + job.getStatus());
        }
        
        // Retrieve the tasks for the job
        List<JobTask> tasks = jobTaskRepository.findByJobUuid(jobUuid);

        // Update the job status to SUBMITTED
        job.setStatus(JobStatus.SUBMITTED);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
        jobNotificationService.sendJobUpdateNotification(job);
        
        // Create a list of task statuses to resubmit
        List<TaskStatus> statusesToResubmit = List.of(TaskStatus.CANCELLED, TaskStatus.SUBMITTED, TaskStatus.INSUFFICIENT_CREDITS);
        
        // Submit the job tasks that match the statuses to be resubmitted
        int tasksSubmitted = submitJobTasks(job, tasks, statusesToResubmit);
        
        log.info("Job {} continued and submitted for processing with {} tasks resubmitted", jobUuid, tasksSubmitted);
        return job;
    }
    
    /**
     * Private helper method to submit tasks for a job
     * 
     * @param job The job to submit tasks for
     * @param tasks The list of all tasks for the job
     * @param statuses The list of task statuses to consider for submission (null or empty list for all)
     * @return The number of tasks submitted
     */
    private int submitJobTasks(Job job, List<JobTask> tasks, List<TaskStatus> statuses) {
        List<JobTaskMessage> messagesToSend = new ArrayList<>();

        for (JobTask task : tasks) {
            // Skip tasks that don't match the status filter (if provided)
            if (statuses != null && !statuses.isEmpty() && !statuses.contains(task.getStatus())) {
                continue;
            }
            
            // Create the message but don't send it yet
            JobTaskMessage message = JobTaskMessage.builder()
                    .jobTaskUuid(task.getJobTaskUuid())
                    .jobUuid(task.getJobUuid())
                    .userId(job.getUserId())
                    .fileRecordUuid(task.getFileRecordUuid())
                    .modelId(task.getModelId())
                    .promptUuid(job.getPromptUuid())
                    .maxTokens(job.getMaxTokens())
                    .temperature(job.getTemperature())
                    .build();
                    
            messagesToSend.add(message);
            
            // Update task status to SUBMITTED
            if (task.getStatus() != TaskStatus.SUBMITTED) {
                task.setStatus(TaskStatus.SUBMITTED);
                jobTaskRepository.save(task);
            }
        }

        // Register a callback to be executed after the transaction is successfully committed
        final int messageCount = messagesToSend.size();
        if (messageCount > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Now that the transaction has committed, send all messages
                    for (JobTaskMessage message : messagesToSend) {
                        messageProducer.sendJobTask(message);
                    }
                    log.info("Sent {} job task messages for job {} after transaction commit", messageCount, job.getJobUuid());
                }
            });
        }
        
        return messageCount;
    }
}