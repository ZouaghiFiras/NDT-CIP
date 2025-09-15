package com.network.digitaltwin.simulation.engine;

import com.network.digitaltwin.model.Device;
import com.network.digitaltwin.model.Connection;
import com.network.digitaltwin.model.Simulation;
import com.network.digitaltwin.model.SimulationEvent;
import com.network.digitaltwin.model.Threat;
import com.network.digitaltwin.model.DeviceThreat;
import com.network.digitaltwin.model.RiskScore;
import com.network.digitaltwin.model.User;
import com.network.digitaltwin.simulation.model.SimulationContext;
import com.network.digitaltwin.simulation.model.SimulationResult;
import com.network.digitaltwin.simulation.model.AttackScenario;
import com.network.digitaltwin.simulation.model.FailureScenario;
import com.network.digitaltwin.simulation.model.MonteCarloResult;
import com.network.digitaltwin.topology.model.NetworkTopology;
import com.network.digitaltwin.repository.SimulationRepository;
import com.network.digitaltwin.repository.SimulationEventRepository;
import com.network.digitaltwin.repository.DeviceThreatRepository;
import com.network.digitaltwin.repository.RiskScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Core simulation engine for running attack and failure simulations on the network topology.
 */
@Component
public class SimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(SimulationEngine.class);

    @Autowired
    private SimulationRepository simulationRepository;

    @Autowired
    private SimulationEventRepository simulationEventRepository;

    @Autowired
    private DeviceThreatRepository deviceThreatRepository;

    @Autowired
    private RiskScoreRepository riskScoreRepository;

    @Autowired
    private NetworkTopology topology;

    // Thread pool for running simulations asynchronously
    private final ExecutorService simulationExecutor;

    // Queue for managing simulation requests
    private final BlockingQueue<SimulationTask> simulationQueue;

    // Active simulations tracking
    private final Map<UUID, SimulationContext> activeSimulations;

    public SimulationEngine() {
        // Configure thread pool based on available processors
        int cores = Runtime.getRuntime().availableProcessors();
        this.simulationExecutor = Executors.newFixedThreadPool(cores * 2);

        // Create a bounded queue for simulation tasks
        this.simulationQueue = new ArrayBlockingQueue<>(100);

        // Track active simulations
        this.activeSimulations = new ConcurrentHashMap<>();

        // Start the simulation processor
        startSimulationProcessor();
    }

    /**
     * Starts the simulation processor that runs simulations from the queue.
     */
    private void startSimulationProcessor() {
        Thread processorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SimulationTask task = simulationQueue.take();
                    processSimulationTask(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Simulation processor thread interrupted");
                } catch (Exception e) {
                    logger.error("Error processing simulation task", e);
                }
            }
        });

        processorThread.setDaemon(true);
        processorThread.start();
        logger.info("Simulation processor started");
    }

    /**
     * Processes a simulation task from the queue.
     * @param task The simulation task to process
     */
    private void processSimulationTask(SimulationTask task) {
        try {
            logger.info("Processing simulation task: {}", task.getSimulation().getId());

            // Update simulation status to running
            task.getSimulation().start();
            simulationRepository.save(task.getSimulation());

            // Create simulation context
            SimulationContext context = new SimulationContext(
                task.getSimulation(),
                topology,
                task.getUser()
            );

            // Track active simulation
            activeSimulations.put(task.getSimulation().getId(), context);

            // Run the simulation
            SimulationResult result;
            if (task.getAttackScenario() != null) {
                result = runAttackSimulation(context, task.getAttackScenario());
            } else if (task.getFailureScenario() != null) {
                result = runFailureSimulation(context, task.getFailureScenario());
            } else if (task.isMonteCarlo()) {
                result = runMonteCarloSimulation(context, task.getIterations());
            } else {
                throw new IllegalArgumentException("No simulation scenario provided");
            }

            // Update simulation status to completed
            task.getSimulation().complete();
            simulationRepository.save(task.getSimulation());

            // Store result in simulation context
            context.setResult(result);

            logger.info("Simulation completed: {}", task.getSimulation().getId());

        } catch (Exception e) {
            logger.error("Simulation failed: {}", task.getSimulation().getId(), e);

            // Update simulation status to failed
            task.getSimulation().fail(e.getMessage());
            simulationRepository.save(task.getSimulation());
        } finally {
            // Remove from active simulations
            activeSimulations.remove(task.getSimulation().getId());
        }
    }

    /**
     * Runs an attack simulation.
     * @param context The simulation context
     * @param scenario The attack scenario to simulate
     * @return The simulation result
     */
    @Transactional
    public SimulationResult runAttackSimulation(SimulationContext context, AttackScenario scenario) {
        logger.info("Running attack simulation: {}", scenario.getName());

        Simulation simulation = context.getSimulation();
        SimulationResult result = new SimulationResult(simulation);

        try {
            // Create simulation event for attack start
            SimulationEvent attackStartEvent = new SimulationEvent(
                simulation,
                "ATTACK_START",
                Instant.now(),
                "INFO",
                "Attack simulation started: " + scenario.getName(),
                "Starting attack simulation: " + scenario.getName()
            );
            simulationEventRepository.save(attackStartEvent);
            simulation.addEvent(attackStartEvent);

            // Process attack steps
            for (AttackStep step : scenario.getSteps()) {
                // Check if simulation was cancelled
                if (simulation.isCancelled()) {
                    throw new RuntimeException("Simulation was cancelled");
                }

                // Process the attack step
                processAttackStep(context, step, result);

                // Update simulation progress
                int progress = (int) ((double) result.getProcessedSteps() / scenario.getSteps().size() * 100);
                simulation.updateProgress(BigDecimal.valueOf(progress));
                simulationRepository.save(simulation);
            }

            // Calculate final impact
            calculateAttackImpact(result);

            // Create simulation event for attack completion
            SimulationEvent attackCompleteEvent = new SimulationEvent(
                simulation,
                "ATTACK_COMPLETE",
                Instant.now(),
                "INFO",
                "Attack simulation completed: " + scenario.getName(),
                "Attack simulation completed. Impact: " + result.getImpactScore()
            );
            simulationEventRepository.save(attackCompleteEvent);
            simulation.addEvent(attackCompleteEvent);

        } catch (Exception e) {
            logger.error("Attack simulation failed", e);

            // Create simulation event for attack failure
            SimulationEvent attackFailureEvent = new SimulationEvent(
                simulation,
                "ATTACK_FAILURE",
                Instant.now(),
                "ERROR",
                "Attack simulation failed: " + e.getMessage(),
                "Attack simulation failed: " + e.getMessage()
            );
            simulationEventRepository.save(attackFailureEvent);
            simulation.addEvent(attackFailureEvent);

            throw e;
        }

        return result;
    }

    /**
     * Processes a single attack step.
     * @param context The simulation context
     * @param step The attack step to process
     * @param result The simulation result to update
     */
    private void processAttackStep(SimulationContext context, AttackStep step, SimulationResult result) {
        logger.debug("Processing attack step: {}", step.getName());

        Simulation simulation = context.getSimulation();

        // Create simulation event for step start
        SimulationEvent stepStartEvent = new SimulationEvent(
            simulation,
            "STEP_START",
            Instant.now(),
            "INFO",
            "Attack step started: " + step.getName(),
            "Starting attack step: " + step.getName()
        );
        simulationEventRepository.save(stepStartEvent);
        simulation.addEvent(stepStartEvent);

        try {
            // Get target devices based on step criteria
            List<Device> targetDevices = findTargetDevices(context, step.getTargetCriteria());

            if (targetDevices.isEmpty()) {
                logger.warn("No target devices found for step: {}", step.getName());
                return;
            }

            // Process each target device
            for (Device device : targetDevices) {
                // Check if simulation was cancelled
                if (simulation.isCancelled()) {
                    throw new RuntimeException("Simulation was cancelled");
                }

                // Check if device is already compromised
                if (result.isDeviceCompromised(device.getId())) {
                    continue;
                }

                // Calculate success probability based on device security posture and step difficulty
                double successProbability = calculateAttackSuccessProbability(device, step);

                // Determine if attack succeeds
                boolean attackSucceeded = Math.random() < successProbability;

                // Create simulation event for device attack
                SimulationEvent deviceAttackEvent = new SimulationEvent(
                    simulation,
                    attackSucceeded ? "DEVICE_COMPROMISED" : "DEVICE_ATTACK_FAILED",
                    Instant.now(),
                    attackSucceeded ? "CRITICAL" : "WARNING",
                    "Attack " + (attackSucceeded ? "succeeded" : "failed") + " on device: " + device.getName(),
                    String.format("Attack '%s' %s on device '%s' (IP: %s)",
                        step.getName(),
                        attackSucceeded ? "succeeded" : "failed",
                        device.getName(),
                        device.getIpAddress())
                );

                // Add device context to the event
                deviceAttackEvent.setDevice(device);
                simulationEventRepository.save(deviceAttackEvent);
                simulation.addEvent(deviceAttackEvent);

                if (attackSucceeded) {
                    // Mark device as compromised
                    result.addCompromisedDevice(device);

                    // Create device threat record
                    DeviceThreat deviceThreat = new DeviceThreat(
                        device,
                        context.getThreat(step.getThreatId()),
                        Instant.now()
                    );
                    deviceThreatRepository.save(deviceThreat);

                    // Update device status
                    device.setStatus("COMPROMISED");
                    // In a real implementation, we would save the device here

                    // Add impact to result
                    result.addImpact(step.getImpactScore());

                    // Propagate to connected devices if step specifies lateral movement
                    if (step.isLateralMovement()) {
                        propagateAttack(context, device, step, result);
                    }
                }

                // Update result statistics
                result.incrementProcessedSteps();
                if (attackSucceeded) {
                    result.incrementSuccessfulAttacks();
                } else {
                    result.incrementFailedAttacks();
                }
            }

        } catch (Exception e) {
            logger.error("Attack step failed: {}", step.getName(), e);

            // Create simulation event for step failure
            SimulationEvent stepFailureEvent = new SimulationEvent(
                simulation,
                "STEP_FAILURE",
                Instant.now(),
                "ERROR",
                "Attack step failed: " + step.getName(),
                "Attack step failed: " + step.getName() + " - " + e.getMessage()
            );
            simulationEventRepository.save(stepFailureEvent);
            simulation.addEvent(stepFailureEvent);

            throw e;
        }
    }

    /**
     * Propagates an attack to connected devices.
     * @param context The simulation context
     * @param sourceDevice The source device that was compromised
     * @param step The attack step causing the propagation
     * @param result The simulation result to update
     */
    private void propagateAttack(SimulationContext context, Device sourceDevice, AttackStep step, SimulationResult result) {
        logger.debug("Propagating attack from device: {}", sourceDevice.getName());

        // Get connected devices
        Set<Device> connectedDevices = topology.getConnectedDevices(sourceDevice.getId());

        for (Device targetDevice : connectedDevices) {
            // Skip if already compromised
            if (result.isDeviceCompromised(targetDevice.getId())) {
                continue;
            }

            // Calculate propagation success probability
            double propagationProbability = calculatePropagationProbability(sourceDevice, targetDevice, step);

            // Determine if propagation succeeds
            boolean propagationSucceeded = Math.random() < propagationProbability;

            if (propagationSucceeded) {
                // Create simulation event for device compromise
                SimulationEvent propagationEvent = new SimulationEvent(
                    context.getSimulation(),
                    "DEVICE_COMPROMISED",
                    Instant.now(),
                    "CRITICAL",
                    "Device compromised through lateral movement: " + targetDevice.getName(),
                    String.format("Device '%s' (IP: %s) compromised through lateral movement from '%s'",
                        targetDevice.getName(),
                        targetDevice.getIpAddress(),
                        sourceDevice.getName())
                );

                // Add device context to the event
                propagationEvent.setDevice(targetDevice);
                simulationEventRepository.save(propagationEvent);
                context.getSimulation().addEvent(propagationEvent);

                // Mark device as compromised
                result.addCompromisedDevice(targetDevice);

                // Create device threat record
                DeviceThreat deviceThreat = new DeviceThreat(
                    targetDevice,
                    context.getThreat(step.getThreatId()),
                    Instant.now()
                );
                deviceThreatRepository.save(deviceThreat);

                // Update device status
                targetDevice.setStatus("COMPROMISED");
                // In a real implementation, we would save the device here

                // Add impact to result
                result.addImpact(step.getImpactScore());

                // Recursively propagate if step specifies multi-hop lateral movement
                if (step.isMultiHopLateralMovement()) {
                    propagateAttack(context, targetDevice, step, result);
                }
            }
        }
    }

    /**
     * Runs a failure simulation.
     * @param context The simulation context
     * @param scenario The failure scenario to simulate
     * @return The simulation result
     */
    @Transactional
    public SimulationResult runFailureSimulation(SimulationContext context, FailureScenario scenario) {
        logger.info("Running failure simulation: {}", scenario.getName());

        Simulation simulation = context.getSimulation();
        SimulationResult result = new SimulationResult(simulation);

        try {
            // Create simulation event for failure start
            SimulationEvent failureStartEvent = new SimulationEvent(
                simulation,
                "FAILURE_START",
                Instant.now(),
                "INFO",
                "Failure simulation started: " + scenario.getName(),
                "Starting failure simulation: " + scenario.getName()
            );
            simulationEventRepository.save(failureStartEvent);
            simulation.addEvent(failureStartEvent);

            // Process failure events
            for (FailureEvent failureEvent : scenario.getEvents()) {
                // Check if simulation was cancelled
                if (simulation.isCancelled()) {
                    throw new RuntimeException("Simulation was cancelled");
                }

                // Process the failure event
                processFailureEvent(context, failureEvent, result);

                // Update simulation progress
                int progress = (int) ((double) result.getProcessedEvents() / scenario.getEvents().size() * 100);
                simulation.updateProgress(BigDecimal.valueOf(progress));
                simulationRepository.save(simulation);
            }

            // Calculate final impact
            calculateFailureImpact(result);

            // Create simulation event for failure completion
            SimulationEvent failureCompleteEvent = new SimulationEvent(
                simulation,
                "FAILURE_COMPLETE",
                Instant.now(),
                "INFO",
                "Failure simulation completed: " + scenario.getName(),
                "Failure simulation completed. Impact: " + result.getImpactScore()
            );
            simulationEventRepository.save(failureCompleteEvent);
            simulation.addEvent(failureCompleteEvent);

        } catch (Exception e) {
            logger.error("Failure simulation failed", e);

            // Create simulation event for failure
            SimulationEvent failureFailureEvent = new SimulationEvent(
                simulation,
                "FAILURE_FAILURE",
                Instant.now(),
                "ERROR",
                "Failure simulation failed: " + e.getMessage(),
                "Failure simulation failed: " + e.getMessage()
            );
            simulationEventRepository.save(failureFailureEvent);
            simulation.addEvent(failureFailureEvent);

            throw e;
        }

        return result;
    }

    /**
     * Processes a single failure event.
     * @param context The simulation context
     * @param failureEvent The failure event to process
     * @param result The simulation result to update
     */
    private void processFailureEvent(SimulationContext context, FailureEvent failureEvent, SimulationResult result) {
        logger.debug("Processing failure event: {}", failureEvent.getName());

        Simulation simulation = context.getSimulation();

        // Create simulation event for event start
        SimulationEvent eventStartEvent = new SimulationEvent(
            simulation,
            "EVENT_START",
            Instant.now(),
            "INFO",
            "Failure event started: " + failureEvent.getName(),
            "Starting failure event: " + failureEvent.getName()
        );
        simulationEventRepository.save(eventStartEvent);
        simulation.addEvent(eventStartEvent);

        try {
            // Get target devices based on event criteria
            List<Device> targetDevices = findTargetDevices(context, failureEvent.getTargetCriteria());

            if (targetDevices.isEmpty()) {
                logger.warn("No target devices found for event: {}", failureEvent.getName());
                return;
            }

            // Process each target device
            for (Device device : targetDevices) {
                // Check if simulation was cancelled
                if (simulation.isCancelled()) {
                    throw new RuntimeException("Simulation was cancelled");
                }

                // Check if device is already affected
                if (result.isDeviceAffected(device.getId())) {
                    continue;
                }

                // Create simulation event for device failure
                SimulationEvent deviceFailureEvent = new SimulationEvent(
                    simulation,
                    "DEVICE_FAILURE",
                    Instant.now(),
                    "CRITICAL",
                    "Device failed: " + device.getName(),
                    String.format("Device '%s' (IP: %s) failed due to: %s",
                        device.getName(),
                        device.getIpAddress(),
                        failureEvent.getFailureType())
                );

                // Add device context to the event
                deviceFailureEvent.setDevice(device);
                simulationEventRepository.save(deviceFailureEvent);
                simulation.addEvent(deviceFailureEvent);

                // Mark device as affected
                result.addAffectedDevice(device);

                // Update device status based on failure type
                switch (failureEvent.getFailureType()) {
                    case "DEVICE_DOWN":
                        device.setStatus("UNHEALTHY");
                        break;
                    case "DEVICE_COMPROMISED":
                        device.setStatus("COMPROMISED");
                        break;
                    case "DEVICE_MISCONFIGURED":
                        device.setStatus("UNHEALTHY");
                        break;
                    default:
                        device.setStatus("UNHEALTHY");
                }

                // In a real implementation, we would save the device here

                // Add impact to result
                result.addImpact(failureEvent.getImpactScore());

                // Calculate downstream impact
                calculateDownstreamImpact(context, device, failureEvent, result);
            }

        } catch (Exception e) {
            logger.error("Failure event failed: {}", failureEvent.getName(), e);

            // Create simulation event for event failure
            SimulationEvent eventFailureEvent = new SimulationEvent(
                simulation,
                "EVENT_FAILURE",
                Instant.now(),
                "ERROR",
                "Failure event failed: " + failureEvent.getName(),
                "Failure event failed: " + failureEvent.getName() + " - " + e.getMessage()
            );
            simulationEventRepository.save(eventFailureEvent);
            simulation.addEvent(eventFailureEvent);

            throw e;
        }
    }

    /**
     * Calculates downstream impact of a device failure.
     * @param context The simulation context
     * @param failedDevice The device that failed
     * @param failureEvent The failure event
     * @param result The simulation result to update
     */
    private void calculateDownstreamImpact(SimulationContext context, Device failedDevice, FailureEvent failureEvent, SimulationResult result) {
        logger.debug("Calculating downstream impact for device: {}", failedDevice.getName());

        // Get all devices that depend on the failed device
        Set<Device> dependentDevices = topology.findReachableDevices(failedDevice.getId());
        dependentDevices.remove(failedDevice); // Remove the failed device itself

        for (Device dependentDevice : dependentDevices) {
            // Skip if already affected
            if (result.isDeviceAffected(dependentDevice.getId())) {
                continue;
            }

            // Create simulation event for downstream impact
            SimulationEvent downstreamEvent = new SimulationEvent(
                context.getSimulation(),
                "DOWNSTREAM_IMPACT",
                Instant.now(),
                "WARNING",
                "Downstream impact detected: " + dependentDevice.getName(),
                String.format("Device '%s' (IP: %s) affected by failure of '%s'",
                    dependentDevice.getName(),
                    dependentDevice.getIpAddress(),
                    failedDevice.getName())
            );

            // Add device context to the event
            downstreamEvent.setDevice(dependentDevice);
            simulationEventRepository.save(downstreamEvent);
            context.getSimulation().addEvent(downstreamEvent);

            // Mark device as affected
            result.addAffectedDevice(dependentDevice);

            // Update device status
            dependentDevice.setStatus("UNHEALTHY");
            // In a real implementation, we would save the device here

            // Add impact to result
            result.addImpact(failureEvent.getDownstreamImpactScore());
        }
    }

    /**
     * Runs a Monte Carlo simulation for probabilistic risk assessment.
     * @param context The simulation context
     * @param iterations The number of iterations to run
     * @return The Monte Carlo simulation result
     */
    @Transactional
    public MonteCarloResult runMonteCarloSimulation(SimulationContext context, int iterations) {
        logger.info("Running Monte Carlo simulation with {} iterations", iterations);

        Simulation simulation = context.getSimulation();
        MonteCarloResult result = new MonteCarloResult(simulation);

        try {
            // Create simulation event for Monte Carlo start
            SimulationEvent mcStartEvent = new SimulationEvent(
                simulation,
                "MONTE_CARLO_START",
                Instant.now(),
                "INFO",
                "Monte Carlo simulation started with " + iterations + " iterations",
                "Starting Monte Carlo simulation"
            );
            simulationEventRepository.save(mcStartEvent);
            simulation.addEvent(mcStartEvent);

            // Run iterations
            for (int i = 0; i < iterations; i++) {
                // Check if simulation was cancelled
                if (simulation.isCancelled()) {
                    throw new RuntimeException("Simulation was cancelled");
                }

                // Run a single iteration
                MonteCarloIterationResult iterationResult = runMonteCarloIteration(context);

                // Aggregate results
                result.addIterationResult(iterationResult);

                // Update simulation progress
                int progress = (int) ((double) (i + 1) / iterations * 100);
                simulation.updateProgress(BigDecimal.valueOf(progress));
                simulationRepository.save(simulation);
            }

            // Calculate final statistics
            result.calculateStatistics();

            // Create simulation event for Monte Carlo completion
            SimulationEvent mcCompleteEvent = new SimulationEvent(
                simulation,
                "MONTE_CARLO_COMPLETE",
                Instant.now(),
                "INFO",
                "Monte Carlo simulation completed",
                String.format("Monte Carlo simulation completed. Mean impact: %.2f, 95th percentile: %.2f",
                    result.getMeanImpact(),
                    result.getPercentileImpact(95))
            );
            simulationEventRepository.save(mcCompleteEvent);
            simulation.addEvent(mcCompleteEvent);

        } catch (Exception e) {
            logger.error("Monte Carlo simulation failed", e);

            // Create simulation event for Monte Carlo failure
            SimulationEvent mcFailureEvent = new SimulationEvent(
                simulation,
                "MONTE_CARLO_FAILURE",
                Instant.now(),
                "ERROR",
                "Monte Carlo simulation failed: " + e.getMessage(),
                "Monte Carlo simulation failed: " + e.getMessage()
            );
            simulationEventRepository.save(mcFailureEvent);
            simulation.addEvent(mcFailureEvent);

            throw e;
        }

        return result;
    }

    /**
     * Runs a single Monte Carlo iteration.
     * @param context The simulation context
     * @return The iteration result
     */
    private MonteCarloIterationResult runMonteCarloIteration(SimulationContext context) {
        // Create a copy of the current topology for this iteration
        NetworkTopology iterationTopology = topology.copy();

        // Generate random attack scenarios based on threat intelligence
        List<AttackScenario> attackScenarios = generateRandomAttackScenarios(context);

        // Run each attack scenario
        MonteCarloIterationResult result = new MonteCarloIterationResult();

        for (AttackScenario scenario : attackScenarios) {
            // Create a temporary simulation context for this scenario
            SimulationContext scenarioContext = new SimulationContext(
                context.getSimulation(),
                iterationTopology,
                context.getUser()
            );

            // Run the attack scenario
            SimulationResult scenarioResult = runAttackSimulation(scenarioContext, scenario);

            // Aggregate results
            result.merge(scenarioResult);
        }

        return result;
    }

    /**
     * Calculates the impact of an attack simulation.
     * @param result The simulation result to update
     */
    private void calculateAttackImpact(SimulationResult result) {
        logger.debug("Calculating attack impact");

        // Base impact from compromised devices
        BigDecimal deviceImpact = result.getCompromisedDevices().size() * 10.0;

        // Additional impact based on device criticality
        BigDecimal criticalityImpact = result.getCompromisedDevices().stream()
            .map(device -> device.getCriticality() != null ? device.getCriticality() : 3)
            .map(BigDecimal::valueOf)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total impact
        BigDecimal totalImpact = deviceImpact.add(criticalityImpact);

        // Set impact score
        result.setImpactScore(totalImpact);

        // Calculate expected loss (simplified)
        BigDecimal expectedLoss = totalImpact.multiply(new BigDecimal("0.1")); // 10% of impact as expected loss
        result.setExpectedLoss(expectedLoss);

        // Calculate downtime impact (simplified)
        BigDecimal downtimeImpact = result.getCompromisedDevices().size() * 2.0; // 2 hours per device
        result.setDowntimeImpact(downtimeImpact);
    }

    /**
     * Calculates the impact of a failure simulation.
     * @param result The simulation result to update
     */
    private void calculateFailureImpact(SimulationResult result) {
        logger.debug("Calculating failure impact");

        // Base impact from affected devices
        BigDecimal deviceImpact = result.getAffectedDevices().size() * 8.0;

        // Additional impact based on device criticality
        BigDecimal criticalityImpact = result.getAffectedDevices().stream()
            .map(device -> device.getCriticality() != null ? device.getCriticality() : 3)
            .map(BigDecimal::valueOf)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total impact
        BigDecimal totalImpact = deviceImpact.add(criticalityImpact);

        // Set impact score
        result.setImpactScore(totalImpact);

        // Calculate expected loss (simplified)
        BigDecimal expectedLoss = totalImpact.multiply(new BigDecimal("0.15")); // 15% of impact as expected loss
        result.setExpectedLoss(expectedLoss);

        // Calculate downtime impact (simplified)
        BigDecimal downtimeImpact = result.getAffectedDevices().size() * 3.0; // 3 hours per device
        result.setDowntimeImpact(downtimeImpact);
    }

    /**
     * Finds target devices based on criteria.
     * @param context The simulation context
     * @param criteria The target criteria
     * @return A list of target devices
     */
    private List<Device> findTargetDevices(SimulationContext context, TargetCriteria criteria) {
        return topology.getAllDevices().stream()
            .filter(device -> matchesCriteria(device, criteria))
            .collect(Collectors.toList());
    }

    /**
     * Checks if a device matches the target criteria.
     * @param device The device to check
     * @param criteria The criteria to match
     * @return true if the device matches the criteria
     */
    private boolean matchesCriteria(Device device, TargetCriteria criteria) {
        // Check device type
        if (criteria.getDeviceTypes() != null && !criteria.getDeviceTypes().isEmpty()) {
            if (!criteria.getDeviceTypes().contains(device.getType())) {
                return false;
            }
        }

        // Check device criticality
        if (criteria.getMinCriticality() != null && device.getCriticality() < criteria.getMinCriticality()) {
            return false;
        }

        if (criteria.getMaxCriticality() != null && device.getCriticality() > criteria.getMaxCriticality()) {
            return false;
        }

        // Check device status
        if (criteria.getStatus() != null && !criteria.getStatus().equals(device.getStatus())) {
            return false;
        }

        // Check IP address pattern
        if (criteria.getIpPattern() != null && device.getIpAddress() != null) {
            if (!device.getIpAddress().matches(criteria.getIpPattern())) {
                return false;
            }
        }

        // Check metadata
        if (criteria.getMetadataCriteria() != null) {
            for (Map.Entry<String, Object> entry : criteria.getMetadataCriteria().entrySet()) {
                Object value = device.getMetadata().get(entry.getKey());
                if (!entry.getValue().equals(value)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Calculates the success probability of an attack on a device.
     * @param device The target device
     * @param step The attack step
     * @return The success probability (0.0 to 1.0)
     */
    private double calculateAttackSuccessProbability(Device device, AttackStep step) {
        // Base success probability from the step
        double baseProbability = step.getSuccessProbability();

        // Adjust based on device security posture
        double securityFactor = 1.0;
        if (device.getRiskFactors() != null) {
            // Get security posture from risk factors
            Object securityPosture = device.getRiskFactors().get("security_posture");
            if (securityPosture instanceof String) {
                switch ((String) securityPosture) {
                    case "HIGH":
                        securityFactor = 0.7; // 30% reduction in success probability
                        break;
                    case "MEDIUM":
                        securityFactor = 0.9; // 10% reduction in success probability
                        break;
                    case "LOW":
                        securityFactor = 1.1; // 10% increase in success probability
                        break;
                    case "NONE":
                        securityFactor = 1.3; // 30% increase in success probability
                        break;
                }
            }
        }

        // Adjust based on device criticality (more critical devices have better security)
        double criticalityFactor = 1.0;
        if (device.getCriticality() != null) {
            // Criticality 1-5, where 1 is most critical
            criticalityFactor = 1.0 - (device.getCriticality() - 1) * 0.1; // 0% to 40% reduction
        }

        // Calculate final probability
        double finalProbability = baseProbability * securityFactor * criticalityFactor;

        // Clamp between 0.0 and 1.0
        return Math.max(0.0, Math.min(1.0, finalProbability));
    }

    /**
     * Calculates the propagation probability between devices.
     * @param sourceDevice The source device
     * @param targetDevice The target device
     * @param step The attack step
     * @return The propagation probability (0.0 to 1.0)
     */
    private double calculatePropagationProbability(Device sourceDevice, Device targetDevice, AttackStep step) {
        // Base success probability from the step
        double baseProbability = step.getPropagationProbability();

        // Adjust based on connection security
        double connectionSecurityFactor = 1.0;
        // In a real implementation, we would get the connection between devices
        // and check its security properties

        // Adjust based on target device security posture
        double targetSecurityFactor = 1.0;
        if (targetDevice.getRiskFactors() != null) {
            Object securityPosture = targetDevice.getRiskFactors().get("security_posture");
            if (securityPosture instanceof String) {
                switch ((String) securityPosture) {
                    case "HIGH":
                        targetSecurityFactor = 0.6; // 40% reduction in success probability
                        break;
                    case "MEDIUM":
                        targetSecurityFactor = 0.8; // 20% reduction in success probability
                        break;
                    case "LOW":
                        targetSecurityFactor = 1.2; // 20% increase in success probability
                        break;
                    case "NONE":
                        targetSecurityFactor = 1.4; // 40% increase in success probability
                        break;
                }
            }
        }

        // Calculate final probability
        double finalProbability = baseProbability * connectionSecurityFactor * targetSecurityFactor;

        // Clamp between 0.0 and 1.0
        return Math.max(0.0, Math.min(1.0, finalProbability));
    }

    /**
     * Generates random attack scenarios for Monte Carlo simulation.
     * @param context The simulation context
     * @return A list of random attack scenarios
     */
    private List<AttackScenario> generateRandomAttackScenarios(SimulationContext context) {
        List<AttackScenario> scenarios = new ArrayList<>();

        // Generate DDoS scenario
        AttackScenario ddosScenario = new AttackScenario();
        ddosScenario.setName("Random DDoS Attack");
        ddosScenario.setType("DDoS");

        // Create random steps for the scenario
        List<AttackStep> steps = new ArrayList<>();

        // Step 1: Initial compromise
        AttackStep initialStep = new AttackStep();
        initialStep.setName("Initial Compromise");
        initialStep.setTargetCriteria(createRandomTargetCriteria());
        initialStep.setSuccessProbability(0.3); // 30% success probability
        initialStep.setImpactScore(5.0);
        initialStep.setThreatId(generateRandomThreatId());
        steps.add(initialStep);

        // Step 2: Lateral movement
        AttackStep lateralStep = new AttackStep();
        lateralStep.setName("Lateral Movement");
        lateralStep.setTargetCriteria(createRandomTargetCriteria());
        lateralStep.setSuccessProbability(0.2); // 20% success probability
        lateralStep.setImpactScore(7.0);
        lateralStep.setThreatId(generateRandomThreatId());
        lateralStep.setLateralMovement(true);
        steps.add(lateralStep);

        // Step 3: DDoS attack
        AttackStep ddosStep = new AttackStep();
        ddosStep.setName("DDoS Attack");
        ddosStep.setTargetCriteria(createRandomTargetCriteria());
        ddosStep.setSuccessProbability(0.5); // 50% success probability
        ddosStep.setImpactScore(15.0);
        ddosStep.setThreatId(generateRandomThreatId());
        steps.add(ddosStep);

        ddosScenario.setSteps(steps);
        scenarios.add(ddosScenario);

        // Generate ransomware scenario
        AttackScenario ransomwareScenario = new AttackScenario();
        ransomwareScenario.setName("Random Ransomware Attack");
        ransomwareScenario.setType("RANSOMWARE");

        // Create random steps for the scenario
        List<AttackStep> ransomwareSteps = new ArrayList<>();

        // Step 1: Phishing email
        AttackStep phishingStep = new AttackStep();
        phishingStep.setName("Phishing Email");
        phishingStep.setTargetCriteria(createRandomTargetCriteria());
        phishingStep.setSuccessProbability(0.1); // 10% success probability
        phishingStep.setImpactScore(3.0);
        phishingStep.setThreatId(generateRandomThreatId());
        ransomwareSteps.add(phishingStep);

        // Step 2: Initial access
        AttackStep accessStep = new AttackStep();
        accessStep.setName("Initial Access");
        accessStep.setTargetCriteria(createRandomTargetCriteria());
        accessStep.setSuccessProbability(0.4); // 40% success probability
        accessStep.setImpactScore(8.0);
        accessStep.setThreatId(generateRandomThreatId());
        ransomwareSteps.add(accessStep);

        // Step 3: Privilege escalation
        AttackStep privilegeStep = new AttackStep();
        privilegeStep.setName("Privilege Escalation");
        privilegeStep.setTargetCriteria(createRandomTargetCriteria());
        privilegeStep.setSuccessProbability(0.3); // 30% success probability
        privilegeStep.setImpactScore(10.0);
        privilegeStep.setThreatId(generateRandomThreatId());
        privilegeStep.setLateralMovement(true);
        ransomwareSteps.add(privilegeStep);

        // Step 4: Ransomware deployment
        AttackStep ransomwareStep = new AttackStep();
        ransomwareStep.setName("Ransomware Deployment");
        ransomwareStep.setTargetCriteria(createRandomTargetCriteria());
        ransomwareStep.setSuccessProbability(0.6); // 60% success probability
        ransomwareStep.setImpactScore(20.0);
        ransomwareStep.setThreatId(generateRandomThreatId());
        ransomwareSteps.add(ransomwareStep);

        ransomwareScenario.setSteps(ransomwareSteps);
        scenarios.add(ransomwareScenario);

        return scenarios;
    }

    /**
     * Creates random target criteria for attack scenarios.
     * @return Random target criteria
     */
    private TargetCriteria createRandomTargetCriteria() {
        TargetCriteria criteria = new TargetCriteria();

        // Randomly select device types
        List<String> deviceTypes = Arrays.asList("ROUTER", "SWITCH", "FIREWALL", "SERVER", "IOT");
        Random random = new Random();
        int typeCount = random.nextInt(3) + 1; // 1-3 types
        Set<String> selectedTypes = new HashSet<>();

        while (selectedTypes.size() < typeCount) {
            selectedTypes.add(deviceTypes.get(random.nextInt(deviceTypes.size())));
        }

        criteria.setDeviceTypes(new ArrayList<>(selectedTypes));

        // Random criticality range
        int minCriticality = random.nextInt(3) + 1; // 1-3
        int maxCriticality = minCriticality + random.nextInt(3) + 1; // min+1 to min+3
        criteria.setMinCriticality(minCriticality);
        criteria.setMaxCriticality(Math.min(maxCriticality, 5));

        // Random status
        List<String> statuses = Arrays.asList("HEALTHY", "UNHEALTHY", "COMPROMISED");
        criteria.setStatus(statuses.get(random.nextInt(statuses.size())));

        return criteria;
    }

    /**
     * Generates a random threat ID for attack scenarios.
     * @return Random threat ID
     */
    private UUID generateRandomThreatId() {
        return UUID.randomUUID();
    }

    /**
     * Submits a simulation to the queue for processing.
     * @param simulation The simulation to run
     * @param user The user running the simulation
     * @param scenario The attack scenario (optional)
     * @param failureScenario The failure scenario (optional)
     * @param isMonteCarlo Whether this is a Monte Carlo simulation
     * @param iterations Number of iterations for Monte Carlo simulation
     * @return The submitted simulation task
     * @throws InterruptedException If the thread is interrupted while waiting to submit the task
     */
    public SimulationTask submitSimulation(
        Simulation simulation,
        User user,
        AttackScenario scenario,
        FailureScenario failureScenario,
        boolean isMonteCarlo,
        int iterations
    ) throws InterruptedException {
        // Create simulation task
        SimulationTask task = new SimulationTask(
            simulation,
            user,
            scenario,
            failureScenario,
            isMonteCarlo,
            iterations
        );

        // Submit to queue
        simulationQueue.put(task);

        logger.info("Simulation submitted to queue: {}", simulation.getId());

        return task;
    }

    /**
     * Cancels an active simulation.
     * @param simulationId The ID of the simulation to cancel
     * @return true if the simulation was cancelled, false if not found or already completed
     */
    public boolean cancelSimulation(UUID simulationId) {
        // Check if simulation is in the queue
        // In a real implementation, we would need to implement this

        // Check if simulation is active
        SimulationContext context = activeSimulations.get(simulationId);
        if (context != null) {
            // Mark simulation as cancelled
            context.getSimulation().cancel();
            simulationRepository.save(context.getSimulation());

            logger.info("Simulation cancelled: {}", simulationId);
            return true;
        }

        return false;
    }

    /**
     * Gets the status of an active simulation.
     * @param simulationId The ID of the simulation
     * @return The simulation context, or null if not found
     */
    public SimulationContext getActiveSimulation(UUID simulationId) {
        return activeSimulations.get(simulationId);
    }

    /**
     * Gets all active simulations.
     * @return A map of active simulations
     */
    public Map<UUID, SimulationContext> getActiveSimulations() {
        return Collections.unmodifiableMap(activeSimulations);
    }

    /**
     * Shuts down the simulation engine.
     */
    public void shutdown() {
        logger.info("Shutting down simulation engine");

        // Cancel all active simulations
        for (SimulationContext context : activeSimulations.values()) {
            context.getSimulation().cancel();
            simulationRepository.save(context.getSimulation());
        }

        // Clear active simulations
        activeSimulations.clear();

        // Shutdown executor
        simulationExecutor.shutdown();

        try {
            // Wait for tasks to complete
            if (!simulationExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                simulationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            simulationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Simulation engine shutdown complete");
    }
}
