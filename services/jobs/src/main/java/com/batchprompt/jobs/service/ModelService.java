package com.batchprompt.jobs.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ModelService {
    
    // List of currently supported model names
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
        "gpt-3.5-turbo",
        "gpt-4",
        "gpt-4-turbo",
        "claude-3-opus",
        "claude-3-sonnet",
        "claude-3-haiku",
        "gemini-pro"
    );
    
    /**
     * Check if the given model name is supported
     * 
     * @param modelName The name of the model to check
     * @return true if the model is supported, false otherwise
     */
    public boolean isModelSupported(String modelName) {
        return SUPPORTED_MODELS.contains(modelName);
    }
    
    /**
     * Get a list of all supported model names
     * 
     * @return List of supported model names
     */
    public List<String> getSupportedModels() {
        return SUPPORTED_MODELS;
    }
}