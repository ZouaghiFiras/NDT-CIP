package com.network.digitaltwin.monitoring.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.Device;
import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing an alert for a device.
 */
@Entity
@Table(name = "alerts",
       indexes = {
           @Index(name = "idx_alerts_device_id", columnList = "device_id"),
           @Index(name = "idx_alerts_alert_type", columnList = "alert_type"),
           @Index(name = "idx_alerts_severity", columnList = "severity"),
           @Index(name = "idx_alerts_status", columnList = "status"),
           @Index(name = "idx_alerts_created_at", columnList = "created_at")
       })
public class Alert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "alert_type", nullable = false)
    private String alertType; // e.g., "HIGH_CPU", "MEMORY_EXCEEDED", "DEVICE_UNREACHABLE", "COMPROMISED"

    @Column(name = "severity", nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "status", nullable = false)
    private String status; // ACTIVE, ACKNOWLEDGED, RESOLVED, ESCALATED

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    @Column(name = "escalation_level", nullable = false)
    private Integer escalationLevel = 0; // 0 = no escalation, 1+ = escalation level

    @Column(name = "next_escalation_at")
    private Instant nextEscalationAt;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors
    public Alert() {
    }

    public Alert(Device device, String alertType, String severity, String message) {
        this.device = device;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.escalationLevel = 0;
    }

    // Getters and Setters
    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public User getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(User acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Integer getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public Instant getNextEscalationAt() {
        return nextEscalationAt;
    }

    public void setNextEscalationAt(Instant nextEscalationAt) {
        this.nextEscalationAt = nextEscalationAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Convenience methods
    public void addPayload(String key, Object value) {
        this.payload.put(key, value);
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isAcknowledged() {
        return "ACKNOWLEDGED".equals(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equals(status);
    }

    public boolean isEscalated() {
        return "ESCALATED".equals(status);
    }

    public boolean needsEscalation() {
        return escalationLevel > 0 && 
               (nextEscalationAt == null || Instant.now().isAfter(nextEscalationAt));
    }

    public boolean isLowSeverity() {
        return "LOW".equals(severity);
    }

    public boolean isMediumSeverity() {
        return "MEDIUM".equals(severity);
    }

    public boolean isHighSeverity() {
        return "HIGH".equals(severity);
    }

    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    public void acknowledge(User user) {
        this.status = "ACKNOWLEDGED";
        this.acknowledgedAt = Instant.now();
        this.acknowledgedBy = user;
        this.updatedAt = Instant.now();
    }

    public void resolve() {
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void escalate() {
        this.status = "ESCALATED";
        this.escalationLevel++;
        this.updatedAt = Instant.now();
    }

    public void scheduleNextEscalation(Integer escalationMinutes) {
        this.nextEscalationAt = Instant.now().plusSeconds(escalationMinutes * 60L);
        this.updatedAt = Instant.now();
    }

    public long getAgeInMinutes() {
        Instant now = Instant.now();
        return java.time.Duration.between(createdAt, now).toMinutes();
    }

    public long getTimeToResolutionInMinutes() {
        if (resolvedAt == null) {
            return 0;
        }
        return java.time.Duration.between(createdAt, resolvedAt).toMinutes();
    }

    @Override
    public String toString() {
        return "Alert{" +
                "id=" + getId() +
                ", device=" + (device != null ? device.getName() : "null") +
                ", alertType='" + alertType + ''' +
                ", severity='" + severity + ''' +
                ", status='" + status + ''' +
                ", createdAt=" + createdAt +
                '}';
    }
}
