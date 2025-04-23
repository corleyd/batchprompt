package com.batchprompt.jobs.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.jobs.dto.PromptDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PromptClient {

    private final RestTemplate restTemplate;
    
    @Value("${services.prompts.url}")
    private String promptsServiceUrl;
    
    public PromptDto getPrompt(UUID promptUuid, String authToken) {
        try {
            ResponseEntity<PromptDto> response = restTemplate.exchange(
                promptsServiceUrl + "/api/prompts/" + promptUuid,
                HttpMethod.GET,
                HeaderUtil.createEntityWithAuthHeader(authToken),
                PromptDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving prompt {}", promptUuid, e);
            return null;
        }
    }
}