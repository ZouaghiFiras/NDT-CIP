package com.network.digitaltwin.model;

import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.dialect.PostgreSQLInetJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entity representing a network connection between two devices.
 */
@Entity
@Table(name = "connections", 
       indexes = {
           @Index(name = "idx_connections_from_device", columnList = "from_device_id"),
           @Index(name = "idx_connections_to_device", columnList = "to_device_id"),
           @Index(name = "idx_connections_type", columnList = "connection_type")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"from_device_id", "to_device_id"}, name = "uk_connection_pair")
       })
@CheckConstraint(name = "ck_no_self_connection", expression = "from_device_id <> to_device_id")
public class Connection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_device_id", nullable = false)
    private Device fromDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_device_id", nullable = false)
    private Device toDevice;

    @NotNull
    @Column(name = "connection_type", nullable = false)
    private String connectionType; // e.g., "ethernet", "wifi", "vpn", etc.

    @Column(name = "bandwidth")
    private BigDecimal bandwidth; // in Mbps

    @Column(name = "latency")
    private BigDecimal latency; // in ms

    @Column(name = "reliability")
    private BigDecimal reliability; // 0.0 to 1.0

    @Column(name = "status")
    private String status = "ACTIVE"; // Default status

    @Type(PostgreSQLInetJdbcType.class)
    @Column(name = "local_ip")
    private String localIp;

    @Type(PostgreSQLInetJdbcType.class)
    @Column(name = "remote_ip")
    private String remoteIp;

    @Column(name = "port")
    private Integer port;

    @Column(name = "protocol")
    private String protocol; // e.g., "TCP", "UDP", "ICMP"

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, Object> properties = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    // Constructors
    public Connection() {
    }

    public Connection(Device fromDevice, Device toDevice, String connectionType) {
        this.fromDevice = fromDevice;
        this.toDevice = toDevice;
        this.connectionType = connectionType;
    }

    // Getters and Setters
    public Device getFromDevice() {
        return fromDevice;
    }

    public void setFromDevice(Device fromDevice) {
        this.fromDevice = fromDevice;
    }

    public Device getToDevice() {
        return toDevice;
    }

    public void setToDevice(Device toDevice) {
        this.toDevice = toDevice;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public BigDecimal getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(BigDecimal bandwidth) {
        this.bandwidth = bandwidth;
    }

    public BigDecimal getLatency() {
        return latency;
    }

    public void setLatency(BigDecimal latency) {
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

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    // Convenience methods
    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void updateLastSeen() {
        setUpdatedAt(Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return Objects.equals(fromDevice, that.fromDevice) &&
                Objects.equals(toDevice, that.toDevice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromDevice, toDevice);
    }

    @Override
    public String toString() {
        return "Connection{" +
                "id=" + getId() +
                ", fromDevice=" + (fromDevice != null ? fromDevice.getName() : "null") +
                ", toDevice=" + (toDevice != null ? toDevice.getName() : "null") +
                ", connectionType='" + connectionType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
