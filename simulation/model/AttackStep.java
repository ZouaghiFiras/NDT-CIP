package com.network.digitaltwin.simulation.model;

import java.util.Map;

/**
 * Represents a single step in an attack scenario.
 */
public class AttackStep {

    private String name;
    private String description;
    private String type; // e.g., "EXPLOIT", "PHISHING", "MALWARE", "INSIDER"
    private Map<String, Object> targetCriteria; // Criteria to identify target devices
    private double successProbability; // Base success probability (0.0-1.0)
    private double impactScore; // Impact score if successful (0.0-1.0)
    private String impactDescription; // Description of the impact
    private boolean isCascading; // Whether this step can trigger other steps
    private String[] cascadingSteps; // IDs of steps that can be triggered

    /**
     * Default constructor.
     */
    public AttackStep() {
    }

    /**
     * Constructor with name and description.
     * @param name The name of the step
     * @param description The description of the step
     */
    public AttackStep(String name, String description) {
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

    public double getSuccessProbability() {
        return successProbability;
    }

    public void setSuccessProbability(double successProbability) {
        this.successProbability = successProbability;
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
}
