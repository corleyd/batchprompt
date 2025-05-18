package com.batchprompt.notifications.api.service;

import com.batchprompt.notifications.model.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for dispatching notifications to WebSocket clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {

    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Dispatches a notification to the appropriate topic based on its type.
     * 
     * @param notification the notification to dispatch
     */
    public void dispatch(Notification notification) {
        if (notification == null) {
            log.warn("Attempted to dispatch null notification");
            return;
        }
        
        // Broadcast to all notifications topic
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        log.debug("Dispatched notification: {}", notification);
    }
}
