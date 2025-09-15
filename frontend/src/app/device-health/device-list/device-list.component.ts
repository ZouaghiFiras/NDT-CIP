import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { DeviceStatus } from '../../models/device-status.model';
import { Alert } from '../../models/alert.model';
import { DeviceHealthService } from '../../services/device-health.service';
import { AlertService } from '../../services/alert.service';
import { DeviceStatusSocketService } from '../../services/device-status-socket.service';
import { DeviceDetailDialogComponent } from '../device-detail/device-detail-dialog.component';
import { AlertDialogComponent } from '../alert-dialog/alert-dialog.component';
import { ExportDialogComponent } from '../export-dialog/export-dialog.component';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-device-list',
  templateUrl: './device-list.component.html',
  styleUrls: ['./device-list.component.scss']
})
export class DeviceListComponent implements OnInit, OnDestroy, AfterViewInit {
  displayedColumns: string[] = [
    'name', 
    'type', 
    'status', 
    'lastSeen', 
    'cpu', 
    'memory', 
    'disk', 
    'criticality',
    'actions'
  ];

  dataSource: MatTableDataSource<DeviceStatus>;
  loading = false;
  filterValues = {
    status: '',
    criticality: '',
    owner: '',
    search: ''
  };

  private searchSubject = new Subject<string>();

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild('filterInput') filterInput: ElementRef;

  constructor(
    private deviceHealthService: DeviceHealthService,
    private alertService: AlertService,
    private deviceStatusSocketService: DeviceStatusSocketService,
    private dialog: MatDialog
  ) {
    this.dataSource = new MatTableDataSource();
  }

  ngOnInit() {
    this.loadDevices();

    // Setup search debouncing
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.filterValues.search = searchTerm;
      this.loadDevices();
    });

    // Subscribe to device status updates
    this.deviceStatusSocketService.deviceStatusUpdates$.subscribe(update => {
      const device = this.dataSource.data.find(d => d.deviceId === update.deviceId);
      if (device) {
        // Update the device in the data source
        const index = this.dataSource.data.indexOf(device);
        if (index !== -1) {
          this.dataSource.data[index] = update;
          this.dataSource._updateChangeSubscription();
        }
      } else {
        // Add new device to data source
        this.dataSource.data.push(update);
        this.dataSource._updateChangeSubscription();
      }
    });

    // Subscribe to new alerts
    this.deviceStatusSocketService.alertUpdates$.subscribe(alert => {
      // Refresh alert counts for the affected device
      this.loadDevices();
    });
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    // Focus filter input on load
    if (this.filterInput) {
      this.filterInput.nativeElement.focus();
    }
  }

  ngOnDestroy() {
    // Cleanup socket subscriptions
    this.deviceStatusSocketService.disconnect();
  }

  /**
   * Load devices with current filters.
   */
  loadDevices() {
    this.loading = true;

    this.deviceHealthService.getDeviceStatusList(
      this.filterValues.status || undefined,
      this.filterValues.criticality ? parseInt(this.filterValues.criticality) : undefined,
      this.filterValues.owner || undefined,
      undefined,
      undefined,
      { pageIndex: 0, pageSize: 10, sort: { active: 'lastSeen', direction: 'desc' } }
    ).subscribe(
      (response) => {
        this.dataSource.data = response.content;
        this.loading = false;
      },
      (error) => {
        console.error('Error loading devices:', error);
        this.loading = false;
      }
    );
  }

  /**
   * Handle search input changes.
   */
  onSearchChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  /**
   * Handle filter changes.
   */
  onFilterChange() {
    this.paginator.pageIndex = 0;
    this.loadDevices();
  }

  /**
   * Clear all filters.
   */
  clearFilters() {
    this.filterValues = {
      status: '',
      criticality: '',
      owner: '',
      search: ''
    };
    if (this.filterInput) {
      this.filterInput.nativeElement.value = '';
    }
    this.paginator.pageIndex = 0;
    this.loadDevices();
  }

  /**
   * Apply pagination and sorting.
   */
  paginate(event: any) {
    this.loadDevices();
  }

  /**
   * Get status badge color.
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'HEALTHY': return 'green';
      case 'DEGRADED': return 'yellow';
      case 'UNHEALTHY': return 'orange';
      case 'COMPROMISED': return 'red';
      case 'UNKNOWN': return 'gray';
      default: return 'gray';
    }
  }

  /**
   * Get criticality badge color.
   */
  getCriticalityColor(criticality: number): string {
    if (criticality >= 4) return 'red';
    if (criticality >= 3) return 'orange';
    if (criticality >= 2) return 'yellow';
    return 'green';
  }

  /**
   * Open device detail dialog.
   */
  openDeviceDetail(device: DeviceStatus): void {
    this.dialog.open(DeviceDetailDialogComponent, {
      width: '90%',
      maxWidth: '1200px',
      data: { device }
    });
  }

  /**
   * Open alert dialog.
   */
  openAlerts(device: DeviceStatus): void {
    this.dialog.open(AlertDialogComponent, {
      width: '80%',
      maxWidth: '1000px',
      data: { deviceId: device.deviceId }
    });
  }

  /**
   * Export device data.
   */
  exportData(): void {
    this.dialog.open(ExportDialogComponent, {
      width: '50%',
      data: {}
    });
  }

  /**
   * Format last seen time.
   */
  formatLastSeen(lastSeen: Date): string {
    if (!lastSeen) return 'Never';

    const now = new Date();
    const diff = Math.floor((now.getTime() - lastSeen.getTime()) / 1000); // in seconds

    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  }

  /**
   * Check if device is stale (no recent heartbeat).
   */
  isDeviceStale(device: DeviceStatus): boolean {
    if (!device.lastSeen) return true;

    const now = new Date();
    const staleThreshold = new Date(now.getTime() - 5 * 60 * 1000); // 5 minutes
    return device.lastSeen < staleThreshold;
  }
}
