package com.network.digitaltwin.api;

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

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for simulation operations.
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;

    /**
     * Run an attack simulation.
     * @param scenario The attack scenario to simulate
     * @param userDetails The authenticated user
     * @return Response containing the simulation ID
     */
    @PostMapping("/attack")
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
    @PostMapping("/failure")
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
     * Run a Monte Carlo simulation for probabilistic risk assessment.
     * @param scenarioId The ID of the scenario to simulate
     * @param iterations The number of iterations to run
     * @param userDetails The authenticated user
     * @return Response containing the simulation ID
     */
    @PostMapping("/monte-carlo")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> runMonteCarloSimulation(@RequestParam UUID scenarioId, 
                                                   @RequestParam(defaultValue = "1000") int iterations,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID simulationId = simulationService.runMonteCarloSimulation(scenarioId, iterations, userDetails);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "simulationId", simulationId,
                "message", "Monte Carlo simulation started"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to start Monte Carlo simulation: " + e.getMessage()
            ));
        }
    }

    /**
     * Fetch simulation results.
     * @param simulationId The ID of the simulation
     * @return Response containing the simulation results
     */
    @GetMapping("/results/{simulationId}")
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
    @GetMapping("/status/{simulationId}")
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
    @PostMapping("/{simulationId}/cancel")
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

    /**
     * Get simulation history.
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Sort criteria (e.g., "name,asc")
     * @param userDetails The authenticated user
     * @return Response containing paginated simulation history
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSimulationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            var result = simulationService.getSimulationHistory(page, size, sort, userDetails);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve simulation history: " + e.getMessage()
            ));
        }
    }

    /**
     * Export simulation results.
     * @param simulationId The ID of the simulation
     * @param format Export format (json, csv, pdf)
     * @return Response containing the exported data or file
     */
    @GetMapping("/results/{simulationId}/export")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> exportSimulationResults(@PathVariable UUID simulationId, 
                                                  @RequestParam(defaultValue = "json") String format) {
        try {
            byte[] exportedData = simulationService.exportSimulationResults(simulationId, format);
            if (exportedData == null) {
                return ResponseEntity.notFound().build();
            }

            String contentType = switch (format.toLowerCase()) {
                case "json" -> "application/json";
                case "csv" -> "text/csv";
                case "pdf" -> "application/pdf";
                default -> "application/octet-stream";
            };

            String filename = "simulation-results-" + simulationId + "." + format.toLowerCase();

            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header("Content-Disposition", "attachment; filename="" + filename + """)
                .body(exportedData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to export simulation results: " + e.getMessage()
            ));
        }
    }
}
