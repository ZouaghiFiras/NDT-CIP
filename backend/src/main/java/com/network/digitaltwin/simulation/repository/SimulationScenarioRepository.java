package com.network.digitaltwin.simulation.repository;

import com.network.digitaltwin.simulation.model.SimulationScenario;
import com.network.digitaltwin.simulation.model.SimulationStatus;
import com.network.digitaltwin.simulation.model.SimulationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for simulation scenarios.
 */
@Repository
public interface SimulationScenarioRepository extends JpaRepository<SimulationScenario, UUID> {

    /**
     * Find simulation scenarios by name.
     * @param name The scenario name
     * @return List of scenarios with the given name
     */
    List<SimulationScenario> findByName(String name);

    /**
     * Find simulation scenarios by type.
     * @param type The scenario type
     * @return List of scenarios with the given type
     */
    List<SimulationScenario> findByType(SimulationType type);

    /**
     * Find simulation scenarios by status.
     * @param status The scenario status
     * @return List of scenarios with the given status
     */
    List<SimulationScenario> findByStatus(SimulationStatus status);

    /**
     * Find simulation scenarios by creator.
     * @param createdBy The creator user ID
     * @return List of scenarios created by the given user
     */
    List<SimulationScenario> findByCreatedBy_Id(UUID createdBy);

    /**
     * Find simulation scenarios by type and status.
     * @param type The scenario type
     * @param status The scenario status
     * @return List of scenarios with the given type and status
     */
    List<SimulationScenario> findByTypeAndStatus(SimulationType type, SimulationStatus status);

    /**
     * Find simulation scenarios by type with pagination.
     * @param type The scenario type
     * @param pageable Pagination information
     * @return Page of scenarios with the given type
     */
    Page<SimulationScenario> findByType(SimulationType type, Pageable pageable);

    /**
     * Find simulation scenarios by status with pagination.
     * @param status The scenario status
     * @param pageable Pagination information
     * @return Page of scenarios with the given status
     */
    Page<SimulationScenario> findByStatus(SimulationStatus status, Pageable pageable);

    /**
     * Find simulation scenarios by creator with pagination.
     * @param createdBy The creator user ID
     * @param pageable Pagination information
     * @return Page of scenarios created by the given user
     */
    Page<SimulationScenario> findByCreatedBy_Id(UUID createdBy, Pageable pageable);

    /**
     * Find simulation scenarios by type and status with pagination.
     * @param type The scenario type
     * @param status The scenario status
     * @param pageable Pagination information
     * @return Page of scenarios with the given type and status
     */
    Page<SimulationScenario> findByTypeAndStatus(SimulationType type, SimulationStatus status, Pageable pageable);

    /**
     * Find simulation scenarios created between two dates.
     * @param start Start date
     * @param end End date
     * @param pageable Pagination information
     * @return Page of scenarios created between the given dates
     */
    Page<SimulationScenario> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * Count simulation scenarios by type.
     * @param type The scenario type
     * @return Count of scenarios with the given type
     */
    long countByType(SimulationType type);

    /**
     * Count simulation scenarios by status.
     * @param status The scenario status
     * @return Count of scenarios with the given status
     */
    long countByStatus(SimulationStatus status);

    /**
     * Count simulation scenarios by type and status.
     * @param type The scenario type
     * @param status The scenario status
     * @return Count of scenarios with the given type and status
     */
    long countByTypeAndStatus(SimulationType type, SimulationStatus status);

    /**
     * Find simulation scenarios by target device.
     * @param deviceId The target device ID
     * @return List of scenarios targeting the given device
     */
    @Query("SELECT s FROM SimulationScenario s JOIN s.targetDevices d WHERE d = :deviceId")
    List<SimulationScenario> findByTargetDevice(@Param("deviceId") UUID deviceId);

    /**
     * Find simulation scenarios by target device with pagination.
     * @param deviceId The target device ID
     * @param pageable Pagination information
     * @return Page of scenarios targeting the given device
     */
    @Query("SELECT s FROM SimulationScenario s JOIN s.targetDevices d WHERE d = :deviceId")
    Page<SimulationScenario> findByTargetDevice(@Param("deviceId") UUID deviceId, Pageable pageable);

    /**
     * Find simulation scenarios containing a specific device in target devices.
     * @param deviceId The device ID
     * @return List of scenarios containing the device in target devices
     */
    @Query("SELECT s FROM SimulationScenario s WHERE :deviceId MEMBER OF s.targetDevices")
    List<SimulationScenario> findByTargetDevicesContaining(@Param("deviceId") UUID deviceId);

    /**
     * Find simulation scenarios by attack vector.
     * @param attackVector The attack vector
     * @return List of scenarios with the given attack vector
     */
    List<SimulationScenario> findByAttackVector(String attackVector);

    /**
     * Find simulation scenarios by attack vector with pagination.
     * @param attackVector The attack vector
     * @param pageable Pagination information
     * @return Page of scenarios with the given attack vector
     */
    Page<SimulationScenario> findByAttackVector(String attackVector, Pageable pageable);

    /**
     * Check if a scenario with the given name exists.
     * @param name The scenario name
     * @return True if a scenario with the given name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find simulation scenarios by name with pagination.
     * @param name The scenario name
     * @param pageable Pagination information
     * @return Page of scenarios with the given name
     */
    Page<SimulationScenario> findByName(String name, Pageable pageable);
}
