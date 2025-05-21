package com.batchprompt.jobs.core.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.model.dto.JobDto;
import com.batchprompt.jobs.model.dto.JobTaskDto;
import com.batchprompt.prompts.client.PromptClient;
import com.batchprompt.prompts.model.dto.PromptDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JobMapper {
    private final PromptClient promptClient;

    public JobDto toDto(Job job, PromptDto promptDto) {
        if (promptDto == null) {
            // Fetch the prompt from the service or repository
            // This is a placeholder; replace with actual fetching logic
            promptDto = promptClient.getPrompt(job.getPromptUuid(), null);
        }
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
                .maxTokens(job.getMaxTokens())
                .temperature(job.getTemperature())
                .maxRecords(job.getMaxRecords())
                .startRecordNumber(job.getStartRecordNumber())
                .creditUsage(job.getCreditUsage())
                .creditEstimate(job.getCreditEstimate())
                .costEstimate(job.getCostEstimate())
                .promptName(promptDto.getName())
                .errorMessage(job.getErrorMessage())
                .build();
    }

    public JobDto toDto(Job job) {
        return toDto(job, null);
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
                .creditUsage(jobTask.getCreditUsage())
                .creditEstimate(jobTask.getCreditEstimate())
                .costEstimate(jobTask.getCostEstimate())
                .thinkingTokens(jobTask.getThinkingTokens())
                .promptText(jobTask.getPromptText())
                .build();
    }
    
    public List<JobTaskDto> toTaskDtoList(List<JobTask> jobTasks) {
        return jobTasks.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<JobDto> toDtoPage(Page<Job> jobsPage) {
        HashMap<UUID, PromptDto> promptCache = new HashMap<>();

        return jobsPage.map(job -> {
            PromptDto promptDto = promptCache.computeIfAbsent(job.getPromptUuid(), uuid -> {
                // Fetch the prompt from the service or repository
                // This is a placeholder; replace with actual fetching logic
                return promptClient.getPrompt(uuid, null);
            });
            return toDto(job, promptDto);
        });
    }
}