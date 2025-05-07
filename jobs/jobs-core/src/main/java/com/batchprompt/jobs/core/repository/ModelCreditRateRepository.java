package com.batchprompt.jobs.core.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.ModelCreditRate;

/**
 * Repository for model credit rate data
 */
@Repository
public interface ModelCreditRateRepository extends JpaRepository<ModelCreditRate, UUID> {
    
    /**
     * Find the applicable credit rate for a model at a specific point in time
     * 
     * @param modelId The model ID to find the rate for
     * @param timestamp The timestamp when the rate applies
     * @return An optional containing the applicable rate if found
     */
    @Query("SELECT mcr FROM ModelCreditRate mcr " +
           "WHERE mcr.modelId = :modelId " +
           "AND mcr.effectiveBeginTimestamp <= :timestamp " +
           "AND (mcr.effectiveEndTimestamp IS NULL OR mcr.effectiveEndTimestamp > :timestamp) " +
           "AND mcr.deleteTimestamp IS NULL " +
           "ORDER BY mcr.effectiveBeginTimestamp DESC")
    Optional<ModelCreditRate> findApplicableRate(@Param("modelId") String modelId, 
                                               @Param("timestamp") LocalDateTime timestamp);

}