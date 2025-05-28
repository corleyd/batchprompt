package com.batchprompt.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batchprompt.common.client.ClientException;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.dto.JobDefinitionDto;
import com.batchprompt.jobs.model.dto.JobDto;
import com.batchprompt.prompts.model.dto.PromptDto;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "job",
         description = "Manage and execute jobs in BatchPrompt",
         subcommands = { JobCommand.RunJobCommand.class },
         mixinStandardHelpOptions = true)
@RequiredArgsConstructor
public class JobCommand implements Runnable {

    @Override
    public void run() {
    }
    @Component
    @Command(name = "run", description = "Validate and submit a job", 
             mixinStandardHelpOptions = true)
    @RequiredArgsConstructor
    static class RunJobCommand implements Runnable {
        private final CommandHelperService commandHelperService;

        @Option(names = { "-p", "--prompt" }, description = "ID of the prompt to use for the job", required = true)
        private UUID promptUuid;

        @Option(names = { "-f", "--file" }, description = "ID of the file to process in the job", required = true)
        private UUID fileUuid;

        @Option(names = { "-m", "--model" }, description = "ID of the model to use for the job", required = true)
        private String modelId;

        @Option(names = { "-r", "--max-records" }, description = "Maximum number of records to process in the job")
        private Integer maxRecords;

        @Option(names = { "-t", "--max-tokens" }, description = "Maximum number of tokens to generate in the job")
        private Integer maxTokens;

        @Override
        public void run() {

            List<String> modelArray;
            if (modelId.toLowerCase().equals("all")) {
                try {
                    modelArray = commandHelperService.getJobClient()
                        .getSupportedModels(null)
                        .stream()
                        .map(model -> model.getModelId())
                        .toList();
                } catch (ClientException e) {
                    throw new RuntimeException("Failed to retrieve supported models", e);
                }
            } else {
                modelArray = List.of(modelId.split(","));
            } 

            PromptDto promptDto = commandHelperService.getPromptClient().getPrompt(promptUuid, null);
            List<Thread> threads = new ArrayList<>();

            for (String model : modelArray) {
                Thread thread = new Thread(() -> runJob(promptDto, fileUuid, model.trim(), maxRecords, maxTokens));
                thread.start();
                threads.add(thread);
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    throw new RuntimeException("Job execution interrupted", e);
                }
            }
        }

        private void runJob(PromptDto promptDto, UUID fileUuid, String modelId, Integer maxRecords, Integer maxTokens) {
            try {

                JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                        .targetUserId(promptDto.getUserId())
                        .promptUuid(promptUuid)
                        .fileUuid(fileUuid)
                        .modelId(modelId)
                        .maxRecords(maxRecords)
                        .maxTokens(maxTokens)
                        .build();

                JobDto jobDto = commandHelperService.getJobClient().validateJob(jobDefinition, null);

                jobDto = commandHelperService.waitForJobStatus(jobDto.getJobUuid(), JobStatus.VALIDATED);

                commandHelperService.getJobClient().submitJob(jobDto.getJobUuid(), null);

                jobDto = commandHelperService.waitForJobStatus(jobDto.getJobUuid(), JobStatus.COMPLETED);

                this.commandHelperService.output(jobDto);
            } catch (Exception e) {
                System.err.println("Error running job: " + e.getMessage());
            }
        }

    }
}



