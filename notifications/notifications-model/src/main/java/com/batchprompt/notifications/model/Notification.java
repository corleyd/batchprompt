package com.batchprompt.notifications.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base notification model for WebSocket messages.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    /**
     * Unique identifier for the notification.
     */
    private UUID id;

    
    /**
     * The ID of the user associated with the notification.
     */
    private String userId;
    
    /**
     * The type of notification event.
     */
    private String notificationType;
    
    /**
     * When the notification was created.
     */
    private Instant timestamp;
    
    /**
     * Optional message providing additional details.
     */
    private Object payload;
    
    /**
     * Creates a new notification with a random ID and current timestamp.
     * 
     * @param type the notification type
     * @param message optional message details
     * @return the new notification
     */
    public static Notification create(String notificationType, Object payload, String userId) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .notificationType(notificationType)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
}
