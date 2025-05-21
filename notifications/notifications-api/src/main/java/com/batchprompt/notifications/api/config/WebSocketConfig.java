package com.batchprompt.notifications.api.config;

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
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

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
    
    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("Session disconnected [ID: {}], principal: {}", sessionId, accessor.getUser());
        log.info("Connected users after disconnect event: {}", userRegistryProvider.getIfAvailable().getUserCount());
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register native WebSocket endpoint without SockJS
        registry.addEndpoint("/ws")
                // Allow connections from any origin for development
                .setAllowedOriginPatterns("*")  
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
        });
    }
}
