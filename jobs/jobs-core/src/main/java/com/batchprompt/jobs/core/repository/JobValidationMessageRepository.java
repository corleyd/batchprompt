package com.batchprompt.jobs.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.JobValidationResultMessage;

/**
 * Repository for JobValidationMessageEntity
 */
@Repository
public interface JobValidationMessageRepository extends JpaRepository<JobValidationResultMessage, UUID> {
    
    /**
     * Find all validation messages for a specific job
     *
     * @param jobUuid the UUID of the job
     * @return list of validation messages for the job
     */
    List<JobValidationResultMessage> findByJobUuid(UUID jobUuid);
    
    /**
     * Find all validation messages for a specific job task
     *
     * @param jobTaskUuid the UUID of the job task
     * @return list of validation messages for the job task
     */
    List<JobValidationResultMessage> findByJobTaskUuid(UUID jobTaskUuid);
    
    /**
     * Delete all validation messages for a specific job
     *
     * @param jobUuid the UUID of the job
     */
    void deleteByJobUuid(UUID jobUuid);
}
