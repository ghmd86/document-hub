package com.documenthub.repository;

import com.documenthub.model.entity.TemplateVendorMapping;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for TemplateVendorMapping entity.
 * Provides methods to query template vendor mappings with field configurations.
 */
@Repository
public interface TemplateVendorMappingRepository extends R2dbcRepository<TemplateVendorMapping, UUID> {

    /**
     * Find all vendor mappings for a specific template version.
     */
    @Query("SELECT * FROM template_vendor_mapping " +
           "WHERE template_id = :templateId " +
           "AND template_version = :templateVersion " +
           "AND is_active = TRUE " +
           "AND archive_indicator = FALSE")
    Flux<TemplateVendorMapping> findByTemplateIdAndVersion(
            @Param("templateId") UUID templateId,
            @Param("templateVersion") Integer templateVersion
    );

    /**
     * Find the primary vendor mapping for a template version.
     */
    @Query("SELECT * FROM template_vendor_mapping " +
           "WHERE template_id = :templateId " +
           "AND template_version = :templateVersion " +
           "AND is_primary = TRUE " +
           "AND is_active = TRUE " +
           "AND archive_indicator = FALSE " +
           "LIMIT 1")
    Mono<TemplateVendorMapping> findPrimaryByTemplateIdAndVersion(
            @Param("templateId") UUID templateId,
            @Param("templateVersion") Integer templateVersion
    );

    /**
     * Find vendor mappings by vendor type.
     */
    @Query("SELECT * FROM template_vendor_mapping " +
           "WHERE vendor = :vendor " +
           "AND is_active = TRUE " +
           "AND archive_indicator = FALSE")
    Flux<TemplateVendorMapping> findByVendor(@Param("vendor") String vendor);

    /**
     * Find all active vendor mappings.
     */
    @Query("SELECT * FROM template_vendor_mapping " +
           "WHERE is_active = TRUE " +
           "AND archive_indicator = FALSE")
    Flux<TemplateVendorMapping> findAllActive();

    /**
     * Find vendor mapping by vendor template key.
     */
    @Query("SELECT * FROM template_vendor_mapping " +
           "WHERE vendor_template_key = :vendorTemplateKey " +
           "AND is_active = TRUE " +
           "LIMIT 1")
    Mono<TemplateVendorMapping> findByVendorTemplateKey(
            @Param("vendorTemplateKey") String vendorTemplateKey
    );

    /**
     * Find vendor mappings by template name.
     */
    @Query("SELECT * FROM template_vendor_mapping " +
           "WHERE template_name = :templateName " +
           "AND is_active = TRUE " +
           "AND archive_indicator = FALSE")
    Flux<TemplateVendorMapping> findByTemplateName(@Param("templateName") String templateName);
}
