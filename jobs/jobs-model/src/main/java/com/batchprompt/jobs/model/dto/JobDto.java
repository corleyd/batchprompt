package com.batchprompt.jobs.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.batchprompt.jobs.model.JobStatus;

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
    private String fileName;
    private UUID promptUuid;
    private String promptName;
    private String modelName;
    private JobStatus status;
    private Integer taskCount;
    private UUID resultFileUuid;
    private Integer completedTaskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}