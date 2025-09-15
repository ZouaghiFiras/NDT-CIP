package com.network.digitaltwin.monitoring.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.Device;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing historical metrics for a device.
 * This table can be partitioned by time for better performance with large datasets.
 */
@Entity
@Table(name = "device_metrics",
       indexes = {
           @Index(name = "idx_device_metrics_device_id", columnList = "device_id"),
           @Index(name = "idx_device_metrics_metric_time", columnList = "metric_time"),
           @Index(name = "idx_device_metrics_status", columnList = "status")
       })
public class DeviceMetrics extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "metric_time", nullable = false)
    private Instant metricTime;

    @Column(name = "status", nullable = false)
    private String status; // HEALTHY, DEGRADED, UNHEALTHY, COMPROMISED, UNKNOWN

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

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // JSON string for additional metadata

    // Constructors
    public DeviceMetrics() {
    }

    public DeviceMetrics(Device device, Instant metricTime, String status) {
        this.device = device;
        this.metricTime = metricTime;
        this.status = status;
    }

    // Getters and Setters
    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Instant getMetricTime() {
        return metricTime;
    }

    public void setMetricTime(Instant metricTime) {
        this.metricTime = metricTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    // Convenience methods
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

    @Override
    public String toString() {
        return "DeviceMetrics{" +
                "id=" + getId() +
                ", device=" + (device != null ? device.getName() : "null") +
                ", metricTime=" + metricTime +
                ", status='" + status + ''' +
                ", cpuPercent=" + cpuPercent +
                ", memoryPercent=" + memoryPercent +
                '}';
    }
}
