package com.example.droolspoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Eligibility Check Request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRequest {

    private String customerId;
    private String accountId;
}
