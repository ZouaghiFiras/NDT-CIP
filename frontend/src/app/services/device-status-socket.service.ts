import { Injectable } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { environment } from '../../environments/environment';
import { DeviceStatus } from '../models/device-status.model';
import { Alert } from '../models/alert.model';
import { filter, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class DeviceStatusSocketService {
  private socket$: WebSocketSubject<any>;
  private messagesSubject = new Subject<any>();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 5000; // 5 seconds

  // Observable streams for different message types
  deviceStatusUpdates$ = this.messagesSubject.pipe(
    filter(msg => msg.type === 'device.status.update'),
    map(msg => msg.payload)
  );

  alertUpdates$ = this.messagesSubject.pipe(
    filter(msg => msg.type === 'alert.update'),
    map(msg => msg.payload)
  );

  connectionStatus$ = this.connectionStatusSubject.asObservable();

  constructor() {
    this.connect();
  }

  /**
   * Connect to the WebSocket server.
   */
  private connect(): void {
    if (this.socket$) {
      this.socket$.complete();
    }

    // Create WebSocket connection with authentication token
    const authToken = localStorage.getItem('auth_token');
    const wsUrl = `${environment.wsUrl}/ws/devices/status`;

    this.socket$ = webSocket({
      url: wsUrl,
      openObserver: {
        next: () => {
          console.log('WebSocket connection established');
          this.connectionStatusSubject.next(true);
          this.reconnectAttempts = 0;

          // Send subscription request for all devices
          this.sendMessage({
            type: 'subscribe',
            payload: {
              filterType: 'all'
            }
          });
        }
      },
      closeObserver: {
        next: () => {
          console.log('WebSocket connection closed');
          this.connectionStatusSubject.next(false);

          // Attempt to reconnect if not manually disconnected
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            console.log(`Attempting to reconnect... (${this.reconnectAttempts + 1}/${this.maxReconnectAttempts})`);
            this.reconnectAttempts++;

            setTimeout(() => {
              this.connect();
            }, this.reconnectInterval);
          } else {
            console.error('Max reconnection attempts reached');
          }
        }
      }
    });

    // Listen for messages
    this.socket$.subscribe(
      (message) => {
        this.handleMessage(message);
      },
      (error) => {
        console.error('WebSocket error:', error);
        this.connectionStatusSubject.next(false);
      }
    );
  }

  /**
   * Handle incoming WebSocket messages.
   */
  private handleMessage(message: any): void {
    console.debug('Received WebSocket message:', message);

    // Handle ping/pong
    if (message.type === 'ping') {
      this.sendMessage({ type: 'pong' });
      return;
    }

    // Handle welcome message
    if (message.type === 'welcome') {
      console.log('Received welcome message from server');
      return;
    }

    // Handle error message
    if (message.type === 'error') {
      console.error('WebSocket error:', message.message);
      return;
    }

    // Pass the message to the subject for further processing
    this.messagesSubject.next(message);
  }

  /**
   * Send a message to the WebSocket server.
   */
  private sendMessage(message: any): void {
    if (this.socket$ && this.connectionStatusSubject.value) {
      this.socket$.next(message);
    } else {
      console.warn('Cannot send message: WebSocket not connected');
    }
  }

  /**
   * Subscribe to device status updates with filters.
   * @param filterType Type of filter ('all', 'device', 'owner', 'criticality')
   * @param filterValue Value to filter by
   */
  subscribeToDeviceUpdates(filterType: string, filterValue?: string): void {
    this.sendMessage({
      type: 'subscribe',
      payload: {
        filterType,
        filterValue
      }
    });
  }

  /**
   * Unsubscribe from device status updates.
   * @param filterType Type of filter ('all', 'device', 'owner', 'criticality')
   * @param filterValue Value to filter by
   */
  unsubscribeFromDeviceUpdates(filterType: string, filterValue?: string): void {
    this.sendMessage({
      type: 'unsubscribe',
      payload: {
        filterType,
        filterValue
      }
    });
  }

  /**
   * Manually disconnect the WebSocket.
   */
  disconnect(): void {
    if (this.socket$) {
      this.socket$.complete();
      this.connectionStatusSubject.next(false);
    }
  }

  /**
   * Get the current connection status.
   */
  isConnected(): boolean {
    return this.connectionStatusSubject.value;
  }
}
