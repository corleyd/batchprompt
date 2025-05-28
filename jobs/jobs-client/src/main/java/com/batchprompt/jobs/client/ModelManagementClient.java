package com.batchprompt.jobs.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.common.client.ClientAuthenticationService;
import com.batchprompt.common.client.ClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ModelManagementClient {
    private final RestTemplate restTemplate;
    private final ClientAuthenticationService authService;
    
    @Value("${services.jobs.url}")
    private String jobsServiceUrl;
    

    public void refreshModels(String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            restTemplate.exchange(
                jobsServiceUrl + "/api/model-management/refresh",
                HttpMethod.POST,
                requestEntity,
                Void.class);
        } catch (Exception e) {
            log.error("Error refreshing models", e);
            throw new ClientException("Error refreshing models", e);
        }
    }
        
    
}
