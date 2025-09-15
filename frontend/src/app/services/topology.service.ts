import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, Subject } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { DeviceStatus } from '../models/device-status.model';
import { Connection } from '../models/connection.model';
import { TopologySnapshot } from '../models/topology-snapshot.model';
import { WebSocketService } from './websocket.service';

@Injectable({
  providedIn: 'root'
})
export class TopologyService {
  private apiUrl = `${environment.apiUrl}/topology`;
  private topologyUpdateSubject = new Subject<any>();

  topologyUpdates$ = this.topologyUpdateSubject.asObservable();

  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService
  ) {
    this.setupWebSocketListeners();
  }

  /**
   * Get the full network topology.
   */
  getTopology(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}`).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching topology:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get a specific connection by ID.
   */
  getConnection(id: string): Observable<Connection> {
    return this.http.get<Connection>(`${this.apiUrl}/${id}`).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching connection:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Create a new connection.
   */
  createConnection(connectionData: any): Observable<Connection> {
    return this.http.post<Connection>(`${this.apiUrl}`, connectionData).pipe(
      catchError(error => {
        console.error('Error creating connection:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Update an existing connection.
   */
  updateConnection(id: string, connectionData: any): Observable<Connection> {
    return this.http.put<Connection>(`${this.apiUrl}/${id}`, connectionData).pipe(
      catchError(error => {
        console.error('Error updating connection:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Delete a connection.
   */
  deleteConnection(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`).pipe(
      catchError(error => {
        console.error('Error deleting connection:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get a list of connections with filtering and pagination.
   */
  getConnections(params?: any): Observable<any> {
    return this.http.get(`${this.apiUrl}/connections`, { params }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching connections:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get topology snapshots.
   */
  getTopologySnapshots(params?: any): Observable<any> {
    return this.http.get(`${this.apiUrl}/snapshots`, { params }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching topology snapshots:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Restore topology from a snapshot.
   */
  restoreTopologySnapshot(snapshotId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/snapshots/${snapshotId}/restore`, {}).pipe(
      catchError(error => {
        console.error('Error restoring topology snapshot:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Check if adding a connection would create a loop.
   */
  validateConnectionLoop(sourceDeviceId: string, targetDeviceId: string): Observable<boolean> {
    return this.http.post<{ wouldCreateLoop: boolean }>(`${this.apiUrl}/validate-loop`, {
      sourceDeviceId,
      targetDeviceId
    }).pipe(
      map(response => response.wouldCreateLoop),
      catchError(error => {
        console.error('Error validating connection loop:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Find the shortest path between two devices.
   */
  findShortestPath(sourceDeviceId: string, targetDeviceId: string): Observable<string[]> {
    return this.http.post<{ path: string[] }>(`${this.apiUrl}/path`, {
      sourceDeviceId,
      targetDeviceId
    }).pipe(
      map(response => response.path),
      catchError(error => {
        console.error('Error finding shortest path:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Export topology to JSON.
   */
  exportTopology(): Observable<any> {
    return this.http.get(`${this.apiUrl}/export`, {
      responseType: 'text'
    }).pipe(
      catchError(error => {
        console.error('Error exporting topology:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Setup WebSocket listeners for topology updates.
   */
  private setupWebSocketListeners(): void {
    this.webSocketService.connect();

    this.webSocketService.messages$.subscribe(message => {
      if (message.type === 'topology.connection.update') {
        this.handleConnectionUpdate(message.payload);
      } else if (message.type === 'topology.snapshot.update') {
        this.handleSnapshotUpdate(message.payload);
      }
    });
  }

  /**
   * Handle connection updates from WebSocket.
   */
  private handleConnectionUpdate(connectionData: any): void {
    this.topologyUpdateSubject.next({
      type: 'connection_update',
      payload: connectionData
    });
  }

  /**
   * Handle topology snapshot updates from WebSocket.
   */
  private handleSnapshotUpdate(snapshotData: any): void {
    this.topologyUpdateSubject.next({
      type: 'snapshot_update',
      payload: snapshotData
    });
  }

  /**
   * Emit a topology update event.
   */
  emitTopologyUpdate(updateType: string, payload: any): void {
    this.topologyUpdateSubject.next({
      type: updateType,
      payload: payload
    });
  }
}
