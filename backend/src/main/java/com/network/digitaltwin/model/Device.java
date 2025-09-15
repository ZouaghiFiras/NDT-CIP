package com.network.digitaltwin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.dialect.PostgreSQLInetJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a network device in the digital twin.
 */
@Entity
@Table(name = "devices", indexes = {
    @Index(name = "idx_devices_status", columnList = "status", condition = "deleted_at IS NULL"),
    @Index(name = "idx_devices_ip", columnList = "ip_address")
})
@TypeDefs({
    @TypeDef(name = "jsonb", typeClass = org.hibernate.type.JsonBinaryType.class)
})
public class Device extends BaseEntity {

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Type(PostgreSQLInetJdbcType.class)
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "type")
    private String type;

    @Column(name = "os")
    private String os;

    @Column(name = "criticality", nullable = false)
    private Integer criticality = 3; // Default to medium criticality

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "status")
    private String status = "ACTIVE"; // Default status

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "location")
    private String location;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "model")
    private String model;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "risk_score")
    private BigDecimal riskScore = BigDecimal.ZERO;

    @Column(name = "risk_factors")
    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "risk_factors", columnDefinition = "jsonb")
    private Map<String, Object> riskFactors = new HashMap<>();

    // Constructors
    public Device() {
    }

    public Device(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public Integer getCriticality() {
        return criticality;
    }

    public void setCriticality(Integer criticality) {
        this.criticality = criticality;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public Map<String, Object> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(Map<String, Object> riskFactors) {
        this.riskFactors = riskFactors;
    }

    // Convenience methods
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void addRiskFactor(String key, Object value) {
        this.riskFactors.put(key, value);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void updateLastSeen() {
        this.lastSeen = Instant.now();
    }

    @Override
    public String toString() {
        return "Device{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
