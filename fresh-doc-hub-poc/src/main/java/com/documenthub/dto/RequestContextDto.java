package com.documenthub.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for request context from headers.
 * Contains correlation ID, requestor information, and version.
 */
@Data
@Builder
public class RequestContextDto {
    private Integer version;
    private String correlationId;
    private UUID requestorId;
    private String requestorType;
}
