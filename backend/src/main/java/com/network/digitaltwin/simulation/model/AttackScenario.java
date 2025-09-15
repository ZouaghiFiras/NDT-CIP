package com.network.digitaltwin.simulation.model;

import java.util.List;

/**
 * Represents an attack scenario to be simulated.
 */
public class AttackScenario {

    private String name;
    private String description;
    private List<AttackStep> steps;
    private boolean isMonteCarlo;
    private int iterations;
    private double probability;

    /**
     * Default constructor.
     */
    public AttackScenario() {
    }

    /**
     * Constructor with name and description.
     * @param name The name of the scenario
     * @param description The description of the scenario
     */
    public AttackScenario(String name, String description) {
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

    public List<AttackStep> getSteps() {
        return steps;
    }

    public void setSteps(List<AttackStep> steps) {
        this.steps = steps;
    }

    public boolean isMonteCarlo() {
        return isMonteCarlo;
    }

    public void setMonteCarlo(boolean monteCarlo) {
        isMonteCarlo = monteCarlo;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }
}
