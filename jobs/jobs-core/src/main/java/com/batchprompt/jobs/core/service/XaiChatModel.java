package com.batchprompt.jobs.core.service;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ChatModel for Xai (Grok) chat models
 */
@Slf4j
public class XaiChatModel extends AbstractOpenAICompatibleChatModel {

    private static final String XAI_API_URL = "https://api.x.ai/v1/chat/completions";
    
    /**
     * Constructor
     * 
     * @param modelName The name of the Xai model
     * @param apiKey The Xai API key
     */
    public XaiChatModel(String modelName, String apiKey) {
        super(modelName, apiKey);
    }
    
    @Override
    protected String getApiEndpoint() {
        return XAI_API_URL;
    }
}