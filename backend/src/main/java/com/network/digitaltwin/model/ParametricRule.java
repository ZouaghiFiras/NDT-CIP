package com.network.digitaltwin.model;

import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a parametric rule used for automated triggers and actions.
 */
@Entity
@Table(name = "parametric_rules",
       indexes = {
           @Index(name = "idx_parametric_rules_name", columnList = "name"),
           @Index(name = "idx_parametric_rules_active", columnList = "active"),
           @Index(name = "idx_parametric_rules_trigger_type", columnList = "trigger_type")
       })
public class ParametricRule extends BaseEntity {

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType; // e.g., "DEVICE_RISK_THRESHOLD", "THREAT_DETECTED", "SIMULATION_COMPLETE"

    @Column(name = "condition", nullable = false, columnDefinition = "text")
    private String condition; // Serialized condition or DSL expression

    @Column(name = "action_type", nullable = false)
    private String actionType; // e.g., "NOTIFICATION", "POLICY_ACTIVATION", "SIMULATION_LAUNCH"

    @Column(name = "action_config", columnDefinition = "jsonb")
    private Map<String, Object> actionConfig = new HashMap<>();

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "priority")
    private Integer priority = 5; // 1-10 scale, 1 being highest priority

    @Column(name = "cooldown_period")
    private Long cooldownPeriod; // in milliseconds

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "trigger_count")
    private Long triggerCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "tags")
    private String tags; // Comma-separated tags for categorization

    // Constructors
    public ParametricRule() {
    }

    public ParametricRule(String name, String triggerType, String condition, String actionType) {
        this.name = name;
        this.triggerType = triggerType;
        this.condition = condition;
        this.actionType = actionType;
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

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(Map<String, Object> actionConfig) {
        this.actionConfig = actionConfig;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getCooldownPeriod() {
        return cooldownPeriod;
    }

    public void setCooldownPeriod(Long cooldownPeriod) {
        this.cooldownPeriod = cooldownPeriod;
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

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    // Convenience methods
    public void addActionConfig(String key, Object value) {
        this.actionConfig.put(key, value);
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = tag;
        } else {
            this.tags = this.tags + "," + tag;
        }
    }

    public String[] getTagArray() {
        return tags != null ? tags.split(",") : new String[0];
    }

    public boolean isActiveRule() {
        return active != null && active;
    }

    public boolean canTrigger() {
        if (cooldownPeriod == null || lastTriggeredAt == null) {
            return true;
        }

        Instant now = Instant.now();
        Instant cooldownEnd = lastTriggeredAt.plusMillis(cooldownPeriod);
        return now.isAfter(cooldownEnd);
    }

    public void recordTrigger() {
        this.lastTriggeredAt = Instant.now();
        this.triggerCount++;
        this.updatedAt = Instant.now();
    }

    public boolean isDeviceRiskThresholdRule() {
        return "DEVICE_RISK_THRESHOLD".equals(triggerType);
    }

    public boolean isThreatDetectedRule() {
        return "THREAT_DETECTED".equals(triggerType);
    }

    public boolean isSimulationCompleteRule() {
        return "SIMULATION_COMPLETE".equals(triggerType);
    }

    public boolean isNotificationAction() {
        return "NOTIFICATION".equals(actionType);
    }

    public boolean isPolicyActivationAction() {
        return "POLICY_ACTIVATION".equals(actionType);
    }

    public boolean isSimulationLaunchAction() {
        return "SIMULATION_LAUNCH".equals(actionType);
    }

    @Override
    public String toString() {
        return "ParametricRule{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", triggerType='" + triggerType + '\'' +
                ", actionType='" + actionType + '\'' +
                ", active=" + active +
                ", priority=" + priority +
                '}';
    }
}
