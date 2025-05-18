package com.batchprompt.notifications.api.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom filter to add CORS headers for WebSocket endpoints.
 * This runs before Spring Security filters to ensure CORS headers are applied
 * even to requests that might be rejected by security.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class WebSocketCorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // Only apply this filter to WebSocket endpoints
        if (requestURI != null && requestURI.startsWith("/ws")) {
            String origin = request.getHeader("Origin");
            
            // Log the request details
            log.debug("WebSocket request received: URI={}, Origin={}, Method={}", 
                    requestURI, origin, request.getMethod());
            log.debug("Authorization header present: {}", 
                    request.getHeader("Authorization") != null ? "YES" : "NO");
                    
            // Always allow the specific origin that sent the request
            if (origin != null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
                log.debug("Setting CORS for origin: {}", origin);
            } else {
                // When no origin header is present, allow all origins
                response.setHeader("Access-Control-Allow-Origin", "*");
                log.debug("No origin header, setting CORS allow all");
            }
            
            // Allow all methods that WebSockets might use
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            
            // Allow all headers, especially Authorization
            response.setHeader("Access-Control-Allow-Headers", "*");
            
            // Cache preflight response for 1 hour
            response.setHeader("Access-Control-Max-Age", "3600");
            
            // Handle preflight requests
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                log.debug("Handling OPTIONS preflight request");
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
