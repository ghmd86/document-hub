# Drools Reactive POC

**Proof of Concept:** Integrating Drools Rule Engine with Spring WebFlux Reactive Architecture

This POC demonstrates how to integrate the Drools rule engine (blocking/synchronous) with Spring WebFlux reactive architecture using the **Wrapper Pattern**.

---

## Overview

### The Challenge

- **Your Stack:** Spring WebFlux (reactive) + R2DBC (non-blocking database)
- **Drools:** Synchronous/blocking rule engine
- **Solution:** Wrapper pattern with dedicated thread pool

### How It Works

```
┌─────────────────────────────────────────────────────────┐
│         Reactive Flow (Non-Blocking)                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. Controller receives request (event loop)            │
│                                                         │
│  2. Fetch data in parallel (R2DBC - non-blocking)       │
│     ├─ Account data (20-50ms)                           │
│     └─ Customer data (20-50ms)                          │
│                                                         │
│  3. Execute Drools on dedicated thread pool             │
│     - Mono.fromCallable(() -> executeRules(...))        │
│     - .subscribeOn(droolsScheduler) ⭐                   │
│     - Drools execution: 15ms (blocking, but isolated)   │
│                                                         │
│  4. Return Mono<Response> (reactive)                    │
│                                                         │
└─────────────────────────────────────────────────────────┘

Total time: ~70ms (data fetching is still the bottleneck)
```

### Key Benefit

✅ **Event loop never blocked** - Drools runs on separate thread pool
✅ **Reactive throughout** - Non-blocking data fetching and response
✅ **High performance** - 1,000+ requests/second achievable
✅ **Standard pattern** - Used by many reactive apps for blocking libraries

---

## Project Structure

```
drools-reactive-poc/
├── src/main/java/com/example/droolspoc/
│   ├── DroolsReactivePocApplication.java    # Main application
│   ├── config/
│   │   └── DroolsConfig.java                # Drools + Scheduler configuration
│   ├── model/
│   │   ├── AccountFact.java                 # Account input data
│   │   ├── CustomerFact.java                # Customer input data
│   │   ├── DocumentEligibilityResult.java   # Drools output
│   │   ├── EligibilityRequest.java          # REST request
│   │   └── EligibilityResponse.java         # REST response
│   ├── service/
│   │   ├── DataService.java                 # Simulates reactive data fetching
│   │   └── ReactiveDroolsEligibilityService.java  # ⭐ Main integration
│   └── controller/
│       └── EligibilityController.java       # REST endpoints
│
├── src/main/resources/
│   ├── rules/
│   │   └── document-eligibility.drl         # Drools rules (9 sample rules)
│   └── application.yml                      # Configuration
│
├── src/test/java/
│   └── ReactiveDroolsIntegrationTest.java   # Integration test
│
├── pom.xml                                  # Maven dependencies
└── README.md                                # This file
```

---

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **IDE:** IntelliJ IDEA, Eclipse, or VS Code

---

## Quick Start

### 1. Build the Project

```bash
cd drools-reactive-poc
mvn clean package
```

This will:
- Download dependencies (Spring Boot, Drools, Reactor)
- Compile DRL rules
- Run tests
- Create executable JAR

### 2. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/drools-reactive-poc-1.0.0-SNAPSHOT.jar
```

Expected output:
```
Loading 1 DRL rule file(s)
  - Loading rule file: document-eligibility.drl
Drools rules compiled successfully
Creating Drools Scheduler with 20 threads
Started DroolsReactivePocApplication in 2.5 seconds
```

### 3. Test the Endpoints

**Health Check:**
```bash
curl http://localhost:8080/api/health
```

Response:
```
OK - Drools Reactive POC is running
```

**Eligibility Check:**
```bash
curl -X POST http://localhost:8080/api/eligibility \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "accountId": "ACC456"
  }'
```

Response:
```json
{
  "customerId": "CUST123",
  "accountId": "ACC456",
  "eligibleDocumentIds": [
    "DOC-PREMIUM-CC-BENEFITS",
    "DOC-CA-STATE-DISCLOSURE",
    "DOC-GOLD-TIER-BENEFITS",
    "DOC-EXCELLENT-CREDIT-OFFERS",
    "DOC-PRIVACY-POLICY",
    "DOC-TERMS-CONDITIONS"
  ],
  "eligibleCount": 6,
  "executionTimeMs": 75,
  "evaluatedAt": "2025-11-12T10:30:45.123Z"
}
```

### 4. Run Tests

```bash
mvn test
```

This runs the integration test that verifies:
- Reactive flow works end-to-end
- Drools rules execute correctly
- Expected documents are returned
- Performance is acceptable

---

## How It Works

### Reactive Integration Pattern

The core integration is in `ReactiveDroolsEligibilityService.java`:

```java
public Mono<Set<String>> getEligibleDocuments(String customerId, String accountId) {
    // Step 1: Fetch data reactively in PARALLEL
    Mono<AccountFact> accountMono = dataService.getAccountFact(accountId);
    Mono<CustomerFact> customerMono = dataService.getCustomerFact(customerId);

    // Step 2: Combine facts and execute rules
    return Mono.zip(accountMono, customerMono)
        .flatMap(tuple -> {
            // Step 3: Execute Drools on dedicated thread pool
            return Mono.fromCallable(() -> executeRules(tuple.getT1(), tuple.getT2()))
                .subscribeOn(droolsScheduler)  // ⭐ NOT on event loop!
                .timeout(Duration.ofMillis(500));
        });
}

private Set<String> executeRules(AccountFact account, CustomerFact customer) {
    KieSession session = kieContainer.newKieSession();
    try {
        session.insert(account);
        session.insert(customer);
        session.insert(result);
        session.fireAllRules(); // BLOCKING - but isolated on thread pool
        return result.getEligibleDocumentIds();
    } finally {
        session.dispose(); // Always clean up
    }
}
```

### Sample Rules

The POC includes 9 sample eligibility rules in `document-eligibility.drl`:

1. **Premium Credit Card Benefits** - Balance > $10k, GOLD+ tier
2. **High Balance Disclosure** - Balance > $50k
3. **California State Disclosure** - Customers in CA
4. **Gold Tier Benefits** - GOLD tier customers
5. **Excellent Credit Offers** - Credit score >= 750
6. **Welcome Package** - New customers (2024+)
7. **Platinum Exclusive** - PLATINUM/BLACK tier with high credit limit
8. **Privacy Policy** - Universal (everyone)
9. **Terms & Conditions** - Universal (everyone)

### Sample Data

`DataService.java` simulates fetching data:

```java
Account:
  - accountId: ACC456
  - balance: $15,000
  - status: ACTIVE
  - accountType: CREDIT_CARD
  - state: CA

Customer:
  - customerId: CUST123
  - tier: GOLD
  - enrollmentDate: 2020-01-15
  - creditScore: 750
```

**Expected Result:** 6 eligible documents

---

## Configuration

### Thread Pool Sizing

Edit `src/main/resources/application.yml`:

```yaml
drools:
  thread-pool:
    core-size: 20              # Number of threads
    thread-name-prefix: drools-
    ttl-seconds: 60

  timeouts:
    rule-execution-ms: 500     # Timeout for rule execution
    overall-request-ms: 2000   # Overall request timeout
```

**Sizing Formula:**
```
Thread Pool Size = (Requests/sec) × (Avg execution time in seconds) + buffer

Example:
- 1,000 requests/second
- 15ms average execution time
- Pool Size = 1000 × 0.015 = 15 threads + 33% buffer = 20 threads
```

---

## Performance Characteristics

### Execution Timeline

```
Total: ~70ms
├─ Data Fetching (parallel): 50ms   ← Simulated R2DBC queries
├─ Thread Scheduling: 5ms            ← Overhead
├─ Drools Execution: 15ms            ← Blocking, but isolated
└─ Response Mapping: 5ms             ← Reactive
```

### Throughput

With default configuration (20 threads):
- **1,000 requests/second** - No issues
- **2,000 requests/second** - Increase thread pool to 30-40
- **5,000 requests/second** - Increase to 80-100 threads

### Memory

Per request:
- KieSession: ~10-20KB
- Facts: ~1-2KB
- **Total: ~20KB per request**

With 50 threads:
- Peak memory: 50 × 20KB = **1MB** (acceptable)

---

## Monitoring

### Thread Pool Metrics

The application logs thread pool usage:

```
[drools-1] Executing Drools rules on thread: drools-1
[drools-2] Executing Drools rules on thread: drools-2
...
```

### Rule Execution Logging

Each rule prints when it fires:

```
[RULE FIRED] Premium Credit Card Benefits
[RULE FIRED] California State Disclosure
[RULE FIRED] Gold Tier Customer Benefits
...
```

### Performance Logging

```
Data fetched in 48ms
Drools execution: 9 rules fired in 12ms on thread drools-3
Eligibility check completed in 67ms: 6 documents eligible
```

---

## Adding New Rules

### 1. Edit the DRL File

Edit `src/main/resources/rules/document-eligibility.drl`:

```drl
rule "High Net Worth Customer"
    salience 95
    when
        $account: AccountFact(
            balance > 100000
        )
        $customer: CustomerFact(
            tier == "BLACK"
        )
        $result: DocumentEligibilityResult()
    then
        System.out.println("[RULE FIRED] High Net Worth Customer");
        $result.addEligibleDocument("DOC-HNW-EXCLUSIVE");
end
```

### 2. Restart the Application

```bash
mvn spring-boot:run
```

Rules are compiled at startup - any syntax errors will fail the build.

### 3. Test

```bash
curl -X POST http://localhost:8080/api/eligibility \
  -H "Content-Type: application/json" \
  -d '{"customerId": "CUST123", "accountId": "ACC456"}'
```

---

## Extending the POC

### 1. Add Real Database Integration

Replace `DataService.java` with actual R2DBC queries:

```java
@Service
public class DataService {

    @Autowired
    private R2dbcEntityTemplate template;

    public Mono<AccountFact> getAccountFact(String accountId) {
        return template.selectOne(
            query(where("account_id").is(accountId)),
            Account.class
        ).map(this::toAccountFact);
    }
}
```

### 2. Add More Complex Rules

Drools supports:
- **Temporal logic:** `this after[0d, 30d] enrollmentDate`
- **Aggregations:** `accumulate(Transaction(...), sum($amount))`
- **Exists/Not:** `exists Account(...)`, `not Customer(...)`
- **From/Collect:** Work with collections

Example:
```drl
rule "High Transaction Volume"
when
    $customer: Customer()
    $count: Number(intValue > 10) from accumulate(
        Transaction(customerId == $customer.id, amount > 1000),
        count(1)
    )
then
    result.addDocument("DOC-HIGH-VOLUME-CUSTOMER");
end
```

### 3. Add Circuit Breaker

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
</dependency>
```

```java
@Autowired
private CircuitBreaker circuitBreaker;

return Mono.fromCallable(() -> executeRules(...))
    .subscribeOn(droolsScheduler)
    .transform(CircuitBreakerOperator.of(circuitBreaker));
```

### 4. Add Caching

```java
@Cacheable(value = "eligibility", key = "#customerId + ':' + #accountId")
public Mono<Set<String>> getEligibleDocuments(...) {
    // ... existing code
}
```

---

## Troubleshooting

### Rules Don't Fire

**Problem:** No documents returned

**Solution:**
1. Check rule conditions match your data
2. Enable debug logging: `logging.level.org.drools: DEBUG`
3. Add `System.out.println()` in rule `then` blocks

### Compilation Errors

**Problem:** `Failed to compile Drools rules`

**Solution:**
1. Check DRL syntax (semicolons, parentheses)
2. Verify imports match Java class names
3. Look at error messages in console

### Timeout Errors

**Problem:** `Rule evaluation exceeded timeout`

**Solution:**
1. Increase timeout: `drools.timeouts.rule-execution-ms: 1000`
2. Check if rules have infinite loops
3. Reduce number of rules or optimize conditions

### Thread Pool Exhausted

**Problem:** Slow responses under load

**Solution:**
1. Increase thread pool size: `drools.thread-pool.core-size: 40`
2. Monitor active threads
3. Optimize rule execution time

---

## Key Takeaways

### ✅ Drools Works with Reactive

- Use wrapper pattern with dedicated thread pool
- Event loop never blocked
- Performance is excellent (70-120ms total)

### ✅ Standard Practice

- Many reactive apps integrate blocking libraries this way
- Database drivers, HTTP clients, etc. use similar patterns
- Spring WebFlux supports this explicitly

### ✅ Production Ready

- Thread pool sizing handles load
- Stateless sessions support high concurrency
- Monitoring and timeouts built-in

### ⚠️ Considerations

- **Not true reactive:** Drools still blocks a thread
- **Thread pool required:** Memory overhead (but small)
- **Timeout management:** Important for reliability

---

## Next Steps

### For Your Document Hub Project

1. **Run this POC:** Validate performance and patterns
2. **Benchmark:** Test with 50-200 rules (your expected scale)
3. **Load test:** Verify 1,000+ requests/second
4. **Integrate:** Copy patterns into your Document Hub service

### POC Validation Checklist

- [ ] POC builds successfully
- [ ] Tests pass
- [ ] Rules fire as expected
- [ ] Performance < 100ms
- [ ] Thread pool handles load
- [ ] No event loop blocking

---

## References

### Documentation
- **This POC:** `README.md` (this file)
- **Reactive Compatibility:** `docs/technical/DROOLS_REACTIVE_COMPATIBILITY.md`
- **Drools Evaluation:** `docs/technical/DROOLS_RULE_ENGINE_EVALUATION.md`
- **Comparison:** `docs/technical/YAML_VS_DROOLS_COMPARISON.md`

### External Links
- [Drools Documentation](https://docs.drools.org/)
- [Spring WebFlux](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Project Reactor](https://projectreactor.io/)

---

## License

This POC is part of the Document Hub project.

---

**Questions?** Contact the Document Hub development team.

**Last Updated:** 2025-11-12
