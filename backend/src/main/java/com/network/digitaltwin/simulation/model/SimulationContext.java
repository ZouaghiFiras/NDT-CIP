package com.network.digitaltwin.simulation.model;

import com.network.digitaltwin.model.Simulation;
import com.network.digitaltwin.model.User;
import com.network.digitaltwin.topology.model.NetworkTopology;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the context for a simulation, including the simulation itself,
 * the topology being simulated, and the current state of the simulation.
 */
public class SimulationContext {

    private UUID id;
    private Simulation simulation;
    private NetworkTopology topology;
    private User user;
    private SimulationResult result;
    private Map<UUID, Object> state;
    private Instant startTime;
    private Instant endTime;
    private boolean cancelled;

    /**
     * Default constructor.
     */
    public SimulationContext() {
        this.id = UUID.randomUUID();
        this.state = new ConcurrentHashMap<>();
        this.startTime = null;
        this.endTime = null;
        this.cancelled = false;
    }

    /**
     * Constructor with simulation, topology, and user.
     * @param simulation The simulation
     * @param topology The network topology
     * @param user The user running the simulation
     */
    public SimulationContext(Simulation simulation, NetworkTopology topology, User user) {
        this();
        this.simulation = simulation;
        this.topology = topology;
        this.user = user;
        this.startTime = java.time.Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public NetworkTopology getTopology() {
        return topology;
    }

    public void setTopology(NetworkTopology topology) {
        this.topology = topology;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SimulationResult getResult() {
        return result;
    }

    public void setResult(SimulationResult result) {
        this.result = result;
    }

    public Map<UUID, Object> getState() {
        return state;
    }

    public void setState(Map<UUID, Object> state) {
        this.state = state;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    // Convenience Methods
    public void addState(UUID key, Object value) {
        if (this.state == null) {
            this.state = new ConcurrentHashMap<>();
        }
        this.state.put(key, value);
    }

    public Object getState(UUID key) {
        if (this.state == null) {
            return null;
        }
        return this.state.get(key);
    }

    public void removeState(UUID key) {
        if (this.state != null) {
            this.state.remove(key);
        }
    }

    public boolean hasState(UUID key) {
        return this.state != null && this.state.containsKey(key);
    }

    public void clearState() {
        if (this.state != null) {
            this.state.clear();
        }
    }

    public void markAsCancelled() {
        this.cancelled = true;
        this.endTime = java.time.Instant.now();
    }

    public void markAsCompleted() {
        this.endTime = java.time.Instant.now();
    }

    public long getDuration() {
        if (startTime == null) {
            return 0;
        }

        Instant end = endTime != null ? endTime : java.time.Instant.now();
        return end.toEpochMilli() - startTime.toEpochMilli();
    }

    public boolean isRunning() {
        return startTime != null && endTime == null && !cancelled;
    }

    public boolean isCompleted() {
        return startTime != null && endTime != null && !cancelled;
    }

    public boolean isFailed() {
        return result != null && "FAILED".equals(result.getStatus());
    }

    public boolean hasResult() {
        return result != null;
    }
}
