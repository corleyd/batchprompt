package com.batchprompt.cli;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batchprompt.common.client.ClientException;
import com.batchprompt.files.client.FileClient;
import com.batchprompt.jobs.client.JobClient;
import com.batchprompt.jobs.client.ModelManagementClient;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.dto.JobDto;
import com.batchprompt.prompts.client.PromptClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommandHelperService {
    private final PromptClient promptClient;
    private final JobClient jobClient;
    private final FileClient fileClient;
    private final ModelManagementClient modelManagementClient;
    private final ObjectMapper objectMapper;
   
    protected PromptClient getPromptClient() {
        return promptClient;
    }
    protected JobClient getJobClient() {
        return jobClient;
    }
    protected FileClient getFileClient() {
        return fileClient;
    }
    protected ModelManagementClient getModelManagementClient() {
        return modelManagementClient;
    }

    public void output(Object object) {
        try {
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            System.out.println(jsonOutput);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing output", e);
        }
    }
    public JobDto waitForJobStatus(UUID jobUuid, JobStatus desiredStatus) throws ClientException {
        do {
            JobDto jobDto = jobClient.getJob(jobUuid, null);
            if (jobDto == null) {
                throw new IllegalArgumentException("Job not found with the provided UUID: " + jobUuid);
            }
            if (jobDto.getStatus() == desiredStatus) {
                return jobDto;
            }
            if (jobDto.getStatus().isTerminal()) {
                throw new IllegalStateException("Job reached unexpected terminal status: " + jobDto.getStatus());
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        } while (true);
    }
}
