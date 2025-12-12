package com.documenthub.service;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.*;
import com.documenthub.repository.MasterTemplateRepository;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for document enquiry operations.
 *
 * <p><b>What:</b> Orchestrates the document retrieval process by coordinating
 * multiple services to fetch, filter, and return documents based on request criteria.</p>
 *
 * <p><b>Why:</b> Document enquiry is a complex operation involving:
 * <ul>
 *   <li>Template filtering (by LOB, type, message center flag)</li>
 *   <li>Access control (sharing scope, requestor type)</li>
 *   <li>Dynamic data extraction (from external APIs)</li>
 *   <li>Document matching (by template type, reference keys)</li>
 *   <li>Response building (pagination, HATEOAS links)</li>
 * </ul>
 * This service acts as the coordinator, delegating specialized tasks to
 * dedicated services while maintaining the overall workflow.</p>
 *
 * <p><b>How:</b> Uses a reactive pipeline (Project Reactor) to:
 * <ol>
 *   <li>Determine line of business (from request or account metadata)</li>
 *   <li>Query active templates matching the criteria</li>
 *   <li>For each account and template combination:
 *     <ul>
 *       <li>Check access permissions</li>
 *       <li>Extract required data from APIs</li>
 *       <li>Query matching documents</li>
 *       <li>Apply single document flag if needed</li>
 *     </ul>
 *   </li>
 *   <li>Build paginated response with links</li>
 * </ol>
 * </p>
 *
 * @see DocumentMatchingService
 * @see DocumentResponseBuilder
 * @see ConfigurableDataExtractionService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentEnquiryService {

    private final MasterTemplateRepository templateRepository;
    private final AccountMetadataService accountMetadataService;
    private final RuleEvaluationService ruleEvaluationService;
    private final ConfigurableDataExtractionService dataExtractionService;
    private final DocumentMatchingService documentMatchingService;
    private final DocumentResponseBuilder responseBuilder;

    /**
     * Retrieves documents for the given request (defaults to CUSTOMER requestor).
     *
     * <p><b>What:</b> Convenience method that defaults requestor type to CUSTOMER.</p>
     *
     * <p><b>Why:</b> Most document enquiries come from customer-facing applications
     * where the requestor is implicitly a CUSTOMER.</p>
     *
     * @param request The document list request containing filter criteria
     * @return Mono containing the paginated document response
     */
    public Mono<DocumentRetrievalResponse> getDocuments(DocumentListRequest request) {
        return getDocuments(request, "CUSTOMER");
    }

    /**
     * Retrieves documents for the given request with specified requestor type.
     *
     * <p><b>What:</b> Main entry point for document enquiry operations.</p>
     *
     * <p><b>Why:</b> Different requestor types (CUSTOMER, BANKER, AGENT) have
     * different access permissions. The requestor type determines which
     * actions (View, Update, Delete, Download) are available for each document.</p>
     *
     * <p><b>How:</b> Validates the request, then delegates to the reactive
     * processing pipeline. Tracks processing time for monitoring.</p>
     *
     * @param request The document list request containing filter criteria
     * @param requestorType Type of requestor (CUSTOMER, BANKER, AGENT)
     * @return Mono containing the paginated document response
     */
    public Mono<DocumentRetrievalResponse> getDocuments(
            DocumentListRequest request,
            String requestorType) {

        logRequestStart(request, requestorType);
        long startTime = System.currentTimeMillis();

        List<String> accountIds = getAccountIds(request);
        if (accountIds.isEmpty()) {
            log.warn("No account IDs provided");
            return Mono.just(responseBuilder.buildEmptyResponse());
        }

        return processRequest(request, requestorType, accountIds, startTime);
    }

    /**
     * Logs the start of a document enquiry request.
     *
     * <p><b>Why:</b> Request logging is essential for debugging, monitoring,
     * and audit trails.</p>
     *
     * @param request The incoming request
     * @param requestorType The type of requestor
     */
    private void logRequestStart(DocumentListRequest request, String requestorType) {
        log.info("Document enquiry - customer: {}, accounts: {}, lob: {}, requestor: {}",
                request.getCustomerId(), request.getAccountId(),
                request.getLineOfBusiness(), requestorType);
    }

    /**
     * Extracts account IDs from the request.
     *
     * <p><b>Why:</b> Account IDs are required for document queries. This method
     * handles null safety.</p>
     *
     * @param request The document list request
     * @return List of account IDs (may be empty)
     */
    private List<String> getAccountIds(DocumentListRequest request) {
        return request.getAccountId() != null
                ? request.getAccountId()
                : Collections.emptyList();
    }

    /**
     * Processes the document enquiry request through the reactive pipeline.
     *
     * <p><b>What:</b> Main processing logic that chains all steps together.</p>
     *
     * <p><b>Why:</b> Separates the processing flow from request validation
     * for cleaner code organization.</p>
     *
     * <p><b>How:</b> Uses reactive operators (flatMap, map) to chain:
     * LOB determination → template query → template processing → response building.
     * Errors are caught and converted to error responses.</p>
     *
     * @param request The document list request
     * @param requestorType Type of requestor
     * @param accountIds List of account IDs to query
     * @param startTime Request start time for processing time calculation
     * @return Mono containing the document response
     */
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

    /**
     * Gets the message center document flag from the request.
     *
     * <p><b>What:</b> Determines if only message center documents should be returned.</p>
     *
     * <p><b>Why:</b> Defaults to true (show message center docs) when not specified.
     * This is the common use case for customer-facing applications.</p>
     *
     * @param request The document list request
     * @return true if message center docs should be included (default), false otherwise
     */
    private Boolean getMessageCenterDocFlag(DocumentListRequest request) {
        return request.getMessageCenterDocFlag() == null
                || request.getMessageCenterDocFlag();
    }

    /**
     * Gets the communication type filter from the request.
     *
     * <p><b>What:</b> Extracts the communication type value if present.</p>
     *
     * <p><b>Why:</b> Communication type is an optional filter. Returns null
     * if not specified to indicate "all types".</p>
     *
     * @param request The document list request
     * @return Communication type value or null
     */
    private String getCommunicationType(DocumentListRequest request) {
        return request.getCommunicationType() != null
                ? request.getCommunicationType().getValue()
                : null;
    }

    /**
     * Determines the line of business for the request.
     *
     * <p><b>What:</b> Resolves the LOB from the request or account metadata.</p>
     *
     * <p><b>Why:</b> LOB is critical for template filtering. It can be explicitly
     * provided in the request, or derived from the account's metadata.</p>
     *
     * <p><b>How:</b> If LOB is in the request, uses it directly. Otherwise,
     * queries account metadata and uses the account's LOB (defaulting to "DEFAULT").</p>
     *
     * @param request The document list request
     * @param firstAccountId First account ID to query for metadata
     * @return Mono containing the resolved line of business
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
     * Queries the database for active templates matching the criteria.
     *
     * <p><b>What:</b> Fetches templates that match LOB, message center flag,
     * and communication type filters.</p>
     *
     * <p><b>Why:</b> Templates define what documents are available and how
     * to query them. Only active templates within their validity period
     * should be considered.</p>
     *
     * <p><b>How:</b> Uses the repository to query with filters, then applies
     * additional template type filtering in-memory.</p>
     *
     * @param lineOfBusiness Line of business to filter by
     * @param templateTypes List of template types to filter (empty = all)
     * @param messageCenterDocFlag Message center document flag
     * @param communicationType Communication type filter (null = all)
     * @return Mono containing list of matching templates
     */
    private Mono<List<MasterTemplateDefinitionEntity>> queryTemplates(
            String lineOfBusiness,
            List<String> templateTypes,
            Boolean messageCenterDocFlag,
            String communicationType) {

        log.info("Querying templates - lob: {}, types: {}, msgCenter: {}, commType: {}",
                lineOfBusiness, templateTypes, messageCenterDocFlag, communicationType);

        Long currentDate = Instant.now().toEpochMilli();

        return templateRepository.findActiveTemplatesWithFilters(
                        lineOfBusiness, messageCenterDocFlag, communicationType, currentDate)
                .filter(template -> matchesTemplateTypes(template, templateTypes))
                .collectList()
                .doOnNext(templates -> log.info("Found {} matching templates", templates.size()));
    }

    /**
     * Checks if a template matches the requested template types.
     *
     * <p><b>What:</b> Filters templates by type when specific types are requested.</p>
     *
     * <p><b>Why:</b> The request may specify which document categories to return.
     * If no types are specified, all templates are included.</p>
     *
     * @param template The template to check
     * @param requestedTypes List of requested template types
     * @return true if template matches or no types specified
     */
    private boolean matchesTemplateTypes(
            MasterTemplateDefinitionEntity template,
            List<String> requestedTypes) {

        if (requestedTypes == null || requestedTypes.isEmpty()) {
            return true;
        }
        return requestedTypes.contains(template.getTemplateType());
    }

    /**
     * Processes all templates for all accounts.
     *
     * <p><b>What:</b> Iterates through accounts and processes each template.</p>
     *
     * <p><b>Why:</b> A request may include multiple accounts. Each account
     * may have different documents matching the templates.</p>
     *
     * <p><b>How:</b> Uses Flux to process accounts in parallel, collecting
     * and flattening the results.</p>
     *
     * @param templates List of templates to process
     * @param accountIds List of account IDs
     * @param request The original request
     * @param requestorType Type of requestor
     * @param postedFromDate Start date filter (epoch ms)
     * @param postedToDate End date filter (epoch ms)
     * @return Mono containing flattened list of document nodes
     */
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

    /**
     * Processes all templates for a single account.
     *
     * <p><b>What:</b> Fetches account metadata and processes each template.</p>
     *
     * <p><b>Why:</b> Account metadata (like account type) is needed to check
     * template access permissions (sharing scope).</p>
     *
     * <p><b>How:</b> First retrieves account metadata, then processes each
     * template with the metadata context.</p>
     *
     * @param templates List of templates to process
     * @param accountId Account ID to process
     * @param request The original request
     * @param requestorType Type of requestor
     * @param postedFromDate Start date filter
     * @param postedToDate End date filter
     * @return Flux of document lists (one per template)
     */
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

    /**
     * Processes a single template for a single account.
     *
     * <p><b>What:</b> The core processing logic for one template/account combination.</p>
     *
     * <p><b>Why:</b> Each template may require different data extraction and
     * document matching logic.</p>
     *
     * <p><b>How:</b>
     * <ol>
     *   <li>Checks if account can access the template (sharing scope)</li>
     *   <li>Executes data extraction if configured</li>
     *   <li>Queries matching documents</li>
     *   <li>Converts to response nodes with HATEOAS links</li>
     * </ol>
     * Errors are caught and logged, returning empty list on failure.</p>
     *
     * @param template The template to process
     * @param accountId Account UUID
     * @param accountMetadata Account metadata for access checks
     * @param request The original request
     * @param requestorType Type of requestor for link generation
     * @param postedFromDate Start date filter
     * @param postedToDate End date filter
     * @return Mono containing list of document nodes
     */
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

    /**
     * Checks if an account can access a template based on sharing scope.
     *
     * <p><b>What:</b> Validates template access based on sharing_scope column.</p>
     *
     * <p><b>Why:</b> Templates can be restricted to specific account types.
     * For example, a template with sharing_scope="BUSINESS" should only
     * be visible to business accounts.</p>
     *
     * <p><b>How:</b> If sharing_scope is null or "ALL", access is granted.
     * Otherwise, the account type must match the sharing scope.</p>
     *
     * @param template The template to check access for
     * @param accountMetadata Account metadata containing account type
     * @return true if access is allowed
     */
    private boolean canAccessTemplate(
            MasterTemplateDefinitionEntity template,
            AccountMetadata accountMetadata) {

        String sharingScope = template.getSharingScope();
        if (sharingScope == null || "ALL".equalsIgnoreCase(sharingScope)) {
            return true;
        }
        return accountMetadata != null
                && sharingScope.equalsIgnoreCase(accountMetadata.getAccountType());
    }

    /**
     * Executes data extraction for a template if configured.
     *
     * <p><b>What:</b> Calls external APIs to extract required field values.</p>
     *
     * <p><b>Why:</b> Some templates require additional context (like disclosureCode)
     * that must be fetched from external APIs before querying documents.</p>
     *
     * <p><b>How:</b> Checks if data_extraction_config is present, then delegates
     * to ConfigurableDataExtractionService. Returns empty map if no config.</p>
     *
     * @param template The template with extraction config
     * @param request The original request for context values
     * @return Mono containing map of extracted field values
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
     * Queries documents and converts them to response nodes.
     *
     * <p><b>What:</b> Fetches matching documents and transforms to API response format.</p>
     *
     * <p><b>Why:</b> Documents are stored in storage_index table. We need to
     * query them based on template criteria and convert to the response DTO
     * with appropriate HATEOAS links.</p>
     *
     * <p><b>How:</b> Delegates querying to DocumentMatchingService, applies
     * single document flag if needed, then converts via DocumentResponseBuilder.</p>
     *
     * @param template The template defining query criteria
     * @param accountId Account UUID for filtering
     * @param extractedData Additional query parameters from data extraction
     * @param requestorType Type for determining available actions
     * @param postedFromDate Start date filter
     * @param postedToDate End date filter
     * @return Mono containing list of document nodes
     */
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

    /**
     * Applies the single document flag to limit results to the most recent document.
     *
     * <p><b>What:</b> If single_document_flag is true, returns only the most recent document.</p>
     *
     * <p><b>Why:</b> Some document types (like terms and conditions) should only
     * show the latest version. The single_document_flag in the template controls this.</p>
     *
     * <p><b>How:</b> If flag is true, sorts documents by creation date and returns
     * only the most recent one. Otherwise returns all documents.</p>
     *
     * @param documents List of documents to filter
     * @param template The template with single_document_flag
     * @return Filtered list (either all documents or just the most recent)
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

    /**
     * Flattens nested document lists into a single list.
     *
     * <p><b>What:</b> Converts List<List<DocumentDetailsNode>> to List<DocumentDetailsNode>.</p>
     *
     * <p><b>Why:</b> Processing produces nested lists (one per template/account).
     * We need a flat list for pagination and response building.</p>
     *
     * @param nestedDocuments Nested list of documents
     * @return Flattened list of all documents
     */
    private List<DocumentDetailsNode> flattenDocuments(
            List<List<DocumentDetailsNode>> nestedDocuments) {

        return nestedDocuments.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Builds the final paginated response.
     *
     * <p><b>What:</b> Creates the API response with pagination and metadata.</p>
     *
     * <p><b>Why:</b> The response must include pagination info, total counts,
     * and processing time for monitoring.</p>
     *
     * <p><b>How:</b> Determines pagination parameters, applies pagination to
     * documents, and delegates to DocumentResponseBuilder.</p>
     *
     * @param allDocuments All matching documents (before pagination)
     * @param request Original request with pagination parameters
     * @param startTime Request start time for processing time calculation
     * @return Complete document retrieval response
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

    /**
     * Extracts template types from the document category groups in the request.
     *
     * <p><b>What:</b> Converts the API's documentTypeCategoryGroup into a flat list of
     * template types for filtering. Each category group contains a documentTypes array
     * which maps to the template_type column in the database.</p>
     *
     * <p><b>Why:</b> The API request groups document types by category for user convenience,
     * but the database query needs a flat list of template_type values.</p>
     *
     * <p><b>How:</b> Flattens all documentTypes arrays from all category groups into
     * a single list, filtering out nulls and empty values.</p>
     *
     * @param request The document list request
     * @return List of template type strings (may be empty if no filter specified)
     */
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
