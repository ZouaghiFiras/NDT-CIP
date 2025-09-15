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
 * Service for network topology management operations.
 */
@Service
public class TopologyService {

    private static final Logger logger = LoggerFactory.getLogger(TopologyService.class);

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
    @Transactional
    public void restoreTopologySnapshot(UUID snapshotId, UserDetails userDetails) {
        logger.info("Restoring topology from snapshot: {}", snapshotId);

        // Get snapshot
        TopologySnapshot snapshot = topologySnapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        // Clear existing connections
        connectionRepository.deleteAll();

        // Restore connections from snapshot data
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> connections = (List<Map<String, Object>>) snapshot.getSnapshotData().get("connections");

        for (Map<String, Object> connectionData : connections) {
            UUID sourceDeviceId = UUID.fromString((String) connectionData.get("sourceDeviceId"));
            UUID targetDeviceId = UUID.fromString((String) connectionData.get("targetDeviceId"));

            Device sourceDevice = deviceRepository.findById(sourceDeviceId)
                .orElseThrow(() -> new IllegalArgumentException("Source device not found in snapshot: " + sourceDeviceId));

            Device targetDevice = deviceRepository.findById(targetDeviceId)
                .orElseThrow(() -> new IllegalArgumentException("Target device not found in snapshot: " + targetDeviceId));

            Connection connection = new Connection();
            connection.setSourceDevice(sourceDevice);
            connection.setTargetDevice(targetDevice);
            connection.setConnectionType((String) connectionData.get("connectionType"));
            connection.setBandwidth(((Number) connectionData.get("bandwidth")).longValue());
            connection.setLatency(((Number) connectionData.get("latency")).longValue());
            connection.setReliability(new BigDecimal(connectionData.get("reliability").toString()));
            connection.setStatus((String) connectionData.get("status"));

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) connectionData.get("metadata");
            if (metadata != null) {
                metadata.forEach(connection::addMetadata);
            }

            connectionRepository.save(connection);
        }

        // Create topology snapshot
        createTopologySnapshot("RESTORE", "Restored topology from snapshot: " + snapshotId, userDetails);

        // Send event to Kafka
        sendTopologyChangeEvent("TOPOLOGY_RESTORED", null);

        logger.info("Restored topology from snapshot: {}", snapshotId);
    }

    /**
     * Check if adding a connection would create a loop.
     * @param sourceDeviceId The source device ID
     * @param targetDeviceId The target device ID
     * @return true if adding the connection would create a loop, false otherwise
     */
    public boolean wouldCreateLoop(UUID sourceDeviceId, UUID targetDeviceId) {
        // If the devices are the same, it's a self-loop
        if (sourceDeviceId.equals(targetDeviceId)) {
            return true;
        }

        // Get all connections
        List<Connection> connections = connectionRepository.findAll();

        // Build adjacency list
        Map<UUID, List<UUID>> adjacencyList = new HashMap<>();
        for (Connection connection : connections) {
            adjacencyList.computeIfAbsent(connection.getSourceDevice().getId(), k -> new ArrayList<>())
                       .add(connection.getTargetDevice().getId());
        }

        // Perform DFS to check for path from target to source
        Set<UUID> visited = new HashSet<>();
        Stack<UUID> stack = new Stack<>();
        stack.push(targetDeviceId);

        while (!stack.isEmpty()) {
            UUID current = stack.pop();

            if (current.equals(sourceDeviceId)) {
                return true; // Path found, would create a loop
            }

            if (!visited.contains(current)) {
                visited.add(current);

                List<UUID> neighbors = adjacencyList.getOrDefault(current, Collections.emptyList());
                for (UUID neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
        }

        return false; // No path found, no loop would be created
    }

    /**
     * Find the shortest path between two devices using BFS.
     * @param sourceDeviceId The source device ID
     * @param targetDeviceId The target device ID
     * @return The shortest path as a list of device IDs
     */
    public List<UUID> findShortestPath(UUID sourceDeviceId, UUID targetDeviceId) {
        // Get all connections
        List<Connection> connections = connectionRepository.findAll();

        // Build adjacency list
        Map<UUID, List<UUID>> adjacencyList = new HashMap<>();
        for (Connection connection : connections) {
            if ("ACTIVE".equals(connection.getStatus())) {
                adjacencyList.computeIfAbsent(connection.getSourceDevice().getId(), k -> new ArrayList<>())
                           .add(connection.getTargetDevice().getId());
            }
        }

        // BFS to find shortest path
        Queue<List<UUID>> queue = new LinkedList<>();
        queue.add(Arrays.asList(sourceDeviceId));

        Set<UUID> visited = new HashSet<>();
        visited.add(sourceDeviceId);

        while (!queue.isEmpty()) {
            List<UUID> path = queue.poll();
            UUID current = path.get(path.size() - 1);

            if (current.equals(targetDeviceId)) {
                return path; // Path found
            }

            List<UUID> neighbors = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (UUID neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    List<UUID> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    /**
     * Export topology to JSON.
     * @return The topology data in JSON format
     */
    public String exportTopologyToJson() {
        com.network.digitaltwin.topology.dto.TopologyDTO topology = getFullTopology();
        return topology.toJson();
    }

    /**
     * Validate connection attributes.
     * @param connectionData The connection data to validate
     */
    private void validateConnectionAttributes(com.network.digitaltwin.topology.dto.ConnectionDTO connectionData) {
        // Validate bandwidth
        if (connectionData.getBandwidth() != null && connectionData.getBandwidth() <= 0) {
            throw new IllegalArgumentException("Bandwidth must be greater than 0");
        }

        // Validate latency
        if (connectionData.getLatency() != null && connectionData.getLatency() < 0) {
            throw new IllegalArgumentException("Latency must be greater than or equal to 0");
        }

        // Validate reliability
        if (connectionData.getReliability() != null) {
            BigDecimal reliability = connectionData.getReliability();
            if (reliability.compareTo(BigDecimal.ZERO) < 0 || reliability.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Reliability must be between 0 and 1");
            }
        }

        // Validate connection type
        if (connectionData.getConnectionType() == null || 
            !Arrays.asList("ETHERNET", "WIFI", "VPN", "OTHER").contains(connectionData.getConnectionType())) {
            throw new IllegalArgumentException("Invalid connection type");
        }

        // Validate status
        if (connectionData.getStatus() != null && 
            !Arrays.asList("ACTIVE", "INACTIVE", "DEGRADED", "FAILED").contains(connectionData.getStatus())) {
            throw new IllegalArgumentException("Invalid connection status");
        }
    }

    /**
     * Create a topology snapshot.
     * @param changeType Type of change (CREATE, UPDATE, DELETE, RESTORE)
     * @param changeDescription Description of the change
     * @param userDetails The authenticated user
     */
    @Transactional
    private void createTopologySnapshot(String changeType, String changeDescription, UserDetails userDetails) {
        // Get current topology version
        Integer currentVersion = topologySnapshotRepository.findTopByOrderByTopologyVersionDesc()
            .map(snapshot -> snapshot.getTopologyVersion() + 1)
            .orElse(1);

        // Get user if available
        User user = null;
        if (userDetails != null) {
            user = userRepository.findByUsername(userDetails.getUsername())
                .orElse(null);
        }

        // Create snapshot
        TopologySnapshot snapshot = new TopologySnapshot();
        snapshot.setTopologyVersion(currentVersion);
        snapshot.setCreatedBy(user);
        snapshot.setChangeType(changeType);
        snapshot.setChangeDescription(changeDescription);

        // Add topology data
        com.network.digitaltwin.topology.dto.TopologyDTO topology = getFullTopology();
        snapshot.getSnapshotData().put("topologyVersion", currentVersion);
        snapshot.getSnapshotData().put("connections", topology.getEdges().stream()
            .map(edge -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", edge.getId());
                data.put("sourceDeviceId", edge.getSourceDeviceId());
                data.put("targetDeviceId", edge.getTargetDeviceId());
                data.put("connectionType", edge.getType());
                data.put("bandwidth", edge.getBandwidth());
                data.put("latency", edge.getLatency());
                data.put("reliability", edge.getReliability());
                data.put("status", edge.getStatus());
                data.put("metadata", edge.getMetadata());
                return data;
            })
            .collect(Collectors.toList()));

        // Save snapshot
        topologySnapshotRepository.save(snapshot);

        logger.info("Created topology snapshot: version={}, changeType={}", currentVersion, changeType);
    }

    /**
     * Send topology change event to Kafka.
     * @param eventType Type of event
     * @param connection The connection involved (optional)
     */
    private void sendTopologyChangeEvent(String eventType, Connection connection) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", Instant.now());

            if (connection != null) {
                Map<String, Object> connectionData = new HashMap<>();
                connectionData.put("id", connection.getId());
                connectionData.put("sourceDeviceId", connection.getSourceDevice().getId());
                connectionData.put("targetDeviceId", connection.getTargetDevice().getId());
                connectionData.put("connectionType", connection.getConnectionType());
                connectionData.put("bandwidth", connection.getBandwidth());
                connectionData.put("latency", connection.getLatency());
                connectionData.put("reliability", connection.getReliability());
                connectionData.put("status", connection.getStatus());
                event.put("connection", connectionData);
            }

            kafkaProducerService.sendTopologyEvent(event);

        } catch (Exception e) {
            logger.error("Error sending topology change event to Kafka", e);
        }
    }
}
