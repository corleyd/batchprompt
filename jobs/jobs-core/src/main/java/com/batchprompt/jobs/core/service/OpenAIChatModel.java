package com.batchprompt.jobs.core.service;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ChatModel for OpenAI chat models
 */
@Slf4j
public class OpenAIChatModel extends AbstractOpenAICompatibleChatModel {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    /**
     * Constructor
     * 
     * @param modelName The name of the OpenAI model
     * @param apiKey The OpenAI API key
     */
    public OpenAIChatModel(String modelId, String providerModelId, String apiKey) {
        super(modelId, providerModelId, apiKey);
    }
    
    @Override
    protected String getApiEndpoint() {
        return OPENAI_API_URL;
    }
}