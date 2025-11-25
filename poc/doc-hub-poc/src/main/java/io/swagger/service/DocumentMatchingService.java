package io.swagger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.entity.StorageIndexEntity;
import io.swagger.model.config.DocumentMatchingStrategy;
import io.swagger.model.config.MatchingCondition;
import io.swagger.model.context.ExtractionContext;
import io.swagger.repository.StorageIndexRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class DocumentMatchingService {

    @Autowired
    private StorageIndexRepository storageIndexRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Find matching documents based on matching strategy
     */
    public Flux<StorageIndexEntity> findMatchingDocuments(
        String templateType,
        Integer templateVersion,
        DocumentMatchingStrategy matchingStrategy,
        ExtractionContext context
    ) {
        if (matchingStrategy == null) {
            log.debug("No matching strategy defined, returning empty result");
            return Flux.empty();
        }

        String strategy = matchingStrategy.getStrategy();
        log.debug("Using matching strategy: {}", strategy);

        return switch (strategy.toLowerCase()) {
            case "reference-key", "reference_key" -> findByReferenceKey(
                templateType,
                templateVersion,
                matchingStrategy,
                context
            );
            case "metadata" -> findByMetadata(
                templateType,
                templateVersion,
                matchingStrategy,
                context
            );
            case "composite" -> findByComposite(
                templateType,
                templateVersion,
                matchingStrategy,
                context
            );
            case "account-key", "account_key" -> findByAccountKey(
                templateType,
                templateVersion,
                context
            );
            case "customer-key", "customer_key" -> findByCustomerKey(
                templateType,
                templateVersion,
                context
            );
            default -> {
                log.warn("Unknown matching strategy: {}", strategy);
                yield Flux.empty();
            }
        };
    }

    /**
     * Find documents by reference key
     */
    private Flux<StorageIndexEntity> findByReferenceKey(
        String templateType,
        Integer templateVersion,
        DocumentMatchingStrategy matchingStrategy,
        ExtractionContext context
    ) {
        List<MatchingCondition> conditions = matchingStrategy.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            log.warn("No conditions defined for reference-key strategy");
            return Flux.empty();
        }

        // Find reference key and type from conditions
        String referenceKey = null;
        String referenceKeyType = null;

        for (MatchingCondition condition : conditions) {
            String metadataKey = condition.getMetadataKey();
            if ("referenceKey".equalsIgnoreCase(metadataKey) || "reference_key".equalsIgnoreCase(metadataKey)) {
                referenceKey = resolveValue(condition.getValueSource(), context);
            } else if ("referenceKeyType".equalsIgnoreCase(metadataKey) || "reference_key_type".equalsIgnoreCase(metadataKey)) {
                referenceKeyType = resolveValue(condition.getValueSource(), context);
            }
        }

        if (referenceKey == null) {
            log.warn("Reference key not found in conditions");
            return Flux.empty();
        }

        log.debug("Finding documents by reference key: {} (type: {})",
            referenceKey, referenceKeyType);

        return storageIndexRepository.findByReferenceKey(
            referenceKey,
            referenceKeyType,
            templateType,
            templateVersion
        );
    }

    /**
     * Find documents by metadata fields
     */
    private Flux<StorageIndexEntity> findByMetadata(
        String templateType,
        Integer templateVersion,
        DocumentMatchingStrategy matchingStrategy,
        ExtractionContext context
    ) {
        List<MatchingCondition> conditions = matchingStrategy.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            log.warn("No conditions defined for metadata strategy");
            return Flux.empty();
        }

        // Build metadata JSON for matching
        Map<String, Object> metadataFields = new HashMap<>();

        for (MatchingCondition condition : conditions) {
            String metadataKey = condition.getMetadataKey();
            String value = resolveValue(condition.getValueSource(), context);

            if (value != null && metadataKey != null) {
                metadataFields.put(metadataKey, value);
            }
        }

        if (metadataFields.isEmpty()) {
            log.warn("No metadata fields resolved for matching");
            return Flux.empty();
        }

        log.debug("Finding documents by metadata: {}", metadataFields);

        try {
            String metadataJson = objectMapper.writeValueAsString(metadataFields);

            return storageIndexRepository.findByMetadataFields(
                templateType,
                templateVersion,
                metadataJson
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata fields", e);
            return Flux.empty();
        }
    }

    /**
     * Find documents by composite strategy (multiple conditions)
     */
    private Flux<StorageIndexEntity> findByComposite(
        String templateType,
        Integer templateVersion,
        DocumentMatchingStrategy matchingStrategy,
        ExtractionContext context
    ) {
        List<MatchingCondition> conditions = matchingStrategy.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            log.warn("No conditions defined for composite strategy");
            return Flux.empty();
        }

        log.debug("Finding documents using composite strategy with {} conditions",
            conditions.size());

        // Start with all documents for template
        Flux<StorageIndexEntity> documents = storageIndexRepository
            .findByTemplateTypeAndTemplateVersion(templateType, templateVersion);

        // Apply each condition as a filter
        for (MatchingCondition condition : conditions) {
            documents = documents.filter(doc ->
                evaluateCondition(doc, condition, context)
            );
        }

        return documents;
    }

    /**
     * Find documents by account key
     */
    private Flux<StorageIndexEntity> findByAccountKey(
        String templateType,
        Integer templateVersion,
        ExtractionContext context
    ) {
        UUID accountId = context.getAccountId();

        if (accountId == null) {
            log.warn("Account ID not found in context");
            return Flux.empty();
        }

        log.debug("Finding documents by account key: {}", accountId);

        return storageIndexRepository.findByAccountKey(
            accountId.toString(),
            templateType,
            templateVersion
        );
    }

    /**
     * Find documents by customer key
     */
    private Flux<StorageIndexEntity> findByCustomerKey(
        String templateType,
        Integer templateVersion,
        ExtractionContext context
    ) {
        UUID customerId = context.getCustomerId();

        if (customerId == null) {
            log.warn("Customer ID not found in context");
            return Flux.empty();
        }

        log.debug("Finding documents by customer key: {}", customerId);

        return storageIndexRepository.findByCustomerKey(
            customerId.toString(),
            templateType,
            templateVersion
        );
    }

    /**
     * Evaluate a matching condition against a document
     */
    private boolean evaluateCondition(
        StorageIndexEntity document,
        MatchingCondition condition,
        ExtractionContext context
    ) {
        String metadataKey = condition.getMetadataKey();
        String operator = condition.getOperator();
        String expectedValue = resolveValue(condition.getValueSource(), context);

        // Get actual value from document
        Object actualValue = getDocumentFieldValue(document, metadataKey);

        log.debug("Evaluating condition: field={}, operator={}, expected={}, actual={}",
            metadataKey, operator, expectedValue, actualValue);

        if (operator == null) {
            operator = "EQUALS";
        }

        return switch (operator.toUpperCase()) {
            case "EQUALS" -> expectedValue != null && expectedValue.equals(String.valueOf(actualValue));
            case "CONTAINS" -> actualValue != null &&
                String.valueOf(actualValue).contains(expectedValue);
            case "STARTS_WITH" -> actualValue != null &&
                String.valueOf(actualValue).startsWith(expectedValue);
            case "IN" -> actualValue != null &&
                String.valueOf(actualValue).equals(expectedValue);
            default -> {
                log.warn("Unknown operator: {}", operator);
                yield false;
            }
        };
    }

    /**
     * Get field value from document
     */
    private Object getDocumentFieldValue(StorageIndexEntity document, String field) {
        if (field == null) {
            return null;
        }

        return switch (field.toLowerCase()) {
            case "accountkey", "account_key" -> document.getAccountKey();
            case "customerkey", "customer_key" -> document.getCustomerKey();
            case "referencekey", "reference_key" -> document.getReferenceKey();
            case "referencekeytype", "reference_key_type" -> document.getReferenceKeyType();
            case "storagedocumentkey", "storage_document_key" -> document.getStorageDocumentKey();
            case "templatetype", "template_type" -> document.getTemplateType();
            case "filename", "file_name" -> document.getFileName();
            default -> {
                // Try to get from metadata
                if (document.getDocMetadata() != null && document.getDocMetadata().has(field)) {
                    yield document.getDocMetadata().get(field).asText();
                }
                yield null;
            }
        };
    }

    /**
     * Resolve value with variable substitution
     */
    private String resolveValue(String value, ExtractionContext context) {
        if (value == null) {
            return null;
        }

        // Check if it's a variable reference (starts with $)
        if (value.startsWith("$")) {
            String varName = value.substring(1);
            Object varValue = context.getVariables().get(varName);

            if (varValue != null) {
                return varValue.toString();
            } else {
                log.warn("Variable {} not found in context", varName);
                return null;
            }
        }

        return value;
    }

    /**
     * Find shared documents based on sharing rules
     */
    public Flux<StorageIndexEntity> findSharedDocuments(
        String templateType,
        Integer templateVersion,
        UUID accountId,
        UUID customerId
    ) {
        log.debug("Finding shared documents for template: {}, version: {}",
            templateType, templateVersion);

        return storageIndexRepository.findSharedDocuments(
            accountId.toString(),
            customerId.toString(),
            templateType,
            templateVersion
        );
    }
}
