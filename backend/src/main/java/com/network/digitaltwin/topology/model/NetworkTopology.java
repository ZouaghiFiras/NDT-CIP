package com.network.digitaltwin.topology.model;

import com.network.digitaltwin.model.Device;
import com.network.digitaltwin.model.Connection;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the network topology as a directed weighted graph.
 * Uses JGraphT for graph operations and analysis.
 */
@Component
public class NetworkTopology {

    // The underlying graph structure
    private final Graph<Device, DefaultEdge> graph;

    // Map of devices by their ID for quick lookup
    private final Map<UUID, Device> devicesMap;

    // Map of connections by their ID for quick lookup
    private final Map<UUID, Connection> connectionsMap;

    /**
     * Creates a new empty network topology.
     */
    public NetworkTopology() {
        this.graph = new DefaultDirectedWeightedGraph<>(DefaultEdge.class);
        this.devicesMap = new HashMap<>();
        this.connectionsMap = new HashMap<>();
    }

    /**
     * Adds a device to the topology.
     * @param device The device to add
     * @return true if the device was added, false if it already exists
     */
    public boolean addDevice(Device device) {
        if (devicesMap.containsKey(device.getId())) {
            return false;
        }

        graph.addVertex(device);
        devicesMap.put(device.getId(), device);
        return true;
    }

    /**
     * Removes a device from the topology.
     * @param deviceId The ID of the device to remove
     * @return true if the device was removed, false if it wasn't found
     */
    public boolean removeDevice(UUID deviceId) {
        Device device = devicesMap.get(deviceId);
        if (device == null) {
            return false;
        }

        graph.removeVertex(device);
        devicesMap.remove(deviceId);

        // Also remove any connections that involve this device
        connectionsMap.entrySet().removeIf(entry -> 
            entry.getValue().getFromDevice().getId().equals(deviceId) || 
            entry.getValue().getToDevice().getId().equals(deviceId)
        );

        return true;
    }

    /**
     * Adds a connection between two devices.
     * @param connection The connection to add
     * @return true if the connection was added, false if the devices don't exist or connection already exists
     */
    public boolean addConnection(Connection connection) {
        if (!devicesMap.containsKey(connection.getFromDevice().getId()) || 
            !devicesMap.containsKey(connection.getToDevice().getId())) {
            return false;
        }

        // Check if connection already exists
        if (connectionsMap.containsKey(connection.getId())) {
            return false;
        }

        DefaultEdge edge = graph.addEdge(connection.getFromDevice(), connection.getToDevice());
        if (edge != null) {
            // Set the weight based on reliability (higher reliability = higher weight)
            double weight = connection.getReliability() != null ? connection.getReliability() : 1.0;
            graph.setEdgeWeight(edge, weight);

            connectionsMap.put(connection.getId(), connection);
            return true;
        }

        return false;
    }

    /**
     * Removes a connection from the topology.
     * @param connectionId The ID of the connection to remove
     * @return true if the connection was removed, false if it wasn't found
     */
    public boolean removeConnection(UUID connectionId) {
        Connection connection = connectionsMap.get(connectionId);
        if (connection == null) {
            return false;
        }

        Device fromDevice = connection.getFromDevice();
        Device toDevice = connection.getToDevice();

        graph.removeEdge(fromDevice, toDevice);
        connectionsMap.remove(connectionId);
        return true;
    }

    /**
     * Gets a device by its ID.
     * @param deviceId The ID of the device to get
     * @return The device, or null if not found
     */
    public Device getDevice(UUID deviceId) {
        return devicesMap.get(deviceId);
    }

    /**
     * Gets a connection by its ID.
     * @param connectionId The ID of the connection to get
     * @return The connection, or null if not found
     */
    public Connection getConnection(UUID connectionId) {
        return connectionsMap.get(connectionId);
    }

    /**
     * Gets all devices in the topology.
     * @return A collection of all devices
     */
    public Collection<Device> getAllDevices() {
        return devicesMap.values();
    }

    /**
     * Gets all connections in the topology.
     * @return A collection of all connections
     */
    public Collection<Connection> getAllConnections() {
        return connectionsMap.values();
    }

    /**
     * Gets all devices connected to a specific device.
     * @param deviceId The ID of the device
     * @return A set of connected devices
     */
    public Set<Device> getConnectedDevices(UUID deviceId) {
        Device device = devicesMap.get(deviceId);
        if (device == null) {
            return Collections.emptySet();
        }

        return Graphs.neighborListOf(graph, device).stream()
            .collect(Collectors.toSet());
    }

    /**
     * Gets all incoming connections to a device.
     * @param deviceId The ID of the device
     * @return A list of incoming connections
     */
    public List<Connection> getIncomingConnections(UUID deviceId) {
        Device device = devicesMap.get(deviceId);
        if (device == null) {
            return Collections.emptyList();
        }

        return graph.incomingEdgesOf(device).stream()
            .map(edge -> connectionsMap.values().stream()
                .filter(conn -> conn.getFromDevice().equals(graph.getEdgeSource(edge)) && 
                               conn.getToDevice().equals(device))
                .findFirst()
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Gets all outgoing connections from a device.
     * @param deviceId The ID of the device
     * @return A list of outgoing connections
     */
    public List<Connection> getOutgoingConnections(UUID deviceId) {
        Device device = devicesMap.get(deviceId);
        if (device == null) {
            return Collections.emptyList();
        }

        return graph.outgoingEdgesOf(device).stream()
            .map(edge -> connectionsMap.values().stream()
                .filter(conn -> conn.getFromDevice().equals(device) && 
                               conn.getToDevice().equals(graph.getEdgeTarget(edge)))
                .findFirst()
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Finds the shortest path between two devices.
     * @param fromDeviceId The ID of the source device
     * @param toDeviceId The ID of the target device
     * @return A list of devices representing the path, or empty if no path exists
     */
    public List<Device> findShortestPath(UUID fromDeviceId, UUID toDeviceId) {
        Device fromDevice = devicesMap.get(fromDeviceId);
        Device toDevice = devicesMap.get(toDeviceId);

        if (fromDevice == null || toDevice == null) {
            return Collections.emptyList();
        }

        DijkstraShortestPath<Device, DefaultEdge> dijkstra = 
            new DijkstraShortestPath<>(graph);

        GraphPath<Device, DefaultEdge> path = dijkstra.getPath(fromDevice, toDevice);

        return path != null ? 
            path.getVertexList().stream().collect(Collectors.toList()) : 
            Collections.emptyList();
    }

    /**
     * Finds all devices reachable from a source device.
     * @param fromDeviceId The ID of the source device
     * @return A set of reachable devices
     */
    public Set<Device> findReachableDevices(UUID fromDeviceId) {
        Device fromDevice = devicesMap.get(fromDeviceId);
        if (fromDevice == null) {
            return Collections.emptySet();
        }

        return Graphs.reachableVertices(graph, fromDevice);
    }

    /**
     * Checks if a device is reachable from another device.
     * @param fromDeviceId The ID of the source device
     * @param toDeviceId The ID of the target device
     * @return true if the target device is reachable from the source device
     */
    public boolean isDeviceReachable(UUID fromDeviceId, UUID toDeviceId) {
        Device fromDevice = devicesMap.get(fromDeviceId);
        Device toDevice = devicesMap.get(toDeviceId);

        if (fromDevice == null || toDevice == null) {
            return false;
        }

        return Graphs.reachableVertices(graph, fromDevice).contains(toDevice);
    }

    /**
     * Gets the number of hops (edges) between two devices.
     * @param fromDeviceId The ID of the source device
     * @param toDeviceId The ID of the target device
     * @return The number of hops, or -1 if no path exists
     */
    public int getHopsBetweenDevices(UUID fromDeviceId, UUID toDeviceId) {
        List<Device> path = findShortestPath(fromDeviceId, toDeviceId);
        return path.isEmpty() ? -1 : path.size() - 1;
    }

    /**
     * Gets the total number of devices in the topology.
     * @return The number of devices
     */
    public int getDeviceCount() {
        return graph.vertexSet().size();
    }

    /**
     * Gets the total number of connections in the topology.
     * @return The number of connections
     */
    public int getConnectionCount() {
        return graph.edgeSet().size();
    }

    /**
     * Exports the topology as a JSON object.
     * @return A map representing the topology
     */
    public Map<String, Object> exportAsJson() {
        Map<String, Object> topology = new HashMap<>();

        // Export devices
        List<Map<String, Object>> devices = devicesMap.values().stream()
            .map(this::deviceToJson)
            .collect(Collectors.toList());

        // Export connections
        List<Map<String, Object>> connections = connectionsMap.values().stream()
            .map(this::connectionToJson)
            .collect(Collectors.toList());

        topology.put("devices", devices);
        topology.put("connections", connections);
        topology.put("deviceCount", getDeviceCount());
        topology.put("connectionCount", getConnectionCount());

        return topology;
    }

    /**
     * Converts a device to a JSON-serializable map.
     * @param device The device to convert
     * @return A map representing the device
     */
    private Map<String, Object> deviceToJson(Device device) {
        Map<String, Object> json = new HashMap<>();
        json.put("id", device.getId());
        json.put("name", device.getName());
        json.put("ipAddress", device.getIpAddress());
        json.put("type", device.getType());
        json.put("os", device.getOs());
        json.put("criticality", device.getCriticality());
        json.put("status", device.getStatus());
        json.put("riskScore", device.getRiskScore());
        json.put("metadata", device.getMetadata());
        json.put("lastSeen", device.getLastSeen());
        return json;
    }

    /**
     * Converts a connection to a JSON-serializable map.
     * @param connection The connection to convert
     * @return A map representing the connection
     */
    private Map<String, Object> connectionToJson(Connection connection) {
        Map<String, Object> json = new HashMap<>();
        json.put("id", connection.getId());
        json.put("fromDeviceId", connection.getFromDevice().getId());
        json.put("toDeviceId", connection.getToDevice().getId());
        json.put("connectionType", connection.getConnectionType());
        json.put("bandwidth", connection.getBandwidth());
        json.put("latency", connection.getLatency());
        json.put("reliability", connection.getReliability());
        json.put("status", connection.getStatus());
        json.put("properties", connection.getProperties());
        return json;
    }
}
