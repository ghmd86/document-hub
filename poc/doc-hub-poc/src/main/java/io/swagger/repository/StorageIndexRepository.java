package io.swagger.repository;

import io.swagger.entity.StorageIndexEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface StorageIndexRepository
    extends R2dbcRepository<StorageIndexEntity, UUID> {

    Flux<StorageIndexEntity> findByAccountKey(UUID accountKey);

    Flux<StorageIndexEntity> findByCustomerKey(UUID customerKey);

    @Query("SELECT * FROM storage_index " +
           "WHERE reference_key = :referenceKey " +
           "AND reference_key_type = :referenceKeyType " +
           "AND template_type = :templateType " +
           "AND template_version = :templateVersion")
    Flux<StorageIndexEntity> findByReferenceKey(
        String referenceKey,
        String referenceKeyType,
        String templateType,
        Integer templateVersion
    );

    @Query("SELECT * FROM storage_index " +
           "WHERE template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND is_shared = true")
    Flux<StorageIndexEntity> findSharedDocuments(
        String templateType,
        Integer templateVersion
    );

    @Query("SELECT * FROM storage_index " +
           "WHERE template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND doc_metadata @> cast(:metadataJson as jsonb)")
    Flux<StorageIndexEntity> findByMetadataFields(
        String templateType,
        Integer templateVersion,
        String metadataJson
    );
}
