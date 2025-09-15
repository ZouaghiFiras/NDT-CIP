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
 * Entity representing application settings and configuration.
 */
@Entity
@Table(name = "settings",
       indexes = {
           @Index(name = "idx_settings_name", columnList = "name"),
           @Index(name = "idx_settings_category", columnList = "category")
       })
public class Settings extends BaseEntity {

    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "category")
    private String category; // e.g., "SECURITY", "NOTIFICATIONS", "UI", "BACKUP", "AUDIT"

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "value", columnDefinition = "jsonb")
    private Map<String, Object> value = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "default_value", columnDefinition = "jsonb")
    private Map<String, Object> defaultValue = new HashMap<>();

    @Column(name = "data_type")
    private String dataType; // e.g., "STRING", "INTEGER", "BOOLEAN", "JSON", "ARRAY"

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    @Column(name = "is_sensitive")
    private Boolean isSensitive = false;

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Column(name = "is_visible")
    private Boolean isVisible = true;

    @Column(name = "is_editable")
    private Boolean isEditable = true;

    @Column(name = "validation_regex")
    private String validationRegex;

    @Column(name = "options")
    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "options", columnDefinition = "jsonb")
    private Map<String, Object> options = new HashMap<>();

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    // Constructors
    public Settings() {
    }

    public Settings(String name, String category, String dataType) {
        this.name = name;
        this.category = category;
        this.dataType = dataType;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getValue() {
        return value;
    }

    public void setValue(Map<String, Object> value) {
        this.value = value;
        this.lastModifiedAt = Instant.now();
    }

    public Map<String, Object> getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Map<String, Object> defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Boolean getIsEncrypted() {
        return isEncrypted;
    }

    public void setIsEncrypted(Boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public Boolean getIsSensitive() {
        return isSensitive;
    }

    public void setIsSensitive(Boolean isSensitive) {
        this.isSensitive = isSensitive;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Boolean isVisible) {
        this.isVisible = isVisible;
    }

    public Boolean getIsEditable() {
        return isEditable;
    }

    public void setIsEditable(Boolean isEditable) {
        this.isEditable = isEditable;
    }

    public String getValidationRegex() {
        return validationRegex;
    }

    public void setValidationRegex(String validationRegex) {
        this.validationRegex = validationRegex;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    // Convenience methods
    public void addValue(String key, Object value) {
        this.value.put(key, value);
        this.lastModifiedAt = Instant.now();
    }

    public void addDefaultValue(String key, Object value) {
        this.defaultValue.put(key, value);
    }

    public void addOption(String key, Object value) {
        this.options.put(key, value);
    }

    public Object getValue(String key) {
        return value.get(key);
    }

    public Object getDefaultValue(String key) {
        return defaultValue.get(key);
    }

    public Object getOption(String key) {
        return options.get(key);
    }

    public void removeValue(String key) {
        value.remove(key);
        this.lastModifiedAt = Instant.now();
    }

    public void removeDefaultValue(String key) {
        defaultValue.remove(key);
    }

    public void removeOption(String key) {
        options.remove(key);
    }

    public boolean hasValue(String key) {
        return value.containsKey(key);
    }

    public boolean hasDefaultValue(String key) {
        return defaultValue.containsKey(key);
    }

    public boolean hasOption(String key) {
        return options.containsKey(key);
    }

    public boolean isStringType() {
        return "STRING".equals(dataType);
    }

    public boolean isIntegerType() {
        return "INTEGER".equals(dataType);
    }

    public boolean isBooleanType() {
        return "BOOLEAN".equals(dataType);
    }

    public boolean isJsonType() {
        return "JSON".equals(dataType);
    }

    public boolean isArrayType() {
        return "ARRAY".equals(dataType);
    }

    public boolean isEncryptedSetting() {
        return isEncrypted != null && isEncrypted;
    }

    public boolean isSensitiveSetting() {
        return isSensitive != null && isSensitive;
    }

    public boolean isSystemSetting() {
        return isSystem != null && isSystem;
    }

    public boolean isVisibleSetting() {
        return isVisible != null && isVisible;
    }

    public boolean isEditableSetting() {
        return isEditable != null && isEditable;
    }

    public void recordModification(String modifier) {
        this.lastModifiedBy = modifier;
        this.lastModifiedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "Settings{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", dataType='" + dataType + '\'' +
                ", isSystem=" + isSystem +
                ", lastModifiedBy='" + lastModifiedBy + '\'' +
                '}';
    }
}
