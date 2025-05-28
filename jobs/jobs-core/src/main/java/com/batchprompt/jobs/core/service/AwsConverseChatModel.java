package com.batchprompt.jobs.core.service;

import com.batchprompt.jobs.core.model.ChatModelResponse;
import com.batchprompt.jobs.core.model.Model;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

/**
 * Implementation of ChatModel for AWS Bedrock Converse models
 * Uses AWS SDK for Java Bedrock client and default credential providers
 */
@Slf4j
public class AwsConverseChatModel extends AbstractChatModel {
    
    private static final Float DEFAULT_TEMPERATURE = 0.7f;
    private static final Integer DEFAULT_MAX_TOKENS = 2000;
    
    private final BedrockRuntimeClient bedrockClient;
    
    /**
     * Constructor 
     * 
     * @param modelName The name of the AWS model
     * @param modelId The AWS model ID (e.g., "anthropic.claude-3-sonnet-20240229-v1:0")
     */
    public AwsConverseChatModel(Model model) {
        super(model);
        
        // Create Bedrock client using default credential providers and region from environment
        this.bedrockClient = BedrockRuntimeClient.create();
        log.info("Created AWS Bedrock client for model {} ({})", getProviderModelId(), model.getModelId());
    }

    @Override
    public ChatModelResponse generateChatResponse(String prompt, @Nullable JsonNode outputSchema,
                                                 @Nullable Integer maxTokens, @Nullable Double temperature) {
        try {
            log.trace("Generating chat response using AWS Bedrock Converse API for prompt: {}", prompt);
            
            // Build the user message content
            String messageContent = prompt;
            
            // Append schema if provided
            if (outputSchema != null) {
                prompt = simulateStructuredOutput(prompt, outputSchema);
            }
            
            // Create a ContentBlock using the fromText factory method
            ContentBlock textBlock = ContentBlock.fromText(messageContent);
            
            // Create the message using the Message builder with the ContentBlock
            Message userMessage = Message.builder()
                .role("user")
                .content(textBlock)
                .build();
            
            // Create inference configuration with temperature and max tokens
            // Convert Double to Float if provided, otherwise use default
            Float tempValue = temperature != null ? temperature.floatValue() : DEFAULT_TEMPERATURE;
            
            InferenceConfiguration inferenceConfig = InferenceConfiguration.builder()
                .temperature(tempValue)
                .maxTokens(maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS)
                .build();
            
            // Create the converse request using the builder pattern
            ConverseRequest converseRequest = ConverseRequest.builder()
                .modelId(getProviderModelId())
                .messages(userMessage)
                .inferenceConfig(inferenceConfig)
                .build();
            
            log.debug("AWS Bedrock Converse request: {}", converseRequest);
            
            // Call the Converse API
            ConverseResponse response = bedrockClient.converse(converseRequest);
            log.debug("AWS Bedrock Converse response: {}", response);
            
            // Extract the response text from the message content
            String responseText = null;
            if (response.output() != null && response.output().message() != null) {
                // For SDK 2.28.21, we need to extract the text from the content blocks
                if (response.output().message().content() != null && !response.output().message().content().isEmpty()) {
                    // The content is a list of ContentBlock, so we need to find the text block
                    for (ContentBlock block : response.output().message().content()) {
                        if (block.text() != null) {
                            responseText = block.text();
                            break;
                        }
                    }
                }
            }
            
            // Extract token usage information - advantage of using the Converse API
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer totalTokens = null;
            
            if (response.usage() != null) {
                promptTokens = response.usage().inputTokens();
                completionTokens = response.usage().outputTokens();
                totalTokens = response.usage().totalTokens();
            }

            if (responseText == null) {
                return handleError("Could not extract response text from AWS Bedrock Converse API response");
            }
            
            return ChatModelResponse.of(responseText, promptTokens, completionTokens, null, totalTokens);
            
        } catch (Exception e) {
            return handleError(e);
        }
    }
}