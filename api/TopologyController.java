package com.network.digitaltwin.api;

import com.network.digitaltwin.model.Device;
import com.network.digitaltwin.model.Connection;
import com.network.digitaltwin.topology.model.NetworkTopology;
import com.network.digitaltwin.topology.service.TopologyService;
import com.network.digitaltwin.simulation.engine.SimulationEngine;
import com.network.digitaltwin.simulation.model.AttackScenario;
import com.network.digitaltwin.simulation.model.FailureScenario;
import com.network.digitaltwin.simulation.model.SimulationResult;
import com.network.digitaltwin.simulation.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for network topology operations.
 */
@RestController
@RequestMapping("/api/topology")
public class TopologyController {

    @Autowired
    private TopologyService topologyService;

    @Autowired
    private SimulationEngine simulationEngine;

    @Autowired
    private SimulationService simulationService;

    /**
     * Upload or create a network topology.
     * @param file The topology file (JSON format)
     * @param userDetails The authenticated user
     * @return Response containing the created topology
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadTopology(@RequestParam("file") MultipartFile file, 
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            NetworkTopology topology = topologyService.uploadTopology(file, userDetails);
            return ResponseEntity.ok(topology);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to upload topology: " + e.getMessage()
            ));
        }
    }

    /**
     * Retrieve the current network topology.
     * @return Response containing the current topology
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getCurrentTopology() {
        try {
            NetworkTopology topology = topologyService.getCurrentTopology();
            return ResponseEntity.ok(topology);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve topology: " + e.getMessage()
            ));
        }
    }

    /**
     * Add a device to the topology.
     * @param device The device to add
     * @param userDetails The authenticated user
     * @return Response containing the added device
     */
    @PostMapping("/devices")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> addDevice(@RequestBody Device device, 
                                     @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Device addedDevice = topologyService.addDevice(device, userDetails);
            return ResponseEntity.ok(addedDevice);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to add device: " + e.getMessage()
            ));
        }
    }

    /**
     * Remove a device from the topology.
     * @param deviceId The ID of the device to remove
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @DeleteMapping("/devices/{deviceId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> removeDevice(@PathVariable UUID deviceId, 
                                        @AuthenticationPrincipal UserDetails userDetails) {
        try {
            topologyService.removeDevice(deviceId, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Device removed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to remove device: " + e.getMessage()
            ));
        }
    }

    /**
     * Add a connection to the topology.
     * @param connection The connection to add
     * @param userDetails The authenticated user
     * @return Response containing the added connection
     */
    @PostMapping("/connections")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> addConnection(@RequestBody Connection connection, 
                                         @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Connection addedConnection = topologyService.addConnection(connection, userDetails);
            return ResponseEntity.ok(addedConnection);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to add connection: " + e.getMessage()
            ));
        }
    }

    /**
     * Remove a connection from the topology.
     * @param connectionId The ID of the connection to remove
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @DeleteMapping("/connections/{connectionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> removeConnection(@PathVariable UUID connectionId, 
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            topologyService.removeConnection(connectionId, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Connection removed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to remove connection: " + e.getMessage()
            ));
        }
    }

    /**
     * Run an attack simulation.
     * @param scenario The attack scenario to simulate
     * @param userDetails The authenticated user
     * @return Response containing the simulation ID
     */
    @PostMapping("/simulation/attack")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> runAttackSimulation(@RequestBody AttackScenario scenario, 
                                              @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID simulationId = simulationService.runAttackSimulation(scenario, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "simulationId", simulationId,
                "message", "Attack simulation started"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to start attack simulation: " + e.getMessage()
            ));
        }
    }

    /**
     * Run a failure simulation.
     * @param scenario The failure scenario to simulate
     * @param userDetails The authenticated user
     * @return Response containing the simulation ID
     */
    @PostMapping("/simulation/failure")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> runFailureSimulation(@RequestBody FailureScenario scenario, 
                                               @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID simulationId = simulationService.runFailureSimulation(scenario, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "simulationId", simulationId,
                "message", "Failure simulation started"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to start failure simulation: " + e.getMessage()
            ));
        }
    }

    /**
     * Fetch simulation results.
     * @param simulationId The ID of the simulation
     * @return Response containing the simulation results
     */
    @GetMapping("/simulation/results/{simulationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSimulationResults(@PathVariable UUID simulationId) {
        try {
            SimulationResult result = simulationService.getSimulationResults(simulationId);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve simulation results: " + e.getMessage()
            ));
        }
    }

    /**
     * Get simulation status.
     * @param simulationId The ID of the simulation
     * @return Response containing the simulation status
     */
    @GetMapping("/simulation/status/{simulationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSimulationStatus(@PathVariable UUID simulationId) {
        try {
            String status = simulationService.getSimulationStatus(simulationId);
            if (status == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "simulationId", simulationId,
                "status", status
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve simulation status: " + e.getMessage()
            ));
        }
    }

    /**
     * Cancel a running simulation.
     * @param simulationId The ID of the simulation to cancel
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @PostMapping("/simulation/{simulationId}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> cancelSimulation(@PathVariable UUID simulationId, 
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            simulationService.cancelSimulation(simulationId, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "simulationId", simulationId,
                "message", "Simulation cancelled"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to cancel simulation: " + e.getMessage()
            ));
        }
    }
}
