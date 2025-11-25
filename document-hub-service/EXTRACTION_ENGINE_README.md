pric# Generic Document Selection with Dynamic Extraction Engine

## Overview

This document describes the generic extraction engine implementation for configuration-driven document selection based on dynamic variable extraction and complex rule evaluation.

## Components Created

### 1. Model/DTO Classes

**Location:** `src/main/java/com/documenthub/model/`

- **dto/DocumentEnquiryRequest.java** - Request DTO for POST /documents-enquiry
- **dto/DocumentEnquiryResponse.java** - Response DTO with extraction metadata
- **extraction/ExtractionConfig.java** - Configuration model parsed from `data_extraction_schema`
- **extraction/ExtractionContext.java** - Runtime context holding variables during extraction
- **extraction/ExtractionResult.java** - Result of extraction execution

### 2. Service Classes

**Location:** `src/main/java/com/documenthub/service/extraction/`

- **GenericExtractionEngine.java** - Main extraction engine orchestrator
- **TransformationService.java** - Applies transformations to extracted data
- **RuleEvaluationService.java** - Evaluates inclusion rules

## Integration with Existing Code

### Step 1: Update SharedDocumentEligibilityService

Replace the existing `isEligible` method to use the extraction engine for `custom_rule` scopes:

```java
@Service
@RequiredArgsConstructor
public class SharedDocumentEligibilityService {

    private final GenericExtractionEngine extractionEngine;
    private final ObjectMapper objectMapper;

    public Mono<Boolean> isEligible(MasterTemplateDefinition template, UUID customerId, UUID accountId) {
        String sharingScope = template.getSharingScope();

        if ("all".equals(sharingScope)) {
            return Mono.just(true);
        }

        if ("credit_card_accounts_only".equals(sharingScope)) {
            return checkCreditCardAccount(accountId);
        }

        if ("custom_rule".equals(sharingScope)) {
            return evaluateCustomRule(template, accountId, customerId);
        }

        return Mono.just(false);
    }

    private Mono<Boolean> evaluateCustomRule(MasterTemplateDefinition template, UUID accountId, UUID customerId) {
        try {
            String schemaJson = template.getDataExtractionSchema();
            ExtractionConfig config = objectMapper.readValue(schemaJson, ExtractionConfig.class);

            String correlationId = UUID.randomUUID().toString();

            return extractionEngine.execute(config, accountId, customerId, correlationId)
                    .map(ExtractionResult::getShouldInclude)
                    .defaultIfEmpty(false);

        } catch (Exception e) {
            log.error("Failed to evaluate custom rule for template: {}", template.getTemplateId(), e);
            return Mono.just(false);
        }
    }
}
```

### Step 2: Update DocumentEnquiryService

Enhance the shared document retrieval to use extraction results for document matching:

```java
private Flux<StorageIndex> getEligibleSharedDocuments(DocumentListRequest request) {
    UUID customerId = request.getCustomerId();
    List<UUID> accountIds = request.getAccountId();
    UUID primaryAccountId = accountIds != null && !accountIds.isEmpty() ? accountIds.get(0) : null;

    long currentTime = Instant.now().getEpochSecond();

    return templateRepository.findActiveSharedDocuments(currentTime)
            .flatMap(template -> {
                String correlationId = UUID.randomUUID().toString();

                return evaluateTemplateWithExtraction(template, customerId, primaryAccountId, correlationId)
                        .flatMapMany(result -> {
                            if (Boolean.TRUE.equals(result.getShouldInclude())) {
                                return fetchDocumentsForTemplate(template, result);
                            }
                            return Flux.empty();
                        });
            });
}

private Mono<ExtractionResult> evaluateTemplateWithExtraction(
        MasterTemplateDefinition template,
        UUID customerId,
        UUID accountId,
        String correlationId) {

    String sharingScope = template.getSharingScope();

    if ("all".equals(sharingScope)) {
        return Mono.just(createSimpleResult());
    }

    if ("custom_rule".equals(sharingScope)) {
        try {
            ExtractionConfig config = objectMapper.readValue(
                template.getDataExtractionSchema(),
                ExtractionConfig.class
            );

            return extractionEngine.execute(config, accountId, customerId, correlationId);
        } catch (Exception e) {
            return Mono.just(ExtractionResult.builder().shouldInclude(false).build());
        }
    }

    return Mono.just(ExtractionResult.builder().shouldInclude(false).build());
}

private Flux<StorageIndex> fetchDocumentsForTemplate(
        MasterTemplateDefinition template,
        ExtractionResult result) {

    MatchingCriteria criteria = result.getMatchingCriteria();

    if ("reference_key".equals(criteria.getMatchBy())) {
        return storageIndexRepository.findByReferenceKeyAndType(
            criteria.getReferenceKeyValue(),
            criteria.getReferenceKeyType()
        );
    } else if ("metadata".equals(criteria.getMatchBy())) {
        // Use JSONB contains query
        return storageIndexRepository.findByDocInfoContaining(criteria.getMetadata());
    } else {
        // template_only
        return storageIndexRepository.findByTemplateId(template.getTemplateId());
    }
}
```

### Step 3: Add Required Dependencies

**pom.xml**:

```xml
<!-- JSONPath for extraction -->
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.8.0</version>
</dependency>

<!-- Resilience4j for circuit breaker and retry -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Step 4: Configure Circuit Breakers

**application.yml**:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 60s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10

  retry:
    instances:
      default:
        maxAttempts: 3
        waitDuration: 100ms
        exponentialBackoffMultiplier: 2
```

### Step 5: Add Repository Methods

**StorageIndexRepository.java**:

```java
public interface StorageIndexRepository extends R2dbcRepository<StorageIndex, UUID> {

    // Existing methods...

    // New methods for extraction-based matching

    @Query("SELECT * FROM storage_index " +
           "WHERE reference_key = :referenceKey " +
           "AND reference_key_type = :referenceKeyType " +
           "AND archive_indicator = false")
    Flux<StorageIndex> findByReferenceKeyAndType(String referenceKey, String referenceKeyType);

    @Query("SELECT * FROM storage_index " +
           "WHERE doc_info @> :metadata::jsonb " +
           "AND archive_indicator = false")
    Flux<StorageIndex> findByDocInfoContaining(Map<String, Object> metadata);

    @Query("SELECT * FROM storage_index " +
           "WHERE template_id = :templateId " +
           "AND archive_indicator = false")
    Flux<StorageIndex> findByTemplateId(UUID templateId);
}
```

### Step 6: Update Entity Classes

**StorageIndex.java** - Ensure these fields exist:

```java
@Table("storage_index")
public class StorageIndex {

    // Existing fields...

    @Column("reference_key")
    private String referenceKey;

    @Column("reference_key_type")
    private String referenceKeyType;

    @Column("doc_info")
    @Type(type = "jsonb")
    private Map<String, Object> docInfo; // JSONB field

    @Column("category")
    private String category; // Denormalized from template
}
```

**MasterTemplateDefinition.java** - Ensure this field exists:

```java
@Table("master_template_definition")
public class MasterTemplateDefinition {

    // Existing fields...

    @Column("data_extraction_schema")
    private String dataExtractionSchema; // JSON configuration
}
```

## Configuration Examples

### Example 1: Disclosure Code Extraction

Store in `master_template_definition.data_extraction_schema`:

```json
{
  "documentMatchingStrategy": {
    "matchBy": "reference_key",
    "referenceKeyType": "DISCLOSURE_CODE"
  },
  "extractionStrategy": [
    {
      "id": "getAccountArrangements",
      "endpoint": {
        "url": "/creditcard/accounts/${$input.accountId}/arrangements",
        "method": "GET",
        "headers": {
          "apikey": "${API_KEY}"
        },
        "timeout": 5000,
        "retryPolicy": {
          "maxAttempts": 3,
          "backoffStrategy": "exponential",
          "initialDelayMs": 100
        }
      },
      "cache": {
        "enabled": true,
        "ttl": 1800,
        "keyPattern": "arrangements:${$input.accountId}"
      },
      "responseMapping": {
        "extract": {
          "pricingId": "$.content[?(@.domain == 'PRICING' && @.status == 'ACTIVE')].domainId | [0]"
        }
      },
      "nextCalls": [
        {
          "condition": {"field": "pricingId", "operator": "notNull"},
          "targetDataSource": "getPricingData"
        }
      ]
    },
    {
      "id": "getPricingData",
      "endpoint": {
        "url": "/pricing-service/prices/${pricingId}",
        "method": "GET"
      },
      "cache": {
        "enabled": true,
        "ttl": 3600,
        "keyPattern": "pricing:${pricingId}"
      },
      "responseMapping": {
        "extract": {
          "disclosureCode": "$.cardholderAgreementsTncCode"
        }
      }
    }
  ],
  "inclusionRules": {
    "operator": "AND",
    "conditions": [
      {"field": "disclosureCode", "operator": "exists"}
    ]
  },
  "outputMapping": {
    "documentReferenceKey": "${disclosureCode}"
  }
}
```

### Example 2: Age-Based Documents

```json
{
  "documentMatchingStrategy": {
    "matchBy": "metadata",
    "metadataFields": {
      "targetAgeGroup": "${ageGroup}"
    }
  },
  "extractionStrategy": [
    {
      "id": "getCustomerProfile",
      "endpoint": {
        "url": "/customer-service/customers/${$input.customerId}",
        "method": "GET"
      },
      "responseMapping": {
        "extract": {
          "dateOfBirth": "$.personalInfo.dateOfBirth"
        },
        "transform": {
          "age": {
            "type": "calculateAge",
            "sourceField": "dateOfBirth"
          },
          "ageGroup": {
            "type": "ageGroupClassification",
            "sourceField": "age",
            "classifications": [
              {"min": 65, "max": 999, "value": "SENIOR"}
            ]
          }
        }
      }
    }
  ],
  "inclusionRules": {
    "operator": "AND",
    "conditions": [
      {"field": "age", "operator": ">=", "value": 65}
    ]
  },
  "outputMapping": {
    "documentMetadata": {
      "targetAgeGroup": "${ageGroup}"
    }
  }
}
```

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class GenericExtractionEngineTest {

    @Mock
    private WebClient webClient;

    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private GenericExtractionEngine extractionEngine;

    @Test
    void shouldExtractDisclosureCode() {
        // Test extraction logic
    }

    @Test
    void shouldHandleCacheHit() {
        // Test caching
    }

    @Test
    void shouldRetryOnFailure() {
        // Test retry logic
    }
}
```

## Monitoring

The extraction engine logs:
- Cache hit/miss rates
- API call counts
- Execution times
- Validation failures
- Circuit breaker state changes

Monitor these metrics in your observability platform.

## Performance Considerations

- **Cache Hit Rate**: Aim for 80-90% with proper TTL configuration
- **Execution Time**: Most extractions should complete in < 500ms with cache
- **Circuit Breaker**: Prevents cascading failures to external APIs
- **Parallel Execution**: Future enhancement for independent data sources

## Troubleshooting

### Issue: Extraction always returns false

**Check:**
1. Extraction configuration JSON is valid
2. External APIs are accessible
3. JSONPath expressions are correct
4. Validation rules are not too strict

### Issue: Poor performance

**Check:**
1. Redis cache is configured and accessible
2. TTL values are appropriate
3. External API response times
4. Circuit breaker not stuck open

## Future Enhancements

1. **Parallel Execution**: Execute independent data sources in parallel
2. **GraphQL Support**: Add GraphQL query support
3. **More Transformations**: Add more built-in transformation functions
4. **UI Builder**: Visual configuration builder for extraction rules
5. **Testing Framework**: Built-in testing for extraction configurations
