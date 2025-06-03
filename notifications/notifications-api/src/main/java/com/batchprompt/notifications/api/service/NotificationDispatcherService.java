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
     * @param notification the notification to dispatch
     */
    public void dispatch(Notification notification) {
        if (notification == null) {
            log.warn("Attempted to dispatch null notification");
            return;
        }
        
        // Get the notification type without any leading slash
        String notificationType = notification.getNotificationType();
        if (notificationType.startsWith("/")) {
            notificationType = notificationType.substring(1);
        }
        
        // Log basic notification information
        log.info("Dispatching notification: type={}, userId={}", notificationType, notification.getUserId());
        
        // For debugging: log the current architecture and formats

        String destination = "/topic/" + notificationType;
        log.debug("Dispatching notification to destination: {}", destination);
        try {
            messagingTemplate.convertAndSendToUser(notification.getUserId(), destination, notification);
        } catch (Exception e) {
            log.error("Error dispatching notification: {}", e.getMessage(), e);
        }

    }
}
