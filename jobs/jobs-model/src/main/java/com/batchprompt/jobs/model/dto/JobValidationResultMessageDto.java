package com.batchprompt.jobs.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for job validation result messages, capturing errors or issues found during job validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobValidationResultMessageDto {
    /**
     * The UUID of the validation message
     */
    private UUID jobValidationMessageUuid;
    
    /**
     * The UUID of the job this validation message is associated with
     */
    private UUID jobUuid;
    
    /**
     * The record number in the file where the validation issue was found, if applicable
     */
    private Integer recordNumber;
    
    /**
     * The name of the field where the validation issue was found, if applicable
     */
    private String fieldName;
    
    /**
     * The validation message describing the issue
     */
    private String message;
}
