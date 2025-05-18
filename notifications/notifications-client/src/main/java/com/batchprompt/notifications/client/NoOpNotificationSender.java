package com.batchprompt.notifications.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * No-op implementation of the notification sender.
 * This is used when notifications are disabled.
 */
@Component
@ConditionalOnProperty(name = "notifications.rest.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NoOpNotificationSender implements NotificationSender {

    public NoOpNotificationSender() {
        log.info("Notifications are disabled. Using no-op implementation.");
    }

    @Override
    public void send(String notificationType, Object payload, String userId) {
        // No operation performed
        log.debug("No-op notification sender. Notification type: {}, payload: {}", notificationType, payload);
    }
    

}
