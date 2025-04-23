package com.batchprompt.jobs.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.batchprompt.jobs.model.Job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private UUID jobUuid;
    private String userId;
    private UUID fileUuid;
    private UUID promptUuid;
    private String modelName;
    private Job.Status status;
    private Integer taskCount;
    private Integer completedTaskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}