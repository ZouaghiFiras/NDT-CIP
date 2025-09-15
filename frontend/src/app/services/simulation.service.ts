import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, Subject } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { SimulationScenario } from '../models/simulation-scenario.model';
import { WebSocketService } from './websocket.service';

@Injectable({
  providedIn: 'root'
})
export class SimulationService {
  private apiUrl = `${environment.apiUrl}/simulations`;
  private simulationUpdateSubject = new Subject<any>();

  simulationUpdates$ = this.simulationUpdateSubject.asObservable();

  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService
  ) {
    this.setupWebSocketListeners();
  }

  /**
   * Create a new simulation scenario.
   */
  createSimulation(scenario: SimulationScenario): Observable<SimulationScenario> {
    return this.http.post<SimulationScenario>(`${this.apiUrl}`, scenario).pipe(
      catchError(error => {
        console.error('Error creating simulation scenario:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get all simulation scenarios with pagination and filtering.
   */
  getSimulations(params?: any): Observable<SimulationScenario[]> {
    return this.http.get<SimulationScenario[]>(`${this.apiUrl}`, { params }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching simulation scenarios:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get a specific simulation scenario by ID.
   */
  getSimulation(id: string): Observable<SimulationScenario> {
    return this.http.get<SimulationScenario>(`${this.apiUrl}/${id}`).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching simulation scenario:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Update a simulation scenario.
   */
  updateSimulation(id: string, scenario: SimulationScenario): Observable<SimulationScenario> {
    return this.http.put<SimulationScenario>(`${this.apiUrl}/${id}`, scenario).pipe(
      catchError(error => {
        console.error('Error updating simulation scenario:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Delete a simulation scenario.
   */
  deleteSimulation(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`).pipe(
      catchError(error => {
        console.error('Error deleting simulation scenario:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Execute a simulation scenario.
   */
  executeSimulation(id: string): Observable<{ executionId: string }> {
    return this.http.post<{ executionId: string }>(`${this.apiUrl}/${id}/execute`, {}).pipe(
      catchError(error => {
        console.error('Error executing simulation scenario:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Cancel a running simulation scenario.
   */
  cancelSimulation(id: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/cancel`, {}).pipe(
      catchError(error => {
        console.error('Error cancelling simulation scenario:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get simulation scenarios by type.
   */
  getSimulationsByType(type: string, params?: any): Observable<SimulationScenario[]> {
    return this.http.get<SimulationScenario[]>(`${this.apiUrl}/type/${type}`, { params }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching simulation scenarios by type:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get simulation scenarios by status.
   */
  getSimulationsByStatus(status: string, params?: any): Observable<SimulationScenario[]> {
    return this.http.get<SimulationScenario[]>(`${this.apiUrl}/status/${status}`, { params }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching simulation scenarios by status:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get simulation scenarios created by a specific user.
   */
  getSimulationsByUser(userId: string, params?: any): Observable<SimulationScenario[]> {
    return this.http.get<SimulationScenario[]>(`${this.apiUrl}/created-by/${userId}`, { params }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching simulation scenarios by user:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Get simulation scenarios targeting a specific device.
   */
  getSimulationsByDevice(deviceId: string, params?: any): Observable<SimulationScenario[]> {
    return this.http.get<SimulationScenario[]>(`${this.apiUrl}/device/${deviceId}`, { params }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error fetching simulation scenarios by device:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Export simulation scenarios to CSV.
   */
  exportToCSV(params?: any): Observable<string> {
    return this.http.get(`${this.apiUrl}/export/csv`, { 
      params,
      responseType: 'text'
    }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error exporting simulation scenarios to CSV:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Export simulation scenarios to PDF.
   */
  exportToPDF(params?: any): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/pdf`, { 
      params,
      responseType: 'blob'
    }).pipe(
      retry(2),
      catchError(error => {
        console.error('Error exporting simulation scenarios to PDF:', error);
        return throwError(error);
      })
    );
  }

  /**
   * Setup WebSocket listeners for real-time simulation updates.
   */
  private setupWebSocketListeners(): void {
    this.webSocketService.connect();

    this.webSocketService.messages$.subscribe(message => {
      if (message.type === 'simulation.created') {
        this.handleSimulationCreated(message.payload);
      } else if (message.type === 'simulation.updated') {
        this.handleSimulationUpdated(message.payload);
      } else if (message.type === 'simulation.deleted') {
        this.handleSimulationDeleted(message.payload);
      } else if (message.type === 'simulation.executed') {
        this.handleSimulationExecuted(message.payload);
      } else if (message.type === 'simulation.cancelled') {
        this.handleSimulationCancelled(message.payload);
      } else if (message.type === 'simulation.completed') {
        this.handleSimulationCompleted(message.payload);
      } else if (message.type === 'simulation.failed') {
        this.handleSimulationFailed(message.payload);
      }
    });
  }

  /**
   * Handle simulation creation notification.
   */
  private handleSimulationCreated(payload: any): void {
    this.simulationUpdateSubject.next({
      type: 'created',
      payload: payload
    });
  }

  /**
   * Handle simulation update notification.
   */
  private handleSimulationUpdated(payload: any): void {
    this.simulationUpdateSubject.next({
      type: 'updated',
      payload: payload
    });
  }

  /**
   * Handle simulation deletion notification.
   */
  private handleSimulationDeleted(payload: any): void {
    this.simulationUpdateSubject.next({
      type: 'deleted',
      payload: payload
    });
  }

  /**
   * Handle simulation execution notification.
   */
  private handleSimulationExecuted(payload: any): void {
    this.simulationUpdateSubject.next({
      type: 'executed',
      payload: payload
    });
  }

  /**
   * Handle simulation cancellation notification.
   */
  private handleSimulationCancelled(payload: any): void {
    this.simulationUpdateSubject.next({
      type: 'cancelled',
      payload: payload
    });
  }

  /**
   * Handle simulation completion notification.
   */
  private handleSimulationCompleted(payload: any): void {
    this.simulationUpdateSubject.next({
      type: 'completed',
      payload: payload
    });
  }

  /**
   * Handle simulation failure notification.
   */
  private handleSimulationFailed(payload: any): void {
    this.simulationUpdateSubject.next({
      type: 'failed',
      payload: payload
    });
  }

  /**
   * Get color based on status.
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING':
        return '#9e9e9e';
      case 'RUNNING':
        return '#2196f3';
      case 'COMPLETED':
        return '#4caf50';
      case 'CANCELLED':
        return '#ff9800';
      case 'FAILED':
        return '#f44336';
      default:
        return '#9e9e9e';
    }
  }

  /**
   * Get icon based on status.
   */
  getStatusIcon(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'schedule';
      case 'RUNNING':
        return 'play_circle';
      case 'COMPLETED':
        return 'check_circle';
      case 'CANCELLED':
        return 'cancel';
      case 'FAILED':
        return 'error';
      default:
        return 'help';
    }
  }

  /**
   * Get text color based on status.
   */
  getStatusTextColor(status: string): string {
    switch (status) {
      case 'PENDING':
      case 'RUNNING':
      case 'COMPLETED':
        return '#ffffff';
      default:
        return '#000000';
    }
  }

  /**
   * Get badge class based on status.
   */
  getStatusBadgeClass(status: string): string {
    return `status-badge status-${status.toLowerCase()}`;
  }

  /**
   * Get badge class based on type.
   */
  getTypeBadgeClass(type: string): string {
    return `type-badge type-${type.toLowerCase()}`;
  }
}
