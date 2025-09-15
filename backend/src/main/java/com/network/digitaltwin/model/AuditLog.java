package com.network.digitaltwin.model;

import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing audit logs for tracking important actions and changes.
 */
@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp"),
           @Index(name = "idx_audit_logs_action", columnList = "action"),
           @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
           @Index(name = "idx_audit_logs_entity_type", columnList = "entity_type"),
           @Index(name = "idx_audit_logs_entity_id", columnList = "entity_id")
       })
public class AuditLog extends BaseEntity {

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @NotNull
    @Column(name = "action", nullable = false)
    private String action; // e.g., "CREATE", "UPDATE", "DELETE", "LOGIN", "LOGOUT", "AUTHORIZE"

    @Column(name = "entity_type")
    private String entityType; // e.g., "DEVICE", "POLICY", "USER"

    @Column(name = "entity_id")
    private String entityId; // ID of the affected entity

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "changes", columnDefinition = "jsonb")
    private Map<String, Object> changes = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "success")
    private Boolean success = true;

    @Column(name = "error_message")
    private String errorMessage;

    // Constructors
    public AuditLog() {
    }

    public AuditLog(String action, String entityType, String entityId, User user) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.user = user;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(Map<String, Object> newValues) {
        this.newValues = newValues;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Convenience methods
    public void addChange(String field, Object oldValue, Object newValue) {
        this.changes.put(field, Map.of("old", oldValue, "new", newValue));
        if (oldValue != null) {
            this.oldValues.put(field, oldValue);
        }
        if (newValue != null) {
            this.newValues.put(field, newValue);
        }
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public boolean isCreateAction() {
        return "CREATE".equals(action);
    }

    public boolean isUpdateAction() {
        return "UPDATE".equals(action);
    }

    public boolean isDeleteAction() {
        return "DELETE".equals(action);
    }

    public boolean isLoginAction() {
        return "LOGIN".equals(action);
    }

    public boolean isLogoutAction() {
        return "LOGOUT".equals(action);
    }

    public boolean isAuthorizeAction() {
        return "AUTHORIZE".equals(action);
    }

    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }

    public boolean hasUser() {
        return user != null;
    }

    public boolean hasIpAddress() {
        return ipAddress != null && !ipAddress.isEmpty();
    }

    public boolean hasSessionId() {
        return sessionId != null && !sessionId.isEmpty();
    }

    public boolean hasRequestId() {
        return requestId != null && !requestId.isEmpty();
    }

    public boolean isSuccessful() {
        return success != null && success;
    }

    public void markFailed(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + getId() +
                ", timestamp=" + timestamp +
                ", action='" + action + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", success=" + success +
                '}';
    }
}
