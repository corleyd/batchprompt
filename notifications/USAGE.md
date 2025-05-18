# BatchPrompt Notification System Usage Guide

This document provides guidance on how to use the WebSocket-based notification system in the BatchPrompt application.

## Overview

The notification system provides real-time updates when key business events occur, such as:
- Job status changes
- Task status changes  
- File status changes
- System notifications

## Server-Side Implementation

### 1. Sending Notifications from Services

Inject the `NotificationSender` in your service:

```java
@Service
public class MyService {
    private final NotificationSender notificationSender;
    
    public MyService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }
    
    public void processJob(Job job) {
        // Process job
        // ...
        
        // Send notification
        notificationSender.sendJobStatusNotification(
            job.getJobUuid(),
            "COMPLETED",
            "PROCESSING"
        );
    }
}
```

### 2. Using the Aspect-Based Approach

The recommended approach is to use the aspect-oriented notification system, which automatically detects status changes and sends notifications:

```java
// This method will trigger a notification automatically 
// via the JobNotificationAspect
@Override
public Job updateJobStatus(UUID jobUuid, JobStatus newStatus) {
    Job job = jobRepository.findById(jobUuid).orElseThrow(...);
    job.setStatus(newStatus);
    return jobRepository.save(job);
}
```

## Client-Side Implementation

### 1. Subscribing to Notifications in Components

```typescript
// In your component
import { Component, OnInit, OnDestroy } from '@angular/core';
import { NotificationService } from '../../services/notification.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-my-component',
  templateUrl: './my-component.component.html'
})
export class MyComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  constructor(private notificationService: NotificationService) {}
  
  ngOnInit(): void {
    // Subscribe to notifications for a specific job
    this.notificationService.subscribeToJob('job-uuid-here');
    
    // Listen for notifications
    this.notificationService.getJobNotifications('job-uuid-here')
      .pipe(takeUntil(this.destroy$))
      .subscribe(notification => {
        console.log('Received notification:', notification);
        // Update UI or refresh data
        this.refreshData();
      });
  }
  
  ngOnDestroy(): void {
    // Clean up subscriptions
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  refreshData(): void {
    // Refresh component data
  }
}
```

### 2. Global Notification Indicator

The application includes a notification indicator component that can be used to display notifications to the user:

```html
<!-- In your template -->
<app-notification-indicator></app-notification-indicator>
```

## Testing Notifications

You can test notifications using the test page at:
`http://localhost:8086/test/ws`

This page allows you to:
1. Connect to WebSocket with your JWT token
2. Subscribe to various notification topics
3. Send test notifications to verify client behavior

## Configuration

### Server Configuration

In your application.properties file:

```properties
# Enable or disable notifications
notifications.rest.enabled=true

# Set the notification service URL
notifications.service.url=http://localhost:8086
```

### Client Configuration

In your environment.ts file:

```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8081',
  notificationServiceUrl: 'http://localhost:8086'
};
```

## Best Practices

1. Always unsubscribe from observables in the ngOnDestroy method to prevent memory leaks
2. Use specific notification topics when possible to reduce client-side filtering
3. Keep notification payloads small - they should contain IDs and minimal status info
4. Handle WebSocket disconnections gracefully with automatic reconnection
5. Use notifications to trigger UI updates, not to replace full API responses
