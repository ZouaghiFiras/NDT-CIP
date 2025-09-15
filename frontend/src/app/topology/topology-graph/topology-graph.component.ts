import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit, Input } from '@angular/core';
import { CytoscapeComponent } from 'angular-cytoscape';
import { DeviceStatus } from '../../models/device-status.model';
import { Connection } from '../../models/connection.model';
import { DeviceStatusSocketService } from '../../services/device-status-socket.service';
import { TopologyService } from '../../services/topology.service';
import { AlertService } from '../../services/alert.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-topology-graph',
  templateUrl: './topology-graph.component.html',
  styleUrls: ['./topology-graph.component.scss']
})
export class TopologyGraphComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('cytoscape') cytoscapeRef: CytoscapeComponent;
  @ViewChild('searchInput') searchInput: ElementRef;

  @Input('layout') layout: 'force' | 'hierarchical' | 'circular' = 'force';
  @Input('showLabels') showLabels: boolean = true;
  @Input('filterByStatus') filterByStatus: string[] = [];
  @Input('filterByType') filterByType: string[] = [];
  @Input('filterByCriticality') filterByCriticality: number[] = [];

  private cy: any;
  private devices: Map<string, DeviceStatus> = new Map();
  private connections: Map<string, Connection> = new Map();
  private layoutOptions: any;
  private destroy$ = new Subject<void>();

  // Loading states
  isLoading = true;
  isUpdating = false;

  // Search
  searchQuery = '';

  // Layout options
  layoutOptionsList = [
    { value: 'force', label: 'Force-Directed' },
    { value: 'hierarchical', label: 'Hierarchical' },
    { value: 'circular', label: 'Circular' }
  ];

  // Status colors
  statusColors = {
    HEALTHY: '#4caf50',
    DEGRADED: '#ff9800',
    UNHEALTHY: '#ff5722',
    COMPROMISED: '#f44336',
    UNKNOWN: '#9e9e9e'
  };

  // Connection types and colors
  connectionTypeColors = {
    ETHERNET: '#2196f3',
    WIFI: '#00bcd4',
    VPN: '#9c27b0',
    OTHER: '#607d8b'
  };

  // Criticality colors
  criticalityColors = {
    1: '#4caf50',
    2: '#8bc34a',
    3: '#ffeb3b',
    4: '#ff9800',
    5: '#f44336'
  };

  constructor(
    private deviceStatusSocketService: DeviceStatusSocketService,
    private topologyService: TopologyService,
    private alertService: AlertService
  ) {
    // Initialize layout options
    this.layoutOptions = {
      name: 'cola',
      // Other cola options
      nodeLength: 100,
      edgeLength: 100,
      nodeSpacing: 100,
      avoidOverlap: 0.5
    };
  }

  ngOnInit() {
    this.loadTopology();
    this.setupSocketListeners();
    this.setupSearch();
  }

  ngAfterViewInit() {
    // Focus search input after view is initialized
    if (this.searchInput) {
      setTimeout(() => this.searchInput.nativeElement.focus(), 500);
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();

    // Cleanup socket subscriptions
    this.deviceStatusSocketService.disconnect();
  }

  /**
   * Load the initial topology from the backend.
   */
  private loadTopology() {
    this.isLoading = true;

    // Get topology
    this.topologyService.getTopology().subscribe(
      (topology) => {
        // Process devices
        topology.nodes.forEach((node) => {
          const deviceStatus: DeviceStatus = {
            deviceId: node.id,
            device: node,
            status: node.status,
            lastSeen: new Date(), // Placeholder
            receivedAt: new Date()
          };
          this.devices.set(node.id, deviceStatus);
        });

        // Process connections
        topology.edges.forEach((edge) => {
          const connection: Connection = {
            id: edge.id,
            sourceDeviceId: edge.sourceDeviceId,
            targetDeviceId: edge.targetDeviceId,
            connectionType: edge.type,
            bandwidth: edge.bandwidth,
            latency: edge.latency,
            reliability: edge.reliability,
            status: edge.status
          };
          this.connections.set(edge.id, connection);
        });

        // Render the graph
        this.renderGraph();
        this.isLoading = false;
      },
      (error) => {
        console.error('Error loading topology:', error);
        this.isLoading = false;
      }
    );
  }

  /**
   * Setup WebSocket listeners for real-time updates.
   */
  private setupSocketListeners() {
    // Listen for device status updates
    this.deviceStatusSocketService.deviceStatusUpdates$.subscribe(update => {
      this.updateDeviceStatus(update);
    });

    // Listen for topology changes
    this.deviceStatusSocketService.topologyUpdates$.subscribe(change => {
      this.handleTopologyChange(change);
    });
  }

  /**
   * Setup search with debouncing.
   */
  private setupSearch() {
    fromEvent(this.searchInput.nativeElement, 'input')
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.searchQuery = this.searchInput.nativeElement.value;
        this.applyFilters();
      });
  }

  /**
   * Render the topology graph.
   */
  private renderGraph() {
    if (!this.cytoscapeRef) return;

    // Create elements for cytoscape
    const elements = [];

    // Add nodes
    this.devices.forEach((device, deviceId) => {
      const deviceNode = device.device;
      const nodeStyle = this.getNodeStyle(device);

      elements.push({
        data: {
          id: deviceId,
          label: this.showLabels ? deviceNode.name : '',
          name: deviceNode.name,
          type: deviceNode.type,
          ip: deviceNode.ip,
          status: device.status,
          criticality: deviceNode.criticality,
          riskScore: deviceNode.riskScore || 0,
          alerts: device.alerts || []
        },
        classes: this.getNodeClasses(device),
        style: nodeStyle
      });
    });

    // Add edges
    this.connections.forEach((connection, connectionId) => {
      const edgeStyle = this.getEdgeStyle(connection);

      elements.push({
        data: {
          id: connectionId,
          source: connection.sourceDeviceId,
          target: connection.targetDeviceId,
          type: connection.connectionType,
          bandwidth: connection.bandwidth,
          latency: connection.latency,
          reliability: connection.reliability,
          status: connection.status
        },
        classes: this.getEdgeClasses(connection),
        style: edgeStyle
      });
    });

    // Initialize cytoscape
    this.cy = this.cytoscapeRef.cy({
      elements: elements,
      style: [
        {
          selector: 'node',
          style: {
            'background-color': '#333',
            'label': 'data(label)',
            'text-valign': 'center',
            'text-halign': 'center',
            'color': '#fff',
            'text-background-color': '#333',
            'text-background-opacity': 0.5,
            'text-background-padding': '2px',
            'width': 'mapData(riskScore, 0, 100, 20, 60)',
            'height': 'mapData(riskScore, 0, 100, 20, 60)',
            'border-width': 2,
            'border-color': '#fff'
          }
        },
        {
          selector: 'edge',
          style: {
            'width': 2,
            'line-color': '#ccc',
            'target-arrow-color': '#ccc',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier'
          }
        },
        {
          selector: '.unhealthy',
          style: {
            'background-color': this.statusColors.UNHEALTHY,
            'border-color': this.statusColors.UNHEALTHY
          }
        },
        {
          selector: '.compromised',
          style: {
            'background-color': this.statusColors.COMPROMISED,
            'border-color': this.statusColors.COMPROMISED,
            'border-width': 4
          }
        },
        {
          selector: '.high-risk',
          style: {
            'line-color': '#f44336',
            'target-arrow-color': '#f44336',
            'width': 3
          }
        },
        {
          selector: '.degraded',
          style: {
            'background-color': this.statusColors.DEGRADED,
            'border-color': this.statusColors.DEGRADED
          }
        },
        {
          selector: '.alert',
          style: {
            'border-width': 4,
            'border-color': '#ffeb3b'
          }
        }
      ],
      layout: {
        name: 'cola',
        ...this.layoutOptions
      }
    });

    // Add event handlers
    this.cy.on('tap', 'node', (evt) => {
      const node = evt.target;
      this.showNodeDetails(node.id());
    });

    this.cy.on('tap', 'edge', (evt) => {
      const edge = evt.target;
      this.showEdgeDetails(edge.id());
    });

    // Fit the graph to the container
    this.cy.fit();
  }

  /**
   * Update device status in the graph.
   */
  private updateDeviceStatus(deviceStatus: DeviceStatus) {
    if (!this.cy) return;

    const deviceId = deviceStatus.deviceId;
    const node = this.cy.getElementById(deviceId);

    if (node.length > 0) {
      // Update node data
      node.data({
        status: deviceStatus.status,
        riskScore: deviceStatus.device?.riskScore || 0,
        alerts: deviceStatus.alerts || []
      });

      // Update node style
      const nodeStyle = this.getNodeStyle(deviceStatus);
      node.style(nodeStyle);

      // Update node classes
      const nodeClasses = this.getNodeClasses(deviceStatus);
      node.classes(nodeClasses);

      // Fit the graph to keep updated node in view
      this.cy.center(node);
    }
  }

  /**
   * Handle topology changes from WebSocket.
   */
  private handleTopologyChange(change: any) {
    if (!this.cy) return;

    this.isUpdating = true;

    switch (change.type) {
      case 'node_added':
        this.addNode(change.node);
        break;
      case 'node_updated':
        this.updateNode(change.node);
        break;
      case 'node_removed':
        this.removeNode(change.nodeId);
        break;
      case 'edge_added':
        this.addEdge(change.edge);
        break;
      case 'edge_updated':
        this.updateEdge(change.edge);
        break;
      case 'edge_removed':
        this.removeEdge(change.edgeId);
        break;
    }

    // Apply current layout
    this.applyLayout();

    // Fit the graph to the container
    this.cy.fit();

    this.isUpdating = false;
  }

  /**
   * Add a node to the graph.
   */
  private addNode(nodeData: any) {
    const deviceStatus: DeviceStatus = {
      deviceId: nodeData.id,
      device: nodeData,
      status: nodeData.status,
      lastSeen: new Date(),
      receivedAt: new Date()
    };

    this.devices.set(nodeData.id, deviceStatus);

    const nodeStyle = this.getNodeStyle(deviceStatus);
    const nodeClasses = this.getNodeClasses(deviceStatus);

    this.cy.add({
      data: {
        id: nodeData.id,
        label: this.showLabels ? nodeData.name : '',
        name: nodeData.name,
        type: nodeData.type,
        ip: nodeData.ip,
        status: nodeData.status,
        criticality: nodeData.criticality,
        riskScore: nodeData.riskScore || 0,
        alerts: nodeData.alerts || []
      },
      classes: nodeClasses,
      style: nodeStyle
    });
  }

  /**
   * Update a node in the graph.
   */
  private updateNode(nodeData: any) {
    const deviceStatus: DeviceStatus = {
      deviceId: nodeData.id,
      device: nodeData,
      status: nodeData.status,
      lastSeen: new Date(),
      receivedAt: new Date()
    };

    this.devices.set(nodeData.id, deviceStatus);
    this.updateDeviceStatus(deviceStatus);
  }

  /**
   * Remove a node from the graph.
   */
  private removeNode(nodeId: string) {
    this.devices.delete(nodeId);
    const node = this.cy.getElementById(nodeId);

    if (node.length > 0) {
      node.remove();
    }

    // Remove associated edges
    this.cy.edges().forEach((edge) => {
      if (edge.source().id() === nodeId || edge.target().id() === nodeId) {
        this.removeEdge(edge.id());
      }
    });
  }

  /**
   * Add an edge to the graph.
   */
  private addEdge(edgeData: any) {
    const connection: Connection = {
      id: edgeData.id,
      sourceDeviceId: edgeData.sourceDeviceId,
      targetDeviceId: edgeData.targetDeviceId,
      connectionType: edgeData.type,
      bandwidth: edgeData.bandwidth,
      latency: edgeData.latency,
      reliability: edgeData.reliability,
      status: edgeData.status
    };

    this.connections.set(edgeData.id, connection);

    const edgeStyle = this.getEdgeStyle(connection);
    const edgeClasses = this.getEdgeClasses(connection);

    this.cy.add({
      data: {
        id: edgeData.id,
        source: edgeData.sourceDeviceId,
        target: edgeData.targetDeviceId,
        type: edgeData.type,
        bandwidth: edgeData.bandwidth,
        latency: edgeData.latency,
        reliability: edgeData.reliability,
        status: edgeData.status
      },
      classes: edgeClasses,
      style: edgeStyle
    });
  }

  /**
   * Update an edge in the graph.
   */
  private updateEdge(edgeData: any) {
    const connection: Connection = {
      id: edgeData.id,
      sourceDeviceId: edgeData.sourceDeviceId,
      targetDeviceId: edgeData.targetDeviceId,
      connectionType: edgeData.type,
      bandwidth: edgeData.bandwidth,
      latency: edgeData.latency,
      reliability: edgeData.reliability,
      status: edgeData.status
    };

    this.connections.set(edgeData.id, connection);

    const edge = this.cy.getElementById(edgeData.id);
    if (edge.length > 0) {
      const edgeStyle = this.getEdgeStyle(connection);
      const edgeClasses = this.getEdgeClasses(connection);

      edge.data({
        type: edgeData.type,
        bandwidth: edgeData.bandwidth,
        latency: edgeData.latency,
        reliability: edgeData.reliability,
        status: edgeData.status
      });

      edge.style(edgeStyle);
      edge.classes(edgeClasses);
    }
  }

  /**
   * Remove an edge from the graph.
   */
  private removeEdge(edgeId: string) {
    this.connections.delete(edgeId);
    const edge = this.cy.getElementById(edgeId);

    if (edge.length > 0) {
      edge.remove();
    }
  }

  /**
   * Get node style based on device status.
   */
  private getNodeStyle(deviceStatus: DeviceStatus): any {
    const device = deviceStatus.device;
    const status = deviceStatus.status;

    let backgroundColor = this.statusColors.UNKNOWN;

    if (status === 'HEALTHY') {
      backgroundColor = this.statusColors.HEALTHY;
    } else if (status === 'DEGRADED') {
      backgroundColor = this.statusColors.DEGRADED;
    } else if (status === 'UNHEALTHY') {
      backgroundColor = this.statusColors.UNHEALTHY;
    } else if (status === 'COMPROMISED') {
      backgroundColor = this.statusColors.COMPROMISED;
    }

    return {
      'background-color': backgroundColor,
      'border-color': '#fff',
      'border-width': status === 'COMPROMISED' ? 4 : 2,
      'width': 'mapData(riskScore, 0, 100, 20, 60)',
      'height': 'mapData(riskScore, 0, 100, 20, 60)'
    };
  }

  /**
   * Get node classes based on device status.
   */
  private getNodeClasses(deviceStatus: DeviceStatus): string {
    const classes = [];

    if (deviceStatus.status === 'UNHEALTHY') {
      classes.push('unhealthy');
    } else if (deviceStatus.status === 'DEGRADED') {
      classes.push('degraded');
    } else if (deviceStatus.status === 'COMPROMISED') {
      classes.push('compromised');
    }

    if (deviceStatus.device?.criticality >= 4) {
      classes.push('high-criticality');
    }

    if (deviceStatus.alerts && deviceStatus.alerts.length > 0) {
      classes.push('alert');
    }

    return classes.join(' ');
  }

  /**
   * Get edge style based on connection status.
   */
  private getEdgeStyle(connection: Connection): any {
    let lineColor = this.connectionTypeColors[connection.connectionType] || '#607d8b';
    let width = 2;

    if (connection.status === 'DEGRADED') {
      lineColor = this.statusColors.DEGRADED;
      width = 3;
    } else if (connection.status === 'FAILED') {
      lineColor = this.statusColors.UNHEALTHY;
      width = 4;
    }

    // Check if connection has high risk
    if (this.isHighRiskConnection(connection)) {
      lineColor = this.statusColors.UNHEALTHY;
      width = 3;
    }

    return {
      'line-color': lineColor,
      'target-arrow-color': lineColor,
      'width': width
    };
  }

  /**
   * Get edge classes based on connection status.
   */
  private getEdgeClasses(connection: Connection): string {
    const classes = [];

    if (connection.status === 'DEGRADED') {
      classes.push('degraded');
    } else if (connection.status === 'FAILED') {
      classes.push('failed');
    }

    if (this.isHighRiskConnection(connection)) {
      classes.push('high-risk');
    }

    return classes.join(' ');
  }

  /**
   * Check if a connection is high risk.
   */
  private isHighRiskConnection(connection: Connection): boolean {
    // High latency or low reliability indicates high risk
    return (connection.latency && connection.latency > 100) ||
           (connection.reliability && connection.reliability < 0.8);
  }

  /**
   * Apply filters to the graph.
   */
  private applyFilters() {
    if (!this.cy) return;

    // Filter nodes
    this.cy.nodes().forEach((node) => {
      const device = this.devices.get(node.id());
      if (device) {
        const shouldShow = this.shouldShowNode(device);
        node.style('display', shouldShow ? 'element' : 'none');
      }
    });

    // Filter edges - only show edges between visible nodes
    this.cy.edges().forEach((edge) => {
      const sourceVisible = this.cy.getElementById(edge.source().id()).style('display') === 'element';
      const targetVisible = this.cy.getElementById(edge.target().id()).style('display') === 'element';
      edge.style('display', (sourceVisible && targetVisible) ? 'element' : 'none');
    });

    // Fit the graph to the visible elements
    this.cy.fit();
  }

  /**
   * Check if a node should be shown based on filters.
   */
  private shouldShowNode(deviceStatus: DeviceStatus): boolean {
    // Search filter
    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      const device = deviceStatus.device;
      return device.name.toLowerCase().includes(query) ||
             device.ip.toLowerCase().includes(query);
    }

    // Status filter
    if (this.filterByStatus.length > 0 && !this.filterByStatus.includes(deviceStatus.status)) {
      return false;
    }

    // Type filter
    if (this.filterByType.length > 0 && !this.filterByType.includes(deviceStatus.device.type)) {
      return false;
    }

    // Criticality filter
    if (this.filterByCriticality.length > 0 && !this.filterByCriticality.includes(deviceStatus.device.criticality)) {
      return false;
    }

    return true;
  }

  /**
   * Apply the selected layout.
   */
  private applyLayout() {
    if (!this.cy) return;

    let layoutName = 'cola';
    let layoutOptions = { ...this.layoutOptions };

    switch (this.layout) {
      case 'hierarchical':
        layoutName = 'breadthfirst';
        layoutOptions = {
          directed: true,
          roots: '#',
          spacingFactor: 1.2
        };
        break;
      case 'circular':
        layoutName = 'circle';
        layoutOptions = {
          radius: Math.min(this.cy.width(), this.cy.height()) / 3
        };
        break;
    }

    this.cy.layout({
      name: layoutName,
      ...layoutOptions
    }).run();
  }

  /**
   * Show node details in a sidebar.
   */
  private showNodeDetails(deviceId: string) {
    const deviceStatus = this.devices.get(deviceId);
    if (!deviceStatus) return;

    // This would typically open a sidebar or modal
    console.log('Show node details:', deviceStatus);

    // In a real implementation, you would emit an event to open a detail sidebar
    // or use a service to manage the active node details
  }

  /**
   * Show edge details in a sidebar.
   */
  private showEdgeDetails(connectionId: string) {
    const connection = this.connections.get(connectionId);
    if (!connection) return;

    // This would typically open a sidebar or modal
    console.log('Show edge details:', connection);

    // In a real implementation, you would emit an event to open a detail sidebar
    // or use a service to manage the active edge details
  }

  /**
   * Export the topology to PNG.
   */
  exportToPNG() {
    if (!this.cy) return;

    // Use cytoscape.js to export as PNG
    const png = this.cy.png({
      output: 'blob',
      bg: '#fff',
      full: true,
      scale: 2
    });

    // Create download link
    const link = document.createElement('a');
    link.href = URL.createObjectURL(png);
    link.download = 'topology.png';
    link.click();
  }

  /**
   * Export the topology to JSON.
   */
  exportToJSON() {
    if (!this.cy) return;

    // Get the current graph data
    const elements = this.cy.json().elements;

    // Create JSON string
    const jsonString = JSON.stringify(elements, null, 2);

    // Create download link
    const blob = new Blob([jsonString], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'topology.json';
    link.click();
  }

  /**
   * Change the layout algorithm.
   */
  changeLayout(newLayout: 'force' | 'hierarchical' | 'circular') {
    this.layout = newLayout;
    this.applyLayout();
  }

  /**
   * Toggle node labels.
   */
  toggleLabels() {
    this.showLabels = !this.showLabels;

    if (this.cy) {
      this.cy.nodes().forEach((node) => {
        node.data('label', this.showLabels ? node.data('name') : '');
      });
    }
  }
}
