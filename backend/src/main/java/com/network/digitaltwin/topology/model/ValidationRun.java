package com.network.digitaltwin.topology.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.*;

/**
 * Entity representing a topology validation run.
 */
@Entity
@Table(name = "topology_validation_runs",
       indexes = {
           @Index(name = "idx_run_status", columnList = "status"),
           @Index(name = "idx_run_created_at", columnList = "created_at"),
           @Index(name = "idx_run_triggered_by", columnList = "triggered_by")
       })
public class ValidationRun extends BaseEntity {

    @Column(name = "run_id", unique = true, nullable = false)
    private String runId;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy; // MANUAL, SCHEDULED, TOPOLOGY_CHANGE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "executed_rules", columnDefinition = "text[]")
    private List<String> executedRules = new ArrayList<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "rule_results", columnDefinition = "jsonb")
    private Map<String, Object> ruleResults = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "alert_ids", columnDefinition = "uuid[]")
    private List<UUID> alertIds = new ArrayList<>();

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors
    public ValidationRun() {
        this.triggeredAt = Instant.now();
        this.status = "PENDING";
    }

    public ValidationRun(String runId, String triggeredBy, User user) {
        this.runId = runId;
        this.triggeredBy = triggeredBy;
        this.user = user;
        this.triggeredAt = Instant.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public List<String> getExecutedRules() {
        return executedRules;
    }

    public void setExecutedRules(List<String> executedRules) {
        this.executedRules = executedRules;
    }

    public Map<String, Object> getRuleResults() {
        return ruleResults;
    }

    public void setRuleResults(Map<String, Object> ruleResults) {
        this.ruleResults = ruleResults;
    }

    public List<UUID> getAlertIds() {
        return alertIds;
    }

    public void setAlertIds(List<UUID> alertIds) {
        this.alertIds = alertIds;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Convenience methods
    public void addExecutedRule(String ruleName) {
        if (this.executedRules == null) {
            this.executedRules = new ArrayList<>();
        }
        this.executedRules.add(ruleName);
    }

    public void addRuleResult(String ruleName, Object result) {
        if (this.ruleResults == null) {
            this.ruleResults = new HashMap<>();
        }
        this.ruleResults.put(ruleName, result);
    }

    public void addAlertId(UUID alertId) {
        if (this.alertIds == null) {
            this.alertIds = new ArrayList<>();
        }
        this.alertIds.add(alertId);
    }

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public void start() {
        this.status = "RUNNING";
        this.startedAt = Instant.now();
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
        if (this.startedAt != null) {
            this.durationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
    }

    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        if (this.startedAt != null) {
            this.durationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isRunning() {
        return "RUNNING".equals(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isManual() {
        return "MANUAL".equals(triggeredBy);
    }

    public boolean isScheduled() {
        return "SCHEDULED".equals(triggeredBy);
    }

    public boolean isTopologyChange() {
        return "TOPOLOGY_CHANGE".equals(triggeredBy);
    }

    @Override
    public String toString() {
        return "ValidationRun{" +
                "id=" + getId() +
                ", runId='" + runId + ''' +
                ", status='" + status + ''' +
                ", triggeredBy='" + triggeredBy + ''' +
                ", triggeredAt=" + triggeredAt +
                ", durationMs=" + durationMs +
                '}';
    }
}
