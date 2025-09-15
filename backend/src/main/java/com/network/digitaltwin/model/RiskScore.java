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
 * Entity representing the current risk score for a device or network.
 */
@Entity
@Table(name = "risk_scores",
       indexes = {
           @Index(name = "idx_risk_scores_device_id", columnList = "device_id"),
           @Index(name = "idx_risk_scores_score", columnList = "score"),
           @Index(name = "idx_risk_scores_computed_at", columnList = "computed_at")
       })
public class RiskScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "score_type", nullable = false)
    private String scoreType; // e.g., "OVERALL", "THREAT", "VULNERABILITY", "COMPLIANCE"

    @NotNull
    @Column(name = "score", nullable = false)
    private BigDecimal score;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "min_score")
    private BigDecimal minScore;

    @Column(name = "threshold_low")
    private BigDecimal thresholdLow;

    @Column(name = "threshold_medium")
    private BigDecimal thresholdMedium;

    @Column(name = "threshold_high")
    private BigDecimal thresholdHigh;

    @Column(name = "status")
    private String status; // e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL"

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "risk_factors", columnDefinition = "jsonb")
    private Map<String, Object> riskFactors = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details = new HashMap<>();

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "computed_by")
    private User computedBy;

    @Column(name = "computation_method")
    private String computationMethod; // e.g., "WEIGHTED_SUM", "NEURAL_NETWORK", "RULE_BASED"

    @Column(name = "confidence")
    private BigDecimal confidence; // 0.0 to 1.0 scale

    @Column(name = "valid_until")
    private Instant validUntil;

    // Constructors
    public RiskScore() {
    }

    public RiskScore(Device device, String scoreType, BigDecimal score) {
        this.device = device;
        this.scoreType = scoreType;
        this.score = score;
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

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
        this.updateStatus();
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

    public BigDecimal getThresholdLow() {
        return thresholdLow;
    }

    public void setThresholdLow(BigDecimal thresholdLow) {
        this.thresholdLow = thresholdLow;
    }

    public BigDecimal getThresholdMedium() {
        return thresholdMedium;
    }

    public void setThresholdMedium(BigDecimal thresholdMedium) {
        this.thresholdMedium = thresholdMedium;
    }

    public BigDecimal getThresholdHigh() {
        return thresholdHigh;
    }

    public void setThresholdHigh(BigDecimal thresholdHigh) {
        this.thresholdHigh = thresholdHigh;
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

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
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

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    // Convenience methods
    public void addRiskFactor(String key, Object value) {
        this.riskFactors.put(key, value);
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }

    public void updateStatus() {
        if (thresholdHigh != null && score.compareTo(thresholdHigh) >= 0) {
            this.status = "CRITICAL";
        } else if (thresholdMedium != null && score.compareTo(thresholdMedium) >= 0) {
            this.status = "HIGH";
        } else if (thresholdLow != null && score.compareTo(thresholdLow) >= 0) {
            this.status = "MEDIUM";
        } else {
            this.status = "LOW";
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

    public boolean isValid() {
        return validUntil == null || Instant.now().isBefore(validUntil);
    }

    public void refresh() {
        this.computedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void expire() {
        this.validUntil = Instant.now();
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "RiskScore{" +
                "id=" + getId() +
                ", device=" + (device != null ? device.getName() : "null") +
                ", scoreType='" + scoreType + '\'' +
                ", score=" + score +
                ", status='" + status + '\'' +
                ", computedAt=" + computedAt +
                '}';
    }
}
