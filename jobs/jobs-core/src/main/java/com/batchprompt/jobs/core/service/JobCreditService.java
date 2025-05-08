package com.batchprompt.jobs.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.JobRepository;
import com.batchprompt.jobs.core.repository.JobTaskRepository;
import com.batchprompt.users.client.AccountClient;
import com.batchprompt.users.model.dto.AccountDto;

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
    private final AccountClient accountClient;
    
    /**
     * Check if a user has sufficient credits for a task
     * 
     * @param userId The ID of the user to check
     * @return true if the user has sufficient credits, false otherwise
     */
    public boolean checkUserHasSufficientCredits(UUID userUuid) {
        if (userUuid == null) {
            log.error("User UUID is null when checking for sufficient credits");
            return false;
        }
        
        try {
            // Get all accounts for the user
            ResponseEntity<List<AccountDto>> accountsResponse = accountClient.getUserAccountsByUserId(userUuid);
            if (!accountsResponse.getStatusCode().is2xxSuccessful() || accountsResponse.getBody() == null || accountsResponse.getBody().isEmpty()) {
                log.error("No accounts found for user UUID: {}", userUuid);
                return false;
            }
            
            // Check each account for available credits
            boolean hasSufficientCredits = false;
            for (AccountDto account : accountsResponse.getBody()) {
                ResponseEntity<Integer> balanceResponse = accountClient.getAccountBalance(account.getAccountUuid());
                if (balanceResponse.getStatusCode().is2xxSuccessful() && balanceResponse.getBody() != null) {
                    int balance = balanceResponse.getBody();
                    if (balance > 0) {
                        hasSufficientCredits = true;
                        log.debug("Account {} has sufficient credits: {}", account.getAccountUuid(), balance);
                        break;
                    }
                }
            }
            
            if (!hasSufficientCredits) {
                log.warn("User {} has insufficient credits available on all accounts", userUuid);
            }
            
            return hasSufficientCredits;
        } catch (Exception e) {
            log.error("Error checking available credits for user {}: {}", userUuid, e.getMessage(), e);
            return false;
        }
    }
    
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

    /**
     * Get user accounts by UUID
     * 
     * @param userId The id of the user
     * @return List of account DTOs if found, empty list otherwise
     */
    public List<AccountDto> getUserAccounts(UUID userUuid) {
        if (userUuid == null) {
            log.error("User UUID is null when retrieving user accounts");
            return List.of();
        }
        
        try {
            ResponseEntity<List<AccountDto>> response = accountClient.getUserAccountsByUserId(userUuid);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Could not retrieve accounts for user: {}", userUuid);
                return List.of();
            }
        } catch (Exception e) {
            log.error("Error retrieving accounts for user {}: {}", userUuid, e.getMessage(), e);
            return List.of();
        }
    }
}