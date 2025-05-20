package com.batchprompt.notifications.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for tracking WebSocket users and their subscriptions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketUserService {

    private final SimpUserRegistry userRegistry;
    
    /**
     * Get the current number of connected WebSocket users.
     *
     * @return the number of connected users
     */
    public int getUserCount() {
        return userRegistry.getUserCount();
    }
    
    /**
     * Get a list of all connected users and their details.
     *
     * @return a list of user information
     */
    public List<String> getUserDetails() {
        List<String> userDetails = new ArrayList<>();
        
        for (SimpUser user : userRegistry.getUsers()) {
            StringBuilder details = new StringBuilder();
            details.append("User: ").append(user.getName()).append("\n");
            
            for (SimpSession session : user.getSessions()) {
                details.append("  Session: ").append(session.getId()).append("\n");
                
                for (SimpSubscription subscription : session.getSubscriptions()) {
                    details.append("    Subscription: ").append(subscription.getId())
                          .append(" to ").append(subscription.getDestination())
                          .append("\n");
                }
            }
            
            userDetails.add(details.toString());
        }
        
        return userDetails;
    }
    
    /**
     * Find a user by their principal name.
     *
     * @param name the principal name to search for
     * @return the SimpUser if found, or null
     */
    public SimpUser findUserByName(String name) {
        return userRegistry.getUser(name);
    }
    
    /**
     * Check if a user is connected via WebSocket.
     *
     * @param username the username to check
     * @return true if the user is connected, false otherwise
     */
    public boolean isUserConnected(String username) {
        return userRegistry.getUser(username) != null;
    }
    
    /**
     * Create user attributes for a WebSocket session.
     *
     * @param userId the user ID to associate with the session
     * @return a header accessor with user information
     */
    public SimpMessageHeaderAccessor createUserHeaders(String userId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(userId);
        // Use the same principal type as in the WebSocketConfig
        headerAccessor.setUser(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
            org.springframework.security.oauth2.jwt.Jwt.withTokenValue("dummy")
                .header("alg", "none")
                .claim("sub", userId)
                .build()
        ));
        return headerAccessor;
    }
}
