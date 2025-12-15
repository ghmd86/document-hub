package com.documenthub.service;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for building document API responses.
 * Handles pagination and response structure.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentResponseBuilder {

    private final ObjectMapper objectMapper;
    private final DocumentAccessControlService accessControlService;

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    /**
     * Convert entities to response nodes with access control.
     */
    public List<DocumentDetailsNode> convertToNodes(
            List<StorageIndexEntity> entities,
            MasterTemplateDefinitionEntity template,
            String requestorType) {

        List<String> permittedActions = accessControlService
                .getPermittedActions(template, requestorType);

        log.debug("Permitted actions for {}: {}", requestorType, permittedActions);

        return entities.stream()
                .map(entity -> buildNode(entity, template, permittedActions))
                .collect(Collectors.toList());
    }

    /**
     * Build paginated response.
     */
    public DocumentRetrievalResponse buildResponse(
            List<DocumentDetailsNode> documents,
            int totalDocuments,
            int pageNumber,
            int pageSize,
            long processingTime) {

        DocumentRetrievalResponse response = new DocumentRetrievalResponse();
        response.setDocumentList(documents);
        response.setPagination(buildPagination(totalDocuments, pageNumber, pageSize));
        response.setLinks(new DocumentRetrievalResponseLinks());

        logResponseBuilt(documents.size(), pageNumber, response.getPagination(), processingTime);

        return response;
    }

    /**
     * Build empty response.
     */
    public DocumentRetrievalResponse buildEmptyResponse() {
        return buildResponse(Collections.emptyList(), 0, 0, defaultPageSize, 0);
    }

    /**
     * Build error response.
     */
    public DocumentRetrievalResponse buildErrorResponse(Throwable error) {
        log.error("Building error response: {}", error.getMessage());
        return buildEmptyResponse();
    }

    /**
     * Apply pagination to document list.
     */
    public List<DocumentDetailsNode> paginate(
            List<DocumentDetailsNode> documents,
            int pageNumber,
            int pageSize) {

        int startIndex = pageNumber * pageSize;

        if (startIndex >= documents.size()) {
            return Collections.emptyList();
        }

        int endIndex = Math.min(startIndex + pageSize, documents.size());
        return documents.subList(startIndex, endIndex);
    }

    /**
     * Determine page size with limits.
     */
    public int determinePageSize(BigDecimal requestedPageSize) {
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
     * Determine page number.
     */
    public int determinePageNumber(BigDecimal requestedPageNumber) {
        if (requestedPageNumber == null) {
            return 0;
        }

        return Math.max(0, requestedPageNumber.intValue());
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    private DocumentDetailsNode buildNode(
            StorageIndexEntity entity,
            MasterTemplateDefinitionEntity template,
            List<String> permittedActions) {

        DocumentDetailsNode node = new DocumentDetailsNode();
        populateBasicFields(node, entity, template);
        populateMetadata(node, entity);
        node.setLinks(accessControlService.buildLinksForDocument(entity, permittedActions));

        return node;
    }

    private void populateBasicFields(
            DocumentDetailsNode node,
            StorageIndexEntity entity,
            MasterTemplateDefinitionEntity template) {

        node.setDocumentId(entity.getStorageIndexId() != null
                ? entity.getStorageIndexId().toString() : null);
        node.setDisplayName(entity.getFileName());
        node.setDescription(template.getTemplateDescription());
        node.setDocumentType(template.getTemplateType());
        node.setCategory(template.getTemplateCategory());
        node.setDatePosted(entity.getDocCreationDate());

        if (template.getLineOfBusiness() != null
                && !template.getLineOfBusiness().isEmpty()) {
            node.setLineOfBusiness(template.getLineOfBusiness());
        }
    }

    private void populateMetadata(DocumentDetailsNode node, StorageIndexEntity entity) {
        if (entity.getDocMetadata() == null) {
            return;
        }

        try {
            List<MetadataNode> metadataNodes = parseMetadata(entity);
            node.setMetadata(metadataNodes);
        } catch (Exception e) {
            log.warn("Failed to parse metadata for document {}: {}",
                    entity.getStorageIndexId(), e.getMessage());
        }
    }

    private List<MetadataNode> parseMetadata(StorageIndexEntity entity) throws Exception {
        List<MetadataNode> metadataNodes = new ArrayList<>();
        JsonNode metadataJson = objectMapper.readTree(entity.getDocMetadata().asString());

        metadataJson.fields().forEachRemaining(field -> {
            MetadataNode metaNode = new MetadataNode();
            metaNode.setKey(field.getKey());
            metaNode.setValue(field.getValue().asText());
            metadataNodes.add(metaNode);
        });

        return metadataNodes;
    }

    private PaginationResponse buildPagination(int totalDocuments, int pageNumber, int pageSize) {
        PaginationResponse pagination = new PaginationResponse();
        pagination.setPageSize(pageSize);
        pagination.setPageNumber(pageNumber);
        pagination.setTotalItems(totalDocuments);
        pagination.setTotalPages((int) Math.ceil((double) totalDocuments / pageSize));
        return pagination;
    }

    private void logResponseBuilt(
            int docCount,
            int pageNumber,
            PaginationResponse pagination,
            long processingTime) {

        log.info("Response: {} docs, page {}/{}, {}ms",
                docCount, pageNumber + 1, pagination.getTotalPages(), processingTime);
    }
}
