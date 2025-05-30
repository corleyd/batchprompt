package com.batchprompt.jobs.model;

public enum JobStatus {
    PENDING_VALIDATION, VALIDATING, VALIDATED, VALIDATION_FAILED, 
    SUBMITTED, PROCESSING, PENDING_OUTPUT, GENERATING_OUTPUT, 
    COMPLETED, COMPLETED_WITH_ERRORS, FAILED, INSUFFICIENT_CREDITS,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == COMPLETED_WITH_ERRORS || 
               this == FAILED || this == INSUFFICIENT_CREDITS || 
               this == CANCELLED;
    }

}
    