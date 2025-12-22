package com.documenthub.service;

import com.documenthub.config.ReferenceKeyConfig;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.repository.StorageIndexRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     * Query documents based on template configuration.
     */
    public Mono<List<StorageIndexEntity>> queryDocuments(
            MasterTemplateDefinitionEntity template,
            UUID accountId,
            Map<String, Object> extractedData,
            Long postedFromDate,
            Long postedToDate) {

        logQueryStart(template, extractedData, postedFromDate, postedToDate);

        if (hasDocumentMatching(template, extractedData)) {
            return queryByDocumentMatching(
                    template, extractedData, postedFromDate, postedToDate);
        }

        return queryStandardDocuments(
                template, accountId, postedFromDate, postedToDate);
    }

    private void logQueryStart(
            MasterTemplateDefinitionEntity template,
            Map<String, Object> extractedData,
            Long fromDate, Long toDate) {

        log.info("QUERY DOCUMENTS for template: {}", template.getTemplateType());
        log.info("  extractedData: {}, dates: {}-{}",
                extractedData != null, fromDate, toDate);
    }

    private boolean hasDocumentMatching(
            MasterTemplateDefinitionEntity template,
            Map<String, Object> extractedData) {

        return template.getDocumentMatchingConfig() != null && extractedData != null;
    }

    private Mono<List<StorageIndexEntity>> queryByDocumentMatching(
            MasterTemplateDefinitionEntity template,
            Map<String, Object> extractedData,
            Long postedFromDate,
            Long postedToDate) {

        try {
            JsonNode matchingNode = objectMapper.readTree(
                    template.getDocumentMatchingConfig().asString());

            if (!matchingNode.has("matchBy")) {
                log.info("  No matchBy field in document_matching_config");
                return queryBySharedFlag(template, null, postedFromDate, postedToDate);
            }

            String matchBy = matchingNode.get("matchBy").asText();

            return executeMatching(
                    matchBy, matchingNode, template, extractedData,
                    postedFromDate, postedToDate);

        } catch (Exception e) {
            log.error("Failed to parse document_matching_config: {}", e.getMessage());
            return Mono.just(Collections.emptyList());
        }
    }

    private Mono<List<StorageIndexEntity>> executeMatching(
            String matchBy,
            JsonNode matchingNode,
            MasterTemplateDefinitionEntity template,
            Map<String, Object> extractedData,
            Long postedFromDate,
            Long postedToDate) {

        switch (matchBy) {
            case "reference_key":
                return queryByReferenceKey(
                        matchingNode, template, extractedData,
                        postedFromDate, postedToDate);

            case "conditional":
                return queryByConditional(
                        matchingNode, template, extractedData,
                        postedFromDate, postedToDate);

            default:
                log.warn("Unknown matchBy: {}", matchBy);
                return Mono.just(Collections.emptyList());
        }
    }

    private Mono<List<StorageIndexEntity>> queryByReferenceKey(
            JsonNode matchingNode,
            MasterTemplateDefinitionEntity template,
            Map<String, Object> extractedData,
            Long postedFromDate,
            Long postedToDate) {

        String referenceKeyField = matchingNode.get("referenceKeyField").asText();
        String referenceKeyType = matchingNode.get("referenceKeyType").asText();

        // Validate reference_key_type is a valid configured value
        if (!referenceKeyConfig.isValid(referenceKeyType)) {
            log.error("Invalid reference_key_type '{}' in template '{}'. Allowed values: {}",
                    referenceKeyType, template.getTemplateType(), referenceKeyConfig.getAllowedTypesString());
            return Mono.error(new IllegalArgumentException(
                    "Invalid reference_key_type: '" + referenceKeyType +
                    "'. Allowed values: " + referenceKeyConfig.getAllowedTypesString()));
        }

        Object referenceKeyValue = extractedData.get(referenceKeyField);

        if (referenceKeyValue == null) {
            log.warn("Reference key '{}' not found in extracted data", referenceKeyField);
            return Mono.just(Collections.emptyList());
        }

        logReferenceKeyQuery(referenceKeyValue, referenceKeyType, template);

        return executeReferenceKeyQuery(
                referenceKeyValue.toString(), referenceKeyType,
                template, postedFromDate, postedToDate);
    }

    private Mono<List<StorageIndexEntity>> queryByConditional(
            JsonNode matchingNode,
            MasterTemplateDefinitionEntity template,
            Map<String, Object> extractedData,
            Long postedFromDate,
            Long postedToDate) {

        JsonNode conditionsNode = matchingNode.get("conditions");

        if (!matchingNode.has("referenceKeyType")) {
            log.error("referenceKeyType is required for conditional matching in template '{}'",
                    template.getTemplateType());
            return Mono.error(new IllegalArgumentException(
                    "referenceKeyType is required in document_matching_config for template: " +
                    template.getTemplateType()));
        }

        String referenceKeyType = matchingNode.get("referenceKeyType").asText();

        // Validate reference_key_type is a valid configured value
        if (!referenceKeyConfig.isValid(referenceKeyType)) {
            log.error("Invalid reference_key_type '{}' in template '{}'. Allowed values: {}",
                    referenceKeyType, template.getTemplateType(), referenceKeyConfig.getAllowedTypesString());
            return Mono.error(new IllegalArgumentException(
                    "Invalid reference_key_type: '" + referenceKeyType +
                    "'. Allowed values: " + referenceKeyConfig.getAllowedTypesString()));
        }

        if (conditionsNode == null || !conditionsNode.isArray()) {
            log.warn("No conditions array found");
            return Mono.just(Collections.emptyList());
        }

        String matchedKey = evaluateConditions(conditionsNode, extractedData);

        if (matchedKey == null) {
            log.warn("No condition matched");
            return Mono.just(Collections.emptyList());
        }

        logConditionalMatch(matchedKey, referenceKeyType, template);

        return executeReferenceKeyQuery(
                matchedKey, referenceKeyType,
                template, postedFromDate, postedToDate);
    }

    private Mono<List<StorageIndexEntity>> executeReferenceKeyQuery(
            String referenceKey,
            String referenceKeyType,
            MasterTemplateDefinitionEntity template,
            Long postedFromDate,
            Long postedToDate) {

        return storageRepository.findByReferenceKeyAndTemplateWithDateRange(
                referenceKey, referenceKeyType,
                template.getTemplateType(), template.getTemplateVersion(),
                postedFromDate, postedToDate, System.currentTimeMillis())
                .collectList()
                .map(validityService::filterByValidity)
                .doOnNext(docs -> log.info("Found {} valid documents", docs.size()));
    }

    private Mono<List<StorageIndexEntity>> queryStandardDocuments(
            MasterTemplateDefinitionEntity template,
            UUID accountId,
            Long postedFromDate,
            Long postedToDate) {

        return queryBySharedFlag(template, accountId, postedFromDate, postedToDate);
    }

    private Mono<List<StorageIndexEntity>> queryBySharedFlag(
            MasterTemplateDefinitionEntity template,
            UUID accountId,
            Long postedFromDate,
            Long postedToDate) {

        if (Boolean.TRUE.equals(template.getSharedDocumentFlag())) {
            return querySharedDocuments(template, postedFromDate, postedToDate);
        }
        return queryAccountDocuments(template, accountId, postedFromDate, postedToDate);
    }

    private Mono<List<StorageIndexEntity>> querySharedDocuments(
            MasterTemplateDefinitionEntity template,
            Long postedFromDate,
            Long postedToDate) {

        return storageRepository.findSharedDocumentsWithDateRange(
                template.getTemplateType(), template.getTemplateVersion(),
                postedFromDate, postedToDate, System.currentTimeMillis())
                .collectList()
                .map(validityService::filterByValidity)
                .doOnNext(docs -> log.debug("Found {} shared documents", docs.size()));
    }

    private Mono<List<StorageIndexEntity>> queryAccountDocuments(
            MasterTemplateDefinitionEntity template,
            UUID accountId,
            Long postedFromDate,
            Long postedToDate) {

        return storageRepository.findAccountSpecificDocumentsWithDateRange(
                accountId, template.getTemplateType(), template.getTemplateVersion(),
                postedFromDate, postedToDate, System.currentTimeMillis())
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
