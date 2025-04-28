package com.batchprompt.jobs.core.exception;

public class JobSubmissionException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public JobSubmissionException(String message) {
        super(message);
    }
    
    public JobSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}