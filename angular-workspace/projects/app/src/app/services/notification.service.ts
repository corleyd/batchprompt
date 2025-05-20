import { Injectable, OnDestroy, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { Client, IFrame, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject, filter, takeUntil, finalize, share, shareReplay } from 'rxjs';
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
  private reconnectTimeout: any = null;
  private subscriptions: { [key: string]: StompSubscription } = {};
  private topicSubjects: { [key: string]: Subject<any> } = {};
  private topicSubscriberCount: { [key: string]: number } = {};
  private topicObservables: { [key: string]: Observable<any> } = {};

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
    // Signal to all observers that we're shutting down
    this.destroy$.next();
    this.destroy$.complete();
    
    // Close all topic subjects
    Object.values(this.topicSubjects).forEach(subject => {
      subject.complete();
    });
    this.topicSubjects = {};
    
    // Disconnect from the WebSocket
    this.disconnect();
  }

  /**
   * Connect to the WebSocket server
   */
  public connect(): void {
    // Don't create a new connection if we're already connected or connecting
    if (this.stompClient && this.stompClient.active) {
      console.log('WebSocket connection already active, skipping connect');
      return;
    }
    
    // Cleanup any existing client that might not be fully disconnected
    if (this.stompClient) {
      console.log('Cleaning up existing STOMP client before creating a new one');
      this.disconnect();
    }
    
    // Get token from Auth0
    this.authService.getAccessTokenSilently().subscribe({
      next: (token) => {
        try {
          const sockjsUrl = `${environment.apiBaseUrl}/ws`;

          console.log('Attempting to connect to WebSocket...');
          
          // Create WebSocket connection using SockJS
          this.stompClient = new Client({
            webSocketFactory: () => {
              // Use SockJS for better browser compatibility
              console.log('Creating SockJS connection to:', sockjsUrl);
              return new SockJS(sockjsUrl);
            },
            // Always include token in STOMP headers
            connectHeaders: {
              'Authorization': `Bearer ${token}`,
              'client-id': `angular-client-${new Date().getTime()}`
            },
            debug: (msg) => { 
              console.debug('STOMP debug:', msg); 
              // Log more verbose information when specific errors are detected
              if (msg.includes('Lost connection')) {
                console.warn('WebSocket connection lost. Will attempt to reconnect.');
              }
              if (msg.includes('error') || msg.includes('Error')) {
                console.error('STOMP protocol error:', msg);
              }
            },
            onConnect: (frame) => {
              console.log('Connected to WebSocket with frame:', frame);
              this.onConnected();
            },
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
    // First unsubscribe from all topics
    Object.values(this.subscriptions).forEach(subscription => {
      try {
        subscription.unsubscribe();
      } catch (e) {
        console.warn('Error unsubscribing from topic:', e);
      }
    });
    this.subscriptions = {};

    // Clear all subscribers
    this.topicSubscriberCount = {};
    this.topicObservables = {};
    
    // Then disconnect the client if it exists
    if (this.stompClient) {
      try {
        if (this.stompClient.active) {
          this.stompClient.deactivate();
          console.log('Disconnected from WebSocket');
        }
        // Clear the client reference
        this.stompClient = null;
      } catch (e) {
        console.error('Error disconnecting from WebSocket:', e);
      }
      this.connected$.next(false);
    }
    
    // Clear any pending reconnect
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
  }

  private onConnected(): void {
    console.log('Connected to WebSocket');
    this.connected$.next(true);
    
    // Re-establish any previous subscriptions
    this.subscriptions = {};

    let topics = Object.keys(this.topicSubscriberCount).filter(topic => this.topicSubscriberCount[topic] > 0);
    
    // Add a small delay before resubscribing to ensure the connection is fully established
    setTimeout(() => {
      topics.forEach(destination => {
        if (this.topicSubscriberCount[destination] > 0 && !this.subscriptions[destination]) {
          console.info(`Re-subscribing to STOMP topic: ${destination}`);
          this.internalSubscribe(destination);
        }
      });
    }, 100); // Small delay of 100ms

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
  private internalSubscribe(destination: string): void {
    if (!this.stompClient || !this.stompClient.active) {
      console.warn('Cannot subscribe, WebSocket not connected');
      return;
    }
    
    // Don't resubscribe if already subscribed
    if (this.subscriptions[destination]) {
      console.info(`Already subscribed to ${destination}`);
      return;
    }
    
    console.info(`Subscribing to STOMP destination: ${destination}`);
    try {
      this.subscriptions[destination] = this.stompClient.subscribe(
        destination,
        (message: IMessage) => {
          try {
            console.log(`Received message on ${destination}:`, message);
            
            // Extract message data
            const body = message.body;
            const headers = message.headers;
            const subscription = headers['subscription'];
            const messageId = headers['message-id'];
            
            console.log(`Message details: id=${messageId}, subscription=${subscription}`);
            console.log(`Message body: ${body}`);
            
            // Parse the notification
            const notification = JSON.parse(body) as Notification;
            
            // Debug notification details
            console.log(`Notification details: type=${notification.notificationType}, userId=${notification.userId}`);
            
            // Push the notification to the corresponding topic's subject
            this.getTopicSubject(destination).next(notification);
          } catch (error) {
            console.error(`Error processing notification on ${destination}:`, error);
            console.error(`Raw message body:`, message.body);
          }
        },
        { 
          // Include custom headers for debugging
          'client-id': `angular-client-${new Date().getTime()}`
        }
      );
      console.log(`Successfully subscribed to ${destination}`);
    } catch (error) {
      console.error(`Error subscribing to ${destination}:`, error);
    }
  }

  /**
   * Get the subject for a specific topic, creating it if it doesn't exist
   */
  private getTopicSubject(topic: string): Subject<any> {
    if (!this.topicSubjects[topic]) {
      this.topicSubjects[topic] = new Subject<any>();
    }
    return this.topicSubjects[topic];
  }

  /**
   * Subscribe to a specific topic and get an Observable for that topic
   * @param topic The topic/destination to subscribe to
   * @returns An Observable that emits messages from the specified topic
   */
  public subscribeTo<T = Notification>(topic: string): Observable<T> {
    // Format the destination according to Spring STOMP expectations
    let destination = topic;
    
    /* 
     * With Spring's STOMP implementation using convertAndSendToUser:
     * 1. Server sends to: /user/{userId}/{destination}
     * 2. Client subscribes to: /user/{destination}
     * 3. Spring's broker does the routing based on the authenticated principal
     * 
     * In addition, it is CRITICAL to prepend /topic/ to the destination. 
     * The code below ensures that the destination is always in the correct format.
     */

    if (destination.startsWith('/user/')) {
      // If the destination starts with /user/, remove the /user
      destination = destination.substring(5);
    }

    if (destination.startsWith('/topic/')) {
      // If the destination starts with /topic/, remove the /topic
      destination = destination.substring(6);
    }

    // Remove the leading slash if it exists
    if (destination.startsWith('/')) {
      destination = destination.substring(1);
    }

    // Finally, prepend /user/topic/ to the cleaned destination
    destination = `/user/topic/${destination}`;
    
    console.log(`Subscribing to topic: ${topic}`);
    console.log(`Using full STOMP destination: ${destination}`);
    
    // Create a cached observable if it doesn't exist
    if (!this.topicObservables[destination]) {
      console.log(`Creating observable for destination: ${destination}`);
      
      // Initialize the subscriber count
      this.topicSubscriberCount[destination] = 0;
      
      // Create a shared observable that tracks subscriptions
      this.topicObservables[destination] = this.getTopicSubject(destination).asObservable().pipe(
        // Use shareReplay(1) to ensure late subscribers get the most recent value
        shareReplay({ bufferSize: 1, refCount: true }),
        // Track when subscribers are added/removed
        finalize(() => {
          // This will be called when all subscribers to this topic are gone
          this.unsubscribeFromTopic(destination);
        })
      );
    }
    
    // Increment subscriber count
    this.topicSubscriberCount[destination] = (this.topicSubscriberCount[destination] || 0) + 1;
    
    // Make sure we're subscribed to the STOMP topic
    if (this.connected$.value) {
      this.internalSubscribe(destination);
    }
    
    // Return an observable that tracks when this specific subscriber unsubscribes
    return new Observable<T>(observer => {
      // Connect the observer to the shared observable
      const subscription = this.topicObservables[destination].subscribe(observer);
      
      // Return a teardown function that runs when this subscriber unsubscribes
      return () => {
        // First unsubscribe from the shared observable
        subscription.unsubscribe();
        
        // Decrement subscriber count
        this.topicSubscriberCount[destination] = (this.topicSubscriberCount[destination] || 1) - 1;
        
        // If this was the last subscriber, clean up
        if (this.topicSubscriberCount[destination] <= 0) {
          this.unsubscribeFromTopic(destination);
          delete this.topicSubscriberCount[destination];
          delete this.topicObservables[destination];
        }
      };
    });
  }
  
  /**
   * Unsubscribe from a STOMP topic when it's no longer needed
   */
  private unsubscribeFromTopic(topic: string): void {
    // Unsubscribe from the STOMP topic
    if (this.subscriptions[topic]) {
      console.log(`Unsubscribing from STOMP topic: ${topic}`);
      this.subscriptions[topic].unsubscribe();
      delete this.subscriptions[topic];
    }
  }
}
