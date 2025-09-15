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
 * Entity representing a detected threat on a specific device.
 */
@Entity
@Table(name = "device_threats",
       indexes = {
           @Index(name = "idx_device_threats_device_id", columnList = "device_id"),
           @Index(name = "idx_device_threats_threat_id", columnList = "threat_id"),
           @Index(name = "idx_device_threats_detected_at", columnList = "detected_at"),
           @Index(name = "idx_device_threats_confidence", columnList = "confidence")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"device_id", "threat_id", "detected_at"}, name = "uk_device_threat_unique")
       })
public class DeviceThreat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threat_id", nullable = false)
    private Threat threat;

    @NotNull
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "confidence")
    private BigDecimal confidence; // 0.0 to 1.0 scale

    @Column(name = "status")
    private String status = "ACTIVE"; // ACTIVE, RESOLVED, FALSE_POSITIVE, IGNORED

    @Column(name = "first_seen")
    private Instant firstSeen;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "count")
    private Long count = 1L;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "context", columnDefinition = "jsonb")
    private Map<String, Object> context = new HashMap<>();

    @Column(name = "notes")
    private String notes;

    @Column(name = "remediation_applied")
    private Boolean remediationApplied = false;

    @Column(name = "remediation_applied_at")
    private Instant remediationAppliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remediated_by")
    private User remediatedBy;

    @Column(name = "remediation_notes")
    private String remediationNotes;

    // Constructors
    public DeviceThreat() {
    }

    public DeviceThreat(Device device, Threat threat, Instant detectedAt) {
        this.device = device;
        this.threat = threat;
        this.detectedAt = detectedAt;
        this.firstSeen = detectedAt;
        this.lastSeen = detectedAt;
    }

    // Getters and Setters
    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Threat getThreat() {
        return threat;
    }

    public void setThreat(Threat threat) {
        this.threat = threat;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getRemediationApplied() {
        return remediationApplied;
    }

    public void setRemediationApplied(Boolean remediationApplied) {
        this.remediationApplied = remediationApplied;
    }

    public Instant getRemediationAppliedAt() {
        return remediationAppliedAt;
    }

    public void setRemediationAppliedAt(Instant remediationAppliedAt) {
        this.remediationAppliedAt = remediationAppliedAt;
    }

    public User getRemediatedBy() {
        return remediatedBy;
    }

    public void setRemediatedBy(User remediatedBy) {
        this.remediatedBy = remediatedBy;
    }

    public String getRemediationNotes() {
        return remediationNotes;
    }

    public void setRemediationNotes(String remediationNotes) {
        this.remediationNotes = remediationNotes;
    }

    // Convenience methods
    public void addContext(String key, Object value) {
        this.context.put(key, value);
    }

    public void incrementCount() {
        this.count++;
        this.lastSeen = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void resolve() {
        this.status = "RESOLVED";
        this.updatedAt = Instant.now();
    }

    public void markAsFalsePositive() {
        this.status = "FALSE_POSITIVE";
        this.updatedAt = Instant.now();
    }

    public void ignore() {
        this.status = "IGNORED";
        this.updatedAt = Instant.now();
    }

    public void applyRemediation(User user, String notes) {
        this.remediationApplied = true;
        this.remediationAppliedAt = Instant.now();
        this.remediatedBy = user;
        this.remediationNotes = notes;
        this.status = "RESOLVED";
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equals(status);
    }

    public boolean isFalsePositive() {
        return "FALSE_POSITIVE".equals(status);
    }

    public boolean isIgnored() {
        return "IGNORED".equals(status);
    }

    public boolean hasRemediationApplied() {
        return remediationApplied != null && remediationApplied;
    }

    @Override
    public String toString() {
        return "DeviceThreat{" +
                "id=" + getId() +
                ", device=" + (device != null ? device.getName() : "null") +
                ", threat=" + (threat != null ? threat.getName() : "null") +
                ", detectedAt=" + detectedAt +
                ", status='" + status + '\'' +
                ", count=" + count +
                '}';
    }
}
