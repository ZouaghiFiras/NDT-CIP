package com.network.digitaltwin.api;

import com.network.digitaltwin.model.auth.User;
import com.network.digitaltwin.simulation.model.SimulationScenario;
import com.network.digitaltwin.simulation.service.SimulationService;
import com.network.digitaltwin.simulation.model.SimulationType;
import com.network.digitaltwin.simulation.model.SimulationStatus;
import com.network.digitaltwin.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * REST controller for simulation scenario management.
 */
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new simulation scenario.
     * @param scenario The scenario data
     * @param userDetails The authenticated user
     * @return The created scenario
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> createSimulation(
            @RequestBody SimulationScenario scenario,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Create scenario
            SimulationScenario createdScenario = simulationService.createSimulationScenario(scenario, user.getId());

            return ResponseEntity.ok(createdScenario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to create simulation scenario: " + e.getMessage()
            ));
        }
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
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSimulationScenarios(
            @RequestParam(required = false) SimulationType type,
            @RequestParam(required = false) SimulationStatus status,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            Pageable pageable) {
        try {
            Page<SimulationScenario> scenarios = simulationService.getSimulationScenarios(
                type, status, createdBy, start, end, pageable);

            return ResponseEntity.ok(scenarios);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve simulation scenarios: " + e.getMessage()
            ));
        }
    }

    /**
     * Get a specific simulation scenario by ID.
     * @param id The ID of the scenario
     * @return The scenario details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSimulationScenario(@PathVariable UUID id) {
        try {
            SimulationScenario scenario = simulationService.getSimulationScenario(id);
            return ResponseEntity.ok(scenario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve simulation scenario: " + e.getMessage()
            ));
        }
    }

    /**
     * Update a simulation scenario.
     * @param id The ID of the scenario to update
     * @param scenarioData The updated scenario data
     * @param userDetails The authenticated user
     * @return The updated scenario
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSimulationScenario(
            @PathVariable UUID id,
            @RequestBody SimulationScenario scenarioData,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Update scenario
            SimulationScenario updatedScenario = simulationService.updateSimulationScenario(id, scenarioData, user.getId());

            return ResponseEntity.ok(updatedScenario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to update simulation scenario: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete a simulation scenario.
     * @param id The ID of the scenario to delete
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSimulationScenario(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Delete scenario
            simulationService.deleteSimulationScenario(id, user.getId());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Simulation scenario deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to delete simulation scenario: " + e.getMessage()
            ));
        }
    }

    /**
     * Execute a simulation scenario.
     * @param id The ID of the scenario to execute
     * @param userDetails The authenticated user
     * @return The execution ID
     */
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> executeSimulationScenario(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Execute scenario asynchronously
            String executionId = simulationService.executeSimulationScenario(id, user.getId()).join();

            return ResponseEntity.ok(Map.of(
                "executionId", executionId,
                "status", "QUEUED"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to execute simulation scenario: " + e.getMessage()
            ));
        }
    }

    /**
     * Cancel a running simulation scenario.
     * @param id The ID of the scenario to cancel
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelSimulationScenario(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Cancel scenario
            simulationService.cancelSimulationScenario(id, user.getId());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Simulation scenario cancelled successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to cancel simulation scenario: " + e.getMessage()
            ));
        }
    }

    /**
     * Get simulation scenarios by type.
     * @param type The scenario type
     * @param pageable Pagination information
     * @return Page of simulation scenarios
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSimulationScenariosByType(
            @PathVariable SimulationType type,
            Pageable pageable) {
        try {
            Page<SimulationScenario> scenarios = simulationService.getSimulationScenariosByType(type, pageable);
            return ResponseEntity.ok(scenarios);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve simulation scenarios: " + e.getMessage()
            ));
        }
    }

    /**
     * Get simulation scenarios by status.
     * @param status The scenario status
     * @param pageable Pagination information
     * @return Page of simulation scenarios
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSimulationScenariosByStatus(
            @PathVariable SimulationStatus status,
            Pageable pageable) {
        try {
            Page<SimulationScenario> scenarios = simulationService.getSimulationScenariosByStatus(status, pageable);
            return ResponseEntity.ok(scenarios);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve simulation scenarios: " + e.getMessage()
            ));
        }
    }

    /**
     * Get simulation scenarios created by a specific user.
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of simulation scenarios
     */
    @GetMapping("/created-by/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSimulationScenariosByUser(
            @PathVariable UUID userId,
            Pageable pageable) {
        try {
            Page<SimulationScenario> scenarios = simulationService.getSimulationScenariosByUser(userId, pageable);
            return ResponseEntity.ok(scenarios);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve simulation scenarios: " + e.getMessage()
            ));
        }
    }

    /**
     * Export simulation scenarios to CSV.
     * @param type Filter by type (optional)
     * @param status Filter by status (optional)
     * @param createdBy Filter by creator (optional)
     * @param start Start date (optional)
     * @param end End date (optional)
     * @return CSV data
     */
    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> exportToCsv(
            @RequestParam(required = false) SimulationType type,
            @RequestParam(required = false) SimulationStatus status,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        try {
            String csvData = simulationService.exportToCsv(type, status, createdBy, start, end);
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename="simulation_scenarios.csv"")
                .header("Content-Type", "text/csv")
                .body(csvData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to export simulation scenarios: " + e.getMessage()
            ));
        }
    }
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
