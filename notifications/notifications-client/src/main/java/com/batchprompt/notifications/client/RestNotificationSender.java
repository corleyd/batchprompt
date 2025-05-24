package com.batchprompt.notifications.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.common.client.ClientAuthenticationService;
import com.batchprompt.notifications.model.Notification;

import lombok.extern.slf4j.Slf4j;

/**
 * REST client implementation of the notification sender.
 * This sends notifications to a centralized notification service via HTTP.
 */
@Component
@ConditionalOnProperty(name = "services.notifications.rest.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RestNotificationSender implements NotificationSender {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;
    private final ClientAuthenticationService authService;
    
    public RestNotificationSender(
            ClientAuthenticationService authService,
            @Value("${services.notifications.url}") String notificationServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.notificationServiceUrl = notificationServiceUrl;
        this.authService = authService;
        log.info("Initialized REST notification sender with service URL: {}", notificationServiceUrl);
    }
    
    /**
     * Sends a notification to the notification service.
     * 
     * @param notificationType The type of the event
     * @param payload The payload of the notification
     */
    @Override
    public void send(String notificationType, Object payload, String userId) {

        // check to see if a transaction is in progress

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // if so, add the notification to the transaction
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendInternal(notificationType, payload, userId);
                }
            });
        } else {
            // if not, send the notification immediately
            sendInternal(notificationType, payload, userId);
        }
    }

    public void sendInternal(String notificationType, Object payload, String userId) {
        try {
            String url = notificationServiceUrl + "/api/notifications";
            HttpHeaders headers = authService.createAuthHeaders(null);
            Notification notification = Notification.create(notificationType, payload, userId);
            HttpEntity<Notification> requestEntity = new HttpEntity<>(notification, headers);

            log.debug("Sending notification: {}", notification);

            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Void.class
            );            
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
