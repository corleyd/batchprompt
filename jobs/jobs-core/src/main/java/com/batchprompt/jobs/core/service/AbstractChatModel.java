package com.batchprompt.jobs.core.service;

public abstract class AbstractChatModel implements ChatModel {
    private final String modelId;
    private final String providerModelId;

    public AbstractChatModel(String modelId, String providerModelId) {
        this.modelId = modelId;
        this.providerModelId = providerModelId;
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public String getProviderModelId() {
        return providerModelId;
    }
    
}
