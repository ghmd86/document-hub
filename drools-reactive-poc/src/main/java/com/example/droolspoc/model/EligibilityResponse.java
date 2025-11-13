package com.example.droolspoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Eligibility Check Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResponse {

    private String customerId;
    private String accountId;
    private Set<String> eligibleDocumentIds;
    private int eligibleCount;
    private long executionTimeMs;
    private Instant evaluatedAt;
}
