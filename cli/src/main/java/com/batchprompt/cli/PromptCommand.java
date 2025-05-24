package com.batchprompt.cli;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batchprompt.prompts.model.dto.PromptDto;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@RequiredArgsConstructor
@Command(name = "prompt", description = "Manage prompts in BatchPrompt", subcommands = {
        PromptCommand.GetPromptCommand.class }, mixinStandardHelpOptions = true)
public class PromptCommand {

    @Component
    @Command(name = "get", description = "Get a prompt by ID or name")
    @RequiredArgsConstructor
    static class GetPromptCommand implements Runnable {
        private final CommandHelperService commandHelperService;

        @Option(names = { "-i", "--id" }, description = "ID of the prompt to retrieve")
        private UUID promptUuid;

        @Option(names = { "-n", "--name" }, description = "Name of the prompt to retrieve")
        private String name;
        
        @Override
        public void run() {
            getPrompt();
        }

        public void getPrompt() {
            PromptDto promptDto;
            if (promptUuid != null) {
                promptDto = commandHelperService.getPromptClient().getPrompt(promptUuid, null);
            } else if (name != null) {
                throw new UnsupportedOperationException("Retrieving prompts by name is not yet implemented.");
            } else {
                throw new IllegalArgumentException("Either ID or name must be provided to retrieve a prompt.");
            }
            if (promptDto == null) {
                throw new IllegalArgumentException("Prompt not found with the provided ID or name.");
            }
            commandHelperService.output(promptDto);
        }
    }

}
