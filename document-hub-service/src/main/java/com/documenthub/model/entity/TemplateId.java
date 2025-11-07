package com.documenthub.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for MasterTemplateDefinition.
 * Represents the combination of template_id and version.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column("template_id")
    private UUID templateId;

    @Column("version")
    private Integer version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateId that = (TemplateId) o;
        return templateId.equals(that.templateId) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(templateId, version);
    }
}
