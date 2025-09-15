package com.network.digitaltwin.monitoring.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.Device;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing the latest status of a device.
 */
@Entity
@Table(name = "device_status",
       indexes = {
           @Index(name = "idx_device_status_device_id", columnList = "device_id"),
           @Index(name = "idx_device_status_status", columnList = "status"),
           @Index(name = "idx_device_status_last_seen", columnList = "last_seen")
       })
public class DeviceStatus extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "status", nullable = false)
    private String status; // HEALTHY, DEGRADED, UNHEALTHY, COMPROMISED, UNKNOWN

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Object> metrics = new HashMap<>();

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "ingest_source")
    private String ingestSource; // AGENT, SIMULATOR, MANUAL

    @Column(name = "heartbeat_id")
    private String heartbeatId;

    @Column(name = "cpu_percent")
    private BigDecimal cpuPercent;

    @Column(name = "memory_percent")
    private BigDecimal memoryPercent;

    @Column(name = "disk_percent")
    private BigDecimal diskPercent;

    @Column(name = "network_in_kb")
    private Long networkInKb;

    @Column(name = "network_out_kb")
    private Long networkOutKb;

    @Column(name = "uptime_seconds")
    private Long uptimeSeconds;

    // Constructors
    public DeviceStatus() {
    }

    public DeviceStatus(Device device, String status, Instant lastSeen, Instant receivedAt) {
        this.device = device;
        this.status = status;
        this.lastSeen = lastSeen;
        this.receivedAt = receivedAt;
    }

    // Getters and Setters
    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getIngestSource() {
        return ingestSource;
    }

    public void setIngestSource(String ingestSource) {
        this.ingestSource = ingestSource;
    }

    public String getHeartbeatId() {
        return heartbeatId;
    }

    public void setHeartbeatId(String heartbeatId) {
        this.heartbeatId = heartbeatId;
    }

    public BigDecimal getCpuPercent() {
        return cpuPercent;
    }

    public void setCpuPercent(BigDecimal cpuPercent) {
        this.cpuPercent = cpuPercent;
    }

    public BigDecimal getMemoryPercent() {
        return memoryPercent;
    }

    public void setMemoryPercent(BigDecimal memoryPercent) {
        this.memoryPercent = memoryPercent;
    }

    public BigDecimal getDiskPercent() {
        return diskPercent;
    }

    public void setDiskPercent(BigDecimal diskPercent) {
        this.diskPercent = diskPercent;
    }

    public Long getNetworkInKb() {
        return networkInKb;
    }

    public void setNetworkInKb(Long networkInKb) {
        this.networkInKb = networkInKb;
    }

    public Long getNetworkOutKb() {
        return networkOutKb;
    }

    public void setNetworkOutKb(Long networkOutKb) {
        this.networkOutKb = networkOutKb;
    }

    public Long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(Long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    // Convenience methods
    public void addMetric(String key, Object value) {
        this.metrics.put(key, value);
    }

    public boolean isHealthy() {
        return "HEALTHY".equals(status);
    }

    public boolean isDegraded() {
        return "DEGRADED".equals(status);
    }

    public boolean isUnhealthy() {
        return "UNHEALTHY".equals(status);
    }

    public boolean isCompromised() {
        return "COMPROMISED".equals(status);
    }

    public boolean isUnknown() {
        return "UNKNOWN".equals(status);
    }

    public boolean hasHighCpu() {
        return cpuPercent != null && cpuPercent.compareTo(new BigDecimal("80")) > 0;
    }

    public boolean hasHighMemory() {
        return memoryPercent != null && memoryPercent.compareTo(new BigDecimal("85")) > 0;
    }

    public boolean hasHighDisk() {
        return diskPercent != null && diskPercent.compareTo(new BigDecimal("90")) > 0;
    }

    public boolean hasNetworkActivity() {
        return (networkInKb != null && networkInKb > 0) || 
               (networkOutKb != null && networkOutKb > 0);
    }

    public boolean isStale() {
        Instant staleThreshold = Instant.now().minusSeconds(300); // 5 minutes
        return lastSeen == null || lastSeen.isBefore(staleThreshold);
    }

    @Override
    public String toString() {
        return "DeviceStatus{" +
                "id=" + getId() +
                ", device=" + (device != null ? device.getName() : "null") +
                ", status='" + status + '\'' +
                ", lastSeen=" + lastSeen +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
