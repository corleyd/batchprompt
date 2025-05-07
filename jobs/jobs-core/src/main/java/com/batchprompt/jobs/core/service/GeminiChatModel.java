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
 * Implementation of ChatModel for Google Gemini models
 */
@Slf4j
public class GeminiChatModel extends AbstractChatModel {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final Double DEFAULT_TEMPERATURE = 0.7;
    private static final Integer DEFAULT_MAX_TOKENS = 2000;
    
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Constructor
     * 
     * @param providerModelId The name of the Gemini model
     * @param apiKey The Google API key
     */
    public GeminiChatModel(String modelId, String providerModelId, String apiKey) {
        super(modelId, providerModelId);
        this.apiKey = apiKey;
    }
    
    @Override
    public ChatModelResponse generateChatResponse(String prompt, @Nullable JsonNode outputSchema,
                                                 @Nullable Integer maxTokens, @Nullable Double temperature) {
        try {
            // Check if API key is available
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("Google API key not found in configuration");
                return ChatModelResponse.of("Error: Google API key not found");
            }
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request body - Gemini API uses different structure than OpenAI
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // Create contents array for the prompt
            ArrayNode contents = requestBody.putArray("contents");
            
            // Create the user message
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            
            // Create parts array for the message
            ArrayNode parts = userMessage.putArray("parts");
            ObjectNode textPart = objectMapper.createObjectNode();
            
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(prompt).append("\n\n");
            
            if (outputSchema != null) {
                contentBuilder.append("\n\nOutput should follow this JSON schema: ")
                             .append(outputSchema.toString());
            }
            
            textPart.put("text", contentBuilder.toString());
            parts.add(textPart);
            
            // Add the message to contents
            contents.add(userMessage);
            
            // Set generation configuration with temperature and max tokens
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("temperature", temperature != null ? temperature : DEFAULT_TEMPERATURE);
            generationConfig.put("maxOutputTokens", maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS);
            
            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            // The model name is used to construct the URL
            String apiUrl = String.format(GEMINI_API_URL, getProviderModelId()) + "?key=" + apiKey;
            
            // Send request to Google API
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl,
                requestEntity,
                String.class
            );
            
            // Process response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String responseText = extractResponseText(responseJson);
            
            // Extract token usage if available
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer thinkingTokens = null;
            Integer totalTokens = null;
            
            if (responseJson.has("usageMetadata")) {
                JsonNode usageMetadata = responseJson.get("usageMetadata");
                if (usageMetadata.has("promptTokenCount")) {
                    promptTokens = usageMetadata.get("promptTokenCount").asInt();
                }
                if (usageMetadata.has("candidatesTokenCount")) {
                    completionTokens = usageMetadata.get("candidatesTokenCount").asInt();
                }
                if (usageMetadata.has("thoughtsTokenCount")) {
                    thinkingTokens = usageMetadata.get("thoughtsTokenCount").asInt();
                }
                if (promptTokens != null) {
                    totalTokens = promptTokens;
                }
                if (completionTokens != null) {
                    totalTokens = (totalTokens != null ? totalTokens : 0) + completionTokens;
                }
                if (thinkingTokens != null) {
                    totalTokens = (totalTokens != null ? totalTokens : 0) + thinkingTokens;
                }
            }
            
            return ChatModelResponse.of(responseText, promptTokens, completionTokens, thinkingTokens, totalTokens);
            
        } catch (Exception e) {
            log.error("Error generating response from Gemini", e);
            return ChatModelResponse.of("Error: " + e.getMessage());
        }
    }
    
    /**
     * Extract text from Gemini API response JSON
     */
    private String extractResponseText(JsonNode responseJson) {
        try {
            if (responseJson.has("candidates") && responseJson.get("candidates").isArray()) {
                JsonNode candidates = responseJson.get("candidates");
                if (candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    if (firstCandidate.has("content") && 
                        firstCandidate.get("content").has("parts") && 
                        firstCandidate.get("content").get("parts").isArray()) {
                        
                        JsonNode parts = firstCandidate.get("content").get("parts");
                        if (parts.size() > 0 && parts.get(0).has("text")) {
                            return parts.get(0).get("text").asText();
                        }
                    }
                }
            }
            
            log.warn("Could not extract text from Gemini API response structure");
            return "Error: Unable to extract text from model response";
        } catch (Exception e) {
            log.error("Error extracting text from Gemini response", e);
            return "Error: " + e.getMessage();
        }
    }
}