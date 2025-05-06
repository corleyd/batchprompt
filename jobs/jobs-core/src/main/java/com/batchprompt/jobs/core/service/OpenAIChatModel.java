package com.batchprompt.jobs.core.service;

import com.batchprompt.jobs.core.model.ChatModelResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of ChatModel for OpenAI chat models
 */
@Slf4j
public class OpenAIChatModel implements ChatModel {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Double DEFAULT_TEMPERATURE = 0.7;
    private static final Integer DEFAULT_MAX_TOKENS = 2000;
    
    private final String apiKey;
    private final String modelName;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Constructor
     * 
     * @param modelName The name of the OpenAI model
     * @param apiKey The OpenAI API key
     */
    public OpenAIChatModel(String modelName, String apiKey) {
        this.modelName = modelName;
        this.apiKey = apiKey;
    }
    
    @Override
    public String getName() {
        return modelName;
    }
    
    @Override
    public ChatModelResponse generateChatResponse(String prompt, String model, @Nullable JsonNode outputSchema,
                                                 @Nullable Integer maxTokens, @Nullable Double temperature) {
        try {
            // Check if API key is available
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("OpenAI API key not found in configuration");
                return ChatModelResponse.of("Error: OpenAI API key not found");
            }
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // Create request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            
            // Create messages array
            ArrayNode messages = requestBody.putArray("messages");
            
            // System message with instructions
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful assistant. Process the following data according to the instructions.");
            messages.add(systemMessage);
            
            // User message with prompt and data
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(prompt).append("\n\n");
            
            if (outputSchema != null) {
                contentBuilder.append("\n\nOutput should follow this JSON schema: ")
                             .append(outputSchema.toString());
            }
            
            userMessage.put("content", contentBuilder.toString());
            messages.add(userMessage);
            
            // Set parameters with defaults if not provided
            requestBody.put("temperature", temperature != null ? temperature : DEFAULT_TEMPERATURE);
            requestBody.put("max_tokens", maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS);
            
            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            // Send request to OpenAI
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(
                OPENAI_API_URL,
                requestEntity,
                String.class
            );
            
            // Process response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode choices = responseJson.get("choices");
            String responseText = "Error: Could not extract response content";
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    responseText = firstChoice.get("message").get("content").asText();
                }
            }
            
            // Extract token usage information
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer totalTokens = null;
            
            if (responseJson.has("usage")) {
                JsonNode usage = responseJson.get("usage");
                if (usage.has("prompt_tokens")) {
                    promptTokens = usage.get("prompt_tokens").asInt();
                }
                if (usage.has("completion_tokens")) {
                    completionTokens = usage.get("completion_tokens").asInt();
                }
                if (usage.has("total_tokens")) {
                    totalTokens = usage.get("total_tokens").asInt();
                }
            }
            
            return ChatModelResponse.of(responseText, promptTokens, completionTokens, totalTokens);
            
        } catch (Exception e) {
            log.error("Error generating response from OpenAI", e);
            return ChatModelResponse.of("Error: " + e.getMessage());
        }
    }
}