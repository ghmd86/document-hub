package com.documenthub.service;

import com.documenthub.config.ReferenceKeyConfig;
import com.documenthub.dto.DocumentQueryParams;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.repository.StorageIndexRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for document matching logic.
 * Handles reference key and conditional matching.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentMatchingService {

    private final StorageIndexRepository storageRepository;
    private final DocumentValidityService validityService;
    private final ReferenceKeyConfig referenceKeyConfig;
    private final ObjectMapper objectMapper;

    /**
     * Query documents based on template configuration using DocumentQueryParams.
     */
    public Mono<List<StorageIndexEntity>> queryDocuments(DocumentQueryParams params) {
        logQueryStart(params);
        if (hasDocumentMatching(params.getTemplate(), params.getExtractedData())) {
            return queryByDocumentMatching(params);
        }
        return queryStandardDocuments(params);
    }

    private void logQueryStart(DocumentQueryParams params) {
        log.info("QUERY DOCUMENTS for template: {}", params.getTemplate().getTemplateType());
        log.info("  extractedData: {}, dates: {}-{}",
                params.getExtractedData() != null, params.getPostedFromDate(), params.getPostedToDate());
    }

    private boolean hasDocumentMatching(MasterTemplateDefinitionEntity template, Map<String, Object> data) {
        return template.getDocumentMatchingConfig() != null && data != null;
    }

    private Mono<List<StorageIndexEntity>> queryByDocumentMatching(DocumentQueryParams params) {
        try {
            JsonNode matchingNode = objectMapper.readTree(
                    params.getTemplate().getDocumentMatchingConfig().asString());
            if (!matchingNode.has("matchBy")) {
                log.info("  No matchBy field in document_matching_config");
                return queryBySharedFlag(params, null);
            }
            return executeMatching(matchingNode.get("matchBy").asText(), matchingNode, params);
        } catch (Exception e) {
            log.error("Failed to parse document_matching_config: {}", e.getMessage());
            return Mono.just(Collections.emptyList());
        }
    }

    private Mono<List<StorageIndexEntity>> executeMatching(
            String matchBy, JsonNode matchingNode, DocumentQueryParams params) {
        switch (matchBy) {
            case "reference_key":
                return queryByReferenceKey(matchingNode, params);
            case "conditional":
                return queryByConditional(matchingNode, params);
            default:
                log.warn("Unknown matchBy: {}", matchBy);
                return Mono.just(Collections.emptyList());
        }
    }

    private Mono<List<StorageIndexEntity>> queryByReferenceKey(JsonNode matchingNode, DocumentQueryParams params) {
        String referenceKeyField = matchingNode.get("referenceKeyField").asText();
        String referenceKeyType = matchingNode.get("referenceKeyType").asText();

        Mono<Void> validation = validateReferenceKeyType(referenceKeyType, params.getTemplate());
        if (validation != null) {
            return validation.then(Mono.just(Collections.emptyList()));
        }

        Object referenceKeyValue = params.getExtractedData().get(referenceKeyField);
        if (referenceKeyValue == null) {
            log.warn("Reference key '{}' not found in extracted data", referenceKeyField);
            return Mono.just(Collections.emptyList());
        }

        logReferenceKeyQuery(referenceKeyValue, referenceKeyType, params.getTemplate());
        return executeReferenceKeyQuery(referenceKeyValue.toString(), referenceKeyType, params);
    }

    private Mono<Void> validateReferenceKeyType(String refKeyType, MasterTemplateDefinitionEntity template) {
        if (referenceKeyConfig.isValid(refKeyType)) {
            return null;
        }
        log.error("Invalid reference_key_type '{}' in template '{}'. Allowed values: {}",
                refKeyType, template.getTemplateType(), referenceKeyConfig.getAllowedTypesString());
        return Mono.error(new IllegalArgumentException(
                "Invalid reference_key_type: '" + refKeyType +
                "'. Allowed values: " + referenceKeyConfig.getAllowedTypesString()));
    }

    private Mono<List<StorageIndexEntity>> queryByConditional(JsonNode matchingNode, DocumentQueryParams params) {
        if (!matchingNode.has("referenceKeyType")) {
            return handleMissingRefKeyType(params.getTemplate());
        }

        String referenceKeyType = matchingNode.get("referenceKeyType").asText();
        Mono<Void> validation = validateReferenceKeyType(referenceKeyType, params.getTemplate());
        if (validation != null) {
            return validation.then(Mono.just(Collections.emptyList()));
        }

        JsonNode conditionsNode = matchingNode.get("conditions");
        if (conditionsNode == null || !conditionsNode.isArray()) {
            log.warn("No conditions array found");
            return Mono.just(Collections.emptyList());
        }

        String matchedKey = evaluateConditions(conditionsNode, params.getExtractedData());
        if (matchedKey == null) {
            log.warn("No condition matched");
            return Mono.just(Collections.emptyList());
        }

        logConditionalMatch(matchedKey, referenceKeyType, params.getTemplate());
        return executeReferenceKeyQuery(matchedKey, referenceKeyType, params);
    }

    private Mono<List<StorageIndexEntity>> handleMissingRefKeyType(MasterTemplateDefinitionEntity template) {
        log.error("referenceKeyType is required for conditional matching in template '{}'",
                template.getTemplateType());
        return Mono.error(new IllegalArgumentException(
                "referenceKeyType is required in document_matching_config for template: " +
                template.getTemplateType()));
    }

    private Mono<List<StorageIndexEntity>> executeReferenceKeyQuery(
            String referenceKey, String referenceKeyType, DocumentQueryParams params) {
        MasterTemplateDefinitionEntity template = params.getTemplate();
        return storageRepository.findByReferenceKeyAndTemplateWithDateRange(
                referenceKey, referenceKeyType,
                template.getTemplateType(), template.getTemplateVersion(),
                params.getPostedFromDate(), params.getPostedToDate(), System.currentTimeMillis())
                .collectList()
                .map(validityService::filterByValidity)
                .doOnNext(docs -> log.info("Found {} valid documents", docs.size()));
    }

    private Mono<List<StorageIndexEntity>> queryStandardDocuments(DocumentQueryParams params) {
        return queryBySharedFlag(params, params.getAccountId());
    }

    private Mono<List<StorageIndexEntity>> queryBySharedFlag(DocumentQueryParams params, UUID accountId) {
        if (Boolean.TRUE.equals(params.getTemplate().getSharedDocumentFlag())) {
            return querySharedDocuments(params);
        }
        return queryAccountDocuments(params, accountId);
    }

    private Mono<List<StorageIndexEntity>> querySharedDocuments(DocumentQueryParams params) {
        MasterTemplateDefinitionEntity template = params.getTemplate();
        return storageRepository.findSharedDocumentsWithDateRange(
                template.getTemplateType(), template.getTemplateVersion(),
                params.getPostedFromDate(), params.getPostedToDate(), System.currentTimeMillis())
                .collectList()
                .map(validityService::filterByValidity)
                .doOnNext(docs -> log.debug("Found {} shared documents", docs.size()));
    }

    private Mono<List<StorageIndexEntity>> queryAccountDocuments(DocumentQueryParams params, UUID accountId) {
        MasterTemplateDefinitionEntity template = params.getTemplate();
        return storageRepository.findAccountSpecificDocumentsWithDateRange(
                accountId, template.getTemplateType(), template.getTemplateVersion(),
                params.getPostedFromDate(), params.getPostedToDate(), System.currentTimeMillis())
                .collectList()
                .map(validityService::filterByValidity)
                .doOnNext(docs -> log.debug("Found {} account documents", docs.size()));
    }

    private String evaluateConditions(JsonNode conditions, Map<String, Object> data) {
        for (JsonNode condition : conditions) {
            String field = condition.get("field").asText();
            String operator = condition.get("operator").asText();
            JsonNode threshold = condition.get("value");
            String referenceKey = condition.get("referenceKey").asText();

            Object fieldValue = data.get(field);

            if (fieldValue == null) {
                continue;
            }

            if (evaluateCondition(fieldValue, operator, threshold)) {
                log.info("Condition matched: {} {} {}", field, operator, threshold);
                return referenceKey;
            }
        }
        return null;
    }

    private boolean evaluateCondition(Object value, String operator, JsonNode threshold) {
        if (threshold.isNumber()) {
            return evaluateNumeric(value, operator, threshold.asDouble());
        }
        if (threshold.isTextual()) {
            return evaluateString(value.toString(), operator, threshold.asText());
        }
        if (threshold.isBoolean()) {
            return evaluateBoolean(value, operator, threshold.asBoolean());
        }
        return false;
    }

    private boolean evaluateNumeric(Object value, String operator, double threshold) {
        try {
            double numValue = value instanceof Number
                    ? ((Number) value).doubleValue()
                    : Double.parseDouble(value.toString());

            switch (operator) {
                case ">=": return numValue >= threshold;
                case ">":  return numValue > threshold;
                case "<=": return numValue <= threshold;
                case "<":  return numValue < threshold;
                case "==": return numValue == threshold;
                case "!=": return numValue != threshold;
                default:   return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean evaluateString(String value, String operator, String threshold) {
        switch (operator) {
            case "==":         return value.equals(threshold);
            case "!=":         return !value.equals(threshold);
            case "contains":   return value.contains(threshold);
            case "startsWith": return value.startsWith(threshold);
            case "endsWith":   return value.endsWith(threshold);
            default:           return false;
        }
    }

    private boolean evaluateBoolean(Object value, String operator, boolean threshold) {
        boolean boolValue = Boolean.parseBoolean(value.toString());

        switch (operator) {
            case "==": return boolValue == threshold;
            case "!=": return boolValue != threshold;
            default:   return false;
        }
    }

    private void logReferenceKeyQuery(
            Object key, String type, MasterTemplateDefinitionEntity template) {

        log.info("DOCUMENT MATCHING: key='{}', type='{}', template='{}'",
                key, type, template.getTemplateType());
    }

    private void logConditionalMatch(
            String key, String type, MasterTemplateDefinitionEntity template) {

        log.info("CONDITIONAL MATCH: key='{}', type='{}', template='{}'",
                key, type, template.getTemplateType());
    }
}
