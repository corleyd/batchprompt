package com.batchprompt.jobs.core.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.batchprompt.jobs.core.model.ModelCreditRate;
import com.batchprompt.jobs.core.repository.ModelCreditRateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the CreditCalculationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCalculationServiceImpl implements CreditCalculationService {
    private static final Double DEFAULT_CREDITS_PER_USD = 1000.0;

    private final ModelCreditRateRepository modelCreditRateRepository;
    
    @Override
    public Double calculateCreditUsage(String modelId, Double costUsd, LocalDateTime timestamp) {
        if (modelId == null || costUsd == null || timestamp == null) {
            return null;
        }
        
        Double creditsPerUsd = getCreditsPerUsdRate(modelId, timestamp);
        if (creditsPerUsd == null) {
            log.warn("No applicable credit rate found for model {} at {}", modelId, timestamp);
            return null;
        }
        
        return costUsd * creditsPerUsd;
    }
    
    @Override
    public Double convertCreditsToUsd(String modelId, Double credits, LocalDateTime timestamp) {
        if (modelId == null || credits == null || timestamp == null) {
            return null;
        }
        
        Double creditsPerUsd = getCreditsPerUsdRate(modelId, timestamp);
        if (creditsPerUsd == null || creditsPerUsd == 0) {
            log.warn("No applicable credit rate found for model {} at {}", modelId, timestamp);
            return null;
        }
        
        return credits / creditsPerUsd;
    }
    
    @Override
    public Double getCreditsPerUsdRate(String modelId, LocalDateTime timestamp) {
        if (modelId == null || timestamp == null) {
            return null;
        }
        
        try {
            Optional<ModelCreditRate> rateOpt = modelCreditRateRepository.findApplicableRate(modelId, timestamp);
            
            if (rateOpt.isPresent()) {
                return rateOpt.get().getCreditsPerUsd();
            } else {
                log.debug("No credit rate found for model {} at {}. Using default of {}", modelId, timestamp, DEFAULT_CREDITS_PER_USD);
                return DEFAULT_CREDITS_PER_USD;
            }
        } catch (Exception e) {
            log.error("Error retrieving credit rate for model {} at {}: {}", modelId, timestamp, e.getMessage(), e);
            return null;
        }
    }
}