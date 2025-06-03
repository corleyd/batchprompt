package com.batchprompt.jobs.core.repository.dto;

import com.batchprompt.jobs.model.TaskStatus;

/**
 * DTO for storing task status counts from database queries
 */
public class TaskStatusCount {
    private TaskStatus status;
    private Long count;
    private Double creditsUsed;

    public TaskStatusCount(TaskStatus status, Long count, Double creditsUsed) {
        this.status = status;
        this.count = count;
        this.creditsUsed = creditsUsed;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Long getCount() {
        return count;
    }

    public Double getCreditsUsed() {
        return creditsUsed;
    }
}
