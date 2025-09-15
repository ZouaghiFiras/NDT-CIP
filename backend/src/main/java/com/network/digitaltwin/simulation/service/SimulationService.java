package com.network.digitaltwin.simulation.service;

import com.network.digitaltwin.model.auth.User;
import com.network.digitaltwin.simulation.model.SimulationScenario;
import com.network.digitaltwin.simulation.repository.SimulationScenarioRepository;
import com.network.digitaltwin.repository.auth.UserRepository;
import com.network.digitaltwin.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing simulation scenarios.
 */
@Service
public class SimulationService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);

    @Autowired
    private SimulationScenarioRepository simulationScenarioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * Create a new simulation scenario.
     * @param scenario The scenario data
     * @param createdBy The user creating the scenario
     * @return The created scenario
     */
    @Transactional
    public SimulationScenario createSimulationScenario(SimulationScenario scenario, UUID createdBy) {
        logger.info("Creating simulation scenario: {}", scenario.getName());

        // Validate scenario
        validateScenario(scenario, createdBy);

        // Set created by
        User user = userRepository.findById(createdBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + createdBy));
        scenario.setCreatedBy(user);

        // Set timestamps
        scenario.setCreatedAt(Instant.now());

        // Save scenario
        scenario = simulationScenarioRepository.save(scenario);

        // Send notification via Kafka
        sendScenarioCreationNotification(scenario);

        logger.info("Created simulation scenario: {}", scenario.getId());
        return scenario;
    }

    /**
     * Update a simulation scenario.
     * @param id The ID of the scenario to update
     * @param scenarioData The updated scenario data
     * @param updatedBy The user updating the scenario
     * @return The updated scenario
     */
    @Transactional
    public SimulationScenario updateSimulationScenario(UUID id, SimulationScenario scenarioData, UUID updatedBy) {
        logger.info("Updating simulation scenario: {}", id);

        // Get existing scenario
        SimulationScenario existingScenario = simulationScenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Simulation scenario not found: " + id));

        // Check if scenario can be updated (only PENDING scenarios can be updated)
        if (!existingScenario.isPending()) {
            throw new IllegalStateException("Only PENDING scenarios can be updated");
        }

        // Validate scenario
        validateScenario(scenarioData, updatedBy);

        // Update scenario properties
        existingScenario.setName(scenarioData.getName());
        existingScenario.setType(scenarioData.getType());
        existingScenario.setDescription(scenarioData.getDescription());
        existingScenario.setTargetDevices(scenarioData.getTargetDevices());
        existingScenario.setAttackVector(scenarioData.getAttackVector());
        existingScenario.setDuration(scenarioData.getDuration());

        // Update metadata if provided
        if (scenarioData.getMetadata() != null) {
            existingScenario.setMetadata(scenarioData.getMetadata());
        }

        // Set updated by
        User user = userRepository.findById(updatedBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + updatedBy));
        existingScenario.setUpdatedBy(user);

        // Update timestamps
        existingScenario.setUpdatedAt(Instant.now());

        // Add execution history
        existingScenario.addExecutionHistoryEntry("UPDATE", "Scenario updated by " + user.getUsername(), Instant.now());

        // Save scenario
        existingScenario = simulationScenarioRepository.save(existingScenario);

        // Send notification via Kafka
        sendScenarioUpdateNotification(existingScenario);

        logger.info("Updated simulation scenario: {}", id);
        return existingScenario;
    }

    /**
     * Delete a simulation scenario.
     * @param id The ID of the scenario to delete
     * @param deletedBy The user deleting the scenario
     */
    @Transactional
    public void deleteSimulationScenario(UUID id, UUID deletedBy) {
        logger.info("Deleting simulation scenario: {}", id);

        // Get scenario
        SimulationScenario scenario = simulationScenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Simulation scenario not found: " + id));

        // Check if scenario can be deleted (only PENDING scenarios can be deleted)
        if (!scenario.isPending()) {
            throw new IllegalStateException("Only PENDING scenarios can be deleted");
        }

        // Get user
        User user = userRepository.findById(deletedBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + deletedBy));

        // Add execution history
        scenario.addExecutionHistoryEntry("DELETE", "Scenario deleted by " + user.getUsername(), Instant.now());

        // Delete scenario
        simulationScenarioRepository.delete(scenario);

        // Send notification via Kafka
        sendScenarioDeletionNotification(scenario);

        logger.info("Deleted simulation scenario: {}", id);
    }

    /**
     * Get a simulation scenario by ID.
     * @param id The ID of the scenario
     * @return The scenario
     */
    public SimulationScenario getSimulationScenario(UUID id) {
        return simulationScenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Simulation scenario not found: " + id));
    }

    /**
     * Get all simulation scenarios with pagination and filtering.
     * @param type Filter by type (optional)
     * @param status Filter by status (optional)
     * @param createdBy Filter by creator (optional)
     * @param start Start date (optional)
     * @param end End date (optional)
     * @param pageable Pagination information
     * @return Page of simulation scenarios
     */
    public Page<SimulationScenario> getSimulationScenarios(
            SimulationType type, SimulationStatus status, UUID createdBy,
            Instant start, Instant end, Pageable pageable) {

        if (type != null && status != null) {
            return simulationScenarioRepository.findByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            return simulationScenarioRepository.findByType(type, pageable);
        } else if (status != null) {
            return simulationScenarioRepository.findByStatus(status, pageable);
        } else if (createdBy != null) {
            return simulationScenarioRepository.findByCreatedBy_Id(createdBy, pageable);
        } else if (start != null && end != null) {
            return simulationScenarioRepository.findByCreatedAtBetween(start, end, pageable);
        } else {
            return simulationScenarioRepository.findAll(pageable);
        }
    }

    /**
     * Execute a simulation scenario asynchronously.
     * @param id The ID of the scenario to execute
     * @param executedBy The user executing the scenario
     * @return The execution ID
     */
    @Async
    @Transactional
    public CompletableFuture<String> executeSimulationScenario(UUID id, UUID executedBy) {
        logger.info("Executing simulation scenario: {}", id);

        // Get scenario
        SimulationScenario scenario = simulationScenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Simulation scenario not found: " + id));

        // Check if scenario can be executed (only PENDING scenarios can be executed)
        if (!scenario.isPending()) {
            throw new IllegalStateException("Only PENDING scenarios can be executed");
        }

        // Get user
        User user = userRepository.findById(executedBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + executedBy));

        // Update scenario status
        scenario.setStatus(SimulationStatus.RUNNING);
        scenario.setUpdatedBy(user);
        scenario.setUpdatedAt(Instant.now());

        // Add execution history
        scenario.addExecutionHistoryEntry("EXECUTE", "Scenario executed by " + user.getUsername(), Instant.now());

        // Save scenario
        scenario = simulationScenarioRepository.save(scenario);

        // Send execution notification via Kafka
        sendScenarioExecutionNotification(scenario);

        // Generate execution ID
        String executionId = UUID.randomUUID().toString();

        try {
            // Simulate execution (in a real implementation, this would delegate to a simulation engine)
            Thread.sleep(scenario.getDuration().toMillis());

            // Update scenario status to completed
            scenario.setStatus(SimulationStatus.COMPLETED);
            scenario.addExecutionHistoryEntry("COMPLETE", "Scenario completed successfully", Instant.now());

            // Save scenario
            scenario = simulationScenarioRepository.save(scenario);

            // Send completion notification via Kafka
            sendScenarioCompletionNotification(scenario, executionId);

            logger.info("Completed simulation scenario: {}", id);

            return CompletableFuture.completedFuture(executionId);

        } catch (InterruptedException e) {
            // Handle interruption
            Thread.currentThread().interrupt();

            // Update scenario status to failed
            scenario.setStatus(SimulationStatus.FAILED);
            scenario.addExecutionHistoryEntry("FAIL", "Scenario execution failed: " + e.getMessage(), Instant.now());

            // Save scenario
            scenario = simulationScenarioRepository.save(scenario);

            // Send failure notification via Kafka
            sendScenarioFailureNotification(scenario, executionId);

            logger.error("Simulation scenario execution failed: {}", id, e);

            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Cancel a running simulation scenario.
     * @param id The ID of the scenario to cancel
     * @param cancelledBy The user cancelling the scenario
     */
    @Transactional
    public void cancelSimulationScenario(UUID id, UUID cancelledBy) {
        logger.info("Cancelling simulation scenario: {}", id);

        // Get scenario
        SimulationScenario scenario = simulationScenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Simulation scenario not found: " + id));

        // Check if scenario can be cancelled (only RUNNING scenarios can be cancelled)
        if (!scenario.isRunning()) {
            throw new IllegalStateException("Only RUNNING scenarios can be cancelled");
        }

        // Get user
        User user = userRepository.findById(cancelledBy)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + cancelledBy));

        // Update scenario status
        scenario.setStatus(SimulationStatus.CANCELLED);
        scenario.setUpdatedBy(user);
        scenario.setUpdatedAt(Instant.now());

        // Add execution history
        scenario.addExecutionHistoryEntry("CANCEL", "Scenario cancelled by " + user.getUsername(), Instant.now());

        // Save scenario
        scenario = simulationScenarioRepository.save(scenario);

        // Send cancellation notification via Kafka
        sendScenarioCancellationNotification(scenario);

        logger.info("Cancelled simulation scenario: {}", id);
    }

    /**
     * Validate a simulation scenario.
     * @param scenario The scenario to validate
     * @param userId The user ID for validation
     */
    private void validateScenario(SimulationScenario scenario, UUID userId) {
        // Validate name
        if (scenario.getName() == null || scenario.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Scenario name is required");
        }

        // Check for duplicate name (excluding the current scenario if updating)
        SimulationScenario existingScenario = simulationScenarioRepository.findByName(scenario.getName());
        if (existingScenario != null && 
            (scenario.getId() == null || !existingScenario.getId().equals(scenario.getId()))) {
            throw new IllegalStateException("Simulation scenario with name '" + scenario.getName() + "' already exists");
        }

        // Validate type
        if (scenario.getType() == null) {
            throw new IllegalArgumentException("Scenario type is required");
        }

        // Validate attack vector
        if (scenario.getAttackVector() == null || scenario.getAttackVector().trim().isEmpty()) {
            throw new IllegalArgumentException("Attack vector is required");
        }

        // Validate duration
        if (scenario.getDuration() == null || scenario.getDuration().isNegative() || scenario.getDuration().isZero()) {
            throw new IllegalArgumentException("Duration must be a positive value");
        }

        // Validate target devices
        if (scenario.getTargetDevices() == null || scenario.getTargetDevices().isEmpty()) {
            throw new IllegalArgumentException("At least one target device is required");
        }

        // Check if target devices exist
        List<UUID> invalidDevices = scenario.getTargetDevices().stream()
            .filter(deviceId -> !deviceRepository.existsById(deviceId))
            .collect(Collectors.toList());

        if (!invalidDevices.isEmpty()) {
            throw new IllegalArgumentException("Invalid target device IDs: " + invalidDevices);
        }

        // Check if target devices are active
        List<UUID> inactiveDevices = deviceRepository.findInactiveDeviceIdsByIds(scenario.getTargetDevices());
        if (!inactiveDevices.isEmpty()) {
            throw new IllegalArgumentException("Some target devices are inactive: " + inactiveDevices);
        }

        // Validate metadata if provided
        if (scenario.getMetadata() != null) {
            // Add type-specific validation here
            switch (scenario.getType()) {
                case RANSOMWARE:
                    validateRansomwareMetadata(scenario.getMetadata());
                    break;
                case DDOS:
                    validateDdosMetadata(scenario.getMetadata());
                    break;
                case INSIDER_THREAT:
                    validateInsiderThreatMetadata(scenario.getMetadata());
                    break;
                case PHISHING:
                    validatePhishingMetadata(scenario.getMetadata());
                    break;
                case CUSTOM:
                    // No specific validation for custom scenarios
                    break;
            }
        }
    }

    /**
     * Validate ransomware scenario metadata.
     * @param metadata The metadata to validate
     */
    private void validateRansomwareMetadata(Map<String, Object> metadata) {
        // Validate encryption method
        if (metadata.containsKey("encryptionMethod")) {
            String encryptionMethod = (String) metadata.get("encryptionMethod");
            if (encryptionMethod == null || encryptionMethod.trim().isEmpty()) {
                throw new IllegalArgumentException("Encryption method is required for ransomware scenarios");
            }
        }

        // Validate ransom note
        if (metadata.containsKey("ransomNote")) {
            String ransomNote = (String) metadata.get("ransomNote");
            if (ransomNote == null || ransomNote.trim().isEmpty()) {
                throw new IllegalArgumentException("Ransom note is required for ransomware scenarios");
            }
        }

        // Validate ransom amount
        if (metadata.containsKey("ransomAmount")) {
            Object ransomAmount = metadata.get("ransomAmount");
            if (ransomAmount instanceof Number) {
                double amount = ((Number) ransomAmount).doubleValue();
                if (amount <= 0) {
                    throw new IllegalArgumentException("Ransom amount must be positive");
                }
            } else {
                throw new IllegalArgumentException("Ransom amount must be a number");
            }
        }
    }

    /**
     * Validate DDoS scenario metadata.
     * @param metadata The metadata to validate
     */
    private void validateDdosMetadata(Map<String, Object> metadata) {
        // Validate attack vector
        if (metadata.containsKey("attackVector")) {
            String attackVector = (String) metadata.get("attackVector");
            if (attackVector == null || attackVector.trim().isEmpty()) {
                throw new IllegalArgumentException("Attack vector is required for DDoS scenarios");
            }
        }

        // Validate traffic volume
        if (metadata.containsKey("trafficVolume")) {
            Object trafficVolume = metadata.get("trafficVolume");
            if (trafficVolume instanceof Number) {
                double volume = ((Number) trafficVolume).doubleValue();
                if (volume <= 0) {
                    throw new IllegalArgumentException("Traffic volume must be positive");
                }
            } else {
                throw new IllegalArgumentException("Traffic volume must be a number");
            }
        }

        // Validate duration
        if (metadata.containsKey("attackDuration")) {
            Object attackDuration = metadata.get("attackDuration");
            if (attackDuration instanceof Number) {
                double duration = ((Number) attackDuration).doubleValue();
                if (duration <= 0) {
                    throw new IllegalArgumentException("Attack duration must be positive");
                }
            } else {
                throw new IllegalArgumentException("Attack duration must be a number");
            }
        }
    }

    /**
     * Validate insider threat scenario metadata.
     * @param metadata The metadata to validate
     */
    private void validateInsiderThreatMetadata(Map<String, Object> metadata) {
        // Validate threat actor
        if (metadata.containsKey("threatActor")) {
            String threatActor = (String) metadata.get("threatActor");
            if (threatActor == null || threatActor.trim().isEmpty()) {
                throw new IllegalArgumentException("Threat actor is required for insider threat scenarios");
            }
        }

        // Validate motivation
        if (metadata.containsKey("motivation")) {
            String motivation = (String) metadata.get("motivation");
            if (motivation == null || motivation.trim().isEmpty()) {
                throw new IllegalArgumentException("Motivation is required for insider threat scenarios");
            }
        }

        // Validate data access level
        if (metadata.containsKey("dataAccessLevel")) {
            String accessLevel = (String) metadata.get("dataAccessLevel");
            if (accessLevel == null || accessLevel.trim().isEmpty()) {
                throw new IllegalArgumentException("Data access level is required for insider threat scenarios");
            }
        }
    }

    /**
     * Validate phishing scenario metadata.
     * @param metadata The metadata to validate
     */
    private void validatePhishingMetadata(Map<String, Object> metadata) {
        // Validate phishing template
        if (metadata.containsKey("phishingTemplate")) {
            String template = (String) metadata.get("phishingTemplate");
            if (template == null || template.trim().isEmpty()) {
                throw new IllegalArgumentException("Phishing template is required for phishing scenarios");
            }
        }

        // Validate target users
        if (metadata.containsKey("targetUsers")) {
            Object targetUsers = metadata.get("targetUsers");
            if (targetUsers instanceof List) {
                List<?> users = (List<?>) targetUsers;
                if (users.isEmpty()) {
                    throw new IllegalArgumentException("At least one target user is required for phishing scenarios");
                }
            } else {
                throw new IllegalArgumentException("Target users must be a list");
            }
        }

        // Validate email subject
        if (metadata.containsKey("emailSubject")) {
            String subject = (String) metadata.get("emailSubject");
            if (subject == null || subject.trim().isEmpty()) {
                throw new IllegalArgumentException("Email subject is required for phishing scenarios");
            }
        }
    }

    /**
     * Send scenario creation notification via Kafka.
     * @param scenario The scenario
     */
    private void sendScenarioCreationNotification(SimulationScenario scenario) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scenarioId", scenario.getId());
        payload.put("scenarioName", scenario.getName());
        payload.put("scenarioType", scenario.getType().name());
        payload.put("createdBy", scenario.getCreatedBy().getUsername());
        payload.put("createdAt", scenario.getCreatedAt().toString());

        kafkaProducerService.sendMessage("simulation.scenario.created", payload);
    }

    /**
     * Send scenario update notification via Kafka.
     * @param scenario The scenario
     */
    private void sendScenarioUpdateNotification(SimulationScenario scenario) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scenarioId", scenario.getId());
        payload.put("scenarioName", scenario.getName());
        payload.put("scenarioType", scenario.getType().name());
        payload.put("updatedBy", scenario.getUpdatedBy().getUsername());
        payload.put("updatedAt", scenario.getUpdatedAt().toString());

        kafkaProducerService.sendMessage("simulation.scenario.updated", payload);
    }

    /**
     * Send scenario deletion notification via Kafka.
     * @param scenario The scenario
     */
    private void sendScenarioDeletionNotification(SimulationScenario scenario) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scenarioId", scenario.getId());
        payload.put("scenarioName", scenario.getName());
        payload.put("deletedAt", Instant.now().toString());

        kafkaProducerService.sendMessage("simulation.scenario.deleted", payload);
    }

    /**
     * Send scenario execution notification via Kafka.
     * @param scenario The scenario
     */
    private void sendScenarioExecutionNotification(SimulationScenario scenario) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scenarioId", scenario.getId());
        payload.put("scenarioName", scenario.getName());
        payload.put("scenarioType", scenario.getType().name());
        payload.put("duration", scenario.getDuration().toString());
        payload.put("targetDevices", scenario.getTargetDevices());
        payload.put("executedAt", Instant.now().toString());

        kafkaProducerService.sendMessage("simulation.scenario.executed", payload);
    }

    /**
     * Send scenario completion notification via Kafka.
     * @param scenario The scenario
     * @param executionId The execution ID
     */
    private void sendScenarioCompletionNotification(SimulationScenario scenario, String executionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scenarioId", scenario.getId());
        payload.put("scenarioName", scenario.getName());
        payload.put("executionId", executionId);
        payload.put("completedAt", Instant.now().toString());

        kafkaProducerService.sendMessage("simulation.scenario.completed", payload);
    }

    /**
     * Send scenario failure notification via Kafka.
     * @param scenario The scenario
     * @param executionId The execution ID
     */
    private void sendScenarioFailureNotification(SimulationScenario scenario, String executionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scenarioId", scenario.getId());
        payload.put("scenarioName", scenario.getName());
        payload.put("executionId", executionId);
        payload.put("failedAt", Instant.now().toString());

        kafkaProducerService.sendMessage("simulation.scenario.failed", payload);
    }

    /**
     * Send scenario cancellation notification via Kafka.
     * @param scenario The scenario
     */
    private void sendScenarioCancellationNotification(SimulationScenario scenario) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scenarioId", scenario.getId());
        payload.put("scenarioName", scenario.getName());
        payload.put("cancelledAt", Instant.now().toString());

        kafkaProducerService.sendMessage("simulation.scenario.cancelled", payload);
    }
}
