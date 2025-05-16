package com.batchprompt.jobs.core.service;

import com.batchprompt.jobs.core.model.Model;

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
    public XaiChatModel(Model model, String apiKey) {
        super(model, apiKey);
    }
    
    @Override
    protected String getApiEndpoint() {
        return XAI_API_URL;
    }
}