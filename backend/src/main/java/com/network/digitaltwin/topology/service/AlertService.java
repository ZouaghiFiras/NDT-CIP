package com.network.digitaltwin.topology.service;

import com.network.digitaltwin.model.auth.User;
import com.network.digitaltwin.topology.model.Alert;
import com.network.digitaltwin.topology.model.Connection;
import com.network.digitaltwin.topology.model.Device;
import com.network.digitaltwin.topology.repository.AlertRepository;
import com.network.digitaltwin.repository.DeviceRepository;
import com.network.digitaltwin.repository.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service for managing topology validation alerts.
 */
@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * Create a new alert.
     * @param alertType The type of alert
     * @param severity The severity level
     * @param message The alert message
     * @param affectedDeviceIds List of affected device IDs
     * @param affectedConnectionIds List of affected connection IDs
     * @param createdBy The user who created the alert
     * @return The created alert
     */
    @Transactional
    public Alert createAlert(String alertType, String severity, String message, 
                           List<UUID> affectedDeviceIds, List<UUID> affectedConnectionIds, 
                           UUID createdBy) {
        logger.info("Creating alert: type={}, severity={}, message={}", alertType, severity, message);

        // Validate severity
        if (!Arrays.asList("CRITICAL", "HIGH", "MEDIUM", "LOW").contains(severity)) {
            throw new IllegalArgumentException("Invalid severity level: " + severity);
        }

        // Get user if provided
        User user = null;
        if (createdBy != null) {
            user = userRepository.findById(createdBy)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + createdBy));
        }

        // Create alert
        Alert alert = new Alert(alertType, severity, message, affectedDeviceIds, affectedConnectionIds);
        alert.setCreatedBy(user);

        // Save alert
        alert = alertRepository.save(alert);

        // Send notification via Kafka
        sendAlertNotification(alert);

        logger.info("Created alert: {}", alert.getId());
        return alert;
    }

    /**
     * Resolve an alert.
     * @param alertId The ID of the alert to resolve
     * @param resolvedBy The user resolving the alert
     * @param resolutionNotes Notes about the resolution
     * @return The resolved alert
     */
    @Transactional
    public Alert resolveAlert(UUID alertId, UUID resolvedBy, String resolutionNotes) {
        logger.info("Resolving alert: {}", alertId);

        // Get alert
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        // Get user
        User user = userRepository.findById(resolvedBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + resolvedBy));

        // Resolve alert
        alert.resolve(user.getId(), resolutionNotes);

        // Save alert
        alert = alertRepository.save(alert);

        // Send resolution notification via Kafka
        sendAlertResolutionNotification(alert);

        logger.info("Resolved alert: {}", alertId);
        return alert;
    }

    /**
     * Unresolve an alert.
     * @param alertId The ID of the alert to unresolve
     * @param unresolvedBy The user unresolving the alert
     * @return The unresolved alert
     */
    @Transactional
    public Alert unresolveAlert(UUID alertId, UUID unresolvedBy) {
        logger.info("Unresolving alert: {}", alertId);

        // Get alert
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        // Get user
        User user = userRepository.findById(unresolvedBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + unresolvedBy));

        // Unresolve alert
        alert.unresolve();

        // Save alert
        alert = alertRepository.save(alert);

        // Send unresolve notification via Kafka
        sendAlertUnresolveNotification(alert);

        logger.info("Unresolved alert: {}", alertId);
        return alert;
    }

    /**
     * Get an alert by ID.
     * @param alertId The ID of the alert
     * @return The alert
     */
    public Alert getAlert(UUID alertId) {
        return alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
    }

    /**
     * Get alerts with filtering and pagination.
     * @param severity Filter by severity (optional)
     * @param alertType Filter by alert type (optional)
     * @param resolved Filter by resolved status (optional)
     * @param deviceId Filter by affected device ID (optional)
     * @param connectionId Filter by affected connection ID (optional)
     * @param start Start date (optional)
     * @param end End date (optional)
     * @param pageable Pagination information
     * @return Page of alerts
     */
    public Page<Alert> getAlerts(String severity, String alertType, Boolean resolved, 
                               UUID deviceId, UUID connectionId, 
                               Instant start, Instant end, Pageable pageable) {
        if (deviceId != null) {
            // Get alerts affecting a specific device
            return alertRepository.findByAffectedDevicesContaining(deviceId, pageable);
        } else if (connectionId != null) {
            // Get alerts affecting a specific connection
            return alertRepository.findByAffectedConnectionsContaining(connectionId, pageable);
        } else if (severity != null && alertType != null) {
            // Get alerts by severity and type
            return alertRepository.findBySeverityAndAlertType(severity, alertType, pageable);
        } else if (severity != null) {
            // Get alerts by severity
            return alertRepository.findBySeverity(severity, pageable);
        } else if (alertType != null) {
            // Get alerts by type
            return alertRepository.findByAlertType(alertType, pageable);
        } else if (resolved != null) {
            // Get alerts by resolved status
            return alertRepository.findByResolved(resolved, pageable);
        } else if (start != null && end != null) {
            // Get alerts by date range
            return alertRepository.findByTimestampBetween(start, end, pageable);
        } else {
            // Get all alerts
            return alertRepository.findAll(pageable);
        }
    }

    /**
     * Get unresolved alerts.
     * @param pageable Pagination information
     * @return Page of unresolved alerts
     */
    public Page<Alert> getUnresolvedAlerts(Pageable pageable) {
        return alertRepository.findByResolved(false, pageable);
    }

    /**
     * Get alerts by severity.
     * @param severity The severity level
     * @param pageable Pagination information
     * @return Page of alerts
     */
    public Page<Alert> getAlertsBySeverity(String severity, Pageable pageable) {
        return alertRepository.findBySeverity(severity, pageable);
    }

    /**
     * Get alerts by type.
     * @param alertType The alert type
     * @param pageable Pagination information
     * @return Page of alerts
     */
    public Page<Alert> getAlertsByType(String alertType, Pageable pageable) {
        return alertRepository.findByAlertType(alertType, pageable);
    }

    /**
     * Delete an alert.
     * @param alertId The ID of the alert to delete
     */
    @Transactional
    public void deleteAlert(UUID alertId) {
        logger.info("Deleting alert: {}", alertId);

        // Delete alert
        alertRepository.deleteById(alertId);

        logger.info("Deleted alert: {}", alertId);
    }

    /**
     * Get alerts affecting a specific device.
     * @param deviceId The device ID
     * @param pageable Pagination information
     * @return Page of alerts
     */
    public Page<Alert> getAlertsByDevice(UUID deviceId, Pageable pageable) {
        return alertRepository.findByAffectedDevicesContaining(deviceId, pageable);
    }

    /**
     * Get alerts affecting a specific connection.
     * @param connectionId The connection ID
     * @param pageable Pagination information
     * @return Page of alerts
     */
    public Page<Alert> getAlertsByConnection(UUID connectionId, Pageable pageable) {
        return alertRepository.findByAffectedConnectionsContaining(connectionId, pageable);
    }

    /**
     * Get alerts created by a specific user.
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of alerts
     */
    public Page<Alert> getAlertsByUser(UUID userId, Pageable pageable) {
        return alertRepository.findByCreatedBy_Id(userId, pageable);
    }

    /**
     * Get recent alerts (last 24 hours).
     * @param pageable Pagination information
     * @return Page of recent alerts
     */
    public Page<Alert> getRecentAlerts(Pageable pageable) {
        Instant start = Instant.now().minusSeconds(24 * 60 * 60);
        return alertRepository.findByTimestampAfter(start, pageable);
    }

    /**
     * Get critical alerts.
     * @param pageable Pagination information
     * @return Page of critical alerts
     */
    public Page<Alert> getCriticalAlerts(Pageable pageable) {
        return alertRepository.findBySeverity("CRITICAL", pageable);
    }

    /**
     * Get alert statistics.
     * @return Map of alert statistics
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Total alerts
        long totalAlerts = alertRepository.count();
        stats.put("totalAlerts", totalAlerts);

        // Unresolved alerts
        long unresolvedAlerts = alertRepository.countByResolved(false);
        stats.put("unresolvedAlerts", unresolvedAlerts);

        // Alerts by severity
        Map<String, Long> alertsBySeverity = new HashMap<>();
        alertsBySeverity.put("CRITICAL", alertRepository.countBySeverity("CRITICAL"));
        alertsBySeverity.put("HIGH", alertRepository.countBySeverity("HIGH"));
        alertsBySeverity.put("MEDIUM", alertRepository.countBySeverity("MEDIUM"));
        alertsBySeverity.put("LOW", alertRepository.countBySeverity("LOW"));
        stats.put("alertsBySeverity", alertsBySeverity);

        // Alerts by type
        Map<String, Long> alertsByType = new HashMap<>();
        List<Object[]> results = alertRepository.countByAlertType();
        for (Object[] result : results) {
            alertsByType.put((String) result[0], (Long) result[1]);
        }
        stats.put("alertsByType", alertsByType);

        // Alerts in last 24 hours
        Instant start = Instant.now().minusSeconds(24 * 60 * 60);
        long recentAlerts = alertRepository.countByTimestampAfter(start);
        stats.put("recentAlerts", recentAlerts);

        // Alerts resolved in last 24 hours
        long resolvedAlerts = alertRepository.countByResolvedTrueAndTimestampAfter(start);
        stats.put("resolvedAlerts", resolvedAlerts);

        return stats;
    }

    /**
     * Send alert notification via Kafka.
     * @param alert The alert to notify about
     */
    private void sendAlertNotification(Alert alert) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ALERT_CREATED");
            notification.put("alertId", alert.getId());
            notification.put("alertType", alert.getAlertType());
            notification.put("severity", alert.getSeverity());
            notification.put("message", alert.getMessage());
            notification.put("timestamp", alert.getTimestamp().toString());
            notification.put("affectedDevices", alert.getAffectedDevices());
            notification.put("affectedConnections", alert.getAffectedConnections());

            kafkaProducerService.send("topology-alerts", notification);
        } catch (Exception e) {
            logger.error("Error sending alert notification for alert: " + alert.getId(), e);
        }
    }

    /**
     * Send alert resolution notification via Kafka.
     * @param alert The resolved alert
     */
    private void sendAlertResolutionNotification(Alert alert) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ALERT_RESOLVED");
            notification.put("alertId", alert.getId());
            notification.put("resolvedAt", alert.getResolvedAt().toString());
            notification.put("resolvedBy", alert.getResolvedBy());
            notification.put("resolutionNotes", alert.getResolutionNotes());

            kafkaProducerService.send("topology-alerts", notification);
        } catch (Exception e) {
            logger.error("Error sending alert resolution notification for alert: " + alert.getId(), e);
        }
    }

    /**
     * Send alert unresolve notification via Kafka.
     * @param alert The unresolved alert
     */
    private void sendAlertUnresolveNotification(Alert alert) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ALERT_UNRESOLVED");
            notification.put("alertId", alert.getId());
            notification.put("unresolvedAt", Instant.now().toString());
            notification.put("unresolvedBy", alert.getResolvedBy());

            kafkaProducerService.send("topology-alerts", notification);
        } catch (Exception e) {
            logger.error("Error sending alert unresolve notification for alert: " + alert.getId(), e);
        }
    }
}
