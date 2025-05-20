package com.batchprompt.notifications.api.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration to ensure proper user tracking in the WebSocket system.
 * This monitors the system to ensure users are being properly registered.
 */
@Configuration
@EnableScheduling
@Slf4j
public class UserRegistryMonitor implements ApplicationContextAware {
    
    private ApplicationContext applicationContext;
    
    @Autowired
    private SimpUserRegistry userRegistry;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    /**
     * Periodically log information about connected users.
     * This helps diagnose issues with user tracking.
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void monitorUserRegistry() {
        // Log current user count
        int userCount = userRegistry.getUserCount();
        log.info("Current WebSocket user count: {}", userCount);
        
        // Log detailed user information if there are users connected
        if (userCount > 0) {
            log.info("Connected users:");
            userRegistry.getUsers().forEach(user -> {
                logUserDetails(user);
            });
        }
    }
    
    /**
     * Log detailed information about a connected user.
     */
    private void logUserDetails(SimpUser user) {
        log.info("  User: {}", user.getName());
        log.info("    Sessions: {}", user.getSessions().size());
        user.getSessions().forEach(session -> {
            log.info("    - Session: {}", session.getId());
            log.info("      Subscriptions: {}", session.getSubscriptions().size());
            session.getSubscriptions().forEach(subscription -> {
                log.info("      * {} -> {}", subscription.getId(), subscription.getDestination());
            });
        });
    }
}
