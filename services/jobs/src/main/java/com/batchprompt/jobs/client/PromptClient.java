package com.batchprompt.jobs.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.commons.services.ServiceAuthenticationService;
import com.batchprompt.jobs.dto.PromptDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PromptClient {

    private final RestTemplate restTemplate;
    private final ServiceAuthenticationService authService;
    
    @Value("${services.prompts.url}")
    private String promptsServiceUrl;
    
    @Value("${services.name:jobs-service}")
    private String serviceName;
    
    /**
     * Get a prompt by UUID
     * 
     * @param promptUuid The UUID of the prompt to retrieve
     * @param authToken The user's auth token, or null for service-to-service call
     * @return The prompt DTO
     */
    public PromptDto getPrompt(UUID promptUuid, String authToken) {
        try {
            String token = getEffectiveToken(authToken);
            ResponseEntity<PromptDto> response = restTemplate.exchange(
                promptsServiceUrl + "/api/prompts/" + promptUuid,
                HttpMethod.GET,
                HeaderUtil.createEntityWithAuthHeader(token),
                PromptDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving prompt {}", promptUuid, e);
            return null;
        }
    }
    
    /**
     * Get an effective token for the request. If user token is null, get a service token.
     * 
     * @param userToken The user's auth token or null
     * @return An effective token for the request
     */
    private String getEffectiveToken(String userToken) {
        if (userToken != null && !userToken.isEmpty()) {
            return userToken;
        }
        // Use service authentication when no user token is provided
        return authService.getServiceToken(serviceName);
    }
}