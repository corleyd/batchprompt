package com.batchprompt.jobs.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ModelService {
    
    // Map of model name to ChatModel instance
    private final Map<String, ChatModel> modelMap = new HashMap<>();
    
    /**
     * Constructor to initialize supported models
     */
    public ModelService(@Value("${openai.api-key}") String openaiApiKey) {
        // Initialize OpenAI models
        OpenAIChatModel gpt4o = new OpenAIChatModel("gpt-4o", openaiApiKey);
        OpenAIChatModel gpt4 = new OpenAIChatModel("gpt-4", openaiApiKey);
        OpenAIChatModel gpt35Turbo = new OpenAIChatModel("gpt-3.5-turbo", openaiApiKey);
        
        // Add models to the map
        modelMap.put(gpt4o.getName(), gpt4o);
        modelMap.put(gpt4.getName(), gpt4);
        modelMap.put(gpt35Turbo.getName(), gpt35Turbo);
    }
    
    /**
     * Check if the given model name is supported
     * 
     * @param modelName The name of the model to check
     * @return true if the model is supported, false otherwise
     */
    public boolean isModelSupported(String modelName) {
        return modelMap.containsKey(modelName);
    }
    
    /**
     * Get a list of all supported model names
     * 
     * @return List of supported model names
     */
    public List<String> getSupportedModels() {
        return modelMap.keySet().stream().collect(Collectors.toList());
    }
    
    /**
     * Get the ModelProvider for a given model name
     *
     * @param modelName The name of the model
     * @return The ModelProvider for the model, or null if not found
     */
    public ModelProvider getProviderForModel(String modelName) {
        ChatModel model = modelMap.get(modelName);
        if (model == null) {
            return null;
        }
        
        if (model instanceof OpenAIChatModel) {
            return ModelProvider.OPENAI;
        }
        
        return null; // Default case if the model type is unknown
    }
    
    /**
     * Get a ChatModel instance by model name
     *
     * @param modelName The name of the model
     * @return The ChatModel instance, or null if not found
     */
    public ChatModel getModel(String modelName) {
        return modelMap.get(modelName);
    }
}