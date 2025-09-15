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
 * Entity representing application/system logs stored in the database.
 */
@Entity
@Table(name = "logs",
       indexes = {
           @Index(name = "idx_logs_timestamp", columnList = "timestamp"),
           @Index(name = "idx_logs_level", columnList = "level"),
           @Index(name = "idx_logs_source", columnList = "source"),
           @Index(name = "idx_logs_user_id", columnList = "user_id")
       })
public class Log extends BaseEntity {

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "level", nullable = false)
    private String level; // e.g., "DEBUG", "INFO", "WARN", "ERROR", "FATAL"

    @NotNull
    @Column(name = "source", nullable = false)
    private String source; // e.g., "AUTH_SERVICE", "DEVICE_MANAGER", "RISK_CALCULATOR"

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "logger")
    private String logger;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "context", columnDefinition = "jsonb")
    private Map<String, Object> context = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "exception", columnDefinition = "jsonb")
    private Map<String, Object> exception = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "session_id")
    private String sessionId;

    // Constructors
    public Log() {
    }

    public Log(String level, String source, String message) {
        this.level = level;
        this.source = source;
        this.message = message;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Map<String, Object> getException() {
        return exception;
    }

    public void setException(Map<String, Object> exception) {
        this.exception = exception;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    // Convenience methods
    public void addContext(String key, Object value) {
        this.context.put(key, value);
    }

    public void addException(String key, Object value) {
        this.exception.put(key, value);
    }

    public boolean isDebug() {
        return "DEBUG".equals(level);
    }

    public boolean isInfo() {
        return "INFO".equals(level);
    }

    public boolean isWarn() {
        return "WARN".equals(level);
    }

    public boolean isError() {
        return "ERROR".equals(level);
    }

    public boolean isFatal() {
        return "FATAL".equals(level);
    }

    public boolean isErrorOrHigher() {
        return isError() || isFatal();
    }

    public boolean hasException() {
        return exception != null && !exception.isEmpty();
    }

    public boolean hasUser() {
        return user != null;
    }

    public boolean hasIpAddress() {
        return ipAddress != null && !ipAddress.isEmpty();
    }

    public boolean hasRequestId() {
        return requestId != null && !requestId.isEmpty();
    }

    public boolean hasSessionId() {
        return sessionId != null && !sessionId.isEmpty();
    }

    @Override
    public String toString() {
        return "Log{" +
                "id=" + getId() +
                ", timestamp=" + timestamp +
                ", level='" + level + '\'' +
                ", source='" + source + '\'' +
                ", message='" + message + '\'' +
                ", user=" + (user != null ? user.getUsername() : "null") +
                '}';
    }
}
