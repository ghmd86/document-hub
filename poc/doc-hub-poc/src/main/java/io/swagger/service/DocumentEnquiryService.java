package io.swagger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.entity.MasterTemplateDefinitionEntity;
import io.swagger.entity.StorageIndexEntity;
import io.swagger.model.config.DataExtractionConfig;
import io.swagger.model.access.AccessControl;
import io.swagger.model.context.ExtractionContext;
import io.swagger.model.DocumentListRequest;
import io.swagger.model.DocumentListResponse;
import io.swagger.model.DocumentMetadata;
import io.swagger.repository.MasterTemplateDefinitionRepository;
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

@Service
@Slf4j
public class DocumentEnquiryService {

    @Autowired
    private MasterTemplateDefinitionRepository templateRepository;

    @Autowired
    private DataExtractionEngine dataExtractionEngine;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private DocumentMatchingService documentMatchingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    /**
     * Main entry point for document enquiry
     */
    public Mono<DocumentListResponse> getDocuments(DocumentListRequest request) {
        log.info("Processing document enquiry for customer: {}, account: {}",
            request.getCustomerId(),
            request.getAccountId());

        long startTime = System.currentTimeMillis();

        // Get all active templates
        return templateRepository.findActiveTemplates(Instant.now().toEpochMilli())
            .collectList()
            .flatMap(templates -> {
                log.debug("Found {} active templates", templates.size());

                // Process each template
                return Flux.fromIterable(templates)
                    .flatMap(template -> processTemplate(template, request))
                    .collectList()
                    .map(documentLists -> {
                        // Flatten and combine all documents
                        List<DocumentMetadata> allDocuments = documentLists.stream()
                            .flatMap(List::stream)
                            .collect(Collectors.toList());

                        log.info("Total documents found: {}", allDocuments.size());

                        // Apply pagination
                        int pageSize = determinePageSize(request.getPageSize());
                        int pageNumber = determinePageNumber(request.getPageNumber());

                        List<DocumentMetadata> paginatedDocs = paginateDocuments(
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
                            request.getCustomerId(),
                            processingTime
                        );
                    });
            })
            .doOnError(e -> log.error("Error processing document enquiry", e))
            .onErrorResume(e -> Mono.just(buildErrorResponse(e)));
    }

    /**
     * Process a single template
     */
    private Mono<List<DocumentMetadata>> processTemplate(
        MasterTemplateDefinitionEntity template,
        DocumentListRequest request
    ) {
        log.debug("Processing template: {} (version {})",
            template.getTemplateType(),
            template.getTemplateVersion());

        try {
            // Parse configurations
            DataExtractionConfig extractionConfig = parseExtractionConfig(
                template.getDataExtractionConfig()
            );
            AccessControl accessControl = parseAccessControl(
                template.getAccessControl()
            );

            // Execute extraction strategy (if defined)
            return dataExtractionEngine
                .executeExtractionStrategy(extractionConfig, request)
                .flatMap(context -> {
                    log.debug("Extraction completed with {} variables",
                        context.getVariables().size());

                    // Evaluate eligibility
                    boolean isEligible = ruleEvaluationService.evaluateEligibility(
                        accessControl,
                        context
                    );

                    if (!isEligible) {
                        log.debug("Template {} not eligible, skipping",
                            template.getTemplateType());
                        return Mono.just(Collections.<DocumentMetadata>emptyList());
                    }

                    log.debug("Template {} is eligible, finding matching documents",
                        template.getTemplateType());

                    // Find matching documents
                    return findMatchingDocuments(template, extractionConfig, context)
                        .map(docs -> convertToMetadata(docs, template, context));
                })
                .onErrorResume(e -> {
                    log.error("Error processing template: {}",
                        template.getTemplateType(), e);
                    return Mono.just(Collections.<DocumentMetadata>emptyList());
                });

        } catch (Exception e) {
            log.error("Error parsing template configuration for: {}",
                template.getTemplateType(), e);
            return Mono.just(Collections.<DocumentMetadata>emptyList());
        }
    }

    /**
     * Find matching documents based on extraction context
     */
    private Mono<List<StorageIndexEntity>> findMatchingDocuments(
        MasterTemplateDefinitionEntity template,
        DataExtractionConfig extractionConfig,
        ExtractionContext context
    ) {
        if (extractionConfig == null ||
            extractionConfig.getDocumentMatchingStrategy() == null) {
            log.debug("No matching strategy defined for template: {}",
                template.getTemplateType());
            return Mono.just(Collections.emptyList());
        }

        return documentMatchingService
            .findMatchingDocuments(
                template.getTemplateType(),
                template.getTemplateVersion(),
                extractionConfig.getDocumentMatchingStrategy(),
                context
            )
            .collectList()
            .doOnNext(docs -> log.debug("Found {} matching documents for template {}",
                docs.size(), template.getTemplateType()));
    }

    /**
     * Convert storage entities to metadata objects
     */
    private List<DocumentMetadata> convertToMetadata(
        List<StorageIndexEntity> entities,
        MasterTemplateDefinitionEntity template,
        ExtractionContext context
    ) {
        return entities.stream()
            .map(entity -> {
                DocumentMetadata metadata = new DocumentMetadata();
                metadata.setDocumentId(entity.getStorageDocumentKey() != null ?
                    entity.getStorageDocumentKey().toString() : null);
                metadata.setTemplateType(template.getTemplateType());
                metadata.setTemplateVersion(template.getTemplateVersion());
                metadata.setDocumentName(template.getTemplateName());
                metadata.setDocumentDescription(template.getTemplateDescription());
                metadata.setStorageLocation(entity.getStorageVendor());
                metadata.setFileFormat(entity.getFileName());
                metadata.setCreatedDate(entity.getDocCreationDate());
                metadata.setLastModifiedDate(entity.getUpdatedTimestamp() != null ?
                    entity.getUpdatedTimestamp().toEpochSecond(java.time.ZoneOffset.UTC) : null);

                // Add custom metadata from extraction context
                Map<String, Object> customMetadata = new HashMap<>();
                if (entity.getDocMetadata() != null) {
                    entity.getDocMetadata().fields().forEachRemaining(field ->
                        customMetadata.put(field.getKey(), field.getValue().asText())
                    );
                }

                // Add extracted variables
                context.getVariables().forEach((key, value) -> {
                    if (key.startsWith("$extracted.")) {
                        String fieldName = key.substring("$extracted.".length());
                        customMetadata.put(fieldName, value);
                    }
                });

                metadata.setMetadata(customMetadata);

                return metadata;
            })
            .collect(Collectors.toList());
    }

    /**
     * Parse data extraction config from JSON
     */
    private DataExtractionConfig parseExtractionConfig(Object configJson) {
        if (configJson == null) {
            return null;
        }

        try {
            return objectMapper.treeToValue(
                objectMapper.valueToTree(configJson),
                DataExtractionConfig.class
            );
        } catch (Exception e) {
            log.error("Failed to parse extraction config", e);
            return null;
        }
    }

    /**
     * Parse access control from JSON
     */
    private AccessControl parseAccessControl(Object accessControlJson) {
        if (accessControlJson == null) {
            return null;
        }

        try {
            return objectMapper.treeToValue(
                objectMapper.valueToTree(accessControlJson),
                AccessControl.class
            );
        } catch (Exception e) {
            log.error("Failed to parse access control", e);
            return null;
        }
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
    private List<DocumentMetadata> paginateDocuments(
        List<DocumentMetadata> documents,
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
    private DocumentListResponse buildResponse(
        List<DocumentMetadata> documents,
        int totalDocuments,
        int pageNumber,
        int pageSize,
        UUID customerId,
        long processingTime
    ) {
        DocumentListResponse response = new DocumentListResponse();
        response.setDocuments(documents);
        response.setTotalCount(totalDocuments);
        response.setPageNumber(pageNumber);
        response.setPageSize(pageSize);
        response.setTotalPages((int) Math.ceil((double) totalDocuments / pageSize));

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customerId", customerId.toString());
        metadata.put("processingTimeMs", processingTime);
        metadata.put("timestamp", Instant.now().toString());

        response.setMetadata(metadata);

        // Add HATEOAS links
        response.setLinks(buildLinks(pageNumber, pageSize, totalDocuments, customerId));

        log.info("Response built: {} documents, page {}/{}, processing time: {}ms",
            documents.size(),
            pageNumber + 1,
            response.getTotalPages(),
            processingTime);

        return response;
    }

    /**
     * Build HATEOAS links
     */
    private Map<String, String> buildLinks(
        int pageNumber,
        int pageSize,
        int totalDocuments,
        UUID customerId
    ) {
        Map<String, String> links = new HashMap<>();

        String baseUrl = "/api/v1/documents-enquiry";
        String queryParams = "?customerId=" + customerId + "&pageSize=" + pageSize;

        // Self link
        links.put("self", baseUrl + queryParams + "&pageNumber=" + pageNumber);

        // First and last links
        links.put("first", baseUrl + queryParams + "&pageNumber=0");

        int lastPage = (int) Math.ceil((double) totalDocuments / pageSize) - 1;
        links.put("last", baseUrl + queryParams + "&pageNumber=" + Math.max(0, lastPage));

        // Previous link
        if (pageNumber > 0) {
            links.put("prev", baseUrl + queryParams + "&pageNumber=" + (pageNumber - 1));
        }

        // Next link
        if ((pageNumber + 1) * pageSize < totalDocuments) {
            links.put("next", baseUrl + queryParams + "&pageNumber=" + (pageNumber + 1));
        }

        return links;
    }

    /**
     * Build error response
     */
    private DocumentListResponse buildErrorResponse(Throwable error) {
        DocumentListResponse response = new DocumentListResponse();
        response.setDocuments(Collections.emptyList());
        response.setTotalCount(0);
        response.setPageNumber(0);
        response.setPageSize(defaultPageSize);
        response.setTotalPages(0);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("error", error.getMessage());
        metadata.put("timestamp", Instant.now().toString());

        response.setMetadata(metadata);

        return response;
    }
}
