package com.batchprompt.jobs.service;


import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.Nullable;

public interface ChatModel {
    String getName();
    String generateResponse(String prompt, String model, JsonNode recordData, @Nullable JsonNode outputSchema);
}
