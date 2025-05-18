package com.batchprompt.notifications.client;

/**
 * Client interface for sending real-time notifications.
 * This interface abstracts the notification mechanism and can be implemented
 * in various ways (WebSocket, REST API, Message Queue, etc.)
 */
public interface NotificationSender {

    /**
     * Send a generic notification
     * 
     * @param notification the notification to send
     */
    void send(String notificationType, Object payload, String userId);
}
