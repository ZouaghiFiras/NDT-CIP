package com.network.digitaltwin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing external feeds that provide threat intelligence.
 */
@Entity
@Table(name = "external_feeds",
       indexes = {
           @Index(name = "idx_external_feeds_name", columnList = "name"),
           @Index(name = "idx_external_feeds_type", columnList = "type"),
           @Index(name = "idx_external_feeds_last_updated", columnList = "last_updated"),
           @Index(name = "idx_external_feeds_status", columnList = "status")
       })
public class ExternalFeed extends BaseEntity {

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "type", nullable = false)
    private String type; // e.g., "CVE", "THREAT_INTEL", "MALWARE_HASH", "IP_REPUTATION"

    @Column(name = "url")
    private String url;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "status")
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, ERROR, PAUSED

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "update_frequency")
    private Integer updateFrequency; // in hours

    @Column(name = "auto_update")
    private Boolean autoUpdate = true;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "source")
    private String source; // e.g., "MITRE", "VIRUSTOTAL", "ABUSEIPDB"

    @Column(name = "version")
    private String version;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "total_records")
    private Long totalRecords = 0L;

    @Column(name = "records_updated")
    private Long recordsUpdated = 0L;

    // Constructors
    public ExternalFeed() {
    }

    public ExternalFeed(String name, String type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Integer getUpdateFrequency() {
        return updateFrequency;
    }

    public void setUpdateFrequency(Integer updateFrequency) {
        this.updateFrequency = updateFrequency;
    }

    public Boolean getAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(Boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Long getRecordsUpdated() {
        return recordsUpdated;
    }

    public void setRecordsUpdated(Long recordsUpdated) {
        this.recordsUpdated = recordsUpdated;
    }

    // Convenience methods
    public void addConfig(String key, Object value) {
        this.config.put(key, value);
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void recordUpdate() {
        this.lastUpdated = Instant.now();
        this.status = "ACTIVE";
        this.lastError = null;
        this.updatedAt = Instant.now();
    }

    public void recordError(String error) {
        this.status = "ERROR";
        this.lastError = error;
        this.updatedAt = Instant.now();
    }

    public void pause() {
        this.status = "PAUSED";
        this.updatedAt = Instant.now();
    }

    public void resume() {
        this.status = "ACTIVE";
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = "INACTIVE";
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isPaused() {
        return "PAUSED".equals(status);
    }

    public boolean hasError() {
        return "ERROR".equals(status);
    }

    public boolean isInactive() {
        return "INACTIVE".equals(status);
    }

    public boolean isExpired() {
        return validUntil != null && LocalDate.now().isAfter(validUntil);
    }

    public boolean needsUpdate() {
        if (!autoUpdate || lastUpdated == null) {
            return false;
        }

        if (updateFrequency == null || updateFrequency <= 0) {
            return false;
        }

        Instant nextUpdate = lastUpdated.plusHours(updateFrequency);
        return Instant.now().isAfter(nextUpdate);
    }

    public void incrementRecordsUpdated() {
        this.recordsUpdated++;
        this.updatedAt = Instant.now();
    }

    public void incrementTotalRecords() {
        this.totalRecords++;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "ExternalFeed{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", source='" + source + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
