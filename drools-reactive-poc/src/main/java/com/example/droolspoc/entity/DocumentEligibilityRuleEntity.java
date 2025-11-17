package com.example.droolspoc.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Document Eligibility Rule Entity
 *
 * Stores document eligibility rules in database using JSONB column.
 *
 * This allows adding/modifying rules without code changes:
 * - Just insert/update a row in the database
 * - Conditions are stored as JSONB
 * - System automatically loads and evaluates the new rules
 */
@Table("document_eligibility_rules")
public class DocumentEligibilityRuleEntity {

    @Id
    private Long id;

    private String ruleId;      // e.g., "RULE-001"
    private String documentId;  // e.g., "DOC-TNC-GOLD-2024-BENEFITS"
    private String name;        // e.g., "Gold TNC Specific Document"
    private String description;
    private Integer priority;   // Higher priority = evaluated first
    private Boolean enabled;

    /**
     * ⭐ JSONB Conditions Column
     *
     * Stores complete rule conditions:
     * {
     *   "type": "ALL",  // or "ANY"
     *   "expressions": [
     *     {
     *       "source": "cardholder_agreements_api",
     *       "field": "cardholderAgreementsTNCCode",
     *       "operator": "EQUALS",
     *       "value": "TNC_GOLD_2024"
     *     },
     *     {
     *       "source": "account_service_api",
     *       "field": "accountBalance",
     *       "operator": "GREATER_THAN",
     *       "value": 50000
     *     }
     *   ]
     * }
     */
    @Column("conditions")
    private Json conditions;

    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructors
    public DocumentEligibilityRuleEntity() {}

    public DocumentEligibilityRuleEntity(
        String ruleId,
        String documentId,
        String name,
        String description,
        Integer priority,
        Boolean enabled,
        Json conditions
    ) {
        this.ruleId = ruleId;
        this.documentId = documentId;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.enabled = enabled;
        this.conditions = conditions;
        this.version = 1;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Json getConditions() {
        return conditions;
    }

    public void setConditions(Json conditions) {
        this.conditions = conditions;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
        return "DocumentEligibilityRuleEntity{" +
            "id=" + id +
            ", ruleId='" + ruleId + '\'' +
            ", documentId='" + documentId + '\'' +
            ", name='" + name + '\'' +
            ", priority=" + priority +
            ", enabled=" + enabled +
            ", version=" + version +
            '}';
    }
}
