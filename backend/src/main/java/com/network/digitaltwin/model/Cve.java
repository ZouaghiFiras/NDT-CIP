package com.network.digitaltwin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing Common Vulnerabilities and Exposures (CVE) records.
 */
@Entity
@Table(name = "cves",
       indexes = {
           @Index(name = "idx_cves_cve_id", columnList = "cve_id"),
           @Index(name = "idx_cves_published_date", columnList = "published_date"),
           @Index(name = "idx_cves_last_modified", columnList = "last_modified"),
           @Index(name = "idx_cves_severity", columnList = "severity")
       })
public class Cve extends BaseEntity {

    @NotNull
    @Column(name = "cve_id", nullable = false, unique = true)
    private String cveId; // e.g., "CVE-2021-44228"

    @Column(name = "cve_url")
    private String cveUrl;

    @Column(name = "summary")
    private String summary;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "last_modified")
    private LocalDate lastModified;

    @Column(name = "severity")
    private String severity; // e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL"

    @Column(name = "base_score")
    private BigDecimal baseScore;

    @Column(name = "exploitability_score")
    private BigDecimal exploitabilityScore;

    @Column(name = "impact_score")
    private BigDecimal impactScore;

    @Column(name = "attack_vector")
    private String attackVector; // e.g., "NETWORK", "LOCAL", "PHYSICAL"

    @Column(name = "attack_complexity")
    private String attackComplexity; // e.g., "LOW", "HIGH"

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

    @Column(name = "remediation_level")
    private String remediationLevel; // e.g., "OFFICIAL", "TEMPORARY", "WORKAROUND", "UNAVAILABLE"

    @Column(name = "report_confidence")
    private String reportConfidence; // e.g., "UNKNOWN", "REASONABLE", "CONFIRMED"

    @Column(name = "vendor_url")
    private String vendorUrl;

    @Column(name = "references", columnDefinition = "text")
    private String references;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "configurations", columnDefinition = "jsonb")
    private Map<String, Object> configurations = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "affected_products", columnDefinition = "jsonb")
    private Map<String, Object> affectedProducts = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "exploits", columnDefinition = "jsonb")
    private Map<String, Object> exploits = new HashMap<>();

    @Column(name = "is_zero_day")
    private Boolean isZeroDay = false;

    @Column(name = "detection_method")
    private String detectionMethod;

    @Column(name = "detection_date")
    private LocalDate detectionDate;

    @Column(name = "active_threats_count")
    private Long activeThreatsCount = 0L;

    @Column(name = "affected_devices_count")
    private Long affectedDevicesCount = 0L;

    @Column(name = "patch_available")
    private Boolean patchAvailable = true;

    @Column(name = "patch_url")
    private String patchUrl;

    @Column(name = "workaround_available")
    private Boolean workaroundAvailable = true;

    @Column(name = "workaround_description")
    private String workaroundDescription;

    // Constructors
    public Cve() {
    }

    public Cve(String cveId, String summary, String severity) {
        this.cveId = cveId;
        this.summary = summary;
        this.severity = severity;
    }

    // Getters and Setters
    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getCveUrl() {
        return cveUrl;
    }

    public void setCveUrl(String cveUrl) {
        this.cveUrl = cveUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDate publishedDate) {
        this.publishedDate = publishedDate;
    }

    public LocalDate getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDate lastModified) {
        this.lastModified = lastModified;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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

    public String getAttackVector() {
        return attackVector;
    }

    public void setAttackVector(String attackVector) {
        this.attackVector = attackVector;
    }

    public String getAttackComplexity() {
        return attackComplexity;
    }

    public void setAttackComplexity(String attackComplexity) {
        this.attackComplexity = attackComplexity;
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

    public String getVendorUrl() {
        return vendorUrl;
    }

    public void setVendorUrl(String vendorUrl) {
        this.vendorUrl = vendorUrl;
    }

    public String getReferences() {
        return references;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public Map<String, Object> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Map<String, Object> configurations) {
        this.configurations = configurations;
    }

    public Map<String, Object> getAffectedProducts() {
        return affectedProducts;
    }

    public void setAffectedProducts(Map<String, Object> affectedProducts) {
        this.affectedProducts = affectedProducts;
    }

    public Map<String, Object> getExploits() {
        return exploits;
    }

    public void setExploits(Map<String, Object> exploits) {
        this.exploits = exploits;
    }

    public Boolean getIsZeroDay() {
        return isZeroDay;
    }

    public void setIsZeroDay(Boolean isZeroDay) {
        this.isZeroDay = isZeroDay;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public LocalDate getDetectionDate() {
        return detectionDate;
    }

    public void setDetectionDate(LocalDate detectionDate) {
        this.detectionDate = detectionDate;
    }

    public Long getActiveThreatsCount() {
        return activeThreatsCount;
    }

    public void setActiveThreatsCount(Long activeThreatsCount) {
        this.activeThreatsCount = activeThreatsCount;
    }

    public Long getAffectedDevicesCount() {
        return affectedDevicesCount;
    }

    public void setAffectedDevicesCount(Long affectedDevicesCount) {
        this.affectedDevicesCount = affectedDevicesCount;
    }

    public Boolean getPatchAvailable() {
        return patchAvailable;
    }

    public void setPatchAvailable(Boolean patchAvailable) {
        this.patchAvailable = patchAvailable;
    }

    public String getPatchUrl() {
        return patchUrl;
    }

    public void setPatchUrl(String patchUrl) {
        this.patchUrl = patchUrl;
    }

    public Boolean getWorkaroundAvailable() {
        return workaroundAvailable;
    }

    public void setWorkaroundAvailable(Boolean workaroundAvailable) {
        this.workaroundAvailable = workaroundAvailable;
    }

    public String getWorkaroundDescription() {
        return workaroundDescription;
    }

    public void setWorkaroundDescription(String workaroundDescription) {
        this.workaroundDescription = workaroundDescription;
    }

    // Convenience methods
    public void addConfiguration(String key, Object value) {
        this.configurations.put(key, value);
    }

    public void addAffectedProduct(String key, Object value) {
        this.affectedProducts.put(key, value);
    }

    public void addExploit(String key, Object value) {
        this.exploits.put(key, value);
    }

    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    public boolean isHigh() {
        return "HIGH".equals(severity) || isCritical();
    }

    public boolean isMedium() {
        return "MEDIUM".equals(severity);
    }

    public boolean isLow() {
        return "LOW".equals(severity);
    }

    public boolean isZeroDayVulnerability() {
        return isZeroDay != null && isZeroDay;
    }

    public boolean hasPatch() {
        return patchAvailable != null && patchAvailable;
    }

    public boolean hasWorkaround() {
        return workaroundAvailable != null && workaroundAvailable;
    }

    public boolean hasActiveThreats() {
        return activeThreatsCount != null && activeThreatsCount > 0;
    }

    public boolean affectsDevices() {
        return affectedDevicesCount != null && affectedDevicesCount > 0;
    }

    public void incrementActiveThreats() {
        if (activeThreatsCount == null) {
            activeThreatsCount = 0L;
        }
        activeThreatsCount++;
        this.updatedAt = Instant.now();
    }

    public void decrementActiveThreats() {
        if (activeThreatsCount != null && activeThreatsCount > 0) {
            activeThreatsCount--;
            this.updatedAt = Instant.now();
        }
    }

    public void incrementAffectedDevices() {
        if (affectedDevicesCount == null) {
            affectedDevicesCount = 0L;
        }
        affectedDevicesCount++;
        this.updatedAt = Instant.now();
    }

    public void decrementAffectedDevices() {
        if (affectedDevicesCount != null && affectedDevicesCount > 0) {
            affectedDevicesCount--;
            this.updatedAt = Instant.now();
        }
    }

    public boolean isRecent() {
        if (publishedDate == null) {
            return false;
        }

        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        return !publishedDate.isBefore(oneYearAgo);
    }

    public boolean isOutdated() {
        if (lastModified == null) {
            return false;
        }

        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        return lastModified.isBefore(sixMonthsAgo);
    }

    @Override
    public String toString() {
        return "Cve{" +
                "id=" + getId() +
                ", cveId='" + cveId + '\'' +
                ", summary='" + summary + '\'' +
                ", severity='" + severity + '\'' +
                ", publishedDate=" + publishedDate +
                '}';
    }
}
