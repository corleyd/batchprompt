package com.batchprompt.notifications.api.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.notifications.api.service.WebSocketUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Test controller for WebSocket diagnostics.
 * Only use this in development environments.
 */
@RestController
@RequestMapping("/test/websocket")
@RequiredArgsConstructor
@Slf4j
public class WebSocketTestController {

    private final SimpUserRegistry simpUserRegistry;
    private final WebSocketUserService webSocketUserService;
    
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getConnectedUsers() {
        // Get the list of connected users
        Set<SimpUser> users = simpUserRegistry.getUsers();
        
        // Get user details
        List<Map<String, Object>> userDetails = users.stream()
            .map(user -> Map.of(
                "name", user.getName(),
                "sessions", user.getSessions().stream()
                    .map(session -> Map.of(
                        "id", session.getId(),
                        "subscriptions", getSubscriptions(session)
                    ))
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
        
        // Build the response
        Map<String, Object> response = Map.of(
            "count", simpUserRegistry.getUserCount(),
            "users", userDetails
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/userDetails")
    public ResponseEntity<List<String>> getUserDetails() {
        return ResponseEntity.ok(webSocketUserService.getUserDetails());
    }
    
    private List<Map<String, String>> getSubscriptions(SimpSession session) {
        return session.getSubscriptions().stream()
            .map(subscription -> Map.of(
                "id", subscription.getId(),
                "destination", subscription.getDestination()
            ))
            .collect(Collectors.toList());
    }
}
