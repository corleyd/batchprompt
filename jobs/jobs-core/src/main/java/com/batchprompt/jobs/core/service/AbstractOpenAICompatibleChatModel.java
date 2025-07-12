package com.batchprompt.jobs.core.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.jobs.core.model.ChatModelResponse;
import com.batchprompt.jobs.core.model.Model;
import com.batchprompt.jobs.model.StopReason;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for OpenAI compatible chat models
 * Implements common functionality for models with OpenAI-compatible APIs
 */
@Slf4j
public abstract class AbstractOpenAICompatibleChatModel extends AbstractChatModel {

    protected static final Double DEFAULT_TEMPERATURE = 0.7;
    protected static final Integer DEFAULT_MAX_TOKENS = 2000;
    
    protected final String apiKey;
    protected final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get the API endpoint for the model
     * 
     * @return The API endpoint URL
     */
    protected abstract String getApiEndpoint();
    
    /**
     * Constructor
     * 
     * @param model The model to use
     * @param apiKey The API key
     */
    public AbstractOpenAICompatibleChatModel(Model model, String apiKey) {
        super(model);
        this.apiKey = apiKey;
    }
    
    @Override
    public ChatModelResponse generateChatResponse(String prompt, @Nullable JsonNode outputSchema,
                                                 @Nullable Integer maxTokens, @Nullable Double temperature) {
        try {
            // Check if API key is available
            if (apiKey == null || apiKey.isEmpty()) {
                return handleError("Error: API key not found");
            }
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // Create request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", getProviderModelId());
            
            // Create messages array
            ArrayNode messages = requestBody.putArray("messages");
            
            // Set parameters with defaults if not provided
            if (getPropertyValueBoolean("supportsTemperature", true)) {
                requestBody.put("temperature", temperature != null ? temperature : DEFAULT_TEMPERATURE);
            }

            String maxTokensProperty = "max_tokens";
            if (getPropertyValueBoolean("useMaxCompletionTokens", false)) {
                maxTokensProperty = "max_completion_tokens";
            }

            requestBody.put(maxTokensProperty, maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS);
            
            if (outputSchema != null) {
                if (getModel().isSimulateStructuredOutput()) {
                    prompt = simulateStructuredOutput(prompt, outputSchema);
                } else {
                    ObjectNode responseFormatNode = objectMapper.createObjectNode();
                    responseFormatNode.put("type", "json_schema");
                    ObjectNode jsonSchemaNode = objectMapper.createObjectNode();
                    jsonSchemaNode.put("name", "response");
                    jsonSchemaNode.set("schema", outputSchema);
                    responseFormatNode.set("json_schema", jsonSchemaNode);
                    requestBody.set("response_format", responseFormatNode);
                }
            }

            // User message with prompt and data
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            requestBody.set("messages", messages);
            
            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            // Send request to API
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(
                getApiEndpoint(),
                requestEntity,
                String.class
            );
            
            // Process response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode choices = responseJson.get("choices");
            
            // Extract response text and stop reason
            String responseText = null;
            StopReason stopReason = null;
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    responseText = firstChoice.get("message").get("content").asText();
                }
                
                // Extract stop reason from finish_reason
                if (firstChoice.has("finish_reason")) {
                    String finishReason = firstChoice.get("finish_reason").asText();
                    stopReason = mapFinishReasonToStopReason(finishReason);
                }
            }
            
            // Extract token usage information  
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer thinkingTokens = null;
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
                // Some models like OpenAI o1 have thinking tokens
                if (usage.has("reasoning_tokens")) {
                    thinkingTokens = usage.get("reasoning_tokens").asInt();
                }
            }

            if (responseText == null) {
                return handleError("Error: No valid response received from API", stopReason, null);
            }
            
            return ChatModelResponse.of(responseText, promptTokens, completionTokens, thinkingTokens, totalTokens, stopReason, null);
            
        } catch (Exception e) {
            return handleError(e);
        }
    }

    /**
     * Maps OpenAI API finish_reason to StopReason enum
     */
    private StopReason mapFinishReasonToStopReason(String finishReason) {
        if (finishReason == null) {
            return StopReason.UNKNOWN;
        }
        
        switch (finishReason.toLowerCase()) {
            case "stop":
                return StopReason.STOP;
            case "length":
                return StopReason.MAX_TOKENS;
            case "function_call":
            case "tool_calls":
                return StopReason.TOOL_CALLS;
            case "content_filter":
                return StopReason.CONTENT_FILTER;
            default:
                return StopReason.UNKNOWN;
        }
    }
}