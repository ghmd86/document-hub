package com.documenthub.processor;

import com.documenthub.dao.MasterTemplateDao;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.*;
import com.documenthub.service.*;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processor for document enquiry operations.
 * Orchestrates the document retrieval flow by coordinating DAOs and services.
 *
 * <p>Flow:
 * <ol>
 *   <li>Determine line of business (from request or account metadata)</li>
 *   <li>Query active templates via MasterTemplateDao</li>
 *   <li>For each account and template: check access, extract data, query documents</li>
 *   <li>Build paginated response</li>
 * </ol>
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentEnquiryProcessor {

    private final MasterTemplateDao masterTemplateDao;
    private final AccountMetadataService accountMetadataService;
    private final RuleEvaluationService ruleEvaluationService;
    private final ConfigurableDataExtractionService dataExtractionService;
    private final DocumentMatchingService documentMatchingService;
    private final DocumentResponseBuilder responseBuilder;

    /**
     * Process document enquiry request (defaults to CUSTOMER requestor).
     */
    public Mono<DocumentRetrievalResponse> processEnquiry(DocumentListRequest request) {
        return processEnquiry(request, "CUSTOMER");
    }

    /**
     * Process document enquiry request with specified requestor type.
     */
    public Mono<DocumentRetrievalResponse> processEnquiry(
            DocumentListRequest request,
            String requestorType) {

        logRequestStart(request, requestorType);
        long startTime = System.currentTimeMillis();

        List<String> accountIds = getAccountIds(request);

        // If accountIds are provided, use them directly
        if (!accountIds.isEmpty()) {
            return processRequest(request, requestorType, accountIds, startTime);
        }

        // If no accountIds but customerId is provided, fetch accounts for that customer
        if (request.getCustomerId() != null) {
            log.info("No accountId provided, fetching all accounts for customerId: {}",
                    request.getCustomerId());
            return accountMetadataService.getAccountsByCustomerId(request.getCustomerId())
                    .map(metadata -> metadata.getAccountId().toString())
                    .collectList()
                    .flatMap(fetchedAccountIds -> {
                        if (fetchedAccountIds.isEmpty()) {
                            log.warn("No accounts found for customerId: {}", request.getCustomerId());
                            return Mono.just(responseBuilder.buildEmptyResponse());
                        }
                        log.info("Found {} accounts for customerId: {}",
                                fetchedAccountIds.size(), request.getCustomerId());
                        return processRequest(request, requestorType, fetchedAccountIds, startTime);
                    });
        }

        // Neither accountId nor customerId provided
        log.warn("No account IDs or customer ID provided");
        return Mono.just(responseBuilder.buildEmptyResponse());
    }

    private void logRequestStart(DocumentListRequest request, String requestorType) {
        log.info("Document enquiry - customer: {}, accounts: {}, lob: {}, requestor: {}",
                request.getCustomerId(), request.getAccountId(),
                request.getLineOfBusiness(), requestorType);
    }

    private List<String> getAccountIds(DocumentListRequest request) {
        return request.getAccountId() != null
                ? request.getAccountId()
                : Collections.emptyList();
    }

    private Mono<DocumentRetrievalResponse> processRequest(
            DocumentListRequest request,
            String requestorType,
            List<String> accountIds,
            long startTime) {

        List<String> templateTypes = extractTemplateTypes(request);
        Boolean messageCenterDocFlag = getMessageCenterDocFlag(request);
        String communicationType = getCommunicationType(request);
        Long postedFromDate = request.getPostedFromDate();
        Long postedToDate = request.getPostedToDate();

        return determineLineOfBusiness(request, accountIds.get(0))
                .flatMap(lob -> queryTemplates(
                        lob, templateTypes, messageCenterDocFlag, communicationType))
                .flatMap(templates -> processTemplates(
                        templates, accountIds, request, requestorType,
                        postedFromDate, postedToDate))
                .map(documents -> buildFinalResponse(
                        documents, request, startTime))
                .onErrorResume(e -> Mono.just(responseBuilder.buildErrorResponse(e)));
    }

    private Boolean getMessageCenterDocFlag(DocumentListRequest request) {
        return request.getMessageCenterDocFlag() == null
                || request.getMessageCenterDocFlag();
    }

    private String getCommunicationType(DocumentListRequest request) {
        return request.getCommunicationType() != null
                ? request.getCommunicationType().getValue()
                : null;
    }

    private Mono<String> determineLineOfBusiness(
            DocumentListRequest request,
            String firstAccountId) {

        if (request.getLineOfBusiness() != null) {
            return Mono.just(request.getLineOfBusiness().getValue());
        }

        return accountMetadataService.getAccountMetadata(UUID.fromString(firstAccountId))
                .map(meta -> meta.getLineOfBusiness() != null
                        ? meta.getLineOfBusiness() : "DEFAULT")
                .defaultIfEmpty("DEFAULT");
    }

    /**
     * Query templates using MasterTemplateDao
     */
    private Mono<List<MasterTemplateDefinitionEntity>> queryTemplates(
            String lineOfBusiness,
            List<String> templateTypes,
            Boolean messageCenterDocFlag,
            String communicationType) {

        log.info("Querying templates - lob: {}, types: {}, msgCenter: {}, commType: {}",
                lineOfBusiness, templateTypes, messageCenterDocFlag, communicationType);

        Long currentDate = Instant.now().toEpochMilli();

        if (templateTypes != null && !templateTypes.isEmpty()) {
            return masterTemplateDao.findActiveTemplatesWithAllFilters(
                            lineOfBusiness, templateTypes, messageCenterDocFlag,
                            communicationType, currentDate)
                    .collectList()
                    .doOnNext(templates -> log.info("Found {} matching templates", templates.size()));
        }

        return masterTemplateDao.findActiveTemplatesWithFilters(
                        lineOfBusiness, messageCenterDocFlag, communicationType, currentDate)
                .collectList()
                .doOnNext(templates -> log.info("Found {} matching templates", templates.size()));
    }

    private Mono<List<DocumentDetailsNode>> processTemplates(
            List<MasterTemplateDefinitionEntity> templates,
            List<String> accountIds,
            DocumentListRequest request,
            String requestorType,
            Long postedFromDate,
            Long postedToDate) {

        if (templates.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        return Flux.fromIterable(accountIds)
                .flatMap(accountId -> processAccountTemplates(
                        templates, accountId, request, requestorType,
                        postedFromDate, postedToDate))
                .collectList()
                .map(this::flattenDocuments);
    }

    private Flux<List<DocumentDetailsNode>> processAccountTemplates(
            List<MasterTemplateDefinitionEntity> templates,
            String accountId,
            DocumentListRequest request,
            String requestorType,
            Long postedFromDate,
            Long postedToDate) {

        UUID accountUuid = UUID.fromString(accountId);

        return accountMetadataService.getAccountMetadata(accountUuid)
                .flatMapMany(metadata -> Flux.fromIterable(templates)
                        .flatMap(template -> processTemplate(
                                template, accountUuid, metadata, request,
                                requestorType, postedFromDate, postedToDate)));
    }

    private Mono<List<DocumentDetailsNode>> processTemplate(
            MasterTemplateDefinitionEntity template,
            UUID accountId,
            AccountMetadata accountMetadata,
            DocumentListRequest request,
            String requestorType,
            Long postedFromDate,
            Long postedToDate) {

        if (!canAccessTemplate(template, accountMetadata)) {
            return Mono.just(Collections.emptyList());
        }

        return executeDataExtraction(template, request)
                .flatMap(extractedData -> queryAndConvertDocuments(
                        template, accountId, extractedData, requestorType,
                        postedFromDate, postedToDate))
                .onErrorResume(e -> {
                    log.error("Error processing template {}: {}",
                            template.getTemplateType(), e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    private boolean canAccessTemplate(
            MasterTemplateDefinitionEntity template,
            AccountMetadata accountMetadata) {

        String sharingScope = template.getSharingScope();
        if (sharingScope == null || "ALL".equalsIgnoreCase(sharingScope)) {
            return true;
        }
        if ("CUSTOM_RULES".equalsIgnoreCase(sharingScope)) {
            return true;
        }
        return accountMetadata != null
                && sharingScope.equalsIgnoreCase(accountMetadata.getAccountType());
    }

    private Mono<Map<String, Object>> executeDataExtraction(
            MasterTemplateDefinitionEntity template,
            DocumentListRequest request) {

        if (template.getDataExtractionConfig() == null) {
            return Mono.just(Collections.emptyMap());
        }

        Json configJson = template.getDataExtractionConfig();
        return dataExtractionService.extractData(configJson, request)
                .doOnSuccess(data -> log.info("Extracted {} fields", data.size()))
                .onErrorResume(e -> {
                    log.error("Data extraction failed: {}", e.getMessage());
                    return Mono.just(Collections.emptyMap());
                });
    }

    private Mono<List<DocumentDetailsNode>> queryAndConvertDocuments(
            MasterTemplateDefinitionEntity template,
            UUID accountId,
            Map<String, Object> extractedData,
            String requestorType,
            Long postedFromDate,
            Long postedToDate) {

        return documentMatchingService.queryDocuments(
                        template, accountId, extractedData, postedFromDate, postedToDate)
                .map(docs -> applySingleDocumentFlag(docs, template))
                .map(docs -> responseBuilder.convertToNodes(docs, template, requestorType));
    }

    private List<StorageIndexEntity> applySingleDocumentFlag(
            List<StorageIndexEntity> documents,
            MasterTemplateDefinitionEntity template) {

        if (documents.isEmpty()) {
            return documents;
        }

        if (!Boolean.TRUE.equals(template.getSingleDocumentFlag())) {
            return documents;
        }

        log.debug("Applying single_document_flag for {}", template.getTemplateType());

        return documents.stream()
                .max(Comparator.comparing(doc ->
                        doc.getDocCreationDate() != null ? doc.getDocCreationDate() : 0L))
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    private List<DocumentDetailsNode> flattenDocuments(
            List<List<DocumentDetailsNode>> nestedDocuments) {

        return nestedDocuments.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private DocumentRetrievalResponse buildFinalResponse(
            List<DocumentDetailsNode> allDocuments,
            DocumentListRequest request,
            long startTime) {

        int pageSize = responseBuilder.determinePageSize(request.getPageSize());
        int pageNumber = responseBuilder.determinePageNumber(request.getPageNumber());
        int totalDocuments = allDocuments.size();

        List<DocumentDetailsNode> paginatedDocs = responseBuilder.paginate(
                allDocuments, pageNumber, pageSize);

        long processingTime = System.currentTimeMillis() - startTime;

        return responseBuilder.buildResponse(
                paginatedDocs, totalDocuments, pageNumber, pageSize, processingTime);
    }

    private List<String> extractTemplateTypes(DocumentListRequest request) {
        if (request.getDocumentTypeCategoryGroup() == null) {
            return Collections.emptyList();
        }

        return request.getDocumentTypeCategoryGroup().stream()
                .filter(group -> group.getDocumentTypes() != null)
                .flatMap(group -> group.getDocumentTypes().stream())
                .filter(Objects::nonNull)
                .filter(type -> !type.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
