package com.batchprompt.jobs.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message DTO for job output processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOutputMessage {
    
    private UUID jobUuid;
    private String userId;
    private boolean hasErrors;
}