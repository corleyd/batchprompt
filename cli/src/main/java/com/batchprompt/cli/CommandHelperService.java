package com.batchprompt.cli;

import org.springframework.stereotype.Service;

import com.batchprompt.files.client.FileClient;
import com.batchprompt.jobs.client.JobClient;
import com.batchprompt.prompts.client.PromptClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommandHelperService {
    private final PromptClient promptClient;
    private final JobClient jobClient;
    private final FileClient fileClient;
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

    public void output(Object object) {
        try {
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            System.out.println(jsonOutput);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing output", e);
        }
    }
}
