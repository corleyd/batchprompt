package com.batchprompt.cli;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;

@Component
@Command(name = "job",
         description = "Manage and execute jobs in BatchPrompt",
         mixinStandardHelpOptions = true)
public class JobCommand {

}
