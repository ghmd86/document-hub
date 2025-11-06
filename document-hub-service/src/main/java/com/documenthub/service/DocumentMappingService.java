package com.documenthub.service;

import com.documenthub.model.entity.MasterTemplateDefinition;
import com.documenthub.model.entity.StorageIndex;
import com.documenthub.model.response.DocumentDetailsNode;
import com.documenthub.repository.MasterTemplateDefinitionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service to map StorageIndex entities to DocumentDetailsNode DTOs.
 * Enriches document data with template metadata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentMappingService {

    private final MasterTemplateDefinitionRepository templateRepository;

    /**
     * Map StorageIndex to DocumentDetailsNode with enrichment.
     */
    public Mono<DocumentDetailsNode> mapToDto(StorageIndex storageIndex) {
        return templateRepository.findById(storageIndex.getTemplateId())
                .map(template -> mapWithTemplate(storageIndex, template))
                .defaultIfEmpty(mapWithoutTemplate(storageIndex));
    }

    /**
     * Map with template metadata enrichment.
     */
    private DocumentDetailsNode mapWithTemplate(StorageIndex storageIndex, MasterTemplateDefinition template) {
        return DocumentDetailsNode.builder()
                .documentId(encodeDocumentId(storageIndex.getStorageIndexId().toString()))
                .sizeInMb(0L) // TODO: Calculate from actual file
                .languageCode(template.getLanguageCode())
                .displayName(storageIndex.getFileName() != null ? storageIndex.getFileName() : template.getTemplateName())
                .mimeType("application/pdf") // TODO: Determine from file
                .description(template.getDescription())
                .lineOfBusiness(List.of(template.getLineOfBusiness()))
                .category(template.getCategory())
                .documentType(template.getDocType())
                .datePosted(storageIndex.getDocCreationDate())
                .lastDownloaded(storageIndex.getLastReferenced())
                .lastClientDownload(storageIndex.getCustomerKey() != null ?
                        storageIndex.getCustomerKey().toString() : null)
                .metadata(extractMetadata(storageIndex))
                .links(buildDocumentLinks(storageIndex))
                .build();
    }

    /**
     * Map without template metadata.
     */
    private DocumentDetailsNode mapWithoutTemplate(StorageIndex storageIndex) {
        return DocumentDetailsNode.builder()
                .documentId(encodeDocumentId(storageIndex.getStorageIndexId().toString()))
                .sizeInMb(0L)
                .displayName(storageIndex.getFileName())
                .mimeType("application/pdf")
                .documentType(storageIndex.getDocType())
                .datePosted(storageIndex.getDocCreationDate())
                .lastDownloaded(storageIndex.getLastReferenced())
                .metadata(extractMetadata(storageIndex))
                .links(buildDocumentLinks(storageIndex))
                .build();
    }

    /**
     * Extract metadata from StorageIndex.
     */
    private List<DocumentDetailsNode.MetadataNode> extractMetadata(StorageIndex storageIndex) {
        List<DocumentDetailsNode.MetadataNode> metadata = new ArrayList<>();

        if (storageIndex.getAccountKey() != null) {
            metadata.add(DocumentDetailsNode.MetadataNode.builder()
                    .key("accountId")
                    .value(storageIndex.getAccountKey().toString())
                    .dataType("STRING")
                    .build());
        }

        if (storageIndex.getCustomerKey() != null) {
            metadata.add(DocumentDetailsNode.MetadataNode.builder()
                    .key("customerId")
                    .value(storageIndex.getCustomerKey().toString())
                    .dataType("STRING")
                    .build());
        }

        if (storageIndex.getReferenceKey() != null) {
            metadata.add(DocumentDetailsNode.MetadataNode.builder()
                    .key(storageIndex.getReferenceKeyType() != null ?
                            storageIndex.getReferenceKeyType() : "referenceKey")
                    .value(storageIndex.getReferenceKey())
                    .dataType("STRING")
                    .build());
        }

        // Extract from doc_info JSON
        if (storageIndex.getDocInfo() != null) {
            JsonNode docInfo = storageIndex.getDocInfo();
            docInfo.fields().forEachRemaining(entry -> {
                metadata.add(DocumentDetailsNode.MetadataNode.builder()
                        .key(entry.getKey())
                        .value(entry.getValue().asText())
                        .dataType("STRING")
                        .build());
            });
        }

        return metadata;
    }

    /**
     * Build document action links (download, delete).
     */
    private DocumentDetailsNode.DocumentLinks buildDocumentLinks(StorageIndex storageIndex) {
        String documentId = encodeDocumentId(storageIndex.getStorageIndexId().toString());

        return DocumentDetailsNode.DocumentLinks.builder()
                .download(DocumentDetailsNode.LinkDetail.builder()
                        .href("/documents/" + documentId)
                        .type("GET")
                        .rel("download")
                        .title("Download this document")
                        .responseTypes(List.of("application/pdf", "application/octet-stream"))
                        .build())
                .delete(DocumentDetailsNode.LinkDetail.builder()
                        .href("/documents/" + documentId)
                        .type("DELETE")
                        .rel("delete")
                        .title("Delete this document")
                        .build())
                .build();
    }

    /**
     * Encode document ID for API responses.
     */
    private String encodeDocumentId(String rawId) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawId.getBytes());
    }
}
