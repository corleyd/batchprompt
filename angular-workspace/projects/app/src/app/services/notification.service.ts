import { Injectable, OnDestroy, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { Client, IFrame, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Subject, filter, takeUntil } from 'rxjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';

export interface Notification {
  id: string;
  notificationType: string;
  userId: string;
  timestamp: string;
  payload: any;
}


@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private authService = inject(AuthService);
  private stompClient: Client | null = null;
  private connected$ = new BehaviorSubject<boolean>(false);
  private destroy$ = new Subject<void>();
  private notificationsSubject = new Subject<Notification>();
  private reconnectTimeout: any = null;
  private subscriptions: { [key: string]: StompSubscription } = {};

  /** Observable for all notifications */
  public notifications$ = this.notificationsSubject.asObservable();

  /** Observable indicating if the WebSocket is connected */
  public connected = this.connected$.asObservable();

  constructor() {
    // Connect when authenticated
    this.authService.isAuthenticated$.pipe(
      takeUntil(this.destroy$),
      filter(isAuthenticated => isAuthenticated)
    ).subscribe(() => {
      this.connect();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.disconnect();
  }

  /**
   * Connect to the WebSocket server
   */
  public connect(): void {
    // Get token from Auth0
    this.authService.getAccessTokenSilently().subscribe({
      next: (token) => {
        try {
          const sockjsUrl = `${environment.apiBaseUrl}/ws`;

          console.log('Attempting to connect to WebSocket...');
          
          // Create WebSocket connection using native WebSockets with SockJS fallback
          this.stompClient = new Client({
            webSocketFactory: () => {
              // Direct WebSocket connection WITHOUT token in URL (more secure)
              const wsUrl = `${environment.apiBaseUrl.replace('http', 'ws')}/ws`;
              console.log('Creating direct WebSocket connection to:', wsUrl);
              return new SockJS(sockjsUrl);
            },
            // Always include token in STOMP headers too
            connectHeaders: {
              'Authorization': `Bearer ${token}`
            },
            debug: (msg) => { 
              console.debug('STOMP debug:', msg); 
              // Log more verbose information when specific errors are detected
              if (msg.includes('Lost connection')) {
                console.warn('WebSocket connection lost. Will attempt to reconnect.');
              }
              if (msg.includes('error')) {
                console.error('STOMP protocol error:', msg);
              }
            },
            onConnect: this.onConnected.bind(this),
            onStompError: (frame) => {
              console.error('STOMP protocol error:', frame.headers, frame.body);
              this.onError(frame);
            },
            onWebSocketError: (error) => {
              console.error('WebSocket error:', error);
              // Try to provide more context about the error
              console.warn('Authorization may have failed - check the server logs');
              console.warn('If using Chrome, check the Network tab for failed WebSocket connections');
              this.tryReconnect();
            },
            onDisconnect: () => {
              console.log('STOMP client explicitly disconnected');
              this.connected$.next(false);
            }
          });

          // Start the client
          this.stompClient.activate();
        } catch (error) {
          console.error('Error connecting to WebSocket:', error);
          this.tryReconnect();
        }
      },
      error: (error) => {
        console.error('Error getting access token:', error);
        this.tryReconnect();
      }
    });
  }

  /**
   * Disconnect from the WebSocket server
   */
  public disconnect(): void {
    if (this.stompClient && this.stompClient.active) {
      // First unsubscribe from all topics
      Object.values(this.subscriptions).forEach(subscription => {
        subscription.unsubscribe();
      });
      this.subscriptions = {};

      // Then disconnect
      this.stompClient.deactivate();
      console.log('Disconnected from WebSocket');
      this.connected$.next(false);
    }
    
    // Clear any pending reconnect
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
  }

  /**
   * Subscribe to all notifications
   */
  public subscribeToAllNotifications(): void {
    this.subscribeTo('/topic/notifications');
  }

  private onConnected(): void {
    console.log('Connected to WebSocket');
    this.connected$.next(true);
    
    // Re-establish any previous subscriptions
    const currentSubscriptions = { ...this.subscriptions };
    this.subscriptions = {};
    
    Object.keys(currentSubscriptions).forEach(destination => {
      this.subscribeTo(destination);
    });
    
    // Subscribe to the default topics
    this.subscribeToAllNotifications();
  }

  private onError(error: IFrame): void {
    console.error('WebSocket connection error:', error);
    this.connected$.next(false);
    this.tryReconnect();
  }

  private tryReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    
    this.reconnectTimeout = setTimeout(() => {
      console.log('Trying to reconnect to WebSocket...');
      this.connect();
    }, 5000); // Try to reconnect after 5 seconds
  }
  private subscribeTo(destination: string): void {
    if (!this.stompClient || !this.stompClient.active) {
      console.warn('Cannot subscribe, WebSocket not connected');
      return;
    }
    
    // Don't resubscribe if already subscribed
    if (this.subscriptions[destination]) {
      return;
    }
    
    this.subscriptions[destination] = this.stompClient.subscribe(
      destination,
      (message: IMessage) => {
        try {
          const notification = JSON.parse(message.body) as Notification;
          this.notificationsSubject.next(notification);
        } catch (error) {
          console.error('Error parsing notification:', error);
        }
      }
    );
  }
}
