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
 * Entity representing credit rate information for a model
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "model_credit_rate")
public class ModelCreditRate {
    
    @Id
    @Column(name = "model_credit_rate_uuid")
    private UUID modelCreditRateUuid;
    
    @Column(name = "model_id", nullable = false)
    private String modelId;
    
    @Column(name = "credits_per_usd", nullable = false)
    private Double creditsPerUsd;
    
    @Column(name = "effective_begin_timestamp", nullable = false)
    private LocalDateTime effectiveBeginTimestamp;
    
    @Column(name = "effective_end_timestamp")
    private LocalDateTime effectiveEndTimestamp;
    
    @Column(name = "create_timestamp", nullable = false)
    private LocalDateTime createTimestamp;
    
    @Column(name = "delete_timestamp")
    private LocalDateTime deleteTimestamp;
}