package com.network.digitaltwin.topology.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.Device;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a network connection between two devices.
 */
@Entity
@Table(name = "topology_connections",
       indexes = {
           @Index(name = "idx_connection_source", columnList = "source_device_id"),
           @Index(name = "idx_connection_target", columnList = "target_device_id"),
           @Index(name = "idx_connection_type", columnList = "connection_type"),
           @Index(name = "idx_connection_status", columnList = "status")
       })
public class Connection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_device_id", nullable = false)
    private Device sourceDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_device_id", nullable = false)
    private Device targetDevice;

    @Column(name = "connection_type", nullable = false)
    private String connectionType; // ETHERNET, WIFI, VPN, OTHER

    @Column(name = "bandwidth", nullable = false)
    private Long bandwidth; // in Mbps

    @Column(name = "latency", nullable = false)
    private Long latency; // in milliseconds

    @Column(name = "reliability", nullable = false, precision = 5, scale = 4)
    private BigDecimal reliability; // 0.0 to 1.0

    @Column(name = "status", nullable = false)
    private String status; // ACTIVE, INACTIVE, DEGRADED, FAILED

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "topology_version")
    private Integer topologyVersion;

    // Constructors
    public Connection() {
    }

    public Connection(Device sourceDevice, Device targetDevice, String connectionType, 
                     Long bandwidth, Long latency, BigDecimal reliability) {
        this.sourceDevice = sourceDevice;
        this.targetDevice = targetDevice;
        this.connectionType = connectionType;
        this.bandwidth = bandwidth;
        this.latency = latency;
        this.reliability = reliability;
        this.status = "ACTIVE";
        this.topologyVersion = 1;
    }

    // Getters and Setters
    public Device getSourceDevice() {
        return sourceDevice;
    }

    public void setSourceDevice(Device sourceDevice) {
        this.sourceDevice = sourceDevice;
    }

    public Device getTargetDevice() {
        return targetDevice;
    }

    public void setTargetDevice(Device targetDevice) {
        this.targetDevice = targetDevice;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public Long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Long bandwidth) {
        this.bandwidth = bandwidth;
    }

    public Long getLatency() {
        return latency;
    }

    public void setLatency(Long latency) {
        this.latency = latency;
    }

    public BigDecimal getReliability() {
        return reliability;
    }

    public void setReliability(BigDecimal reliability) {
        this.reliability = reliability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Integer getTopologyVersion() {
        return topologyVersion;
    }

    public void setTopologyVersion(Integer topologyVersion) {
        this.topologyVersion = topologyVersion;
    }

    // Convenience methods
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isInactive() {
        return "INACTIVE".equals(status);
    }

    public boolean isDegraded() {
        return "DEGRADED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isEthernet() {
        return "ETHERNET".equals(connectionType);
    }

    public boolean isWifi() {
        return "WIFI".equals(connectionType);
    }

    public boolean isVpn() {
        return "VPN".equals(connectionType);
    }

    public boolean isOther() {
        return "OTHER".equals(connectionType);
    }

    public boolean isHighBandwidth() {
        return bandwidth != null && bandwidth > 1000; // > 1 Gbps
    }

    public boolean isHighLatency() {
        return latency != null && latency > 100; // > 100ms
    }

    public boolean isLowReliability() {
        return reliability != null && reliability.compareTo(new BigDecimal("0.9")) < 0;
    }

    public boolean isLoop() {
        return sourceDevice != null && 
               targetDevice != null && 
               sourceDevice.getId().equals(targetDevice.getId());
    }

    @Override
    public String toString() {
        return "Connection{" +
                "id=" + getId() +
                ", sourceDevice=" + (sourceDevice != null ? sourceDevice.getName() : "null") +
                ", targetDevice=" + (targetDevice != null ? targetDevice.getName() : "null") +
                ", connectionType='" + connectionType + ''' +
                ", status='" + status + ''' +
                ", bandwidth=" + bandwidth +
                ", latency=" + latency +
                ", reliability=" + reliability +
                '}';
    }
}
