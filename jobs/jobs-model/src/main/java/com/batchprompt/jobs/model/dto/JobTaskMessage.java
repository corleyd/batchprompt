package com.batchprompt.jobs.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTaskMessage {
    private UUID jobTaskUuid;
    private UUID jobUuid;
    private UUID fileRecordUuid;
    private String modelId;
    private UUID promptUuid;
    private String userId;
    private Integer maxTokens;
    private Double temperature;
}