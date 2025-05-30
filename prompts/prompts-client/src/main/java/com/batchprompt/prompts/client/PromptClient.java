package com.batchprompt.prompts.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.common.client.ClientAuthenticationService;
import com.batchprompt.prompts.model.dto.PromptDto;
import com.batchprompt.prompts.model.dto.PromptJobInfoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PromptClient {

    private final RestTemplate restTemplate;
    private final ClientAuthenticationService authService;
    
    @Value("${services.prompts.url}")
    private String promptsServiceUrl;
    
    /**
     * Get a prompt by UUID
     * 
     * @param promptUuid The UUID of the prompt to retrieve
     * @param authToken The user's auth token, or null for service-to-service call
     * @return The prompt DTO
     */
    public PromptDto getPrompt(UUID promptUuid, String authToken) {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<PromptDto> response = restTemplate.exchange(
                promptsServiceUrl + "/api/prompts/" + promptUuid,
                HttpMethod.GET,
                requestEntity,
                PromptDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving prompt {}", promptUuid, e);
            return null;
        }
    }

    public PromptDto updateJobInfo(UUID promptUuid, PromptJobInfoDto jobInfo, String authToken) {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<PromptJobInfoDto> requestEntity = new HttpEntity<>(jobInfo, headers);
            
            ResponseEntity<PromptDto> response = restTemplate.exchange(
                promptsServiceUrl + "/api/prompts/" + promptUuid + "/job-info",
                HttpMethod.PUT,
                requestEntity,
                PromptDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error updating job info for prompt {}", promptUuid, e);
            return null;
        }
    }
}