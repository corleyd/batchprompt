package com.batchprompt.notifications.api.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for debugging and monitoring WebSocket connections
 */
@RestController
@RequestMapping("/api/ws-diagnostics")
@RequiredArgsConstructor
@Slf4j
public class WebSocketDiagnosticsController {

    private final SimpUserRegistry userRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired(required = false)
    private WebSocketMessageBrokerStats brokerStats;
    
    /**
     * Get information about connected WebSocket clients
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Get user count
        int userCount = userRegistry.getUserCount();
        status.put("userCount", userCount);
        
        // Get user details
        Map<String, Object> userDetails = new HashMap<>();
        userRegistry.getUsers().forEach(user -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("sessionCount", user.getSessions().size());
            userData.put("sessions", user.getSessions().stream()
                .map(session -> {
                    Map<String, Object> sessionData = new HashMap<>();
                    sessionData.put("id", session.getId());
                    sessionData.put("subscriptionCount", session.getSubscriptions().size());
                    sessionData.put("subscriptions", session.getSubscriptions().stream()
                        .map(sub -> Map.of(
                            "id", sub.getId(),
                            "destination", sub.getDestination()
                        ))
                        .collect(Collectors.toList()));
                    return sessionData;
                })
                .collect(Collectors.toList()));
            
            userDetails.put(user.getName(), userData);
        });
        status.put("users", userDetails);
        
        // Add broker stats if available
        if (brokerStats != null) {
            status.put("brokerStats", brokerStats.toString());
        }
        
        return status;
    }
    
    /**
     * Send a test message to a specific user
     */
    @PostMapping("/test-message/{userId}")
    public Map<String, Object> sendTestMessage(@PathVariable String userId) {
        Map<String, Object> result = new HashMap<>();
        
        // Check if user is connected
        SimpUser user = userRegistry.getUser(userId);
        if (user != null) {
            // User is connected, send test message
            Map<String, Object> message = new HashMap<>();
            message.put("type", "TEST");
            message.put("message", "This is a test message");
            message.put("timestamp", System.currentTimeMillis());
            
            try {
                messagingTemplate.convertAndSendToUser(
                    userId,
                    "/topic/test",
                    message
                );
                result.put("success", true);
                result.put("message", "Test message sent to user " + userId);
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", "Failed to send message: " + e.getMessage());
            }
        } else {
            // User is not connected
            result.put("success", false);
            result.put("error", "User " + userId + " is not connected");
        }
        
        return result;
    }
}
