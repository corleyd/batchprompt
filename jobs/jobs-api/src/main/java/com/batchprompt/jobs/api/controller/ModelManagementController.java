package com.batchprompt.jobs.api.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.jobs.core.model.ModelProviderEntity;
import com.batchprompt.jobs.core.repository.ModelProviderRepository;
import com.batchprompt.jobs.core.service.ModelService;
import com.batchprompt.jobs.model.dto.ModelDto;

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
    private final ModelProviderRepository providerRepository;
    
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
    public ResponseEntity<List<ModelProviderEntity>> getAllProviders() {
        return ResponseEntity.ok(modelService.getAllProviders());
    }
    
    /**
     * Get enabled model providers
     */
    @GetMapping("/providers/enabled")
    public ResponseEntity<List<ModelProviderEntity>> getEnabledProviders() {
        return ResponseEntity.ok(modelService.getEnabledProviders());
    }
    
    /**
     * Update a model provider (admin only)
     */
    @PutMapping("/providers/{modelProviderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModelProviderEntity> updateProvider(
            @PathVariable String modelProviderId,
            @RequestBody ModelProviderEntity provider,
            @AuthenticationPrincipal Jwt jwt) {
            
        Optional<ModelProviderEntity> existingProvider = providerRepository.findById(modelProviderId);
        
        if (!existingProvider.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        ModelProviderEntity existing = existingProvider.get();
        existing.setDisplayName(provider.getDisplayName());
        
        ModelProviderEntity updatedProvider = providerRepository.save(existing);
        
        // Refresh models to apply changes
        modelService.refreshModels();
        
        return ResponseEntity.ok(updatedProvider);
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
}