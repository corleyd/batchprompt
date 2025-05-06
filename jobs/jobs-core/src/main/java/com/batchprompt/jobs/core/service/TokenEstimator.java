package com.batchprompt.jobs.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to estimate token counts for different models
 * This is a simplified estimation model that can be enhanced with more accurate calculations
 */
@Slf4j
@RequiredArgsConstructor
public class TokenEstimator {
    
    // Simple ratio of characters per token (average) for different model families
    private static final double GPT_CHARS_PER_TOKEN = 4.0;
    private static final double CLAUDE_CHARS_PER_TOKEN = 3.5;
    private static final double DEFAULT_CHARS_PER_TOKEN = 4.0;
    
    /**
     * Estimate token count for a given text and model provider
     * 
     * @param text The text to estimate token count for
     * @param provider The model provider
     * @return Estimated token count
     */
    public static int estimateTokenCount(String text, ModelProvider provider) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        double charsPerToken;
        switch (provider) {
            case OPENAI:
                charsPerToken = GPT_CHARS_PER_TOKEN;
                break;
            case AWS:
                charsPerToken = CLAUDE_CHARS_PER_TOKEN;
                break;
            default:
                charsPerToken = DEFAULT_CHARS_PER_TOKEN;
        }
        
        // Simple estimation based on character count divided by average chars per token
        return (int) Math.ceil(text.length() / charsPerToken);
    }
    
    /**
     * Estimate token count for a given text and model name
     * 
     * @param text The text to estimate token count for
     * @param modelName The model name
     * @param modelService The model service to get provider from model name
     * @return Estimated token count
     */
    public static int estimateTokenCount(String text, String modelName, ModelService modelService) {
        ModelProvider provider = modelService.getProviderForModel(modelName);
        if (provider == null) {
            // If we can't determine the provider, use default estimation
            return estimateTokenCount(text, ModelProvider.DEFAULT);
        }
        return estimateTokenCount(text, provider);
    }
}