package com.documenthub.service;

import com.documenthub.model.AccountMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for retrieving account metadata
 *
 * This is a MOCK implementation for POC purposes.
 * In production, this would call an external Account/Customer API
 * or query a dedicated account_metadata table.
 */
@Service
@Slf4j
public class AccountMetadataService {

    /**
     * Line of Business constants.
     * These match the values in master_template_definition.line_of_business
     */
    public static final String LOB_CREDIT_CARD = "CREDIT_CARD";
    public static final String LOB_DIGITAL_BANK = "DIGITAL_BANK";
    public static final String LOB_ENTERPRISE = "ENTERPRISE";  // Applies to all LOBs

    // Mock data store
    private final Map<UUID, AccountMetadata> mockAccountData = new HashMap<>();

    public AccountMetadataService() {
        initializeMockData();
    }

    /**
     * Get account metadata by account ID
     */
    public Mono<AccountMetadata> getAccountMetadata(UUID accountId) {
        log.debug("Fetching account metadata for accountId: {}", accountId);

        AccountMetadata metadata = mockAccountData.get(accountId);

        if (metadata == null) {
            log.warn("No metadata found for accountId: {}, returning default metadata", accountId);
            // Return default metadata for unknown accounts
            return Mono.just(AccountMetadata.builder()
                .accountId(accountId)
                .accountType("unknown")
                .lineOfBusiness(LOB_CREDIT_CARD)  // Default to CREDIT_CARD
                .isActive(true)
                .build());
        }

        return Mono.just(metadata);
    }

    /**
     * Derive lineOfBusiness from accountType.
     * This is used when the request doesn't explicitly specify lineOfBusiness.
     *
     * Mapping:
     *   - credit_card, secured_credit_card → CREDIT_CARD
     *   - digital_bank, savings, checking → DIGITAL_BANK
     *   - unknown/other → CREDIT_CARD (default)
     */
    public String deriveLineOfBusiness(String accountType) {
        if (accountType == null) {
            return LOB_CREDIT_CARD;
        }

        String type = accountType.toLowerCase();

        if (type.contains("credit") || type.contains("card")) {
            return LOB_CREDIT_CARD;
        } else if (type.contains("digital") || type.contains("bank") ||
                   type.contains("saving") || type.contains("checking")) {
            return LOB_DIGITAL_BANK;
        }

        // Default to CREDIT_CARD for unknown types
        return LOB_CREDIT_CARD;
    }

    /**
     * Initialize mock data for testing
     */
    private void initializeMockData() {
        // Account 1: Credit Card, VIP, US_WEST
        UUID account1 = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
        mockAccountData.put(account1, AccountMetadata.builder()
            .accountId(account1)
            .customerId(UUID.fromString("cccc0000-0000-0000-0000-000000000001"))
            .accountType("credit_card")
            .region("US_WEST")
            .state("CA")
            .customerSegment("VIP")
            .accountOpenDate(Instant.now().minus(730, ChronoUnit.DAYS).toEpochMilli()) // 2 years ago
            .lineOfBusiness(LOB_CREDIT_CARD)
            .isActive(true)
            .build());

        // Account 2: Credit Card, STANDARD, US_WEST (same customer)
        UUID account2 = UUID.fromString("aaaa0000-0000-0000-0000-000000000002");
        mockAccountData.put(account2, AccountMetadata.builder()
            .accountId(account2)
            .customerId(UUID.fromString("cccc0000-0000-0000-0000-000000000001"))
            .accountType("credit_card")
            .region("US_WEST")
            .state("CA")
            .customerSegment("STANDARD")
            .accountOpenDate(Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli()) // 3 months ago
            .lineOfBusiness(LOB_CREDIT_CARD)
            .isActive(true)
            .build());

        // Account 3: Digital Bank, ENTERPRISE, US_EAST
        UUID account3 = UUID.fromString("aaaa0000-0000-0000-0000-000000000003");
        mockAccountData.put(account3, AccountMetadata.builder()
            .accountId(account3)
            .customerId(UUID.fromString("cccc0000-0000-0000-0000-000000000002"))
            .accountType("digital_bank")
            .region("US_EAST")
            .state("NY")
            .customerSegment("ENTERPRISE")
            .accountOpenDate(Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli()) // 1 year ago
            .lineOfBusiness(LOB_DIGITAL_BANK)
            .isActive(true)
            .build());

        // Account 4: Savings, STANDARD, US_EAST
        UUID account4 = UUID.fromString("aaaa0000-0000-0000-0000-000000000004");
        mockAccountData.put(account4, AccountMetadata.builder()
            .accountId(account4)
            .customerId(UUID.fromString("cccc0000-0000-0000-0000-000000000003"))
            .accountType("savings")
            .region("US_EAST")
            .state("TX")
            .customerSegment("STANDARD")
            .accountOpenDate(Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()) // 1 month ago
            .lineOfBusiness(LOB_DIGITAL_BANK)
            .isActive(true)
            .build());

        log.info("Initialized mock account data for {} accounts", mockAccountData.size());
    }
}
