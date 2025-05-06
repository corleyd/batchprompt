package com.batchprompt.jobs.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.batchprompt.jobs.core.config.ModelConfig;
import com.batchprompt.jobs.core.config.ModelConfig.ModelDefinition;

@Service
public class ModelService {
    
    // Map of model name to ChatModel instance
    private final Map<String, ChatModel> modelMap = new HashMap<>();
    
    // Map of model name to queue name
    private final Map<String, String> queueMap = new HashMap<>();
    
    /**
     * Constructor to initialize supported models from configuration
     */
    public ModelService(
            ModelConfig modelConfig,
            @Value("${openai.api-key}") String openaiApiKey,
            @Value("${google.api-key:#{null}}") String googleApiKey) {
        
        // Initialize models from configuration
        for (ModelDefinition modelDef : modelConfig.getSupported()) {
            ChatModel model = null;
            
            switch (modelDef.getProvider()) {
                case OPENAI:
                    model = new OpenAIChatModel(modelDef.getName(), openaiApiKey);
                    break;
                    
                case AWS:
                    String arn = (String) modelDef.getProperties().get("arn");
                    if (arn != null) {
                        model = new AwsConverseChatModel(modelDef.getName(), arn);
                    }
                    break;
                    
                case GOOGLE:
                    model = new GeminiChatModel(modelDef.getName(), googleApiKey);
                    break;
                    
                default:
                    // Skip unsupported provider types
                    continue;
            }
            
            if (model != null) {
                modelMap.put(model.getName(), model);
                
                // Store the queue name for this model
                if (modelDef.getQueue() != null && !modelDef.getQueue().isEmpty()) {
                    queueMap.put(model.getName(), modelDef.getQueue());
                }
            }
        }
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
        
        if (model instanceof AwsConverseChatModel) {
            return ModelProvider.AWS;
        }
        
        if (model instanceof GeminiChatModel) {
            return ModelProvider.GOOGLE;
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
    
    /**
     * Get the queue name for a specific model
     *
     * @param modelName The name of the model
     * @return The queue name for the model, or null if not found
     */
    public String getQueueForModel(String modelName) {
        return queueMap.get(modelName);
    }
}