package com.network.digitaltwin.monitoring.service;

import com.network.digitaltwin.model.Device;
import com.network.digitaltwin.model.auth.User;
import com.network.digitaltwin.monitoring.model.DeviceStatus;
import com.network.digitaltwin.monitoring.model.DeviceMetrics;
import com.network.digitaltwin.monitoring.model.HealthRule;
import com.network.digitaltwin.monitoring.repository.DeviceMetricsRepository;
import com.network.digitaltwin.monitoring.repository.DeviceStatusRepository;
import com.network.digitaltwin.monitoring.repository.HealthRuleRepository;
import com.network.digitaltwin.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for device health monitoring operations.
 */
@Service
public class DeviceHealthService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceHealthService.class);

    private static final String DEVICE_STATUS_CACHE_KEY_PREFIX = "device:status:";
    private static final long CACHE_TTL_SECONDS = 60; // 1 minute

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceStatusRepository deviceStatusRepository;

    @Autowired
    private DeviceMetricsRepository deviceMetricsRepository;

    @Autowired
    private HealthRuleRepository healthRuleRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private HealthEvaluationService healthEvaluationService;

    /**
     * Get the latest status of a device.
     * @param deviceId The ID of the device
     * @return The latest status of the device
     */
    public DeviceStatus getLatestDeviceStatus(UUID deviceId) {
        // Try to get from cache first
        String cacheKey = DEVICE_STATUS_CACHE_KEY_PREFIX + deviceId;
        DeviceStatus cachedStatus = (DeviceStatus) redisTemplate.opsForValue().get(cacheKey);

        if (cachedStatus != null) {
            logger.debug("Retrieved device status from cache for device: {}", deviceId);
            return cachedStatus;
        }

        // If not in cache, get from database
        DeviceStatus status = deviceStatusRepository.findTopByDeviceIdOrderByReceivedAtDesc(deviceId);

        // Cache the result
        if (status != null) {
            redisTemplate.opsForValue().set(cacheKey, status, CACHE_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            logger.debug("Cached device status for device: {}", deviceId);
        }

        return status;
    }

    /**
     * Process a heartbeat from a device.
     * @param deviceId The ID of the device
     * @param heartbeat The heartbeat data
     * @param user The user processing the heartbeat (if applicable)
     */
    @Transactional
    public void processHeartbeat(UUID deviceId, Map<String, Object> heartbeat, User user) {
        logger.debug("Processing heartbeat for device: {}", deviceId);

        // Validate heartbeat
        validateHeartbeat(heartbeat);

        // Extract heartbeat data
        String heartbeatId = (String) heartbeat.get("heartbeatId");
        Instant timestamp = Instant.parse((String) heartbeat.get("timestamp"));
        String status = (String) heartbeat.get("status");
        Map<String, Object> metrics = (Map<String, Object>) heartbeat.get("metrics");
        Map<String, Object> metadata = (Map<String, Object>) heartbeat.getOrDefault("metadata", new HashMap<>());

        // Check if this is a duplicate heartbeat
        if (heartbeatId != null && isDuplicateHeartbeat(deviceId, heartbeatId)) {
            logger.warn("Duplicate heartbeat detected for device: {}, heartbeatId: {}", deviceId, heartbeatId);
            return;
        }

        // Get device
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        // Validate timestamp
        validateTimestamp(timestamp);

        // Create device status
        DeviceStatus deviceStatus = new DeviceStatus();
        deviceStatus.setDevice(device);
        deviceStatus.setStatus(status);
        deviceStatus.setLastSeen(timestamp);
        deviceStatus.setReceivedAt(Instant.now());
        deviceStatus.setIngestSource("AGENT");
        deviceStatus.setHeartbeatId(heartbeatId);

        // Set metrics
        if (metrics != null) {
            deviceStatus.setCpuPercent(getBigDecimalValue(metrics, "cpuPercent"));
            deviceStatus.setMemoryPercent(getBigDecimalValue(metrics, "memoryPercent"));
            deviceStatus.setDiskPercent(getBigDecimalValue(metrics, "diskPercent"));
            deviceStatus.setNetworkInKb(getLongValue(metrics, "networkInKb"));
            deviceStatus.setNetworkOutKb(getLongValue(metrics, "networkOutKb"));
            deviceStatus.setUptimeSeconds(getLongValue(metrics, "uptimeSeconds"));

            // Add any additional metrics
            metrics.entrySet().stream()
                .filter(entry -> !isStandardMetric(entry.getKey()))
                .forEach(entry -> deviceStatus.addMetric(entry.getKey(), entry.getValue()));
        }

        // Add metadata
        metadata.entrySet().stream()
            .forEach(entry -> deviceStatus.addMetric("meta:" + entry.getKey(), entry.getValue()));

        // Save device status
        deviceStatus = deviceStatusRepository.save(deviceStatus);

        // Create device metrics record
        createDeviceMetrics(deviceStatus);

        // Update cache
        String cacheKey = DEVICE_STATUS_CACHE_KEY_PREFIX + deviceId;
        redisTemplate.opsForValue().set(cacheKey, deviceStatus, CACHE_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

        // Evaluate health rules asynchronously
        evaluateHealthRulesAsync(deviceStatus);

        // Send event to Kafka
        sendHeartbeatEvent(deviceStatus);

        logger.info("Processed heartbeat for device: {}, status: {}", deviceId, status);
    }

    /**
     * Get historical status and metrics for a device.
     * @param deviceId The ID of the device
     * @param start Start date (optional)
     * @param end End date (optional)
     * @param statusFilter Status filter (optional)
     * @param pageable Pagination information
     * @return Paginated historical metrics and status events
     */
    public Page<DeviceMetrics> getDeviceStatusHistory(UUID deviceId, Instant start, Instant end, 
                                                     String statusFilter, Pageable pageable) {
        // If no start date, default to 7 days ago
        if (start == null) {
            start = Instant.now().minus(7, ChronoUnit.DAYS);
        }

        // If no end date, default to now
        if (end == null) {
            end = Instant.now();
        }

        return deviceMetricsRepository.findByDeviceIdAndMetricTimeBetweenAndStatusOrderByMetricTimeDesc(
            deviceId, start, end, statusFilter, pageable);
    }

    /**
     * Get a list of devices with their latest status.
     * @param statusFilter Status filter (optional)
     * @param criticalityFilter Criticality filter (optional)
     * @param ownerFilter Owner filter (optional)
     * @param lastSeenBefore Filter devices last seen before this date (optional)
     * @param lastSeenAfter Filter devices last seen after this date (optional)
     * @param pageable Pagination information
     * @return Paginated list of devices with their latest status
     */
    public Page<DeviceStatus> getDeviceStatusList(String statusFilter, Integer criticalityFilter, 
                                                UUID ownerFilter, Instant lastSeenBefore, 
                                                Instant lastSeenAfter, Pageable pageable) {
        return deviceStatusRepository.findFilteredDevices(
            statusFilter, criticalityFilter, ownerFilter, 
            lastSeenBefore, lastSeenAfter, pageable);
    }

    /**
     * Export device metrics to CSV.
     * @param deviceId The ID of the device
     * @param start Start date (optional)
     * @param end End date (optional)
     * @return CSV byte array
     */
    public byte[] exportDeviceMetrics(UUID deviceId, Instant start, Instant end) {
        // If no start date, default to 30 days ago
        if (start == null) {
            start = Instant.now().minus(30, ChronoUnit.DAYS);
        }

        // If no end date, default to now
        if (end == null) {
            end = Instant.now();
        }

        List<DeviceMetrics> metrics = deviceMetricsRepository
            .findByDeviceIdAndMetricTimeBetweenOrderByMetricTimeAsc(deviceId, start, end);

        if (metrics.isEmpty()) {
            return null;
        }

        // Build CSV content
        StringBuilder csv = new StringBuilder();

        // CSV header
        csv.append("Timestamp,Status,CPU (%),Memory (%),Disk (%),Network In (KB),Network Out (KB),Uptime (seconds)
");

        // CSV rows
        for (DeviceMetrics metric : metrics) {
            csv.append(metric.getMetricTime())
               .append(",")
               .append(metric.getStatus())
               .append(",")
               .append(metric.getCpuPercent() != null ? metric.getCpuPercent() : "")
               .append(",")
               .append(metric.getMemoryPercent() != null ? metric.getMemoryPercent() : "")
               .append(",")
               .append(metric.getDiskPercent() != null ? metric.getDiskPercent() : "")
               .append(",")
               .append(metric.getNetworkInKb() != null ? metric.getNetworkInKb() : "")
               .append(",")
               .append(metric.getNetworkOutKb() != null ? metric.getNetworkOutKb() : "")
               .append(",")
               .append(metric.getUptimeSeconds() != null ? metric.getUptimeSeconds() : "")
               .append("
");
        }

        return csv.toString().getBytes();
    }

    /**
     * Asynchronously evaluate health rules for a device status.
     * @param deviceStatus The device status to evaluate
     */
    @Async
    public CompletableFuture<Void> evaluateHealthRulesAsync(DeviceStatus deviceStatus) {
        try {
            List<HealthRule> rules = healthRuleRepository.findByEnabledTrueOrderByPriorityAsc();
            healthEvaluationService.evaluateRules(deviceStatus, rules);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error evaluating health rules for device: {}", deviceStatus.getDevice().getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send a heartbeat event to Kafka.
     * @param deviceStatus The device status
     */
    private void sendHeartbeatEvent(DeviceStatus deviceStatus) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("deviceId", deviceStatus.getDevice().getId());
            event.put("timestamp", deviceStatus.getLastSeen());
            event.put("status", deviceStatus.getStatus());
            event.put("metrics", deviceStatus.getMetrics());
            event.put("receivedAt", deviceStatus.getReceivedAt());

            kafkaProducerService.send("device.heartbeat", event);
        } catch (Exception e) {
            logger.error("Error sending heartbeat event to Kafka for device: {}", 
                deviceStatus.getDevice().getId(), e);
        }
    }

    /**
     * Create a device metrics record from device status.
     * @param deviceStatus The device status
     */
    private void createDeviceMetrics(DeviceStatus deviceStatus) {
        DeviceMetrics metrics = new DeviceMetrics();
        metrics.setDevice(deviceStatus.getDevice());
        metrics.setMetricTime(deviceStatus.getLastSeen());
        metrics.setStatus(deviceStatus.getStatus());
        metrics.setCpuPercent(deviceStatus.getCpuPercent());
        metrics.setMemoryPercent(deviceStatus.getMemoryPercent());
        metrics.setDiskPercent(deviceStatus.getDiskPercent());
        metrics.setNetworkInKb(deviceStatus.getNetworkInKb());
        metrics.setNetworkOutKb(deviceStatus.getNetworkOutKb());
        metrics.setUptimeSeconds(deviceStatus.getUptimeSeconds());
        metrics.setMetadata(deviceStatus.getMetrics().toString());

        deviceMetricsRepository.save(metrics);
    }

    /**
     * Validate a heartbeat.
     * @param heartbeat The heartbeat data
     * @throws IllegalArgumentException if the heartbeat is invalid
     */
    private void validateHeartbeat(Map<String, Object> heartbeat) {
        if (heartbeat == null) {
            throw new IllegalArgumentException("Heartbeat data is required");
        }

        if (!heartbeat.containsKey("timestamp")) {
            throw new IllegalArgumentException("Heartbeat timestamp is required");
        }

        if (!heartbeat.containsKey("status")) {
            throw new IllegalArgumentException("Heartbeat status is required");
        }

        if (!heartbeat.containsKey("metrics")) {
            throw new IllegalArgumentException("Heartbeat metrics are required");
        }

        String status = (String) heartbeat.get("status");
        if (!Arrays.asList("HEALTHY", "DEGRADED", "UNHEALTHY", "COMPROMISED", "UNKNOWN").contains(status)) {
            throw new IllegalArgumentException("Invalid heartbeat status: " + status);
        }
    }

    /**
     * Validate a timestamp.
     * @param timestamp The timestamp to validate
     * @throws IllegalArgumentException if the timestamp is invalid
     */
    private void validateTimestamp(Instant timestamp) {
        Instant now = Instant.now();
        Instant futureThreshold = now.plusSeconds(300); // 5 minutes in the future

        if (timestamp.isAfter(futureThreshold)) {
            throw new IllegalArgumentException("Heartbeat timestamp is too far in the future");
        }

        // Check if this is earlier than the last heartbeat for this device
        DeviceStatus lastStatus = deviceStatusRepository.findTopByDeviceIdOrderByReceivedAtDesc(
            ((Map<String, Object>) redisTemplate.opsForValue().get("device:current:" + timestamp)).get("deviceId"));

        if (lastStatus != null && timestamp.isBefore(lastStatus.getLastSeen())) {
            logger.warn("Non-monotonic heartbeat detected for device. Current: {}, Previous: {}", 
                timestamp, lastStatus.getLastSeen());
        }
    }

    /**
     * Check if a heartbeat is a duplicate.
     * @param deviceId The ID of the device
     * @param heartbeatId The heartbeat ID
     * @return true if the heartbeat is a duplicate
     */
    private boolean isDuplicateHeartbeat(UUID deviceId, String heartbeatId) {
        if (heartbeatId == null) {
            return false;
        }

        return deviceStatusRepository.existsByDeviceIdAndHeartbeatId(deviceId, heartbeatId);
    }

    /**
     * Get a BigDecimal value from a map.
     * @param map The map to get the value from
     * @param key The key to get
     * @return The BigDecimal value or null if not found
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return null;
    }

    /**
     * Get a Long value from a map.
     * @param map The map to get the value from
     * @param key The key to get
     * @return The Long value or null if not found
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * Check if a key is a standard metric.
     * @param key The key to check
     * @return true if the key is a standard metric
     */
    private boolean isStandardMetric(String key) {
        return Arrays.asList(
            "cpuPercent", "memoryPercent", "diskPercent", 
            "networkInKb", "networkOutKb", "uptimeSeconds"
        ).contains(key);
    }
}
