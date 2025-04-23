package com.batchprompt.jobs.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.batchprompt.jobs.dto.JobDto;
import com.batchprompt.jobs.dto.JobTaskDto;
import com.batchprompt.jobs.model.Job;
import com.batchprompt.jobs.model.JobTask;

@Component
public class JobMapper {

    public JobDto toDto(Job job) {
        return JobDto.builder()
                .jobUuid(job.getJobUuid())
                .userId(job.getUserId())
                .fileUuid(job.getFileUuid())
                .promptUuid(job.getPromptUuid())
                .modelName(job.getModelName())
                .status(job.getStatus())
                .taskCount(job.getTaskCount())
                .completedTaskCount(job.getCompletedTaskCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
    
    public JobTaskDto toDto(JobTask jobTask) {
        return JobTaskDto.builder()
                .jobTaskUuid(jobTask.getJobTaskUuid())
                .jobUuid(jobTask.getJobUuid())
                .fileRecordUuid(jobTask.getFileRecordUuid())
                .modelName(jobTask.getModelName())
                .status(jobTask.getStatus())
                .errorMessage(jobTask.getErrorMessage())
                .beginTimestamp(jobTask.getBeginTimestamp())
                .endTimestamp(jobTask.getEndTimestamp())
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