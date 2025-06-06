import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface UiNotification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class UiNotificationService {
  private notifications = new BehaviorSubject<UiNotification[]>([]);
  public notifications$ = this.notifications.asObservable();

  private nextId = 1;

  showSuccess(message: string) {
    this.addNotification('success', message);
  }

  showError(message: string) {
    this.addNotification('error', message);
  }

  showWarning(message: string) {
    this.addNotification('warning', message);
  }

  showInfo(message: string) {
    this.addNotification('info', message);
  }

  private addNotification(type: UiNotification['type'], message: string) {
    const notification: UiNotification = {
      id: (this.nextId++).toString(),
      type,
      message,
      timestamp: new Date()
    };

    const current = this.notifications.value;
    this.notifications.next([...current, notification]);

    // Auto-remove after 5 seconds
    setTimeout(() => {
      this.removeNotification(notification.id);
    }, 5000);
  }

  removeNotification(id: string) {
    const current = this.notifications.value;
    this.notifications.next(current.filter(n => n.id !== id));
  }

  clearAll() {
    this.notifications.next([]);
  }
}