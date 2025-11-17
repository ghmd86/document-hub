package com.example.eligibility.repository;

import com.example.eligibility.entity.EligibilityRuleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Eligibility Rule Repository
 *
 * R2DBC repository for accessing eligibility_rules table.
 * Provides reactive (non-blocking) database operations.
 */
@Repository
public interface EligibilityRuleRepository extends ReactiveCrudRepository<EligibilityRuleEntity, Long> {

    /**
     * Find all enabled rules, ordered by priority (descending)
     * Higher priority rules are evaluated first
     *
     * @return Flux of enabled rules ordered by priority
     */
    Flux<EligibilityRuleEntity> findByEnabledTrueOrderByPriorityDesc();

    /**
     * Find rules for a specific document
     *
     * @param documentId Document ID
     * @return Flux of rules for the document
     */
    Flux<EligibilityRuleEntity> findByDocumentIdAndEnabledTrue(String documentId);
}
