package com.batchprompt.jobs.core.service;


import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.Nullable;

public interface ChatModel {
    String getName();
    String generateResponse(String prompt, String model, @Nullable JsonNode outputSchema);
}
