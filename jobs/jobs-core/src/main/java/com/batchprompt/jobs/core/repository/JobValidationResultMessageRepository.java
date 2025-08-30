package com.batchprompt.jobs.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.JobValidationResultMessage;

/**
 * Repository for JobValidationMessageEntity
 */
@Repository
public interface JobValidationResultMessageRepository extends JpaRepository<JobValidationResultMessage, UUID> {
    
    /**
     * Find all validation messages for a specific job
     *
     * @param jobUuid the UUID of the job
     * @return list of validation messages for the job
     */
    List<JobValidationResultMessage> findByJobUuid(UUID jobUuid);
    
    /**
     * Get validation message summary with counts for a specific job
     *
     * @param jobUuid the UUID of the job
     * @return list of objects containing message and count, ordered by count desc
     */
    @Query("SELECT j.message as message, COUNT(j.message) as count " +
           "FROM JobValidationResultMessage j " + 
           "WHERE j.jobUuid = :jobUuid " +
           "GROUP BY j.message " +
           "ORDER BY COUNT(j.message) DESC")
    List<Object[]> getValidationMessageSummary(@Param("jobUuid") UUID jobUuid);
    
    /**
     * Delete all validation messages for a specific job
     *
     * @param jobUuid the UUID of the job
     */
    void deleteByJobUuid(UUID jobUuid);
}
