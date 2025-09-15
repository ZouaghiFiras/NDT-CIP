package com.network.digitaltwin.topology.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.*;

/**
 * Entity representing a topology validation rule.
 */
@Entity
@Table(name = "topology_validation_rules",
       indexes = {
           @Index(name = "idx_rule_type", columnList = "rule_type"),
           @Index(name = "rule_enabled", columnList = "enabled"),
           @Index(name = "rule_severity", columnList = "severity")
       })
public class ValidationRule extends BaseEntity {

    @Column(name = "rule_name", unique = true, nullable = false)
    private String ruleName;

    @Column(name = "rule_type", nullable = false)
    private String ruleType; // CONNECTIVITY, LOOP_DETECTION, POLICY_COMPLIANCE, SECURITY, CUSTOM

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "severity", nullable = false)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "configurable", nullable = false)
    private boolean configurable = true;

    @Column(name = "execution_order")
    private Integer executionOrder;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "rule_parameters", columnDefinition = "jsonb")
    private Map<String, Object> ruleParameters = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "validation_logic", columnDefinition = "jsonb")
    private Map<String, Object> validationLogic = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "execution_count")
    private Long executionCount = 0L;

    @Column(name = "failure_count")
    private Long failureCount = 0L;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "failure_details", columnDefinition = "jsonb")
    private Map<String, Object> failureDetails = new HashMap<>();

    // Constructors
    public ValidationRule() {
        this.createdAt = Instant.now();
    }

    public ValidationRule(String ruleName, String ruleType, String severity, String description) {
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.severity = severity;
        this.description = description;
        this.enabled = true;
        this.configurable = true;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public void setConfigurable(boolean configurable) {
        this.configurable = configurable;
    }

    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    public Map<String, Object> getRuleParameters() {
        return ruleParameters;
    }

    public void setRuleParameters(Map<String, Object> ruleParameters) {
        this.ruleParameters = ruleParameters;
    }

    public Map<String, Object> getValidationLogic() {
        return validationLogic;
    }

    public void setValidationLogic(Map<String, Object> validationLogic) {
        this.validationLogic = validationLogic;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(Instant lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public Long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }

    public Long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Long failureCount) {
        this.failureCount = failureCount;
    }

    public Map<String, Object> getFailureDetails() {
        return failureDetails;
    }

    public void setFailureDetails(Map<String, Object> failureDetails) {
        this.failureDetails = failureDetails;
    }

    // Convenience methods
    public void addRuleParameter(String key, Object value) {
        if (this.ruleParameters == null) {
            this.ruleParameters = new HashMap<>();
        }
        this.ruleParameters.put(key, value);
    }

    public void addValidationLogic(String key, Object value) {
        if (this.validationLogic == null) {
            this.validationLogic = new HashMap<>();
        }
        this.validationLogic.put(key, value);
    }

    public void addFailureDetail(String key, Object value) {
        if (this.failureDetails == null) {
            this.failureDetails = new HashMap<>();
        }
        this.failureDetails.put(key, value);
    }

    public void incrementExecutionCount() {
        this.executionCount = executionCount != null ? executionCount + 1 : 1L;
    }

    public void incrementFailureCount() {
        this.failureCount = failureCount != null ? failureCount + 1 : 1L;
    }

    public void updateLastExecuted() {
        this.lastExecutedAt = Instant.now();
    }

    public void updateTimestamps() {
        this.updatedAt = Instant.now();
    }

    public boolean isConnectivityRule() {
        return "CONNECTIVITY".equals(ruleType);
    }

    public boolean isLoopDetectionRule() {
        return "LOOP_DETECTION".equals(ruleType);
    }

    public boolean isPolicyComplianceRule() {
        return "POLICY_COMPLIANCE".equals(ruleType);
    }

    public boolean isSecurityRule() {
        return "SECURITY".equals(ruleType);
    }

    public boolean isCustomRule() {
        return "CUSTOM".equals(ruleType);
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

    @Override
    public String toString() {
        return "ValidationRule{" +
                "id=" + getId() +
                ", ruleName='" + ruleName + ''' +
                ", ruleType='" + ruleType + ''' +
                ", severity='" + severity + ''' +
                ", enabled=" + enabled +
                ", executionCount=" + executionCount +
                ", failureCount=" + failureCount +
                '}';
    }
}
