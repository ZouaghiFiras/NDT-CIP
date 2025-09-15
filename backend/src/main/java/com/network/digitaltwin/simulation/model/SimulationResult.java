package com.network.digitaltwin.simulation.model;

import com.network.digitaltwin.model.Simulation;
import java.time.Instant;
import java.util.*;

/**
 * Represents the results of a simulation.
 */
public class SimulationResult {

    private UUID id;
    private UUID simulationId;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private double progress;
    private String scenarioType; // "ATTACK" or "FAILURE"
    private String scenarioName;
    private Map<String, Object> summary;
    private List<SimulationOutcome> outcomes;
    private Map<UUID, DeviceImpact> deviceImpacts;
    private double totalImpactScore;
    private double totalDowntime; // in minutes
    private double expectedLoss; // in currency
    private String[] recommendations;

    /**
     * Default constructor.
     */
    public SimulationResult() {
    }

    /**
     * Constructor with simulation.
     * @param simulation The simulation this result belongs to
     */
    public SimulationResult(Simulation simulation) {
        this.id = UUID.randomUUID();
        this.simulationId = simulation.getId();
        this.startTime = Instant.now();
        this.status = "RUNNING";
        this.progress = 0.0;
        this.scenarioType = simulation.getScenarioType();
        this.scenarioName = simulation.getName();

        // Initialize collections
        this.outcomes = new ArrayList<>();
        this.deviceImpacts = new HashMap<>();
        this.summary = new HashMap<>();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSimulationId() {
        return simulationId;
    }

    public void setSimulationId(UUID simulationId) {
        this.simulationId = simulationId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(String scenarioType) {
        this.scenarioType = scenarioType;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary;
    }

    public List<SimulationOutcome> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<SimulationOutcome> outcomes) {
        this.outcomes = outcomes;
    }

    public Map<UUID, DeviceImpact> getDeviceImpacts() {
        return deviceImpacts;
    }

    public void setDeviceImpacts(Map<UUID, DeviceImpact> deviceImpacts) {
        this.deviceImpacts = deviceImpacts;
    }

    public double getTotalImpactScore() {
        return totalImpactScore;
    }

    public void setTotalImpactScore(double totalImpactScore) {
        this.totalImpactScore = totalImpactScore;
    }

    public double getTotalDowntime() {
        return totalDowntime;
    }

    public void setTotalDowntime(double totalDowntime) {
        this.totalDowntime = totalDowntime;
    }

    public double getExpectedLoss() {
        return expectedLoss;
    }

    public void setExpectedLoss(double expectedLoss) {
        this.expectedLoss = expectedLoss;
    }

    public String[] getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String[] recommendations) {
        this.recommendations = recommendations;
    }

    // Convenience Methods
    public void addOutcome(SimulationOutcome outcome) {
        if (this.outcomes == null) {
            this.outcomes = new ArrayList<>();
        }
        this.outcomes.add(outcome);
    }

    public void addDeviceImpact(UUID deviceId, DeviceImpact impact) {
        if (this.deviceImpacts == null) {
            this.deviceImpacts = new HashMap<>();
        }
        this.deviceImpacts.put(deviceId, impact);
    }

    public void addSummary(String key, Object value) {
        if (this.summary == null) {
            this.summary = new HashMap<>();
        }
        this.summary.put(key, value);
    }

    public void updateProgress(double progress) {
        this.progress = progress;
        if (progress >= 100.0) {
            this.status = "COMPLETED";
            this.endTime = Instant.now();
        }
    }

    public void markAsCompleted() {
        this.status = "COMPLETED";
        this.progress = 100.0;
        this.endTime = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.endTime = Instant.now();
        this.addSummary("error", errorMessage);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isRunning() {
        return "RUNNING".equals(status);
    }

    public boolean isDeviceCompromised(UUID deviceId) {
        DeviceImpact impact = deviceImpacts.get(deviceId);
        return impact != null && impact.isCompromised();
    }

    public boolean isDeviceUnavailable(UUID deviceId) {
        DeviceImpact impact = deviceImpacts.get(deviceId);
        return impact != null && impact.isUnavailable();
    }

    public double getDeviceImpactScore(UUID deviceId) {
        DeviceImpact impact = deviceImpacts.get(deviceId);
        return impact != null ? impact.getImpactScore() : 0.0;
    }

    public int getCompromisedDeviceCount() {
        if (deviceImpacts == null) {
            return 0;
        }

        return (int) deviceImpacts.values().stream()
            .filter(DeviceImpact::isCompromised)
            .count();
    }

    public int getUnavailableDeviceCount() {
        if (deviceImpacts == null) {
            return 0;
        }

        return (int) deviceImpacts.values().stream()
            .filter(DeviceImpact::isUnavailable)
            .count();
    }

    public double getAverageImpactScore() {
        if (deviceImpacts == null || deviceImpacts.isEmpty()) {
            return 0.0;
        }

        return deviceImpacts.values().stream()
            .mapToDouble(DeviceImpact::getImpactScore)
            .average()
            .orElse(0.0);
    }
}
