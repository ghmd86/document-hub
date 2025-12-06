package com.documenthub.service;

import com.documenthub.model.AccountMetadata;
import com.documenthub.model.EligibilityCriteria;
import com.documenthub.model.Rule;
import com.documenthub.model.TemplateConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RuleEvaluationService
 * Tests eligibility criteria evaluation with various scenarios including:
 * - Zipcode filtering (extracted from API)
 * - Credit score based eligibility
 * - Geographic region filtering
 * - Customer segment targeting
 * - Multi-criteria evaluation (AND/OR logic)
 */
public class RuleEvaluationServiceTest {

    private RuleEvaluationService ruleEvaluationService;

    @BeforeEach
    void setUp() {
        ruleEvaluationService = new RuleEvaluationService();
    }

    // ========================================================================
    // SCENARIO 1: Zipcode Filtering (Extracted Field)
    // Use Case: A shared document is only applicable to customers in specific zipcodes
    // ========================================================================
    @Nested
    @DisplayName("Scenario 1: Zipcode Filtering")
    class ZipcodeFilteringTests {

        @Test
        @DisplayName("Should allow access when customer zipcode is in allowed list")
        void shouldAllowAccess_whenZipcodeInAllowedList() {
            // Given: Template config with zipcode eligibility
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("zipcode")
                    .operator("IN")
                    .value(Arrays.asList("10222", "12220", "90210", "123345"))
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            // Simulating extracted data from Customer API
            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("zipcode", "10222");  // Customer lives in allowed zipcode

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "Should allow access for customer in zipcode 10222");
        }

        @Test
        @DisplayName("Should deny access when customer zipcode is NOT in allowed list")
        void shouldDenyAccess_whenZipcodeNotInAllowedList() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("zipcode")
                    .operator("IN")
                    .value(Arrays.asList("10222", "12220", "90210", "123345"))
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("zipcode", "99999");  // Customer NOT in allowed zipcode

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertFalse(result, "Should deny access for customer in zipcode 99999");
        }

        @Test
        @DisplayName("Should deny access when zipcode field is missing from extracted data")
        void shouldDenyAccess_whenZipcodeMissing() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("zipcode")
                    .operator("IN")
                    .value(Arrays.asList("10222", "12220"))
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();
            Map<String, Object> requestContext = new HashMap<>();  // No zipcode extracted

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertFalse(result, "Should deny access when zipcode is not available");
        }
    }

    // ========================================================================
    // SCENARIO 2: Credit Score Based Eligibility
    // Use Case: Premium credit card offers only for customers with credit score >= 750
    // ========================================================================
    @Nested
    @DisplayName("Scenario 2: Credit Score Eligibility")
    class CreditScoreEligibilityTests {

        @Test
        @DisplayName("Should allow premium offer for high credit score (>= 750)")
        void shouldAllowPremiumOffer_whenCreditScoreHigh() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("creditScore")
                    .operator("GREATER_THAN_OR_EQUAL")
                    .value(750)
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("creditScore", 785);  // Extracted from Credit Bureau API

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "Should allow premium offer for credit score 785");
        }

        @Test
        @DisplayName("Should deny premium offer for low credit score (< 750)")
        void shouldDenyPremiumOffer_whenCreditScoreLow() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("creditScore")
                    .operator("GREATER_THAN_OR_EQUAL")
                    .value(750)
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("creditScore", 680);

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertFalse(result, "Should deny premium offer for credit score 680");
        }

        @Test
        @DisplayName("Should allow offer for tiered credit score ranges")
        void shouldEvaluateTieredCreditScore() {
            // Given: Gold tier requires 700-799
            TemplateConfig templateConfig = TemplateConfig.builder()
                .eligibilityCriteria(EligibilityCriteria.builder()
                    .operator("AND")
                    .rules(Arrays.asList(
                        Rule.builder()
                            .field("creditScore")
                            .operator("GREATER_THAN_OR_EQUAL")
                            .value(700)
                            .build(),
                        Rule.builder()
                            .field("creditScore")
                            .operator("LESS_THAN")
                            .value(800)
                            .build()
                    ))
                    .build())
                .build();

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("creditScore", 750);

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "Credit score 750 should qualify for Gold tier (700-799)");
        }
    }

    // ========================================================================
    // SCENARIO 3: Income Based Eligibility
    // Use Case: Wealth management documents for customers with income >= $150,000
    // ========================================================================
    @Nested
    @DisplayName("Scenario 3: Income Based Eligibility")
    class IncomeBasedEligibilityTests {

        @Test
        @DisplayName("Should show wealth management docs for high income customers")
        void shouldShowWealthDocs_whenIncomeHigh() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("annualIncome")
                    .operator("GREATER_THAN_OR_EQUAL")
                    .value(150000)
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("annualIncome", 250000);  // From income verification API

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "Should show wealth docs for income $250,000");
        }

        @Test
        @DisplayName("Should hide wealth management docs for lower income customers")
        void shouldHideWealthDocs_whenIncomeLow() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("annualIncome")
                    .operator("GREATER_THAN_OR_EQUAL")
                    .value(150000)
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("annualIncome", 75000);

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertFalse(result, "Should hide wealth docs for income $75,000");
        }
    }

    // ========================================================================
    // SCENARIO 4: Account Type + Region Combined
    // Use Case: Promotional offer for credit card customers in specific states
    // ========================================================================
    @Nested
    @DisplayName("Scenario 4: Multi-Criteria AND Logic")
    class MultiCriteriaAndLogicTests {

        @Test
        @DisplayName("Should allow access when ALL criteria match (AND)")
        void shouldAllowAccess_whenAllCriteriaMatch() {
            // Given: Offer for VIP credit card customers in CA or NY
            TemplateConfig templateConfig = TemplateConfig.builder()
                .eligibilityCriteria(EligibilityCriteria.builder()
                    .operator("AND")
                    .rules(Arrays.asList(
                        Rule.builder()
                            .field("accountType")
                            .operator("EQUALS")
                            .value("credit_card")
                            .build(),
                        Rule.builder()
                            .field("customerSegment")
                            .operator("EQUALS")
                            .value("VIP")
                            .build(),
                        Rule.builder()
                            .field("state")
                            .operator("IN")
                            .value(Arrays.asList("CA", "NY", "TX"))
                            .build()
                    ))
                    .build())
                .build();

            AccountMetadata accountMetadata = AccountMetadata.builder()
                .accountId(UUID.randomUUID())
                .accountType("credit_card")
                .customerSegment("VIP")
                .state("CA")
                .region("US_WEST")
                .build();

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, new HashMap<>()
            );

            // Then
            assertTrue(result, "VIP credit card customer in CA should see the offer");
        }

        @Test
        @DisplayName("Should deny access when ANY criteria fails (AND)")
        void shouldDenyAccess_whenAnyCriteriaFails() {
            // Given: Same rules as above
            TemplateConfig templateConfig = TemplateConfig.builder()
                .eligibilityCriteria(EligibilityCriteria.builder()
                    .operator("AND")
                    .rules(Arrays.asList(
                        Rule.builder()
                            .field("accountType")
                            .operator("EQUALS")
                            .value("credit_card")
                            .build(),
                        Rule.builder()
                            .field("customerSegment")
                            .operator("EQUALS")
                            .value("VIP")
                            .build(),
                        Rule.builder()
                            .field("state")
                            .operator("IN")
                            .value(Arrays.asList("CA", "NY", "TX"))
                            .build()
                    ))
                    .build())
                .build();

            // Customer is VIP credit card but in FL (not in allowed states)
            AccountMetadata accountMetadata = AccountMetadata.builder()
                .accountId(UUID.randomUUID())
                .accountType("credit_card")
                .customerSegment("VIP")
                .state("FL")  // Not in allowed list
                .region("US_EAST")
                .build();

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, new HashMap<>()
            );

            // Then
            assertFalse(result, "VIP credit card customer in FL should NOT see the offer");
        }
    }

    // ========================================================================
    // SCENARIO 5: OR Logic - Promotional Offer
    // Use Case: Show offer if customer is VIP OR has high credit score OR is in pilot market
    // ========================================================================
    @Nested
    @DisplayName("Scenario 5: Multi-Criteria OR Logic")
    class MultiCriteriaOrLogicTests {

        @Test
        @DisplayName("Should allow access when ANY criteria matches (OR)")
        void shouldAllowAccess_whenAnyCriteriaMatches() {
            // Given: Offer available to VIP customers OR high credit score OR pilot market
            TemplateConfig templateConfig = TemplateConfig.builder()
                .eligibilityCriteria(EligibilityCriteria.builder()
                    .operator("OR")
                    .rules(Arrays.asList(
                        Rule.builder()
                            .field("customerSegment")
                            .operator("EQUALS")
                            .value("VIP")
                            .build(),
                        Rule.builder()
                            .field("creditScore")
                            .operator("GREATER_THAN_OR_EQUAL")
                            .value(800)
                            .build(),
                        Rule.builder()
                            .field("state")
                            .operator("IN")
                            .value(Arrays.asList("CA", "WA"))  // Pilot states
                            .build()
                    ))
                    .build())
                .build();

            // Customer is not VIP, has low credit score, but is in pilot state
            AccountMetadata accountMetadata = AccountMetadata.builder()
                .accountId(UUID.randomUUID())
                .accountType("credit_card")
                .customerSegment("STANDARD")  // Not VIP
                .state("CA")  // In pilot state
                .build();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("creditScore", 650);  // Low credit score

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "Standard customer in CA (pilot state) should see the offer");
        }

        @Test
        @DisplayName("Should deny access when NO criteria matches (OR)")
        void shouldDenyAccess_whenNoCriteriaMatches() {
            // Given: Same OR rules
            TemplateConfig templateConfig = TemplateConfig.builder()
                .eligibilityCriteria(EligibilityCriteria.builder()
                    .operator("OR")
                    .rules(Arrays.asList(
                        Rule.builder()
                            .field("customerSegment")
                            .operator("EQUALS")
                            .value("VIP")
                            .build(),
                        Rule.builder()
                            .field("creditScore")
                            .operator("GREATER_THAN_OR_EQUAL")
                            .value(800)
                            .build(),
                        Rule.builder()
                            .field("state")
                            .operator("IN")
                            .value(Arrays.asList("CA", "WA"))
                            .build()
                    ))
                    .build())
                .build();

            // Customer doesn't match any criteria
            AccountMetadata accountMetadata = AccountMetadata.builder()
                .accountId(UUID.randomUUID())
                .accountType("credit_card")
                .customerSegment("STANDARD")  // Not VIP
                .state("TX")  // Not in pilot states
                .build();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("creditScore", 650);  // Below 800

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertFalse(result, "Customer matching no criteria should NOT see the offer");
        }
    }

    // ========================================================================
    // SCENARIO 6: Product Eligibility Based on Employment
    // Use Case: Self-employed customers get different tax documents
    // ========================================================================
    @Nested
    @DisplayName("Scenario 6: Employment Type Eligibility")
    class EmploymentTypeEligibilityTests {

        @Test
        @DisplayName("Should show self-employed tax forms for self-employed customers")
        void shouldShowSelfEmployedForms_whenEmploymentTypeMatches() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("employmentType")
                    .operator("IN")
                    .value(Arrays.asList("SELF_EMPLOYED", "BUSINESS_OWNER", "FREELANCER"))
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("employmentType", "SELF_EMPLOYED");  // From customer profile API

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "Self-employed customer should see self-employed tax forms");
        }

        @Test
        @DisplayName("Should hide self-employed forms for salaried employees")
        void shouldHideSelfEmployedForms_whenEmployeeIsSalaried() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("employmentType")
                    .operator("IN")
                    .value(Arrays.asList("SELF_EMPLOYED", "BUSINESS_OWNER", "FREELANCER"))
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("employmentType", "SALARIED");

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertFalse(result, "Salaried employee should NOT see self-employed tax forms");
        }
    }

    // ========================================================================
    // SCENARIO 7: Age/Tenure Based Eligibility
    // Use Case: Loyalty rewards for customers with account > 2 years
    // ========================================================================
    @Nested
    @DisplayName("Scenario 7: Account Tenure Eligibility")
    class AccountTenureEligibilityTests {

        @Test
        @DisplayName("Should show loyalty rewards for long-term customers (tenure >= 24 months)")
        void shouldShowLoyaltyRewards_whenTenureHigh() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("accountTenureMonths")
                    .operator("GREATER_THAN_OR_EQUAL")
                    .value(24)
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("accountTenureMonths", 36);  // 3 years

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "3-year customer should see loyalty rewards");
        }

        @Test
        @DisplayName("Should hide loyalty rewards for new customers (tenure < 24 months)")
        void shouldHideLoyaltyRewards_whenTenureLow() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("accountTenureMonths")
                    .operator("GREATER_THAN_OR_EQUAL")
                    .value(24)
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("accountTenureMonths", 6);  // 6 months

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertFalse(result, "6-month customer should NOT see loyalty rewards");
        }
    }

    // ========================================================================
    // SCENARIO 8: String Pattern Matching
    // Use Case: Documents for specific product codes
    // ========================================================================
    @Nested
    @DisplayName("Scenario 8: String Pattern Matching")
    class StringPatternMatchingTests {

        @Test
        @DisplayName("Should match product codes starting with specific prefix")
        void shouldMatchProductCode_withStartsWithOperator() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("productCode")
                    .operator("STARTS_WITH")
                    .value("CC-PLAT")
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("productCode", "CC-PLAT-REWARDS-001");

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "Product code starting with CC-PLAT should match");
        }

        @Test
        @DisplayName("Should match email domains using CONTAINS")
        void shouldMatchEmailDomain_withContainsOperator() {
            // Given: Corporate discounts for specific company domains
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("email")
                    .operator("CONTAINS")
                    .value("@acmecorp.com")
                    .build()
            );

            AccountMetadata accountMetadata = createDefaultAccountMetadata();

            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("email", "john.doe@acmecorp.com");

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, requestContext
            );

            // Then
            assertTrue(result, "Email from acmecorp.com should match");
        }
    }

    // ========================================================================
    // SCENARIO 9: Null/Empty Handling
    // Use Case: Graceful handling when no eligibility criteria defined
    // ========================================================================
    @Nested
    @DisplayName("Scenario 9: Null/Empty Handling")
    class NullEmptyHandlingTests {

        @Test
        @DisplayName("Should allow access when templateConfig is null")
        void shouldAllowAccess_whenTemplateConfigNull() {
            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                (TemplateConfig) null,
                createDefaultAccountMetadata(),
                new HashMap<>()
            );

            // Then
            assertTrue(result, "Null templateConfig should allow access (no restrictions)");
        }

        @Test
        @DisplayName("Should allow access when eligibility criteria is null")
        void shouldAllowAccess_whenEligibilityCriteriaNull() {
            // Given
            TemplateConfig templateConfig = TemplateConfig.builder()
                .eligibilityCriteria(null)
                .build();

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig,
                createDefaultAccountMetadata(),
                new HashMap<>()
            );

            // Then
            assertTrue(result, "Null eligibility criteria should allow access");
        }

        @Test
        @DisplayName("Should allow access when rules list is empty")
        void shouldAllowAccess_whenRulesEmpty() {
            // Given
            TemplateConfig templateConfig = TemplateConfig.builder()
                .eligibilityCriteria(EligibilityCriteria.builder()
                    .operator("AND")
                    .rules(Collections.emptyList())
                    .build())
                .build();

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig,
                createDefaultAccountMetadata(),
                new HashMap<>()
            );

            // Then
            assertTrue(result, "Empty rules list should allow access");
        }
    }

    // ========================================================================
    // SCENARIO 10: NOT_IN Operator (Exclusion Lists)
    // Use Case: Exclude customers in specific states from regulatory documents
    // ========================================================================
    @Nested
    @DisplayName("Scenario 10: Exclusion Lists (NOT_IN)")
    class ExclusionListTests {

        @Test
        @DisplayName("Should allow access when customer is NOT in excluded states")
        void shouldAllowAccess_whenNotInExcludedList() {
            // Given: Document not available in restricted states
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("state")
                    .operator("NOT_IN")
                    .value(Arrays.asList("NY", "NJ", "CT"))  // Restricted states
                    .build()
            );

            AccountMetadata accountMetadata = AccountMetadata.builder()
                .accountId(UUID.randomUUID())
                .state("CA")  // Not in restricted list
                .build();

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, new HashMap<>()
            );

            // Then
            assertTrue(result, "Customer in CA should see the document");
        }

        @Test
        @DisplayName("Should deny access when customer IS in excluded states")
        void shouldDenyAccess_whenInExcludedList() {
            // Given
            TemplateConfig templateConfig = createTemplateConfigWithRules(
                "AND",
                Rule.builder()
                    .field("state")
                    .operator("NOT_IN")
                    .value(Arrays.asList("NY", "NJ", "CT"))
                    .build()
            );

            AccountMetadata accountMetadata = AccountMetadata.builder()
                .accountId(UUID.randomUUID())
                .state("NY")  // In restricted list
                .build();

            // When
            boolean result = ruleEvaluationService.evaluateEligibility(
                templateConfig, accountMetadata, new HashMap<>()
            );

            // Then
            assertFalse(result, "Customer in NY should NOT see the document");
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private TemplateConfig createTemplateConfigWithRules(String operator, Rule... rules) {
        return TemplateConfig.builder()
            .eligibilityCriteria(EligibilityCriteria.builder()
                .operator(operator)
                .rules(Arrays.asList(rules))
                .build())
            .build();
    }

    private AccountMetadata createDefaultAccountMetadata() {
        return AccountMetadata.builder()
            .accountId(UUID.randomUUID())
            .customerId(UUID.randomUUID())
            .accountType("credit_card")
            .region("US_WEST")
            .state("CA")
            .customerSegment("STANDARD")
            .lineOfBusiness("CREDIT_CARD")
            .isActive(true)
            .build();
    }
}
