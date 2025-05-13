package com.batchprompt.jobs.model.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDefinitionDto {
    @NotNull
    private UUID fileUuid;
    
    @NotNull
    private UUID promptUuid;
    
    @NotNull
    private String modelId;
    
    private List<UUID> outputFieldUuids;
    
    // Optional parameters for job processing
    private Integer maxTokens;
    private Double temperature;
    private Integer maxRecords;
    private Integer startRecordNumber;
    
    // Target user ID when submitting a job on behalf of another user
    private String targetUserId;
}