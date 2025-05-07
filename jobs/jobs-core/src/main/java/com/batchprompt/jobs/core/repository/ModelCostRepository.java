package com.batchprompt.jobs.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.ModelCost;

/**
 * Repository for accessing model cost data
 */
@Repository
public interface ModelCostRepository extends JpaRepository<ModelCost, UUID> {
    
    /**
     * Find the applicable costs for a model
     * Costs are considered applicable when:
     * - model_id matches the given model_id
     * - current timestamp is >= effective_begin_timestamp
     * - current timestamp is < effective_end_timestamp OR effective_end_timestamp is null
     * 
     * @param modelId The model ID to find costs for
     * @return A list of ModelCost entities that are currently effective for the model
     */
    @Query("SELECT mc FROM ModelCost mc WHERE mc.modelId = :modelId " +
           "AND mc.effectiveBeginTimestamp <= CURRENT_TIMESTAMP " +
           "AND (mc.effectiveEndTimestamp IS NULL OR mc.effectiveEndTimestamp > CURRENT_TIMESTAMP) " +
           "ORDER BY mc.effectiveBeginTimestamp DESC")
    List<ModelCost> findCurrentCostsForModel(@Param("modelId") String modelId);
}