import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, Subject, BehaviorSubject } from 'rxjs';
import { catchError, retry, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Alert } from '../models/alert.model';
import { WebSocketService } from './websocket.service';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private apiUrl = `${environment.apiUrl}/topology/validate`;
  private alertsSubject = new BehaviorSubject<Alert[]>([]);
  private newAlertSubject = new Subject<Alert>();

  alerts$ = this.alertsSubject.asObservable();
  newAlerts$ = this.newAlertSubject.asObservable();

  private wsConnected = false;
  private pendingAlerts: Alert[] = [];

  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService
  ) {
    this.setupWebSocketListeners();
  }

  /**
   * Get alerts with filtering and pagination.
   */
  getAlerts(params?: any): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.apiUrl}/alerts`, { params }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching alerts:', error);
        return throwError(error);
      }),
      tap(alerts => {
        this.alertsSubject.next(alerts);
      })
    );
  }

  /**
   * Get unresolved alerts.
   */
  getUnresolvedAlerts(): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.apiUrl}/alerts/unresolved`).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching unresolved alerts:', error);
        return throwError(error);
      }),
      tap(alerts => {
        this.alertsSubject.next(alerts);
      })
    );
  }

  /**
   * Get alerts by severity.
   */
  getAlertsBySeverity(severity: string): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.apiUrl}/alerts/severity/${severity}`).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching alerts by severity:', error);
        return throwError(error);
      }),
      tap(alerts => {
        this.alertsSubject.next(alerts);
      })
    );
  }

  /**
   * Get alerts affecting a specific device.
   */
  getAlertsByDevice(deviceId: string): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.apiUrl}/alerts/device/${deviceId}`).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching alerts by device:', error);
        return throwError(error);
      }),
      tap(alerts => {
        this.alertsSubject.next(alerts);
      })
    );
  }

  /**
   * Get alerts affecting a specific connection.
   */
  getAlertsByConnection(connectionId: string): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.apiUrl}/alerts/connection/${connectionId}`).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching alerts by connection:', error);
        return throwError(error);
      }),
      tap(alerts => {
        this.alertsSubject.next(alerts);
      })
    );
  }

  /**
   * Get alert statistics.
   */
  getAlertStatistics(): Observable<any> {
    return this.http.get(`${this.apiUrl}/alerts/statistics`).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching alert statistics:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Resolve an alert.
   */
  resolveAlert(alertId: string, resolutionNotes: string): Observable<Alert> {
    return this.http.post<Alert>(`${this.apiUrl}/alerts/${alertId}/resolve`, {
      resolutionNotes
    }).pipe(
      catchError(error => {
        console.error('Error resolving alert:', error);
        return throwError(error);
      }),
      tap(alert => {
        // Update local alerts
        const currentAlerts = this.alertsSubject.value;
        const index = currentAlerts.findIndex(a => a.id === alert.id);
        if (index !== -1) {
          currentAlerts[index] = alert;
          this.alertsSubject.next([...currentAlerts]);
        }
      })
    );
  }

  /**
   * Unresolve an alert.
   */
  unresolveAlert(alertId: string): Observable<Alert> {
    return this.http.post<Alert>(`${this.apiUrl}/alerts/${alertId}/unresolve`, {}).pipe(
      catchError(error => {
        console.error('Error unresolving alert:', error);
        return throwError(error);
      }),
      tap(alert => {
        // Update local alerts
        const currentAlerts = this.alertsSubject.value;
        const index = currentAlerts.findIndex(a => a.id === alert.id);
        if (index !== -1) {
          currentAlerts[index] = alert;
          this.alertsSubject.next([...currentAlerts]);
        }
      })
    );
  }

  /**
   * Setup WebSocket listeners for real-time alerts.
   */
  private setupWebSocketListeners(): void {
    this.webSocketService.connect();

    this.webSocketService.messages$.subscribe(message => {
      if (message.type === 'alert.new') {
        this.handleNewAlert(message.payload);
      } else if (message.type === 'alert.resolved') {
        this.handleAlertResolution(message.payload);
      } else if (message.type === 'alert.unresolved') {
        this.handleAlertUnresolution(message.payload);
      } else if (message.type === 'validation.completed') {
        this.handleValidationCompletion(message.payload);
      } else if (message.type === 'validation.failed') {
        this.handleValidationFailure(message.payload);
      } else if (message.type === 'welcome') {
        this.handleWebSocketConnection();
      }
    });
  }

  /**
   * Handle WebSocket connection.
   */
  private handleWebSocketConnection(): void {
    this.wsConnected = true;

    // Send any pending alerts
    if (this.pendingAlerts.length > 0) {
      this.pendingAlerts.forEach(alert => {
        this.newAlertSubject.next(alert);
      });
      this.pendingAlerts = [];
    }
  }

  /**
   * Handle new alert from WebSocket.
   */
  private handleNewAlert(payload: any): void {
    const alert: Alert = {
      id: payload.alertId,
      alertType: payload.alertType,
      severity: payload.severity,
      message: payload.message,
      timestamp: new Date(payload.timestamp),
      affectedDevices: payload.affectedDevices,
      affectedConnections: payload.affectedConnections,
      resolved: payload.resolved,
      resolvedAt: payload.resolvedAt ? new Date(payload.resolvedAt) : null,
      resolutionNotes: payload.resolutionNotes
    };

    if (this.wsConnected) {
      this.newAlertSubject.next(alert);
    } else {
      // Store for later if WebSocket is not connected
      this.pendingAlerts.push(alert);
    }
  }

  /**
   * Handle alert resolution from WebSocket.
   */
  private handleAlertResolution(payload: any): void {
    const alert: Alert = {
      id: payload.alertId,
      alertType: payload.alertType,
      severity: payload.severity,
      message: payload.message,
      timestamp: new Date(payload.timestamp),
      affectedDevices: payload.affectedDevices,
      affectedConnections: payload.affectedConnections,
      resolved: payload.resolved,
      resolvedAt: payload.resolvedAt ? new Date(payload.resolvedAt) : null,
      resolutionNotes: payload.resolutionNotes
    };

    // Update local alerts
    const currentAlerts = this.alertsSubject.value;
    const index = currentAlerts.findIndex(a => a.id === alert.id);
    if (index !== -1) {
      currentAlerts[index] = alert;
      this.alertsSubject.next([...currentAlerts]);
    }
  }

  /**
   * Handle alert unresolution from WebSocket.
   */
  private handleAlertUnresolution(payload: any): void {
    const alert: Alert = {
      id: payload.alertId,
      alertType: payload.alertType,
      severity: payload.severity,
      message: payload.message,
      timestamp: new Date(payload.timestamp),
      affectedDevices: payload.affectedDevices,
      affectedConnections: payload.affectedConnections,
      resolved: payload.resolved,
      resolvedAt: payload.resolvedAt ? new Date(payload.resolvedAt) : null,
      resolutionNotes: payload.resolutionNotes
    };

    // Update local alerts
    const currentAlerts = this.alertsSubject.value;
    const index = currentAlerts.findIndex(a => a.id === alert.id);
    if (index !== -1) {
      currentAlerts[index] = alert;
      this.alertsSubject.next([...currentAlerts]);
    }
  }

  /**
   * Handle validation completion from WebSocket.
   */
  private handleValidationCompletion(payload: any): void {
    console.log('Validation completed:', payload);
    // Fetch updated alerts
    this.getAlerts().subscribe();
  }

  /**
   * Handle validation failure from WebSocket.
   */
  private handleValidationFailure(payload: any): void {
    console.error('Validation failed:', payload);
    // Optionally show error notification
  }

  /**
   * Subscribe to alerts by filter.
   */
  subscribeToAlerts(filterType: string, filterValue?: string): void {
    this.webSocketService.sendMessage({
      type: 'subscribe',
      payload: {
        filterType,
        filterValue
      }
    });
  }

  /**
   * Unsubscribe from alerts by filter.
   */
  unsubscribeFromAlerts(filterType: string, filterValue?: string): void {
    this.webSocketService.sendMessage({
      type: 'unsubscribe',
      payload: {
        filterType,
        filterValue
      }
    });
  }

  /**
   * Get color based on severity.
   */
  getSeverityColor(severity: string): string {
    switch (severity) {
      case 'CRITICAL':
        return '#f44336';
      case 'HIGH':
        return '#ff9800';
      case 'MEDIUM':
        return '#ffc107';
      case 'LOW':
        return '#2196f3';
      default:
        return '#9e9e9e';
    }
  }

  /**
   * Get icon based on severity.
   */
  getSeverityIcon(severity: string): string {
    switch (severity) {
      case 'CRITICAL':
        return 'error';
      case 'HIGH':
        return 'warning';
      case 'MEDIUM':
        return 'report_problem';
      case 'LOW':
        return 'info';
      default:
        return 'help';
    }
  }

  /**
   * Get text color based on severity.
   */
  getSeverityTextColor(severity: string): string {
    switch (severity) {
      case 'CRITICAL':
      case 'HIGH':
        return '#ffffff';
      default:
        return '#000000';
    }
  }
}
