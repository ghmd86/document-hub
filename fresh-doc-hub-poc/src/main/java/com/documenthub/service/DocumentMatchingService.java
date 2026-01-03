package com.documenthub.service;

import com.documenthub.config.ReferenceKeyConfig;
import com.documenthub.dao.StorageIndexCriteriaDao;
import com.documenthub.dto.DocumentQueryParamsDto;
import com.documenthub.dto.MasterTemplateDto;
import com.documenthub.dto.StorageIndexDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Service for document matching logic.
 * Handles reference key and conditional matching.
 *
 * Uses Criteria API via StorageIndexCriteriaDao for dynamic query building.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentMatchingService {

    private final StorageIndexCriteriaDao criteriaDao;
    private final DocumentValidityService validityService;
    private final ReferenceKeyConfig referenceKeyConfig;
    private final ObjectMapper objectMapper;

    /**
     * Query documents based on template configuration using DocumentQueryParamsDto.
     */
    public Mono<List<StorageIndexDto>> queryDocuments(DocumentQueryParamsDto params) {
        logQueryStart(params);
        if (hasDocumentMatching(params.getTemplate(), params.getExtractedData())) {
            return queryByDocumentMatching(params);
        }
        return queryStandardDocuments(params);
    }

    private void logQueryStart(DocumentQueryParamsDto params) {
        log.info("QUERY DOCUMENTS for template: {}", params.getTemplate().getTemplateType());
        log.info("  extractedData: {}, dates: {}-{}",
                params.getExtractedData() != null, params.getPostedFromDate(), params.getPostedToDate());
    }

    private boolean hasDocumentMatching(MasterTemplateDto template, Map<String, Object> data) {
        return template.getDocumentMatchingConfig() != null && data != null;
    }

    private Mono<List<StorageIndexDto>> queryByDocumentMatching(DocumentQueryParamsDto params) {
        try {
            JsonNode matchingNode = objectMapper.readTree(params.getTemplate().getDocumentMatchingConfig());
            if (!matchingNode.has("matchBy")) {
                log.info("  No matchBy field in document_matching_config");
                return queryBySharedFlag(params);
            }
            return executeMatching(matchingNode.get("matchBy").asText(), matchingNode, params);
        } catch (Exception e) {
            log.error("Failed to parse document_matching_config: {}", e.getMessage());
            return Mono.just(Collections.emptyList());
        }
    }

    private Mono<List<StorageIndexDto>> executeMatching(
            String matchBy, JsonNode matchingNode, DocumentQueryParamsDto params) {

        // Get matchMode (default to "extracted" for backward compatibility)
        String matchMode = matchingNode.has("matchMode")
                ? matchingNode.get("matchMode").asText()
                : "extracted";

        switch (matchBy) {
            case "reference_key":
                return queryByReferenceKeyWithMode(matchingNode, params, matchMode);
            case "conditional":
                return queryByConditional(matchingNode, params);
            default:
                log.warn("Unknown matchBy: {}", matchBy);
                return Mono.just(Collections.emptyList());
        }
    }

    /**
     * Route to appropriate reference key query based on matchMode.
     *
     * @param matchingNode the document_matching_config JSON node
     * @param params query parameters
     * @param matchMode one of: "direct", "extracted", "auto_discover"
     */
    private Mono<List<StorageIndexDto>> queryByReferenceKeyWithMode(
            JsonNode matchingNode, DocumentQueryParamsDto params, String matchMode) {

        String referenceKeyType = matchingNode.get("referenceKeyType").asText();

        Mono<Void> validation = validateReferenceKeyType(referenceKeyType, params.getTemplate());
        if (validation != null) {
            return validation.then(Mono.just(Collections.emptyList()));
        }

        switch (matchMode) {
            case "direct":
                // Use referenceKey from request
                return queryByDirectReferenceKey(params, referenceKeyType);

            case "auto_discover":
                // Query by type only, filter by validity, return latest
                return queryByAutoDiscover(params, referenceKeyType);

            case "extracted":
            default:
                // Current behavior - use referenceKeyField from extractedData
                return queryByReferenceKey(matchingNode, params);
        }
    }

    /**
     * Direct mode: Use referenceKey directly from the enquiry request.
     */
    private Mono<List<StorageIndexDto>> queryByDirectReferenceKey(
            DocumentQueryParamsDto params, String referenceKeyType) {

        String referenceKey = params.getRequestReferenceKey();
        if (referenceKey == null || referenceKey.isEmpty()) {
            log.warn("Direct mode requires referenceKey in request, but none provided");
            return Mono.just(Collections.emptyList());
        }

        log.info("DIRECT MATCH: key='{}', type='{}', template='{}'",
                referenceKey, referenceKeyType, params.getTemplate().getTemplateType());
        return executeReferenceKeyQuery(referenceKey, referenceKeyType, params);
    }

    /**
     * Auto-discover mode: Query by reference key type only (no specific key).
     * Returns the latest valid document matching the type.
     */
    private Mono<List<StorageIndexDto>> queryByAutoDiscover(
            DocumentQueryParamsDto params, String referenceKeyType) {

        log.info("AUTO-DISCOVER: type='{}', template='{}'",
                referenceKeyType, params.getTemplate().getTemplateType());

        return criteriaDao.findByReferenceKeyType(referenceKeyType, params)
            .collectList()
            .map(validityService::filterByValidity)
            .map(this::keepLatestOnly)
            .doOnNext(docs -> log.info("Auto-discover found {} document(s)", docs.size()));
    }

    /**
     * Keep only the latest document (by doc_creation_date) from a list.
     * Used by auto_discover mode to return a single result.
     */
    private List<StorageIndexDto> keepLatestOnly(List<StorageIndexDto> docs) {
        if (docs.isEmpty()) {
            return docs;
        }
        return docs.stream()
                .max(Comparator.comparing(d ->
                        d.getDocCreationDate() != null ? d.getDocCreationDate() : 0L))
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    /**
     * Extracted mode (default): Use referenceKeyField from extractedData.
     */
    private Mono<List<StorageIndexDto>> queryByReferenceKey(JsonNode matchingNode, DocumentQueryParamsDto params) {
        String referenceKeyField = matchingNode.has("referenceKeyField")
                ? matchingNode.get("referenceKeyField").asText()
                : "referenceKey";
        String referenceKeyType = matchingNode.get("referenceKeyType").asText();

        Object referenceKeyValue = params.getExtractedData().get(referenceKeyField);
        if (referenceKeyValue == null) {
            log.warn("Reference key '{}' not found in extracted data", referenceKeyField);
            return Mono.just(Collections.emptyList());
        }

        logReferenceKeyQuery(referenceKeyValue, referenceKeyType, params.getTemplate());
        return executeReferenceKeyQuery(referenceKeyValue.toString(), referenceKeyType, params);
    }

    private Mono<Void> validateReferenceKeyType(String refKeyType, MasterTemplateDto template) {
        if (referenceKeyConfig.isValid(refKeyType)) {
            return null;
        }
        log.error("Invalid reference_key_type '{}' in template '{}'. Allowed values: {}",
                refKeyType, template.getTemplateType(), referenceKeyConfig.getAllowedTypesString());
        return Mono.error(new IllegalArgumentException(
                "Invalid reference_key_type: '" + refKeyType +
                "'. Allowed values: " + referenceKeyConfig.getAllowedTypesString()));
    }

    private Mono<List<StorageIndexDto>> queryByConditional(JsonNode matchingNode, DocumentQueryParamsDto params) {
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

    private Mono<List<StorageIndexDto>> handleMissingRefKeyType(MasterTemplateDto template) {
        log.error("referenceKeyType is required for conditional matching in template '{}'",
                template.getTemplateType());
        return Mono.error(new IllegalArgumentException(
                "referenceKeyType is required in document_matching_config for template: " +
                template.getTemplateType()));
    }

    private Mono<List<StorageIndexDto>> executeReferenceKeyQuery(
            String referenceKey, String referenceKeyType, DocumentQueryParamsDto params) {

        return criteriaDao.findByReferenceKey(referenceKey, referenceKeyType, params)
                .collectList()
                .map(validityService::filterByValidity)
                .doOnNext(docs -> log.info("Found {} valid documents", docs.size()));
    }

    private Mono<List<StorageIndexDto>> queryStandardDocuments(DocumentQueryParamsDto params) {
        return queryBySharedFlag(params);
    }

    private Mono<List<StorageIndexDto>> queryBySharedFlag(DocumentQueryParamsDto params) {
        if (Boolean.TRUE.equals(params.getTemplate().getSharedDocumentFlag())) {
            return querySharedDocuments(params);
        }
        return queryAccountDocuments(params);
    }

    private Mono<List<StorageIndexDto>> querySharedDocuments(DocumentQueryParamsDto params) {
        return criteriaDao.findSharedDocuments(params)
                .collectList()
                .map(validityService::filterByValidity)
                .doOnNext(docs -> log.debug("Found {} shared documents", docs.size()));
    }

    private Mono<List<StorageIndexDto>> queryAccountDocuments(DocumentQueryParamsDto params) {
        return criteriaDao.findAccountDocuments(params)
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
            Object key, String type, MasterTemplateDto template) {

        log.info("DOCUMENT MATCHING: key='{}', type='{}', template='{}'",
                key, type, template.getTemplateType());
    }

    private void logConditionalMatch(
            String key, String type, MasterTemplateDto template) {

        log.info("CONDITIONAL MATCH: key='{}', type='{}', template='{}'",
                key, type, template.getTemplateType());
    }
}
