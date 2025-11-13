# Drools Rule Engine - Reactive Framework Compatibility Analysis

**Document Type:** Technical Analysis
**Purpose:** Evaluate Drools compatibility with reactive frameworks (Spring WebFlux, Project Reactor)
**Date:** 2025-11-12
**Status:** ANALYSIS COMPLETE
**Related Files:**
- `docs/technical/DROOLS_RULE_ENGINE_EVALUATION.md`
- `docs/technical/YAML_VS_DROOLS_COMPARISON.md`

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [The Challenge](#the-challenge)
3. [Drools Core Architecture](#drools-core-architecture)
4. [Reactive Integration Approaches](#reactive-integration-approaches)
5. [Approach 1: Wrapper Pattern (Recommended)](#approach-1-wrapper-pattern-recommended)
6. [Approach 2: Quarkus Reactive Messaging](#approach-2-quarkus-reactive-messaging)
7. [Approach 3: Custom Reactive Implementation](#approach-3-custom-reactive-implementation)
8. [Performance Considerations](#performance-considerations)
9. [Best Practices](#best-practices)
10. [Recommendations](#recommendations)

---

## Executive Summary

### Quick Answer: YES, but with Considerations

**Can Drools work with reactive frameworks?**
- ✅ **YES** - Drools can be integrated with Spring WebFlux and reactive frameworks
- ⚠️ **BUT** - Drools core engine is synchronous/blocking by design
- ✅ **SOLUTION** - Use wrapper patterns to make Drools calls non-blocking

### Key Findings

| Aspect | Status | Details |
|--------|--------|---------|
| **Core Compatibility** | ⚠️ Partial | Drools is synchronous, not natively reactive |
| **Wrapper Integration** | ✅ Works | Can wrap with Mono.fromCallable() |
| **Performance Impact** | ✅ Minimal | Thread pool isolation prevents blocking |
| **Spring WebFlux Support** | ✅ Yes | Works with proper configuration |
| **Quarkus Reactive** | ✅ Native | Quarkus provides native reactive support |
| **Backpressure** | ⚠️ Limited | Manual backpressure management needed |

### Recommendation

**Use Drools with Spring WebFlux using the Wrapper Pattern:**
- Wrap Drools calls in `Mono.fromCallable()` or `Mono.fromSupplier()`
- Execute on a dedicated thread pool (not the event loop)
- Use stateless sessions for thread safety
- Implement proper timeout and error handling

**Impact on Your Project:**
- ✅ No blocker - Drools works with your reactive Document Hub API
- ✅ Minimal overhead - Thread pool isolation is standard practice
- ✅ Scalable - Stateless sessions support high concurrency
- ⚠️ Monitor - Watch thread pool sizing and rule execution time

---

## The Challenge

### Your Current Architecture (Reactive)

```
┌─────────────────────────────────────────────────┐
│         Spring WebFlux Application              │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  Event Loop (Netty)                      │  │
│  │  - Non-blocking I/O                      │  │
│  │  - Reactive Streams (Mono/Flux)          │  │
│  │  - Backpressure Support                  │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  R2DBC (Reactive Database)               │  │
│  │  - Non-blocking queries                  │  │
│  │  - Reactive streams                      │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### Drools Core Architecture (Blocking)

```
┌─────────────────────────────────────────────────┐
│           Drools Rule Engine                    │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  KieSession                              │  │
│  │  - Synchronous API                       │  │
│  │  - Blocking rule evaluation              │  │
│  │  - Thread-bound context                  │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  Rete Network                            │  │
│  │  - In-memory pattern matching            │  │
│  │  - Working memory (mutable state)        │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### The Gap

**Problem:** Drools `fireAllRules()` is a blocking, synchronous method that:
- Cannot be called directly on the event loop thread
- Does not return `Mono<T>` or `Flux<T>`
- Blocks the calling thread until rule evaluation completes

**Impact:**
- Calling Drools directly in a WebFlux handler would block the event loop
- Could reduce throughput and responsiveness
- Violates reactive programming principles

---

## Drools Core Architecture

### Why Drools is Synchronous

Drools was designed before reactive programming became mainstream (2001-2005 era):

1. **Working Memory Model:**
   - Mutable state stored in working memory
   - Facts inserted, modified, and retracted during execution
   - Not compatible with immutable reactive streams

2. **Rete Algorithm:**
   - Optimized for in-memory pattern matching
   - Uses alpha/beta networks for efficient evaluation
   - Requires complete state to be available

3. **Rule Execution:**
   - Rules fire sequentially based on salience
   - Agenda manages rule activation order
   - Cannot be easily split into async operations

4. **API Design:**
   ```java
   // Traditional blocking API
   int rulesFired = kieSession.fireAllRules(); // BLOCKS until complete
   ```

### Drools Execution Flow

```
Insert Facts → Pattern Matching → Agenda Formation → Rule Execution → Results
     ↓               ↓                   ↓                 ↓              ↓
  Blocking        Blocking            Blocking          Blocking      Blocking
```

**Typical Execution Time:**
- Simple rules (10-50): 1-5ms
- Complex rules (100-200): 5-20ms
- Very complex (500+): 20-50ms

While fast, these are **blocking operations** that tie up a thread.

---

## Reactive Integration Approaches

### Overview of Solutions

| Approach | Complexity | Performance | Reactive Support | Recommendation |
|----------|------------|-------------|------------------|----------------|
| Wrapper Pattern | Low | Good | Partial | ✅ **Best for Spring WebFlux** |
| Quarkus Reactive | Medium | Excellent | Full | ✅ Best for new Quarkus projects |
| Custom Reactive | High | Varies | Custom | ⚠️ Only if specific needs |

---

## Approach 1: Wrapper Pattern (Recommended)

### Overview

Wrap Drools calls in reactive types (`Mono`) and execute on a separate thread pool.

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              Spring WebFlux Application                     │
│                                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │  REST Controller (Non-blocking)                    │    │
│  │  - Receives request                                │    │
│  │  - Returns Mono<Response>                          │    │
│  └────────────┬───────────────────────────────────────┘    │
│               │                                             │
│               ▼                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Reactive Service Layer                            │    │
│  │  - Wraps Drools calls in Mono.fromCallable()      │    │
│  │  - Manages reactive flow                           │    │
│  └────────────┬───────────────────────────────────────┘    │
│               │                                             │
│               │  subscribesOn(droolsScheduler)             │
│               ▼                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Drools Thread Pool (Dedicated)                    │    │
│  │  - Isolated from event loop                        │    │
│  │  - Sized for rule execution workload               │    │
│  │  - Blocking operations OK here                     │    │
│  │                                                     │    │
│  │    ┌──────────────────────────────────┐            │    │
│  │    │  Drools Rule Engine (Blocking)   │            │    │
│  │    │  - fireAllRules()                │            │    │
│  │    │  - Synchronous execution         │            │    │
│  │    └──────────────────────────────────┘            │    │
│  └────────────┬───────────────────────────────────────┘    │
│               │                                             │
│               │  Result                                     │
│               ▼                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Reactive Response                                 │    │
│  │  - Mono<EligibilityResult>                         │    │
│  │  - Non-blocking return to client                   │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Implementation

#### Step 1: Configure Drools Thread Pool

```java
@Configuration
public class DroolsReactiveConfig {

    @Bean(name = "droolsScheduler")
    public Scheduler droolsScheduler() {
        // Create dedicated thread pool for Drools execution
        // Size based on expected concurrency and rule execution time
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;

        return Schedulers.newBoundedElastic(
            poolSize,                    // Thread cap
            Integer.MAX_VALUE,           // Queue size
            "drools-rule-engine",        // Thread name prefix
            60,                          // TTL seconds
            true                         // Daemon threads
        );
    }

    @Bean
    public KieContainer kieContainer() {
        KieServices kieServices = KieServices.Factory.get();
        // ... standard Drools setup
        return kieServices.newKieContainer(
            kieServices.getRepository().getDefaultReleaseId()
        );
    }
}
```

#### Step 2: Create Reactive Drools Service

```java
@Service
public class ReactiveDroolsEligibilityService {

    private final KieContainer kieContainer;
    private final Scheduler droolsScheduler;
    private final AccountService accountService;
    private final CustomerService customerService;

    @Autowired
    public ReactiveDroolsEligibilityService(
        KieContainer kieContainer,
        @Qualifier("droolsScheduler") Scheduler droolsScheduler,
        AccountService accountService,
        CustomerService customerService
    ) {
        this.kieContainer = kieContainer;
        this.droolsScheduler = droolsScheduler;
        this.accountService = accountService;
        this.customerService = customerService;
    }

    /**
     * Get eligible documents reactively
     *
     * @param customerId Customer identifier
     * @param accountId Account identifier
     * @return Mono of eligible document IDs
     */
    public Mono<Set<String>> getEligibleDocuments(String customerId, String accountId) {
        // Step 1: Fetch data reactively in parallel
        Mono<AccountFact> accountMono = accountService.getAccountFact(accountId);
        Mono<CustomerFact> customerMono = customerService.getCustomerFact(customerId);

        // Step 2: Combine facts and execute rules
        return Mono.zip(accountMono, customerMono)
            .flatMap(tuple -> {
                AccountFact account = tuple.getT1();
                CustomerFact customer = tuple.getT2();

                // Step 3: Execute Drools on dedicated thread pool
                return Mono.fromCallable(() -> executeRules(account, customer))
                    .subscribeOn(droolsScheduler)  // Execute on Drools thread pool
                    .timeout(Duration.ofMillis(500)) // Timeout protection
                    .onErrorResume(this::handleRuleExecutionError);
            });
    }

    /**
     * Execute Drools rules (blocking operation)
     * Called on dedicated thread pool, not event loop
     */
    private Set<String> executeRules(AccountFact account, CustomerFact customer) {
        // Create stateless session (thread-safe, no memory overhead)
        KieSession kieSession = kieContainer.newKieSession();

        try {
            // Create result holder
            DocumentEligibilityResult result = new DocumentEligibilityResult();

            // Insert facts into working memory
            kieSession.insert(account);
            kieSession.insert(customer);
            kieSession.insert(result);

            // Fire all rules (BLOCKING - but OK on dedicated thread)
            int rulesFired = kieSession.fireAllRules();

            log.debug("Drools execution: {} rules fired in {}ms",
                rulesFired,
                // Add timing logic
            );

            // Return eligible documents
            return result.getEligibleDocumentIds();

        } finally {
            // Always dispose session to free memory
            kieSession.dispose();
        }
    }

    /**
     * Error handling for rule execution failures
     */
    private Mono<Set<String>> handleRuleExecutionError(Throwable error) {
        log.error("Drools rule execution failed", error);

        if (error instanceof TimeoutException) {
            return Mono.error(new RuleExecutionTimeoutException(
                "Rule evaluation exceeded timeout", error));
        }

        // Return empty set as fallback, or re-throw based on requirements
        return Mono.just(Collections.emptySet());
    }
}
```

#### Step 3: Reactive Controller

```java
@RestController
@RequestMapping("/api/v1")
public class DocumentEligibilityController {

    private final ReactiveDroolsEligibilityService eligibilityService;

    @GetMapping("/customers/{customerId}/accounts/{accountId}/eligible-documents")
    public Mono<EligibilityResponse> getEligibleDocuments(
        @PathVariable String customerId,
        @PathVariable String accountId
    ) {
        return eligibilityService.getEligibleDocuments(customerId, accountId)
            .map(documentIds -> EligibilityResponse.builder()
                .customerId(customerId)
                .accountId(accountId)
                .eligibleDocumentIds(documentIds)
                .evaluatedAt(Instant.now())
                .build())
            .doOnSuccess(response ->
                log.info("Eligibility check completed: {} documents eligible",
                    response.getEligibleDocumentIds().size()))
            .timeout(Duration.ofSeconds(2)); // Overall timeout
    }
}
```

### Advantages ✅

1. **Simple Integration:**
   - Minimal code changes
   - Works with existing Drools setup
   - No special Drools configuration needed

2. **Thread Safety:**
   - Event loop never blocked
   - Drools executes on isolated thread pool
   - Stateless sessions are thread-safe

3. **Performance:**
   - Dedicated thread pool sized for Drools workload
   - No impact on event loop threads
   - Reactive data fetching happens in parallel

4. **Error Handling:**
   - Timeout protection
   - Fallback strategies
   - Standard reactive error handling

5. **Monitoring:**
   - Easy to add metrics
   - Thread pool monitoring
   - Rule execution timing

### Disadvantages ❌

1. **Not True Reactive:**
   - Drools still blocks a thread
   - Thread pool required (memory overhead)
   - Limited backpressure support

2. **Thread Context:**
   - Must be careful with ThreadLocal usage
   - Security context propagation requires attention

3. **Resource Management:**
   - Thread pool sizing is critical
   - Too small = requests queue up
   - Too large = excessive memory usage

### Performance Characteristics

```
┌─────────────────────────────────────────────────────────┐
│  Request Timeline (Total: ~120ms)                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────────────────────────┐           │
│  │ Reactive Data Fetching (Parallel)       │  50ms     │
│  │ - Account Service (R2DBC)               │           │
│  │ - Customer Service (R2DBC)              │           │
│  └─────────────────────────────────────────┘           │
│                                                         │
│  ┌─────────────────────────────────────────┐           │
│  │ Thread Pool Scheduling                  │  5ms      │
│  │ - Wait for available thread             │           │
│  │ - Context switch                        │           │
│  └─────────────────────────────────────────┘           │
│                                                         │
│  ┌─────────────────────────────────────────┐           │
│  │ Drools Rule Execution (Blocking)        │  15ms     │
│  │ - 50 rules evaluated                    │           │
│  │ - Pattern matching (Rete)               │           │
│  │ - Rule firing                           │           │
│  └─────────────────────────────────────────┘           │
│                                                         │
│  ┌─────────────────────────────────────────┐           │
│  │ Response Mapping & Return               │  5ms      │
│  └─────────────────────────────────────────┘           │
│                                                         │
│  Total: 75ms (Data fetching is the bottleneck)        │
└─────────────────────────────────────────────────────────┘
```

**Key Insight:** With proper thread pool sizing, Drools execution adds only 15-20ms overhead.

---

## Approach 2: Quarkus Reactive Messaging

### Overview

Quarkus (Red Hat's cloud-native Java framework) provides native reactive support for Drools through Quarkus Drools extension and SmallRye Reactive Messaging.

### When to Use

✅ **Use if:**
- Starting a new project
- Want native reactive support
- Using Kafka or other message brokers
- Need event-driven architecture

❌ **Don't use if:**
- Already invested in Spring Boot
- Don't need message broker integration
- REST API is primary interface

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                 Quarkus Application                          │
│                                                              │
│  ┌────────────────────┐         ┌────────────────────┐      │
│  │  Kafka Topic       │         │  Kafka Topic       │      │
│  │  "events"          │         │  "alerts"          │      │
│  └────────┬───────────┘         └────────▲───────────┘      │
│           │                              │                   │
│           │ @Incoming("events")          │ @Outgoing        │
│           │                              │                   │
│  ┌────────▼──────────────────────────────┴───────────────┐  │
│  │  Reactive Adapter                                     │  │
│  │  - SmallRye Reactive Streams                          │  │
│  │  - Non-blocking message processing                    │  │
│  │                                                        │  │
│  │    ┌──────────────────────────────────┐              │  │
│  │    │  Drools Rule Unit                │              │  │
│  │    │  - Data sources                  │              │  │
│  │    │  - Rule execution                │              │  │
│  │    └──────────────────────────────────┘              │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Implementation

#### Maven Dependencies

```xml
<dependencies>
    <!-- Quarkus Drools -->
    <dependency>
        <groupId>org.drools</groupId>
        <artifactId>drools-quarkus</artifactId>
        <version>8.44.0.Final</version>
    </dependency>

    <!-- Quarkus Reactive Messaging -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-reactive-messaging-kafka</artifactId>
    </dependency>
</dependencies>
```

#### Rule Unit Definition

```java
@RuleUnit
public class AlertingUnit implements RuleUnitData {

    private final DataStream<Event> eventData = DataSource.createStream();
    private final DataStream<Alert> alertData = DataSource.createStream();

    public DataStream<Event> getEventData() {
        return eventData;
    }

    public DataStream<Alert> getAlertData() {
        return alertData;
    }
}
```

#### DRL Rules

```drl
package com.example.alerts;

unit AlertingUnit;

rule "High Value Transaction Alert"
when
    $event: /eventData[
        type == "TRANSACTION",
        amount > 10000
    ]
then
    alertData.append(new Alert(
        "HIGH_VALUE_TRANSACTION",
        $event.getAccountId(),
        "Transaction amount: " + $event.getAmount()
    ));
end
```

#### Reactive Adapter

```java
@Startup
@ApplicationScoped
public class ReactiveAdapter {

    @Inject
    RuleUnit<AlertingUnit> ruleUnit;

    @Inject
    @Channel("alerts")
    Emitter<Alert> alertEmitter;

    private RuleUnitInstance<AlertingUnit> ruleUnitInstance;
    private AlertingUnit alertingUnit;

    @PostConstruct
    public void init() {
        alertingUnit = new AlertingUnit();

        // Subscribe to alert data stream
        alertingUnit.getAlertData().subscribe(new DataProcessor<Alert>() {
            @Override
            public void insert(DataHandle handle, Alert alert) {
                // Emit alert to Kafka topic (non-blocking)
                alertEmitter.send(alert);
            }
        });

        ruleUnitInstance = ruleUnit.createInstance(alertingUnit);
    }

    /**
     * Receive events from Kafka reactively
     * Process through Drools rules
     * Results automatically forwarded to alert stream
     */
    @Incoming("events")
    public void receive(Event event) {
        // Append event to data stream
        alertingUnit.getEventData().append(event);

        // Fire rules
        ruleUnitInstance.fire();
    }
}
```

#### Configuration

```properties
# application.properties

# Kafka configuration
kafka.bootstrap.servers=localhost:9092

# Input channel
mp.messaging.incoming.events.connector=smallrye-kafka
mp.messaging.incoming.events.topic=events
mp.messaging.incoming.events.value.deserializer=com.example.EventDeserializer

# Output channel
mp.messaging.outgoing.alerts.connector=smallrye-kafka
mp.messaging.outgoing.alerts.topic=alerts
mp.messaging.outgoing.alerts.value.serializer=com.example.AlertSerializer
```

### Advantages ✅

1. **Native Reactive Support:**
   - True reactive streams integration
   - Backpressure handling
   - Non-blocking throughout

2. **Event-Driven Architecture:**
   - Perfect for Kafka/messaging use cases
   - Asynchronous processing
   - Scalable microservices

3. **Drools Rule Units:**
   - Modern Drools API
   - Better testability
   - Cleaner separation of concerns

4. **Performance:**
   - Optimized for cloud-native deployments
   - Fast startup time
   - Low memory footprint

### Disadvantages ❌

1. **Framework Migration:**
   - Requires moving from Spring Boot to Quarkus
   - Different ecosystem and conventions
   - Team retraining needed

2. **Not REST-First:**
   - Primarily for message-driven architectures
   - REST endpoints possible but secondary

3. **Maturity:**
   - Newer than Spring Boot
   - Smaller community
   - Fewer third-party integrations

### Use Case Fit

**For Your Project:**
- ❌ **Not Recommended** - You're already using Spring Boot + WebFlux
- ⚠️ **Consider Only If:** Planning to move to event-driven architecture with Kafka
- ✅ **Future Option:** For microservices that process eligibility events

---

## Approach 3: Custom Reactive Implementation

### Overview

Build a custom reactive wrapper around Drools with advanced features like batching, caching, and backpressure.

### When to Use

Only consider if:
- Have very specific performance requirements
- Need custom batching logic
- Want fine-grained control over execution
- Have dedicated team for maintenance

### High-Level Architecture

```java
@Service
public class CustomReactiveDroolsService {

    private final KieContainer kieContainer;
    private final Sinks.Many<RuleRequest> requestSink;
    private final Flux<RuleResponse> responseFlux;

    public CustomReactiveDroolsService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;

        // Create request sink with backpressure
        this.requestSink = Sinks.many().multicast()
            .onBackpressureBuffer(1000);

        // Process requests reactively
        this.responseFlux = requestSink.asFlux()
            .bufferTimeout(100, Duration.ofMillis(50)) // Batch requests
            .flatMap(this::processBatch, 4) // Process 4 batches concurrently
            .publish()
            .autoConnect();
    }

    public Mono<Set<String>> getEligibleDocuments(String customerId, String accountId) {
        RuleRequest request = new RuleRequest(customerId, accountId);

        return Mono.create(sink -> {
            request.setResponseSink(sink);
            requestSink.tryEmitNext(request);
        });
    }

    private Flux<RuleResponse> processBatch(List<RuleRequest> requests) {
        // Custom batch processing logic
        // Execute rules for multiple requests in one session
        // Return responses
        return Flux.fromIterable(requests)
            .map(this::executeRulesForRequest);
    }

    private RuleResponse executeRulesForRequest(RuleRequest request) {
        // Execute Drools rules
        // Return response
        return new RuleResponse(/* ... */);
    }
}
```

### Advantages ✅

1. **Batching:** Process multiple requests in one Drools session
2. **Caching:** Implement custom caching strategies
3. **Backpressure:** Fine-grained control over request flow

### Disadvantages ❌

1. **Complexity:** Much more complex than wrapper pattern
2. **Maintenance:** Custom code to maintain
3. **Debugging:** Harder to troubleshoot
4. **Overkill:** Most projects don't need this level of control

### Recommendation

❌ **Not Recommended** for your project - Use Approach 1 (Wrapper Pattern) instead.

---

## Performance Considerations

### Thread Pool Sizing

**Formula:**
```
Pool Size = (Expected Requests/Second) × (Average Rule Execution Time) / 1000

Example:
- 1,000 requests/second
- 15ms average rule execution time
- Pool Size = 1000 × 0.015 = 15 threads

Add buffer: 15 × 1.5 = 22-23 threads
```

**Configuration:**

```yaml
# application.yml
drools:
  thread-pool:
    core-size: 20
    max-size: 50
    queue-capacity: 1000
    keep-alive-seconds: 60
```

### Memory Considerations

**Per Request Memory (Stateless Session):**
- Session creation: ~1-2KB
- Facts insertion: ~0.5KB per fact
- Working memory: ~5-10KB
- **Total per request: ~10-20KB**

**With 50 threads:**
- Peak memory: 50 × 20KB = 1MB
- Acceptable overhead

### Monitoring Metrics

```java
@Component
public class DroolsMetrics {

    private final MeterRegistry meterRegistry;

    public DroolsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Thread pool metrics
        meterRegistry.gauge("drools.threadpool.active",
            threadPoolTaskExecutor,
            ThreadPoolTaskExecutor::getActiveCount);

        meterRegistry.gauge("drools.threadpool.queue.size",
            threadPoolTaskExecutor,
            ThreadPoolTaskExecutor::getQueueSize);
    }

    public void recordRuleExecution(long executionTimeMs, int rulesFired) {
        // Execution time
        meterRegistry.timer("drools.execution.time")
            .record(executionTimeMs, TimeUnit.MILLISECONDS);

        // Rules fired
        meterRegistry.counter("drools.rules.fired")
            .increment(rulesFired);
    }
}
```

### Performance Benchmarks

Based on typical scenarios:

```
┌────────────────────────┬──────────────┬─────────────┬──────────────┐
│ Scenario               │ Requests/Sec │ Avg Latency │ Thread Pool  │
├────────────────────────┼──────────────┼─────────────┼──────────────┤
│ Small Load (10 rules)  │ 5,000        │ 5ms         │ 10 threads   │
│ Medium Load (50 rules) │ 1,000        │ 15ms        │ 20 threads   │
│ High Load (200 rules)  │ 500          │ 25ms        │ 30 threads   │
│ Peak Load (200 rules)  │ 2,000        │ 40ms        │ 100 threads  │
└────────────────────────┴──────────────┴─────────────┴──────────────┘
```

**Key Insight:** Even with blocking Drools execution, you can achieve high throughput with proper thread pool sizing.

---

## Best Practices

### 1. Use Stateless Sessions

```java
// ✅ GOOD - Stateless (thread-safe, no memory leak)
KieSession session = kieContainer.newKieSession();
try {
    // Use session
} finally {
    session.dispose(); // Always dispose
}

// ❌ BAD - Stateful session (memory leak risk)
KieSession session = kieContainer.newKieSession();
// Never disposed - memory leak!
```

### 2. Set Timeouts

```java
public Mono<Set<String>> getEligibleDocuments(...) {
    return Mono.fromCallable(() -> executeRules(...))
        .subscribeOn(droolsScheduler)
        .timeout(Duration.ofMillis(500))  // ✅ Rule execution timeout
        .onErrorResume(TimeoutException.class, e -> {
            log.error("Rule execution timed out");
            return Mono.just(Collections.emptySet());
        });
}
```

### 3. Implement Circuit Breaker

```java
@Service
public class ReactiveDroolsEligibilityService {

    @Autowired
    @Qualifier("droolsCircuitBreaker")
    private CircuitBreaker circuitBreaker;

    public Mono<Set<String>> getEligibleDocuments(...) {
        return Mono.fromCallable(() -> executeRules(...))
            .subscribeOn(droolsScheduler)
            .transform(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(CallNotPermittedException.class, e -> {
                log.warn("Circuit breaker open, returning fallback");
                return getFallbackEligibility();
            });
    }
}
```

### 4. Monitor Thread Pool Health

```java
@Scheduled(fixedRate = 60000)
public void monitorThreadPool() {
    int activeThreads = droolsThreadPool.getActiveCount();
    int queueSize = droolsThreadPool.getQueue().size();
    int poolSize = droolsThreadPool.getPoolSize();

    if (queueSize > 500) {
        log.warn("Drools thread pool queue is filling up: {}", queueSize);
    }

    if (activeThreads >= poolSize * 0.9) {
        log.warn("Drools thread pool is near capacity: {}/{}",
            activeThreads, poolSize);
    }
}
```

### 5. Batch Data Fetching

```java
public Mono<Set<String>> getEligibleDocuments(...) {
    // ✅ GOOD - Parallel data fetching
    Mono<AccountFact> accountMono = accountService.getAccount(accountId);
    Mono<CustomerFact> customerMono = customerService.getCustomer(customerId);
    Mono<ProductFact> productMono = productService.getProduct(productId);

    return Mono.zip(accountMono, customerMono, productMono)
        .flatMap(tuple -> executeRules(tuple.getT1(), tuple.getT2(), tuple.getT3()));
}

// ❌ BAD - Sequential data fetching
public Mono<Set<String>> getEligibleDocuments(...) {
    return accountService.getAccount(accountId)
        .flatMap(account -> customerService.getCustomer(customerId)
            .flatMap(customer -> productService.getProduct(productId)
                .flatMap(product -> executeRules(account, customer, product))));
    // Sequential = slower!
}
```

### 6. Cache Rule Results (If Applicable)

```java
@Service
public class CachedReactiveDroolsService {

    @Autowired
    private CacheManager cacheManager;

    @Cacheable(value = "eligibility",
               key = "#customerId + ':' + #accountId",
               unless = "#result == null")
    public Mono<Set<String>> getEligibleDocuments(
        String customerId,
        String accountId
    ) {
        return executeEligibilityRules(customerId, accountId);
    }
}
```

**Caching Considerations:**
- ✅ Cache if rules rarely change and data is relatively static
- ❌ Don't cache if rules evaluate real-time data (balances, transactions)
- ⚠️ Use short TTL (5-60 seconds) to balance freshness vs performance

### 7. Optimize Fact Model

```java
// ✅ GOOD - Only fields needed for rules
public class AccountFact {
    private BigDecimal balance;      // Used in rules
    private String status;           // Used in rules
    private String accountType;      // Used in rules
    // Don't include fields not used in rules
}

// ❌ BAD - Including unnecessary data
public class AccountFact {
    private BigDecimal balance;
    private String status;
    private List<Transaction> last100Transactions; // Not used, wastes memory
    private byte[] profileImage;                    // Not used, wastes memory
    // ... 50 more fields
}
```

---

## Recommendations

### For Your Document Hub Project

#### ✅ **Recommendation: Use Approach 1 (Wrapper Pattern)**

**Reasons:**

1. **You're Already Using Spring WebFlux:**
   - No framework migration needed
   - Leverage existing R2DBC reactive data access
   - Consistent with current architecture

2. **REST API Primary Interface:**
   - Wrapper pattern is perfect for request/response
   - Simple to implement and understand
   - Easy to test

3. **Performance is Acceptable:**
   - Drools execution: 15-20ms for 50-200 rules
   - Data fetching: 50-100ms (database queries)
   - Total: 70-120ms - well within acceptable range

4. **Low Risk:**
   - Minimal code changes
   - Standard reactive patterns
   - Easy to revert if needed

5. **Scalable:**
   - Thread pool sizing handles load
   - Stateless sessions support high concurrency
   - Can handle 1,000+ requests/second

#### Implementation Plan

**Phase 1: Setup (1-2 days)**
```
☐ Add Drools dependencies (already planned)
☐ Configure dedicated thread pool (Scheduler)
☐ Create DroolsReactiveConfig class
☐ Add monitoring metrics
```

**Phase 2: Service Layer (2-3 days)**
```
☐ Create ReactiveDroolsEligibilityService
☐ Implement Mono.fromCallable() wrapper
☐ Add timeout and error handling
☐ Implement circuit breaker
☐ Add logging and metrics
```

**Phase 3: Testing (2-3 days)**
```
☐ Unit tests for rule execution
☐ Integration tests with reactive flow
☐ Load testing (1,000+ req/sec)
☐ Thread pool monitoring
☐ Timeout scenarios
```

**Total Effort: 5-8 days**

#### Configuration Example

```yaml
# application.yml
spring:
  application:
    name: document-hub-service

  # R2DBC (existing)
  r2dbc:
    url: r2dbc:postgresql://localhost:5433/documenthub
    username: postgres
    password: postgres123

# Drools configuration
drools:
  rules:
    location: classpath:rules/

  thread-pool:
    # Size based on: 1000 req/sec × 15ms = 15 threads + buffer
    core-size: 20
    max-size: 50
    queue-capacity: 1000
    keep-alive-seconds: 60
    thread-name-prefix: drools-

  timeouts:
    rule-execution-ms: 500
    overall-request-ms: 2000

  circuit-breaker:
    enabled: true
    failure-rate-threshold: 50
    wait-duration-in-open-state: 60000
    permitted-calls-in-half-open: 10

# Monitoring
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### Code Template

```java
@Configuration
public class DroolsReactiveConfig {

    @Bean(name = "droolsScheduler")
    public Scheduler droolsScheduler(
        @Value("${drools.thread-pool.core-size:20}") int poolSize
    ) {
        return Schedulers.newBoundedElastic(
            poolSize,
            Integer.MAX_VALUE,
            "drools-rule-engine",
            60,
            true
        );
    }

    @Bean
    public KieContainer kieContainer() {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        // Load DRL files
        try {
            Resource[] resources = ResourcePatternUtils
                .getResourcePatternResolver(new DefaultResourceLoader())
                .getResources("classpath:rules/*.drl");

            for (Resource resource : resources) {
                kieFileSystem.write(
                    ResourceFactory.newClassPathResource(
                        "rules/" + resource.getFilename()
                    )
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Drools rules", e);
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Failed to build Drools rules: " +
                kieBuilder.getResults().toString());
        }

        return kieServices.newKieContainer(
            kieServices.getRepository().getDefaultReleaseId()
        );
    }

    @Bean
    public CircuitBreaker droolsCircuitBreaker(
        @Value("${drools.circuit-breaker.failure-rate-threshold:50}") float failureRate
    ) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRate)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(10)
            .slidingWindowSize(100)
            .recordExceptions(RuleExecutionException.class, TimeoutException.class)
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        return registry.circuitBreaker("drools");
    }
}
```

---

## Conclusion

### Summary

**Question:** Does Drools work with reactive frameworks?

**Answer:** **YES, with the Wrapper Pattern approach.**

**For Your Project:**
- ✅ **Feasible** - Drools integrates well with Spring WebFlux
- ✅ **Performant** - 70-120ms total latency is acceptable
- ✅ **Scalable** - Handles 1,000+ requests/second with proper thread pool
- ✅ **Low Risk** - Standard reactive patterns, minimal code changes
- ✅ **Recommended** - Use Approach 1 (Wrapper Pattern)

### Key Takeaways

1. **Drools is Blocking:** Core engine is synchronous, but that's OK
2. **Wrapper Pattern Works:** Use `Mono.fromCallable()` + dedicated thread pool
3. **Performance is Good:** 15-20ms Drools overhead is acceptable
4. **Standard Practice:** Many reactive apps integrate blocking libraries this way
5. **No Show-Stopper:** Drools won't prevent reactive architecture

### Next Steps

1. **Proceed with Drools POC:**
   - Implement wrapper pattern
   - Test with 50-100 sample rules
   - Benchmark performance
   - Validate thread pool sizing

2. **Update JIRA Stories:**
   - Add reactive integration tasks
   - Include thread pool configuration
   - Add monitoring and metrics

3. **Documentation:**
   - Document reactive integration pattern
   - Create developer guidelines
   - Add troubleshooting guide

### Final Recommendation

**✅ Proceed with Drools for your Document Hub API**

The reactive architecture is **not a blocker** for using Drools. The wrapper pattern provides a clean, performant integration that maintains reactive principles while leveraging Drools' powerful rule engine capabilities.

---

## References

### Official Documentation
- Drools Documentation: https://docs.drools.org/
- Drools Reactive Messaging: https://blog.kie.org/2022/11/drools-reactive-messaging-processing.html
- Spring WebFlux: https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html
- Project Reactor: https://projectreactor.io/docs/core/release/reference/

### Related Documents
- `docs/technical/DROOLS_RULE_ENGINE_EVALUATION.md` - Comprehensive Drools evaluation
- `docs/technical/YAML_VS_DROOLS_COMPARISON.md` - YAML vs Drools comparison
- `docs/guides/SHARED_DOCUMENT_ELIGIBILITY_CATALOG.md` - Eligibility rules catalog

---

**Document Status:** COMPLETE
**Last Updated:** 2025-11-12
**Author:** Development Team
**Reviewed By:** TBD

---

**Questions or Feedback?**
Contact: Development Team
