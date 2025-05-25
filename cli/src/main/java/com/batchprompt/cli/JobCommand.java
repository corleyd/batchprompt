package com.batchprompt.cli;

import java.util.UUID;

import org.springframework.stereotype.Component;

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

        @Override
        public void run() {

            PromptDto promptDto = commandHelperService.getPromptClient().getPrompt(promptUuid, null);

            JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                    .targetUserId(promptDto.getUserId())
                    .promptUuid(promptUuid)
                    .fileUuid(fileUuid)
                    .modelId(modelId)
                    .build();

            JobDto jobDto = commandHelperService.getJobClient().validateJob(jobDefinition, null);

            jobDto = commandHelperService.waitForJobStatus(jobDto.getJobUuid(), JobStatus.VALIDATED);

            commandHelperService.getJobClient().submitJob(jobDto.getJobUuid(), null);

            jobDto = commandHelperService.waitForJobStatus(jobDto.getJobUuid(), JobStatus.COMPLETED);

            this.commandHelperService.output(jobDto);
        }

    }
}



