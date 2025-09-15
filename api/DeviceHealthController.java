package com.network.digitaltwin.api;

import com.network.digitaltwin.monitoring.model.DeviceStatus;
import com.network.digitaltwin.monitoring.model.DeviceMetrics;
import com.network.digitaltwin.monitoring.model.Alert;
import com.network.digitaltwin.monitoring.service.DeviceHealthService;
import com.network.digitaltwin.monitoring.service.AlertService;
import com.network.digitaltwin.simulation.service.DeviceSimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for device health monitoring operations.
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceHealthController {

    @Autowired
    private DeviceHealthService deviceHealthService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private DeviceSimulatorService deviceSimulatorService;

    /**
     * Get the latest status of a device.
     * @param deviceId The ID of the device
     * @return The latest status of the device
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getDeviceStatus(@PathVariable UUID id) {
        try {
            DeviceStatus status = deviceHealthService.getLatestDeviceStatus(id);
            if (status == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve device status: " + e.getMessage()
            ));
        }
    }

    /**
     * Submit a heartbeat for a device.
     * @param deviceId The ID of the device
     * @param heartbeat The heartbeat data
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @PostMapping("/{id}/heartbeat")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> submitHeartbeat(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> heartbeat,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            deviceHealthService.processHeartbeat(id, heartbeat, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Heartbeat processed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to process heartbeat: " + e.getMessage()
            ));
        }
    }

    /**
     * Get historical status and metrics for a device.
     * @param deviceId The ID of the device
     * @param start Start date (optional)
     * @param end End date (optional)
     * @param statusFilter Status filter (optional)
     * @param pageable Pagination information
     * @return Paginated historical metrics and status events
     */
    @GetMapping("/{id}/status/history")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getDeviceStatusHistory(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(required = false) String statusFilter,
            Pageable pageable) {
        try {
            Page<DeviceMetrics> history = deviceHealthService.getDeviceStatusHistory(
                id, start, end, statusFilter, pageable);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve device status history: " + e.getMessage()
            ));
        }
    }

    /**
     * Get a list of devices with their latest status.
     * @param statusFilter Status filter (optional)
     * @param criticalityFilter Criticality filter (optional)
     * @param ownerFilter Owner filter (optional)
     * @param lastSeenBefore Filter devices last seen before this date (optional)
     * @param lastSeenAfter Filter devices last seen after this date (optional)
     * @param pageable Pagination information
     * @return Paginated list of devices with their latest status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getDeviceStatusList(
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) Integer criticalityFilter,
            @RequestParam(required = false) UUID ownerFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant lastSeenBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant lastSeenAfter,
            Pageable pageable) {
        try {
            Page<DeviceStatus> statusList = deviceHealthService.getDeviceStatusList(
                statusFilter, criticalityFilter, ownerFilter, 
                lastSeenBefore, lastSeenAfter, pageable);
            return ResponseEntity.ok(statusList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve device status list: " + e.getMessage()
            ));
        }
    }

    /**
     * Get alerts for a device.
     * @param deviceId The ID of the device
     * @param statusFilter Status filter (optional)
     * @param severityFilter Severity filter (optional)
     * @param pageable Pagination information
     * @return Paginated list of alerts for the device
     */
    @GetMapping("/{id}/alerts")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getDeviceAlerts(
            @PathVariable UUID id,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String severityFilter,
            Pageable pageable) {
        try {
            Page<Alert> alerts = alertService.getDeviceAlerts(id, statusFilter, severityFilter, pageable);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve device alerts: " + e.getMessage()
            ));
        }
    }

    /**
     * Acknowledge an alert.
     * @param alertId The ID of the alert
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> acknowledgeAlert(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            alertService.acknowledgeAlert(alertId, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Alert acknowledged"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to acknowledge alert: " + e.getMessage()
            ));
        }
    }

    /**
     * Resolve an alert.
     * @param alertId The ID of the alert
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @PostMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> resolveAlert(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            alertService.resolveAlert(alertId, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Alert resolved"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to resolve alert: " + e.getMessage()
            ));
        }
    }

    /**
     * Start device simulator for a device.
     * @param deviceId The ID of the device
     * @param config Simulator configuration
     * @return Response indicating success or failure
     */
    @PostMapping("/{id}/simulate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> startDeviceSimulator(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> config) {
        try {
            deviceSimulatorService.startSimulation(id, config);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Device simulator started"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to start device simulator: " + e.getMessage()
            ));
        }
    }

    /**
     * Stop device simulator for a device.
     * @param deviceId The ID of the device
     * @return Response indicating success or failure
     */
    @PostMapping("/{id}/simulate/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> stopDeviceSimulator(@PathVariable UUID id) {
        try {
            deviceSimulatorService.stopSimulation(id);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Device simulator stopped"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to stop device simulator: " + e.getMessage()
            ));
        }
    }

    /**
     * Export device metrics to CSV.
     * @param deviceId The ID of the device
     * @param start Start date (optional)
     * @param end End date (optional)
     * @return Response containing the exported data
     */
    @GetMapping("/{id}/metrics/export")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> exportDeviceMetrics(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        try {
            byte[] exportedData = deviceHealthService.exportDeviceMetrics(id, start, end);
            if (exportedData == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename="device-metrics-" + id + ".csv"")
                .body(exportedData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to export device metrics: " + e.getMessage()
            ));
        }
    }
}
