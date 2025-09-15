package com.network.digitaltwin.simulation.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single outcome in a simulation.
 */
public class SimulationOutcome {

    private UUID id;
    private String type; // e.g., "DEVICE_COMPROMISED", "DEVICE_FAILURE", "LINK_FAILURE"
    private UUID deviceId;
    private UUID connectionId;
    private Instant timestamp;
    private String description;
    private double impactScore;
    private Map<String, Object> details;
    private boolean isSuccess;
    private String errorMessage;

    /**
     * Default constructor.
     */
    public SimulationOutcome() {
        this.id = UUID.randomUUID();
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with type and description.
     * @param type The type of outcome
     * @param description The description of the outcome
     */
    public SimulationOutcome(String type, String description) {
        this();
        this.type = type;
        this.description = description;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(double impactScore) {
        this.impactScore = impactScore;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Convenience Methods
    public void addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }

    public boolean isDeviceRelated() {
        return deviceId != null;
    }

    public boolean isConnectionRelated() {
        return connectionId != null;
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
}
