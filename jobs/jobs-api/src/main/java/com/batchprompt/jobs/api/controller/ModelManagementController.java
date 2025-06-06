package com.batchprompt.jobs.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.jobs.core.service.JobPricingService;
import com.batchprompt.jobs.core.service.ModelService;
import com.batchprompt.jobs.model.dto.ModelCreditUsageRequestDto;
import com.batchprompt.jobs.model.dto.ModelDto;
import com.batchprompt.jobs.model.dto.ModelProviderDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for managing model providers and models
 */
@RestController
@RequestMapping("/api/model-management")
@RequiredArgsConstructor
@Slf4j
public class ModelManagementController {

    private final ModelService modelService;
    private final JobPricingService jobPricingService;    
    /**
     * Get a list of all supported models
     */
    @GetMapping("/models")
    public ResponseEntity<List<ModelDto>> getAllModels() {
        return ResponseEntity.ok(modelService.getSupportedModelDetails());
    }
    
    /**
     * Get a list of all model providers
     */
    @GetMapping("/providers")
    public ResponseEntity<List<ModelProviderDto>> getAllProviders() {
        return ResponseEntity.ok(modelService.getAllProviders());
    }
    
    /**
     * Get enabled model providers
     */
    @GetMapping("/providers/enabled")
    public ResponseEntity<List<ModelProviderDto>> getEnabledProviders() {
        return ResponseEntity.ok(modelService.getEnabledProviders());
    }
    /**
     * Force refresh of models from database
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> refreshModels() {
        modelService.refreshModels();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/credit-usage")
    public ResponseEntity<Map<String,Double>> calculateCreditUsage(
        @RequestBody @Valid ModelCreditUsageRequestDto modelCreditUsageRequestDto
    ) {
        HashMap<String, Double> modelResults = new HashMap<>();
        for (ModelDto model : modelService.getSupportedModelDetails()) {
            Double result = jobPricingService.calculateCreditUsage(
                    model.getModelId(),
                    modelCreditUsageRequestDto.getInputTokens(),
                    modelCreditUsageRequestDto.getOutputTokens(),
                    modelCreditUsageRequestDto.getThinkingTokens());
            if (result != null) {
                modelResults.put(model.getModelId(), result);
            } else {
                log.warn("Could not calculate cost for model {}", model.getModelId());
            }
        }
        return ResponseEntity.ok(modelResults);
    }
}