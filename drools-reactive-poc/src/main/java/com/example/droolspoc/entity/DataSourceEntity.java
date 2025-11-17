package com.example.droolspoc.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Data Source Entity (External API Configuration)
 *
 * Stores external API configuration in database using JSONB column.
 *
 * This allows adding new APIs without code changes:
 * - Just insert a new row in the database
 * - Configuration is stored as JSONB
 * - System automatically loads and uses the new API
 */
@Table("data_sources")
public class DataSourceEntity {

    @Id
    private String id;  // e.g., "arrangements_api"

    private String name;  // e.g., "Arrangements API"
    private String type;  // e.g., "REST_API"

    /**
     * ⭐ JSONB Configuration Column
     *
     * Stores complete API configuration:
     * {
     *   "method": "GET",
     *   "baseUrl": "http://localhost:8081",
     *   "endpoint": "/api/v1/arrangements/{arrangementId}",
     *   "timeoutMs": 5000,
     *   "retryCount": 2,
     *   "dependsOn": [...],       // For chained calls
     *   "responseMapping": [...]   // Field mappings
     * }
     */
    @Column("configuration")
    private Json configuration;

    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructors
    public DataSourceEntity() {}

    public DataSourceEntity(String id, String name, String type, Json configuration, Boolean enabled) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.configuration = configuration;
        this.enabled = enabled;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Json getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Json configuration) {
        this.configuration = configuration;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "DataSourceEntity{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", enabled=" + enabled +
            '}';
    }
}
