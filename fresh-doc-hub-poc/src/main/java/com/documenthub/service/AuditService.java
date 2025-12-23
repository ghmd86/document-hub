package com.documenthub.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for audit logging of sensitive operations.
 * Logs are structured for easy parsing by log aggregation systems (ELK, Splunk, etc.)
 *
 * Audit events include:
 * - Document uploads
 * - Document downloads
 * - Document deletions
 * - Access control checks
 * - Authentication events
 */
@Slf4j
@Service
public class AuditService {

    private static final String AUDIT_MARKER = "[AUDIT]";

    public enum AuditAction {
        DOCUMENT_UPLOAD,
        DOCUMENT_DOWNLOAD,
        DOCUMENT_DELETE,
        DOCUMENT_VIEW,
        DOCUMENT_METADATA_ACCESS,
        ACCESS_GRANTED,
        ACCESS_DENIED,
        TEMPLATE_ACCESS,
        ENQUIRY_REQUEST
    }

    public enum AuditResult {
        SUCCESS,
        FAILURE,
        DENIED
    }

    /**
     * Log a document upload event
     */
    public void logDocumentUpload(UUID storageIndexId, String templateType,
                                   UUID accountId, String requestorType,
                                   AuditResult result, String details) {
        Map<String, Object> auditData = createBaseAuditData(AuditAction.DOCUMENT_UPLOAD, result);
        auditData.put("storageIndexId", storageIndexId);
        auditData.put("templateType", templateType);
        auditData.put("accountId", accountId);
        auditData.put("requestorType", requestorType);
        auditData.put("details", details);

        logAuditEvent(auditData);
    }

    /**
     * Log a document download event
     */
    public void logDocumentDownload(String documentId, String requestorType,
                                     String requestorId, AuditResult result) {
        Map<String, Object> auditData = createBaseAuditData(AuditAction.DOCUMENT_DOWNLOAD, result);
        auditData.put("documentId", documentId);
        auditData.put("requestorType", requestorType);
        auditData.put("requestorId", requestorId);

        logAuditEvent(auditData);
    }

    /**
     * Log a document deletion event
     */
    public void logDocumentDelete(String documentId, String requestorType,
                                   String requestorId, AuditResult result, String reason) {
        Map<String, Object> auditData = createBaseAuditData(AuditAction.DOCUMENT_DELETE, result);
        auditData.put("documentId", documentId);
        auditData.put("requestorType", requestorType);
        auditData.put("requestorId", requestorId);
        auditData.put("reason", reason);

        logAuditEvent(auditData);
    }

    /**
     * Log a document view/metadata access event
     */
    public void logDocumentAccess(String documentId, AuditAction action,
                                   String requestorType, String requestorId,
                                   AuditResult result) {
        Map<String, Object> auditData = createBaseAuditData(action, result);
        auditData.put("documentId", documentId);
        auditData.put("requestorType", requestorType);
        auditData.put("requestorId", requestorId);

        logAuditEvent(auditData);
    }

    /**
     * Log an access control decision
     */
    public void logAccessControl(String templateType, String requestorType,
                                  String action, AuditResult result) {
        Map<String, Object> auditData = createBaseAuditData(
                result == AuditResult.SUCCESS ? AuditAction.ACCESS_GRANTED : AuditAction.ACCESS_DENIED,
                result);
        auditData.put("templateType", templateType);
        auditData.put("requestorType", requestorType);
        auditData.put("requestedAction", action);

        logAuditEvent(auditData);
    }

    /**
     * Log a document enquiry request
     */
    public void logEnquiryRequest(UUID customerId, int accountCount,
                                   String requestorType, int documentsReturned) {
        Map<String, Object> auditData = createBaseAuditData(AuditAction.ENQUIRY_REQUEST, AuditResult.SUCCESS);
        auditData.put("customerId", customerId);
        auditData.put("accountCount", accountCount);
        auditData.put("requestorType", requestorType);
        auditData.put("documentsReturned", documentsReturned);

        logAuditEvent(auditData);
    }

    /**
     * Log a security event (failed access attempt)
     */
    public void logSecurityEvent(String eventType, String requestorType,
                                  String requestorId, String resource, String reason) {
        Map<String, Object> auditData = createBaseAuditData(AuditAction.ACCESS_DENIED, AuditResult.DENIED);
        auditData.put("eventType", eventType);
        auditData.put("requestorType", requestorType);
        auditData.put("requestorId", requestorId);
        auditData.put("resource", resource);
        auditData.put("reason", reason);

        log.warn("{} Security event: {}", AUDIT_MARKER, formatAuditData(auditData));
    }

    private Map<String, Object> createBaseAuditData(AuditAction action, AuditResult result) {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", Instant.now().toString());
        data.put("action", action.name());
        data.put("result", result.name());
        data.put("correlationId", MDC.get("correlationId"));
        return data;
    }

    private void logAuditEvent(Map<String, Object> auditData) {
        AuditResult result = AuditResult.valueOf((String) auditData.get("result"));
        String message = formatAuditData(auditData);

        switch (result) {
            case SUCCESS:
                log.info("{} {}", AUDIT_MARKER, message);
                break;
            case FAILURE:
                log.error("{} {}", AUDIT_MARKER, message);
                break;
            case DENIED:
                log.warn("{} {}", AUDIT_MARKER, message);
                break;
        }
    }

    private String formatAuditData(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        data.forEach((key, value) -> {
            if (value != null) {
                sb.append(key).append("=").append(value).append(" ");
            }
        });
        return sb.toString().trim();
    }
}
