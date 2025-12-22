package com.documenthub.processor;

import com.documenthub.dao.MasterTemplateDao;
import com.documenthub.dao.StorageIndexDao;
import com.documenthub.dto.upload.DocumentUploadRequest;
import com.documenthub.dto.upload.DocumentUploadResponse;
import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.integration.ecms.EcmsClient;
import com.documenthub.integration.ecms.dto.EcmsDocumentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Processor for document upload operations.
 * Orchestrates the upload flow including validation, ECMS upload, and storage index creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentUploadProcessor {

    private static final String STORAGE_VENDOR_ECMS = "ECMS";

    private final MasterTemplateDao masterTemplateDao;
    private final StorageIndexDao storageIndexDao;
    private final EcmsClient ecmsClient;
    private final ObjectMapper objectMapper;

    /**
     * Process document upload with FilePart (multipart upload)
     */
    public Mono<DocumentUploadResponse> processUpload(FilePart filePart,
                                                       DocumentUploadRequest request,
                                                       String userId) {
        log.info("Processing document upload: templateType={}, fileName={}, userId={}",
            request.getTemplateType(), request.getFileName(), userId);

        return validateAndGetTemplate(request.getTemplateType(), request.getTemplateVersion())
            .flatMap(template -> {
                // Validate required fields
                List<String> validationErrors = validateRequiredFields(template, request);
                if (!validationErrors.isEmpty()) {
                    return Mono.error(new IllegalArgumentException(
                        "Required fields validation failed: " + String.join(", ", validationErrors)));
                }

                // Upload to ECMS
                return ecmsClient.uploadDocument(filePart, request)
                    .flatMap(ecmsResponse ->
                        createStorageIndexEntry(request, ecmsResponse, template, userId)
                            .map(storageIndex -> buildUploadResponse(storageIndex, ecmsResponse))
                    );
            })
            .doOnSuccess(resp -> log.info("Document upload completed: storageIndexId={}, ecmsId={}",
                resp.getStorageIndexId(), resp.getEcmsDocumentId()))
            .doOnError(e -> log.error("Document upload failed", e));
    }

    /**
     * Process document upload from byte array
     */
    public Mono<DocumentUploadResponse> processUpload(byte[] fileContent,
                                                       DocumentUploadRequest request,
                                                       String userId) {
        log.info("Processing document upload from bytes: templateType={}, fileName={}, size={}, userId={}",
            request.getTemplateType(), request.getFileName(), fileContent.length, userId);

        return validateAndGetTemplate(request.getTemplateType(), request.getTemplateVersion())
            .flatMap(template -> {
                // Validate required fields
                List<String> validationErrors = validateRequiredFields(template, request);
                if (!validationErrors.isEmpty()) {
                    return Mono.error(new IllegalArgumentException(
                        "Required fields validation failed: " + String.join(", ", validationErrors)));
                }

                // Upload to ECMS
                return ecmsClient.uploadDocument(fileContent, request)
                    .flatMap(ecmsResponse ->
                        createStorageIndexEntry(request, ecmsResponse, template, userId)
                            .map(storageIndex -> buildUploadResponse(storageIndex, ecmsResponse))
                    );
            })
            .doOnSuccess(resp -> log.info("Document upload completed: storageIndexId={}, ecmsId={}",
                resp.getStorageIndexId(), resp.getEcmsDocumentId()))
            .doOnError(e -> log.error("Document upload failed", e));
    }

    /**
     * Validate template exists and is active
     */
    private Mono<MasterTemplateDefinitionEntity> validateAndGetTemplate(String templateType, Integer templateVersion) {
        return masterTemplateDao.findByTypeAndVersion(templateType, templateVersion)
            .switchIfEmpty(Mono.error(new IllegalArgumentException(
                "Template not found: type=" + templateType + ", version=" + templateVersion)));
    }

    /**
     * Validate required fields from template configuration against the upload request
     */
    private List<String> validateRequiredFields(MasterTemplateDefinitionEntity template,
                                                 DocumentUploadRequest request) {
        List<String> errors = new ArrayList<>();

        Json requiredFieldsJson = template.getRequiredFields();
        if (requiredFieldsJson == null) {
            log.debug("No required fields defined for template: type={}, version={}",
                template.getTemplateType(), template.getTemplateVersion());
            return errors;
        }

        try {
            JsonNode requiredFields = objectMapper.readTree(requiredFieldsJson.asString());

            if (requiredFields.isArray()) {
                for (JsonNode fieldDef : requiredFields) {
                    String fieldName = fieldDef.path("field").asText();
                    String fieldType = fieldDef.path("type").asText();
                    boolean required = fieldDef.path("required").asBoolean(true);

                    if (required) {
                        Object fieldValue = getFieldValue(request, fieldName);
                        if (fieldValue == null || (fieldValue instanceof String && ((String) fieldValue).isBlank())) {
                            errors.add("Missing required field: " + fieldName);
                        } else {
                            // Validate field type
                            String typeError = validateFieldType(fieldName, fieldValue, fieldType);
                            if (typeError != null) {
                                errors.add(typeError);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse required fields config: {}", e.getMessage());
        }

        return errors;
    }

    /**
     * Get field value from request by field name
     */
    private Object getFieldValue(DocumentUploadRequest request, String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "accountid", "account_id" -> request.getAccountId();
            case "customerid", "customer_id" -> request.getCustomerId();
            case "referencekey", "reference_key" -> request.getReferenceKey();
            case "referencekeytype", "reference_key_type" -> request.getReferenceKeyType();
            case "filename", "file_name" -> request.getFileName();
            case "displayname", "display_name" -> request.getDisplayName();
            case "templatetype", "template_type" -> request.getTemplateType();
            case "templateversion", "template_version" -> request.getTemplateVersion();
            case "startdate", "start_date" -> request.getStartDate();
            case "enddate", "end_date" -> request.getEndDate();
            default -> {
                // Check metadata for custom fields
                if (request.getMetadata() != null) {
                    yield request.getMetadata().get(fieldName);
                }
                yield null;
            }
        };
    }

    /**
     * Validate field value matches expected type
     */
    private String validateFieldType(String fieldName, Object value, String expectedType) {
        if (value == null || expectedType == null || expectedType.isBlank()) {
            return null;
        }

        try {
            switch (expectedType.toLowerCase()) {
                case "uuid":
                    if (!(value instanceof UUID)) {
                        UUID.fromString(value.toString());
                    }
                    break;
                case "number":
                    if (!(value instanceof Number)) {
                        Double.parseDouble(value.toString());
                    }
                    break;
                case "date":
                    if (!(value instanceof Long)) {
                        Long.parseLong(value.toString());
                    }
                    break;
                case "boolean":
                    if (!(value instanceof Boolean)) {
                        String strVal = value.toString().toLowerCase();
                        if (!strVal.equals("true") && !strVal.equals("false")) {
                            return "Field '" + fieldName + "' must be a boolean";
                        }
                    }
                    break;
                case "string":
                    // String is always valid
                    break;
                default:
                    log.debug("Unknown field type '{}' for field '{}'", expectedType, fieldName);
            }
        } catch (Exception e) {
            return "Field '" + fieldName + "' must be of type " + expectedType;
        }

        return null;
    }

    /**
     * Create a storage index entry for the uploaded document
     */
    private Mono<StorageIndexEntity> createStorageIndexEntry(DocumentUploadRequest request,
                                                              EcmsDocumentResponse ecmsResponse,
                                                              MasterTemplateDefinitionEntity template,
                                                              String userId) {
        UUID storageIndexId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        long currentTimeMs = System.currentTimeMillis();

        // Determine sharedFlag: inherit from template's sharedDocumentFlag
        boolean sharedFlag = Boolean.TRUE.equals(template.getSharedDocumentFlag())
            || Boolean.TRUE.equals(request.getSharedFlag());

        StorageIndexEntity entity = StorageIndexEntity.builder()
            .storageIndexId(storageIndexId)
            .masterTemplateId(template.getMasterTemplateId())
            .templateVersion(request.getTemplateVersion())
            .templateType(request.getTemplateType())
            .storageVendor(STORAGE_VENDOR_ECMS)
            .storageDocumentKey(ecmsResponse.getId())
            .fileName(request.getFileName())
            .referenceKey(request.getReferenceKey())
            .referenceKeyType(request.getReferenceKeyType())
            .accountKey(request.getAccountId())
            .customerKey(request.getCustomerId())
            .docCreationDate(currentTimeMs)
            .accessibleFlag(true)
            .sharedFlag(sharedFlag)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .createdBy(userId)
            .createdTimestamp(now)
            .archiveIndicator(false)
            .versionNumber(1L)
            .recordStatus("ACTIVE")
            .build();

        // Set metadata if provided
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            try {
                String metadataJson = objectMapper.writeValueAsString(request.getMetadata());
                entity.setDocMetadata(Json.of(metadataJson));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize metadata, skipping: {}", e.getMessage());
            }
        }

        log.debug("Creating storage index entry: id={}, ecmsDocId={}, templateType={}, sharedFlag={}",
            storageIndexId, ecmsResponse.getId(), request.getTemplateType(), sharedFlag);

        return storageIndexDao.save(entity);
    }

    /**
     * Build the upload response
     */
    private DocumentUploadResponse buildUploadResponse(StorageIndexEntity storageIndex,
                                                        EcmsDocumentResponse ecmsResponse) {
        DocumentUploadResponse.FileSize fileSize = null;
        if (ecmsResponse.getFileSize() != null) {
            fileSize = DocumentUploadResponse.FileSize.builder()
                .value(ecmsResponse.getFileSize().getValue())
                .unit(ecmsResponse.getFileSize().getUnit())
                .build();
        }

        return DocumentUploadResponse.builder()
            .storageIndexId(storageIndex.getStorageIndexId())
            .ecmsDocumentId(ecmsResponse.getId())
            .fileName(storageIndex.getFileName())
            .displayName(ecmsResponse.getName())
            .templateType(storageIndex.getTemplateType())
            .templateVersion(storageIndex.getTemplateVersion())
            .accountId(storageIndex.getAccountKey())
            .customerId(storageIndex.getCustomerKey())
            .referenceKey(storageIndex.getReferenceKey())
            .referenceKeyType(storageIndex.getReferenceKeyType())
            .documentLink(ecmsResponse.getLink())
            .fileSize(fileSize)
            .createdAt(storageIndex.getCreatedTimestamp())
            .status("SUCCESS")
            .message("Document uploaded successfully")
            .build();
    }
}
