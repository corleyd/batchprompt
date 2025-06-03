package com.batchprompt.notifications.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for WebSocket message conversion to ensure consistent date serialization.
 * This configures the Jackson ObjectMapper used by WebSocket messaging to handle LocalDateTime
 * in the same way as REST endpoints.
 */
@Configuration
public class WebSocketMessageConfig {

    /**
     * Creates a customized message converter that uses our configured ObjectMapper
     */
    @Bean
    @Primary
    public MappingJackson2MessageConverter mappingJackson2MessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
