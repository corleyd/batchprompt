package com.batchprompt.jobs.model;

/**
 * Enum representing the reason why the model stopped generating tokens
 */
public enum StopReason {
    /**
     * The model finished generating the response naturally
     */
    STOP,
    
    /**
     * The model reached the maximum token limit
     */
    MAX_TOKENS,
    
    /**
     * The model generated a tool call (function call)
     */
    TOOL_CALLS,
    
    /**
     * The model was stopped due to content filtering
     */
    CONTENT_FILTER,
    
    /**
     * The model finished generating because it reached a stop sequence
     */
    STOP_SEQUENCE,
    
    /**
     * Unknown or unspecified reason
     */
    UNKNOWN
}
