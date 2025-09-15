package com.network.digitaltwin.api;

import com.network.digitaltwin.model.auth.User;
import com.network.digitaltwin.topology.model.Alert;
import com.network.digitaltwin.topology.model.ValidationRun;
import com.network.digitaltwin.topology.model.ValidationRule;
import com.network.digitaltwin.topology.service.AlertService;
import com.network.digitaltwin.topology.service.ValidationService;
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
 * REST controller for topology validation operations.
 */
@RestController
@RequestMapping("/api/topology/validate")
public class ValidationController {

    @Autowired
    private ValidationService validationService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Run topology validation.
     * @param userDetails The authenticated user
     * @return The validation run ID
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> runValidation(
            @AuthenticationPrincipal UserDetails userDetails) {

        // Get user
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Run validation asynchronously
        String runId = validationService.runValidation("MANUAL", user.getId()).join();

        return ResponseEntity.ok(Map.of(
            "runId", runId,
            "status", "QUEUED"
        ));
    }

    /**
     * Get validation results by run ID.
     * @param runId The validation run ID
     * @return The validation run details
     */
    @GetMapping("/{runId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getValidationResults(@PathVariable String runId) {
        try {
            ValidationRun validationRun = validationService.getValidationRun(runId);

            Map<String, Object> response = new HashMap<>();
            response.put("runId", validationRun.getRunId());
            response.put("status", validationRun.getStatus());
            response.put("triggeredBy", validationRun.getTriggeredBy());
            response.put("triggeredAt", validationRun.getTriggeredAt());
            response.put("startedAt", validationRun.getStartedAt());
            response.put("completedAt", validationRun.getCompletedAt());
            response.put("durationMs", validationRun.getDurationMs());
            response.put("executedRules", validationRun.getExecutedRules());
            response.put("ruleResults", validationRun.getRuleResults());
            response.put("alertCount", validationRun.getAlertIds().size());
            response.put("errorMessage", validationRun.getErrorMessage());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve validation results: " + e.getMessage()
            ));
        }
    }

    /**
     * Get alerts with filtering and pagination.
     * @param severity Filter by severity (optional)
     * @param alertType Filter by alert type (optional)
     * @param resolved Filter by resolved status (optional)
     * @param deviceId Filter by affected device ID (optional)
     * @param connectionId Filter by affected connection ID (optional)
     * @param start Start date (optional)
     * @param end End date (optional)
     * @param pageable Pagination information
     * @return Page of alerts
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAlerts(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) UUID deviceId,
            @RequestParam(required = false) UUID connectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            Pageable pageable) {

        try {
            Page<Alert> alerts = alertService.getAlerts(
                severity, alertType, resolved, deviceId, connectionId, start, end, pageable);

            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve alerts: " + e.getMessage()
            ));
        }
    }

    /**
     * Resolve an alert.
     * @param alertId The ID of the alert to resolve
     * @param resolutionNotes Notes about the resolution
     * @param userDetails The authenticated user
     * @return The resolved alert
     */
    @PostMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolveAlert(
            @PathVariable UUID alertId,
            @RequestBody Map<String, String> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String resolutionNotes = requestBody.getOrDefault("resolutionNotes", "");

            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Alert alert = alertService.resolveAlert(alertId, user.getId(), resolutionNotes);

            return ResponseEntity.ok(alert);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to resolve alert: " + e.getMessage()
            ));
        }
    }

    /**
     * Unresolve an alert.
     * @param alertId The ID of the alert to unresolve
     * @param userDetails The authenticated user
     * @return The unresolved alert
     */
    @PostMapping("/alerts/{alertId}/unresolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unresolveAlert(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Alert alert = alertService.unresolveAlert(alertId, user.getId());

            return ResponseEntity.ok(alert);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to unresolve alert: " + e.getMessage()
            ));
        }
    }

    /**
     * Get unresolved alerts.
     * @param pageable Pagination information
     * @return Page of unresolved alerts
     */
    @GetMapping("/alerts/unresolved")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getUnresolvedAlerts(Pageable pageable) {
        try {
            Page<Alert> alerts = alertService.getUnresolvedAlerts(pageable);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve unresolved alerts: " + e.getMessage()
            ));
        }
    }

    /**
     * Get alerts by severity.
     * @param severity The severity level
     * @param pageable Pagination information
     * @return Page of alerts
     */
    @GetMapping("/alerts/severity/{severity}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAlertsBySeverity(
            @PathVariable String severity,
            Pageable pageable) {
        try {
            Page<Alert> alerts = alertService.getAlertsBySeverity(severity, pageable);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve alerts: " + e.getMessage()
            ));
        }
    }

    /**
     * Get alerts affecting a specific device.
     * @param deviceId The device ID
     * @param pageable Pagination information
     * @return Page of alerts
     */
    @GetMapping("/alerts/device/{deviceId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAlertsByDevice(
            @PathVariable UUID deviceId,
            Pageable pageable) {
        try {
            Page<Alert> alerts = alertService.getAlertsByDevice(deviceId, pageable);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve alerts: " + e.getMessage()
            ));
        }
    }

    /**
     * Get alerts affecting a specific connection.
     * @param connectionId The connection ID
     * @param pageable Pagination information
     * @return Page of alerts
     */
    @GetMapping("/alerts/connection/{connectionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAlertsByConnection(
            @PathVariable UUID connectionId,
            Pageable pageable) {
        try {
            Page<Alert> alerts = alertService.getAlertsByConnection(connectionId, pageable);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve alerts: " + e.getMessage()
            ));
        }
    }

    /**
     * Get alert statistics.
     * @return Map of alert statistics
     */
    @GetMapping("/alerts/statistics")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAlertStatistics() {
        try {
            Map<String, Object> stats = alertService.getAlertStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve alert statistics: " + e.getMessage()
            ));
        }
    }

    /**
     * Get validation rules.
     * @param pageable Pagination information
     * @return Page of validation rules
     */
    @GetMapping("/rules")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getValidationRules(Pageable pageable) {
        try {
            Page<ValidationRule> rules = validationService.getValidationRules(pageable);
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve validation rules: " + e.getMessage()
            ));
        }
    }

    /**
     * Get a specific validation rule.
     * @param ruleName The rule name
     * @return The validation rule
     */
    @GetMapping("/rules/{ruleName}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getValidationRule(@PathVariable String ruleName) {
        try {
            ValidationRule rule = validationService.getValidationRule(ruleName);
            return ResponseEntity.ok(rule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve validation rule: " + e.getMessage()
            ));
        }
    }

    /**
     * Create a new validation rule.
     * @param rule The validation rule
     * @param userDetails The authenticated user
     * @return The created validation rule
     */
    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createValidationRule(
            @RequestBody ValidationRule rule,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            rule.setCreatedBy(user);
            rule.setCreatedAt(Instant.now());

            ValidationRule createdRule = validationService.createValidationRule(rule);
            return ResponseEntity.ok(createdRule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to create validation rule: " + e.getMessage()
            ));
        }
    }

    /**
     * Update an existing validation rule.
     * @param ruleName The rule name
     * @param rule The updated validation rule
     * @param userDetails The authenticated user
     * @return The updated validation rule
     */
    @PutMapping("/rules/{ruleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateValidationRule(
            @PathVariable String ruleName,
            @RequestBody ValidationRule rule,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            rule.setCreatedBy(user);

            ValidationRule updatedRule = validationService.updateValidationRule(ruleName, rule);
            return ResponseEntity.ok(updatedRule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to update validation rule: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete a validation rule.
     * @param ruleName The rule name
     * @param userDetails The authenticated user
     * @return Response indicating success or failure
     */
    @DeleteMapping("/rules/{ruleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteValidationRule(
            @PathVariable String ruleName,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            validationService.deleteValidationRule(ruleName);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Validation rule deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to delete validation rule: " + e.getMessage()
            ));
        }
    }

    /**
     * Enable a validation rule.
     * @param ruleName The rule name
     * @param userDetails The authenticated user
     * @return The updated validation rule
     */
    @PostMapping("/rules/{ruleName}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enableValidationRule(
            @PathVariable String ruleName,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ValidationRule rule = validationService.enableValidationRule(ruleName);
            return ResponseEntity.ok(rule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to enable validation rule: " + e.getMessage()
            ));
        }
    }

    /**
     * Disable a validation rule.
     * @param ruleName The rule name
     * @param userDetails The authenticated user
     * @return The updated validation rule
     */
    @PostMapping("/rules/{ruleName}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disableValidationRule(
            @PathVariable String ruleName,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ValidationRule rule = validationService.disableValidationRule(ruleName);
            return ResponseEntity.ok(rule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to disable validation rule: " + e.getMessage()
            ));
        }
    }
}
