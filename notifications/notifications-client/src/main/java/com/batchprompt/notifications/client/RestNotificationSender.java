package com.batchprompt.notifications.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.notifications.model.Notification;

import lombok.extern.slf4j.Slf4j;

/**
 * REST client implementation of the notification sender.
 * This sends notifications to a centralized notification service via HTTP.
 */
@Component
@ConditionalOnProperty(name = "notifications.rest.enabled", havingValue = "true")
@Slf4j
public class RestNotificationSender implements NotificationSender {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;
    
    public RestNotificationSender(
            @Value("${notifications.service.url}") String notificationServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.notificationServiceUrl = notificationServiceUrl;
        log.info("Initialized REST notification sender with service URL: {}", notificationServiceUrl);
    }
    
    /**
     * Sends a notification to the notification service.
     * 
     * @param eventType The type of the event
     * @param payload The payload of the notification
     */
    @Override
    public void send(String eventType, Object payload, String userId) {
        try {
            String url = notificationServiceUrl + "/api/notifications";
            Notification notification = Notification.create(eventType, payload, userId);
            ResponseEntity<Void> response = restTemplate.postForEntity(url, notification, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Successfully sent notification: {}", notification);
            } else {
                log.warn("Failed to send notification, status code: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending notification", e);
        }
    }
}
