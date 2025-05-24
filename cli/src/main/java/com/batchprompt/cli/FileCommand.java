package com.batchprompt.cli;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;

@Component
@Command(name = "file",
           description = "Manage files in BatchPrompt",
           mixinStandardHelpOptions = true)
public class FileCommand {

}
