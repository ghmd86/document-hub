package com.documenthub.service;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.*;
import com.documenthub.repository.MasterTemplateRepository;
import com.documenthub.repository.StorageIndexRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main service for document enquiry
 * Implements logic for retrieving account-specific and shared documents
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentEnquiryService {

    private final MasterTemplateRepository templateRepository;
    private final StorageIndexRepository storageRepository;
    private final AccountMetadataService accountMetadataService;
    private final RuleEvaluationService ruleEvaluationService;
    private final ConfigurableDataExtractionService dataExtractionService;
    private final ObjectMapper objectMapper;

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    @Value("${app.links.download.expiration-seconds:600}")
    private int linkExpirationSeconds;

    /**
     * Main entry point for document enquiry (legacy - without requestor type)
     * Defaults to CUSTOMER requestor type
     */
    public Mono<DocumentRetrievalResponse> getDocuments(DocumentListRequest request) {
        return getDocuments(request, "CUSTOMER");
    }

    /**
     * Main entry point for document enquiry with requestor type
     *
     * Implements TWO-STEP filtering based on John's clarification:
     * STEP 1: Filter templates by line_of_business (which business unit's templates)
     *         Also applies documentTypeCategoryGroup filter (maps to template_type)
     *         NEW: Also applies messageCenterDocFlag and communicationType filters
     * STEP 2: Filter by sharing_scope (who can access within those templates)
     *
     * @param request The document list request with filters
     * @param requestorType The type of requestor (CUSTOMER, AGENT, SYSTEM) for access control
     */
    public Mono<DocumentRetrievalResponse> getDocuments(DocumentListRequest request, String requestorType) {
        log.info("Processing document enquiry - customerId: {}, accountIds: {}, lineOfBusiness: {}, messageCenterDocFlag: {}, communicationType: {}, requestorType: {}",
            request.getCustomerId(),
            request.getAccountId(),
            request.getLineOfBusiness(),
            request.getMessageCenterDocFlag(),
            request.getCommunicationType(),
            requestorType);

        long startTime = System.currentTimeMillis();
        Long currentEpochTime = Instant.now().toEpochMilli();

        // Process each account ID from the request
        List<String> accountIds = request.getAccountId() != null ?
            request.getAccountId() : Collections.emptyList();

        if (accountIds.isEmpty()) {
            // If no account IDs provided, return empty result
            log.warn("No account IDs provided in request");
            return Mono.just(buildEmptyResponse(request));
        }

        // Extract template types from documentTypeCategoryGroup (maps to template_type in DB)
        List<String> requestedTemplateTypes = extractTemplateTypes(request);

        // Extract P0 filters
        Boolean messageCenterDocFlag = request.getMessageCenterDocFlag() != null ?
            request.getMessageCenterDocFlag() : true; // Default to true
        String communicationType = request.getCommunicationType() != null ?
            request.getCommunicationType().getValue() : null;

        // Extract date range filters
        Long postedFromDate = request.getPostedFromDate();
        Long postedToDate = request.getPostedToDate();

        // STEP 1: Determine line of business for template filtering
        // If not provided in request, derive from first account's metadata
        return determineLineOfBusiness(request, accountIds.get(0))
            .flatMap(lineOfBusiness -> {
                log.info("═══════════════════════════════════════════════════════════════");
                log.info("STEP 1: TEMPLATE FILTERS");
                log.info("  lineOfBusiness = '{}'", lineOfBusiness);
                log.info("  messageCenterDocFlag = {}", messageCenterDocFlag);
                log.info("  communicationType = '{}'", communicationType);
                if (!requestedTemplateTypes.isEmpty()) {
                    log.info("  templateTypes = {}", requestedTemplateTypes);
                }
                if (postedFromDate != null || postedToDate != null) {
                    log.info("  postedFromDate = {}, postedToDate = {}", postedFromDate, postedToDate);
                }
                log.info("═══════════════════════════════════════════════════════════════");

                // Load templates with all filters applied
                Flux<MasterTemplateDefinitionEntity> templateFlux;
                if (!requestedTemplateTypes.isEmpty()) {
                    // Filter by line of business, template types, AND P0 filters
                    templateFlux = templateRepository.findActiveTemplatesWithAllFilters(
                        lineOfBusiness, requestedTemplateTypes, messageCenterDocFlag, communicationType, currentEpochTime);
                } else {
                    // Filter by line of business AND P0 filters only
                    templateFlux = templateRepository.findActiveTemplatesWithFilters(
                        lineOfBusiness, messageCenterDocFlag, communicationType, currentEpochTime);
                }

                return templateFlux.collectList()
                    .flatMap(templates -> {
                        log.info("═══════════════════════════════════════════════════════════════");
                        log.info("TEMPLATES LOADED: Found {} templates after filtering",
                            templates.size());
                        log.info("═══════════════════════════════════════════════════════════════");
                        for (MasterTemplateDefinitionEntity t : templates) {
                            log.info("  Template: type={}, LOB={}, msgCenter={}, commType={}, shared={}, sharingScope={}",
                                t.getTemplateType(),
                                t.getLineOfBusiness(),
                                t.getMessageCenterDocFlag(),
                                t.getCommunicationType(),
                                t.getSharedDocumentFlag(),
                                t.getSharingScope());
                        }
                        log.info("═══════════════════════════════════════════════════════════════");
                        log.info("STEP 2: SHARING SCOPE FILTER (per template)");
                        log.info("═══════════════════════════════════════════════════════════════");

                        // STEP 2: Process documents for each account, applying sharing_scope logic
                        return Flux.fromIterable(accountIds)
                            .flatMap(accountIdStr -> processAccount(
                                UUID.fromString(accountIdStr),
                                templates,
                                request,
                                postedFromDate,
                                postedToDate,
                                requestorType
                            ))
                            .collectList()
                            .map(documentLists -> {
                                // Flatten all documents
                                List<DocumentDetailsNode> allDocuments = documentLists.stream()
                                    .flatMap(List::stream)
                                    .collect(Collectors.toList());

                                log.info("Total documents found: {}", allDocuments.size());

                                // Apply pagination
                                int pageSize = determinePageSize(request.getPageSize());
                                int pageNumber = determinePageNumber(request.getPageNumber());

                                List<DocumentDetailsNode> paginatedDocs = paginateDocuments(
                                    allDocuments,
                                    pageNumber,
                                    pageSize
                                );

                                // Build response
                                long processingTime = System.currentTimeMillis() - startTime;

                                return buildResponse(
                                    paginatedDocs,
                                    allDocuments.size(),
                                    pageNumber,
                                    pageSize,
                                    processingTime
                                );
                            });
                    });
            })
            .doOnError(e -> log.error("Error processing document enquiry", e))
            .onErrorResume(e -> Mono.just(buildErrorResponse(e)));
    }

    /**
     * Determine line of business for template filtering.
     *
     * Priority:
     * 1. If provided in request, use that value
     * 2. Otherwise, derive from account metadata
     */
    private Mono<String> determineLineOfBusiness(DocumentListRequest request, String firstAccountId) {
        // Check if lineOfBusiness is provided in request
        // Note: request.getLineOfBusiness() will be available after regenerating from OpenAPI
        String requestLineOfBusiness = getLineOfBusinessFromRequest(request);

        if (requestLineOfBusiness != null && !requestLineOfBusiness.isEmpty()) {
            log.debug("Using lineOfBusiness from request: {}", requestLineOfBusiness);
            return Mono.just(requestLineOfBusiness);
        }

        // Derive from first account's metadata
        log.debug("lineOfBusiness not in request, deriving from account: {}", firstAccountId);
        return accountMetadataService.getAccountMetadata(UUID.fromString(firstAccountId))
            .map(metadata -> {
                String lob = metadata.getLineOfBusiness();
                if (lob == null || lob.isEmpty()) {
                    // Fall back to deriving from account type
                    lob = accountMetadataService.deriveLineOfBusiness(metadata.getAccountType());
                }
                log.debug("Derived lineOfBusiness from account: {}", lob);
                return lob;
            })
            .defaultIfEmpty(AccountMetadataService.LOB_CREDIT_CARD);
    }

    /**
     * Extract lineOfBusiness from request using reflection.
     * This allows the code to work before and after regenerating the OpenAPI model.
     */
    private String getLineOfBusinessFromRequest(DocumentListRequest request) {
        try {
            java.lang.reflect.Method method = request.getClass().getMethod("getLineOfBusiness");
            Object result = method.invoke(request);
            return result != null ? result.toString() : null;
        } catch (NoSuchMethodException e) {
            // Method doesn't exist yet (model not regenerated)
            log.debug("getLineOfBusiness() method not found - model needs regeneration");
            return null;
        } catch (Exception e) {
            log.warn("Error getting lineOfBusiness from request: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract template types from documentTypeCategoryGroup in the request.
     * The documentTypes in the API request map directly to template_type in the database.
     *
     * Example request structure:
     * {
     *   "documentTypeCategoryGroup": [
     *     { "category": "Statement", "documentTypes": ["Statement", "SavingsStatement"] },
     *     { "category": "PrivacyPolicy", "documentTypes": ["PrivacyNotice"] }
     *   ]
     * }
     *
     * This extracts all documentTypes: ["Statement", "SavingsStatement", "PrivacyNotice"]
     * which map to template_type values in master_template_definition table.
     */
    private List<String> extractTemplateTypes(DocumentListRequest request) {
        List<DocumentCategoryGroup> categoryGroups = request.getDocumentTypeCategoryGroup();

        if (categoryGroups == null || categoryGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> templateTypes = new ArrayList<>();
        for (DocumentCategoryGroup group : categoryGroups) {
            if (group.getDocumentTypes() != null) {
                // documentTypes is List<String> - each string maps to template_type
                for (String docType : group.getDocumentTypes()) {
                    if (docType != null && !docType.isEmpty()) {
                        templateTypes.add(docType);
                    }
                }
            }
        }

        log.debug("Extracted template types from documentTypeCategoryGroup: {}", templateTypes);
        return templateTypes;
    }

    /**
     * Process documents for a single account
     */
    private Mono<List<DocumentDetailsNode>> processAccount(
        UUID accountId,
        List<MasterTemplateDefinitionEntity> templates,
        DocumentListRequest request,
        Long postedFromDate,
        Long postedToDate,
        String requestorType
    ) {
        log.debug("Processing account: {}", accountId);

        // Get account metadata
        return accountMetadataService.getAccountMetadata(accountId)
            .flatMap(accountMetadata -> {
                log.debug("Account metadata retrieved: type={}, segment={}, region={}",
                    accountMetadata.getAccountType(),
                    accountMetadata.getCustomerSegment(),
                    accountMetadata.getRegion());

                // Build request context
                Map<String, Object> requestContext = buildRequestContext(request);

                // Process each template
                return Flux.fromIterable(templates)
                    .flatMap(template -> processTemplate(
                        template,
                        accountId,
                        accountMetadata,
                        requestContext,
                        request,
                        postedFromDate,
                        postedToDate,
                        requestorType
                    ))
                    .collectList()
                    .map(documentLists -> documentLists.stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));
            })
            .defaultIfEmpty(Collections.emptyList());
    }

    /**
     * Process a single template
     */
    private Mono<List<DocumentDetailsNode>> processTemplate(
        MasterTemplateDefinitionEntity template,
        UUID accountId,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext,
        DocumentListRequest request,
        Long postedFromDate,
        Long postedToDate,
        String requestorType
    ) {
        log.info("───────────────────────────────────────────────────────────────");
        log.info("PROCESSING TEMPLATE: {}", template.getTemplateType());
        log.info("  sharedDocumentFlag = {}", template.getSharedDocumentFlag());
        log.info("  sharingScope = '{}'", template.getSharingScope());
        log.info("  hasDataExtractionConfig = {}", template.getDataExtractionConfig() != null);

        boolean isShared = Boolean.TRUE.equals(template.getSharedDocumentFlag());
        boolean isCustomRules = "CUSTOM_RULES".equalsIgnoreCase(template.getSharingScope());
        boolean hasConfig = template.getDataExtractionConfig() != null;

        log.info("  CUSTOM_RULES check: isShared={}, isCustomRules={}, hasConfig={}",
            isShared, isCustomRules, hasConfig);
        log.info("───────────────────────────────────────────────────────────────");

        // For CUSTOM_RULES sharing scope, extract data first
        if (isShared && isCustomRules && hasConfig) {

            log.info(">>> Template {} QUALIFIES for CUSTOM_RULES - starting data extraction", template.getTemplateType());

            return dataExtractionService.extractData(template.getDataExtractionConfig(), request)
                .flatMap(extractedData -> {
                    log.info("═══════════════════════════════════════════════════════════════");
                    log.info("DATA EXTRACTION COMPLETED for template: {}", template.getTemplateType());
                    log.info("  Fields extracted: {}", extractedData.size());
                    for (Map.Entry<String, Object> entry : extractedData.entrySet()) {
                        log.info("    {} = {}", entry.getKey(), entry.getValue());
                    }
                    log.info("═══════════════════════════════════════════════════════════════");

                    // Merge extracted data into request context
                    Map<String, Object> enhancedContext = new HashMap<>(requestContext);
                    enhancedContext.putAll(extractedData);

                    // Determine if we can access this template with enhanced context
                    boolean canAccess = canAccessTemplate(template, accountMetadata, enhancedContext);

                    if (!canAccess) {
                        log.warn("Template {} not accessible after data extraction, skipping",
                            template.getTemplateType());
                        return Mono.just(Collections.<DocumentDetailsNode>emptyList());
                    }

                    log.info("Template {} is accessible - querying documents with extracted data",
                        template.getTemplateType());

                    // Query documents (pass extracted data for document matching)
                    return queryDocuments(template, accountId, accountMetadata, extractedData, postedFromDate, postedToDate)
                        .doOnNext(docs -> log.info("queryDocuments returned {} documents for template {}",
                            docs.size(), template.getTemplateType()))
                        .map(docs -> convertToDocumentDetailsNodes(docs, template, requestorType));
                })
                .onErrorResume(e -> {
                    log.error("Data extraction failed for template {}: {} - Skipping template",
                        template.getTemplateType(), e.getMessage());
                    return Mono.just(Collections.<DocumentDetailsNode>emptyList());
                });
        } else {
            // Standard processing for non-CUSTOM_RULES templates
            boolean canAccess = canAccessTemplate(template, accountMetadata, requestContext);

            if (!canAccess) {
                log.debug("Template {} not accessible, skipping", template.getTemplateType());
                return Mono.just(Collections.<DocumentDetailsNode>emptyList());
            }

            // Query documents (no extracted data)
            return queryDocuments(template, accountId, accountMetadata, null, postedFromDate, postedToDate)
                .map(docs -> convertToDocumentDetailsNodes(docs, template, requestorType));
        }
    }

    /**
     * Check if account can access template based on sharing_scope.
     *
     * Sharing Scope Values (after removing redundant ACCOUNT_TYPE):
     * - NULL: Account-specific documents (only owning account can access)
     * - ALL: Everyone can access (no eligibility check needed)
     * - CUSTOM_RULES: Complex eligibility rules with optional data extraction
     *
     * Note: ACCOUNT_TYPE was removed as it's redundant with line_of_business filtering.
     * Business unit filtering is now handled in STEP 1 via line_of_business field.
     */
    private boolean canAccessTemplate(
        MasterTemplateDefinitionEntity template,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        // Non-shared templates (sharing_scope = NULL) are always accessible if queried by account
        if (Boolean.FALSE.equals(template.getSharedDocumentFlag())) {
            return true;
        }

        // Shared templates - check sharing scope
        String sharingScope = template.getSharingScope();

        if (sharingScope == null || sharingScope.isEmpty()) {
            // NULL/empty sharing_scope = account-specific, accessible by owning account
            return true;
        } else if ("ALL".equalsIgnoreCase(sharingScope)) {
            // Shared with all - no eligibility check needed
            return true;
        } else if ("CUSTOM_RULES".equalsIgnoreCase(sharingScope)) {
            // Custom rules evaluation from template config
            return evaluateTemplateEligibility(template, accountMetadata, requestContext);
        }

        // Unknown sharing_scope - log warning and deny access
        log.warn("Unknown sharing_scope '{}' for template {}, denying access",
            sharingScope, template.getTemplateType());
        return false;
    }

    /**
     * Evaluate eligibility rules from template config
     */
    private boolean evaluateTemplateEligibility(
        MasterTemplateDefinitionEntity template,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        if (template.getTemplateConfig() == null) {
            log.debug("No template config defined for template: {}, allowing access",
                template.getTemplateType());
            return true;
        }

        try {
            TemplateConfig templateConfig = objectMapper.readValue(
                template.getTemplateConfig().asString(),
                TemplateConfig.class
            );

            return ruleEvaluationService.evaluateEligibility(
                templateConfig,
                accountMetadata,
                requestContext
            );
        } catch (Exception e) {
            log.error("Failed to parse template config for template: {}",
                template.getTemplateType(), e);
            return false;
        }
    }

    /**
     * Query documents from storage index
     */
    private Mono<List<StorageIndexEntity>> queryDocuments(
        MasterTemplateDefinitionEntity template,
        UUID accountId,
        AccountMetadata accountMetadata,
        Map<String, Object> extractedData,
        Long postedFromDate,
        Long postedToDate
    ) {
        log.info("───────────────────────────────────────────────────────────────");
        log.info("QUERY DOCUMENTS for template: {}", template.getTemplateType());
        log.info("  hasDataExtractionConfig: {}", template.getDataExtractionConfig() != null);
        log.info("  hasExtractedData: {}", extractedData != null);
        log.info("  postedFromDate: {}, postedToDate: {}", postedFromDate, postedToDate);
        log.info("───────────────────────────────────────────────────────────────");

        // Check for document matching configuration
        if (template.getDataExtractionConfig() != null && extractedData != null) {
            try {
                // Parse data extraction config to check for documentMatching
                com.fasterxml.jackson.databind.JsonNode configNode =
                    objectMapper.readTree(template.getDataExtractionConfig().asString());

                log.info("  Checking for documentMatching config...");
                log.info("  Config has documentMatching: {}", configNode.has("documentMatching"));

                if (configNode.has("documentMatching")) {
                    com.fasterxml.jackson.databind.JsonNode matchingNode = configNode.get("documentMatching");
                    String matchBy = matchingNode.get("matchBy").asText();
                    log.info("  matchBy = {}", matchBy);

                    if ("reference_key".equals(matchBy)) {
                        String referenceKeyField = matchingNode.get("referenceKeyField").asText();
                        String referenceKeyType = matchingNode.get("referenceKeyType").asText();
                        log.info("  referenceKeyField = {}", referenceKeyField);
                        log.info("  referenceKeyType = {}", referenceKeyType);

                        // Get the reference key value from extracted data
                        Object referenceKeyValue = extractedData.get(referenceKeyField);
                        log.info("  referenceKeyValue from extractedData = {}", referenceKeyValue);

                        if (referenceKeyValue != null) {
                            log.info("═══════════════════════════════════════════════════════════════");
                            log.info("DOCUMENT MATCHING QUERY:");
                            log.info("  reference_key = '{}'", referenceKeyValue);
                            log.info("  reference_key_type = '{}'", referenceKeyType);
                            log.info("  template_type = '{}'", template.getTemplateType());
                            log.info("  template_version = {}", template.getTemplateVersion());
                            log.info("═══════════════════════════════════════════════════════════════");

                            // Query by reference key with date range
                            return storageRepository.findByReferenceKeyAndTemplateWithDateRange(
                                referenceKeyValue.toString(),
                                referenceKeyType,
                                template.getTemplateType(),
                                template.getTemplateVersion(),
                                postedFromDate,
                                postedToDate
                            ).collectList()
                            .map(this::filterByValidity)
                            .doOnNext(docs -> log.info("findByReferenceKeyAndTemplate returned {} valid documents", docs.size()));
                        } else {
                            log.warn("Reference key field '{}' not found in extracted data - SKIPPING template: {}",
                                referenceKeyField, template.getTemplateType());
                            // Return empty list - don't fall through to shared documents
                            return Mono.just(Collections.<StorageIndexEntity>emptyList());
                        }
                    } else if ("conditional".equals(matchBy)) {
                        // Conditional document matching based on comparison operators
                        log.info("  Processing CONDITIONAL document matching");

                        com.fasterxml.jackson.databind.JsonNode conditionsNode = matchingNode.get("conditions");
                        String referenceKeyType = matchingNode.has("referenceKeyType") ?
                            matchingNode.get("referenceKeyType").asText() : "CONDITION_MATCH";

                        if (conditionsNode != null && conditionsNode.isArray()) {
                            // Evaluate conditions in order (first match wins)
                            String matchedReferenceKey = evaluateConditions(conditionsNode, extractedData);

                            if (matchedReferenceKey != null) {
                                log.info("═══════════════════════════════════════════════════════════════");
                                log.info("CONDITIONAL MATCHING - MATCHED:");
                                log.info("  reference_key = '{}'", matchedReferenceKey);
                                log.info("  reference_key_type = '{}'", referenceKeyType);
                                log.info("  template_type = '{}'", template.getTemplateType());
                                log.info("═══════════════════════════════════════════════════════════════");

                                return storageRepository.findByReferenceKeyAndTemplateWithDateRange(
                                    matchedReferenceKey,
                                    referenceKeyType,
                                    template.getTemplateType(),
                                    template.getTemplateVersion(),
                                    postedFromDate,
                                    postedToDate
                                ).collectList()
                                .map(this::filterByValidity)
                                .doOnNext(docs -> log.info("Conditional match returned {} valid documents", docs.size()));
                            } else {
                                log.warn("No condition matched for extracted data - SKIPPING template: {}",
                                    template.getTemplateType());
                                // Return empty list - don't fall through to shared documents
                                return Mono.just(Collections.<StorageIndexEntity>emptyList());
                            }
                        } else {
                            log.warn("No conditions array found - SKIPPING template: {}", template.getTemplateType());
                            return Mono.just(Collections.<StorageIndexEntity>emptyList());
                        }
                    }
                } else {
                    log.info("  No documentMatching config found in data extraction config");
                }
            } catch (Exception e) {
                log.error("Failed to parse documentMatching config - SKIPPING template {}: {}",
                    template.getTemplateType(), e.getMessage(), e);
                // Return empty list instead of falling through
                return Mono.just(Collections.<StorageIndexEntity>emptyList());
            }
        }

        // Standard document queries with date range filtering
        if (Boolean.TRUE.equals(template.getSharedDocumentFlag())) {
            // Query shared documents with date range
            return storageRepository.findSharedDocumentsWithDateRange(
                template.getTemplateType(),
                template.getTemplateVersion(),
                postedFromDate,
                postedToDate
            ).collectList()
            .map(this::filterByValidity)
            .doOnNext(docs -> log.debug("findSharedDocuments returned {} valid documents for template {}",
                docs.size(), template.getTemplateType()));
        } else {
            // Query account-specific documents with date range
            return storageRepository.findAccountSpecificDocumentsWithDateRange(
                accountId,
                template.getTemplateType(),
                template.getTemplateVersion(),
                postedFromDate,
                postedToDate
            ).collectList()
            .map(this::filterByValidity)
            .doOnNext(docs -> log.debug("findAccountSpecificDocuments returned {} valid documents for account {}",
                docs.size(), accountId));
        }
    }

    /**
     * Evaluate conditional rules to determine which reference key to use
     * Conditions are evaluated in order; first matching condition wins
     *
     * Supported operators: >=, >, <=, <, ==, !=
     */
    private String evaluateConditions(
        com.fasterxml.jackson.databind.JsonNode conditionsNode,
        Map<String, Object> extractedData
    ) {
        for (com.fasterxml.jackson.databind.JsonNode condition : conditionsNode) {
            String field = condition.get("field").asText();
            String operator = condition.get("operator").asText();
            com.fasterxml.jackson.databind.JsonNode thresholdNode = condition.get("value");
            String referenceKey = condition.get("referenceKey").asText();

            Object fieldValue = extractedData.get(field);

            log.info("  Evaluating condition: {} {} {} → referenceKey={}",
                field, operator, thresholdNode, referenceKey);
            log.info("    Field value from extracted data: {} (type: {})",
                fieldValue, fieldValue != null ? fieldValue.getClass().getSimpleName() : "null");

            if (fieldValue == null) {
                log.info("    Field '{}' not found in extracted data, skipping condition", field);
                continue;
            }

            boolean matches = evaluateSingleCondition(fieldValue, operator, thresholdNode);
            log.info("    Condition result: {}", matches);

            if (matches) {
                log.info("  ✓ Condition MATCHED: {} {} {} → returning referenceKey={}",
                    field, operator, thresholdNode, referenceKey);
                return referenceKey;
            }
        }

        log.warn("  No conditions matched");
        return null;
    }

    /**
     * Evaluate a single condition with the given operator
     */
    private boolean evaluateSingleCondition(
        Object fieldValue,
        String operator,
        com.fasterxml.jackson.databind.JsonNode thresholdNode
    ) {
        try {
            // Handle numeric comparisons
            if (thresholdNode.isNumber()) {
                double threshold = thresholdNode.asDouble();
                double value;

                if (fieldValue instanceof Number) {
                    value = ((Number) fieldValue).doubleValue();
                } else {
                    value = Double.parseDouble(fieldValue.toString());
                }

                log.debug("    Numeric comparison: {} {} {}", value, operator, threshold);

                switch (operator) {
                    case ">=":
                        return value >= threshold;
                    case ">":
                        return value > threshold;
                    case "<=":
                        return value <= threshold;
                    case "<":
                        return value < threshold;
                    case "==":
                        return value == threshold;
                    case "!=":
                        return value != threshold;
                    default:
                        log.warn("Unknown operator: {}", operator);
                        return false;
                }
            }
            // Handle string comparisons
            else if (thresholdNode.isTextual()) {
                String threshold = thresholdNode.asText();
                String value = fieldValue.toString();

                log.debug("    String comparison: '{}' {} '{}'", value, operator, threshold);

                switch (operator) {
                    case "==":
                        return value.equals(threshold);
                    case "!=":
                        return !value.equals(threshold);
                    case "contains":
                        return value.contains(threshold);
                    case "startsWith":
                        return value.startsWith(threshold);
                    case "endsWith":
                        return value.endsWith(threshold);
                    default:
                        log.warn("Unsupported string operator: {}", operator);
                        return false;
                }
            }
            // Handle boolean comparisons
            else if (thresholdNode.isBoolean()) {
                boolean threshold = thresholdNode.asBoolean();
                boolean value = Boolean.parseBoolean(fieldValue.toString());

                log.debug("    Boolean comparison: {} {} {}", value, operator, threshold);

                switch (operator) {
                    case "==":
                        return value == threshold;
                    case "!=":
                        return value != threshold;
                    default:
                        log.warn("Unsupported boolean operator: {}", operator);
                        return false;
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse value '{}' as number: {}", fieldValue, e.getMessage());
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", e.getMessage(), e);
        }

        return false;
    }

    /**
     * Convert storage entities to API response nodes
     * Legacy method without requestor type - defaults to CUSTOMER
     */
    private List<DocumentDetailsNode> convertToDocumentDetailsNodes(
        List<StorageIndexEntity> entities,
        MasterTemplateDefinitionEntity template
    ) {
        return convertToDocumentDetailsNodes(entities, template, "CUSTOMER");
    }

    /**
     * Convert storage entities to API response nodes with access control
     * Builds HATEOAS links based on requestor type and template access_control
     */
    private List<DocumentDetailsNode> convertToDocumentDetailsNodes(
        List<StorageIndexEntity> entities,
        MasterTemplateDefinitionEntity template,
        String requestorType
    ) {
        // Get permitted actions for this requestor type
        List<String> permittedActions = getPermittedActions(template, requestorType);
        log.debug("Permitted actions for requestorType={}: {}", requestorType, permittedActions);

        return entities.stream()
            .map(entity -> {
                DocumentDetailsNode node = new DocumentDetailsNode();
                node.setDocumentId(entity.getStorageDocumentKey() != null ?
                    entity.getStorageDocumentKey().toString() : null);
                node.setDisplayName(entity.getFileName());
                node.setDescription(template.getTemplateDescription());
                node.setDocumentType(template.getTemplateType());
                node.setCategory(template.getTemplateCategory());
                node.setDatePosted(entity.getDocCreationDate());

                // Set lineOfBusiness from template
                if (template.getLineOfBusiness() != null && !template.getLineOfBusiness().isEmpty()) {
                    node.setLineOfBusiness(template.getLineOfBusiness());
                }

                // Add metadata
                if (entity.getDocMetadata() != null) {
                    try {
                        List<MetadataNode> metadataNodes = new ArrayList<>();
                        com.fasterxml.jackson.databind.JsonNode metadataJson =
                            objectMapper.readTree(entity.getDocMetadata().asString());
                        metadataJson.fields().forEachRemaining(field -> {
                            MetadataNode metaNode = new MetadataNode();
                            metaNode.setKey(field.getKey());
                            metaNode.setValue(field.getValue().asText());
                            metadataNodes.add(metaNode);
                        });
                        node.setMetadata(metadataNodes);
                    } catch (Exception e) {
                        log.warn("Failed to parse doc_metadata for document {}: {}",
                            entity.getStorageIndexId(), e.getMessage());
                    }
                }

                // Build HATEOAS links based on access control
                // IMPORTANT: Never include delete link in enquiry response (per John's requirement)
                Links links = buildLinksForDocument(entity, permittedActions);
                node.setLinks(links);

                return node;
            })
            .collect(Collectors.toList());
    }

    /**
     * Build HATEOAS links for a document based on permitted actions.
     * IMPORTANT: Delete link is NEVER included in enquiry response (per John's Day 1 feedback).
     */
    private Links buildLinksForDocument(StorageIndexEntity document, List<String> permittedActions) {
        Links links = new Links();

        // Only add download link if permitted (View or Download action allowed)
        if (permittedActions.contains("Download") || permittedActions.contains("View")) {
            LinksDownload download = new LinksDownload();
            download.setHref("/documents/" + document.getStorageDocumentKey());
            download.setType("GET");
            download.setRel("download");
            download.setTitle("Download this document");
            download.setResponseTypes(Arrays.asList("application/pdf", "application/octet-stream"));

            // Add expiration timestamp (configurable, default 10 minutes)
            long expiresAt = Instant.now().plusSeconds(linkExpirationSeconds).getEpochSecond();
            download.setExpiresAt(expiresAt);

            links.setDownload(download);
        }

        // NEVER add delete link in enquiry response (per John's requirement)
        // Delete action should only be available through direct DELETE /documents/{documentId} endpoint

        return links;
    }

    /**
     * Get permitted actions for a requestor type based on template access_control.
     * Maps X-requestor-type header to access_control roles.
     */
    private List<String> getPermittedActions(MasterTemplateDefinitionEntity template, String requestorType) {
        if (template.getAccessControl() == null) {
            // No restrictions - return default based on requestor type
            return getDefaultActionsForRequestorType(requestorType);
        }

        try {
            // Parse access_control JSON array
            com.fasterxml.jackson.databind.JsonNode accessControlNode =
                objectMapper.readTree(template.getAccessControl().asString());

            // Map X-requestor-type to access_control role
            String role = mapRequestorTypeToRole(requestorType);

            // Find matching role in access_control array
            if (accessControlNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode entry : accessControlNode) {
                    String entryRole = entry.has("role") ? entry.get("role").asText() : null;
                    if (role.equalsIgnoreCase(entryRole)) {
                        List<String> actions = new ArrayList<>();
                        com.fasterxml.jackson.databind.JsonNode actionsNode = entry.get("actions");
                        if (actionsNode != null && actionsNode.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode action : actionsNode) {
                                actions.add(action.asText());
                            }
                        }
                        return actions;
                    }
                }
            }

            // Role not found in access_control - return defaults
            log.debug("Role '{}' not found in access_control, using defaults", role);
            return getDefaultActionsForRequestorType(requestorType);

        } catch (Exception e) {
            log.warn("Failed to parse access_control for template {}: {}",
                template.getTemplateType(), e.getMessage());
            return getDefaultActionsForRequestorType(requestorType);
        }
    }

    /**
     * Map X-requestor-type header to access_control role
     */
    private String mapRequestorTypeToRole(String requestorType) {
        if (requestorType == null) {
            return "customer"; // Default to most restrictive
        }

        switch (requestorType.toUpperCase()) {
            case "CUSTOMER":
                return "customer";
            case "AGENT":
                return "agent";
            case "SYSTEM":
                return "system";
            default:
                return "customer";
        }
    }

    /**
     * Get default actions when no access_control defined
     */
    private List<String> getDefaultActionsForRequestorType(String requestorType) {
        if (requestorType == null) {
            return Arrays.asList("View", "Download");
        }

        switch (requestorType.toUpperCase()) {
            case "CUSTOMER":
                return Arrays.asList("View", "Download");
            case "AGENT":
                return Arrays.asList("View", "Download");
            case "SYSTEM":
                return Arrays.asList("View", "Update", "Delete", "Download");
            default:
                return Arrays.asList("View", "Download");
        }
    }

    /**
     * Build request context from DocumentListRequest
     */
    private Map<String, Object> buildRequestContext(DocumentListRequest request) {
        Map<String, Object> context = new HashMap<>();

        if (request.getReferenceKey() != null) {
            context.put("referenceKey", request.getReferenceKey());
        }
        if (request.getReferenceKeyType() != null) {
            context.put("referenceKeyType", request.getReferenceKeyType());
        }

        return context;
    }

    /**
     * Determine page size with limits
     */
    private int determinePageSize(BigDecimal requestedPageSize) {
        if (requestedPageSize == null) {
            return defaultPageSize;
        }

        int pageSize = requestedPageSize.intValue();
        if (pageSize <= 0) {
            return defaultPageSize;
        }

        return Math.min(pageSize, maxPageSize);
    }

    /**
     * Determine page number
     */
    private int determinePageNumber(BigDecimal requestedPageNumber) {
        if (requestedPageNumber == null) {
            return 0;
        }

        int pageNumber = requestedPageNumber.intValue();
        return Math.max(0, pageNumber);
    }

    /**
     * Apply pagination to document list
     */
    private List<DocumentDetailsNode> paginateDocuments(
        List<DocumentDetailsNode> documents,
        int pageNumber,
        int pageSize
    ) {
        int startIndex = pageNumber * pageSize;

        if (startIndex >= documents.size()) {
            return Collections.emptyList();
        }

        int endIndex = Math.min(startIndex + pageSize, documents.size());

        return documents.subList(startIndex, endIndex);
    }

    /**
     * Build successful response
     */
    private DocumentRetrievalResponse buildResponse(
        List<DocumentDetailsNode> documents,
        int totalDocuments,
        int pageNumber,
        int pageSize,
        long processingTime
    ) {
        DocumentRetrievalResponse response = new DocumentRetrievalResponse();
        response.setDocumentList(documents);

        // Pagination
        PaginationResponse pagination = new PaginationResponse();
        pagination.setPageSize(pageSize);
        pagination.setPageNumber(pageNumber);
        pagination.setTotalItems(totalDocuments);
        pagination.setTotalPages((int) Math.ceil((double) totalDocuments / pageSize));
        response.setPagination(pagination);

        // HATEOAS links
        response.setLinks(new DocumentRetrievalResponseLinks());

        log.info("Response built: {} documents, page {}/{}, processing time: {}ms",
            documents.size(),
            pageNumber + 1,
            pagination.getTotalPages(),
            processingTime);

        return response;
    }

    /**
     * Build empty response
     */
    private DocumentRetrievalResponse buildEmptyResponse(DocumentListRequest request) {
        return buildResponse(Collections.emptyList(), 0, 0, defaultPageSize, 0);
    }

    /**
     * Build error response
     */
    private DocumentRetrievalResponse buildErrorResponse(Throwable error) {
        log.error("Building error response: {}", error.getMessage());
        return buildResponse(Collections.emptyList(), 0, 0, defaultPageSize, 0);
    }

    /**
     * Filter documents by validity period from doc_metadata
     * Checks for valid_from/validFrom and valid_until/validUntil fields
     */
    private List<StorageIndexEntity> filterByValidity(List<StorageIndexEntity> documents) {
        LocalDate today = LocalDate.now();

        return documents.stream()
            .filter(doc -> isDocumentValid(doc, today))
            .collect(Collectors.toList());
    }

    /**
     * Check if a document is currently valid based on its doc_metadata
     * Supports multiple field naming conventions:
     * - Start: valid_from, validFrom, effective_date, effectiveDate
     * - End: valid_until, validUntil, expiry_date, expiryDate
     *
     * If no validity fields are present, the document is considered valid
     */
    private boolean isDocumentValid(StorageIndexEntity document, LocalDate today) {
        if (document.getDocMetadata() == null) {
            return true; // No metadata = always valid
        }

        try {
            com.fasterxml.jackson.databind.JsonNode metadata =
                objectMapper.readTree(document.getDocMetadata().asString());

            // Check start validity (valid_from, validFrom, effective_date, effectiveDate)
            LocalDate validFrom = extractDate(metadata, "valid_from", "validFrom", "effective_date", "effectiveDate");
            if (validFrom != null && today.isBefore(validFrom)) {
                log.debug("Document {} not yet valid (valid_from: {}, today: {})",
                    document.getStorageIndexId(), validFrom, today);
                return false;
            }

            // Check end validity (valid_until, validUntil, expiry_date, expiryDate)
            LocalDate validUntil = extractDate(metadata, "valid_until", "validUntil", "expiry_date", "expiryDate");
            if (validUntil != null && today.isAfter(validUntil)) {
                log.debug("Document {} has expired (valid_until: {}, today: {})",
                    document.getStorageIndexId(), validUntil, today);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("Failed to parse doc_metadata for validity check on document {}: {}",
                document.getStorageIndexId(), e.getMessage());
            return true; // On error, assume valid
        }
    }

    /**
     * Extract a date from metadata, checking multiple possible field names
     */
    private LocalDate extractDate(com.fasterxml.jackson.databind.JsonNode metadata, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (metadata.has(fieldName) && !metadata.get(fieldName).isNull()) {
                String dateStr = metadata.get(fieldName).asText();
                LocalDate date = parseDate(dateStr);
                if (date != null) {
                    return date;
                }
            }
        }
        return null;
    }

    /**
     * Parse a date string, supporting multiple formats:
     * - ISO date: 2024-01-15
     * - US format: 01/15/2024
     * - Epoch millis as string
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Try ISO format (YYYY-MM-DD)
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {}

        // Try US format (MM/dd/yyyy)
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (DateTimeParseException ignored) {}

        // Try epoch millis
        try {
            long epochMillis = Long.parseLong(dateStr);
            return Instant.ofEpochMilli(epochMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        } catch (NumberFormatException ignored) {}

        log.warn("Could not parse date string: {}", dateStr);
        return null;
    }
}
