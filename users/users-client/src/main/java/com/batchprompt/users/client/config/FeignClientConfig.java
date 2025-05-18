package com.batchprompt.users.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.batchprompt.common.services.ServiceAuthenticationService;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignClientConfig {
    
    private final ServiceAuthenticationService authService;
    
    @Bean
    public RequestInterceptor serviceAuthRequestInterceptor() {
        return requestTemplate -> {
            String token = authService.getServiceToken();
            if (token != null) {
                requestTemplate.header("Authorization", token);
                log.debug("Added service authentication token to request");
            } else {
                log.warn("Failed to obtain service authentication token");
            }
        };
    }
}