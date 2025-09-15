package com.network.digitaltwin.topology.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.*;

/**
 * Entity representing topology validation alerts.
 */
@Entity
@Table(name = "topology_alerts",
       indexes = {
           @Index(name = "idx_alert_type", columnList = "alert_type"),
           @Index(name = "idx_alert_severity", columnList = "severity"),
           @Index(name = "idx_alert_resolved", columnList = "resolved"),
           @Index(name = "idx_alert_timestamp", columnList = "timestamp")
       })
public class Alert extends BaseEntity {

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "severity", nullable = false)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "affected_devices", columnDefinition = "uuid[]")
    private List<UUID> affectedDevices = new ArrayList<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "affected_connections", columnDefinition = "uuid[]")
    private List<UUID> affectedConnections = new ArrayList<>();

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors
    public Alert() {
        this.timestamp = Instant.now();
    }

    public Alert(String alertType, String severity, String message, 
                List<UUID> affectedDevices, List<UUID> affectedConnections) {
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.affectedDevices = affectedDevices != null ? affectedDevices : new ArrayList<>();
        this.affectedConnections = affectedConnections != null ? affectedConnections : new ArrayList<>();
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<UUID> getAffectedDevices() {
        return affectedDevices;
    }

    public void setAffectedDevices(List<UUID> affectedDevices) {
        this.affectedDevices = affectedDevices;
    }

    public List<UUID> getAffectedConnections() {
        return affectedConnections;
    }

    public void setAffectedConnections(List<UUID> affectedConnections) {
        this.affectedConnections = affectedConnections;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(UUID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Convenience methods
    public void addAffectedDevice(UUID deviceId) {
        if (this.affectedDevices == null) {
            this.affectedDevices = new ArrayList<>();
        }
        this.affectedDevices.add(deviceId);
    }

    public void addAffectedConnection(UUID connectionId) {
        if (this.affectedConnections == null) {
            this.affectedConnections = new ArrayList<>();
        }
        this.affectedConnections.add(connectionId);
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void resolve(UUID resolvedBy, String resolutionNotes) {
        this.resolved = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Instant.now();
        this.resolutionNotes = resolutionNotes;
    }

    public void unresolve() {
        this.resolved = false;
        this.resolvedBy = null;
        this.resolvedAt = null;
        this.resolutionNotes = null;
    }

    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    public boolean isHigh() {
        return "HIGH".equals(severity);
    }

    public boolean isMedium() {
        return "MEDIUM".equals(severity);
    }

    public boolean isLow() {
        return "LOW".equals(severity);
    }

    public boolean isTopologyViolation() {
        return "TOPOLOGY_VIOLATION".equals(alertType);
    }

    public boolean isConnectivityIssue() {
        return "CONNECTIVITY_ISSUE".equals(alertType);
    }

    public boolean isPolicyViolation() {
        return "POLICY_VIOLATION".equals(alertType);
    }

    public boolean isSecurityIssue() {
        return "SECURITY_ISSUE".equals(alertType);
    }

    @Override
    public String toString() {
        return "Alert{" +
                "id=" + getId() +
                ", alertType='" + alertType + ''' +
                ", severity='" + severity + ''' +
                ", message='" + message + ''' +
                ", timestamp=" + timestamp +
                ", resolved=" + resolved +
                '}';
    }
}
