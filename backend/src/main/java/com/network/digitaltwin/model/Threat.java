package com.network.digitaltwin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.dialect.PostgreSQLInetJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a known threat or vulnerability in the system.
 */
@Entity
@Table(name = "threats",
       indexes = {
           @Index(name = "idx_threats_name", columnList = "name"),
           @Index(name = "idx_threats_cve_id", columnList = "cve_id"),
           @Index(name = "idx_threats_severity", columnList = "severity"),
           @Index(name = "idx_threats_last_seen", columnList = "last_seen")
       })
public class Threat extends BaseEntity {

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "cve_id")
    private String cveId;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "threat_type", nullable = false)
    private String threatType; // e.g., "MALWARE", "PHISHING", "DDoS", "VULNERABILITY", etc.

    @Column(name = "severity", nullable = false)
    private String severity; // e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL"

    @Column(name = "attack_vector")
    private String attackVector; // e.g., "NETWORK", "LOCAL", "PHYSICAL", "ADJACENT"

    @Column(name = "complexity")
    private String complexity; // e.g., "LOW", "MEDIUM", "HIGH"

    @Column(name = "privileges_required")
    private String privilegesRequired; // e.g., "NONE", "LOW", "HIGH"

    @Column(name = "user_interaction")
    private Boolean userInteraction;

    @Column(name = "scope")
    private String scope; // e.g., "UNCHANGED", "CHANGED"

    @Column(name = "confidentiality_impact")
    private String confidentialityImpact; // e.g., "NONE", "LOW", "HIGH"

    @Column(name = "integrity_impact")
    private String integrityImpact; // e.g., "NONE", "LOW", "HIGH"

    @Column(name = "availability_impact")
    private String availabilityImpact; // e.g., "NONE", "LOW", "HIGH"

    @Column(name = "base_score")
    private BigDecimal baseScore;

    @Column(name = "exploitability_score")
    private BigDecimal exploitabilityScore;

    @Column(name = "impact_score")
    private BigDecimal impactScore;

    @Column(name = "remediation_level")
    private String remediationLevel; // e.g., "OFFICIAL", "TEMPORARY", "WORKAROUND", "UNAVAILABLE"

    @Column(name = "report_confidence")
    private String reportConfidence; // e.g., "UNKNOWN", "REASONABLE", "CONFIRMED"

    @Column(name = "reference_url")
    private String referenceUrl;

    @Column(name = "detection_method")
    private String detectionMethod;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "active")
    private Boolean active = true;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "affected_products", columnDefinition = "jsonb")
    private Map<String, Object> affectedProducts = new HashMap<>();

    // Constructors
    public Threat() {
    }

    public Threat(String name, String threatType, String severity) {
        this.name = name;
        this.threatType = threatType;
        this.severity = severity;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThreatType() {
        return threatType;
    }

    public void setThreatType(String threatType) {
        this.threatType = threatType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getAttackVector() {
        return attackVector;
    }

    public void setAttackVector(String attackVector) {
        this.attackVector = attackVector;
    }

    public String getComplexity() {
        return complexity;
    }

    public void setComplexity(String complexity) {
        this.complexity = complexity;
    }

    public String getPrivilegesRequired() {
        return privilegesRequired;
    }

    public void setPrivilegesRequired(String privilegesRequired) {
        this.privilegesRequired = privilegesRequired;
    }

    public Boolean getUserInteraction() {
        return userInteraction;
    }

    public void setUserInteraction(Boolean userInteraction) {
        this.userInteraction = userInteraction;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getConfidentialityImpact() {
        return confidentialityImpact;
    }

    public void setConfidentialityImpact(String confidentialityImpact) {
        this.confidentialityImpact = confidentialityImpact;
    }

    public String getIntegrityImpact() {
        return integrityImpact;
    }

    public void setIntegrityImpact(String integrityImpact) {
        this.integrityImpact = integrityImpact;
    }

    public String getAvailabilityImpact() {
        return availabilityImpact;
    }

    public void setAvailabilityImpact(String availabilityImpact) {
        this.availabilityImpact = availabilityImpact;
    }

    public BigDecimal getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(BigDecimal baseScore) {
        this.baseScore = baseScore;
    }

    public BigDecimal getExploitabilityScore() {
        return exploitabilityScore;
    }

    public void setExploitabilityScore(BigDecimal exploitabilityScore) {
        this.exploitabilityScore = exploitabilityScore;
    }

    public BigDecimal getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(BigDecimal impactScore) {
        this.impactScore = impactScore;
    }

    public String getRemediationLevel() {
        return remediationLevel;
    }

    public void setRemediationLevel(String remediationLevel) {
        this.remediationLevel = remediationLevel;
    }

    public String getReportConfidence() {
        return reportConfidence;
    }

    public void setReportConfidence(String reportConfidence) {
        this.reportConfidence = reportConfidence;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public void setReferenceUrl(String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getAffectedProducts() {
        return affectedProducts;
    }

    public void setAffectedProducts(Map<String, Object> affectedProducts) {
        this.affectedProducts = affectedProducts;
    }

    // Convenience methods
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void addAffectedProduct(String key, Object value) {
        this.affectedProducts.put(key, value);
    }

    public void updateLastSeen() {
        this.lastSeen = Instant.now();
    }

    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    public boolean isHighSeverity() {
        return "HIGH".equals(severity) || isCritical();
    }

    public boolean isMediumSeverity() {
        return "MEDIUM".equals(severity);
    }

    public boolean isLowSeverity() {
        return "LOW".equals(severity);
    }

    public boolean isActiveThreat() {
        return active != null && active;
    }

    @Override
    public String toString() {
        return "Threat{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", cveId='" + cveId + '\'' +
                ", threatType='" + threatType + '\'' +
                ", severity='" + severity + '\'' +
                ", active=" + active +
                '}';
    }
}
