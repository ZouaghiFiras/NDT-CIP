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
 * Entity representing the history of alert state changes for audit purposes.
 */
@Entity
@Table(name = "alert_history",
       indexes = {
           @Index(name = "idx_alert_history_alert_id", columnList = "alert_id"),
           @Index(name = "idx_alert_history_timestamp", columnList = "timestamp"),
           @Index(name = "idx_alert_history_action", columnList = "action")
       })
public class AlertHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "action", nullable = false)
    private String action; // CREATED, ACKNOWLEDGED, RESOLVED, ESCALATED, REOPENED

    @Column(name = "status", nullable = false)
    private String status; // ACTIVE, ACKNOWLEDGED, RESOLVED, ESCALATED

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "escalation_level")
    private Integer escalationLevel;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors
    public AlertHistory() {
    }

    public AlertHistory(Alert alert, Device device, String action, String status) {
        this.alert = alert;
        this.device = device;
        this.action = action;
        this.status = status;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
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

    public boolean isCreated() {
        return "CREATED".equals(action);
    }

    public boolean isAcknowledged() {
        return "ACKNOWLEDGED".equals(action);
    }

    public boolean isResolved() {
        return "RESOLVED".equals(action);
    }

    public boolean isEscalated() {
        return "ESCALATED".equals(action);
    }

    public boolean isReopened() {
        return "REOPENED".equals(action);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isAcknowledgedStatus() {
        return "ACKNOWLEDGED".equals(status);
    }

    public boolean isResolvedStatus() {
        return "RESOLVED".equals(status);
    }

    public boolean isEscalatedStatus() {
        return "ESCALATED".equals(status);
    }

    public boolean hasUser() {
        return user != null;
    }

    @Override
    public String toString() {
        return "AlertHistory{" +
                "id=" + getId() +
                ", alert=" + (alert != null ? alert.getId() : "null") +
                ", device=" + (device != null ? device.getName() : "null") +
                ", action='" + action + ''' +
                ", status='" + status + ''' +
                ", timestamp=" + timestamp +
                '}';
    }
}
