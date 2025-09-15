package com.network.digitaltwin.monitoring.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.Device;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a health evaluation rule for devices.
 */
@Entity
@Table(name = "health_rules",
       indexes = {
           @Index(name = "idx_health_rules_name", columnList = "name"),
           @Index(name = "idx_health_rules_enabled", columnList = "enabled"),
           @Index(name = "idx_health_rules_priority", columnList = "priority")
       })
public class HealthRule extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "rule_type", nullable = false)
    private String ruleType; // THRESHOLD, ANOMALY, TIMEOUT, CUSTOM

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "priority", nullable = false)
    private Integer priority = 5; // 1-10, 1 is highest priority

    @Column(name = "severity", nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private Map<String, Object> conditions = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "actions", columnDefinition = "jsonb")
    private Map<String, Object> actions = new HashMap<>();

    @Column(name = "cooldown_period")
    private Long cooldownPeriod; // in milliseconds

    @Column(name = "alert_type")
    private String alertType; // e.g., "HIGH_CPU", "MEMORY_EXCEEDED"

    @Column(name = "device_filter")
    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "device_filter", columnDefinition = "jsonb")
    private Map<String, Object> deviceFilter = new HashMap<>();

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "trigger_count")
    private Long triggerCount = 0L;

    // Constructors
    public HealthRule() {
    }

    public HealthRule(String name, String ruleType, String severity) {
        this.name = name;
        this.ruleType = ruleType;
        this.severity = severity;
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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public Map<String, Object> getActions() {
        return actions;
    }

    public void setActions(Map<String, Object> actions) {
        this.actions = actions;
    }

    public Long getCooldownPeriod() {
        return cooldownPeriod;
    }

    public void setCooldownPeriod(Long cooldownPeriod) {
        this.cooldownPeriod = cooldownPeriod;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public Map<String, Object> getDeviceFilter() {
        return deviceFilter;
    }

    public void setDeviceFilter(Map<String, Object> deviceFilter) {
        this.deviceFilter = deviceFilter;
    }

    public Instant getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(Instant lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Long getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Long triggerCount) {
        this.triggerCount = triggerCount;
    }

    // Convenience methods
    public void addCondition(String key, Object value) {
        this.conditions.put(key, value);
    }

    public void addAction(String key, Object value) {
        this.actions.put(key, value);
    }

    public void addDeviceFilter(String key, Object value) {
        this.deviceFilter.put(key, value);
    }

    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public boolean isThresholdRule() {
        return "THRESHOLD".equals(ruleType);
    }

    public boolean isAnomalyRule() {
        return "ANOMALY".equals(ruleType);
    }

    public boolean isTimeoutRule() {
        return "TIMEOUT".equals(ruleType);
    }

    public boolean isCustomRule() {
        return "CUSTOM".equals(ruleType);
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

    public boolean canTrigger() {
        if (lastTriggeredAt == null || cooldownPeriod == null) {
            return true;
        }

        Instant cooldownEnd = lastTriggeredAt.plusMillis(cooldownPeriod);
        return Instant.now().isAfter(cooldownEnd);
    }

    public void recordTrigger() {
        this.lastTriggeredAt = Instant.now();
        this.triggerCount++;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "HealthRule{" +
                "id=" + getId() +
                ", name='" + name + ''' +
                ", ruleType='" + ruleType + ''' +
                ", severity='" + severity + ''' +
                ", enabled=" + enabled +
                ", priority=" + priority +
                '}';
    }
}
