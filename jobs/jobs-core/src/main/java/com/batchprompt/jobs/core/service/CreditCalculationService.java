package com.batchprompt.jobs.core.service;

import java.time.LocalDateTime;

/**
 * Service interface for handling credit calculations
 */
public interface CreditCalculationService {
    
    /**
     * Calculate credit usage from USD cost for a specific model at a specific time
     * 
     * @param modelId The model ID
     * @param costUsd The cost in USD
     * @param timestamp The timestamp when the calculation applies (usually job creation time)
     * @return The calculated credit usage, or null if no applicable credit rate found
     */
    Double calculateCreditUsage(String modelId, Double costUsd, LocalDateTime timestamp);
    
    /**
     * Convert credits to USD for a specific model at a specific time
     * 
     * @param modelId The model ID
     * @param credits The credits amount
     * @param timestamp The timestamp when the calculation applies
     * @return The calculated USD amount, or null if no applicable credit rate found
     */
    Double convertCreditsToUsd(String modelId, Double credits, LocalDateTime timestamp);
    
    /**
     * Get the credits per USD rate for a specific model at a specific time
     * 
     * @param modelId The model ID
     * @param timestamp The timestamp when the rate applies
     * @return The credits per USD rate, or null if no applicable rate found
     */
    Double getCreditsPerUsdRate(String modelId, LocalDateTime timestamp);
}