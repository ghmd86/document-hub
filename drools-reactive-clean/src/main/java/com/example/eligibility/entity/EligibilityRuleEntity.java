package com.example.eligibility.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Eligibility Rule Entity
 *
 * Represents business rule for document eligibility stored in database.
 *
 * Maps to table: eligibility_rules
 *
 * JSONB Structure in 'conditions' column:
 * {
 *   "type": "ALL",  // or "ANY"
 *   "expressions": [
 *     {
 *       "source": "cardholder_agreements_api",
 *       "field": "cardholderAgreementsTNCCode",
 *       "operator": "EQUALS",
 *       "value": "TNC_GOLD_2024"
 *     }
 *   ]
 * }
 */
@Table("eligibility_rules")
public class EligibilityRuleEntity {

    @Id
    private Long id;

    @Column("rule_id")
    private String ruleId;

    @Column("document_id")
    private String documentId;

    private String name;

    private String description;

    private Integer priority;

    private Boolean enabled;

    @Column("conditions")
    private Json conditions;

    private Integer version;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    // ============================================
    // Constructors
    // ============================================

    public EligibilityRuleEntity() {
    }

    public EligibilityRuleEntity(String ruleId, String documentId, String name,
                                  Integer priority, Boolean enabled, Json conditions) {
        this.ruleId = ruleId;
        this.documentId = documentId;
        this.name = name;
        this.priority = priority;
        this.enabled = enabled;
        this.conditions = conditions;
    }

    // ============================================
    // Getters and Setters
    // ============================================

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
        return "EligibilityRuleEntity{" +
                "id=" + id +
                ", ruleId='" + ruleId + '\'' +
                ", documentId='" + documentId + '\'' +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}
