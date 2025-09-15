package com.network.digitaltwin.simulation.model;

import java.util.Map;

/**
 * Represents a single step in a failure scenario.
 */
public class FailureStep {

    private String name;
    private String description;
    private String type; // e.g., "DEVICE_FAILURE", "LINK_FAILURE", "POWER_OUTAGE", "CONFIG_ERROR"
    private Map<String, Object> targetCriteria; // Criteria to identify target devices/connections
    private double failureProbability; // Base failure probability (0.0-1.0)
    private double impactScore; // Impact score if failed (0.0-1.0)
    private String impactDescription; // Description of the impact
    private boolean isCascading; // Whether this failure can trigger other failures
    private String[] cascadingSteps; // IDs of steps that can be triggered
    private int repairTime; // Estimated repair time in minutes

    /**
     * Default constructor.
     */
    public FailureStep() {
    }

    /**
     * Constructor with name and description.
     * @param name The name of the step
     * @param description The description of the step
     */
    public FailureStep(String name, String description) {
        this.name = name;
        this.description = description;
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

    public Map<String, Object> getTargetCriteria() {
        return targetCriteria;
    }

    public void setTargetCriteria(Map<String, Object> targetCriteria) {
        this.targetCriteria = targetCriteria;
    }

    public double getFailureProbability() {
        return failureProbability;
    }

    public void setFailureProbability(double failureProbability) {
        this.failureProbability = failureProbability;
    }

    public double getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(double impactScore) {
        this.impactScore = impactScore;
    }

    public String getImpactDescription() {
        return impactDescription;
    }

    public void setImpactDescription(String impactDescription) {
        this.impactDescription = impactDescription;
    }

    public boolean isCascading() {
        return isCascading;
    }

    public void setCascading(boolean cascading) {
        isCascading = cascading;
    }

    public String[] getCascadingSteps() {
        return cascadingSteps;
    }

    public void setCascadingSteps(String[] cascadingSteps) {
        this.cascadingSteps = cascadingSteps;
    }

    public int getRepairTime() {
        return repairTime;
    }

    public void setRepairTime(int repairTime) {
        this.repairTime = repairTime;
    }
}
