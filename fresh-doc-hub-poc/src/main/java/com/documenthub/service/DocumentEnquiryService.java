package com.documenthub.service;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.*;
import com.documenthub.repository.MasterTemplateRepository;
import com.documenthub.repository.StorageIndexRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for document enquiry
 * Implements logic for retrieving account-specific and shared documents
 */
@Service
@Slf4j
public class DocumentEnquiryService {

    @Autowired
    private MasterTemplateRepository templateRepository;

    @Autowired
    private StorageIndexRepository storageRepository;

    @Autowired
    private AccountMetadataService accountMetadataService;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private ConfigurableDataExtractionService dataExtractionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    /**
     * Main entry point for document enquiry
     */
    public Mono<DocumentRetrievalResponse> getDocuments(DocumentListRequest request) {
        log.info("Processing document enquiry - customerId: {}, accountIds: {}",
            request.getCustomerId(),
            request.getAccountId());

        long startTime = System.currentTimeMillis();
        Long currentEpochTime = Instant.now().toEpochMilli();

        // Get active templates
        return templateRepository.findActiveTemplates(currentEpochTime)
            .collectList()
            .flatMap(templates -> {
                log.debug("Found {} active templates", templates.size());

                // Process each account ID from the request
                List<String> accountIds = request.getAccountId() != null ?
                    request.getAccountId() : Collections.emptyList();

                if (accountIds.isEmpty()) {
                    // If no account IDs provided, return empty result
                    log.warn("No account IDs provided in request");
                    return Mono.just(buildEmptyResponse(request));
                }

                // Process documents for each account
                return Flux.fromIterable(accountIds)
                    .flatMap(accountIdStr -> processAccount(
                        UUID.fromString(accountIdStr),
                        templates,
                        request
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
            })
            .doOnError(e -> log.error("Error processing document enquiry", e))
            .onErrorResume(e -> Mono.just(buildErrorResponse(e)));
    }

    /**
     * Process documents for a single account
     */
    private Mono<List<DocumentDetailsNode>> processAccount(
        UUID accountId,
        List<MasterTemplateDefinitionEntity> templates,
        DocumentListRequest request
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
                        request
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
        DocumentListRequest request
    ) {
        log.debug("Processing template: {} (shared={}, sharingScope={})",
            template.getTemplateType(),
            template.getSharedDocumentFlag(),
            template.getSharingScope());

        // For CUSTOM_RULES sharing scope, extract data first
        if (Boolean.TRUE.equals(template.getSharedDocumentFlag()) &&
            "CUSTOM_RULES".equalsIgnoreCase(template.getSharingScope()) &&
            template.getDataExtractionConfig() != null) {

            log.info("Template {} uses CUSTOM_RULES - extracting additional data", template.getTemplateType());

            return dataExtractionService.extractData(template.getDataExtractionConfig(), request)
                .flatMap(extractedData -> {
                    log.info("Data extraction completed for template {} - {} fields extracted",
                        template.getTemplateType(), extractedData.size());

                    // Merge extracted data into request context
                    Map<String, Object> enhancedContext = new HashMap<>(requestContext);
                    enhancedContext.putAll(extractedData);

                    // Determine if we can access this template with enhanced context
                    boolean canAccess = canAccessTemplate(template, accountMetadata, enhancedContext);

                    if (!canAccess) {
                        log.debug("Template {} not accessible after data extraction, skipping",
                            template.getTemplateType());
                        return Mono.just(Collections.<DocumentDetailsNode>emptyList());
                    }

                    // Query documents
                    return queryDocuments(template, accountId, accountMetadata)
                        .map(docs -> convertToDocumentDetailsNodes(docs, template));
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

            // Query documents
            return queryDocuments(template, accountId, accountMetadata)
                .map(docs -> convertToDocumentDetailsNodes(docs, template));
        }
    }

    /**
     * Check if account can access template
     */
    private boolean canAccessTemplate(
        MasterTemplateDefinitionEntity template,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        // Non-shared templates are always accessible if queried by account
        if (Boolean.FALSE.equals(template.getSharedDocumentFlag())) {
            return true;
        }

        // Shared templates - check sharing scope
        String sharingScope = template.getSharingScope();

        if ("ALL".equalsIgnoreCase(sharingScope)) {
            // Shared with all
            return true;
        } else if ("ACCOUNT_TYPE".equalsIgnoreCase(sharingScope)) {
            // Shared by account type - check access control rules
            return evaluateAccessControl(template, accountMetadata, requestContext);
        } else if ("CUSTOM_RULES".equalsIgnoreCase(sharingScope)) {
            // Custom rules evaluation
            return evaluateAccessControl(template, accountMetadata, requestContext);
        }

        return false;
    }

    /**
     * Evaluate access control rules
     */
    private boolean evaluateAccessControl(
        MasterTemplateDefinitionEntity template,
        AccountMetadata accountMetadata,
        Map<String, Object> requestContext
    ) {
        if (template.getAccessControl() == null) {
            return true;
        }

        try {
            AccessControl accessControl = objectMapper.readValue(
                template.getAccessControl().asString(),
                AccessControl.class
            );

            return ruleEvaluationService.evaluateEligibility(
                accessControl,
                accountMetadata,
                requestContext
            );
        } catch (Exception e) {
            log.error("Failed to parse access control for template: {}",
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
        AccountMetadata accountMetadata
    ) {
        if (Boolean.TRUE.equals(template.getSharedDocumentFlag())) {
            // Query shared documents
            return storageRepository.findSharedDocuments(
                template.getTemplateType(),
                template.getTemplateVersion()
            ).collectList();
        } else {
            // Query account-specific documents
            return storageRepository.findAccountSpecificDocuments(
                accountId,
                template.getTemplateType(),
                template.getTemplateVersion()
            ).collectList();
        }
    }

    /**
     * Convert storage entities to API response nodes
     */
    private List<DocumentDetailsNode> convertToDocumentDetailsNodes(
        List<StorageIndexEntity> entities,
        MasterTemplateDefinitionEntity template
    ) {
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

                // Add metadata
                if (entity.getDocMetadata() != null) {
                    List<MetadataNode> metadataNodes = new ArrayList<>();
                    entity.getDocMetadata().fields().forEachRemaining(field -> {
                        MetadataNode metaNode = new MetadataNode();
                        metaNode.setKey(field.getKey());
                        metaNode.setValue(field.getValue().asText());
                        metadataNodes.add(metaNode);
                    });
                    node.setMetadata(metadataNodes);
                }

                return node;
            })
            .collect(Collectors.toList());
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
}
