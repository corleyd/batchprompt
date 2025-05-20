package com.batchprompt.notifications.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
 * Controller for inspecting and diagnosing WebSocket issues.
 * For development use only.
 */
@RestController
@RequestMapping("/api/websocket/debug")
@RequiredArgsConstructor
@Slf4j
public class WebSocketInspectorController {

    private final SimpUserRegistry userRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketMessageBrokerStats brokerStats;
    
    /**
     * Get detailed information about the registry and connections
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Registry information
        status.put("registryClass", userRegistry.getClass().getName());
        status.put("userCount", userRegistry.getUserCount());
        
        // User details
        List<Map<String, Object>> users = userRegistry.getUsers().stream()
            .map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("name", user.getName());
                userInfo.put("sessionCount", user.getSessions().size());
                return userInfo;
            })
            .collect(Collectors.toList());
        status.put("users", users);
        
        // WebSocket stats
        status.put("webSocketSessions", brokerStats.getWebSocketSessionStats());
        status.put("stompBrokerRelay", brokerStats.getStompBrokerRelayStats());
        status.put("stompSubProtocol", brokerStats.getStompSubProtocolStats());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Send a test message to a specific user to verify subscription
     */
    @PostMapping("/test/send/{userId}")
    public ResponseEntity<Map<String, Object>> sendTestMessage(@PathVariable String userId) {
        String destination = "/user/" + userId + "/queue/notifications";
        String message = "Test notification message at " + java.time.LocalDateTime.now();
        
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", message);
            log.info("Sent test message to user {} at {}", userId, destination);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Test message sent to " + userId,
                "destination", destination
            ));
        } catch (Exception e) {
            log.error("Failed to send test message to user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
