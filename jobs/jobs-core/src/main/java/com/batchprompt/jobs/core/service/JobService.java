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
import com.batchprompt.jobs.core.model.JobOutputField;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.JobOutputFieldRepository;
import com.batchprompt.jobs.core.repository.JobRepository;
import com.batchprompt.jobs.core.repository.JobTaskRepository;
import com.batchprompt.jobs.core.specification.JobSpecification;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.TaskStatus;
import com.batchprompt.jobs.model.dto.JobDto;
import com.batchprompt.jobs.model.dto.JobOutputMessage;
import com.batchprompt.jobs.model.dto.JobSubmissionDto;
import com.batchprompt.jobs.model.dto.JobTaskMessage;
import com.batchprompt.prompts.client.PromptClient;
import com.batchprompt.prompts.model.dto.PromptDto;

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
     * Get paginated jobs with optional filtering by userId, modelId, and status
     * 
     * @param userId Optional user ID to filter by
     * @param modelId Optional model id to filter by
     * @param status Optional job status to filter by
     * @param pageable Pageable object containing pagination and sorting information
     * @return Page of filtered jobs
     */
    public Page<Job> getJobsPaginated(String userId, String modelId, JobStatus status, Pageable pageable) {
        return jobRepository.findAll(JobSpecification.withFilters(userId, modelId, status), pageable);
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
     * Submit a new job for processing
     * 
     * @param jobSubmissionDto The job submission data transfer object
     * @param userId The user ID submitting the job
     * @param authToken The authentication token for calling other services
     * @return The created job
     * @throws JobSubmissionException If there's an error during job submission
     */
    @Transactional
    public Job submitJob(JobSubmissionDto jobSubmissionDto, String userId, String authToken) {
        // Validate model
        if (!modelService.isModelSupported(jobSubmissionDto.getModelId())) {
            throw new JobSubmissionException("Unsupported model: " + jobSubmissionDto.getModelId());
        }
        
        // Validate file exists and belongs to the user
        FileDto file = fileClient.getFile(jobSubmissionDto.getFileUuid(), authToken);
        if (file == null) {
            throw new JobSubmissionException("File not found: " + jobSubmissionDto.getFileUuid());
        }
        
        if (!file.getUserId().equals(userId)) {
            throw new JobSubmissionException("File " + jobSubmissionDto.getFileUuid() + " does not belong to user " + userId);
        }
        
        // Validate file is ready
        if (file.getStatus() != FileStatus.READY) {
            throw new JobSubmissionException("File is not ready for processing. Current status: " + file.getStatus());
        }
        
        // Validate prompt exists and belongs to the user
        PromptDto prompt = promptClient.getPrompt(jobSubmissionDto.getPromptUuid(), authToken);
        if (prompt == null) {
            throw new JobSubmissionException("Prompt not found: " + jobSubmissionDto.getPromptUuid());
        }
        
        if (!prompt.getUserId().equals(userId)) {
            throw new JobSubmissionException("Prompt " + jobSubmissionDto.getPromptUuid() + " does not belong to user " + userId);
        }
        
        // Create job
        UUID jobUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        // Determine record range parameters
        int startRecordNumber = jobSubmissionDto.getStartRecordNumber() != null ? jobSubmissionDto.getStartRecordNumber() : 1;
        Integer maxRecords = jobSubmissionDto.getMaxRecords();
        
        // Get file record count to determine how many records to process
        int pageSize = 100; // Process records in batches of 100
        int startPage = (startRecordNumber - 1) / pageSize; // Calculate the starting page based on startRecordNumber
        
        // Retrieve first page to get total count and start processing
        Page<FileRecordDto> recordsPage = fileClient.getFileRecordsPaginated(
            jobSubmissionDto.getFileUuid(), 
            startPage, 
            pageSize, 
            "recordNumber", 
            "asc", 
            authToken
        );
        
        if (recordsPage == null || recordsPage.getTotalElements() == 0) {
            throw new JobSubmissionException("No records found for file: " + jobSubmissionDto.getFileUuid());
        }
        
        // Calculate total records to process based on maxRecords and starting position
        long totalRecords = recordsPage.getTotalElements();
        long recordsToProcess = maxRecords != null 
            ? Math.min(maxRecords, totalRecords - (startRecordNumber - 1)) 
            : (totalRecords - (startRecordNumber - 1));
            
        if (recordsToProcess <= 0) {
            throw new JobSubmissionException("No records to process based on startRecordNumber: " + startRecordNumber);
        }
        
        log.info("Processing {} records from file {} starting at record {}", recordsToProcess, jobSubmissionDto.getFileUuid(), startRecordNumber);
        
        Job job = Job.builder()
                .jobUuid(jobUuid)
                .userId(userId)
                .fileUuid(jobSubmissionDto.getFileUuid())
                .fileName(file.getFileName())
                .promptUuid(jobSubmissionDto.getPromptUuid())
                .modelId(jobSubmissionDto.getModelId())
                .status(JobStatus.SUBMITTED)
                .taskCount((int)recordsToProcess)
                .completedTaskCount(0)
                .maxTokens(jobSubmissionDto.getMaxTokens())
                .temperature(jobSubmissionDto.getTemperature())
                .maxRecords(jobSubmissionDto.getMaxRecords())
                .startRecordNumber(jobSubmissionDto.getStartRecordNumber())
                .createdAt(now)
                .updatedAt(now)
                .build();
                
        jobRepository.save(job);
        
        // Log the outputFieldUuids parameter to check if it's null or empty
        log.info("Job {} submitted with outputFieldUuids: {}", jobUuid, jobSubmissionDto.getOutputFieldUuids());
        
        // Save selected output fields if provided
        if (jobSubmissionDto.getOutputFieldUuids() != null && !jobSubmissionDto.getOutputFieldUuids().isEmpty()) {
            int fieldOrder = 0;
            for (UUID fieldUuid : jobSubmissionDto.getOutputFieldUuids()) {
                JobOutputField outputField = JobOutputField.builder()
                        .jobOutputFieldUuid(UUID.randomUUID())
                        .job(job)
                        .fieldOrder(fieldOrder++)
                        .fieldUuid(fieldUuid)
                        .build();
                jobOutputFieldRepository.save(outputField);
            }
            log.info("Saved {} output fields for job {}", jobSubmissionDto.getOutputFieldUuids().size(), jobUuid);
        } else {
            log.info("No output fields specified for job {}, will include all fields", jobUuid);
        }
        
        // Store messages to be sent after transaction commits
        final List<JobTaskMessage> messagesToSend = new ArrayList<>();
        
        // Calculate the actual offset within the page for the start record
        int recordOffset = startRecordNumber - 1 - (startPage * pageSize);
        int recordsInFirstBatch = (int) Math.min(recordsToProcess, recordsPage.getContent().size() - recordOffset);
        
        // Create job tasks for the first batch
        for (int i = 0; i < recordsInFirstBatch; i++) {
            FileRecordDto record = recordsPage.getContent().get(i + recordOffset);
            UUID jobTaskUuid = UUID.randomUUID();
            
            JobTask task = JobTask.builder()
                    .jobTaskUuid(jobTaskUuid)
                    .jobUuid(jobUuid)
                    .fileRecordUuid(record.getFileRecordUuid())
                    .recordNumber(record.getRecordNumber())
                    .modelId(jobSubmissionDto.getModelId())
                    .status(TaskStatus.SUBMITTED)
                    .build();
                    
            jobTaskRepository.save(task);
            
            // Create the message but don't send it yet
            JobTaskMessage message = JobTaskMessage.builder()
                    .jobTaskUuid(jobTaskUuid)
                    .jobUuid(jobUuid)
                    .userId(userId)
                    .fileRecordUuid(record.getFileRecordUuid())
                    .modelId(jobSubmissionDto.getModelId())
                    .promptUuid(jobSubmissionDto.getPromptUuid())
                    .authToken(authToken)
                    .maxTokens(job.getMaxTokens())
                    .temperature(job.getTemperature())
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
                
                // Process remaining pages after initial batch if there are more records to process
                if (recordsToProcess > recordsPage.getContent().size()) {
                    // The method will run in a separate transaction to avoid holding transaction too long
                    processBatchRecords(job, recordsPage.getNumberOfElements(), recordsToProcess, startPage + 1, 
                                       pageSize, userId, authToken);
                }
            }
        });
        
        log.info("Job {} submitted for file {} and prompt {}", jobUuid, jobSubmissionDto.getFileUuid(), jobSubmissionDto.getPromptUuid());
        
        return job;
    }
    
    /**
     * Process remaining file records in batches
     * 
     * @param job The job being processed
     * @param processedSoFar Number of records already processed
     * @param totalToProcess Total number of records to process
     * @param startPage The page number to start from (0-based)
     * @param pageSize Size of each batch
     * @param userId User ID for authorization
     * @param authToken Auth token for service calls
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatchRecords(Job job, long processedSoFar, long totalToProcess, 
                                  int startPage, int pageSize, String userId, String authToken) {
        log.info("Processing next batch for job {} - page {}, processed so far: {}/{}", 
                job.getJobUuid(), startPage, processedSoFar, totalToProcess);
        
        // Calculate how many more records to process
        long remainingToProcess = totalToProcess - processedSoFar;
        
        try {
            // Get the next batch of records
            Page<FileRecordDto> recordsPage = fileClient.getFileRecordsPaginated(
                job.getFileUuid(), 
                startPage, 
                pageSize, 
                "recordNumber", 
                "asc", 
                authToken
            );
            
            if (recordsPage == null || recordsPage.getContent().isEmpty()) {
                log.warn("No more records found for job {} at page {}", job.getJobUuid(), startPage);
                return;
            }
            
            // Store messages to be sent after transaction commits
            final List<JobTaskMessage> messagesToSend = new ArrayList<>();
            
            // Determine how many records to take from this page
            int recordsToTakeFromPage = (int) Math.min(remainingToProcess, recordsPage.getContent().size());
            
            // Create job tasks for this batch
            for (int i = 0; i < recordsToTakeFromPage; i++) {
                FileRecordDto record = recordsPage.getContent().get(i);
                UUID jobTaskUuid = UUID.randomUUID();
                
                JobTask task = JobTask.builder()
                        .jobTaskUuid(jobTaskUuid)
                        .jobUuid(job.getJobUuid())
                        .fileRecordUuid(record.getFileRecordUuid())
                        .recordNumber(record.getRecordNumber())
                        .modelId(job.getModelId())
                        .status(TaskStatus.SUBMITTED)
                        .build();
                        
                jobTaskRepository.save(task);
                
                // Create the message
                JobTaskMessage message = JobTaskMessage.builder()
                        .jobTaskUuid(jobTaskUuid)
                        .jobUuid(job.getJobUuid())
                        .fileRecordUuid(record.getFileRecordUuid())
                        .modelId(job.getModelId())
                        .promptUuid(job.getPromptUuid())
                        .userId(userId)
                        .authToken(authToken)
                        .maxTokens(job.getMaxTokens())
                        .temperature(job.getTemperature())
                        .build();
                        
                messagesToSend.add(message);
            }
            
            // Register a callback to be executed after the transaction is successfully committed
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Send all messages for this batch
                    for (JobTaskMessage message : messagesToSend) {
                        messageProducer.sendJobTask(message);
                    }
                    log.info("Sent {} job task messages for job {} batch {} after transaction commit", 
                             messagesToSend.size(), job.getJobUuid(), startPage);
                    
                    // Process remaining pages if needed
                    long newProcessedSoFar = processedSoFar + recordsToTakeFromPage;
                    if (newProcessedSoFar < totalToProcess) {
                        // Continue with the next page in a new transaction
                        processBatchRecords(job, newProcessedSoFar, totalToProcess, 
                                          startPage + 1, pageSize, userId, authToken);
                    } else {
                        log.info("Completed processing all records for job {}: {}/{}", 
                                 job.getJobUuid(), newProcessedSoFar, totalToProcess);
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("Error processing batch for job {}: {}", job.getJobUuid(), e.getMessage(), e);
        }
    }
    
    /**
     * Submit a new job for processing with specific output fields
     * 
     * @param fileUuid The UUID of the file to process
     * @param promptUuid The UUID of the prompt to use
     * @param modelId The ID of the model to use
     * @param outputFieldUuids List of field UUIDs to include in the output (can be null for all fields)
     * @param userId The user ID submitting the job
     * @param authToken The authentication token for calling other services
     * @return The created job
     * @throws JobSubmissionException If there's an error during job submission
     */
    @Transactional
    public Job submitJob(UUID fileUuid, UUID promptUuid, String modelId, List<UUID> outputFieldUuids, String userId, String authToken) {
        JobSubmissionDto jobSubmissionDto = JobSubmissionDto.builder()
                .fileUuid(fileUuid)
                .promptUuid(promptUuid)
                .modelId(modelId)
                .outputFieldUuids(outputFieldUuids)
                .build();
        
        return submitJob(jobSubmissionDto, userId, authToken);
    }
    
    /**
     * Submit a new job for processing with all fields included
     * 
     * @param fileUuid The UUID of the file to process
     * @param promptUuid The UUID of the prompt to use
     * @param modelId The ID of the model to use
     * @param userId The user ID submitting the job
     * @param authToken The authentication token for calling other services
     * @return The created job
     * @throws JobSubmissionException If there's an error during job submission
     */
    @Transactional
    public Job submitJob(UUID fileUuid, UUID promptUuid, String modelId, String userId, String authToken) {
        JobSubmissionDto jobSubmissionDto = JobSubmissionDto.builder()
                .fileUuid(fileUuid)
                .promptUuid(promptUuid)
                .modelId(modelId)
                .build();
        
        return submitJob(jobSubmissionDto, userId, authToken);
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
        int insufficientCreditsCount = 0;
        
        for (JobTask task : tasks) {
            if (task.getStatus() == TaskStatus.COMPLETED) {
                completedCount++;
            } else if (task.getStatus() == TaskStatus.FAILED) {
                failedCount++;
            } else if (task.getStatus() == TaskStatus.INSUFFICIENT_CREDITS) {
                insufficientCreditsCount++;
            }
        }
            
        // We only update the count if it has changed
        boolean countsChanged = job.getCompletedTaskCount() != (completedCount + failedCount + insufficientCreditsCount); 
        if (countsChanged) {
            job.setCompletedTaskCount(completedCount + failedCount + insufficientCreditsCount);
        }
        
        JobStatus newStatus = null;
        
        // Check for insufficient credits first - if any task has insufficient credits, mark the whole job
        if (insufficientCreditsCount > 0) {
            newStatus = JobStatus.INSUFFICIENT_CREDITS;
        }
        // If no insufficient credits but all tasks are done, mark as pending output
        else if (completedCount + failedCount + insufficientCreditsCount == tasks.size()) {
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
                    jobUuid, job.getStatus(), job.getCompletedTaskCount(), tasks.size());
            }
        }
    }

    public JobDto convertToDto(Job job) {
        var builder = JobDto.builder()
                .jobUuid(job.getJobUuid())
                .userId(job.getUserId())
                .fileUuid(job.getFileUuid())
                .fileName(job.getFileName())
                .resultFileUuid(job.getResultFileUuid())
                .promptUuid(job.getPromptUuid())
                .modelId(job.getModelId())
                .status(job.getStatus())
                .taskCount(job.getTaskCount())
                .completedTaskCount(job.getCompletedTaskCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt());

        PromptDto prompt = promptClient.getPrompt(job.getPromptUuid(), null);
        if (prompt != null) {
            builder.promptName(prompt.getName());
        }
        return builder.build();
    }
}