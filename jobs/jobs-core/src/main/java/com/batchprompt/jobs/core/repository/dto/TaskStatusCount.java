package com.batchprompt.jobs.core.repository.dto;

import com.batchprompt.jobs.model.TaskStatus;

/**
 * DTO for storing task status counts from database queries
 */
public class TaskStatusCount {
    private TaskStatus status;
    private Long count;

    public TaskStatusCount(TaskStatus status, Long count) {
        this.status = status;
        this.count = count;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Long getCount() {
        return count;
    }
}
