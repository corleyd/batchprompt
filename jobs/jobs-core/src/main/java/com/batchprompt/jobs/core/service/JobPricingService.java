package com.batchprompt.jobs.core.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.model.ModelCost;
import com.batchprompt.jobs.core.repository.ModelCostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating pricing information for job tasks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobPricingService {

    private final ModelCostRepository modelCostRepository;
    private final CreditCalculationService creditCalculationService;

    /**
     * Calculate the cost of a job task based on its token usage and model pricing
     * 
     * @param jobTask The job task to calculate cost for
     * @return The calculated cost in USD, or null if cost cannot be calculated
     */
    public Double calculateCost(JobTask jobTask) {
        if (jobTask == null) {
            log.warn("Cannot calculate cost for null job task");
            return null;
        }

        // We need token counts to calculate cost
        if (jobTask.getPromptTokens() == null || jobTask.getCompletionTokens() == null) {
            log.debug("Cannot calculate cost for job task {} as token counts are missing", 
                    jobTask.getJobTaskUuid());
            return null;
        }

        // Delegate to the parameterized calculateCost method
        Double totalCost = calculateCost(
                jobTask.getModelId(),
                jobTask.getPromptTokens(),
                jobTask.getCompletionTokens(),
                jobTask.getThinkingTokens());

        if (totalCost != null) {
            // Add job task UUID to the log if available
            log.debug("Calculated cost for job task {}: ${}", jobTask.getJobTaskUuid(), totalCost);
            
            // Calculate and set credit usage for this task
            calculateAndSetCreditUsage(jobTask, totalCost);
        }

        return totalCost;
    }

    /**
     * Calculate the cost based on model ID and token counts
     * 
     * @param modelId The ID of the model used
     * @param promptTokens The number of input/prompt tokens
     * @param completionTokens The number of output/completion tokens
     * @param thinkingTokens The number of thinking tokens (optional)
     * @return The calculated cost in USD, or null if cost cannot be calculated
     */
    public Double calculateCost(String modelId, Integer promptTokens, Integer completionTokens, Integer thinkingTokens) {
        if (modelId == null || promptTokens == null || completionTokens == null) {
            log.warn("Cannot calculate cost: model ID or token counts are null");
            return null;
        }

        // Find current pricing for the model
        List<ModelCost> modelCosts = modelCostRepository.findCurrentCostsForModel(modelId);
        if (modelCosts.isEmpty()) {
            log.warn("No pricing information found for model: {}", modelId);
            return null;
        }

        // Find the applicable model cost based on input token count
        ModelCost applicableModelCost = findApplicableModelCost(modelCosts, promptTokens);
        if (applicableModelCost == null) {
            log.warn("No applicable pricing tier found for model: {} with {} input tokens", 
                    modelId, promptTokens);
            return null;
        }

        // Calculate cost based on the token usage and pricing rates
        double inputCost = calculateTokenTypeCost(
                promptTokens, 
                applicableModelCost.getInputToken1mCostUsd());
        
        double outputCost = calculateTokenTypeCost(
                completionTokens,
                applicableModelCost.getOutputToken1mCostUsd());
        
        double thinkingCost = 0.0;
        if (thinkingTokens != null && thinkingTokens > 0) {
            thinkingCost = calculateTokenTypeCost(
                    thinkingTokens,
                    applicableModelCost.getThinkingToken1mCostUsd());
        }

        double totalCost = inputCost + outputCost + thinkingCost;

        // Log detailed cost breakdown for debugging
        log.debug("Cost calculation: model: {}, input tokens: {} (${} per 1M) = ${}, " +
                "output tokens: {} (${} per 1M) = ${}, thinking tokens: {} (${} per 1M) = ${}, " +
                "total cost: ${}",
                modelId,
                promptTokens, applicableModelCost.getInputToken1mCostUsd(), inputCost,
                completionTokens, applicableModelCost.getOutputToken1mCostUsd(), outputCost,
                thinkingTokens, applicableModelCost.getThinkingToken1mCostUsd(), thinkingCost,
                totalCost);

        return totalCost;
    }

    /**
     * Calculate the credit usage for a job task and set it on the task
     * 
     * @param jobTask The job task to calculate credit usage for
     * @param costUsd The cost in USD that needs to be converted to credits
     */
    public void calculateAndSetCreditUsage(JobTask jobTask, Double costUsd) {
        if (jobTask == null || costUsd == null) {
            return;
        }

        // Use the job begin timestamp or current time if not available
        LocalDateTime timestamp = jobTask.getBeginTimestamp();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        // Calculate credit usage based on the USD cost and applicable rate
        Double creditUsage = creditCalculationService.calculateCreditUsage(
                jobTask.getModelId(), costUsd, timestamp);
        
        if (creditUsage != null) {
            jobTask.setCreditUsage(creditUsage);
            log.debug("Calculated credit usage for job task {}: {} credits (from ${} USD)", 
                    jobTask.getJobTaskUuid(), creditUsage, costUsd);
        } else {
            log.warn("Could not calculate credit usage for job task {} (model: {})", 
                    jobTask.getJobTaskUuid(), jobTask.getModelId());
        }
    }

    public Double calculateCreditUsage(String modelId, int inputTokens, int outputTokens, int thinkingTokens) {
        if (modelId == null || inputTokens <= 0 || outputTokens <= 0) {
            log.warn("Cannot calculate credit usage: model ID or token counts are invalid");
            return null;
        }

        // Find current pricing for the model
        List<ModelCost> modelCosts = modelCostRepository.findCurrentCostsForModel(modelId);
        if (modelCosts.isEmpty()) {
            log.warn("No pricing information found for model: {}", modelId);
            return null;
        }

        // Find the applicable model cost based on input token count
        ModelCost applicableModelCost = findApplicableModelCost(modelCosts, inputTokens);
        if (applicableModelCost == null) {
            log.warn("No applicable pricing tier found for model: {} with {} input tokens", 
                    modelId, inputTokens);
            return null;
        }

        // Calculate cost in USD
        double totalCost = calculateCost(modelId, inputTokens, outputTokens, thinkingTokens);
        
        // Convert USD cost to credit usage
        Double creditUsage = creditCalculationService.calculateCreditUsage(
                modelId, totalCost, LocalDateTime.now());
        
        return creditUsage;
    }

    /**
     * Find the applicable model cost based on the input token count
     * 
     * @param modelCosts List of current model costs
     * @param inputTokens Number of input tokens
     * @return The applicable ModelCost or null if none found
     */
    private ModelCost findApplicableModelCost(List<ModelCost> modelCosts, Integer inputTokens) {
        for (ModelCost modelCost : modelCosts) {
            // Check if the input token count falls within the range
            // Null minInputTokens is treated as 0
            boolean minSatisfied = modelCost.getMinInputTokens() == null || 
                                  inputTokens >= modelCost.getMinInputTokens();
            
            // Null maxInputTokens is treated as infinity (always satisfied)
            boolean maxSatisfied = modelCost.getMaxInputTokens() == null || 
                                  inputTokens <= modelCost.getMaxInputTokens();
            
            if (minSatisfied && maxSatisfied) {
                return modelCost;
            }
        }
        return null;
    }

    /**
     * Calculate cost for a specific token type
     * 
     * @param tokenCount Number of tokens
     * @param costPer1M Cost per 1 million tokens in USD
     * @return Cost in USD
     */
    private double calculateTokenTypeCost(Integer tokenCount, Double costPer1M) {
        if (tokenCount == null || tokenCount <= 0 || costPer1M == null || costPer1M <= 0) {
            return 0.0;
        }
        
        // Convert rate from "per million tokens" to "per token" and multiply by the token count
        return (tokenCount * costPer1M) / 1_000_000.0;
    }
}