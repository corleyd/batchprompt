package com.batchprompt.jobs.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.model.dto.JobDto;
import com.batchprompt.jobs.model.dto.JobTaskDto;

@Component
public class JobMapper {

    public JobDto toDto(Job job) {
        return JobDto.builder()
                .jobUuid(job.getJobUuid())
                .userId(job.getUserId())
                .fileUuid(job.getFileUuid())
                .fileName(job.getFileName())
                .resultFileUuid(job.getResultFileUuid())
                .promptUuid(job.getPromptUuid())
                .modelId(job.getModelId())
                .status(job.getStatus())
                .taskCount(job.getTaskCount())
                .completedTaskCount(job.getCompletedTaskCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                // Map the new optional fields
                .maxTokens(job.getMaxTokens())
                .temperature(job.getTemperature())
                .maxRecords(job.getMaxRecords())
                .startRecordNumber(job.getStartRecordNumber())
                .build();
    }
    
    public JobTaskDto toDto(JobTask jobTask) {
        return JobTaskDto.builder()
                .jobTaskUuid(jobTask.getJobTaskUuid())
                .jobUuid(jobTask.getJobUuid())
                .fileRecordUuid(jobTask.getFileRecordUuid())
                .recordNumber(jobTask.getRecordNumber())
                .modelId(jobTask.getModelId())
                .status(jobTask.getStatus())
                .responseText(jobTask.getResponseText())
                .errorMessage(jobTask.getErrorMessage())
                .beginTimestamp(jobTask.getBeginTimestamp())
                .endTimestamp(jobTask.getEndTimestamp())
                .estimatedPromptTokens(jobTask.getEstimatedPromptTokens())
                .promptTokens(jobTask.getPromptTokens())
                .completionTokens(jobTask.getCompletionTokens())
                .totalTokens(jobTask.getTotalTokens())
                .calculatedCostUsd(jobTask.getCalculatedCostUsd())
                .build();
    }
    
    public List<JobDto> toDtoList(List<Job> jobs) {
        return jobs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<JobTaskDto> toTaskDtoList(List<JobTask> jobTasks) {
        return jobTasks.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}