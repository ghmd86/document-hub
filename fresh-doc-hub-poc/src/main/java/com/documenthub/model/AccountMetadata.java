package com.documenthub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Account metadata used for rule evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountMetadata {

    private UUID accountId;
    private UUID customerId;
    private String accountType;       // e.g., "credit_card", "digital_bank", "savings"
    private String region;             // e.g., "US_WEST", "US_EAST", "EUROPE"
    private String state;              // e.g., "CA", "NY", "TX"
    private String customerSegment;    // e.g., "VIP", "ENTERPRISE", "STANDARD"
    private Long accountOpenDate;      // Epoch time for tenure calculation
    private String lineOfBusiness;     // e.g., "CREDIT_CARD", "BANKING"
    private Boolean isActive;
}
