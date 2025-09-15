package com.network.digitaltwin.model;

import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;

/**
 * Entity representing a simulation scenario in the digital twin.
 */
@Entity
@Table(name = "simulations",
       indexes = {
           @Index(name = "idx_simulations_status", columnList = "status"),
           @Index(name = "idx_simulations_started_at", columnList = "started_at"),
           @Index(name = "idx_simulations_created_by", columnList = "created_by")
       })
public class Simulation extends BaseEntity {

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "scenario_type", nullable = false)
    private String scenarioType; // e.g., "threat_simulation", "vulnerability_scan", "stress_test"

    @Column(name = "status", nullable = false)
    private String status = "DRAFT"; // DRAFT, RUNNING, COMPLETED, FAILED, CANCELLED

    @Column(name = "priority")
    private Integer priority = 5; // 1-10 scale, 1 being highest priority

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private createdBy;


    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration")
    private Long duration; // in milliseconds

    @Column(name = "progress")
    private BigDecimal progress = BigDecimal.ZERO; // 0.0 to 100.0

    @Column(name = "target_devices", columnDefinition = "uuid[]")
    private List<UUID> targetDevices = new ArrayList<>();

    @Column(name = "affected_devices_count")
    private Integer affectedDevicesCount = 0;

    @Column(name = "total_events")
    private Long totalEvents = 0L;

    @Column(name = "processed_events")
    private Long processedEvents = 0L;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "results", columnDefinition = "jsonb")
    private Map<String, Object> results = new HashMap<>();

    @Column(name = "risk_score_impact")
    private BigDecimal riskScoreImpact = BigDecimal.ZERO;

    @Column(name = "error_message")
    private String errorMessage;

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimulationEvent> events = new ArrayList<>();

    // Constructors
    public Simulation() {
    }

    public Simulation(String name, String scenarioType) {
        this.name = name;
        this.scenarioType = scenarioType;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(String scenarioType) {
        this.scenarioType = scenarioType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public BigDecimal getProgress() {
        return progress;
    }

    public void setProgress(BigDecimal progress) {
        this.progress = progress;
    }

    public List<UUID> getTargetDevices() {
        return targetDevices;
    }

    public void setTargetDevices(List<UUID> targetDevices) {
        this.targetDevices = targetDevices;
    }

    public Integer getAffectedDevicesCount() {
        return affectedDevicesCount;
    }

    public void setAffectedDevicesCount(Integer affectedDevicesCount) {
        this.affectedDevicesCount = affectedDevicesCount;
    }

    public Long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(Long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public Long getProcessedEvents() {
        return processedEvents;
    }

    public void setProcessedEvents(Long processedEvents) {
        this.processedEvents = processedEvents;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getResults() {
        return results;
    }

    public void setResults(Map<String, Object> results) {
        this.results = results;
    }

    public BigDecimal getRiskScoreImpact() {
        return riskScoreImpact;
    }

    public void setRiskScoreImpact(BigDecimal riskScoreImpact) {
        this.riskScoreImpact = riskScoreImpact;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<SimulationEvent> getEvents() {
        return events;
    }

    public void setEvents(List<SimulationEvent> events) {
        this.events = events;
    }

    // Convenience methods
    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    public void addResult(String key, Object value) {
        this.results.put(key, value);
    }

    public void addEvent(SimulationEvent event) {
        event.setSimulation(this);
        this.events.add(event);
        this.processedEvents++;
    }

    public void start() {
        this.status = "RUNNING";
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
        this.duration = this.completedAt.toEpochMilli() - this.startedAt.toEpochMilli();
        this.updatedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }

    public void updateProgress(BigDecimal progress) {
        this.progress = progress;
        this.updatedAt = Instant.now();
    }

    public boolean isRunning() {
        return "RUNNING".equals(this.status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(this.status);
    }

    @Override
    public String toString() {
        return "Simulation{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", scenarioType='" + scenarioType + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                '}';
    }
}
