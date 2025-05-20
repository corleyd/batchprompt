package com.batchprompt.notifications.api.config;

import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for WebSocket message broker.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final ObjectProvider<SimpUserRegistry> userRegistryProvider;
    // DefaultSimpUserRegistry
    /*
     * WebSocket's SimpUserRegistry has specific behavior we need to understand:
     * 1. Users are only added AFTER authentication is successful and the CONNECT frame is fully processed
     * 2. The registry is tied to the broker infrastructure
     * 3. It only tracks users who have established a successful connection + completed handshake
     */
    @EventListener
    public void handleSessionConnectEvent(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("Session connected [ID: {}], principal: {}", sessionId, accessor.getUser());
        log.info("Connected users after connect event: {}", userRegistryProvider.getIfAvailable().getUserCount());
        log.info("Active sessions: {}", userRegistryProvider.getIfAvailable().getUsers().stream()
            .map(user -> user.getName() + " - " + user.getSessions().size() + " sessions")
            .collect(Collectors.joining(", ")));
    }
    
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        log.info("Session subscription [ID: {}] to '{}', principal: {}", 
            sessionId, destination, accessor.getUser());
        log.info("Connected users after subscribe event: {}", userRegistryProvider.getIfAvailable().getUserCount());
        dumpUserRegistryDetails();
    }
    
    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("Session disconnected [ID: {}], principal: {}", sessionId, accessor.getUser());
        log.info("Connected users after disconnect event: {}", userRegistryProvider.getIfAvailable().getUserCount());
    }
    
    /**
     * Dumps detailed information about the user registry for debugging purposes.
     */
    private void dumpUserRegistryDetails() {
        SimpUserRegistry registry = userRegistryProvider.getIfAvailable();
        if (registry == null) {
            log.warn("SimpUserRegistry is not available");
            return;
        }
        
        log.info("User registry details:");
        log.info("  User count: {}", registry.getUserCount());
        
        registry.getUsers().forEach(user -> {
            log.info("  User: {}", user.getName());
            user.getSessions().forEach(session -> {
                log.info("    Session: {}", session.getId());
                session.getSubscriptions().forEach(subscription -> {
                    log.info("      Subscription: {} -> {}", subscription.getId(), subscription.getDestination());
                });
            });
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Topic prefix for broadcasting messages to subscribers
        // Enable /topic for public broadcasts and /user for user-specific messages
        config.enableSimpleBroker("/topic", "/queue");
        
        // Application destination prefix for messages from clients
        config.setApplicationDestinationPrefixes("/app");
        
        // Set the user destination prefix
        config.setUserDestinationPrefix("/user");
        
        // Log broker setup 
        log.info("WebSocket message broker configured with user prefix: /user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register native WebSocket endpoint without SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Allow connections from any origin for development
                // Enable SockJS fallback for browsers that don't support WebSocket
                .withSockJS();
        
        log.info("Registered WebSocket endpoint at /ws with JWT handshake interceptor");
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                log.debug(null != message ? "Received message: " + message : "Received null message");
                log.debug("user registry user count in preSend: {}", userRegistryProvider.getIfAvailable().getUserCount());
                

                /*
                 * NOTE: Must use MessageHeaderAccessor.getAccessor to get the StompHeaderAccessor. If you create a new one,
                 * then it will not be able to add the user to the user registry.
                 */

                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                StompCommand command = accessor.getCommand();
                
                if (StompCommand.CONNECT.equals(command)) {
                    log.info("Processing CONNECT frame with sessionId: {}", accessor.getSessionId());
                    
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            // Decode and validate the JWT token
                            Jwt jwt = jwtDecoder.decode(token);
                            
                            // Extract user information from JWT for better tracking
                            String userId = jwt.getSubject();
                            log.info("Authenticated user from JWT: {}", userId);
                            
                            // Create a proper JwtAuthenticationToken that Spring's security infrastructure expects
                            // This ensures consistent principal handling across the application
                            Authentication authentication = new JwtAuthenticationToken(jwt);
                            accessor.setUser(authentication);
                            
                            // Keep the session attribute for reference
                            accessor.getSessionAttributes().put("userId", userId);
                            
                            log.info("Authentication principal: {}, name: {}", 
                                authentication.getPrincipal(), authentication.getName());
                            
                            log.info("WebSocket connection authorized for user [{}] with token [{}]", 
                                userId, token.substring(0, 10) + "...");
                        } catch (Exception e) {
                            log.error("Invalid JWT token: {}", e.getMessage());
                            throw new RuntimeException("Invalid JWT token: " + e.getMessage());
                        }
                    } else {
                        log.warn("No valid Authorization header found in CONNECT frame");
                        throw new RuntimeException("Missing or invalid Authorization header");
                    }
                } else if (command != null) {
                    // Log info about other STOMP frames as well
                    log.debug("Processing {} frame for session: {}, user: {}", 
                        command, accessor.getSessionId(), accessor.getUser());
                }
                
                return message;
            }

            @Override
            public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
                log.debug("user registry user count in afterSendCompletion: {}", userRegistryProvider.getIfAvailable().getUserCount());
            }

            @Override
            public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
                log.debug("user registry user count in postSend: {}", userRegistryProvider.getIfAvailable().getUserCount());
            }

        });
    }
    
    /**
     * Log the registry class after Spring has fully initialized the application.
     * This helps us diagnose issues with the SimpUserRegistry implementation.
     */
    @EventListener
    public void onApplicationEvent(org.springframework.context.event.ContextRefreshedEvent event) {
        SimpUserRegistry registry = userRegistryProvider.getIfAvailable();
        log.info("SimpUserRegistry implementation: {}", 
            registry != null ? registry.getClass().getName() : "Not available");
        log.info("UserRegistry available: {}, implementation: {}", 
            (registry != null), 
            (registry != null ? registry.getClass().getName() : "N/A"));
    }
}
