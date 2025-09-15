package com.network.digitaltwin.model;

import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;

/**
 * Entity representing an event that occurred during a simulation.
 */
@Entity
@Table(name = "simulation_events",
       indexes = {
           @Index(name = "idx_simulation_events_simulation_id", columnList = "simulation_id"),
           @Index(name = "idx_simulation_events_device_id", columnList = "device_id"),
           @Index(name = "idx_simulation_events_event_type", columnList = "event_type"),
           @Index(name = "idx_simulation_events_timestamp", columnList = "timestamp")
       })
public class SimulationEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @NotNull
    @Column(name = "event_type", nullable = false)
    private String eventType; // e.g., "threat_detected", "vulnerability_found", "device_compromised"

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "severity")
    private String severity; // e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL"

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, Object> properties = new HashMap<>();

    @Column(name = "impact_score")
    private BigDecimal impactScore = BigDecimal.ZERO;

    @Column(name = "affected_devices_count")
    private Integer affectedDevicesCount = 0;

    @Column(name = "resolved")
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    // Constructors
    public SimulationEvent() {
    }

    public SimulationEvent(Simulation simulation, String eventType, Instant timestamp) {
        this.simulation = simulation;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public BigDecimal getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(BigDecimal impactScore) {
        this.impactScore = impactScore;
    }

    public Integer getAffectedDevicesCount() {
        return affectedDevicesCount;
    }

    public void setAffectedDevicesCount(Integer affectedDevicesCount) {
        this.affectedDevicesCount = affectedDevicesCount;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public User getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(User resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    // Convenience methods
    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public void resolve(User user, String notes) {
        this.resolved = true;
        this.resolvedAt = Instant.now();
        this.resolvedBy = user;
        this.resolutionNotes = notes;
        this.updatedAt = Instant.now();
    }

    public void unresolve() {
        this.resolved = false;
        this.resolvedAt = null;
        this.resolvedBy = null;
        this.resolutionNotes = null;
        this.updatedAt = Instant.now();
    }

    public boolean isResolved() {
        return resolved != null && resolved;
    }

    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    public boolean isHighSeverity() {
        return "HIGH".equals(severity) || isCritical();
    }

    public boolean isMediumSeverity() {
        return "MEDIUM".equals(severity);
    }

    public boolean isLowSeverity() {
        return "LOW".equals(severity);
    }

    @Override
    public String toString() {
        return "SimulationEvent{" +
                "id=" + getId() +
                ", eventType='" + eventType + '\'' +
                ", severity='" + severity + '\'' +
                ", title='" + title + '\'' +
                ", resolved=" + resolved +
                '}';
    }
}
