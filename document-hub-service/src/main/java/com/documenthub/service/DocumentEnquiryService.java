package com.documenthub.service;

import com.documenthub.model.entity.MasterTemplateDefinition;
import com.documenthub.model.entity.StorageIndex;
import com.documenthub.model.request.DocumentListRequest;
import com.documenthub.model.response.DocumentDetailsNode;
import com.documenthub.model.response.DocumentRetrievalResponse;
import com.documenthub.model.response.PaginationResponse;
import com.documenthub.repository.MasterTemplateDefinitionRepository;
import com.documenthub.repository.StorageIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for document enquiry.
 * Retrieves account-specific and shared documents, applies filtering, deduplication, and pagination.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEnquiryService {

    private final StorageIndexRepository storageIndexRepository;
    private final MasterTemplateDefinitionRepository templateRepository;
    private final SharedDocumentEligibilityService sharedDocumentEligibilityService;
    private final DocumentMappingService documentMappingService;

    /**
     * Retrieve documents based on request criteria.
     */
    public Mono<DocumentRetrievalResponse> getDocuments(DocumentListRequest request) {
        log.info("Processing document enquiry request for customerId: {}, accountIds: {}",
                request.getCustomerId(), request.getAccountId());

        // Step 1: Retrieve account-specific documents
        Flux<StorageIndex> accountDocuments = getAccountSpecificDocuments(request);

        // Step 2: Retrieve eligible shared documents
        Flux<StorageIndex> sharedDocuments = getEligibleSharedDocuments(request);

        // Step 3: Merge and deduplicate
        return Flux.concat(accountDocuments, sharedDocuments)
                .distinct(doc -> doc.getTemplateId() + "_" + doc.getDocType())
                .collectList()
                .flatMap(allDocuments -> {
                    log.debug("Total documents after merge and dedup: {}", allDocuments.size());

                    // Apply filtering based on request criteria
                    List<StorageIndex> filteredDocs = applyFilters(allDocuments, request);

                    // Apply sorting
                    List<StorageIndex> sortedDocs = applySorting(filteredDocs, request);

                    // Apply pagination
                    return applyPagination(sortedDocs, request);
                });
    }

    /**
     * Get account-specific documents from storage_index.
     */
    private Flux<StorageIndex> getAccountSpecificDocuments(DocumentListRequest request) {
        UUID customerId = request.getCustomerId();
        List<UUID> accountIds = request.getAccountId();
        Long fromDate = request.getPostedFromDate();
        Long toDate = request.getPostedToDate();

        if (customerId == null) {
            log.debug("No customerId provided, skipping account-specific documents");
            return Flux.empty();
        }

        // If account IDs are provided, use them; otherwise get all for customer
        if (accountIds != null && !accountIds.isEmpty()) {
            UUID[] accountArray = accountIds.toArray(new UUID[0]);

            if (fromDate != null && toDate != null) {
                return storageIndexRepository.findByCustomerAndAccountsWithDateRange(
                        customerId, accountArray, fromDate, toDate);
            } else {
                return storageIndexRepository.findByAccountKeysAndIsAccessible(accountArray, true);
            }
        } else {
            if (fromDate != null && toDate != null) {
                return storageIndexRepository.findByCustomerWithDateRange(customerId, fromDate, toDate);
            } else {
                return storageIndexRepository.findByCustomerKeyAndIsAccessible(customerId, true);
            }
        }
    }

    /**
     * Get eligible shared documents based on sharing rules.
     */
    private Flux<StorageIndex> getEligibleSharedDocuments(DocumentListRequest request) {
        UUID customerId = request.getCustomerId();
        List<UUID> accountIds = request.getAccountId();

        if (customerId == null) {
            log.debug("No customerId provided, skipping shared documents");
            return Flux.empty();
        }

        // Use first account ID for eligibility checks (or implement multi-account logic)
        UUID primaryAccountId = (accountIds != null && !accountIds.isEmpty()) ?
                accountIds.get(0) : null;

        long currentTime = Instant.now().getEpochSecond();

        return templateRepository.findActiveSharedDocuments(currentTime)
                .flatMap(template ->
                    sharedDocumentEligibilityService.isEligible(template, customerId, primaryAccountId)
                            .filter(eligible -> eligible)
                            .flatMap(eligible -> createVirtualDocument(template))
                )
                .doOnNext(doc -> log.debug("Added shared document: {}", doc.getTemplateId()));
    }

    /**
     * Create a virtual StorageIndex document for shared documents.
     * These don't exist in storage_index but are derived from template definitions.
     */
    private Mono<StorageIndex> createVirtualDocument(MasterTemplateDefinition template) {
        return Mono.just(StorageIndex.builder()
                .storageIndexId(UUID.randomUUID()) // Virtual ID
                .templateId(template.getTemplateId())
                .docType(template.getDocType())
                .docCreationDate(template.getEffectiveDate())
                .isAccessible(true)
                .fileName(template.getTemplateName())
                .build());
    }

    /**
     * Apply filters based on request criteria.
     */
    private List<StorageIndex> applyFilters(List<StorageIndex> documents, DocumentListRequest request) {
        List<StorageIndex> filtered = new ArrayList<>(documents);

        // Filter by document type and category if specified
        if (request.getDocumentTypeCategoryGroup() != null && !request.getDocumentTypeCategoryGroup().isEmpty()) {
            filtered = filtered.stream()
                    .filter(doc -> matchesCategoryGroup(doc, request.getDocumentTypeCategoryGroup()))
                    .collect(Collectors.toList());
        }

        return filtered;
    }

    /**
     * Check if document matches any of the category groups.
     */
    private boolean matchesCategoryGroup(StorageIndex doc,
                                        List<DocumentListRequest.DocumentCategoryGroup> groups) {
        // This would require joining with template to get category
        // For now, simplified logic
        return true; // TODO: Implement proper category matching with template join
    }

    /**
     * Apply sorting based on request criteria.
     */
    private List<StorageIndex> applySorting(List<StorageIndex> documents, DocumentListRequest request) {
        if (request.getSortOrder() == null || request.getSortOrder().isEmpty()) {
            // Default sort by creation date descending
            return documents.stream()
                    .sorted(Comparator.comparing(StorageIndex::getDocCreationDate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        // Apply custom sorting
        List<StorageIndex> sorted = new ArrayList<>(documents);
        for (DocumentListRequest.SortOrder sortOrder : request.getSortOrder()) {
            Comparator<StorageIndex> comparator = createComparator(sortOrder);
            sorted.sort(comparator);
        }

        return sorted;
    }

    /**
     * Create comparator based on sort order.
     */
    private Comparator<StorageIndex> createComparator(DocumentListRequest.SortOrder sortOrder) {
        Comparator<StorageIndex> comparator = switch (sortOrder.getOrderBy()) {
            case "creationDate", "datePosted" ->
                Comparator.comparing(StorageIndex::getDocCreationDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "fileName" ->
                Comparator.comparing(StorageIndex::getFileName, Comparator.nullsLast(Comparator.naturalOrder()));
            default ->
                Comparator.comparing(StorageIndex::getDocCreationDate, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if ("desc".equalsIgnoreCase(sortOrder.getSortBy())) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    /**
     * Apply pagination and build response.
     */
    private Mono<DocumentRetrievalResponse> applyPagination(List<StorageIndex> documents,
                                                            DocumentListRequest request) {
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        long totalItems = documents.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, documents.size());

        List<StorageIndex> pageDocuments = startIndex < documents.size() ?
                documents.subList(startIndex, endIndex) : Collections.emptyList();

        // Map to DTOs
        return Flux.fromIterable(pageDocuments)
                .flatMap(documentMappingService::mapToDto)
                .collectList()
                .map(documentList -> DocumentRetrievalResponse.builder()
                        .documentList(documentList)
                        .pagination(PaginationResponse.builder()
                                .pageNumber(pageNumber)
                                .pageSize(pageSize)
                                .totalItems(totalItems)
                                .totalPages(totalPages)
                                .build())
                        .links(buildPaginationLinks(pageNumber, totalPages))
                        .build());
    }

    /**
     * Build pagination links (self, next, prev).
     */
    private DocumentRetrievalResponse.Links buildPaginationLinks(int currentPage, int totalPages) {
        String baseUrl = "/documents-enquiry";

        return DocumentRetrievalResponse.Links.builder()
                .self(DocumentRetrievalResponse.Link.builder()
                        .href(baseUrl)
                        .type("POST")
                        .rel("self")
                        .responseTypes(List.of("application/json"))
                        .build())
                .next(currentPage < totalPages ? DocumentRetrievalResponse.Link.builder()
                        .href(baseUrl)
                        .type("POST")
                        .rel("next")
                        .responseTypes(List.of("application/json"))
                        .build() : null)
                .prev(currentPage > 1 ? DocumentRetrievalResponse.Link.builder()
                        .href(baseUrl)
                        .type("POST")
                        .rel("prev")
                        .responseTypes(List.of("application/json"))
                        .build() : null)
                .build();
    }
}
