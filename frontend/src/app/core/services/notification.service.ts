import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: 'success' | 'info' | 'warning' | 'error';
  timestamp: Date;
  read: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notifications: Notification[] = [];
  private notificationsSubject = new Subject<Notification[]>();
  private notificationAddedSubject = new Subject<Notification>();
  private notificationClearedSubject = new Subject<void>();

  /**
   * Get all notifications
   */
  getNotifications(): Observable<Notification[]> {
    return this.notificationsSubject.asObservable();
  }

  /**
   * Get unread notifications count
   */
  getUnreadCount(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  /**
   * Initialize notification service
   */
  init(): void {
    // Load notifications from storage if available
    const storedNotifications = localStorage.getItem('notifications');
    if (storedNotifications) {
      try {
        this.notifications = JSON.parse(storedNotifications).map((n: any) => ({
          ...n,
          timestamp: new Date(n.timestamp)
        }));
        this.notificationsSubject.next([...this.notifications]);
      } catch (e) {
        console.error('Failed to load notifications from storage', e);
      }
    }

    // Listen for storage changes to sync notifications across tabs
    window.addEventListener('storage', (event) => {
      if (event.key === 'notifications') {
        try {
          const storedNotifications = event.newValue ? JSON.parse(event.newValue) : [];
          this.notifications = storedNotifications.map((n: any) => ({
            ...n,
            timestamp: new Date(n.timestamp)
          }));
          this.notificationsSubject.next([...this.notifications]);
        } catch (e) {
          console.error('Failed to sync notifications from storage', e);
        }
      }
    });
  }

  /**
   * Add a new notification
   * @param notification Notification object
   */
  addNotification(notification: Omit<Notification, 'id' | 'timestamp' | 'read'>): void {
    const newNotification: Notification = {
      ...notification,
      id: Date.now(),
      timestamp: new Date(),
      read: false
    };

    this.notifications.unshift(newNotification);
    this.saveNotifications();
    this.notificationsSubject.next([...this.notifications]);
    this.notificationAddedSubject.next(newNotification);

    // Auto-remove notification after 5 seconds if it's not a warning or error
    if (notification.type === 'success' || notification.type === 'info') {
      setTimeout(() => {
        this.removeNotification(newNotification.id);
      }, 5000);
    }
  }

  /**
   * Show success notification
   * @param title Notification title
   * @param message Notification message
   */
  success(title: string, message: string): void {
    this.addNotification({
      type: 'success',
      title,
      message
    });
  }

  /**
   * Show info notification
   * @param title Notification title
   * @param message Notification message
   */
  info(title: string, message: string): void {
    this.addNotification({
      type: 'info',
      title,
      message
    });
  }

  /**
   * Show warning notification
   * @param title Notification title
   * @param message Notification message
   */
  warning(title: string, message: string): void {
    this.addNotification({
      type: 'warning',
      title,
      message
    });
  }

  /**
   * Show error notification
   * @param title Notification title
   * @param message Notification message
   */
  error(title: string, message: string): void {
    this.addNotification({
      type: 'error',
      title,
      message
    });
  }

  /**
   * Remove notification by ID
   * @param id Notification ID
   */
  removeNotification(id: number): void {
    this.notifications = this.notifications.filter(n => n.id !== id);
    this.saveNotifications();
    this.notificationsSubject.next([...this.notifications]);
  }

  /**
   * Mark notification as read
   * @param id Notification ID
   */
  markAsRead(id: number): void {
    const notification = this.notifications.find(n => n.id === id);
    if (notification) {
      notification.read = true;
      this.saveNotifications();
      this.notificationsSubject.next([...this.notifications]);
    }
  }

  /**
   * Mark all notifications as read
   */
  markAllAsRead(): void {
    this.notifications.forEach(n => n.read = true);
    this.saveNotifications();
    this.notificationsSubject.next([...this.notifications]);
  }

  /**
   * Clear all notifications
   */
  clearNotifications(): void {
    this.notifications = [];
    this.saveNotifications();
    this.notificationsSubject.next([...this.notifications]);
    this.notificationClearedSubject.next();
  }

  /**
   * Save notifications to localStorage
   */
  private saveNotifications(): void {
    try {
      localStorage.setItem('notifications', JSON.stringify(this.notifications));
    } catch (e) {
      console.error('Failed to save notifications to storage', e);
    }
  }

  /**
   * Get observable for when a notification is added
   */
  onNotificationAdded(): Observable<Notification> {
    return this.notificationAddedSubject.asObservable();
  }

  /**
   * Get observable for when notifications are cleared
   */
  onNotificationsCleared(): Observable<void> {
    return this.notificationClearedSubject.asObservable();
  }

  /**
   * Get notifications by type
   * @param type Notification type
   */
  getNotificationsByType(type: Notification['type']): Observable<Notification[]> {
    return this.getNotifications().pipe(
      filter(notifications => notifications.some(n => n.type === type))
    );
  }
}
