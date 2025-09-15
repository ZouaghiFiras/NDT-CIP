package com.network.digitaltwin.topology.model;

import com.network.digitaltwin.model.BaseEntity;
import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a snapshot of the network topology for auditing and versioning.
 */
@Entity
@Table(name = "topology_snapshots",
       indexes = {
           @Index(name = "idx_snapshot_version", columnList = "topology_version"),
           @Index(name = "snapshot_created_at", columnList = "created_at")
       })
public class TopologySnapshot extends BaseEntity {

    @Column(name = "topology_version", nullable = false)
    private Integer topologyVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "snapshot_data", columnDefinition = "jsonb")
    private Map<String, Object> snapshotData = new HashMap<>();

    @Column(name = "change_description", columnDefinition = "text")
    private String changeDescription;

    @Column(name = "change_type", nullable = false)
    private String changeType; // CREATE, UPDATE, DELETE, RESTORE

    // Constructors
    public TopologySnapshot() {
        this.createdAt = Instant.now();
    }

    public TopologySnapshot(Integer topologyVersion, User createdBy, Map<String, Object> snapshotData, 
                           String changeDescription, String changeType) {
        this.topologyVersion = topologyVersion;
        this.createdBy = createdBy;
        this.snapshotData = snapshotData;
        this.changeDescription = changeDescription;
        this.changeType = changeType;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Integer getTopologyVersion() {
        return topologyVersion;
    }

    public void setTopologyVersion(Integer topologyVersion) {
        this.topologyVersion = topologyVersion;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getSnapshotData() {
        return snapshotData;
    }

    public void setSnapshotData(Map<String, Object> snapshotData) {
        this.snapshotData = snapshotData;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    // Convenience methods
    public void addSnapshotData(String key, Object value) {
        this.snapshotData.put(key, value);
    }

    public boolean isCreate() {
        return "CREATE".equals(changeType);
    }

    public boolean isUpdate() {
        return "UPDATE".equals(changeType);
    }

    public boolean isDelete() {
        return "DELETE".equals(changeType);
    }

    public boolean isRestore() {
        return "RESTORE".equals(changeType);
    }

    @Override
    public String toString() {
        return "TopologySnapshot{" +
                "id=" + getId() +
                ", topologyVersion=" + topologyVersion +
                ", changeType='" + changeType + ''' +
                ", createdAt=" + createdAt +
                '}';
    }
}
