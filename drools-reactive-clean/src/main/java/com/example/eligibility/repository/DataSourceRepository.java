package com.example.eligibility.repository;

import com.example.eligibility.entity.DataSourceEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Data Source Repository
 *
 * R2DBC repository for accessing data_sources table.
 * Provides reactive (non-blocking) database operations.
 */
@Repository
public interface DataSourceRepository extends ReactiveCrudRepository<DataSourceEntity, String> {

    /**
     * Find all enabled data sources
     *
     * @return Flux of enabled data sources
     */
    Flux<DataSourceEntity> findByEnabledTrue();
}
