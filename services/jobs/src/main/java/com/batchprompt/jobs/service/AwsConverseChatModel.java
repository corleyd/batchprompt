package com.batchprompt.jobs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Implementation of ChatModel for AWS Bedrock Converse models
 * Uses AWS SDK for Java Bedrock client and default credential providers
 */
@Slf4j
public class AwsConverseChatModel implements ChatModel {
    
    private final String modelName;
    private final String modelId;
    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Constructor 
     * 
     * @param modelName The name of the AWS model
     * @param modelId The AWS model ID (e.g., "anthropic.claude-3-sonnet-20240229-v1:0")
     */
    public AwsConverseChatModel(String modelName, String modelId) {
        this.modelName = modelName;
        this.modelId = modelId;
        
        // Create Bedrock client using default credential providers and region from environment
        this.bedrockClient = BedrockRuntimeClient.create();
        log.info("Created AWS Bedrock client for model {} ({})", modelName, modelId);
    }
    
    @Override
    public String getName() {
        return modelName;
    }
    
    @Override
    public String generateResponse(String prompt, String model, @Nullable JsonNode outputSchema) {
        try {
            // Prepare request payload according to the Bedrock model being used
            // This assumes an Anthropic Claude model by default
            ObjectNode requestBody = createModelRequestPayload(prompt, outputSchema);
            
            log.trace("AWS Bedrock request payload: {}", requestBody);
            
            // Convert request to JSON bytes
            SdkBytes payloadBytes = SdkBytes.fromUtf8String(objectMapper.writeValueAsString(requestBody));
            
            // Create and execute the model invoke request
            InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(payloadBytes)
                .build();
                
            // Invoke the model
            InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);
            
            // Process the response
            String responseBody = response.body().asUtf8String();
            log.trace("AWS Bedrock response: {}", responseBody);
            
            // Parse the response and extract the generated text
            JsonNode responseJson = objectMapper.readTree(responseBody);
            return extractTextFromResponse(responseJson);
            
        } catch (Exception e) {
            log.error("Error generating response from AWS Bedrock", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Create the request payload for the specific model
     * 
     * @param prompt The input prompt
     * @param outputSchema Optional JSON schema for structured output
     * @return Request payload as JsonNode
     */
    private ObjectNode createModelRequestPayload(String prompt, @Nullable JsonNode outputSchema) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        
        // Set the model ID        
        // Add messages
        ArrayNode messagesArray = requestBody.putArray("messages");
        ObjectNode userMessage = messagesArray.addObject();
        userMessage.put("role", "user");
        
        StringBuilder messageContent = new StringBuilder(prompt);
        
        if (outputSchema != null) {
            messageContent.append("\n\nOutput should follow this JSON schema: ")
                         .append(outputSchema.toString());
        }
        
        userMessage.put("content", messageContent.toString());
        
        // Set model parameters
        requestBody.put("max_tokens", 2000);
        requestBody.put("temperature", 0.7);
        
        return requestBody;
    }

    /**
     * Extract the generated text from the model response
     * 
     * @param responseJson Response as JsonNode
     * @return Extracted text
     */
    private String extractTextFromResponse(JsonNode responseJson) {
        
        // Generic fallback extraction - try common response patterns
        if (responseJson.has("choices") && responseJson.get("choices").isArray()) {
            ArrayNode choices = (ArrayNode) responseJson.get("choices");
            if (choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                } else if (firstChoice.has("text")) {
                    return firstChoice.get("text").asText();
                }
            }
        }
        if (responseJson.has("output") && responseJson.get("output").has("text")) {
            return responseJson.get("output").get("text").asText();
        } else if (responseJson.has("generation")) {
            return responseJson.get("generation").asText();
        } else if (responseJson.has("completion")) {
            return responseJson.get("completion").asText();
        } else if (responseJson.has("response")) {
            return responseJson.get("response").asText();
        } else if (responseJson.has("text")) {
            return responseJson.get("text").asText();
        }
        
        // If we can't extract specifically, return the whole response as a string
        log.warn("Could not extract specific output from response, returning full response");
        return responseJson.toString();
    }
}