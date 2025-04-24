package com.batchprompt.jobs.service;

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
    public String generateResponse(String prompt, String model, @Nullable JsonNode outputSchema) {
        try {
            // Check if API key is available
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("OpenAI API key not found in configuration");
                return "Error: OpenAI API key not found";
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
            
            // Set additional parameters
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
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
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }
            
            return "Error: Could not extract response content";
            
        } catch (Exception e) {
            log.error("Error generating response from OpenAI", e);
            return "Error: " + e.getMessage();
        }
    }
}