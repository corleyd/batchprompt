package com.batchprompt.notifications.api.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * A diagnostic component that examines how Spring STOMP WebSocket user registry is configured.
 * This helps identify how users are tracked in the internal registry implementation.
 */
@Component
@Slf4j
public class WebSocketDiagnostics implements InitializingBean {

    @Autowired
    private SimpUserRegistry userRegistry;
    
    @Autowired(required = false)
    private WebSocketMessageBrokerStats stats;
    
    @Autowired(required = false)
    @Lazy
    private SimpMessagingTemplate messagingTemplate;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("WebSocket diagnostics:");
        log.info("User registry implementation: {}", userRegistry.getClass().getName());
        
        // Check what implementation we're using
        String className = userRegistry.getClass().getName();
        
        if (className.contains("DefaultSimpUserRegistry")) {
            log.info("Using DefaultSimpUserRegistry implementation");
        } else if (className.contains("ImmutableUserRegistry")) {
            log.warn("Using ImmutableUserRegistry - this won't track users!");
        } else {
            log.info("Using custom user registry implementation: {}", className);
        }
        
        // Inspect the registry fields
        inspectObject(userRegistry);
        
        if (stats != null) {
            log.info("WebSocketMessageBrokerStats is available");
        } else {
            log.warn("WebSocketMessageBrokerStats is NOT available - can't monitor stats");
        }
        
        if (messagingTemplate != null) {
            log.info("SimpMessagingTemplate is available: {}", messagingTemplate.getClass().getName());
        } else {
            log.warn("SimpMessagingTemplate is NOT available - messaging may be misconfigured");
        }
    }
    
    /**
     * Use reflection to inspect an object's fields for diagnostic purposes
     */
    private void inspectObject(Object obj) {
        if (obj == null) return;
        
        Class<?> clazz = obj.getClass();
        log.info("Inspecting {} fields:", clazz.getSimpleName());
        
        try {
            // Get all fields, including private ones
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                // Skip static fields
                if (Modifier.isStatic(field.getModifiers())) continue;
                
                field.setAccessible(true);
                Object value = field.get(obj);
                String valueDesc = (value == null) ? "null" : 
                    (value.getClass().isArray() ? Arrays.toString((Object[])value) : value.toString());
                
                log.info("  {} ({}) = {}", field.getName(), field.getType().getSimpleName(), valueDesc);
            }
        } catch (Exception e) {
            log.warn("Error inspecting fields: {}", e.getMessage());
        }
    }
}
