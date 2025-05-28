package com.batchprompt.cli;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;

@Component
@Command(name = "batchprompt", 
         description = "BatchPrompt CLI for managing prompts and executing batch operations",
         mixinStandardHelpOptions = true,
         subcommands = { PromptCommand.class, 
                         JobCommand.class,
                         FileCommand.class,
                         ModelsCommand.class })
public class BatchPromptCommand implements Runnable {

    @Override
    public void run() {
        // This method can be used to display a general help message or perform any startup tasks.
        System.out.println("Welcome to BatchPrompt CLI! Use --help for command options.");
    }

    
}
