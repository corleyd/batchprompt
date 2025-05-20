package com.batchprompt.notifications.api.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;

/**
 * Periodically logs the SimpUserRegistry details to help diagnose WebSocket issues.
 */
@Configuration
@EnableScheduling
@Slf4j
public class UserRegistryLogger implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private SimpUserRegistry userRegistry;

    /**
     * Log registry details on application startup
     */
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        log.info("Application started - User registry details:");
        logRegistryDetails();
    }
    
    /**
     * Log registry details periodically
     */
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    public void logUserRegistry() {
        log.info("Periodic SimpUserRegistry check:");
        logRegistryDetails();
    }
    
    private void logRegistryDetails() {
        try {
            log.info("  Registry class: {}", userRegistry.getClass().getName());
            log.info("  User count: {}", userRegistry.getUserCount());
            
            if (userRegistry.getUserCount() > 0) {
                userRegistry.getUsers().forEach(user -> {
                    log.info("  - User: {}", user.getName());
                    log.info("    Sessions: {}", user.getSessions().size());
                });
            } else {
                log.info("  No connected users");
            }
            
            log.info("  Registry hashcode: {}", userRegistry.hashCode());
        } catch (Exception e) {
            log.error("Error logging registry details", e);
        }
    }
}
