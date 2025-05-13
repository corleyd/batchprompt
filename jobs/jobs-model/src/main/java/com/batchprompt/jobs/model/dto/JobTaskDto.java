package com.batchprompt.jobs.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.batchprompt.jobs.model.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTaskDto {
    private UUID jobTaskUuid;
    private UUID jobUuid;
    private UUID fileRecordUuid;
    private int recordNumber;
    private String modelId;
    private TaskStatus status;
    private String responseText;
    private String errorMessage;
    private LocalDateTime beginTimestamp;
    private LocalDateTime endTimestamp;
    private Integer estimatedPromptTokens;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer thinkingTokens;
    private Integer totalTokens;
    private Double calculatedCostUsd;
    private Double creditUsage;
    private Double creditEstimate;
}