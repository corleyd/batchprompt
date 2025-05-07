package com.batchprompt.jobs.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing pricing information for a model
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "model_cost")
public class ModelCost {
    
    @Id
    @Column(name = "model_cost_uuid")
    private UUID modelCostUuid;
    
    @Column(name = "model_id", nullable = false)
    private String modelId;
    
    @Column(name = "min_input_tokens")
    private Integer minInputTokens;
    
    @Column(name = "max_input_tokens")
    private Integer maxInputTokens;
    
    @Column(name = "input_token_1m_cost_usd", nullable = false)
    private Double inputToken1mCostUsd;
    
    @Column(name = "output_token_1m_cost_usd", nullable = false)
    private Double outputToken1mCostUsd;
    
    @Column(name = "thinking_token_1m_cost_usd", nullable = false)
    private Double thinkingToken1mCostUsd;
    
    @Column(name = "effective_begin_timestamp", nullable = false)
    private LocalDateTime effectiveBeginTimestamp;
    
    @Column(name = "effective_end_timestamp")
    private LocalDateTime effectiveEndTimestamp;
}