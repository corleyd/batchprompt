package com.batchprompt.notifications.api.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.notifications.model.Notification;
import com.batchprompt.notifications.api.service.NotificationDispatcherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for receiving notifications from other services.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationDispatcherService notificationDispatcher;

    /**
     * Endpoint for receiving notifications from other services.
     * 
     * @param notification the notification to broadcast
     * @return 200 OK if the notification was successfully dispatched
     */
    @PostMapping
    public ResponseEntity<Void> receiveNotification(@RequestBody Notification notification) {
        log.info("Received notification: {}", notification);
        notificationDispatcher.dispatch(notification);
        return ResponseEntity.ok().build();
    }
}
