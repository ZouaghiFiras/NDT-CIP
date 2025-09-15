package com.network.digitaltwin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing tags that can be applied to various entities.
 */
@Entity
@Table(name = "tags",
       indexes = {
           @Index(name = "idx_tags_name", columnList = "name"),
           @Index(name = "idx_tags_category", columnList = "category")
       })
public class Tag extends BaseEntity {

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category")
    private String category; // e.g., "SECURITY", "COMPLIANCE", "BUSINESS", "TECHNICAL"

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "color")
    private String color; // Hex color code for UI representation

    @Column(name = "icon")
    private String icon; // Icon class name for UI representation

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "usage_count")
    private Long usageCount = 0L;

    // Constructors
    public Tag() {
    }

    public Tag(String name, String category) {
        this.name = name;
        this.category = category;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }

    // Convenience methods
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void incrementUsageCount() {
        this.usageCount++;
        this.updatedAt = Instant.now();
    }

    public void decrementUsageCount() {
        if (this.usageCount > 0) {
            this.usageCount--;
            this.updatedAt = Instant.now();
        }
    }

    public boolean isActiveTag() {
        return active != null && active;
    }

    public boolean hasColor() {
        return color != null && !color.isEmpty();
    }

    public boolean hasIcon() {
        return icon != null && !icon.isEmpty();
    }

    public boolean hasDescription() {
        return description != null && !description.isEmpty();
    }

    public boolean hasCategory() {
        return category != null && !category.isEmpty();
    }

    @Override
    public String toString() {
        return "Tag{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", active=" + active +
                ", usageCount=" + usageCount +
                '}';
    }
}
