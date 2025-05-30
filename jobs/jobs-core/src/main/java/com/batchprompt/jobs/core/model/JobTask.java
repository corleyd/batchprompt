package com.batchprompt.jobs.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.batchprompt.jobs.model.TaskStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job_task")
public class JobTask {
    
   
    @Id
    @Column(name = "job_task_uuid")
    private UUID jobTaskUuid;
    
    @Column(name = "job_uuid", nullable = false)
    private UUID jobUuid;
    
    @Column(name = "file_record_uuid", nullable = false)
    private UUID fileRecordUuid;

    @Column(name = "record_number", nullable = false)
    private Integer recordNumber;
    
    @Column(name = "model_id", nullable = false)
    private String modelId;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "response_text")
    private String responseText;

    @Column(name = "begin_timestamp")
    private LocalDateTime beginTimestamp;
    
    @Column(name = "end_timestamp")
    private LocalDateTime endTimestamp;
    
    @Column(name = "estimated_prompt_tokens")
    private Integer estimatedPromptTokens;

    @Column(name = "estimated_completion_tokens")
    private Integer estimatedCompletionTokens;

    @Column(name = "estimated_thinking_tokens")
    private Integer estimatedThinkingTokens;
    
    @Column(name = "prompt_tokens")
    private Integer promptTokens;
    
    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "thinking_tokens")
    private Integer thinkingTokens;    
    
    @Column(name = "total_tokens")
    private Integer totalTokens;
    
    @Column(name = "calculated_cost_usd")
    private Double calculatedCostUsd;
    
    @Column(name = "credit_usage")
    private Double creditUsage;

    @Column(name = "cost_estimate")
    private Double costEstimate;

    @Column(name = "credit_estimate")
    private Double creditEstimate;

    @Column(name = "prompt_text")
    private String promptText;
}