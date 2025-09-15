import { Component, OnInit, OnDestroy } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';
import { SimulationScenario } from '../../models/simulation-scenario.model';
import { SimulationService } from '../../services/simulation.service';
import { SimulationDetailDialogComponent } from '../simulation-detail-dialog/simulation-detail-dialog.component';
import { SimulationFormDialogComponent } from '../simulation-form-dialog/simulation-form-dialog.component';

@Component({
  selector: 'app-simulation-list',
  templateUrl: './simulation-list.component.html',
  styleUrls: ['./simulation-list.component.scss']
})
export class SimulationListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  simulations: SimulationScenario[] = [];
  filteredSimulations: SimulationScenario[] = [];

  // Loading states
  isLoading = false;
  isExecuting = false;
  isCancelling = false;

  // Filters
  typeFilter: string = '';
  statusFilter: string = '';
  searchQuery: string = '';

  // Pagination
  page = 1;
  pageSize = 10;
  totalItems = 0;

  // Type options
  typeOptions = [
    { value: 'RANSOMWARE', label: 'Ransomware' },
    { value: 'DDOS', label: 'DDoS' },
    { value: 'INSIDER_THREAT', label: 'Insider Threat' },
    { value: 'PHISHING', label: 'Phishing' },
    { value: 'CUSTOM', label: 'Custom' }
  ];

  // Status options
  statusOptions = [
    { value: 'PENDING', label: 'Pending' },
    { value: 'RUNNING', label: 'Running' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELLED', label: 'Cancelled' },
    { value: 'FAILED', label: 'Failed' }
  ];

  constructor(
    private simulationService: SimulationService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadSimulations();
    this.setupRealTimeUpdates();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load simulations from the backend.
   */
  private loadSimulations() {
    this.isLoading = true;

    // Get simulations
    this.simulationService.getSimulations().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (simulations) => {
        this.simulations = simulations;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading simulations:', error);
        this.isLoading = false;
        this.showSnackBar('Error loading simulations: ' + error.message, 'error');
      }
    });
  }

  /**
   * Setup real-time updates for simulations.
   */
  private setupRealTimeUpdates() {
    // Subscribe to simulation updates
    this.simulationService.simulationUpdates$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(update => {
      switch (update.type) {
        case 'created':
          this.simulations.unshift(update.payload);
          this.applyFilters();
          this.showSnackBar(`Simulation "${update.payload.name}" created successfully`, 'success');
          break;

        case 'updated':
          const index = this.simulations.findIndex(s => s.id === update.payload.id);
          if (index !== -1) {
            this.simulations[index] = update.payload;
            this.applyFilters();
          }
          this.showSnackBar(`Simulation "${update.payload.name}" updated successfully`, 'success');
          break;

        case 'deleted':
          this.simulations = this.simulations.filter(s => s.id !== update.payload.id);
          this.applyFilters();
          this.showSnackBar(`Simulation "${update.payload.name}" deleted successfully`, 'success');
          break;

        case 'executed':
          const execIndex = this.simulations.findIndex(s => s.id === update.payload.id);
          if (execIndex !== -1) {
            this.simulations[execIndex].status = 'RUNNING';
            this.applyFilters();
          }
          this.showSnackBar(`Simulation "${update.payload.name}" execution started`, 'info');
          break;

        case 'cancelled':
          const cancelIndex = this.simulations.findIndex(s => s.id === update.payload.id);
          if (cancelIndex !== -1) {
            this.simulations[cancelIndex].status = 'CANCELLED';
            this.applyFilters();
          }
          this.showSnackBar(`Simulation "${update.payload.name}" cancelled`, 'info');
          break;

        case 'completed':
          const compIndex = this.simulations.findIndex(s => s.id === update.payload.id);
          if (compIndex !== -1) {
            this.simulations[compIndex].status = 'COMPLETED';
            this.applyFilters();
          }
          this.showSnackBar(`Simulation "${update.payload.name}" completed successfully`, 'success');
          break;

        case 'failed':
          const failIndex = this.simulations.findIndex(s => s.id === update.payload.id);
          if (failIndex !== -1) {
            this.simulations[failIndex].status = 'FAILED';
            this.applyFilters();
          }
          this.showSnackBar(`Simulation "${update.payload.name}" failed: ${update.payload.error}`, 'error');
          break;
      }
    });
  }

  /**
   * Apply filters to the simulations list.
   */
  applyFilters() {
    this.filteredSimulations = this.simulations.filter(simulation => {
      // Filter by type
      if (this.typeFilter && simulation.type !== this.typeFilter) {
        return false;
      }

      // Filter by status
      if (this.statusFilter && simulation.status !== this.statusFilter) {
        return false;
      }

      // Filter by search query
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase();
        return simulation.name.toLowerCase().includes(query) ||
               simulation.description.toLowerCase().includes(query) ||
               simulation.attackVector.toLowerCase().includes(query);
      }

      return true;
    });

    // Update total items
    this.totalItems = this.filteredSimulations.length;
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
    this.typeFilter = '';
    this.statusFilter = '';
    this.searchQuery = '';
    this.applyFilters();
  }

  /**
   * Create a new simulation.
   */
  createSimulation() {
    const dialogRef = this.dialog.open(SimulationFormDialogComponent, {
      width: '800px',
      data: { mode: 'create' }
    });

    dialogRef.afterClosed().pipe(
      takeUntil(this.destroy$)
    ).subscribe(result => {
      if (result) {
        this.simulationService.createSimulation(result).subscribe({
          next: (simulation) => {
            this.showSnackBar(`Simulation "${simulation.name}" created successfully`, 'success');
          },
          error: (error) => {
            console.error('Error creating simulation:', error);
            this.showSnackBar('Error creating simulation: ' + error.message, 'error');
          }
        });
      }
    });
  }

  /**
   * Edit a simulation.
   */
  editSimulation(simulation: SimulationScenario) {
    const dialogRef = this.dialog.open(SimulationFormDialogComponent, {
      width: '800px',
      data: { mode: 'edit', simulation }
    });

    dialogRef.afterClosed().pipe(
      takeUntil(this.destroy$)
    ).subscribe(result => {
      if (result) {
        this.simulationService.updateSimulation(simulation.id, result).subscribe({
          next: (updatedSimulation) => {
            this.showSnackBar(`Simulation "${updatedSimulation.name}" updated successfully`, 'success');
          },
          error: (error) => {
            console.error('Error updating simulation:', error);
            this.showSnackBar('Error updating simulation: ' + error.message, 'error');
          }
        });
      }
    });
  }

  /**
   * View simulation details.
   */
  viewSimulationDetails(simulation: SimulationScenario) {
    const dialogRef = this.dialog.open(SimulationDetailDialogComponent, {
      width: '800px',
      data: { simulation }
    });
  }

  /**
   * Execute a simulation.
   */
  executeSimulation(simulation: SimulationScenario) {
    if (simulation.status !== 'PENDING') {
      this.showSnackBar('Only PENDING simulations can be executed', 'error');
      return;
    }

    this.isExecuting = true;

    this.simulationService.executeSimulation(simulation.id).subscribe({
      next: (response) => {
        this.showSnackBar(`Simulation "${simulation.name}" execution started`, 'success');
        this.isExecuting = false;
      },
      error: (error) => {
        console.error('Error executing simulation:', error);
        this.showSnackBar('Error executing simulation: ' + error.message, 'error');
        this.isExecuting = false;
      }
    });
  }

  /**
   * Cancel a simulation.
   */
  cancelSimulation(simulation: SimulationScenario) {
    if (simulation.status !== 'RUNNING') {
      this.showSnackBar('Only RUNNING simulations can be cancelled', 'error');
      return;
    }

    this.isCancelling = true;

    this.simulationService.cancelSimulation(simulation.id).subscribe({
      next: () => {
        this.showSnackBar(`Simulation "${simulation.name}" cancelled`, 'success');
        this.isCancelling = false;
      },
      error: (error) => {
        console.error('Error cancelling simulation:', error);
        this.showSnackBar('Error cancelling simulation: ' + error.message, 'error');
        this.isCancelling = false;
      }
    });
  }

  /**
   * Delete a simulation.
   */
  deleteSimulation(simulation: SimulationScenario) {
    if (simulation.status !== 'PENDING') {
      this.showSnackBar('Only PENDING simulations can be deleted', 'error');
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: { 
        title: 'Delete Simulation',
        message: `Are you sure you want to delete the simulation "${simulation.name}"? This action cannot be undone.`
      }
    });

    dialogRef.afterClosed().pipe(
      takeUntil(this.destroy$)
    ).subscribe(confirmed => {
      if (confirmed) {
        this.simulationService.deleteSimulation(simulation.id).subscribe({
          next: () => {
            this.showSnackBar(`Simulation "${simulation.name}" deleted successfully`, 'success');
          },
          error: (error) => {
            console.error('Error deleting simulation:', error);
            this.showSnackBar('Error deleting simulation: ' + error.message, 'error');
          }
        });
      }
    });
  }

  /**
   * Export simulations to CSV.
   */
  exportToCSV() {
    this.simulationService.exportToCSV().subscribe({
      next: (csvData) => {
        const blob = new Blob([csvData], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.setAttribute('download', 'simulations.csv');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        this.showSnackBar('Simulations exported successfully', 'success');
      },
      error: (error) => {
        console.error('Error exporting simulations:', error);
        this.showSnackBar('Error exporting simulations: ' + error.message, 'error');
      }
    });
  }

  /**
   * Export simulations to PDF.
   */
  exportToPDF() {
    this.simulationService.exportToPDF().subscribe({
      next: (pdfData) => {
        const blob = new Blob([pdfData], { type: 'application/pdf' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.setAttribute('download', 'simulations.pdf');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        this.showSnackBar('Simulations exported successfully', 'success');
      },
      error: (error) => {
        console.error('Error exporting simulations:', error);
        this.showSnackBar('Error exporting simulations: ' + error.message, 'error');
      }
    });
  }

  /**
   * Show a notification.
   */
  private showSnackBar(message: string, type: 'success' | 'error' | 'info' = 'success') {
    const panelClass = type === 'success' ? ['mat-snack-bar-success'] : 
                     type === 'error' ? ['mat-snack-bar-error'] : 
                     ['mat-snack-bar-info'];

    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: panelClass
    });
  }

  /**
   * Get color based on status.
   */
  getStatusColor(status: string): string {
    return this.simulationService.getStatusColor(status);
  }

  /**
   * Get icon based on status.
   */
  getStatusIcon(status: string): string {
    return this.simulationService.getStatusIcon(status);
  }

  /**
   * Get text color based on status.
   */
  getStatusTextColor(status: string): string {
    return this.simulationService.getStatusTextColor(status);
  }

  /**
   * Get badge class based on status.
   */
  getStatusBadgeClass(status: string): string {
    return this.simulationService.getStatusBadgeClass(status);
  }

  /**
   * Get badge class based on type.
   */
  getTypeBadgeClass(type: string): string {
    return this.simulationService.getTypeBadgeClass(type);
  }
}
