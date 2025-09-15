package com.network.digitaltwin.simulation.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Entity representing a cybersecurity simulation scenario.
 */
@Entity
@Table(name = "simulation_scenarios",
       indexes = {
           @Index(name = "idx_scenario_name", columnList = "name"),
           @Index(name = "idx_scenario_type", columnList = "type"),
           @Index(name = "idx_scenario_status", columnList = "status"),
           @Index(name = "idx_scenario_created_by", columnList = "created_by"),
           @Index(name = "idx_scenario_created_at", columnList = "created_at")
       })
public class SimulationScenario extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SimulationType type;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "target_devices", columnDefinition = "uuid[]")
    private List<UUID> targetDevices = new ArrayList<>();

    @Column(name = "attack_vector", nullable = false)
    private String attackVector;

    @Column(name = "duration", nullable = false)
    private Duration duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SimulationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "execution_history", columnDefinition = "jsonb")
    private List<Map<String, Object>> executionHistory = new ArrayList<>();

    // Constructors
    public SimulationScenario() {
        this.status = SimulationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public SimulationScenario(String name, SimulationType type, String description, 
                            List<UUID> targetDevices, String attackVector, Duration duration) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.targetDevices = targetDevices != null ? targetDevices : new ArrayList<>();
        this.attackVector = attackVector;
        this.duration = duration;
        this.status = SimulationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SimulationType getType() {
        return type;
    }

    public void setType(SimulationType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<UUID> getTargetDevices() {
        return targetDevices;
    }

    public void setTargetDevices(List<UUID> targetDevices) {
        this.targetDevices = targetDevices;
    }

    public String getAttackVector() {
        return attackVector;
    }

    public void setAttackVector(String attackVector) {
        this.attackVector = attackVector;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public SimulationStatus getStatus() {
        return status;
    }

    public void setStatus(SimulationStatus status) {
        this.status = status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<Map<String, Object>> getExecutionHistory() {
        return executionHistory;
    }

    public void setExecutionHistory(List<Map<String, Object>> executionHistory) {
        this.executionHistory = executionHistory;
    }

    // Convenience methods
    public void addTargetDevice(UUID deviceId) {
        if (this.targetDevices == null) {
            this.targetDevices = new ArrayList<>();
        }
        this.targetDevices.add(deviceId);
    }

    public void removeTargetDevice(UUID deviceId) {
        if (this.targetDevices != null) {
            this.targetDevices.remove(deviceId);
        }
    }

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public void addExecutionHistoryEntry(String action, String details, Instant timestamp) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("action", action);
        entry.put("details", details);
        entry.put("timestamp", timestamp.toString());

        if (this.executionHistory == null) {
            this.executionHistory = new ArrayList<>();
        }
        this.executionHistory.add(entry);
    }

    public boolean isPending() {
        return SimulationStatus.PENDING.equals(status);
    }

    public boolean isRunning() {
        return SimulationStatus.RUNNING.equals(status);
    }

    public boolean isCompleted() {
        return SimulationStatus.COMPLETED.equals(status);
    }

    public boolean isCancelled() {
        return SimulationStatus.CANCELLED.equals(status);
    }

    public boolean isFailed() {
        return SimulationStatus.FAILED.equals(status);
    }

    public boolean isRansomware() {
        return SimulationType.RANSOMWARE.equals(type);
    }

    public boolean isDdos() {
        return SimulationType.DDOS.equals(type);
    }

    public boolean isInsiderThreat() {
        return SimulationType.INSIDER_THREAT.equals(type);
    }

    public boolean isPhishing() {
        return SimulationType.PHISHING.equals(type);
    }

    public boolean isCustom() {
        return SimulationType.CUSTOM.equals(type);
    }

    @Override
    public String toString() {
        return "SimulationScenario{" +
                "id=" + getId() +
                ", name='" + name + ''' +
                ", type=" + type +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

/**
 * Enum for simulation types.
 */
enum SimulationType {
    RANSOMWARE,
    DDOS,
    INSIDER_THREAT,
    PHISHING,
    CUSTOM
}

/**
 * Enum for simulation statuses.
 */
enum SimulationStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}
