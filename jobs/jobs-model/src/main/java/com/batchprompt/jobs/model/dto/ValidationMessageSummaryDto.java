package com.batchprompt.jobs.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a summary of validation messages with counts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationMessageSummaryDto {
    /**
     * The validation message content
     */
    private String message;
    
    /**
     * Number of times this message appears for the job
     */
    private Long count;
}
