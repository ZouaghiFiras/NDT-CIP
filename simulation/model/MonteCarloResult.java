package com.network.digitaltwin.simulation.model;

import java.util.*;

/**
 * Represents the results of a Monte Carlo simulation.
 */
public class MonteCarloResult {

    private UUID id;
    private UUID simulationId;
    private String scenarioName;
    private int iterations;
    private int successfulIterations;
    private Map<String, Integer> outcomeCounts;
    private Map<String, Double> outcomeProbabilities;
    private List<Double> impactScores;
    private List<Double> financialLosses;
    private Map<String, Object> statistics;
    private String[] recommendations;

    /**
     * Default constructor.
     */
    public MonteCarloResult() {
    }

    /**
     * Constructor with simulation ID and scenario name.
     * @param simulationId The ID of the simulation
     * @param scenarioName The name of the scenario
     */
    public MonteCarloResult(UUID simulationId, String scenarioName) {
        this.id = UUID.randomUUID();
        this.simulationId = simulationId;
        this.scenarioName = scenarioName;
        this.iterations = 0;
        this.successfulIterations = 0;
        this.outcomeCounts = new HashMap<>();
        this.outcomeProbabilities = new HashMap<>();
        this.impactScores = new ArrayList<>();
        this.financialLosses = new ArrayList<>();
        this.statistics = new HashMap<>();
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

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public int getSuccessfulIterations() {
        return successfulIterations;
    }

    public void setSuccessfulIterations(int successfulIterations) {
        this.successfulIterations = successfulIterations;
    }

    public Map<String, Integer> getOutcomeCounts() {
        return outcomeCounts;
    }

    public void setOutcomeCounts(Map<String, Integer> outcomeCounts) {
        this.outcomeCounts = outcomeCounts;
    }

    public Map<String, Double> getOutcomeProbabilities() {
        return outcomeProbabilities;
    }

    public void setOutcomeProbabilities(Map<String, Double> outcomeProbabilities) {
        this.outcomeProbabilities = outcomeProbabilities;
    }

    public List<Double> getImpactScores() {
        return impactScores;
    }

    public void setImpactScores(List<Double> impactScores) {
        this.impactScores = impactScores;
    }

    public List<Double> getFinancialLosses() {
        return financialLosses;
    }

    public void setFinancialLosses(List<Double> financialLosses) {
        this.financialLosses = financialLosses;
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }

    public String[] getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String[] recommendations) {
        this.recommendations = recommendations;
    }

    // Convenience Methods
    public void addOutcome(String outcome) {
        if (this.outcomeCounts == null) {
            this.outcomeCounts = new HashMap<>();
        }

        int count = outcomeCounts.getOrDefault(outcome, 0) + 1;
        outcomeCounts.put(outcome, count);

        // Update probabilities
        if (iterations > 0) {
            double probability = (double) count / iterations;
            outcomeProbabilities.put(outcome, probability);
        }
    }

    public void addImpactScore(double score) {
        if (this.impactScores == null) {
            this.impactScores = new ArrayList<>();
        }
        this.impactScores.add(score);
    }

    public void addFinancialLoss(double loss) {
        if (this.financialLosses == null) {
            this.financialLosses = new ArrayList<>();
        }
        this.financialLosses.add(loss);
    }

    public void addStatistic(String key, Object value) {
        if (this.statistics == null) {
            this.statistics = new HashMap<>();
        }
        this.statistics.put(key, value);
    }

    public void incrementIterations() {
        this.iterations++;
    }

    public void incrementSuccessfulIterations() {
        this.successfulIterations++;
    }

    public void calculateStatistics() {
        if (impactScores != null && !impactScores.isEmpty()) {
            double mean = impactScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = impactScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double min = impactScores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double stdDev = calculateStandardDeviation(impactScores);

            addStatistic("impactScoreMean", mean);
            addStatistic("impactScoreMax", max);
            addStatistic("impactScoreMin", min);
            addStatistic("impactScoreStdDev", stdDev);
        }

        if (financialLosses != null && !financialLosses.isEmpty()) {
            double mean = financialLosses.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = financialLosses.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double min = financialLosses.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double stdDev = calculateStandardDeviation(financialLosses);

            addStatistic("financialLossMean", mean);
            addStatistic("financialLossMax", max);
            addStatistic("financialLossMin", min);
            addStatistic("financialLossStdDev", stdDev);
        }

        if (iterations > 0) {
            double successRate = (double) successfulIterations / iterations;
            addStatistic("successRate", successRate);
        }
    }

    private double calculateStandardDeviation(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(x -> Math.pow(x - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }

    public double getImpactScoreMean() {
        return (double) statistics.getOrDefault("impactScoreMean", 0.0);
    }

    public double getImpactScoreMax() {
        return (double) statistics.getOrDefault("impactScoreMax", 0.0);
    }

    public double getImpactScoreMin() {
        return (double) statistics.getOrDefault("impactScoreMin", 0.0);
    }

    public double getImpactScoreStdDev() {
        return (double) statistics.getOrDefault("impactScoreStdDev", 0.0);
    }

    public double getFinancialLossMean() {
        return (double) statistics.getOrDefault("financialLossMean", 0.0);
    }

    public double getFinancialLossMax() {
        return (double) statistics.getOrDefault("financialLossMax", 0.0);
    }

    public double getFinancialLossMin() {
        return (double) statistics.getOrDefault("financialLossMin", 0.0);
    }

    public double getFinancialLossStdDev() {
        return (double) statistics.getOrDefault("financialLossStdDev", 0.0);
    }

    public double getSuccessRate() {
        return (double) statistics.getOrDefault("successRate", 0.0);
    }

    public boolean isHighRisk() {
        return getImpactScoreMean() >= 0.7 || getSuccessRate() >= 0.8;
    }

    public boolean isMediumRisk() {
        return getImpactScoreMean() >= 0.4 && getImpactScoreMean() < 0.7;
    }

    public boolean isLowRisk() {
        return getImpactScoreMean() < 0.4;
    }
}
