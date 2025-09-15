package com.network.digitaltwin.topology.service;

import com.network.digitaltwin.model.auth.User;
import com.network.digitaltwin.topology.model.Alert;
import com.network.digitaltwin.topology.model.Connection;
import com.network.digitaltwin.topology.model.Device;
import com.network.digitaltwin.topology.model.ValidationRule;
import com.network.digitaltwin.topology.model.ValidationRun;
import com.network.digitaltwin.topology.repository.AlertRepository;
import com.network.digitaltwin.topology.repository.ConnectionRepository;
import com.network.digitaltwin.topology.repository.DeviceRepository;
import com.network.digitaltwin.topology.repository.ValidationRuleRepository;
import com.network.digitaltwin.topology.repository.ValidationRunRepository;
import com.network.digitaltwin.repository.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for topology validation operations.
 */
@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    @Autowired
    private ValidationRunRepository validationRunRepository;

    @Autowired
    private ValidationRuleRepository validationRuleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * Run topology validation asynchronously.
     * @param triggeredBy How the validation was triggered
     * @param userId The user triggering the validation (optional)
     * @return The validation run ID
     */
    @Async
    @Transactional
    public CompletableFuture<String> runValidation(String triggeredBy, UUID userId) {
        logger.info("Starting topology validation triggered by: {}", triggeredBy);

        // Get user if provided
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        }

        // Generate run ID
        String runId = UUID.randomUUID().toString();

        // Create validation run
        ValidationRun validationRun = new ValidationRun(runId, triggeredBy, user);
        validationRun = validationRunRepository.save(validationRun);

        try {
            // Start validation run
            validationRun.start();
            validationRunRepository.save(validationRun);

            // Get enabled validation rules
            List<ValidationRule> rules = validationRuleRepository.findByEnabledOrderByExecutionOrder(true);

            // Execute rules
            for (ValidationRule rule : rules) {
                try {
                    executeRule(validationRun, rule);
                } catch (Exception e) {
                    logger.error("Error executing validation rule: " + rule.getRuleName(), e);

                    // Add rule result
                    validationRun.addRuleResult(rule.getRuleName(), 
                        Map.of("status", "FAILED", "error", e.getMessage()));

                    // Update failure count
                    rule.incrementFailureCount();
                    rule.addFailureDetail("lastError", e.getMessage());
                    validationRuleRepository.save(rule);
                }
            }

            // Complete validation run
            validationRun.complete();
            validationRunRepository.save(validationRun);

            logger.info("Completed topology validation run: {}", runId);

            // Send completion notification
            sendValidationCompletionNotification(validationRun);

            return CompletableFuture.completedFuture(runId);

        } catch (Exception e) {
            logger.error("Error running topology validation", e);

            // Mark validation run as failed
            validationRun.fail(e.getMessage());
            validationRunRepository.save(validationRun);

            // Send failure notification
            sendValidationFailureNotification(validationRun);

            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Execute a validation rule.
     * @param validationRun The validation run
     * @param rule The validation rule
     */
    private void executeRule(ValidationRun validationRun, ValidationRule rule) {
        logger.debug("Executing validation rule: {}", rule.getRuleName());

        // Update validation run
        validationRun.addExecutedRule(rule.getRuleName());
        validationRunRepository.save(validationRun);

        // Update rule
        rule.incrementExecutionCount();
        rule.updateLastExecuted();
        validationRuleRepository.save(rule);

        // Execute rule based on type
        Map<String, Object> result = new HashMap<>();
        List<Alert> alerts = new ArrayList<>();

        switch (rule.getRuleType()) {
            case "CONNECTIVITY":
                alerts = executeConnectivityRule(rule);
                break;
            case "LOOP_DETECTION":
                alerts = executeLoopDetectionRule(rule);
                break;
            case "POLICY_COMPLIANCE":
                alerts = executePolicyComplianceRule(rule);
                break;
            case "SECURITY":
                alerts = executeSecurityRule(rule);
                break;
            case "CUSTOM":
                alerts = executeCustomRule(rule);
                break;
            default:
                throw new IllegalArgumentException("Unknown rule type: " + rule.getRuleType());
        }

        // Save alerts
        for (Alert alert : alerts) {
            alert = alertRepository.save(alert);
            validationRun.addAlertId(alert.getId());
        }

        // Add rule result
        result.put("status", alerts.isEmpty() ? "PASSED" : "FAILED");
        result.put("alertCount", alerts.size());
        result.put("alerts", alerts.stream().map(Alert::getId).collect(Collectors.toList()));
        validationRun.addRuleResult(rule.getRuleName(), result);
    }

    /**
     * Execute a connectivity validation rule.
     * @param rule The validation rule
     * @return List of alerts generated
     */
    private List<Alert> executeConnectivityRule(ValidationRule rule) {
        logger.debug("Executing connectivity rule: {}", rule.getRuleName());

        List<Alert> alerts = new ArrayList<>();

        // Get all devices
        List<Device> devices = deviceRepository.findAll();

        // Get all connections
        List<Connection> connections = connectionRepository.findAll();

        // Check for unreachable devices
        for (Device device : devices) {
            boolean hasConnection = connections.stream()
                .anyMatch(conn -> 
                    conn.getSourceDevice().getId().equals(device.getId()) || 
                    conn.getTargetDevice().getId().equals(device.getId()));

            if (!hasConnection) {
                // Device has no connections
                Alert alert = new Alert(
                    "CONNECTIVITY_ISSUE",
                    rule.getSeverity(),
                    "Device '" + device.getName() + "' has no network connections",
                    List.of(device.getId()),
                    Collections.emptyList()
                );
                alerts.add(alert);
            }
        }

        // Check for single points of failure
        Map<Device, Long> connectionCounts = new HashMap<>();
        for (Connection connection : connections) {
            connectionCounts.merge(connection.getSourceDevice(), 1L, Long::sum);
            connectionCounts.merge(connection.getTargetDevice(), 1L, Long::sum);
        }

        // Find devices with only one connection
        List<Device> singleConnectionDevices = connectionCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1L)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Check if any of these are critical devices
        for (Device device : singleConnectionDevices) {
            if (device.getCriticality() >= 4) { // High criticality
                Alert alert = new Alert(
                    "CONNECTIVITY_ISSUE",
                    rule.getSeverity(),
                    "High-criticality device '" + device.getName() + "' has only one connection (single point of failure)",
                    List.of(device.getId()),
                    Collections.emptyList()
                );
                alerts.add(alert);
            }
        }

        // Verify redundant paths for critical devices
        for (Device device : devices) {
            if (device.getCriticality() >= 4) { // High criticality
                // Check if device has redundant paths
                boolean hasRedundantPath = false;

                // This is a simplified check - in a real implementation, we would
                // use graph algorithms to find multiple paths between devices
                long connectionCount = connectionCounts.getOrDefault(device, 0L);
                if (connectionCount >= 2) {
                    hasRedundantPath = true;
                }

                if (!hasRedundantPath) {
                    Alert alert = new Alert(
                        "CONNECTIVITY_ISSUE",
                        rule.getSeverity(),
                        "High-criticality device '" + device.getName() + "' lacks redundant network paths",
                        List.of(device.getId()),
                        Collections.emptyList()
                    );
                    alerts.add(alert);
                }
            }
        }

        return alerts;
    }

    /**
     * Execute a loop detection validation rule.
     * @param rule The validation rule
     * @return List of alerts generated
     */
    private List<Alert> executeLoopDetectionRule(ValidationRule rule) {
        logger.debug("Executing loop detection rule: {}", rule.getRuleName());

        List<Alert> alerts = new ArrayList<>();

        // Get all connections
        List<Connection> connections = connectionRepository.findAll();

        // Build adjacency list
        Map<UUID, List<UUID>> adjacencyList = new HashMap<>();
        for (Connection connection : connections) {
            adjacencyList.computeIfAbsent(connection.getSourceDevice().getId(), k -> new ArrayList<>())
                .add(connection.getTargetDevice().getId());
        }

        // Detect cycles using DFS
        Set<UUID> visited = new HashSet<>();
        Set<UUID> recursionStack = new HashSet<>();

        for (UUID deviceId : adjacencyList.keySet()) {
            if (!visited.contains(deviceId)) {
                if (hasCycle(deviceId, visited, recursionStack, adjacencyList)) {
                    // Found a cycle
                    Device device = deviceRepository.findById(deviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

                    Alert alert = new Alert(
                        "TOPOLOGY_VIOLATION",
                        rule.getSeverity(),
                        "Network loop detected involving device '" + device.getName() + "'",
                        List.of(deviceId),
                        Collections.emptyList()
                    );
                    alerts.add(alert);
                }
            }
        }

        return alerts;
    }

    /**
     * Check if a graph has a cycle using DFS.
     * @param deviceId Current device ID
     * @param visited Set of visited devices
     * @param recursionStack Set of devices in recursion stack
     * @param adjacencyList Adjacency list representation of the graph
     * @return True if a cycle is detected
     */
    private boolean hasCycle(UUID deviceId, Set<UUID> visited, Set<UUID> recursionStack, 
                           Map<UUID, List<UUID>> adjacencyList) {
        if (!visited.contains(deviceId)) {
            // Mark current node as visited and add to recursion stack
            visited.add(deviceId);
            recursionStack.add(deviceId);

            // Recur for all neighbors
            for (UUID neighborId : adjacencyList.getOrDefault(deviceId, Collections.emptyList())) {
                if (!visited.contains(neighborId)) {
                    if (hasCycle(neighborId, visited, recursionStack, adjacencyList)) {
                        return true;
                    }
                } else if (recursionStack.contains(neighborId)) {
                    // If neighbor is in recursion stack, there is a cycle
                    return true;
                }
            }
        }

        // Remove node from recursion stack
        recursionStack.remove(deviceId);
        return false;
    }

    /**
     * Execute a policy compliance validation rule.
     * @param rule The validation rule
     * @return List of alerts generated
     */
    private List<Alert> executePolicyComplianceRule(ValidationRule rule) {
        logger.debug("Executing policy compliance rule: {}", rule.getRuleName());

        List<Alert> alerts = new ArrayList<>();

        // Get all devices
        List<Device> devices = deviceRepository.findAll();

        // Get all connections
        List<Connection> connections = connectionRepository.findAll();

        // Check VLAN/subnet isolation policies
        // This is a simplified check - in a real implementation, we would
        // have more sophisticated policy validation
        Map<String, List<Device>> subnetDevices = new HashMap<>();
        for (Device device : devices) {
            if (device.getIp() != null && !device.getIp().isEmpty()) {
                // Extract subnet from IP (simplified)
                String subnet = device.getIp().substring(0, device.getIp().lastIndexOf('.'));
                subnetDevices.computeIfAbsent(subnet, k -> new ArrayList<>()).add(device);
            }
        }

        // Check for mixed criticality devices in same subnet
        for (Map.Entry<String, List<Device>> entry : subnetDevices.entrySet()) {
            String subnet = entry.getKey();
            List<Device> subnetDeviceList = entry.getValue();

            // Find max and min criticality in subnet
            int maxCriticality = subnetDeviceList.stream()
                .mapToInt(Device::getCriticality)
                .max()
                .orElse(0);

            int minCriticality = subnetDeviceList.stream()
                .mapToInt(Device::getCriticality)
                .min()
                .orElse(0);

            // If subnet contains both high and low criticality devices, flag as potential issue
            if (maxCriticality >= 4 && minCriticality <= 2) {
                for (Device device : subnetDeviceList) {
                    if (device.getCriticality() >= 4) {
                        Alert alert = new Alert(
                            "POLICY_VIOLATION",
                            rule.getSeverity(),
                            "High-criticality device '" + device.getName() + "' is in subnet with low-criticality devices",
                            List.of(device.getId()),
                            Collections.emptyList()
                        );
                        alerts.add(alert);
                    }
                }
            }
        }

        // Check bandwidth/latency/reliability constraints
        for (Connection connection : connections) {
            // Check bandwidth constraints
            if (connection.getBandwidth() != null && connection.getBandwidth() < 100) {
                // Low bandwidth connection
                Alert alert = new Alert(
                    "POLICY_VIOLATION",
                    rule.getSeverity(),
                    "Connection between '" + 
                        connection.getSourceDevice().getName() + "' and '" + 
                        connection.getTargetDevice().getName() + 
                        "' has low bandwidth (" + connection.getBandwidth() + " Mbps)",
                    List.of(connection.getSourceDevice().getId(), connection.getTargetDevice().getId()),
                    List.of(connection.getId())
                );
                alerts.add(alert);
            }

            // Check latency constraints
            if (connection.getLatency() != null && connection.getLatency() > 100) {
                // High latency connection
                Alert alert = new Alert(
                    "POLICY_VIOLATION",
                    rule.getSeverity(),
                    "Connection between '" + 
                        connection.getSourceDevice().getName() + "' and '" + 
                        connection.getTargetDevice().getName() + 
                        "' has high latency (" + connection.getLatency() + " ms)",
                    List.of(connection.getSourceDevice().getId(), connection.getTargetDevice().getId()),
                    List.of(connection.getId())
                );
                alerts.add(alert);
            }

            // Check reliability constraints
            if (connection.getReliability() != null && connection.getReliability().compareTo(new java.math.BigDecimal("0.95")) < 0) {
                // Low reliability connection
                Alert alert = new Alert(
                    "POLICY_VIOLATION",
                    rule.getSeverity(),
                    "Connection between '" + 
                        connection.getSourceDevice().getName() + "' and '" + 
                        connection.getTargetDevice().getName() + 
                        "' has low reliability (" + connection.getReliability() + ")",
                    List.of(connection.getSourceDevice().getId(), connection.getTargetDevice().getId()),
                    List.of(connection.getId())
                );
                alerts.add(alert);
            }
        }

        return alerts;
    }

    /**
     * Execute a security validation rule.
     * @param rule The validation rule
     * @return List of alerts generated
     */
    private List<Alert> executeSecurityRule(ValidationRule rule) {
        logger.debug("Executing security rule: {}", rule.getRuleName());

        List<Alert> alerts = new ArrayList<>();

        // Get all devices
        List<Device> devices = deviceRepository.findAll();

        // Get all connections
        List<Connection> connections = connectionRepository.findAll();

        // Check for unsecured connections
        for (Connection connection : connections) {
            if ("WIFI".equals(connection.getConnectionType())) {
                // Check if WiFi connection has proper security
                // This is a simplified check - in a real implementation, we would
                // have more sophisticated security validation

                // Add metadata if not present
                if (connection.getMetadata() == null) {
                    connection.setMetadata(new HashMap<>());
                }

                // Check for encryption
                boolean hasEncryption = Boolean.TRUE.equals(connection.getMetadata().get("encrypted"));
                if (!hasEncryption) {
                    Alert alert = new Alert(
                        "SECURITY_ISSUE",
                        rule.getSeverity(),
                        "WiFi connection between '" + 
                            connection.getSourceDevice().getName() + "' and '" + 
                            connection.getTargetDevice().getName() + 
                            "' lacks encryption",
                        List.of(connection.getSourceDevice().getId(), connection.getTargetDevice().getId()),
                        List.of(connection.getId())
                    );
                    alerts.add(alert);
                }
            }
        }

        // Check for exposed critical devices
        for (Device device : devices) {
            if (device.getCriticality() >= 4) { // High criticality
                // Check if device is exposed to external network
                // This is a simplified check - in a real implementation, we would
                // have more sophisticated exposure detection

                // Add metadata if not present
                if (device.getMetadata() == null) {
                    device.setMetadata(new HashMap<>());
                }

                boolean isExposed = Boolean.TRUE.equals(device.getMetadata().get("exposed"));
                if (isExposed) {
                    Alert alert = new Alert(
                        "SECURITY_ISSUE",
                        rule.getSeverity(),
                        "High-criticality device '" + device.getName() + "' is exposed to external network",
                        List.of(device.getId()),
                        Collections.emptyList()
                    );
                    alerts.add(alert);
                }
            }
        }

        return alerts;
    }

    /**
     * Execute a custom validation rule.
     * @param rule The validation rule
     * @return List of alerts generated
     */
    private List<Alert> executeCustomRule(ValidationRule rule) {
        logger.debug("Executing custom rule: {}", rule.getRuleName());

        List<Alert> alerts = new ArrayList<>();

        // Get custom validation logic from rule
        Map<String, Object> validationLogic = rule.getValidationLogic();
        if (validationLogic == null || validationLogic.isEmpty()) {
            logger.warn("Custom rule '{}' has no validation logic defined", rule.getRuleName());
            return alerts;
        }

        // Get rule parameters
        Map<String, Object> parameters = rule.getRuleParameters();
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        // Execute custom validation logic
        // This is a simplified implementation - in a real implementation, we would
        // use a rule engine or scripting language to execute custom logic

        // Example: Check for devices with specific metadata
        if (validationLogic.containsKey("checkMetadata")) {
            Map<String, Object> metadataCheck = (Map<String, Object>) validationLogic.get("checkMetadata");
            String metadataKey = (String) metadataCheck.get("key");
            Object metadataValue = metadataCheck.get("value");
            String operator = (String) metadataCheck.getOrDefault("operator", "equals");

            List<Device> devices = deviceRepository.findAll();

            for (Device device : devices) {
                Map<String, Object> deviceMetadata = device.getMetadata();
                if (deviceMetadata != null) {
                    Object deviceValue = deviceMetadata.get(metadataKey);

                    boolean matches = false;
                    switch (operator) {
                        case "equals":
                            matches = Objects.equals(deviceValue, metadataValue);
                            break;
                        case "not_equals":
                            matches = !Objects.equals(deviceValue, metadataValue);
                            break;
                        case "contains":
                            if (deviceValue instanceof String && metadataValue instanceof String) {
                                matches = ((String) deviceValue).contains((String) metadataValue);
                            }
                            break;
                        case "greater_than":
                            if (deviceValue instanceof Number && metadataValue instanceof Number) {
                                matches = ((Number) deviceValue).doubleValue() > ((Number) metadataValue).doubleValue();
                            }
                            break;
                        case "less_than":
                            if (deviceValue instanceof Number && metadataValue instanceof Number) {
                                matches = ((Number) deviceValue).doubleValue() < ((Number) metadataValue).doubleValue();
                            }
                            break;
                    }

                    if (matches) {
                        Alert alert = new Alert(
                            "CUSTOM_VIOLATION",
                            rule.getSeverity(),
                            "Device '" + device.getName() + "' has metadata '" + metadataKey + 
                                "' that matches custom rule condition",
                            List.of(device.getId()),
                            Collections.emptyList()
                        );
                        alerts.add(alert);
                    }
                }
            }
        }

        return alerts;
    }

    /**
     * Get a validation run by ID.
     * @param runId The validation run ID
     * @return The validation run
     */
    public ValidationRun getValidationRun(String runId) {
        return validationRunRepository.findByRunId(runId)
            .orElseThrow(() -> new IllegalArgumentException("Validation run not found: " + runId));
    }

    /**
     * Get validation runs with filtering and pagination.
     * @param status Filter by status (optional)
     * @param triggeredBy Filter by triggered by (optional)
     * @param start Start date (optional)
     * @param end End date (optional)
     * @param pageable Pagination information
     * @return Page of validation runs
     */
    public Page<ValidationRun> getValidationRuns(String status, String triggeredBy, 
                                               Instant start, Instant end, Pageable pageable) {
        if (status != null && triggeredBy != null) {
            return validationRunRepository.findByStatusAndTriggeredBy(status, triggeredBy, pageable);
        } else if (status != null) {
            return validationRunRepository.findByStatus(status, pageable);
        } else if (triggeredBy != null) {
            return validationRunRepository.findByTriggeredBy(triggeredBy, pageable);
        } else if (start != null && end != null) {
            return validationRunRepository.findByTriggeredAtBetween(start, end, pageable);
        } else {
            return validationRunRepository.findAll(pageable);
        }
    }

    /**
     * Get validation runs by user.
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of validation runs
     */
    public Page<ValidationRun> getValidationRunsByUser(UUID userId, Pageable pageable) {
        return validationRunRepository.findByUserId(userId, pageable);
    }

    /**
     * Get recent validation runs (last 24 hours).
     * @param pageable Pagination information
     * @return Page of recent validation runs
     */
    public Page<ValidationRun> getRecentValidationRuns(Pageable pageable) {
        Instant start = Instant.now().minusSeconds(24 * 60 * 60);
        return validationRunRepository.findByTriggeredAtAfter(start, pageable);
    }

    /**
     * Get validation statistics.
     * @return Map of validation statistics
     */
    public Map<String, Object> getValidationStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Total validation runs
        long totalRuns = validationRunRepository.count();
        stats.put("totalRuns", totalRuns);

        // Completed validation runs
        long completedRuns = validationRunRepository.countByStatus("COMPLETED");
        stats.put("completedRuns", completedRuns);

        // Failed validation runs
        long failedRuns = validationRunRepository.countByStatus("FAILED");
        stats.put("failedRuns", failedRuns);

        // Validation runs by triggered by
        Map<String, Long> runsByTriggeredBy = new HashMap<>();
        List<Object[]> results = validationRunRepository.countByTriggeredBy();
        for (Object[] result : results) {
            runsByTriggeredBy.put((String) result[0], (Long) result[1]);
        }
        stats.put("runsByTriggeredBy", runsByTriggeredBy);

        // Average validation duration
        Double avgDuration = validationRunRepository.calculateAverageDuration();
        stats.put("averageDurationMs", avgDuration != null ? avgDuration : 0);

        // Enabled validation rules
        long enabledRules = validationRuleRepository.countByEnabled(true);
        stats.put("enabledRules", enabledRules);

        // Disabled validation rules
        long disabledRules = validationRuleRepository.countByEnabled(false);
        stats.put("disabledRules", disabledRules);

        return stats;
    }

    /**
     * Create a new validation rule.
     * @param ruleName The rule name
     * @param ruleType The rule type
     * @param severity The severity level
     * @param description The rule description
     * @param createdBy The user creating the rule
     * @return The created validation rule
     */
    @Transactional
    public ValidationRule createValidationRule(String ruleName, String ruleType, String severity, 
                                             String description, UUID createdBy) {
        logger.info("Creating validation rule: {}", ruleName);

        // Validate severity
        if (!Arrays.asList("CRITICAL", "HIGH", "MEDIUM", "LOW").contains(severity)) {
            throw new IllegalArgumentException("Invalid severity level: " + severity);
        }

        // Get user
        User user = userRepository.findById(createdBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + createdBy));

        // Create rule
        ValidationRule rule = new ValidationRule(ruleName, ruleType, severity, description);
        rule.setCreatedBy(user);
        rule.setCreatedAt(Instant.now());

        // Save rule
        rule = validationRuleRepository.save(rule);

        logger.info("Created validation rule: {}", rule.getId());
        return rule;
    }

    /**
     * Update a validation rule.
     * @param ruleId The rule ID
     * @param ruleName The rule name
     * @param ruleType The rule type
     * @param severity The severity level
     * @param description The rule description
     * @param enabled Whether the rule is enabled
     * @param ruleParameters The rule parameters
     * @param validationLogic The validation logic
     * @param updatedBy The user updating the rule
     * @return The updated validation rule
     */
    @Transactional
    public ValidationRule updateValidationRule(UUID ruleId, String ruleName, String ruleType, 
                                             String severity, String description, boolean enabled,
                                             Map<String, Object> ruleParameters, 
                                             Map<String, Object> validationLogic, UUID updatedBy) {
        logger.info("Updating validation rule: {}", ruleId);

        // Get rule
        ValidationRule rule = validationRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Validation rule not found: " + ruleId));

        // Get user
        User user = userRepository.findById(updatedBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + updatedBy));

        // Update rule
        rule.setRuleName(ruleName);
        rule.setRuleType(ruleType);
        rule.setSeverity(severity);
        rule.setDescription(description);
        rule.setEnabled(enabled);
        rule.setRuleParameters(ruleParameters);
        rule.setValidationLogic(validationLogic);
        rule.updateTimestamps();

        // Save rule
        rule = validationRuleRepository.save(rule);

        logger.info("Updated validation rule: {}", ruleId);
        return rule;
    }

    /**
     * Delete a validation rule.
     * @param ruleId The rule ID
     */
    @Transactional
    public void deleteValidationRule(UUID ruleId) {
        logger.info("Deleting validation rule: {}", ruleId);

        // Delete rule
        validationRuleRepository.deleteById(ruleId);

        logger.info("Deleted validation rule: {}", ruleId);
    }

    /**
     * Get a validation rule by ID.
     * @param ruleId The rule ID
     * @return The validation rule
     */
    public ValidationRule getValidationRule(UUID ruleId) {
        return validationRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Validation rule not found: " + ruleId));
    }

    /**
     * Get validation rules with filtering and pagination.
     * @param enabled Filter by enabled status (optional)
     * @param ruleType Filter by rule type (optional)
     * @param severity Filter by severity (optional)
     * @param pageable Pagination information
     * @return Page of validation rules
     */
    public Page<ValidationRule> getValidationRules(Boolean enabled, String ruleType, 
                                                 String severity, Pageable pageable) {
        if (enabled != null && ruleType != null && severity != null) {
            return validationRuleRepository.findByEnabledAndRuleTypeAndSeverity(enabled, ruleType, severity, pageable);
        } else if (enabled != null && ruleType != null) {
            return validationRuleRepository.findByEnabledAndRuleType(enabled, ruleType, pageable);
        } else if (enabled != null && severity != null) {
            return validationRuleRepository.findByEnabledAndSeverity(enabled, severity, pageable);
        } else if (ruleType != null && severity != null) {
            return validationRuleRepository.findByRuleTypeAndSeverity(ruleType, severity, pageable);
        } else if (enabled != null) {
            return validationRuleRepository.findByEnabled(enabled, pageable);
        } else if (ruleType != null) {
            return validationRuleRepository.findByRuleType(ruleType, pageable);
        } else if (severity != null) {
            return validationRuleRepository.findBySeverity(severity, pageable);
        } else {
            return validationRuleRepository.findAll(pageable);
        }
    }

    /**
     * Get validation rules by user.
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of validation rules
     */
    public Page<ValidationRule> getValidationRulesByUser(UUID userId, Pageable pageable) {
        return validationRuleRepository.findByCreatedBy_Id(userId, pageable);
    }

    /**
     * Get validation rule statistics.
     * @return Map of validation rule statistics
     */
    public Map<String, Object> getValidationRuleStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Total validation rules
        long totalRules = validationRuleRepository.count();
        stats.put("totalRules", totalRules);

        // Enabled validation rules
        long enabledRules = validationRuleRepository.countByEnabled(true);
        stats.put("enabledRules", enabledRules);

        // Disabled validation rules
        long disabledRules = validationRuleRepository.countByEnabled(false);
        stats.put("disabledRules", disabledRules);

        // Validation rules by type
        Map<String, Long> rulesByType = new HashMap<>();
        List<Object[]> results = validationRuleRepository.countByRuleType();
        for (Object[] result : results) {
            rulesByType.put((String) result[0], (Long) result[1]);
        }
        stats.put("rulesByType", rulesByType);

        // Validation rules by severity
        Map<String, Long> rulesBySeverity = new HashMap<>();
        results = validationRuleRepository.countBySeverity();
        for (Object[] result : results) {
            rulesBySeverity.put((String) result[0], (Long) result[1]);
        }
        stats.put("rulesBySeverity", rulesBySeverity);

        // Total executions
        Long totalExecutions = validationRuleRepository.sumExecutionCount();
        stats.put("totalExecutions", totalExecutions != null ? totalExecutions : 0);

        // Total failures
        Long totalFailures = validationRuleRepository.sumFailureCount();
        stats.put("totalFailures", totalFailures != null ? totalFailures : 0);

        return stats;
    }

    /**
     * Send validation completion notification via Kafka.
     * @param validationRun The validation run
     */
    private void sendValidationCompletionNotification(ValidationRun validationRun) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "VALIDATION_COMPLETED");
            payload.put("runId", validationRun.getRunId());
            payload.put("status", validationRun.getStatus());
            payload.put("triggeredBy", validationRun.getTriggeredBy());
            payload.put("triggeredAt", validationRun.getTriggeredAt());
            payload.put("completedAt", validationRun.getCompletedAt());
            payload.put("durationMs", validationRun.getDurationMs());
            payload.put("alertCount", validationRun.getAlertIds().size());
            payload.put("executedRules", validationRun.getExecutedRules());

            kafkaProducerService.sendMessage("topology-validation", payload);
        } catch (Exception e) {
            logger.error("Error sending validation completion notification", e);
        }
    }

    /**
     * Send validation failure notification via Kafka.
     * @param validationRun The validation run
     */
    private void sendValidationFailureNotification(ValidationRun validationRun) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "VALIDATION_FAILED");
            payload.put("runId", validationRun.getRunId());
            payload.put("status", validationRun.getStatus());
            payload.put("triggeredBy", validationRun.getTriggeredBy());
            payload.put("triggeredAt", validationRun.getTriggeredAt());
            payload.put("completedAt", validationRun.getCompletedAt());
            payload.put("errorMessage", validationRun.getErrorMessage());

            kafkaProducerService.sendMessage("topology-validation", payload);
        } catch (Exception e) {
            logger.error("Error sending validation failure notification", e);
        }
    }

    /**
     * Send alert notification via Kafka.
     * @param alert The alert
     */
    private void sendAlertNotification(Alert alert) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ALERT_CREATED");
            payload.put("alertId", alert.getId().toString());
            payload.put("alertType", alert.getAlertType());
            payload.put("severity", alert.getSeverity());
            payload.put("message", alert.getMessage());
            payload.put("timestamp", alert.getTimestamp());
            payload.put("resolved", alert.isResolved());
            payload.put("affectedDevices", alert.getAffectedDevices());
            payload.put("affectedConnections", alert.getAffectedConnections());

            kafkaProducerService.sendMessage("topology-alerts", payload);
        } catch (Exception e) {
            logger.error("Error sending alert notification", e);
        }
    }

    /**
     * Send alert resolution notification via Kafka.
     * @param alert The alert
     */
    private void sendAlertResolutionNotification(Alert alert) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ALERT_RESOLVED");
            payload.put("alertId", alert.getId().toString());
            payload.put("severity", alert.getSeverity());
            payload.put("message", alert.getMessage());
            payload.put("timestamp", alert.getTimestamp());
            payload.put("resolvedAt", alert.getResolvedAt());
            payload.put("resolvedBy", alert.getResolvedBy());
            payload.put("resolutionNotes", alert.getResolutionNotes());

            kafkaProducerService.sendMessage("topology-alerts", payload);
        } catch (Exception e) {
            logger.error("Error sending alert resolution notification", e);
        }
    }

    /**
     * Send alert unresolve notification via Kafka.
     * @param alert The alert
     */
    private void sendAlertUnresolveNotification(Alert alert) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ALERT_UNRESOLVED");
            payload.put("alertId", alert.getId().toString());
            payload.put("severity", alert.getSeverity());
            payload.put("message", alert.getMessage());
            payload.put("timestamp", alert.getTimestamp());
            payload.put("unresolvedAt", Instant.now());

            kafkaProducerService.sendMessage("topology-alerts", payload);
        } catch (Exception e) {
            logger.error("Error sending alert unresolve notification", e);
        }
    }
}
