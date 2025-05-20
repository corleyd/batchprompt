package com.batchprompt.notifications.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

import lombok.extern.slf4j.Slf4j;

/**
 * Component to verify that the WebSocket subsystem is properly initialized.
 * This runs after the application context is refreshed to verify the configuration.
 */
@Component
@Slf4j
public class WebSocketInitVerifier implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private SimpUserRegistry userRegistry;
    
    @Autowired(required = false)
    private WebSocketMessageBrokerStats brokerStats;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("WebSocket initialization verification:");
        log.info("  SimpUserRegistry implementation: {}", userRegistry.getClass().getName());
        
        // Check if we have the expected DefaultSimpUserRegistry or a subclass of it
        if (userRegistry.getClass().getName().contains("DefaultSimpUserRegistry")) {
            log.info("  Using a mutable user registry implementation (good!)");
        } else if (userRegistry.getClass().getName().contains("ImmutableUserRegistry")) {
            log.warn("  Using an immutable user registry - users won't be tracked properly!");
        } else {
            log.info("  Using a custom user registry implementation: {}", userRegistry.getClass().getName());
        }
        
        // Log broker stats if available
        if (brokerStats != null) {
            log.info("  WebSocket broker stats configuration:");
            log.info("    Broker available: true");
            log.info("    Logging period: {} ms", brokerStats.getLoggingPeriod());
        } else {
            log.warn("  WebSocketMessageBrokerStats not available - monitoring disabled");
        }
    }
}
