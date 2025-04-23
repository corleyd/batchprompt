package com.batchprompt.jobs.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.batchprompt.jobs.model.JobTask;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTaskDto {
    private UUID jobTaskUuid;
    private UUID jobUuid;
    private UUID fileRecordUuid;
    private String modelName;
    private JobTask.Status status;
    private String responseText;
    private String errorMessage;
    private LocalDateTime beginTimestamp;
    private LocalDateTime endTimestamp;
}