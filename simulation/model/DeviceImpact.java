package com.network.digitaltwin.simulation.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the impact on a device during a simulation.
 */
public class DeviceImpact {

    private UUID deviceId;
    private String deviceName;
    private String deviceType;
    private boolean compromised;
    private boolean unavailable;
    private double impactScore;
    private Instant compromiseTime;
    private Instant recoveryTime;
    private Map<String, Object> details;
    private String[] affectedServices;
    private double downtime; // in minutes
    private double dataLoss; // in GB or percentage
    private double financialLoss; // in currency

    /**
     * Default constructor.
     */
    public DeviceImpact() {
    }

    /**
     * Constructor with device ID.
     * @param deviceId The ID of the device
     */
    public DeviceImpact(UUID deviceId) {
        this.deviceId = deviceId;
        this.compromised = false;
        this.unavailable = false;
        this.impactScore = 0.0;
        this.downtime = 0.0;
        this.dataLoss = 0.0;
        this.financialLoss = 0.0;
    }

    // Getters and Setters
    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isCompromised() {
        return compromised;
    }

    public void setCompromised(boolean compromised) {
        this.compromised = compromised;
        if (compromised && compromiseTime == null) {
            this.compromiseTime = Instant.now();
        }
    }

    public boolean isUnavailable() {
        return unavailable;
    }

    public void setUnavailable(boolean unavailable) {
        this.unavailable = unavailable;
    }

    public double getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(double impactScore) {
        this.impactScore = impactScore;
    }

    public Instant getCompromiseTime() {
        return compromiseTime;
    }

    public void setCompromiseTime(Instant compromiseTime) {
        this.compromiseTime = compromiseTime;
    }

    public Instant getRecoveryTime() {
        return recoveryTime;
    }

    public void setRecoveryTime(Instant recoveryTime) {
        this.recoveryTime = recoveryTime;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public String[] getAffectedServices() {
        return affectedServices;
    }

    public void setAffectedServices(String[] affectedServices) {
        this.affectedServices = affectedServices;
    }

    public double getDowntime() {
        return downtime;
    }

    public void setDowntime(double downtime) {
        this.downtime = downtime;
    }

    public double getDataLoss() {
        return dataLoss;
    }

    public void setDataLoss(double dataLoss) {
        this.dataLoss = dataLoss;
    }

    public double getFinancialLoss() {
        return financialLoss;
    }

    public void setFinancialLoss(double financialLoss) {
        this.financialLoss = financialLoss;
    }

    // Convenience Methods
    public void addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }

    public void addAffectedService(String service) {
        if (this.affectedServices == null) {
            this.affectedServices = new String[1];
            this.affectedServices[0] = service;
        } else {
            String[] newServices = new String[affectedServices.length + 1];
            System.arraycopy(affectedServices, 0, newServices, 0, affectedServices.length);
            newServices[affectedServices.length] = service;
            this.affectedServices = newServices;
        }
    }

    public void markAsRecovered() {
        this.compromised = false;
        this.unavailable = false;
        this.recoveryTime = Instant.now();
    }

    public void updateDowntime() {
        if (compromiseTime != null && recoveryTime != null) {
            this.downtime = (double) (recoveryTime.toEpochMilli() - compromiseTime.toEpochMilli()) / 60000; // Convert to minutes
        }
    }

    public boolean isCritical() {
        return impactScore >= 0.8;
    }

    public boolean isHighImpact() {
        return impactScore >= 0.6;
    }

    public boolean isMediumImpact() {
        return impactScore >= 0.4;
    }

    public boolean isLowImpact() {
        return impactScore < 0.4;
    }

    public boolean hasDataLoss() {
        return dataLoss > 0;
    }

    public boolean hasFinancialLoss() {
        return financialLoss > 0;
    }

    public boolean isRecovered() {
        return compromised == false && recoveryTime != null;
    }

    public boolean isCurrentlyCompromised() {
        return compromised && recoveryTime == null;
    }
}
