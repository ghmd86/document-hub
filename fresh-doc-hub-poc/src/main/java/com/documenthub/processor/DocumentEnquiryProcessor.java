package com.documenthub.processor;

import com.documenthub.dao.MasterTemplateDao;
import com.documenthub.dto.DocumentQueryParams;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.AccountMetadata;
import com.documenthub.model.DocumentDetailsNode;
import com.documenthub.model.DocumentListRequest;
import com.documenthub.model.DocumentRetrievalResponse;
import com.documenthub.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.Builder;
import lombok.Data;
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
 *
 * <p><b>What:</b> Orchestrates the document retrieval flow by coordinating template lookup,
 * access control, data extraction, document matching, and response building.</p>
 *
 * <p><b>Why:</b> Document enquiry is complex because it must filter templates by LOB,
 * check sharing_scope access, optionally extract data from external APIs, query documents
 * with various matching strategies, and build paginated responses with HATEOAS links.</p>
 *
 * <p><b>How:</b> The enquiry flow follows these steps:
 * <ol>
 *   <li><b>Step 1 - Resolve Account IDs:</b> Use request.accountId[] or fetch by customerId</li>
 *   <li><b>Step 2 - Determine LOB:</b> From request or derive from account metadata</li>
 *   <li><b>Step 3 - Query Templates:</b> Find active templates with LOB/messageCenterDocFlag/communicationType filters</li>
 *   <li><b>Step 4 - Process Templates:</b> For each account+template: check access, extract data, query docs</li>
 *   <li><b>Step 5 - Apply Single Document Flag:</b> Keep only latest document if flag is true</li>
 *   <li><b>Step 6 - Build Response:</b> Paginate results and construct final response with HATEOAS links</li>
 * </ol>
 * </p>
 *
 * @see MasterTemplateDao
 * @see AccountMetadataService
 * @see ConfigurableDataExtractionService
 * @see DocumentMatchingService
 * @see DocumentResponseBuilder
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
    private final ObjectMapper objectMapper;

    /**
     * Process document enquiry request (defaults to CUSTOMER requestor).
     */
    public Mono<DocumentRetrievalResponse> processEnquiry(DocumentListRequest request) {
        return processEnquiry(request, "CUSTOMER");
    }

    /**
     * Main entry point for document enquiry.
     *
     * <p><b>What:</b> Orchestrates the complete document enquiry flow from account resolution
     * to paginated response construction.</p>
     *
     * <p><b>Why:</b> Consumers need a single API to query documents across accounts with
     * proper filtering by template, LOB, and access control.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li><b>Step 1:</b> Resolve accountIds from request or fetch by customerId</li>
     *   <li><b>Steps 2-6:</b> Delegate to {@link #processRequest} for template query, document
     *       matching, and response building</li>
     * </ol>
     * </p>
     *
     * @param request The enquiry request with filters and pagination
     * @param requestorType The type of requestor (CUSTOMER, AGENT, SYSTEM)
     * @return Mono containing paginated document list with HATEOAS links
     */
    public Mono<DocumentRetrievalResponse> processEnquiry(
            DocumentListRequest request,
            String requestorType) {

        logRequestStart(request, requestorType);
        long startTime = System.currentTimeMillis();

        // Step 1: Resolve account IDs
        List<String> accountIds = getAccountIds(request);

        // Step 1a: If accountIds provided, use them directly
        if (!accountIds.isEmpty()) {
            return processRequest(request, requestorType, accountIds, startTime);
        }

        // Step 1b: If no accountIds but customerId provided, fetch accounts for that customer
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

    /**
     * Steps 2-6: Process the request after account IDs are resolved.
     *
     * <p><b>What:</b> Executes the core enquiry logic: determine LOB, query templates,
     * process each template for each account, and build the final response.</p>
     *
     * <p><b>Why:</b> Separating this from processEnquiry() allows for cleaner account
     * resolution logic and makes the template-based processing testable in isolation.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li><b>Step 2:</b> Determine LOB from request or account metadata</li>
     *   <li><b>Step 3:</b> Query active templates with LOB and filters</li>
     *   <li><b>Step 4:</b> Process templates (access check, data extraction, document query)</li>
     *   <li><b>Steps 5-6:</b> Apply single_document_flag and build paginated response</li>
     * </ol>
     * </p>
     */
    private Mono<DocumentRetrievalResponse> processRequest(
            DocumentListRequest request,
            String requestorType,
            List<String> accountIds,
            long startTime) {
        EnquiryContext context = buildEnquiryContext(request, requestorType, accountIds);
        return determineLineOfBusiness(request, accountIds.get(0))  // Step 2
                .flatMap(lob -> queryTemplates(lob, context))  // Step 3
                .flatMap(templates -> processTemplates(templates, context))  // Step 4
                .map(documents -> buildFinalResponse(documents, request, startTime))  // Step 6
                .onErrorResume(e -> Mono.just(responseBuilder.buildErrorResponse(e)));
    }

    private EnquiryContext buildEnquiryContext(
            DocumentListRequest request, String requestorType, List<String> accountIds) {
        return EnquiryContext.builder()
                .request(request)
                .requestorType(requestorType)
                .accountIds(accountIds)
                .templateTypes(extractTemplateTypes(request))
                .messageCenterDocFlag(getMessageCenterDocFlag(request))
                .communicationType(getCommunicationType(request))
                .postedFromDate(request.getPostedFromDate())
                .postedToDate(request.getPostedToDate())
                .build();
    }

    private Mono<List<MasterTemplateDefinitionEntity>> queryTemplates(String lob, EnquiryContext ctx) {
        return queryTemplates(lob, ctx.getTemplateTypes(), ctx.getMessageCenterDocFlag(), ctx.getCommunicationType());
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

    /**
     * Step 2: Determine Line of Business.
     *
     * <p><b>What:</b> Resolves the line of business to use for template filtering.</p>
     *
     * <p><b>Why:</b> Templates are scoped by LOB (e.g., CREDIT_CARD, DIGITAL_BANK).
     * We need to know which LOB to query templates for. ENTERPRISE templates are
     * always included regardless of LOB.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>If request.lineOfBusiness is provided, use it directly</li>
     *   <li>Otherwise, fetch account metadata and derive LOB from it</li>
     *   <li>Default to "DEFAULT" if LOB cannot be determined</li>
     * </ol>
     * </p>
     */
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
     * Step 3: Query Templates.
     *
     * <p><b>What:</b> Queries master_template_definition for active templates matching
     * the LOB and request filters.</p>
     *
     * <p><b>Why:</b> Templates define which documents can be retrieved and their access
     * control rules. We filter by LOB, messageCenterDocFlag, and communicationType
     * to return only relevant templates.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>If documentTypes are specified, use findActiveTemplatesWithAllFilters</li>
     *   <li>Otherwise, use findActiveTemplatesWithFilters for all templates in LOB</li>
     *   <li>Both queries filter by: active_flag, date range, LOB (including ENTERPRISE)</li>
     * </ol>
     * </p>
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

    /**
     * Step 4: Process Templates.
     *
     * <p><b>What:</b> Iterates through each account and template combination to
     * check access, extract data, and query documents.</p>
     *
     * <p><b>Why:</b> Each account may have different access to templates (via sharing_scope),
     * and each template may require different data extraction before querying documents.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>For each accountId, process all templates</li>
     *   <li>For each template: check sharing_scope access, extract data if configured</li>
     *   <li>Query documents using DocumentMatchingService</li>
     *   <li>Flatten results from all account+template combinations</li>
     * </ol>
     * </p>
     */
    private Mono<List<DocumentDetailsNode>> processTemplates(
            List<MasterTemplateDefinitionEntity> templates,
            EnquiryContext context) {
        if (templates.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }
        return Flux.fromIterable(context.getAccountIds())
                .flatMap(accountId -> processAccountTemplates(templates, accountId, context))
                .collectList()
                .map(this::flattenDocuments);
    }

    private Flux<List<DocumentDetailsNode>> processAccountTemplates(
            List<MasterTemplateDefinitionEntity> templates,
            String accountId,
            EnquiryContext context) {
        UUID accountUuid = UUID.fromString(accountId);
        return accountMetadataService.getAccountMetadata(accountUuid)
                .flatMapMany(metadata -> Flux.fromIterable(templates)
                        .flatMap(template -> processTemplate(template, accountUuid, metadata, context)));
    }

    /**
     * Step 4 (continued): Process a single template for an account.
     *
     * <p><b>What:</b> Checks access, extracts data, queries documents, and applies
     * single_document_flag for a single account+template combination.</p>
     *
     * <p><b>Why:</b> This is the core processing unit. Each template may have different
     * sharing_scope rules and data extraction requirements.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Step 4a: Check sharing_scope vs accountType (canAccessTemplate)</li>
     *   <li>Step 4b: Execute data extraction if template has data_extraction_config</li>
     *   <li>Step 4b.5: Check eligibility for auto_discover templates (if defined)</li>
     *   <li>Step 4c: Query documents via DocumentMatchingService</li>
     *   <li>Step 5: Apply single_document_flag if true</li>
     * </ol>
     * </p>
     */
    private Mono<List<DocumentDetailsNode>> processTemplate(
            MasterTemplateDefinitionEntity template,
            UUID accountId,
            AccountMetadata accountMetadata,
            EnquiryContext context) {
        // Step 4a: Check sharing_scope access
        if (!canAccessTemplate(template, accountMetadata)) {
            return Mono.just(Collections.emptyList());
        }
        // Steps 4b, 4b.5, 4c, 5: Extract data, check eligibility, query docs, apply single_document_flag
        return executeDataExtraction(template, context.getRequest())
                .flatMap(extractedData -> {
                    // Step 4b.5: Check eligibility for auto_discover templates (if defined)
                    if (isAutoDiscoverTemplate(template) && hasEligibilityCriteria(template)) {
                        boolean eligible = ruleEvaluationService.evaluateEligibility(
                                template.getEligibilityCriteria(),
                                accountMetadata,
                                extractedData
                        );
                        if (!eligible) {
                            log.debug("Account {} not eligible for auto_discover template: {}",
                                    accountId, template.getTemplateType());
                            return Mono.just(Collections.<DocumentDetailsNode>emptyList());
                        }
                    }
                    return queryAndConvertDocuments(template, accountId, extractedData, context);
                })
                .onErrorResume(e -> handleTemplateError(template, e));
    }

    /**
     * Check if template uses auto_discover matchMode.
     */
    private boolean isAutoDiscoverTemplate(MasterTemplateDefinitionEntity template) {
        if (template.getDocumentMatchingConfig() == null) {
            return false;
        }
        try {
            JsonNode config = objectMapper.readTree(
                    template.getDocumentMatchingConfig().asString());
            return "auto_discover".equals(config.path("matchMode").asText());
        } catch (Exception e) {
            log.debug("Error parsing document_matching_config: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if template has eligibility criteria defined.
     */
    private boolean hasEligibilityCriteria(MasterTemplateDefinitionEntity template) {
        return template.getEligibilityCriteria() != null;
    }

    private Mono<List<DocumentDetailsNode>> handleTemplateError(
            MasterTemplateDefinitionEntity template, Throwable e) {
        log.error("Error processing template {}: {}", template.getTemplateType(), e.getMessage());
        return Mono.just(Collections.emptyList());
    }

    /**
     * Step 4a: Check sharing_scope access.
     *
     * <p><b>What:</b> Determines if an account can access documents from this template
     * based on the template's sharing_scope configuration.</p>
     *
     * <p><b>Why:</b> Different templates may be restricted to specific account types
     * (e.g., only CHECKING accounts can see bank statements).</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>If sharing_scope is null or "ALL", allow access</li>
     *   <li>If sharing_scope is "CUSTOM_RULES", allow (rules evaluated elsewhere)</li>
     *   <li>Otherwise, match sharing_scope against accountMetadata.accountType</li>
     * </ol>
     * </p>
     */
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

    /**
     * Step 4b: Execute Data Extraction.
     *
     * <p><b>What:</b> Fetches additional data from external APIs if the template
     * has a data_extraction_config.</p>
     *
     * <p><b>Why:</b> Some templates require dynamic data (e.g., disclosureCode, pricingId)
     * to match documents. This data is fetched from external APIs and used in document queries.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Check if template has data_extraction_config</li>
     *   <li>If yes, delegate to ConfigurableDataExtractionService</li>
     *   <li>Return extracted fields as Map, or empty map on error</li>
     * </ol>
     * </p>
     *
     * @see ConfigurableDataExtractionService
     */
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

    /**
     * Step 4c: Query and Convert Documents.
     *
     * <p><b>What:</b> Queries storage_index for documents matching the template and account,
     * applies single_document_flag, and converts to response nodes with HATEOAS links.</p>
     *
     * <p><b>Why:</b> This is where documents are actually retrieved from the database.
     * The DocumentMatchingService handles reference_key matching and date filtering.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Build DocumentQueryParams with template, account, extractedData, and date filters</li>
     *   <li>Include requestReferenceKey/Type for 'direct' matchMode support</li>
     *   <li>Call DocumentMatchingService.queryDocuments()</li>
     *   <li>Step 5: Apply single_document_flag if true (keep only latest)</li>
     *   <li>Convert to DocumentDetailsNode with HATEOAS links via ResponseBuilder</li>
     * </ol>
     * </p>
     */
    private Mono<List<DocumentDetailsNode>> queryAndConvertDocuments(
            MasterTemplateDefinitionEntity template,
            UUID accountId,
            Map<String, Object> extractedData,
            EnquiryContext context) {
        DocumentListRequest request = context.getRequest();
        DocumentQueryParams queryParams = DocumentQueryParams.builder()
                .template(template)
                .accountId(accountId)
                .extractedData(extractedData)
                .postedFromDate(context.getPostedFromDate())
                .postedToDate(context.getPostedToDate())
                .requestReferenceKey(request.getReferenceKey())
                .requestReferenceKeyType(request.getReferenceKeyType())
                .build();
        return documentMatchingService.queryDocuments(queryParams)
                .map(docs -> applySingleDocumentFlag(docs, template))  // Step 5
                .map(docs -> responseBuilder.convertToNodes(docs, template, context.getRequestorType()));
    }

    /**
     * Step 5: Apply Single Document Flag.
     *
     * <p><b>What:</b> If template.single_document_flag is true, keeps only the most
     * recent document (by doc_creation_date) and discards the rest.</p>
     *
     * <p><b>Why:</b> Some document types (e.g., Privacy Policy) should only show the
     * current version to users, even if multiple versions exist in the database.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Check if single_document_flag is true</li>
     *   <li>If false, return all documents unchanged</li>
     *   <li>If true, find document with max doc_creation_date</li>
     *   <li>Return single-element list with only the latest document</li>
     * </ol>
     * </p>
     */
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

    /**
     * Step 6: Build Final Response.
     *
     * <p><b>What:</b> Paginates the document list and constructs the final response
     * with metadata (totalCount, pageNumber, pageSize, processingTime).</p>
     *
     * <p><b>Why:</b> Clients expect paginated results with navigation links and metadata
     * to handle large document sets efficiently.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Determine pageSize and pageNumber from request (with defaults)</li>
     *   <li>Paginate the document list</li>
     *   <li>Calculate processing time for diagnostics</li>
     *   <li>Build response with documents, pagination info, and HATEOAS links</li>
     * </ol>
     * </p>
     */
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

    @Data
    @Builder
    private static class EnquiryContext {
        private DocumentListRequest request;
        private String requestorType;
        private List<String> accountIds;
        private List<String> templateTypes;
        private Boolean messageCenterDocFlag;
        private String communicationType;
        private Long postedFromDate;
        private Long postedToDate;
    }
}
