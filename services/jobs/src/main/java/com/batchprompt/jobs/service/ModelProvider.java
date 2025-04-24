package com.batchprompt.jobs.service;

/**
 * Enum representing different model providers
 */
public enum ModelProvider {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    GOOGLE("Google"),
    AWS("Amazon Web Services");

    private final String displayName;

    ModelProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}