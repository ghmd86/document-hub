package com.example.droolspoc;

import com.example.droolspoc.model.EligibilityRequest;
import com.example.droolspoc.model.EligibilityResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test for Reactive Drools POC
 *
 * Tests the complete reactive flow from controller to Drools rules.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReactiveDroolsIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testHealthEndpoint() {
        webTestClient.get()
            .uri("/api/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> assertThat(body).contains("OK"));
    }

    @Test
    void testEligibilityCheck() {
        // Arrange
        EligibilityRequest request = EligibilityRequest.builder()
            .customerId("CUST123")
            .accountId("ACC456")
            .build();

        // Act & Assert
        webTestClient.post()
            .uri("/api/eligibility")
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(EligibilityResponse.class)
            .value(response -> {
                // Verify response structure
                assertThat(response.getCustomerId()).isEqualTo("CUST123");
                assertThat(response.getAccountId()).isEqualTo("ACC456");
                assertThat(response.getEligibleDocumentIds()).isNotNull();
                assertThat(response.getEligibleCount()).isGreaterThan(0);
                assertThat(response.getExecutionTimeMs()).isGreaterThan(0);
                assertThat(response.getEvaluatedAt()).isNotNull();

                // Based on the sample data in DataService:
                // - Account: balance=15000, status=ACTIVE, type=CREDIT_CARD, state=CA
                // - Customer: tier=GOLD, enrollmentDate=2020-01-15, creditScore=750

                // Expected eligible documents based on rules:
                // 1. DOC-PREMIUM-CC-BENEFITS (balance>10000, CREDIT_CARD, GOLD tier)
                // 2. DOC-CA-STATE-DISCLOSURE (state=CA)
                // 3. DOC-GOLD-TIER-BENEFITS (tier=GOLD)
                // 4. DOC-EXCELLENT-CREDIT-OFFERS (creditScore>=750)
                // 5. DOC-PRIVACY-POLICY (universal)
                // 6. DOC-TERMS-CONDITIONS (universal)

                assertThat(response.getEligibleDocumentIds())
                    .contains(
                        "DOC-PREMIUM-CC-BENEFITS",
                        "DOC-CA-STATE-DISCLOSURE",
                        "DOC-GOLD-TIER-BENEFITS",
                        "DOC-EXCELLENT-CREDIT-OFFERS",
                        "DOC-PRIVACY-POLICY",
                        "DOC-TERMS-CONDITIONS"
                    );

                assertThat(response.getEligibleCount()).isEqualTo(6);

                System.out.println("\n✅ Test passed!");
                System.out.println("Eligible documents: " + response.getEligibleDocumentIds());
                System.out.println("Execution time: " + response.getExecutionTimeMs() + "ms");
            });
    }
}
