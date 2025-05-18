package com.batchprompt.notifications.api.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket controller for handling client messages.
 */
@Controller
@Slf4j
public class WebSocketController {

    /**
     * Handles client connection messages and sends back an acknowledgement.
     * 
     * @param message the client message
     * @return an acknowledgement message
     */
    @MessageMapping("/connect")
    @SendTo("/topic/ack")
    public String handleConnect(String message) {
        log.info("Received connection message: {}", message);
        return "Connected to notification service at " + java.time.Instant.now();
    }
}
