package com.batchprompt.notifications.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Configuration for WebSocket message conversion to ensure consistent date serialization.
 * This configures the Jackson ObjectMapper used by WebSocket messaging to handle LocalDateTime
 * in the same way as REST endpoints.
 */
@Configuration
public class WebSocketMessageConfig {

    /**
     * Creates a customized ObjectMapper for WebSocket message conversion
     * that properly handles Java 8 date/time types like LocalDateTime
     */
    @Bean
    @Primary
    public ObjectMapper webSocketObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register JavaTimeModule to properly handle Java 8 date/time types
        objectMapper.registerModule(new JavaTimeModule());
        
        // Disable timestamp serialization, use ISO-8601 format instead
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // Ensure that time zone information is included
        objectMapper.configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false);
        
        // Be lenient when deserializing unknown properties
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return objectMapper;
    }
    
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
