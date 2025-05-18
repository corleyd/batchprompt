package com.batchprompt.notifications.api.controller;

import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.notifications.api.service.NotificationDispatcherService;
import com.batchprompt.notifications.model.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Test controller for sending notifications during development/testing.
 */
@RestController
@RequestMapping("/test/notifications")
@RequiredArgsConstructor
@Slf4j
public class TestNotificationController {

    private final NotificationDispatcherService notificationDispatcher;
    
    /**
     * Endpoint for sending a simple system notification.
     */
    @PostMapping("/test")
    public ResponseEntity<String> sendTestSystemNotification(
            @RequestParam String message) {
        
        Notification notification = Notification.create("test", 
                new HashMap<String, Object>() {{
                    put("message", message);
                }}, null);
        
        notificationDispatcher.dispatch(notification);
        log.info("Sent test system notification: {}", notification);
        
        return ResponseEntity.ok("Sent system notification with ID: " + notification.getId());
    }
}
