package com.batchprompt.notifications.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to handle SockJS info endpoint requests.
 * This provides the needed information for SockJS to establish a connection.
 */
@RestController
// Remove the @CrossOrigin annotation since we're setting headers manually in the method
@Slf4j
public class SockJsInfoController {

    /**
     * Handles SockJS info requests.
     * This endpoint returns information about the server's WebSocket capabilities
     * to the SockJS client during the handshake process.
     * 
     * @param token Authorization token (optional)
     * @return SockJS server information
     */
    @GetMapping("/ws/info")
    public ResponseEntity<Map<String, Object>> getWsInfo(@RequestParam(required = false) String token) {
        log.info("Handling /ws/info request with token present: {}", token != null);
        
        // Return the standard SockJS info response
        Map<String, Object> info = new HashMap<>();
        info.put("websocket", true);
        info.put("origins", new String[]{"*"});
        info.put("cookie_needed", false);
        info.put("entropy", Math.round(Math.random() * 2147483647));
        
        // Return with appropriate headers for CORS
        return ResponseEntity.status(HttpStatus.OK)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "DNT, X-CustomHeader, Keep-Alive, User-Agent, X-Requested-With, If-Modified-Since, Cache-Control, Content-Type, Authorization")
                .header("Access-Control-Max-Age", "1728000")
                .body(info);
    }
}
