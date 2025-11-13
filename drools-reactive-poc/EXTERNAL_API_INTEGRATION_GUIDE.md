# External API Integration with Drools - Complete Guide

## Overview

This guide explains how to configure and use **external API calls** with **Drools rule engine** in a **reactive Spring WebFlux** application.

### Your Use Case

You need to:
1. **Fetch pricingId** from Arrangements API
2. **Use that pricingId** to fetch cardholderAgreementsTNCCode from Cardholder Agreements API
3. **Use the TNC code** in Drools rules for document eligibility

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    REQUEST FLOW                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Controller receives request                                 │
│                                                                 │
│  2. REACTIVE API CALLS (parallel and chained):                  │
│     ┌──────────────────────────────────────────┐               │
│     │ CHAIN 1 (Sequential):                    │               │
│     │  ├─ Arrangements API                     │  50ms         │
│     │  │  GET /api/v1/arrangements/{id}        │               │
│     │  │  → pricingId: "PRICING456"            │               │
│     │  │                                        │               │
│     │  └─ Cardholder Agreements API            │  50ms         │
│     │     GET /api/v1/cardholder-agreements/   │               │
│     │          {pricingId}                      │               │
│     │     → TNCCode: "TNC_GOLD_2024"           │               │
│     │                                           │               │
│     │ PARALLEL (at same time):                 │               │
│     │  ├─ Account Service API                  │  50ms         │
│     │  └─ Customer Service API                 │  50ms         │
│     └──────────────────────────────────────────┘               │
│                                                                 │
│  3. ASSEMBLE FACTS:                                             │
│     EnhancedAccountFact {                                       │
│       balance: $15,000                                          │
│       status: "ACTIVE"                                          │
│       pricingId: "PRICING456"         ⭐ From Arrangements API │
│       cardholderAgreementsTNCCode:    ⭐ From chained call     │
│         "TNC_GOLD_2024"                                         │
│     }                                                           │
│                                                                 │
│  4. EXECUTE DROOLS (on dedicated thread pool):                  │
│     - Rules can now use pricingId and TNCCode                   │
│     - Pattern matching: 15ms                                    │
│                                                                 │
│  5. RETURN RESULTS (reactive)                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Configuration

### 1. Application Configuration (`application.yml`)

```yaml
# External API Configuration
external-api:
  # Arrangements API (to fetch pricingId)
  arrangements:
    base-url: ${ARRANGEMENTS_API_URL:http://localhost:8081}

  # Cardholder Agreements API (to fetch TNC code)
  cardholder-agreements:
    base-url: ${CARDHOLDER_AGREEMENTS_API_URL:http://localhost:8082}

  # Account Service API
  account-service:
    base-url: ${ACCOUNT_SERVICE_API_URL:http://localhost:8083}

  # Customer Service API
  customer-service:
    base-url: ${CUSTOMER_SERVICE_API_URL:http://localhost:8084}

  # Timeouts
  timeout:
    connection-ms: 5000    # Connection timeout
    read-ms: 10000         # Read timeout
    write-ms: 10000        # Write timeout
```

**Environment Variables (for different environments):**
```bash
# Development
export ARRANGEMENTS_API_URL=http://localhost:8081
export CARDHOLDER_AGREEMENTS_API_URL=http://localhost:8082

# QA
export ARRANGEMENTS_API_URL=https://qa-arrangements.example.com
export CARDHOLDER_AGREEMENTS_API_URL=https://qa-cardholder.example.com

# Production
export ARRANGEMENTS_API_URL=https://prod-arrangements.example.com
export CARDHOLDER_AGREEMENTS_API_URL=https://prod-cardholder.example.com
```

### 2. WebClient Configuration (`WebClientConfig.java`)

Creates WebClient beans for each external API:

```java
@Configuration
public class WebClientConfig {

    @Bean(name = "arrangementsWebClient")
    public WebClient arrangementsWebClient() {
        return WebClient.builder()
            .baseUrl(arrangementsBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Bean(name = "cardholderAgreementsWebClient")
    public WebClient cardholderAgreementsWebClient() {
        return WebClient.builder()
            .baseUrl(cardholderAgreementsBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
```

---

## Implementation

### Step 1: Define Response Models

**ArrangementResponse.java:**
```java
@Data
public class ArrangementResponse {
    private String arrangementId;
    private String pricingId;        // ⭐ Need this for next API call
    private String productCode;
    private String status;
}
```

**CardholderAgreementResponse.java:**
```java
@Data
public class CardholderAgreementResponse {
    private String pricingId;
    private String cardholderAgreementsTNCCode;  // ⭐ Need this for Drools
    private String effectiveDate;
}
```

### Step 2: Create External API Service

**ExternalApiService.java:**

```java
@Service
public class ExternalApiService {

    @Autowired
    @Qualifier("arrangementsWebClient")
    private WebClient arrangementsWebClient;

    @Autowired
    @Qualifier("cardholderAgreementsWebClient")
    private WebClient cardholderAgreementsWebClient;

    /**
     * Step 1: Get arrangement (to extract pricingId)
     */
    public Mono<ArrangementResponse> getArrangement(String arrangementId) {
        return arrangementsWebClient
            .get()
            .uri("/api/v1/arrangements/{arrangementId}", arrangementId)
            .retrieve()
            .bodyToMono(ArrangementResponse.class)
            .timeout(Duration.ofSeconds(5));
    }

    /**
     * Step 2: Get cardholder agreement (using pricingId)
     */
    public Mono<CardholderAgreementResponse> getCardholderAgreement(String pricingId) {
        return cardholderAgreementsWebClient
            .get()
            .uri("/api/v1/cardholder-agreements/{pricingId}", pricingId)
            .retrieve()
            .bodyToMono(CardholderAgreementResponse.class)
            .timeout(Duration.ofSeconds(5));
    }

    /**
     * ⭐ CHAINED API CALLS
     *
     * Get arrangement → extract pricingId → get cardholder agreement
     */
    public Mono<CardholderAgreementResponse> getCardholderAgreementByArrangement(
        String arrangementId
    ) {
        return getArrangement(arrangementId)           // Step 1
            .flatMap(arrangement -> {                  // Step 2: Use result from Step 1
                String pricingId = arrangement.getPricingId();
                return getCardholderAgreement(pricingId);
            });
    }

    /**
     * ⭐ COMPLETE DATA ASSEMBLY
     *
     * Combines:
     * - CHAINED calls (arrangement → cardholder agreement)
     * - PARALLEL calls (account, customer)
     */
    public Mono<CompleteEligibilityData> assembleCompleteData(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        // Chained calls (sequential)
        Mono<CardholderAgreementResponse> agreementMono =
            getCardholderAgreementByArrangement(arrangementId);

        // Parallel calls
        Mono<AccountFact> accountMono = getAccount(accountId);
        Mono<CustomerFact> customerMono = getCustomer(customerId);

        // Combine all results
        return Mono.zip(accountMono, customerMono, agreementMono)
            .map(tuple -> CompleteEligibilityData.builder()
                .account(tuple.getT1())
                .customer(tuple.getT2())
                .cardholderAgreementsTNCCode(
                    tuple.getT3().getCardholderAgreementsTNCCode()
                )
                .pricingId(tuple.getT3().getPricingId())
                .build()
            );
    }
}
```

### Step 3: Enhanced Account Fact

**EnhancedAccountFact.java:**
```java
@Data
@Builder
public class EnhancedAccountFact {
    // Basic account data
    private String accountId;
    private BigDecimal balance;
    private String status;
    private String accountType;

    // ⭐ Data from external APIs
    private String pricingId;                      // From Arrangements API
    private String cardholderAgreementsTNCCode;    // From Cardholder Agreements API
}
```

### Step 4: Enhanced Reactive Drools Service

**EnhancedReactiveDroolsService.java:**
```java
@Service
public class EnhancedReactiveDroolsService {

    @Autowired
    private ExternalApiService externalApiService;

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    @Qualifier("droolsScheduler")
    private Scheduler droolsScheduler;

    public Mono<Set<String>> getEligibleDocumentsWithExternalApis(
        String customerId,
        String accountId,
        String arrangementId
    ) {
        // ⭐ Step 1: Fetch all data from external APIs
        return externalApiService.assembleCompleteData(customerId, accountId, arrangementId)
            .flatMap(data -> {
                // ⭐ Step 2: Create enhanced fact with all data
                EnhancedAccountFact enhancedAccount = EnhancedAccountFact.builder()
                    .accountId(data.getAccount().getAccountId())
                    .balance(data.getAccount().getBalance())
                    .status(data.getAccount().getStatus())
                    .accountType(data.getAccount().getAccountType())
                    .pricingId(data.getPricingId())                           // ⭐ From API
                    .cardholderAgreementsTNCCode(data.getCardholderAgreementsTNCCode())  // ⭐ From API
                    .build();

                // ⭐ Step 3: Execute Drools on dedicated thread pool
                return Mono.fromCallable(() -> executeRules(enhancedAccount, data.getCustomer()))
                    .subscribeOn(droolsScheduler)
                    .timeout(Duration.ofMillis(500));
            });
    }

    private Set<String> executeRules(EnhancedAccountFact account, CustomerFact customer) {
        KieSession session = kieContainer.newKieSession();
        try {
            DocumentEligibilityResult result = new DocumentEligibilityResult();

            session.insert(account);   // Has pricingId and TNCCode
            session.insert(customer);
            session.insert(result);

            session.fireAllRules();

            return result.getEligibleDocumentIds();
        } finally {
            session.dispose();
        }
    }
}
```

---

## Drools Rules Using External API Data

**enhanced-eligibility.drl:**

```drl
package com.example.droolspoc.rules;

import com.example.droolspoc.model.EnhancedAccountFact;
import com.example.droolspoc.model.CustomerFact;
import com.example.droolspoc.model.DocumentEligibilityResult;

// Rule using TNC Code from external API
rule "Gold TNC Specific Document"
    when
        $account: EnhancedAccountFact(
            cardholderAgreementsTNCCode == "TNC_GOLD_2024"  // ⭐ From external API
        )
        $result: DocumentEligibilityResult()
    then
        $result.addEligibleDocument("DOC-TNC-GOLD-2024-BENEFITS");
end

// Rule using Pricing ID from external API
rule "Premium Pricing Package"
    when
        $account: EnhancedAccountFact(
            pricingId matches "PRICING_PREMIUM_.*"  // ⭐ From external API
        )
        $result: DocumentEligibilityResult()
    then
        $result.addEligibleDocument("DOC-PREMIUM-PRICING-BENEFITS");
end

// Rule combining external API data with account data
rule "High Balance Gold TNC Exclusive"
    when
        $account: EnhancedAccountFact(
            cardholderAgreementsTNCCode == "TNC_GOLD_2024",  // ⭐ External API
            balance > 50000                                   // Account data
        )
        $result: DocumentEligibilityResult()
    then
        $result.addEligibleDocument("DOC-HIGH-BALANCE-GOLD-EXCLUSIVE");
end
```

---

## Timeline & Performance

### Request Timeline

```
Total: ~150ms
├─ Chained API Calls (sequential):
│  ├─ Arrangements API: 50ms
│  └─ Cardholder Agreements API: 50ms (waits for step 1)
│
├─ Parallel API Calls (same time as above):
│  ├─ Account Service API: 50ms
│  └─ Customer Service API: 50ms
│
├─ Thread Scheduling: 5ms
├─ Drools Execution: 15ms
└─ Response Mapping: 5ms
```

**Key Insight:** The chained calls add latency (100ms), but parallel calls happen simultaneously, so total API time is ~100ms not 200ms.

---

## Error Handling

### API Call Failures

```java
public Mono<CardholderAgreementResponse> getCardholderAgreement(String pricingId) {
    return cardholderAgreementsWebClient
        .get()
        .uri("/api/v1/cardholder-agreements/{pricingId}", pricingId)
        .retrieve()
        .bodyToMono(CardholderAgreementResponse.class)
        .timeout(Duration.ofSeconds(5))
        .retry(2)  // Retry up to 2 times
        .onErrorResume(WebClientResponseException.class, error -> {
            if (error.getStatusCode().is4xxClientError()) {
                log.error("Client error fetching cardholder agreement: {}", pricingId);
                return Mono.empty();  // Return empty, don't fail entire request
            }
            return Mono.error(error);  // Server error, propagate
        })
        .onErrorResume(TimeoutException.class, error -> {
            log.error("Timeout fetching cardholder agreement: {}", pricingId);
            return Mono.error(new RuntimeException("API timeout", error));
        });
}
```

---

## Testing

### Mock External APIs

For testing, use WireMock or similar:

```java
@SpringBootTest
@AutoConfigureWireMock(port = 8081)
class ExternalApiIntegrationTest {

    @Test
    void testChainedApiCalls() {
        // Mock Arrangements API
        stubFor(get(urlEqualTo("/api/v1/arrangements/ARR123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "arrangementId": "ARR123",
                      "pricingId": "PRICING456",
                      "productCode": "GOLD_CARD",
                      "status": "ACTIVE"
                    }
                    """)));

        // Mock Cardholder Agreements API
        stubFor(get(urlEqualTo("/api/v1/cardholder-agreements/PRICING456"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "pricingId": "PRICING456",
                      "cardholderAgreementsTNCCode": "TNC_GOLD_2024",
                      "effectiveDate": "2024-01-01"
                    }
                    """)));

        // Test
        Mono<CardholderAgreementResponse> result =
            externalApiService.getCardholderAgreementByArrangement("ARR123");

        StepVerifier.create(result)
            .assertNext(agreement -> {
                assertThat(agreement.getCardholderAgreementsTNCCode())
                    .isEqualTo("TNC_GOLD_2024");
            })
            .verifyComplete();
    }
}
```

---

## Summary

### Key Points

1. **External APIs are called BEFORE Drools**
   - Fetch all data reactively
   - Assemble into fact objects
   - Then execute rules

2. **Chained calls use flatMap**
   - Get arrangement → extract pricingId → get cardholder agreement
   - Sequential execution (100ms total)

3. **Parallel calls use Mono.zip**
   - Account and customer fetched simultaneously
   - Reduces total time

4. **Drools rules use the assembled data**
   - Rules can reference cardholderAgreementsTNCCode
   - Rules can reference pricingId
   - Combined with other account/customer data

5. **Reactive throughout**
   - Non-blocking API calls
   - Drools on dedicated thread pool
   - Fast response times

### Files Created

- `WebClientConfig.java` - WebClient beans for each API
- `ExternalApiService.java` - API integration with chained and parallel calls
- `EnhancedAccountFact.java` - Fact model with external API data
- `EnhancedReactiveDroolsService.java` - Service using external APIs
- `enhanced-eligibility.drl` - Rules using TNC code and pricing ID
- `application.yml` - API configuration

---

**Questions?** Review the code in `ExternalApiService.java` and `EnhancedReactiveDroolsService.java` for complete examples.
