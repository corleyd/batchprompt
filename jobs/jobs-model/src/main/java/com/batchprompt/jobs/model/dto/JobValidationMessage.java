package com.batchprompt.jobs.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message for job validation to be sent to the validation worker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobValidationMessage {
    /**
     * The UUID of the job to validate
     */
    private UUID jobUuid;

}
