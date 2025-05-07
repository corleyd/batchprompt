package com.batchprompt.jobs.core.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key class for ModelCost entity
 * Consists of modelId and effectiveBeginTimestamp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelCostId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String modelId;
    private LocalDateTime effectiveBeginTimestamp;
}