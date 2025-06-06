package com.batchprompt.notifications.api.service;

import com.batchprompt.notifications.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Service for dispatching notifications to WebSocket clients and handling email notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {

    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    /**
     * Dispatches a notification to the appropriate topic based on its type.
     * Also handles email notifications for specific types.
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
        
        // Handle email notifications for waitlist events
        handleEmailNotification(notificationType, notification);
        
        // Handle WebSocket notification (existing functionality)
        handleWebSocketNotification(notificationType, notification);
    }
    
    private void handleEmailNotification(String notificationType, Notification notification) {
        if (emailService == null) {
            log.debug("Email service not available, skipping email notification");
            return;
        }
        
        try {
            Map<String, Object> payload = (Map<String, Object>) notification.getPayload();
            
            switch (notificationType) {
                case "waitlist.signup":
                    String email = (String) payload.get("email");
                    String name = (String) payload.get("name");
                    Integer position = (Integer) payload.get("position");
                    emailService.sendWaitlistSignupEmail(email, name, position);
                    break;
                    
                case "waitlist.invitation":
                    String inviteEmail = (String) payload.get("email");
                    String inviteName = (String) payload.get("name");
                    String company = (String) payload.get("company");
                    emailService.sendWaitlistInvitationEmail(inviteEmail, inviteName, company);
                    break;
                    
                default:
                    log.debug("No email handler for notification type: {}", notificationType);
            }
        } catch (Exception e) {
            log.error("Error sending email notification for type: {}", notificationType, e);
        }
    }
    
    private void handleWebSocketNotification(String notificationType, Notification notification) {
        String destination = "/topic/" + notificationType;
        log.debug("Dispatching WebSocket notification to destination: {}", destination);
        try {
            messagingTemplate.convertAndSendToUser(notification.getUserId(), destination, notification);
        } catch (Exception e) {
            log.error("Error dispatching WebSocket notification: {}", e.getMessage(), e);
        }
    }
}
