package com.batchprompt.jobs.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.JobRepository;
import com.batchprompt.jobs.core.repository.JobTaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing job credit usage calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobCreditService {

    private final JobRepository jobRepository;
    private final JobTaskRepository jobTaskRepository;
    private final CreditCalculationService creditCalculationService;
    
    /**
     * Update the credit usage for a job by summing the credit usage from all its tasks
     * 
     * @param jobUuid The UUID of the job to update
     * @return true if the update was successful, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateJobCreditUsage(UUID jobUuid) {
        try {
            Job job = jobRepository.findById(jobUuid).orElse(null);
            if (job == null) {
                log.error("Job not found: {}", jobUuid);
                return false;
            }
            
            List<JobTask> tasks = jobTaskRepository.findByJobUuid(jobUuid);
            if (tasks.isEmpty()) {
                log.warn("No tasks found for job: {}", jobUuid);
                return false;
            }
            
            // Sum up credit usage from all tasks
            double totalCreditUsage = 0.0;
            int tasksWithCreditUsage = 0;
            
            for (JobTask task : tasks) {
                if (task.getCreditUsage() != null) {
                    totalCreditUsage += task.getCreditUsage();
                    tasksWithCreditUsage++;
                }
            }
            
            // Only update if we have at least one task with credit usage
            if (tasksWithCreditUsage > 0) {
                job.setCreditUsage(totalCreditUsage);
                job.setUpdatedAt(LocalDateTime.now());
                jobRepository.save(job);
                
                log.info("Updated credit usage for job {}: {} credits from {} tasks", 
                        jobUuid, totalCreditUsage, tasksWithCreditUsage);
                return true;
            } else {
                log.warn("No tasks with credit usage found for job: {}", jobUuid);
                return false;
            }
        } catch (Exception e) {
            log.error("Error updating credit usage for job {}: {}", jobUuid, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Calculate credit usage for a specified job using its USD cost
     * 
     * @param jobUuid The UUID of the job
     * @param costUsd The cost in USD
     * @return true if calculation was successful, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean calculateJobCreditUsage(UUID jobUuid, Double costUsd) {
        if (jobUuid == null || costUsd == null) {
            return false;
        }
        
        try {
            Job job = jobRepository.findById(jobUuid).orElse(null);
            if (job == null) {
                log.error("Job not found: {}", jobUuid);
                return false;
            }
            
            LocalDateTime timestamp = job.getCreatedAt();
            Double creditUsage = creditCalculationService.calculateCreditUsage(
                    job.getModelId(), costUsd, timestamp);
            
            if (creditUsage != null) {
                job.setCreditUsage(creditUsage);
                job.setUpdatedAt(LocalDateTime.now());
                jobRepository.save(job);
                
                log.info("Set credit usage for job {}: {} credits (from ${} USD)", 
                        jobUuid, creditUsage, costUsd);
                return true;
            } else {
                log.warn("Could not calculate credit usage for job {} (model: {})", 
                        jobUuid, job.getModelId());
                return false;
            }
        } catch (Exception e) {
            log.error("Error calculating credit usage for job {}: {}", jobUuid, e.getMessage(), e);
            return false;
        }
    }
}