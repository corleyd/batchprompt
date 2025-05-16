package com.batchprompt.jobs.core.service;

import com.batchprompt.jobs.core.model.ChatModelResponse;
import com.batchprompt.jobs.core.model.Model;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.Nullable;

public abstract class AbstractChatModel {
    private final Model model;

    public AbstractChatModel(Model model) {
        this.model = model;
    }

    public abstract ChatModelResponse generateChatResponse(String prompt, @Nullable JsonNode outputSchema,
                                                 @Nullable Integer maxTokens, @Nullable Double temperature);

    public String getModelId() {
        return model.getModelId();
    }

    public String getProviderModelId() {
        return model.getModelProviderModelId();
    }

    public Model getModel() {
        return model;
    }

    protected String simulateStructuredOutput(String prompt, JsonNode outputSchema) {
        return prompt + "\n\n" +
                "You MUST respond with ONLY a valid JSON object that follows the schema below. " +
                "DO NOT include the schema in your response. " +
                "DO NOT explain your response. " +
                "Create actual content for each required field based on the prompt above.\n\n" +
                "JSON SCHEMA:\n" +
                outputSchema.toString() + "\n\n" +
                "Remember: Your entire response must be ONLY valid JSON that matches this schema. " +
                "Do not repeat the schema or include explanations.\n";
    }
    
}
