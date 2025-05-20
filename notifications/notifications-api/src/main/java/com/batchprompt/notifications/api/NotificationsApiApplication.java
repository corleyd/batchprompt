package com.batchprompt.notifications.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** UserDestinationMessageHandler
 * Spring Boot Application for the Notifications Service.
 * This service handles real-time notifications using WebSockets.
 */
@SpringBootApplication
public class NotificationsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationsApiApplication.class, args);
    }
}
