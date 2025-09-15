import { Component, OnInit, OnDestroy } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';
import { Alert } from '../../models/alert.model';
import { AlertService } from '../../services/alert.service';
import { AlertDetailDialogComponent } from '../alert-detail-dialog/alert-detail-dialog.component';
import { AlertResolveDialogComponent } from '../alert-resolve-dialog/alert-resolve-dialog.component';

@Component({
  selector: 'app-alert-dashboard',
  templateUrl: './alert-dashboard.component.html',
  styleUrls: ['./alert-dashboard.component.scss']
})
export class AlertDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  alerts: Alert[] = [];
  filteredAlerts: Alert[] = [];

  // Loading states
  isLoading = false;
  isResolving = false;

  // Filters
  severityFilter: string = '';
  alertTypeFilter: string = '';
  resolvedFilter: boolean | null = null;

  // Statistics
  alertStatistics: any = {
    totalAlerts: 0,
    unresolvedAlerts: 0,
    alertsBySeverity: {},
    alertsByType: {}
  };

  // Severity options
  severityOptions = [
    { value: 'CRITICAL', label: 'Critical' },
    { value: 'HIGH', label: 'High' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'LOW', label: 'Low' }
  ];

  // Alert type options
  alertTypeOptions = [
    { value: 'TOPOLOGY_VIOLATION', label: 'Topology Violation' },
    { value: 'CONNECTIVITY_ISSUE', label: 'Connectivity Issue' },
    { value: 'POLICY_VIOLATION', label: 'Policy Violation' },
    { value: 'SECURITY_ISSUE', label: 'Security Issue' },
    { value: 'CUSTOM', label: 'Custom' }
  ];

  constructor(
    private alertService: AlertService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadAlerts();
    this.loadAlertStatistics();
    this.setupRealTimeUpdates();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load alerts from the backend.
   */
  private loadAlerts() {
    this.isLoading = true;

    // Get unresolved alerts by default
    this.alertService.getUnresolvedAlerts().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (alerts) => {
        this.alerts = alerts;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading alerts:', error);
        this.isLoading = false;
        this.showSnackBar('Error loading alerts: ' + error.message, 'error');
      }
    });
  }

  /**
   * Load alert statistics.
   */
  private loadAlertStatistics() {
    this.alertService.getAlertStatistics().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (stats) => {
        this.alertStatistics = stats;
      },
      error: (error) => {
        console.error('Error loading alert statistics:', error);
      }
    });
  }

  /**
   * Setup real-time updates for alerts.
   */
  private setupRealTimeUpdates() {
    // Subscribe to new alerts
    this.alertService.newAlerts$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(alert => {
      this.alerts.unshift(alert);
      this.applyFilters();

      // Show notification for critical alerts
      if (alert.severity === 'CRITICAL') {
        this.showSnackBar(`Critical Alert: ${alert.message}`, 'critical');
      }
    });

    // Subscribe to alerts
    this.alertService.alerts$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(alerts => {
      this.alerts = alerts;
      this.applyFilters();
    });
  }

  /**
   * Apply filters to the alerts list.
   */
  applyFilters() {
    this.filteredAlerts = this.alerts.filter(alert => {
      // Filter by severity
      if (this.severityFilter && alert.severity !== this.severityFilter) {
        return false;
      }

      // Filter by alert type
      if (this.alertTypeFilter && alert.alertType !== this.alertTypeFilter) {
        return false;
      }

      // Filter by resolved status
      if (this.resolvedFilter !== null && alert.resolved !== this.resolvedFilter) {
        return false;
      }

      return true;
    });
  }

  /**
   * Handle filter changes.
   */
  onFilterChange() {
    this.applyFilters();
  }

  /**
   * Clear all filters.
   */
  clearFilters() {
    this.severityFilter = '';
    this.alertTypeFilter = '';
    this.resolvedFilter = null;
    this.applyFilters();
  }

  /**
   * View alert details.
   */
  viewAlertDetails(alert: Alert) {
    const dialogRef = this.dialog.open(AlertDetailDialogComponent, {
      width: '600px',
      data: { alert }
    });

    dialogRef.afterClosed().pipe(
      takeUntil(this.destroy$)
    ).subscribe(result => {
      if (result) {
        // Handle any actions from the dialog
      }
    });
  }

  /**
   * Resolve an alert.
   */
  resolveAlert(alert: Alert) {
    const dialogRef = this.dialog.open(AlertResolveDialogComponent, {
      width: '500px',
      data: { alert }
    });

    dialogRef.afterClosed().pipe(
      takeUntil(this.destroy$)
    ).subscribe(result => {
      if (result) {
        this.isResolving = true;

        this.alertService.resolveAlert(alert.id, result.resolutionNotes).pipe(
          takeUntil(this.destroy$)
        ).subscribe({
          next: (updatedAlert) => {
            // Update local alert
            const index = this.alerts.findIndex(a => a.id === updatedAlert.id);
            if (index !== -1) {
              this.alerts[index] = updatedAlert;
              this.applyFilters();
            }

            this.showSnackBar(`Alert "${updatedAlert.message}" has been resolved`, 'success');
            this.isResolving = false;
          },
          error: (error) => {
            console.error('Error resolving alert:', error);
            this.showSnackBar('Error resolving alert: ' + error.message, 'error');
            this.isResolving = false;
          }
        });
      }
    });
  }

  /**
   * Unresolve an alert.
   */
  unresolveAlert(alert: Alert) {
    this.isResolving = true;

    this.alertService.unresolveAlert(alert.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (updatedAlert) => {
        // Update local alert
        const index = this.alerts.findIndex(a => a.id === updatedAlert.id);
        if (index !== -1) {
          this.alerts[index] = updatedAlert;
          this.applyFilters();
        }

        this.showSnackBar(`Alert "${updatedAlert.message}" has been unresolved`, 'success');
        this.isResolving = false;
      },
      error: (error) => {
        console.error('Error unresolving alert:', error);
        this.showSnackBar('Error unresolving alert: ' + error.message, 'error');
        this.isResolving = false;
      }
    });
  }

  /**
   * Show a notification.
   */
  private showSnackBar(message: string, type: 'success' | 'error' | 'critical' = 'success') {
    const panelClass = type === 'success' ? ['mat-snack-bar-success'] : 
                     type === 'error' ? ['mat-snack-bar-error'] : 
                     ['mat-snack-bar-critical'];

    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: panelClass
    });
  }

  /**
   * Get color based on severity.
   */
  getSeverityColor(severity: string): string {
    return this.alertService.getSeverityColor(severity);
  }

  /**
   * Get icon based on severity.
   */
  getSeverityIcon(severity: string): string {
    return this.alertService.getSeverityIcon(severity);
  }

  /**
   * Get text color based on severity.
   */
  getSeverityTextColor(severity: string): string {
    return this.alertService.getSeverityTextColor(severity);
  }

  /**
   * Get badge class based on severity.
   */
  getSeverityBadgeClass(severity: string): string {
    return `severity-badge severity-${severity.toLowerCase()}`;
  }

  /**
   * Get badge class based on alert type.
   */
  getAlertTypeBadgeClass(alertType: string): string {
    return `alert-type-badge alert-type-${alertType.toLowerCase().replace('_', '-')}`;
  }
}
