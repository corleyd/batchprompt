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
     * @param modelId The name of the Xai model
     * @param providerModelId The provider model ID
     * @param apiKey The Xai API key
     */
    public XaiChatModel(String modelId, String providerModelId, String apiKey) {
        super(modelId, providerModelId, apiKey);
    }
    
    @Override
    protected String getApiEndpoint() {
        return XAI_API_URL;
    }
}