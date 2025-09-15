package com.network.digitaltwin.model;

import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing historical risk score calculations for tracking changes over time.
 */
@Entity
@Table(name = "risk_history",
       indexes = {
           @Index(name = "idx_risk_history_device_id", columnList = "device_id"),
           @Index(name = "idx_risk_history_computed_at", columnList = "computed_at"),
           @Index(name = "idx_risk_history_score_type", columnList = "score_type")
       })
public class RiskHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "scenario_id")
    private UUID scenarioId; // Optional reference to simulation or event that triggered calculation

    @NotNull
    @Column(name = "score_type", nullable = false)
    private String scoreType; // e.g., "OVERALL", "THREAT", "VULNERABILITY", "COMPLIANCE"

    @NotNull
    @Column(name = "previous_score", nullable = false)
    private BigDecimal previousScore;

    @NotNull
    @Column(name = "new_score", nullable = false)
    private BigDecimal newScore;

    @Column(name = "score_change")
    private BigDecimal scoreChange;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "min_score")
    private BigDecimal minScore;

    @Column(name = "status")
    private String status; // e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL"

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "risk_factors", columnDefinition = "jsonb")
    private Map<String, Object> riskFactors = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "reason", columnDefinition = "jsonb")
    private Map<String, Object> reason = new HashMap<>();

    @NotNull
    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "computed_by")
    private User computedBy;

    @Column(name = "computation_method")
    private String computationMethod; // e.g., "WEIGHTED_SUM", "NEURAL_NETWORK", "RULE_BASED"

    @Column(name = "confidence")
    private BigDecimal confidence; // 0.0 to 1.0 scale

    // Constructors
    public RiskHistory() {
    }

    public RiskHistory(Device device, String scoreType, BigDecimal previousScore, BigDecimal newScore) {
        this.device = device;
        this.scoreType = scoreType;
        this.previousScore = previousScore;
        this.newScore = newScore;
        this.scoreChange = newScore.subtract(previousScore);
        this.computedAt = Instant.now();
        this.updateStatus();
    }

    // Getters and Setters
    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(UUID scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public BigDecimal getPreviousScore() {
        return previousScore;
    }

    public void setPreviousScore(BigDecimal previousScore) {
        this.previousScore = previousScore;
    }

    public BigDecimal getNewScore() {
        return newScore;
    }

    public void setNewScore(BigDecimal newScore) {
        this.newScore = newScore;
        this.scoreChange = newScore.subtract(previousScore);
        this.updateStatus();
    }

    public BigDecimal getScoreChange() {
        return scoreChange;
    }

    public void setScoreChange(BigDecimal scoreChange) {
        this.scoreChange = scoreChange;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(Map<String, Object> riskFactors) {
        this.riskFactors = riskFactors;
    }

    public Map<String, Object> getReason() {
        return reason;
    }

    public void setReason(Map<String, Object> reason) {
        this.reason = reason;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public User getComputedBy() {
        return computedBy;
    }

    public void setComputedBy(User computedBy) {
        this.computedBy = computedBy;
    }

    public String getComputationMethod() {
        return computationMethod;
    }

    public void setComputationMethod(String computationMethod) {
        this.computationMethod = computationMethod;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    // Convenience methods
    public void addRiskFactor(String key, Object value) {
        this.riskFactors.put(key, value);
    }

    public void addReason(String key, Object value) {
        this.reason.put(key, value);
    }

    public void updateStatus() {
        if (maxScore != null && newScore.compareTo(maxScore) >= 0) {
            this.status = "CRITICAL";
        } else if (minScore != null && newScore.compareTo(minScore) <= 0) {
            this.status = "LOW";
        } else if (scoreChange.compareTo(BigDecimal.ZERO) > 0) {
            // Score increased
            if (newScore.compareTo(new BigDecimal("0.7")) >= 0) {
                this.status = "CRITICAL";
            } else if (newScore.compareTo(new BigDecimal("0.4")) >= 0) {
                this.status = "HIGH";
            } else {
                this.status = "MEDIUM";
            }
        } else {
            // Score decreased or stayed the same
            if (newScore.compareTo(new BigDecimal("0.2")) < 0) {
                this.status = "LOW";
            } else if (newScore.compareTo(new BigDecimal("0.5")) < 0) {
                this.status = "MEDIUM";
            } else {
                this.status = "HIGH";
            }
        }
    }

    public boolean isCritical() {
        return "CRITICAL".equals(status);
    }

    public boolean isHigh() {
        return "HIGH".equals(status);
    }

    public boolean isMedium() {
        return "MEDIUM".equals(status);
    }

    public boolean isLow() {
        return "LOW".equals(status);
    }

    public boolean isScoreIncreased() {
        return scoreChange != null && scoreChange.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isScoreDecreased() {
        return scoreChange != null && scoreChange.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isScoreUnchanged() {
        return scoreChange == null || scoreChange.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public String toString() {
        return "RiskHistory{" +
                "id=" + getId() +
                ", device=" + (device != null ? device.getName() : "null") +
                ", scoreType='" + scoreType + '\'' +
                ", previousScore=" + previousScore +
                ", newScore=" + newScore +
                ", scoreChange=" + scoreChange +
                ", status='" + status + '\'' +
                ", computedAt=" + computedAt +
                '}';
    }
}
