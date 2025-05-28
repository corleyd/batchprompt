package com.batchprompt.jobs.core.service;

import java.util.UUID;

import com.batchprompt.jobs.core.model.ChatModelResponse;
import com.batchprompt.jobs.core.model.Model;
import com.fasterxml.jackson.databind.JsonNode;

public class BatchPromptChatModel extends AbstractChatModel {

    public BatchPromptChatModel(Model model) {
        super(model);
    }

    @Override
    public ChatModelResponse generateChatResponse(String prompt, JsonNode outputSchema, Integer maxTokens,
            Double temperature) {

        switch (getModel().getModelProviderModelId()) {
            case "echo":
                return ChatModelResponse.of(prompt);
            case "random":
                String response;
                if (outputSchema != null) {
                    response = generateStructuredOutput(prompt, outputSchema);
                } else {
                    response = UUID.randomUUID().toString();
                }
                return ChatModelResponse.of(response);
            default:
                return handleError("Unknown test model: " + getModelId());
        }
    }

    /*
     * Given the JSON schema provided in outputSchema, this method simulates a structured output
     * by generating a JSON response that matches the schema.
     */
    private String generateStructuredOutput(String prompt, JsonNode outputSchema) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode result = mapper.createObjectNode();
            
            JsonNode properties = outputSchema.get("properties");
            if (properties != null) {
                properties.fields().forEachRemaining(entry -> {
                    String fieldName = entry.getKey();
                    JsonNode fieldSchema = entry.getValue();
                    String type = fieldSchema.get("type").asText();
                    
                    switch (type) {
                        case "string":
                            result.put(fieldName, "test_" + fieldName);
                            break;
                        case "integer":
                            result.put(fieldName, 42);
                            break;
                        case "number":
                            result.put(fieldName, 3.14);
                            break;
                        case "boolean":
                            result.put(fieldName, true);
                            break;
                        case "array":
                            result.set(fieldName, mapper.createArrayNode().add("item1").add("item2"));
                            break;
                        default:
                            result.put(fieldName, "unknown_type");
                    }
                });
            }
            
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate structured output\"}";
        }
    }

    
}
