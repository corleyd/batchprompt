package com.batchprompt.notifications.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for WebSocket monitoring and diagnostics.
 * This provides tools to monitor WebSocket connections without
 * interfering with Spring's default implementation.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketRegistryConfig {

    /**
     * Bean to monitor WebSocket statistics, including connections and subscriptions.
     * This helps diagnose issues with user registration and tracking.
     */
    @Bean
    public WebSocketStatsMonitor webSocketStatsMonitor(WebSocketMessageBrokerStats stats, SimpUserRegistry userRegistry) {
        return new WebSocketStatsMonitor(stats, userRegistry);
    }
    
    /**
     * Monitor class for WebSocket statistics.
     * Provides diagnostic information about WebSocket connections and sessions.
     */
    @Slf4j
    public static class WebSocketStatsMonitor {
        private final WebSocketMessageBrokerStats stats;
        private final SimpUserRegistry userRegistry;
        
        public WebSocketStatsMonitor(WebSocketMessageBrokerStats stats, SimpUserRegistry userRegistry) {
            this.stats = stats;
            this.userRegistry = userRegistry;
            
            // Log initial stats on startup
            logStats();
        }
        
        /**
         * Log current WebSocket statistics.
         * Helps diagnose issues with connections and sessions.
         */
        public void logStats() {
            log.info("WebSocket Stats:");
            log.info("  Connected users: {}", userRegistry.getUserCount());
            log.info("  WebSocket sessions: {}", stats.getWebSocketSessionStats());
            log.info("  STOMP broker relay: {}", stats.getStompBrokerRelayStats());
            log.info("  STOMP broker: {}", stats.getStompBrokerRelayStats());
            log.info("  STOMP client inbound: {}", stats.getClientInboundExecutorStatsInfo()); 
            log.info("  STOMP client outbound: {}", stats.getClientOutboundExecutorStatsInfo());
            log.info("  STOMP sub/unsub: {}", stats.getStompSubProtocolStats());
        }
    }
}
