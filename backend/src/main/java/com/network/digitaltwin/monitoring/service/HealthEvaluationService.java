package com.network.digitaltwin.monitoring.service;

import com.network.digitaltwin.monitoring.model.DeviceStatus;
import com.network.digitaltwin.monitoring.model.DeviceMetrics;
import com.network.digitaltwin.monitoring.model.HealthRule;
import com.network.digitaltwin.monitoring.model.Alert;
import com.network.digitaltwin.monitoring.repository.DeviceMetricsRepository;
import com.network.digitaltwin.monitoring.repository.AlertRepository;
import com.network.digitaltwin.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for evaluating device health based on rules.
 */
@Service
public class HealthEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(HealthEvaluationService.class);

    private static final long METRICS_LOOKBACK_MINUTES = 5;
    private static final double CPU_THRESHOLD = 90.0;
    private static final double MEMORY_THRESHOLD = 85.0;
    private static final double DISK_THRESHOLD = 90.0;
    private static final long HEARTBEAT_TIMEOUT_MINUTES = 5;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceMetricsRepository deviceMetricsRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    /**
     * Evaluate health rules for a device status.
     * @param deviceStatus The device status to evaluate
     * @param rules The rules to evaluate
     */
    public void evaluateRules(DeviceStatus deviceStatus, List<HealthRule> rules) {
        if (deviceStatus == null || rules == null || rules.isEmpty()) {
            return;
        }

        UUID deviceId = deviceStatus.getDevice().getId();
        logger.debug("Evaluating {} health rules for device: {}", rules.size(), deviceId);

        for (HealthRule rule : rules) {
            try {
                if (!rule.isEnabled()) {
                    continue;
                }

                // Check if the rule applies to this device
                if (!deviceMatchesFilter(deviceStatus.getDevice(), rule)) {
                    continue;
                }

                // Check cooldown period
                if (!rule.canTrigger()) {
                    continue;
                }

                // Evaluate the rule
                boolean ruleTriggered = evaluateRule(deviceStatus, rule);

                if (ruleTriggered) {
                    // Create alert if needed
                    createAlertIfNeeded(deviceStatus, rule);

                    // Record rule trigger
                    rule.recordTrigger();
                    // Note: In a real implementation, we would save the rule here

                    logger.info("Health rule triggered for device {}: {}", deviceId, rule.getName());
                }
            } catch (Exception e) {
                logger.error("Error evaluating health rule {} for device: {}", rule.getName(), deviceId, e);
            }
        }
    }

    /**
     * Evaluate a single health rule.
     * @param deviceStatus The device status
     * @param rule The rule to evaluate
     * @return true if the rule is triggered, false otherwise
     */
    private boolean evaluateRule(DeviceStatus deviceStatus, HealthRule rule) {
        String ruleType = rule.getRuleType();

        switch (ruleType) {
            case "THRESHOLD":
                return evaluateThresholdRule(deviceStatus, rule);
            case "ANOMALY":
                return evaluateAnomalyRule(deviceStatus, rule);
            case "TIMEOUT":
                return evaluateTimeoutRule(deviceStatus, rule);
            case "CUSTOM":
                return evaluateCustomRule(deviceStatus, rule);
            default:
                logger.warn("Unknown rule type: {} for rule: {}", ruleType, rule.getName());
                return false;
        }
    }

    /**
     * Evaluate a threshold rule.
     * @param deviceStatus The device status
     * @param rule The threshold rule
     * @return true if the rule is triggered, false otherwise
     */
    private boolean evaluateThresholdRule(DeviceStatus deviceStatus, HealthRule rule) {
        Map<String, Object> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        // Check CPU threshold
        if (conditions.containsKey("cpuPercent")) {
            double threshold = getDoubleValue(conditions, "cpuPercent");
            BigDecimal cpuPercent = deviceStatus.getCpuPercent();
            if (cpuPercent != null && cpuPercent.compareTo(BigDecimal.valueOf(threshold)) > 0) {
                return true;
            }
        }

        // Check memory threshold
        if (conditions.containsKey("memoryPercent")) {
            double threshold = getDoubleValue(conditions, "memoryPercent");
            BigDecimal memoryPercent = deviceStatus.getMemoryPercent();
            if (memoryPercent != null && memoryPercent.compareTo(BigDecimal.valueOf(threshold)) > 0) {
                return true;
            }
        }

        // Check disk threshold
        if (conditions.containsKey("diskPercent")) {
            double threshold = getDoubleValue(conditions, "diskPercent");
            BigDecimal diskPercent = deviceStatus.getDiskPercent();
            if (diskPercent != null && diskPercent.compareTo(BigDecimal.valueOf(threshold)) > 0) {
                return true;
            }
        }

        // Check network thresholds
        if (conditions.containsKey("networkInKb")) {
            long threshold = getLongValue(conditions, "networkInKb");
            Long networkInKb = deviceStatus.getNetworkInKb();
            if (networkInKb != null && networkInKb > threshold) {
                return true;
            }
        }

        if (conditions.containsKey("networkOutKb")) {
            long threshold = getLongValue(conditions, "networkOutKb");
            Long networkOutKb = deviceStatus.getNetworkOutKb();
            if (networkOutKb != null && networkOutKb > threshold) {
                return true;
            }
        }

        return false;
    }

    /**
     * Evaluate an anomaly rule.
     * @param deviceStatus The device status
     * @param rule The anomaly rule
     * @return true if the rule is triggered, false otherwise
     */
    private boolean evaluateAnomalyRule(DeviceStatus deviceStatus, HealthRule rule) {
        Map<String, Object> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        // Get lookback period (default to 5 minutes)
        long lookbackMinutes = conditions.containsKey("lookbackMinutes") ? 
            getLongValue(conditions, "lookbackMinutes") : METRICS_LOOKBACK_MINUTES;

        // Get threshold multiplier (default to 1.5)
        double thresholdMultiplier = conditions.containsKey("thresholdMultiplier") ? 
            getDoubleValue(conditions, "thresholdMultiplier") : 1.5;

        // Get metric to check
        String metric = conditions.containsKey("metric") ? 
            (String) conditions.get("metric") : "cpuPercent";

        // Get historical metrics
        Instant lookbackStart = Instant.now().minus(lookbackMinutes, ChronoUnit.MINUTES);
        List<DeviceMetrics> historicalMetrics = deviceMetricsRepository
            .findByDeviceIdAndMetricTimeBetweenAndStatusOrderByMetricTimeDesc(
                deviceStatus.getDevice().getId(), lookbackStart, Instant.now(), null);

        if (historicalMetrics.isEmpty()) {
            return false;
        }

        // Calculate average and standard deviation
        List<Double> metricValues = historicalMetrics.stream()
            .map(metric -> getMetricValue(metric, metric))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (metricValues.isEmpty()) {
            return false;
        }

        double average = metricValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double standardDeviation = calculateStandardDeviation(metricValues);

        // Get current value
        double currentValue = getMetricValue(deviceStatus, metric);
        if (currentValue == 0.0) {
            return false;
        }

        // Check if current value is an anomaly
        double upperThreshold = average + (standardDeviation * thresholdMultiplier);
        return currentValue > upperThreshold;
    }

    /**
     * Evaluate a timeout rule.
     * @param deviceStatus The device status
     * @param rule The timeout rule
     * @return true if the rule is triggered, false otherwise
     */
    private boolean evaluateTimeoutRule(DeviceStatus deviceStatus, HealthRule rule) {
        Map<String, Object> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        // Get timeout period (default to 5 minutes)
        long timeoutMinutes = conditions.containsKey("timeoutMinutes") ? 
            getLongValue(conditions, "timeoutMinutes") : HEARTBEAT_TIMEOUT_MINUTES;

        // Check if last heartbeat is older than timeout
        Instant lastSeen = deviceStatus.getLastSeen();
        if (lastSeen == null) {
            return true;
        }

        Instant timeoutThreshold = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);
        return lastSeen.isBefore(timeoutThreshold);
    }

    /**
     * Evaluate a custom rule.
     * @param deviceStatus The device status
     * @param rule The custom rule
     * @return true if the rule is triggered, false otherwise
     */
    private boolean evaluateCustomRule(DeviceStatus deviceStatus, HealthRule rule) {
        // In a real implementation, this would use a rule engine or script execution
        // For now, we'll implement a simple example

        Map<String, Object> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        // Example: Check for high CPU and high memory simultaneously
        if (conditions.containsKey("highCpuAndMemory")) {
            boolean check = (boolean) conditions.get("highCpuAndMemory");
            if (check) {
                BigDecimal cpuPercent = deviceStatus.getCpuPercent();
                BigDecimal memoryPercent = deviceStatus.getMemoryPercent();

                return cpuPercent != null && cpuPercent.compareTo(BigDecimal.valueOf(80)) > 0 &&
                       memoryPercent != null && memoryPercent.compareTo(BigDecimal.valueOf(80)) > 0;
            }
        }

        return false;
    }

    /**
     * Check if a device matches the filter criteria of a rule.
     * @param device The device to check
     * @param rule The rule with filter criteria
     * @return true if the device matches the filter, false otherwise
     */
    private boolean deviceMatchesFilter(com.network.digitaltwin.model.Device device, HealthRule rule) {
        Map<String, Object> filter = rule.getDeviceFilter();
        if (filter == null || filter.isEmpty()) {
            return true; // No filter means all devices match
        }

        // Check criticality filter
        if (filter.containsKey("criticality")) {
            int requiredCriticality = getIntValue(filter, "criticality");
            if (device.getCriticality() != null && device.getCriticality() < requiredCriticality) {
                return false;
            }
        }

        // Check type filter
        if (filter.containsKey("type")) {
            String requiredType = (String) filter.get("type");
            if (device.getType() == null || !device.getType().equals(requiredType)) {
                return false;
            }
        }

        // Check owner filter
        if (filter.containsKey("ownerId")) {
            UUID requiredOwner = (UUID) filter.get("ownerId");
            if (device.getOwner() == null || !device.getOwner().getId().equals(requiredOwner)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Create an alert if needed.
     * @param deviceStatus The device status
     * @param rule The rule that triggered
     */
    private void createAlertIfNeeded(DeviceStatus deviceStatus, HealthRule rule) {
        UUID deviceId = deviceStatus.getDevice().getId();

        // Check if there's already an active alert for this rule and device
        boolean existingAlert = alertRepository.existsByDeviceIdAndAlertTypeAndStatus(
            deviceId, rule.getAlertType(), "ACTIVE");

        if (existingAlert) {
            logger.debug("Active alert already exists for device {} and rule type: {}", 
                deviceId, rule.getAlertType());
            return;
        }

        // Create new alert
        Alert alert = new Alert(
            deviceStatus.getDevice(),
            rule.getAlertType(),
            rule.getSeverity(),
            rule.getName() + " triggered: " + rule.getDescription()
        );

        // Add payload with rule information
        alert.addPayload("ruleName", rule.getName());
        alert.addPayload("ruleType", rule.getRuleType());
        alert.addPayload("ruleConditions", rule.getConditions());
        alert.addPayload("deviceStatus", deviceStatus.getStatus());
        alert.addPayload("deviceMetrics", deviceStatus.getMetrics());

        // Save alert
        alert = alertRepository.save(alert);

        // Process alert (send notifications, etc.)
        alertService.processAlert(alert);

        logger.info("Created alert {} for device {}: {}", alert.getId(), deviceId, rule.getAlertType());
    }

    /**
     * Calculate the standard deviation of a list of values.
     * @param values The list of values
     * @return The standard deviation
     */
    private double calculateStandardDeviation(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }

    /**
     * Get the value of a metric from a device status.
     * @param deviceStatus The device status
     * @param metric The metric name
     * @return The metric value as a double, or 0.0 if not found
     */
    private double getMetricValue(DeviceStatus deviceStatus, String metric) {
        switch (metric) {
            case "cpuPercent":
                return deviceStatus.getCpuPercent() != null ? 
                    deviceStatus.getCpuPercent().doubleValue() : 0.0;
            case "memoryPercent":
                return deviceStatus.getMemoryPercent() != null ? 
                    deviceStatus.getMemoryPercent().doubleValue() : 0.0;
            case "diskPercent":
                return deviceStatus.getDiskPercent() != null ? 
                    deviceStatus.getDiskPercent().doubleValue() : 0.0;
            case "networkInKb":
                return deviceStatus.getNetworkInKb() != null ? 
                    deviceStatus.getNetworkInKb().doubleValue() : 0.0;
            case "networkOutKb":
                return deviceStatus.getNetworkOutKb() != null ? 
                    deviceStatus.getNetworkOutKb().doubleValue() : 0.0;
            default:
                // Try to get from metrics map
                Object value = deviceStatus.getMetrics().get(metric);
                return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
        }
    }

    /**
     * Get the value of a metric from device metrics.
     * @param metrics The device metrics
     * @param metric The metric name
     * @return The metric value as a double, or 0.0 if not found
     */
    private double getMetricValue(DeviceMetrics metrics, String metric) {
        switch (metric) {
            case "cpuPercent":
                return metrics.getCpuPercent() != null ? 
                    metrics.getCpuPercent().doubleValue() : 0.0;
            case "memoryPercent":
                return metrics.getMemoryPercent() != null ? 
                    metrics.getMemoryPercent().doubleValue() : 0.0;
            case "diskPercent":
                return metrics.getDiskPercent() != null ? 
                    metrics.getDiskPercent().doubleValue() : 0.0;
            case "networkInKb":
                return metrics.getNetworkInKb() != null ? 
                    metrics.getNetworkInKb().doubleValue() : 0.0;
            case "networkOutKb":
                return metrics.getNetworkOutKb() != null ? 
                    metrics.getNetworkOutKb().doubleValue() : 0.0;
            default:
                // Try to get from metadata
                Object value = metrics.getMetadata();
                if (value instanceof Map) {
                    Object metricValue = ((Map<?, ?>) value).get(metric);
                    return metricValue instanceof Number ? ((Number) metricValue).doubleValue() : 0.0;
                }
                return 0.0;
        }
    }

    /**
     * Get a double value from a map.
     * @param map The map
     * @param key The key
     * @return The double value
     */
    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get a long value from a map.
     * @param map The map
     * @param key The key
     * @return The long value
     */
    private long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * Get an int value from a map.
     * @param map The map
     * @param key The key
     * @return The int value
     */
    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
