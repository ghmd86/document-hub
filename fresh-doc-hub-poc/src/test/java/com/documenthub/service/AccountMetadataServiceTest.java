package com.documenthub.service;

import com.documenthub.model.AccountMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccountMetadataService.
 * Tests account metadata retrieval including the new getAccountsByCustomerId method.
 */
public class AccountMetadataServiceTest {

    private AccountMetadataService accountMetadataService;

    // Test data - matches mock data in AccountMetadataService
    private static final UUID CUSTOMER_1 = UUID.fromString("cccc0000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_2 = UUID.fromString("cccc0000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_3 = UUID.fromString("cccc0000-0000-0000-0000-000000000003");
    private static final UUID UNKNOWN_CUSTOMER = UUID.fromString("cccc0000-0000-0000-0000-999999999999");

    private static final UUID ACCOUNT_1 = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_2 = UUID.fromString("aaaa0000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_3 = UUID.fromString("aaaa0000-0000-0000-0000-000000000003");
    private static final UUID ACCOUNT_4 = UUID.fromString("aaaa0000-0000-0000-0000-000000000004");

    @BeforeEach
    void setUp() {
        accountMetadataService = new AccountMetadataService();
    }

    // ========================================================================
    // Tests for getAccountMetadata (existing functionality)
    // ========================================================================
    @Nested
    @DisplayName("getAccountMetadata Tests")
    class GetAccountMetadataTests {

        @Test
        @DisplayName("Should return account metadata for known account")
        void shouldReturnMetadata_forKnownAccount() {
            // When
            AccountMetadata result = accountMetadataService.getAccountMetadata(ACCOUNT_1).block();

            // Then
            assertNotNull(result);
            assertEquals(ACCOUNT_1, result.getAccountId());
            assertEquals(CUSTOMER_1, result.getCustomerId());
            assertEquals("credit_card", result.getAccountType());
            assertEquals("CREDIT_CARD", result.getLineOfBusiness());
        }

        @Test
        @DisplayName("Should return default metadata for unknown account")
        void shouldReturnDefaultMetadata_forUnknownAccount() {
            // Given
            UUID unknownAccount = UUID.randomUUID();

            // When
            AccountMetadata result = accountMetadataService.getAccountMetadata(unknownAccount).block();

            // Then
            assertNotNull(result);
            assertEquals(unknownAccount, result.getAccountId());
            assertEquals("unknown", result.getAccountType());
            assertEquals("CREDIT_CARD", result.getLineOfBusiness()); // Default
            assertTrue(result.getIsActive());
        }
    }

    // ========================================================================
    // Tests for getAccountsByCustomerId (new functionality)
    // ========================================================================
    @Nested
    @DisplayName("getAccountsByCustomerId Tests")
    class GetAccountsByCustomerIdTests {

        @Test
        @DisplayName("Should return multiple accounts for customer with multiple accounts")
        void shouldReturnMultipleAccounts_forCustomerWithMultipleAccounts() {
            // Customer 1 has 2 accounts (ACCOUNT_1 and ACCOUNT_2)
            StepVerifier.create(accountMetadataService.getAccountsByCustomerId(CUSTOMER_1))
                    .expectNextMatches(meta -> meta.getAccountId().equals(ACCOUNT_1) || meta.getAccountId().equals(ACCOUNT_2))
                    .expectNextMatches(meta -> meta.getAccountId().equals(ACCOUNT_1) || meta.getAccountId().equals(ACCOUNT_2))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return correct count of accounts for customer")
        void shouldReturnCorrectCount_forCustomerAccounts() {
            // When
            Long count = accountMetadataService.getAccountsByCustomerId(CUSTOMER_1).count().block();

            // Then
            assertEquals(2L, count, "Customer 1 should have 2 accounts");
        }

        @Test
        @DisplayName("Should return single account for customer with one account")
        void shouldReturnSingleAccount_forCustomerWithOneAccount() {
            // Customer 2 has 1 account (ACCOUNT_3)
            StepVerifier.create(accountMetadataService.getAccountsByCustomerId(CUSTOMER_2))
                    .expectNextMatches(meta -> {
                        assertEquals(ACCOUNT_3, meta.getAccountId());
                        assertEquals("digital_bank", meta.getAccountType());
                        return true;
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return single account for customer 3")
        void shouldReturnSingleAccount_forCustomer3() {
            // Customer 3 has 1 account (ACCOUNT_4)
            StepVerifier.create(accountMetadataService.getAccountsByCustomerId(CUSTOMER_3))
                    .expectNextMatches(meta -> {
                        assertEquals(ACCOUNT_4, meta.getAccountId());
                        assertEquals("savings", meta.getAccountType());
                        return true;
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty flux for unknown customer")
        void shouldReturnEmpty_forUnknownCustomer() {
            // When/Then
            StepVerifier.create(accountMetadataService.getAccountsByCustomerId(UNKNOWN_CUSTOMER))
                    .verifyComplete(); // No elements emitted
        }

        @Test
        @DisplayName("Should return empty flux for null customer ID")
        void shouldReturnEmpty_forNullCustomerId() {
            // When/Then - null customer should not match any accounts
            StepVerifier.create(accountMetadataService.getAccountsByCustomerId(null))
                    .verifyComplete();
        }

        @Test
        @DisplayName("All returned accounts should belong to the requested customer")
        void shouldOnlyReturnAccounts_belongingToCustomer() {
            // When
            accountMetadataService.getAccountsByCustomerId(CUSTOMER_1)
                    .doOnNext(meta -> {
                        // Then - verify each account belongs to the customer
                        assertEquals(CUSTOMER_1, meta.getCustomerId(),
                                "Account " + meta.getAccountId() + " should belong to customer " + CUSTOMER_1);
                    })
                    .blockLast();
        }

        @Test
        @DisplayName("Should return accounts with all metadata populated")
        void shouldReturnAccountsWithFullMetadata() {
            // When
            AccountMetadata account = accountMetadataService.getAccountsByCustomerId(CUSTOMER_1)
                    .filter(meta -> meta.getAccountId().equals(ACCOUNT_1))
                    .blockFirst();

            // Then
            assertNotNull(account);
            assertEquals(ACCOUNT_1, account.getAccountId());
            assertEquals(CUSTOMER_1, account.getCustomerId());
            assertEquals("credit_card", account.getAccountType());
            assertEquals("US_WEST", account.getRegion());
            assertEquals("CA", account.getState());
            assertEquals("VIP", account.getCustomerSegment());
            assertEquals("CREDIT_CARD", account.getLineOfBusiness());
            assertTrue(account.getIsActive());
            assertNotNull(account.getAccountOpenDate());
        }
    }

    // ========================================================================
    // Tests for deriveLineOfBusiness
    // ========================================================================
    @Nested
    @DisplayName("deriveLineOfBusiness Tests")
    class DeriveLineOfBusinessTests {

        @Test
        @DisplayName("Should return CREDIT_CARD for credit card account types")
        void shouldReturnCreditCard_forCreditCardTypes() {
            assertEquals("CREDIT_CARD", accountMetadataService.deriveLineOfBusiness("credit_card"));
            assertEquals("CREDIT_CARD", accountMetadataService.deriveLineOfBusiness("secured_credit_card"));
            assertEquals("CREDIT_CARD", accountMetadataService.deriveLineOfBusiness("CREDIT"));
            assertEquals("CREDIT_CARD", accountMetadataService.deriveLineOfBusiness("card"));
        }

        @Test
        @DisplayName("Should return DIGITAL_BANK for banking account types")
        void shouldReturnDigitalBank_forBankingTypes() {
            assertEquals("DIGITAL_BANK", accountMetadataService.deriveLineOfBusiness("digital_bank"));
            assertEquals("DIGITAL_BANK", accountMetadataService.deriveLineOfBusiness("savings"));
            assertEquals("DIGITAL_BANK", accountMetadataService.deriveLineOfBusiness("checking"));
            assertEquals("DIGITAL_BANK", accountMetadataService.deriveLineOfBusiness("SAVING"));
        }

        @Test
        @DisplayName("Should return CREDIT_CARD for unknown account types")
        void shouldReturnCreditCard_forUnknownTypes() {
            assertEquals("CREDIT_CARD", accountMetadataService.deriveLineOfBusiness("unknown"));
            assertEquals("CREDIT_CARD", accountMetadataService.deriveLineOfBusiness("other"));
            assertEquals("CREDIT_CARD", accountMetadataService.deriveLineOfBusiness("loan"));
        }

        @Test
        @DisplayName("Should return CREDIT_CARD for null account type")
        void shouldReturnCreditCard_forNullType() {
            assertEquals("CREDIT_CARD", accountMetadataService.deriveLineOfBusiness(null));
        }
    }
}
