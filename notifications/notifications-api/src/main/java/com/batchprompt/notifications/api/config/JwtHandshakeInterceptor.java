package com.batchprompt.notifications.api.config;

import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor for WebSocket handshake that validates JWT tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        log.info("WebSocket handshake request received from: {} to {}", 
                request.getRemoteAddress(), request.getURI());
        
        // Log all headers for debugging
        request.getHeaders().forEach((key, values) -> {
            log.debug("Header: {} = {}", key, values);
        });
                
        String token = extractTokenFromRequest(request);
        
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket connection attempt with missing JWT token from {}", 
                    request.getRemoteAddress());
            return false;
        }
        
        try {
            Jwt jwt = jwtDecoder.decode(token);
            attributes.put("user", jwt.getSubject());
            log.info("WebSocket handshake authorized for user: {}", jwt.getSubject());
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT token in WebSocket handshake: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }
    
    private String extractTokenFromRequest(ServerHttpRequest request) {
        log.info("Extracting token from request: {}", request.getURI());
        
        // Method 1: Check for Authorization header (preferred method)
        if (request.getHeaders().containsKey("Authorization")) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            log.info("Found Authorization header: {}", authHeader != null ? "YES" : "NO");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                log.info("Found token in Authorization header");
                return authHeader.substring("Bearer ".length());
            }
        }
        
        // Method 2: Check for token in WebSocket protocol 
        // WebSocket standard allows sending additional protocols which can be used for auth
        List<String> protocols = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            log.info("Found {} WebSocket protocols", protocols.size());
            for (String protocol : protocols) {
                if (protocol != null && protocol.startsWith("Bearer ")) {
                    log.info("Found token in WebSocket protocol");
                    return protocol.substring("Bearer ".length());
                }
            }
        }
        
        // Method 3 (fallback): Check URL parameters (deprecated, less secure)
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            log.warn("Using token from URL parameters is deprecated and less secure");
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring("token=".length());
                }
            }
        }
        
        log.warn("No token found in request");
        return null;
    }
}
