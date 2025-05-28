package com.batchprompt.cli;

import org.springframework.stereotype.Component;

import com.batchprompt.common.client.ClientException;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;

@Component
@RequiredArgsConstructor
@Command(name = "models", description = "Manage models in BatchPrompt", subcommands = {
        ModelsCommand.RefreshModelsCommand.class }, mixinStandardHelpOptions = true)
public class ModelsCommand {

    @Component
    @Command(name = "refresh", description = "Refresh the models from the database")
    @RequiredArgsConstructor
    static class RefreshModelsCommand implements Runnable {
        private final CommandHelperService commandHelperService;
        
        @Override
        public void run() {
            try {
                commandHelperService.getModelManagementClient().refreshModels(null);
            } catch (ClientException e) {
                throw new RuntimeException("Error refreshing models", e);
            }
        }
    }
}