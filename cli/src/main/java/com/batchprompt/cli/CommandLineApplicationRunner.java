package com.batchprompt.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import picocli.CommandLine.IFactory;

@Component
public class CommandLineApplicationRunner implements CommandLineRunner, ExitCodeGenerator {

    private final BatchPromptCommand batchPromptCommand;

    private final IFactory factory;

    private int exitCode;

    public CommandLineApplicationRunner(BatchPromptCommand batchPromptCommand, IFactory factory) {
        this.batchPromptCommand = batchPromptCommand;
        this.factory = factory;
    }

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(String... args) throws Exception {
        exitCode = new picocli.CommandLine(batchPromptCommand, factory)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                    System.err.println("Error: " + ex.getMessage());
                    return 1; // Return a non-zero exit code on error
                })
                .execute(args);
	}
    
}
