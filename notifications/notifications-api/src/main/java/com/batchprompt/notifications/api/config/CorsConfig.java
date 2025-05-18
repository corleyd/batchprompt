package com.batchprompt.notifications.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;

/**
 * CORS configuration specifically for the notifications service
 */
@Configuration
@Slf4j
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow any origin for WebSockets - required for Authorization header handling
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // Allow credentials like cookies, authorization headers
        config.setAllowCredentials(true);
        
        // Log the configuration
        log.info("Setting up CORS with allowed origins: * (all origins)");
        
        // Allow common HTTP methods
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        
        // Allow ALL headers
        config.addAllowedHeader("*");
        
        // Specifically make sure Authorization is allowed
        config.addAllowedHeader("Authorization");
        
        // Expose these headers to the client
        config.addExposedHeader("Access-Control-Allow-Origin");
        config.addExposedHeader("Access-Control-Allow-Credentials");
        config.addExposedHeader("Authorization");
        
        // Apply this configuration to all endpoints
        source.registerCorsConfiguration("/**", config);
        source.registerCorsConfiguration("/ws", config);
        source.registerCorsConfiguration("/ws/info", config);
        
        return new CorsFilter(source);
    }
}
