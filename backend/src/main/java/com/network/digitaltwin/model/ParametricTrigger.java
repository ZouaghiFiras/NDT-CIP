package com.network.digitaltwin.model;

import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a triggered parametric rule execution.
 */
@Entity
@Table(name = "parametric_triggers",
       indexes = {
           @Index(name = "idx_parametric_triggers_rule_id", columnList = "rule_id"),
           @Index(name = "idx_parametric_triggers_triggered_at", columnList = "triggered_at"),
           @Index(name = "idx_parametric_triggers_status", columnList = "status")
       })
public class ParametricTrigger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private ParametricRule rule;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    @Column(name = "trigger_data", columnDefinition = "jsonb")
    private Map<String, Object> triggerData = new HashMap<>();

    @Column(name = "action_result", columnDefinition = "jsonb")
    private Map<String, Object> actionResult = new HashMap<>();

    @NotNull
    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "status")
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "duration")
    private Long duration; // in milliseconds

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by")
    private User triggeredBy;

    @Column(name = "notes")
    private String notes;

    // Constructors
    public ParametricTrigger() {
    }

    public ParametricTrigger(ParametricRule rule, String triggerType, Map<String, Object> triggerData) {
        this.rule = rule;
        this.triggerType = triggerType;
        this.triggerData = triggerData;
        this.triggeredAt = Instant.now();
    }

    // Getters and Setters
    public ParametricRule getRule() {
        return rule;
    }

    public void setRule(ParametricRule rule) {
        this.rule = rule;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public Map<String, Object> getTriggerData() {
        return triggerData;
    }

    public void setTriggerData(Map<String, Object> triggerData) {
        this.triggerData = triggerData;
    }

    public Map<String, Object> getActionResult() {
        return actionResult;
    }

    public void setActionResult(Map<String, Object> actionResult) {
        this.actionResult = actionResult;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public User getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(User triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Convenience methods
    public void addTriggerData(String key, Object value) {
        this.triggerData.put(key, value);
    }

    public void addActionResult(String key, Object value) {
        this.actionResult.put(key, value);
    }

    public void startProcessing() {
        this.status = "PROCESSING";
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
        this.duration = this.completedAt.toEpochMilli() - this.triggeredAt.toEpochMilli();
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

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isProcessing() {
        return "PROCESSING".equals(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    public void recordTrigger(User user) {
        this.triggeredBy = user;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "ParametricTrigger{" +
                "id=" + getId() +
                ", rule=" + (rule != null ? rule.getName() : "null") +
                ", triggerType='" + triggerType + '\'' +
                ", status='" + status + '\'' +
                ", triggeredAt=" + triggeredAt +
                '}';
    }
}
