import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { NotificationService, Notification } from '../../services/notification.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-notification-indicator',
  standalone: true,
  imports: [CommonModule, MatBadgeModule, MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <button mat-icon-button [matMenuTriggerFor]="menu" [matBadge]="unreadCount" matBadgeColor="warn" 
            [matBadgeHidden]="!connected || unreadCount === 0">
      <mat-icon>notifications</mat-icon>
    </button>
    <mat-menu #menu="matMenu" xPosition="before" class="notification-menu">
      <div class="notification-header">
        <span class="notification-title">Notifications</span>
        <button mat-button *ngIf="unreadCount > 0" (click)="markAllAsRead()">Clear all</button>
      </div>
      <div class="notification-container">
        <div *ngIf="!connected" class="notification-connection-status">
          WebSocket disconnected
        </div>
        <div *ngIf="notifications.length === 0" class="no-notifications">
          No notifications
        </div>
        <div *ngFor="let notification of notifications" class="notification-item">
          <div class="notification-content">
            <div class="notification-message">{{ notification.payload | json }}</div>
            <div class="notification-time">{{ formatTime(notification.timestamp) }}</div>
          </div>
        </div>
      </div>
    </mat-menu>
  `,
  styles: [`
    .notification-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 16px;
      border-bottom: 1px solid #eee;
    }
    .notification-title {
      font-weight: bold;
    }
    .notification-container {
      max-height: 300px;
      overflow-y: auto;
      min-width: 300px;
    }
    .notification-item {
      padding: 8px 16px;
      border-bottom: 1px solid #f5f5f5;
    }
    .notification-content {
      display: flex;
      flex-direction: column;
    }
    .notification-message {
      flex: 1;
    }
    .notification-time {
      font-size: 12px;
      color: #888;
      margin-top: 4px;
    }
    .no-notifications, .notification-connection-status {
      padding: 16px;
      text-align: center;
      color: #888;
    }
    .notification-connection-status {
      color: #f44336;
    }
  `]
})
export class NotificationIndicatorComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount = 0;
  connected = false;
  private destroy$ = new Subject<void>();
  private maxNotifications = 10;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    // Subscribe to connection status
    this.notificationService.connected
      .pipe(takeUntil(this.destroy$))
      .subscribe(connected => {
        this.connected = connected;
      });

    // Subscribe to all notifications
    this.notificationService.notifications$
      .pipe(takeUntil(this.destroy$))
      .subscribe(notification => {
        this.addNotification(notification);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  markAllAsRead(): void {
    this.notifications = [];
    this.unreadCount = 0;
  }

  formatTime(timestamp: string): string {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString();
  }

  private addNotification(notification: Notification): void {
    // Add to the beginning of the array
    this.notifications.unshift(notification);
    
    // Limit the number of notifications
    if (this.notifications.length > this.maxNotifications) {
      this.notifications = this.notifications.slice(0, this.maxNotifications);
    }
    
    this.unreadCount++;
  }
}
