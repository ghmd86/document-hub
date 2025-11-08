# Disclosure Code Extraction - Complete API Flow

## Overview

This sequence diagram captures the complete extraction logic for the Reactive Disclosure Extractor service. It demonstrates how the system chains multiple API calls to extract disclosure codes for credit card accounts.

## Architecture Summary

The extraction follows a configuration-driven approach where:
1. Client requests disclosure code for an account
2. System reads extraction configuration from JSON
3. Executes chained API calls based on configuration
4. Applies caching, retry logic, and error handling
5. Returns extracted disclosure code with metadata

---

## Complete Extraction Flow Sequence Diagram

```mermaid
sequenceDiagram
    autonumber

    participant Client as Client Application
    participant Controller as DisclosureExtractionController
    participant Service as DisclosureExtractionService
    participant Config as ExtractionConfig
    participant Cache as Redis Cache
    participant CB as Circuit Breaker
    participant Metrics as Metrics Registry
    participant ArrangementsAPI as Arrangements API<br/>(External)
    participant PricingAPI as Pricing API<br/>(External)

    rect rgb(240, 248, 255)
        Note over Client,PricingAPI: 1. Client Request Phase
        Client->>+Controller: POST /api/v1/disclosure/extract<br/>{accountId: "ACC-123456"}
        Controller->>Controller: Validate Request<br/>Generate Correlation ID
        Controller->>Config: Load enhanced_disclosure_extraction.json
        Config-->>Controller: ExtractionConfig Object
        Controller->>+Service: extract(config, {accountId}, correlationId)
        Service->>Metrics: Start Timer (extraction.time)
    end

    rect rgb(255, 250, 240)
        Note over Service,ArrangementsAPI: 2. Step 1: Get Account Arrangements
        Service->>Service: Build Execution Context<br/>{$input.accountId = "ACC-123456"}
        Service->>Service: Get first data source: "getAccountArrangements"

        Service->>Service: Interpolate URL<br/>${$input.accountId} → "ACC-123456"
        Note right of Service: URL: /accounts/ACC-123456/arrangements

        Service->>Cache: Check cache key: "arrangements:ACC-123456"

        alt Cache Hit
            Cache-->>Service: Return cached arrangements data
            Service->>Metrics: Increment cache.hit counter
        else Cache Miss
            Cache-->>Service: null (not found)
            Service->>Metrics: Increment cache.miss counter

            Service->>CB: Check circuit breaker state
            alt Circuit Breaker OPEN
                CB-->>Service: Circuit OPEN - Fail Fast
                Service->>Service: Return error or fallback
            else Circuit Breaker CLOSED
                CB-->>Service: Circuit CLOSED - Proceed

                Service->>Service: Apply retry policy<br/>(max 3 attempts, exponential backoff)

                Service->>+ArrangementsAPI: GET /accounts/ACC-123456/arrangements<br/>Headers: {apikey, x-correlation-Id}

                alt Success Response (200 OK)
                    ArrangementsAPI-->>-Service: 200 OK<br/>{content: [{domain: "PRICING", domainId: "PRICING_789", status: "ACTIVE"}]}
                    Service->>CB: Record success

                    Service->>Service: Parse JSON response
                    Service->>Service: Apply JSONPath extraction<br/>$.content[?(@.domain == 'PRICING' && @.status == 'ACTIVE')].domainId | [0]
                    Service->>Service: Extract fields:<br/>- pricingId: "PRICING_789"<br/>- allPricingIds: ["PRICING_789"]<br/>- arrangementStatus: "ACTIVE"

                    Service->>Service: Apply transforms<br/>(selectFirst on pricingId)
                    Service->>Service: Validate extracted data<br/>- pricingId: required, pattern: ^[A-Z0-9_-]+$

                    alt Validation Passes
                        Service->>Service: Store results in context<br/>getAccountArrangements.pricingId = "PRICING_789"
                        Service->>Cache: Set cache with TTL=1800s<br/>Key: "arrangements:ACC-123456"
                        Cache-->>Service: Cached
                    else Validation Fails
                        Service->>Service: Handle validation error<br/>(action: fail, return default)
                        Service-->>Controller: Validation Error<br/>{success: false, fallback data}
                    end

                else API Error (404 Not Found)
                    ArrangementsAPI-->>Service: 404 Not Found
                    Service->>CB: Record failure
                    Service->>Service: Apply error handling<br/>(on404: fail with message)
                    Service-->>Controller: Error: Account arrangements not found

                else API Error (5xx Server Error)
                    ArrangementsAPI-->>Service: 500/502/503/504
                    Service->>CB: Record failure
                    Service->>Service: Retry with backoff<br/>Attempt 2: wait 100ms<br/>Attempt 3: wait 200ms

                    alt Retry Succeeds
                        ArrangementsAPI-->>Service: 200 OK (on retry)
                        Note right of Service: Continue with successful path
                    else All Retries Exhausted
                        Service->>CB: Update failure count
                        Service->>Metrics: Increment extraction.failure
                        Service-->>Controller: Error: Max retries exceeded
                    end

                else Timeout
                    ArrangementsAPI-->>Service: Request Timeout (5000ms exceeded)
                    Service->>CB: Record failure
                    Service->>Service: Retry with backoff
                    Service-->>Controller: Error: Request timeout
                end
            end
        end
    end

    rect rgb(240, 255, 240)
        Note over Service,PricingAPI: 3. Step 2: Get Pricing Data (Conditional)
        Service->>Service: Check nextCalls conditions
        Service->>Service: Evaluate condition:<br/>field: "pricingId", operator: "notNull"

        alt Condition Met (pricingId exists)
            Service->>Service: Get next data source: "getPricingData"
            Service->>Service: Interpolate URL<br/>${pricingId} → "PRICING_789"
            Note right of Service: URL: /prices/PRICING_789

            Service->>Cache: Check cache key: "pricing:PRICING_789"

            alt Cache Hit
                Cache-->>Service: Return cached pricing data
                Service->>Metrics: Increment cache.hit counter
            else Cache Miss
                Cache-->>Service: null (not found)
                Service->>Metrics: Increment cache.miss counter

                Service->>CB: Check circuit breaker state (getPricingData)

                alt Circuit Breaker CLOSED
                    Service->>Service: Apply retry policy<br/>(max 3 attempts, exponential backoff)

                    Service->>+PricingAPI: GET /prices/PRICING_789<br/>Headers: {apikey, x-correlation-Id}

                    alt Success Response (200 OK)
                        PricingAPI-->>-Service: 200 OK<br/>{cardholderAgreementsTncCode: "DISC_CC_CA_001",<br/>version: "2.1", effectiveDate: "2024-01-01T00:00:00Z"}
                        Service->>CB: Record success

                        Service->>Service: Parse JSON response
                        Service->>Service: Apply JSONPath extraction<br/>- disclosureCode: $.cardholderAgreementsTncCode<br/>- pricingVersion: $.version<br/>- effectiveDate: $.effectiveDate

                        Service->>Service: Extract fields:<br/>- disclosureCode: "DISC_CC_CA_001"<br/>- pricingVersion: "2.1"<br/>- effectiveDate: "2024-01-01T00:00:00Z"

                        Service->>Service: Validate extracted data<br/>- disclosureCode: required, pattern: ^DISC_[A-Z0-9_]+$<br/>- effectiveDate: type: date, validateInPast: true

                        alt Validation Passes
                            Service->>Service: Store results in context<br/>disclosureData.disclosureCode = "DISC_CC_CA_001"<br/>disclosureData.pricingVersion = "2.1"
                            Service->>Cache: Set cache with TTL=3600s<br/>Key: "pricing:PRICING_789"
                            Cache-->>Service: Cached
                        else Validation Fails
                            Service->>Service: Handle validation error<br/>(action: log-and-fail)
                            Service->>Metrics: Increment extraction.failure
                            Service-->>Controller: Validation Error
                        end

                    else API Error (404 Not Found)
                        PricingAPI-->>Service: 404 Not Found
                        Service->>CB: Record failure
                        Service->>Service: Apply error handling<br/>(on404: fail with message)
                        Service-->>Controller: Error: Pricing data not found for pricingId

                    else API Error (5xx Server Error)
                        PricingAPI-->>Service: 500/502/503/504
                        Service->>CB: Record failure
                        Service->>Service: Retry with backoff

                        alt Retry Succeeds
                            PricingAPI-->>Service: 200 OK (on retry)
                            Note right of Service: Continue with successful path
                        else All Retries Exhausted
                            Service->>CB: Update failure count<br/>(Check if threshold reached)
                            alt Failure Threshold Reached (5 failures)
                                CB->>CB: Open Circuit Breaker<br/>Set reset timeout: 60s
                                Note right of CB: Future requests will fail fast<br/>until half-open state
                            end
                            Service->>Metrics: Increment extraction.failure
                            Service-->>Controller: Error: Max retries exceeded
                        end
                    end
                else Circuit Breaker OPEN
                    CB-->>Service: Circuit OPEN - Fail Fast
                    Service->>Service: Return default response<br/>{disclosureCode: "DEFAULT_DISCLOSURE"}
                    Service-->>Controller: Error with fallback
                end
            end

        else Condition Not Met (pricingId is null)
            Service->>Service: Skip getPricingData step
            Service->>Service: Apply error handling default response
            Service->>Service: Build result with fallback data
        end
    end

    rect rgb(255, 240, 245)
        Note over Service,Controller: 4. Build Response & Return
        Service->>Service: Check execution status

        alt All Steps Completed Successfully
            Service->>Service: Build output using outputSchema<br/>- disclosureCode: ${disclosureData.disclosureCode}<br/>- metadata: {pricingId, version, executionTime, cacheHits}
            Service->>Metrics: Stop Timer (execution.time)
            Service->>Metrics: Increment extraction.success counter

            Service->>Service: Create ExtractionResult:<br/>{<br/>  success: true,<br/>  data: {<br/>    getAccountArrangements: {...},<br/>    disclosureData: {...}<br/>  },<br/>  metadata: {<br/>    executionTimeMs: 245,<br/>    cacheHits: 0,<br/>    apiCalls: 2,<br/>    traceId: "correlation-id-123"<br/>  }<br/>}

            Service-->>-Controller: ExtractionResult (success)
            Controller->>Controller: Format response
            Controller-->>-Client: 200 OK<br/>{success: true, data: {...}, metadata: {...}}

        else Execution Failed
            Service->>Service: Build error response<br/>Apply executionRules.errorHandling
            Service->>Metrics: Stop Timer (execution.time)
            Service->>Metrics: Increment extraction.failure counter

            Service->>Service: Create ExtractionResult:<br/>{<br/>  success: false,<br/>  error: {<br/>    code: "EXTRACTION_FAILED",<br/>    message: "...",<br/>    details: "..."<br/>  },<br/>  fallback: {<br/>    disclosureCode: "DEFAULT_DISCLOSURE",<br/>    source: "fallback"<br/>  }<br/>}

            Service-->>Controller: ExtractionResult (failed)
            Controller->>Controller: Log error details
            Controller-->>Client: 200 OK (with error data)<br/>or 500 Internal Server Error<br/>(depending on configuration)
        end
    end

    rect rgb(245, 245, 245)
        Note over Client,Metrics: 5. Monitoring & Observability
        Metrics->>Metrics: Record metrics:<br/>- disclosure.extraction.time<br/>- disclosure.extraction.success<br/>- disclosure.extraction.failure<br/>- disclosure.cache.hit<br/>- disclosure.cache.miss

        Note over Metrics: Metrics available at:<br/>/actuator/prometheus<br/>/actuator/metrics

        alt Slow Request Detected (> 3000ms)
            Metrics->>Metrics: Trigger alert:<br/>slowRequestThresholdMs exceeded
        end

        alt High Error Rate (> 5%)
            Metrics->>Metrics: Trigger alert:<br/>errorRateThreshold exceeded
        end
    end
```

---

## Flow Summary

### Step 1: Initial Request
1. Client sends POST request with `accountId`
2. Controller validates request and generates correlation ID
3. Loads extraction configuration from JSON
4. Initializes extraction service with configuration

### Step 2: Get Account Arrangements
1. Build execution context with input parameters
2. Interpolate URL variables (`${$input.accountId}`)
3. Check Redis cache with pattern: `arrangements:{accountId}`
4. On cache miss: Call external Arrangements API
5. Apply retry policy with exponential backoff (3 attempts)
6. Extract data using JSONPath expressions
7. Apply transformations (selectFirst, uppercase, etc.)
8. Validate extracted data (required fields, regex patterns)
9. Cache result with TTL=1800s (30 minutes)
10. Store results in execution context

### Step 3: Get Pricing Data (Conditional)
1. Evaluate `nextCalls` conditions (`pricingId notNull`)
2. If condition met, proceed to next data source
3. Interpolate URL with previous results (`${pricingId}`)
4. Check Redis cache with pattern: `pricing:{pricingId}`
5. On cache miss: Call external Pricing API
6. Apply circuit breaker pattern (fail fast if open)
7. Extract disclosure code and related fields
8. Validate disclosure code format and date fields
9. Cache result with TTL=3600s (1 hour)
10. Store final results in execution context

### Step 4: Build & Return Response
1. Compile all extracted data from execution context
2. Apply output schema to format final response
3. Calculate execution metadata (time, cache hits, API calls)
4. Record metrics (success/failure counters, timers)
5. Return structured response to client

### Step 5: Error Handling
- **404 Errors**: Return specific error messages or defaults
- **5xx Errors**: Retry with exponential backoff
- **Timeouts**: Retry up to max attempts (5 seconds per request)
- **Validation Errors**: Return fallback values or fail
- **Circuit Breaker Open**: Fail fast with default response

### Step 6: Observability
- **Metrics**: Execution time, success rate, cache hit rate
- **Tracing**: Correlation ID tracked through all calls
- **Alerts**: Slow requests (>3s), high error rate (>5%)
- **Circuit Breaker**: Monitors failure threshold (5 failures)

---

## Configuration-Driven Execution

The entire flow is driven by the `enhanced_disclosure_extraction.json` configuration:

### Key Configuration Elements

1. **Data Sources** (`extractionStrategy`)
   - Define API endpoints, methods, headers
   - Specify cache settings and TTL
   - Define retry policies and timeouts

2. **Response Mapping**
   - JSONPath expressions for data extraction
   - Transformations (selectFirst, uppercase, etc.)
   - Validation rules (required, patterns, types)

3. **Error Handling**
   - Per-status-code actions (404, 5xx)
   - Validation error actions
   - Default/fallback values

4. **Next Calls** (Conditional Chaining)
   - Conditions for executing subsequent calls
   - Dependencies on previous results
   - Target data source references

5. **Execution Rules**
   - Start point and execution mode
   - Global error handling strategy
   - Stop-on-error behavior

6. **Monitoring**
   - Metrics configuration
   - Alert thresholds
   - Circuit breaker settings

---

## Performance Characteristics

### Latency Breakdown

| Scenario | Cache Status | Latency | Notes |
|----------|--------------|---------|-------|
| **Best Case** | Both APIs cached | ~5-10ms | Redis cache hits |
| **Partial Cache** | Arrangements cached | ~130ms | 1 API call + cache |
| **Worst Case** | Both APIs miss | ~250-500ms | 2 API calls |
| **With Retries** | 1 retry needed | ~350-700ms | Includes backoff delay |
| **Circuit Open** | Fail fast | ~2-5ms | No API calls |

### Cache Hit Rates (Production Estimates)

- **Arrangements API**: 80-85% (accounts checked multiple times)
- **Pricing API**: 90-95% (pricing data very stable)
- **Overall**: 85-90% cache hit rate

### Resource Usage

- **Memory**: 256MB idle, 512MB under load
- **Connections**: 10-50 concurrent HTTP connections
- **Redis**: 100-500 keys, ~10MB memory
- **CPU**: 5% idle, 30% under load

---

## Error Scenarios & Recovery

### Scenario 1: Account Not Found
```
1. Client requests ACC-INVALID
2. Arrangements API returns 404
3. Error handling: on404 = "fail"
4. Return: {success: false, error: "Account arrangements not found"}
```

### Scenario 2: No Pricing Arrangement
```
1. Arrangements API returns empty array
2. JSONPath extraction returns null for pricingId
3. Validation fails (required field)
4. Error handling: onValidationError = "fail"
5. Return: {success: false, fallback: {disclosureCode: "DEFAULT_DISCLOSURE"}}
```

### Scenario 3: Pricing API Temporarily Down
```
1. Arrangements API succeeds, pricingId = "PRICING_789"
2. Pricing API returns 503 (Service Unavailable)
3. Retry #1: wait 100ms → 503
4. Retry #2: wait 200ms → 503
5. Circuit breaker increments failure count
6. Return: {success: false, error: "Max retries exceeded"}
```

### Scenario 4: Circuit Breaker Opens
```
1. 5 consecutive failures to Pricing API
2. Circuit breaker opens for 60 seconds
3. Subsequent requests fail fast (no API calls)
4. After 60s, enter half-open state
5. Allow 3 test requests to check recovery
6. If successful, close circuit; if failed, reopen
```

---

## API Dependencies

### External APIs Used

1. **Arrangements API**
   - **Purpose**: Get account arrangements including pricing domain
   - **Endpoint**: `GET /accounts/{accountId}/arrangements`
   - **Response**: Array of arrangements with domain and domainId
   - **Cache TTL**: 1800s (30 minutes)

2. **Pricing API**
   - **Purpose**: Get pricing details including disclosure code
   - **Endpoint**: `GET /prices/{pricingId}`
   - **Response**: Pricing object with cardholderAgreementsTncCode
   - **Cache TTL**: 3600s (1 hour)

### Internal Dependencies

1. **Redis Cache**
   - Store API responses
   - TTL-based expiration
   - Key patterns: `arrangements:{id}`, `pricing:{id}`

2. **Circuit Breaker Registry**
   - One circuit per data source
   - Shared state across instances
   - Failure threshold: 5 failures

3. **Metrics Registry**
   - Micrometer + Prometheus
   - Timers, counters for all operations
   - Available at `/actuator/prometheus`

---

## Testing Scenarios

### Unit Test Cases

1. ✅ Successful extraction with cache miss
2. ✅ Successful extraction with cache hit
3. ✅ Account not found (404)
4. ✅ No pricing arrangement (empty array)
5. ✅ Multiple pricing arrangements (select first active)
6. ✅ Pricing API timeout with retry
7. ✅ Circuit breaker opens after failures
8. ✅ Validation failure on disclosure code format
9. ✅ JSONPath extraction with complex filters
10. ✅ Variable interpolation in URLs and headers

### Integration Test Cases

1. ✅ End-to-end extraction with real Redis
2. ✅ Cache invalidation on TTL expiration
3. ✅ Concurrent requests to same account (cache efficiency)
4. ✅ Circuit breaker recovery (half-open → closed)
5. ✅ Metrics collection and export
6. ✅ Correlation ID propagation through all calls

---

## Production Deployment Considerations

### Scaling
- **Horizontal**: Multiple instances behind load balancer
- **Vertical**: 2GB memory, 2 CPU cores recommended
- **Redis**: Cluster mode for high availability
- **Circuit Breaker**: Shared state via Redis or Hazelcast

### Monitoring
- **Dashboards**: Grafana with Prometheus datasource
- **Alerts**: PagerDuty/Slack for critical errors
- **Tracing**: Distributed tracing with correlation IDs
- **Logs**: Centralized logging (ELK, Splunk)

### Security
- **API Keys**: Stored in environment variables or vault
- **Rate Limiting**: 100 req/s per client
- **Authentication**: JWT tokens for client requests
- **Encryption**: TLS for all API calls

---

## Conclusion

This comprehensive sequence diagram captures the complete extraction logic including:

✅ Configuration-driven execution
✅ Multi-step API chaining with conditions
✅ Redis caching with TTL
✅ Retry logic with exponential backoff
✅ Circuit breaker pattern
✅ JSONPath data extraction
✅ Data validation and transformation
✅ Error handling at each step
✅ Metrics and observability
✅ Correlation ID tracing

The system is production-ready with resilience, performance optimization, and comprehensive error handling.
