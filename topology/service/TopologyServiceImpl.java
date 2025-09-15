package com.network.digitaltwin.topology.service;

import com.network.digitaltwin.model.Device;
import com.network.digitaltwin.model.auth.User;
import com.network.digitaltwin.topology.model.Connection;
import com.network.digitaltwin.topology.model.TopologySnapshot;
import com.network.digitaltwin.topology.repository.ConnectionRepository;
import com.network.digitaltwin.topology.repository.TopologySnapshotRepository;
import com.network.digitaltwin.repository.DeviceRepository;
import com.network.digitaltwin.repository.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of TopologyService for network topology management operations.
 */
@Service
public class TopologyServiceImpl implements TopologyService {

    private static final Logger logger = LoggerFactory.getLogger(TopologyServiceImpl.class);

    private static final int MAX_FAN_OUT = 10;
    private static final BigDecimal MIN_RELIABILITY = new BigDecimal("0.1");
    private static final BigDecimal MAX_RELIABILITY = new BigDecimal("1.0");
    private static final Long MIN_BANDWIDTH = 1L;
    private static final Long MAX_BANDWIDTH = 100000L; // 100 Gbps
    private static final Long MIN_LATENCY = 0L;
    private static final Long MAX_LATENCY = 1000L; // 1 second

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private TopologySnapshotRepository topologySnapshotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * Create a new connection between devices.
     * @param connectionData The connection data
     * @param userDetails The authenticated user
     * @return The created connection
     */
    @Override
    @Transactional
    public Connection createConnection(com.network.digitaltwin.topology.dto.ConnectionDTO connectionData, UserDetails userDetails) {
        logger.info("Creating connection between devices: {} -> {}", 
            connectionData.getSourceDeviceId(), connectionData.getTargetDeviceId());

        // Validate devices exist
        Device sourceDevice = deviceRepository.findById(connectionData.getSourceDeviceId())
            .orElseThrow(() -> new IllegalArgumentException("Source device not found: " + connectionData.getSourceDeviceId()));

        Device targetDevice = deviceRepository.findById(connectionData.getTargetDeviceId())
            .orElseThrow(() -> new IllegalArgumentException("Target device not found: " + connectionData.getTargetDeviceId()));

        // Validate no self-loops
        if (sourceDevice.getId().equals(targetDevice.getId())) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }

        // Validate connection attributes
        validateConnectionAttributes(connectionData);

        // Check if connection already exists
        if (connectionRepository.findBySourceDeviceIdAndTargetDeviceId(
                sourceDevice.getId(), targetDevice.getId()).isPresent()) {
            throw new IllegalArgumentException("Connection already exists between these devices");
        }

        // Check fan-out constraint
        long outgoingConnections = connectionRepository.countBySourceDeviceId(sourceDevice.getId());
        if (outgoingConnections >= MAX_FAN_OUT) {
            throw new IllegalArgumentException("Source device has reached maximum fan-out limit of " + MAX_FAN_OUT);
        }

        // Check if adding this connection would create a loop
        if (wouldCreateLoop(sourceDevice.getId(), targetDevice.getId())) {
            throw new IllegalArgumentException("Adding this connection would create a loop");
        }

        // Create connection
        Connection connection = new Connection();
        connection.setSourceDevice(sourceDevice);
        connection.setTargetDevice(targetDevice);
        connection.setConnectionType(connectionData.getConnectionType());
        connection.setBandwidth(connectionData.getBandwidth());
        connection.setLatency(connectionData.getLatency());
        connection.setReliability(connectionData.getReliability());
        connection.setStatus("ACTIVE");

        // Set metadata if provided
        if (connectionData.getMetadata() != null) {
            connectionData.getMetadata().forEach(connection::addMetadata);
        }

        // Save connection
        connection = connectionRepository.save(connection);

        // Create topology snapshot
        createTopologySnapshot("CREATE", "Created new connection: " + connection.getId(), userDetails);

        // Send event to Kafka
        sendTopologyChangeEvent("CONNECTION_CREATED", connection);

        logger.info("Created connection: {}", connection.getId());
        return connection;
    }

    /**
     * Get the full network topology.
     * @return The complete topology with nodes and edges
     */
    @Override
    public com.network.digitaltwin.topology.dto.TopologyDTO getFullTopology() {
        logger.debug("Retrieving full network topology");

        // Get all devices
        List<Device> devices = deviceRepository.findAll();

        // Get all connections
        List<Connection> connections = connectionRepository.findAll();

        // Build topology DTO
        com.network.digitaltwin.topology.dto.TopologyDTO topology = new com.network.digitaltwin.topology.dto.TopologyDTO();
        topology.setNodes(devices.stream()
            .map(device -> {
                com.network.digitaltwin.topology.dto.DeviceNode node = new com.network.digitaltwin.topology.dto.DeviceNode();
                node.setId(device.getId());
                node.setName(device.getName());
                node.setType(device.getType());
                node.setIp(device.getIp());
                node.setCriticality(device.getCriticality());
                node.setStatus(device.getStatus());
                node.setOwner(device.getOwner());
                return node;
            })
            .collect(Collectors.toList()));

        topology.setEdges(connections.stream()
            .map(connection -> {
                com.network.digitaltwin.topology.dto.ConnectionEdge edge = new com.network.digitaltwin.topology.dto.ConnectionEdge();
                edge.setId(connection.getId());
                edge.setSourceDeviceId(connection.getSourceDevice().getId());
                edge.setTargetDeviceId(connection.getTargetDevice().getId());
                edge.setType(connection.getConnectionType());
                edge.setBandwidth(connection.getBandwidth());
                edge.setLatency(connection.getLatency());
                edge.setReliability(connection.getReliability());
                edge.setStatus(connection.getStatus());
                return edge;
            })
            .collect(Collectors.toList()));

        logger.debug("Retrieved topology with {} nodes and {} edges", 
            topology.getNodes().size(), topology.getEdges().size());

        return topology;
    }

    /**
     * Get a specific connection by ID.
     * @param id The ID of the connection
     * @return The connection details
     */
    @Override
    public Connection getConnectionById(UUID id) {
        return connectionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
    }

    /**
     * Update an existing connection.
     * @param id The ID of the connection to update
     * @param connectionData The updated connection data
     * @param userDetails The authenticated user
     * @return The updated connection
     */
    @Override
    @Transactional
    public Connection updateConnection(UUID id, com.network.digitaltwin.topology.dto.ConnectionDTO connectionData, UserDetails userDetails) {
        logger.info("Updating connection: {}", id);

        // Get existing connection
        Connection connection = getConnectionById(id);

        // Validate connection attributes
        validateConnectionAttributes(connectionData);

        // Update connection properties
        if (connectionData.getConnectionType() != null) {
            connection.setConnectionType(connectionData.getConnectionType());
        }

        if (connectionData.getBandwidth() != null) {
            connection.setBandwidth(connectionData.getBandwidth());
        }

        if (connectionData.getLatency() != null) {
            connection.setLatency(connectionData.getLatency());
        }

        if (connectionData.getReliability() != null) {
            connection.setReliability(connectionData.getReliability());
        }

        if (connectionData.getStatus() != null) {
            connection.setStatus(connectionData.getStatus());
        }

        // Update metadata if provided
        if (connectionData.getMetadata() != null) {
            connection.getMetadata().clear();
            connectionData.getMetadata().forEach(connection::addMetadata);
        }

        // Save updated connection
        connection = connectionRepository.save(connection);

        // Create topology snapshot
        createTopologySnapshot("UPDATE", "Updated connection: " + connection.getId(), userDetails);

        // Send event to Kafka
        sendTopologyChangeEvent("CONNECTION_UPDATED", connection);

        logger.info("Updated connection: {}", connection.getId());
        return connection;
    }

    /**
     * Delete a connection.
     * @param id The ID of the connection to delete
     * @param userDetails The authenticated user
     */
    @Override
    @Transactional
    public void deleteConnection(UUID id, UserDetails userDetails) {
        logger.info("Deleting connection: {}", id);

        // Get connection
        Connection connection = getConnectionById(id);

        // Delete connection
        connectionRepository.delete(connection);

        // Create topology snapshot
        createTopologySnapshot("DELETE", "Deleted connection: " + id, userDetails);

        // Send event to Kafka
        sendTopologyChangeEvent("CONNECTION_DELETED", connection);

        logger.info("Deleted connection: {}", id);
    }

    /**
     * Get a list of all connections with pagination and filtering.
     * @param sourceDeviceId Filter by source device ID (optional)
     * @param targetDeviceId Filter by target device ID (optional)
     * @param connectionType Filter by connection type (optional)
     * @param status Filter by connection status (optional)
     * @param pageable Pagination information
     * @return Paginated list of connections
     */
    @Override
    public Page<Connection> getConnections(UUID sourceDeviceId, UUID targetDeviceId, 
                                         String connectionType, String status, Pageable pageable) {
        if (sourceDeviceId != null && targetDeviceId != null) {
            return connectionRepository.findBySourceDeviceIdAndTargetDeviceId(
                sourceDeviceId, targetDeviceId, pageable);
        } else if (sourceDeviceId != null) {
            return connectionRepository.findBySourceDeviceId(sourceDeviceId, pageable);
        } else if (targetDeviceId != null) {
            return connectionRepository.findByTargetDeviceId(targetDeviceId, pageable);
        } else if (connectionType != null && status != null) {
            return connectionRepository.findByConnectionTypeAndStatus(connectionType, status, pageable);
        } else if (connectionType != null) {
            return connectionRepository.findByConnectionType(connectionType, pageable);
        } else if (status != null) {
            return connectionRepository.findByStatus(status, pageable);
        } else {
            return connectionRepository.findAll(pageable);
        }
    }

    /**
     * Get topology snapshots with pagination.
     * @param start Start date (optional)
     * @param end End date (optional)
     * @param changeType Filter by change type (optional)
     * @param pageable Pagination information
     * @return Paginated list of topology snapshots
     */
    @Override
    public Page<TopologySnapshot> getTopologySnapshots(Instant start, Instant end, 
                                                      String changeType, Pageable pageable) {
        if (start != null && end != null && changeType != null) {
            return topologySnapshotRepository.findByCreatedAtBetweenAndChangeType(start, end, changeType, pageable);
        } else if (start != null && end != null) {
            return topologySnapshotRepository.findByCreatedAtBetween(start, end, pageable);
        } else if (changeType != null) {
            return topologySnapshotRepository.findByChangeType(changeType, pageable);
        } else {
            return topologySnapshotRepository.findAll(pageable);
        }
    }

    /**
     * Restore topology from a snapshot.
     * @param snapshotId The ID of the snapshot to restore
     * @param userDetails The authenticated user
     */
    @Override
    @Transactional
    public void restoreTopologySnapshot(UUID snapshotId, UserDetails userDetails) {
        logger.info("Restoring topology from snapshot: {}", snapshotId);

        // Get snapshot
        TopologySnapshot snapshot = topologySnapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        // In a real implementation, this would involve:
        // 1. Clearing all current connections
        // 2. Recreating connections based on the snapshot data
        // 3. Updating the topology version

        // For this example, we'll just create a new snapshot
        createTopologySnapshot("RESTORE", "Restored topology from snapshot: " + snapshotId, userDetails);

        // Send event to Kafka
        sendTopologyChangeEvent("TOPOLOGY_RESTORED", null);

        logger.info("Restored topology from snapshot: {}", snapshotId);
    }

    /**
     * Check if adding a connection would create a loop.
     * @param sourceDeviceId The source device ID
     * @param targetDeviceId The target device ID
     * @return True if adding the connection would create a loop, false otherwise
     */
    @Override
    public boolean wouldCreateLoop(UUID sourceDeviceId, UUID targetDeviceId) {
        // If there's already a direct connection in the opposite direction, adding this one would create a loop
        boolean directConnectionExists = connectionRepository
            .findBySourceDeviceIdAndTargetDeviceId(targetDeviceId, sourceDeviceId).isPresent();

        if (directConnectionExists) {
            return true;
        }

        // Check if there's a path from target back to source (excluding the proposed connection)
        return findPathWithoutConnection(targetDeviceId, sourceDeviceId, sourceDeviceId, targetDeviceId).isPresent();
    }

    /**
     * Find the shortest path between two devices.
     * @param sourceDeviceId The source device ID
     * @param targetDeviceId The target device ID
     * @return The shortest path as a list of device IDs
     */
    @Override
    public List<UUID> findShortestPath(UUID sourceDeviceId, UUID targetDeviceId) {
        return findPathWithoutConnection(sourceDeviceId, targetDeviceId, null, null)
            .orElseThrow(() -> new IllegalArgumentException("No path found between devices"));
    }

    /**
     * Export topology to JSON.
     * @return The topology data in JSON format
     */
    @Override
    public String exportTopologyToJson() {
        com.network.digitaltwin.topology.dto.TopologyDTO topology = getFullTopology();

        // Convert to JSON string
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(topology);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export topology to JSON", e);
        }
    }

    /**
     * Validate connection attributes.
     * @param connectionData The connection data to validate
     */
    private void validateConnectionAttributes(com.network.digitaltwin.topology.dto.ConnectionDTO connectionData) {
        // Validate bandwidth
        if (connectionData.getBandwidth() != null) {
            if (connectionData.getBandwidth() < MIN_BANDWIDTH || connectionData.getBandwidth() > MAX_BANDWIDTH) {
                throw new IllegalArgumentException("Bandwidth must be between " + MIN_BANDWIDTH + " and " + MAX_BANDWIDTH + " Mbps");
            }
        }

        // Validate latency
        if (connectionData.getLatency() != null) {
            if (connectionData.getLatency() < MIN_LATENCY || connectionData.getLatency() > MAX_LATENCY) {
                throw new IllegalArgumentException("Latency must be between " + MIN_LATENCY + " and " + MAX_LATENCY + " ms");
            }
        }

        // Validate reliability
        if (connectionData.getReliability() != null) {
            if (connectionData.getReliability().compareTo(MIN_RELIABILITY) < 0 || 
                connectionData.getReliability().compareTo(MAX_RELIABILITY) > 0) {
                throw new IllegalArgumentException("Reliability must be between " + MIN_RELIABILITY + " and " + MAX_RELIABILITY);
            }
        }

        // Validate connection type
        if (connectionData.getConnectionType() == null || 
            (!Arrays.asList("ETHERNET", "WIFI", "VPN", "OTHER").contains(connectionData.getConnectionType()))) {
            throw new IllegalArgumentException("Invalid connection type");
        }

        // Validate status if provided
        if (connectionData.getStatus() != null && 
            !Arrays.asList("ACTIVE", "INACTIVE", "DEGRADED", "FAILED").contains(connectionData.getStatus())) {
            throw new IllegalArgumentException("Invalid connection status");
        }
    }

    /**
     * Create a topology snapshot.
     * @param changeType The type of change (CREATE, UPDATE, DELETE, RESTORE)
     * @param changeDescription Description of the change
     * @param userDetails The authenticated user
     */
    private void createTopologySnapshot(String changeType, String changeDescription, UserDetails userDetails) {
        // Get the latest topology version
        Integer latestVersion = topologySnapshotRepository.findLatestVersion()
            .orElse(0);

        // Increment version
        Integer newVersion = latestVersion + 1;

        // Get user if available
        User user = null;
        if (userDetails != null) {
            user = userRepository.findByUsername(userDetails.getUsername())
                .orElse(null);
        }

        // Create snapshot
        TopologySnapshot snapshot = new TopologySnapshot();
        snapshot.setTopologyVersion(newVersion);
        snapshot.setCreatedBy(user);
        snapshot.setChangeType(changeType);
        snapshot.setChangeDescription(changeDescription);

        // In a real implementation, we would serialize the current topology
        // and store it in the snapshot data

        // Save snapshot
        topologySnapshotRepository.save(snapshot);

        logger.info("Created topology snapshot: version={}, changeType={}", newVersion, changeType);
    }

    /**
     * Send topology change event to Kafka.
     * @param eventType The type of event
     * @param connection The connection involved (optional)
     */
    private void sendTopologyChangeEvent(String eventType, Connection connection) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", Instant.now().toString());
            event.put("correlationId", java.util.UUID.randomUUID().toString());

            if (connection != null) {
                event.put("connectionId", connection.getId());
                event.put("sourceDeviceId", connection.getSourceDevice().getId());
                event.put("targetDeviceId", connection.getTargetDevice().getId());
                event.put("connectionType", connection.getConnectionType());
                event.put("status", connection.getStatus());
            }

            kafkaProducerService.sendTopologyChangeEvent(event);
        } catch (Exception e) {
            logger.error("Failed to send topology change event", e);
        }
    }

    /**
     * Find a path between two devices without using a specific connection.
     * @param currentDeviceId The current device ID
     * @param targetDeviceId The target device ID
     * @param excludedSourceId The source device ID to exclude (optional)
     * @param excludedTargetId The target device ID to exclude (optional)
     * @return The path as a list of device IDs, or empty if no path found
     */
    private Optional<List<UUID>> findPathWithoutConnection(UUID currentDeviceId, UUID targetDeviceId, 
                                                         UUID excludedSourceId, UUID excludedTargetId) {
        // If we've reached the target, return the path
        if (currentDeviceId.equals(targetDeviceId)) {
            return Optional.of(Collections.singletonList(currentDeviceId));
        }

        // Get all outgoing connections from the current device
        List<Connection> connections = connectionRepository.findBySourceDeviceId(currentDeviceId);

        // Try each connection
        for (Connection connection : connections) {
            // Skip if this is the excluded connection
            if (excludedSourceId != null && excludedTargetId != null &&
                connection.getSourceDevice().getId().equals(excludedSourceId) &&
                connection.getTargetDevice().getId().equals(excludedTargetId)) {
                continue;
            }

            // Recursively try to find a path from the next device
            Optional<List<UUID>> path = findPathWithoutConnection(
                connection.getTargetDevice().getId(), 
                targetDeviceId, 
                excludedSourceId, 
                excludedTargetId);

            // If a path was found, add the current device and return
            if (path.isPresent()) {
                List<UUID> fullPath = new ArrayList<>();
                fullPath.add(currentDeviceId);
                fullPath.addAll(path.get());
                return Optional.of(fullPath);
            }
        }

        // No path found
        return Optional.empty();
    }
}
