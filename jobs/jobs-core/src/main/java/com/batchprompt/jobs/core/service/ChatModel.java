package com.batchprompt.jobs.core.service;

import com.batchprompt.jobs.core.model.ChatModelResponse;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.Nullable;

public interface ChatModel {
    String getModelId();
    public String getProviderModelId();

    /**
     * Generates a response from the chat model including token usage information
     * 
     * @param prompt The prompt to send to the model
     * @param model The specific model to use
     * @param outputSchema Optional schema for structured output
     * @param maxTokens Optional maximum number of tokens to generate
     * @param temperature Optional temperature setting for response randomness
     * @return ChatModelResponse containing response text and token usage information
     */
    ChatModelResponse generateChatResponse(String prompt, @Nullable JsonNode outputSchema, 
                                           @Nullable Integer maxTokens, @Nullable Double temperature);
    
    /**
     * Generates a response from the chat model including token usage information (with default parameters)
     * 
     * @param prompt The prompt to send to the model
     * @param model The specific model to use
     * @param outputSchema Optional schema for structured output
     * @return ChatModelResponse containing response text and token usage information
     */
    default ChatModelResponse generateChatResponse(String prompt, @Nullable JsonNode outputSchema) {
        return generateChatResponse(prompt, outputSchema, null, null);
    }
}
