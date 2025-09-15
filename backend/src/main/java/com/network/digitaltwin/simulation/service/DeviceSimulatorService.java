package com.network.digitaltwin.simulation.service;

import com.network.digitaltwin.model.Device;
import com.network.digitaltwin.monitoring.model.DeviceStatus;
import com.network.digitaltwin.monitoring.repository.DeviceStatusRepository;
import com.network.digitaltwin.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for simulating device heartbeats for testing and demo purposes.
 */
@Service
public class DeviceSimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceSimulatorService.class);

    private final Map<UUID, ScheduledExecutorService> activeSimulators = new ConcurrentHashMap<>();

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceStatusRepository deviceStatusRepository;

    @Autowired
    private DeviceHealthService deviceHealthService;

    /**
     * Start simulating heartbeats for a device.
     * @param deviceId The ID of the device
     * @param config Configuration for the simulator
     */
    public void startSimulation(UUID deviceId, Map<String, Object> config) {
        // Check if already running
        if (activeSimulators.containsKey(deviceId)) {
            throw new IllegalStateException("Simulation already running for device: " + deviceId);
        }

        // Get device
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        // Create simulator
        DeviceSimulator simulator = new DeviceSimulator(device, config);

        // Create scheduled executor
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        activeSimulators.put(deviceId, scheduler);

        // Schedule heartbeat generation
        long interval = getLongValue(config, "interval", 30L); // Default 30 seconds
        scheduler.scheduleAtFixedRate(simulator::generateHeartbeat, 0, interval, TimeUnit.SECONDS);

        logger.info("Started device simulation for device: {}, interval: {}s", deviceId, interval);
    }

    /**
     * Stop simulating heartbeats for a device.
     * @param deviceId The ID of the device
     */
    public void stopSimulation(UUID deviceId) {
        ScheduledExecutorService scheduler = activeSimulators.remove(deviceId);

        if (scheduler != null) {
            scheduler.shutdownNow();
            logger.info("Stopped device simulation for device: {}", deviceId);
        } else {
            logger.warn("No active simulation found for device: {}", deviceId);
        }
    }

    /**
     * Check if a device is being simulated.
     * @param deviceId The ID of the device
     * @return true if the device is being simulated, false otherwise
     */
    public boolean isSimulating(UUID deviceId) {
        return activeSimulators.containsKey(deviceId);
    }

    /**
     * Get all devices currently being simulated.
     * @return Set of device IDs being simulated
     */
    public Set<UUID> getSimulatedDevices() {
        return new HashSet<>(activeSimulators.keySet());
    }

    /**
     * Internal class for simulating device heartbeats.
     */
    private class DeviceSimulator implements Runnable {

        private final Device device;
        private final Map<String, Object> config;
        private final Random random;

        public DeviceSimulator(Device device, Map<String, Object> config) {
            this.device = device;
            this.config = config;
            this.random = new Random(config.containsKey("seed") ? 
                (long) config.get("seed") : System.currentTimeMillis());
        }

        @Override
        @Async
        public void run() {
            generateHeartbeat();
        }

        /**
         * Generate a heartbeat for the device.
         */
        public void generateHeartbeat() {
            try {
                // Create heartbeat data
                Map<String, Object> heartbeat = new HashMap<>();

                // Add heartbeat ID
                heartbeat.put("heartbeatId", UUID.randomUUID().toString());

                // Add device ID
                heartbeat.put("deviceId", device.getId());

                // Add timestamp
                heartbeat.put("timestamp", Instant.now().toString());

                // Add status
                String status = generateStatus();
                heartbeat.put("status", status);

                // Add metrics
                Map<String, Object> metrics = generateMetrics();
                heartbeat.put("metrics", metrics);

                // Add metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("simulated", true);
                metadata.put("simulatorVersion", "1.0.0");
                heartbeat.put("metadata", metadata);

                // Process the heartbeat
                deviceHealthService.processHeartbeat(device.getId(), heartbeat, null);

            } catch (Exception e) {
                logger.error("Error generating heartbeat for device: " + device.getId(), e);
            }
        }

        /**
         * Generate a status for the device.
         */
        private String generateStatus() {
            String baseStatus = (String) config.getOrDefault("baseStatus", "HEALTHY");
            double failureProbability = getDoubleValue(config, "failureProbability", 0.05);

            // Check if we should simulate a failure
            if (random.nextDouble() < failureProbability) {
                String[] failureTypes = {"DEGRADED", "UNHEALTHY", "COMPROMISED"};
                return failureTypes[random.nextInt(failureTypes.length)];
            }

            return baseStatus;
        }

        /**
         * Generate metrics for the device.
         */
        private Map<String, Object> generateMetrics() {
            Map<String, Object> metrics = new HashMap<>();

            // CPU percentage
            double cpuBase = getDoubleValue(config, "cpuBase", 30.0);
            double cpuNoise = getDoubleValue(config, "cpuNoise", 10.0);
            double cpu = Math.max(0, Math.min(100, cpuBase + random.nextGaussian() * cpuNoise));
            metrics.put("cpuPercent", BigDecimal.valueOf(cpu));

            // Memory percentage
            double memoryBase = getDoubleValue(config, "memoryBase", 50.0);
            double memoryNoise = getDoubleValue(config, "memoryNoise", 15.0);
            double memory = Math.max(0, Math.min(100, memoryBase + random.nextGaussian() * memoryNoise));
            metrics.put("memoryPercent", BigDecimal.valueOf(memory));

            // Disk percentage
            double diskBase = getDoubleValue(config, "diskBase", 60.0);
            double diskNoise = getDoubleValue(config, "diskNoise", 5.0);
            double disk = Math.max(0, Math.min(100, diskBase + random.nextGaussian() * diskNoise));
            metrics.put("diskPercent", BigDecimal.valueOf(disk));

            // Network
            long networkInBase = getLongValue(config, "networkInBase", 1000L);
            long networkInNoise = getLongValue(config, "networkInNoise", 500L);
            long networkIn = Math.max(0, networkInBase + random.nextLong(networkInNoise * 2) - networkInNoise);
            metrics.put("networkInKb", networkIn);

            long networkOutBase = getLongValue(config, "networkOutBase", 500L);
            long networkOutNoise = getLongValue(config, "networkOutNoise", 250L);
            long networkOut = Math.max(0, networkOutBase + random.nextLong(networkOutNoise * 2) - networkOutNoise);
            metrics.put("networkOutKb", networkOut);

            // Uptime
            long uptimeBase = getLongValue(config, "uptimeBase", 86400L); // 24 hours
            long uptimeNoise = getLongValue(config, "uptimeNoise", 3600L); // 1 hour
            long uptime = uptimeBase + random.nextLong(uptimeNoise * 2) - uptimeNoise;
            metrics.put("uptimeSeconds", uptime);

            return metrics;
        }

        /**
         * Get a double value from config with default.
         */
        private double getDoubleValue(Map<String, Object> config, String key, double defaultValue) {
            if (config.containsKey(key)) {
                Object value = config.get(key);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                } else if (value instanceof String) {
                    return Double.parseDouble((String) value);
                }
            }
            return defaultValue;
        }

        /**
         * Get a long value from config with default.
         */
        private long getLongValue(Map<String, Object> config, String key, long defaultValue) {
            if (config.containsKey(key)) {
                Object value = config.get(key);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                } else if (value instanceof String) {
                    return Long.parseLong((String) value);
                }
            }
            return defaultValue;
        }
    }
}
