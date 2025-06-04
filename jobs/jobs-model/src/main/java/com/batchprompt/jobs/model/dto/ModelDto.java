package com.batchprompt.jobs.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Data Transfer Object for models available in the system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDto {
    private String modelId;
    private String displayName;
    private String modelProviderId;
    private String modelProviderModelId;
    private String modelProviderDisplayName;
    private JsonNode modelProviderProperties;
    private Integer modelProviderDisplayOrder;
    private String taskQueueName;
    private boolean simulateStructuredOutput;
}