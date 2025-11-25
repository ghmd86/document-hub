# Document Hub POC - Architecture Design

## Overview

This document outlines the complete architecture for implementing a Document Hub service that returns documents based on account ownership and complex sharing rules. The system supports multi-step data extraction from external services and dynamic rule evaluation.

---

## Problem Statement

Implement a `/documents-enquire` endpoint that:
1. Returns documents belonging to a specific account (account-specific documents)
2. Returns shared documents based on various rules:
   - Shared with all accounts
   - Shared with specific account types
   - Shared based on custom rules (location, segment, tenure, disclosure codes, etc.)

**Key Requirement**: Make the data extraction and rule evaluation fully configurable via JSON stored in the database, allowing non-technical teams to configure document retrieval logic.

---

## Technology Stack

- **Java 17**
- **Spring Boot 2.x** with **WebFlux** (Reactive/Non-blocking)
- **Spring Data R2DBC** (Reactive database access)
- **PostgreSQL** with JSONB support
- **Redis** (Caching layer)
- **WebClient** (Reactive HTTP client for external API calls)

---

## Database Schema

### Tables

#### 1. `master_template_definition`
Stores document template metadata and configuration.

**Key Columns:**
- `master_template_id` (UUID) - Primary key
- `template_version` (int) - Version number
- `template_type` (varchar) - Type of document (e.g., "Statement", "PrivacyNotice")
- `line_of_business` (varchar) - Business line (e.g., "CREDIT_CARD", "DIGITAL_BANK")
- `is_shared_document` (boolean) - Whether this is a shared document
- `sharing_scope` (varchar) - Scope of sharing (e.g., "ALL", "ACCOUNT_TYPE", "CONDITIONAL")
- **`data_extraction_config` (jsonb)** - Multi-step extraction configuration
- **`access_control` (jsonb)** - Eligibility rules
- `is_active` (boolean) - Template active status
- `start_date`, `end_date` (long) - Validity period

#### 2. `storage_index`
Stores document metadata and references to actual document storage.

**Key Columns:**
- `storage_index_id` (UUID) - Primary key
- `master_template_id` (UUID) - References template
- `template_version` (int) - Template version
- `template_type` (varchar) - Document type
- `reference_key` (varchar) - Reference key (e.g., statement ID, disclosure code)
- `reference_key_type` (varchar) - Type of reference key
- `is_shared` (boolean) - Whether document is shared
- `account_key` (UUID) - Account ID (for account-specific docs)
- `customer_key` (UUID) - Customer ID
- **`doc_metadata` (jsonb)** - Additional metadata for matching
- `storage_document_key` (UUID) - Key to retrieve actual document
- `doc_creation_date` (long) - Creation timestamp

#### 3. `template_vendor_mapping`
Maps templates to vendor-specific configurations.

**Key Columns:**
- `template_vendor_id` (UUID) - Primary key
- `master_template_id` (UUID) - References template
- `vendor` (enum) - Vendor name
- `api_config` (jsonb) - Vendor API configuration

---

## Core Architecture Concepts

### 1. Multi-Step Data Extraction

Templates can define **sequential or parallel API calls** to extract data needed for document matching and eligibility evaluation.

**Example Use Case**: Fetching disclosure code
1. **Step 1**: Call Account Service → Get `pricingId`
2. **Step 2**: Call Pricing Service with `pricingId` → Get `disclosureCode`
3. **Step 3**: Use `disclosureCode` to match documents in `storage_index`

**Key Features:**
- **Chaining**: Output from Step 1 becomes input to Step 2
- **Conditional Execution**: Execute Step 2 only if Step 1 returns valid data
- **Placeholder Resolution**: URLs use `${variableName}` syntax
- **JSONPath Extraction**: Extract specific fields from JSON responses
- **Caching**: Cache API responses with configurable TTL
- **Parallel Execution**: Some steps can run in parallel for performance

### 2. Access Control Rules

Templates define **eligibility criteria** to determine which users can see which documents.

**Rule Types:**
- `ACCOUNT_TYPE`: Match account type (e.g., credit_card, digital_bank)
- `REGION`: Match customer location (state, country)
- `SEGMENT`: Match customer segment (VIP, Enterprise)
- `TENURE`: Match account age (e.g., > 1 year)
- `ACCOUNT_STATUS`: Match account status (active, delinquent)
- `DISCLOSURE_CODE`: Match specific disclosure requirements
- `DATE_RANGE`: Match document validity dates

**Logic Operators:**
- `AND`: All rules must pass
- `OR`: At least one rule must pass
- Supports nested rule groups

### 3. Document Matching Strategies

After extracting data and validating eligibility, the system matches documents from `storage_index`.

**Matching Types:**
- **Reference Key**: Match by `reference_key` and `reference_key_type`
- **Metadata Fields**: Match by JSONB fields in `doc_metadata`
- **Composite**: Combine multiple conditions
- **Account Key**: Direct match by `account_key`

**Selection Strategies:**
- `LATEST_VERSION`: Return the most recent version
- `LATEST_BY_DATE`: Return the newest by creation date
- `ALL_MATCHES`: Return all matching documents

---

## Configuration Schema

### A. `data_extraction_config` (JSONB)

Defines how to extract data from external services.

```json
{
  "executionRules": {
    "startFrom": "step1Id",
    "executionMode": "sequential",
    "stopOnError": true,
    "errorHandling": {
      "strategy": "fail-fast",
      "defaultResponse": {}
    }
  },
  "extractionStrategy": [
    {
      "id": "step1Id",
      "description": "Description of what this step does",
      "endpoint": {
        "url": "/api/resource/${$input.accountId}",
        "method": "GET|POST",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${token}"
        }
      },
      "cache": {
        "enabled": true,
        "ttl": 1800,
        "keyPattern": "cache:${$input.accountId}"
      },
      "responseMapping": {
        "extract": {
          "fieldName": "$.jsonPath.to.field",
          "anotherField": "$.another.path"
        },
        "validate": {
          "fieldName": {
            "type": "string",
            "required": true,
            "pattern": "^[A-Z0-9_]+$"
          }
        },
        "transform": {
          "computedField": {
            "type": "classification",
            "expression": "${fieldName} in ['VALUE1', 'VALUE2']"
          }
        }
      },
      "dependencies": ["previousFieldName"],
      "nextCalls": [
        {
          "condition": {
            "field": "fieldName",
            "operator": "notNull|equals|greaterThan|lessThan|in",
            "value": "expectedValue"
          },
          "targetDataSource": "step2Id"
        }
      ],
      "errorHandling": {
        "on404": {
          "action": "fail|skip|use-default",
          "message": "Error message"
        },
        "on5xx": {
          "action": "retry",
          "maxAttempts": 3
        }
      }
    }
  ],
  "documentMatchingStrategy": {
    "matchBy": "reference_key|metadata_fields|composite|account_key",
    "referenceKeyType": "STATEMENT_ID|DISCLOSURE_CODE|...",
    "referenceKeySource": "${extractedFieldName}",
    "conditions": [
      {
        "type": "reference_key|metadata_field|date_range",
        "metadataKey": "doc_metadata_field_name",
        "valueSource": "${extractedFieldName}",
        "operator": "EQUALS|IN|GREATER_THAN|LESS_THAN",
        "priority": 1
      }
    ],
    "selectionStrategy": "LATEST_VERSION|LATEST_BY_DATE|ALL_MATCHES",
    "filters": [
      {
        "metadataKey": "status",
        "operator": "EQUALS",
        "value": "ACTIVE"
      }
    ]
  }
}
```

**Key Concepts:**

- **`$input.fieldName`**: References fields from the incoming request (e.g., `$input.accountId`, `$input.customerId`)
- **`${extractedField}`**: References fields extracted from previous API calls
- **JSONPath**: Uses `$.path.to.field` syntax to extract data from JSON responses
- **Chaining**: `nextCalls` defines conditional execution of subsequent steps
- **Dependencies**: `dependencies` array ensures a step only runs if required fields are available

### B. `access_control` (JSONB)

Defines who can see this document template.

```json
{
  "eligibility_type": "ACCOUNT_OWNED|SHARED_WITH_ALL|SHARED_BY_ACCOUNT_TYPE|CONDITIONAL",
  "eligibilityCriteria": {
    "ruleId1": {
      "type": "ACCOUNT_TYPE|REGION|SEGMENT|TENURE|ACCOUNT_STATUS|DISCLOSURE_CODE",
      "field": "account.type",
      "operator": "IN_LIST|EQUALS|GREATER_THAN|LESS_THAN|CONTAINS",
      "values": ["credit_card", "digital_bank"],
      "value": "single_value",
      "dataSource": "extractionStepId",
      "computed": false
    },
    "ruleId2": {
      "type": "TENURE",
      "field": "account.tenure_days",
      "operator": "GREATER_THAN",
      "value": 365,
      "computed": true,
      "computation": "days_between(current_date, account.open_date)"
    }
  },
  "logic": "AND|OR",
  "ruleGroups": [
    {
      "group_id": "primary_eligibility",
      "rules": ["ruleId1", "ruleId2"],
      "logic": "AND"
    }
  ],
  "groupLogic": "OR"
}
```

**Eligibility Types:**

1. **`ACCOUNT_OWNED`**: Only the account owner can see these documents
2. **`SHARED_WITH_ALL`**: All users can see these documents
3. **`SHARED_BY_ACCOUNT_TYPE`**: Users with matching account type can see
4. **`CONDITIONAL`**: Complex rules determine eligibility

---

## Template Configuration Examples

### Example 1: Simple Statement (No External Calls)

**Use Case**: Return account statements by statement ID

```json
{
  "template_type": "Statement",
  "template_version": 1,
  "is_shared_document": false,
  "data_extraction_config": {
    "extractionStrategy": [],
    "documentMatchingStrategy": {
      "matchBy": "account_key"
    }
  },
  "access_control": {
    "eligibility_type": "ACCOUNT_OWNED"
  }
}
```

**Flow:**
1. No external API calls needed
2. Match documents where `account_key = request.accountId`
3. Return all matched documents

---

### Example 2: Privacy Policy (Location-Based Shared Document)

**Use Case**: Return state-specific privacy policies

```json
{
  "template_type": "PrivacyNotice",
  "template_version": 1,
  "is_shared_document": true,
  "sharing_scope": "LOCATION_BASED",
  "data_extraction_config": {
    "extractionStrategy": [
      {
        "id": "getCustomerLocation",
        "endpoint": {
          "url": "/api/customers/${$input.customerId}",
          "method": "GET",
          "timeout": 3000
        },
        "responseMapping": {
          "extract": {
            "customerState": "$.address.state",
            "customerCountry": "$.address.country"
          }
        }
      }
    ],
    "documentMatchingStrategy": {
      "matchBy": "metadata_fields",
      "conditions": [
        {
          "metadataKey": "target_state",
          "valueSource": "${customerState}",
          "operator": "EQUALS"
        }
      ]
    }
  },
  "access_control": {
    "eligibility_type": "SHARED_WITH_ALL"
  }
}
```

**Flow:**
1. Call Customer Service to get customer's state
2. Match documents where `doc_metadata->>'target_state' = customerState`
3. All users are eligible (shared with all)

---

### Example 3: Cardholder Agreement (Multi-Step with Disclosure Code)

**Use Case**: Return cardholder agreement based on pricing arrangement

```json
{
  "template_type": "CardholderAgreement",
  "template_version": 1,
  "is_shared_document": false,
  "data_extraction_config": {
    "extractionStrategy": [
      {
        "id": "getAccountArrangements",
        "description": "Get pricing ID from account arrangements",
        "endpoint": {
          "url": "/creditcard/accounts/${$input.accountId}/arrangements",
          "method": "GET",
          "timeout": 5000
        },
        "cache": {
          "enabled": true,
          "ttl": 1800,
          "keyPattern": "arrangements:${$input.accountId}"
        },
        "responseMapping": {
          "extract": {
            "pricingId": "$.content[?(@.domain == 'PRICING' && @.status == 'ACTIVE')].domainId | [0]",
            "accountType": "$.content[?(@.domain == 'PRODUCT')].domainId | [0]"
          },
          "validate": {
            "pricingId": {
              "type": "string",
              "required": true
            }
          }
        },
        "nextCalls": [
          {
            "condition": {
              "field": "pricingId",
              "operator": "notNull"
            },
            "targetDataSource": "getPricingData"
          }
        ]
      },
      {
        "id": "getPricingData",
        "description": "Fetch disclosure code using pricing ID",
        "dependencies": ["pricingId"],
        "endpoint": {
          "url": "/enterprise/product-management-system/pricing-management-service/prices/${pricingId}",
          "method": "GET",
          "timeout": 5000
        },
        "cache": {
          "enabled": true,
          "ttl": 3600,
          "keyPattern": "pricing:${pricingId}"
        },
        "responseMapping": {
          "extract": {
            "disclosureCode": "$.cardholderAgreementsTncCode",
            "pricingVersion": "$.version"
          },
          "validate": {
            "disclosureCode": {
              "type": "string",
              "required": true,
              "pattern": "^DISC_[A-Z0-9_]+$"
            }
          }
        }
      }
    ],
    "documentMatchingStrategy": {
      "matchBy": "composite",
      "strategy": "ALL_CONDITIONS_MATCH",
      "conditions": [
        {
          "type": "reference_key",
          "referenceKeyType": "DISCLOSURE_CODE",
          "valueSource": "${disclosureCode}"
        },
        {
          "type": "metadata_field",
          "metadataKey": "pricing_version",
          "valueSource": "${pricingVersion}",
          "operator": "EQUALS"
        }
      ],
      "selectionStrategy": "LATEST_VERSION"
    }
  },
  "access_control": {
    "eligibility_type": "CONDITIONAL",
    "eligibilityCriteria": {
      "accountType": {
        "type": "IN_LIST",
        "field": "account.type",
        "values": ["credit_card", "secured_card"],
        "dataSource": "getAccountArrangements"
      }
    }
  }
}
```

**Flow:**
1. Call Account Arrangements API → Extract `pricingId` and `accountType`
2. Validate `pricingId` is not null
3. Call Pricing API with `pricingId` → Extract `disclosureCode` and `pricingVersion`
4. Check eligibility: accountType must be in ["credit_card", "secured_card"]
5. Match documents:
   - `reference_key = disclosureCode`
   - `reference_key_type = 'DISCLOSURE_CODE'`
   - `doc_metadata->>'pricing_version' = pricingVersion`
6. Return latest version

---

### Example 4: VIP Customer Letter (Three-Step Chain)

**Use Case**: Personalized letters for VIP customers

```json
{
  "template_type": "VIPCustomerLetter",
  "template_version": 1,
  "is_shared_document": true,
  "sharing_scope": "SEGMENT_BASED",
  "data_extraction_config": {
    "executionRules": {
      "executionMode": "sequential",
      "stopOnError": true
    },
    "extractionStrategy": [
      {
        "id": "getCustomerProfile",
        "endpoint": {
          "url": "/api/customers/${$input.customerId}",
          "method": "GET",
          "timeout": 3000
        },
        "responseMapping": {
          "extract": {
            "customerSegmentId": "$.segmentId",
            "customerTier": "$.tier"
          }
        },
        "nextCalls": [
          {
            "condition": {
              "field": "customerSegmentId",
              "operator": "notNull"
            },
            "targetDataSource": "getSegmentDetails"
          }
        ]
      },
      {
        "id": "getSegmentDetails",
        "dependencies": ["customerSegmentId"],
        "endpoint": {
          "url": "/api/segments/${customerSegmentId}",
          "method": "GET",
          "timeout": 3000
        },
        "responseMapping": {
          "extract": {
            "segmentName": "$.name",
            "segmentBenefits": "$.benefits"
          },
          "transform": {
            "isVIP": {
              "type": "classification",
              "expression": "${segmentName} in ['PLATINUM', 'DIAMOND', 'VIP']"
            }
          }
        },
        "nextCalls": [
          {
            "condition": {
              "field": "isVIP",
              "operator": "equals",
              "value": true
            },
            "targetDataSource": "getVIPOffers"
          }
        ]
      },
      {
        "id": "getVIPOffers",
        "dependencies": ["customerSegmentId"],
        "endpoint": {
          "url": "/api/offers/vip/${$input.customerId}",
          "method": "GET",
          "timeout": 3000
        },
        "responseMapping": {
          "extract": {
            "activeOffers": "$.offers[?(@.status == 'ACTIVE')]"
          }
        }
      }
    ],
    "documentMatchingStrategy": {
      "matchBy": "metadata_fields",
      "conditions": [
        {
          "metadataKey": "customer_id",
          "valueSource": "${$input.customerId}",
          "operator": "EQUALS"
        },
        {
          "metadataKey": "segment_name",
          "valueSource": "${segmentName}",
          "operator": "EQUALS"
        }
      ],
      "selectionStrategy": "LATEST_BY_DATE"
    }
  },
  "access_control": {
    "eligibility_type": "CONDITIONAL",
    "eligibilityCriteria": {
      "vipStatus": {
        "type": "BOOLEAN_CHECK",
        "field": "isVIP",
        "expectedValue": true,
        "dataSource": "getSegmentDetails"
      }
    }
  }
}
```

**Flow:**
1. Step 1: Get customer profile → Extract `customerSegmentId`
2. Step 2: Get segment details → Extract `segmentName`, compute `isVIP`
3. Step 3: If VIP, get active offers
4. Check eligibility: `isVIP == true`
5. Match documents by customer_id and segment_name
6. Return latest document

---

## Implementation Components

### 1. Entity Classes

#### `MasterTemplateDefinitionEntity`
```java
@Table("master_template_definition")
@Data
public class MasterTemplateDefinitionEntity {
    @Id
    private UUID masterTemplateId;
    private Integer templateVersion;
    private String templateType;
    private String lineOfBusiness;
    private Boolean isSharedDocument;
    private String sharingScope;

    @Column("data_extraction_config")
    private JsonNode dataExtractionConfig;

    @Column("access_control")
    private JsonNode accessControl;

    private Boolean isActive;
    private Long startDate;
    private Long endDate;
}
```

#### `StorageIndexEntity`
```java
@Table("storage_index")
@Data
public class StorageIndexEntity {
    @Id
    private UUID storageIndexId;
    private UUID masterTemplateId;
    private Integer templateVersion;
    private String templateType;
    private String referenceKey;
    private String referenceKeyType;
    private Boolean isShared;
    private UUID accountKey;
    private UUID customerKey;

    @Column("doc_metadata")
    private JsonNode docMetadata;

    private UUID storageDocumentKey;
    private Long docCreationDate;
}
```

### 2. Configuration Models

#### `DataExtractionConfig`
```java
@Data
public class DataExtractionConfig {
    private ExecutionRules executionRules;
    private List<DataSourceConfig> extractionStrategy;
    private DocumentMatchingStrategy documentMatchingStrategy;
}

@Data
public class DataSourceConfig {
    private String id;
    private String description;
    private EndpointConfig endpoint;
    private CacheConfig cache;
    private ResponseMapping responseMapping;
    private List<String> dependencies;
    private List<NextCall> nextCalls;
    private ErrorHandlingConfig errorHandling;
}

@Data
public class ResponseMapping {
    private Map<String, String> extract;       // JSONPath expressions
    private Map<String, ValidationRule> validate;
    private Map<String, TransformConfig> transform;
}

@Data
public class NextCall {
    private Condition condition;
    private String targetDataSource;
}
```

#### `AccessControl`
```java
@Data
public class AccessControl {
    private String eligibilityType;
    private Map<String, EligibilityCriteria> eligibilityCriteria;
    private String logic;  // AND/OR
    private List<RuleGroup> ruleGroups;
}

@Data
public class EligibilityCriteria {
    private String type;
    private String field;
    private String operator;
    private List<String> values;
    private Object value;
    private String dataSource;
    private Boolean computed;
    private String computation;
}
```

### 3. Core Services

#### `DataExtractionEngine`
- Executes multi-step extraction strategies
- Handles API call chaining
- Resolves placeholders (${variable})
- Extracts data using JSONPath
- Manages caching
- Handles errors and retries

**Key Methods:**
- `executeExtractionStrategy()`: Orchestrates all extraction steps
- `executeDataSource()`: Executes a single API call
- `resolvePlaceholders()`: Replaces ${...} with actual values
- `extractDataFromResponse()`: Applies JSONPath expressions
- `triggerNextCalls()`: Evaluates conditions and triggers next steps

#### `RuleEvaluationService`
- Evaluates access control rules
- Determines document eligibility
- Supports AND/OR logic
- Handles rule groups

**Key Methods:**
- `evaluateEligibility()`: Main evaluation entry point
- `evaluateRule()`: Evaluates a single rule
- `evaluateCompositeRules()`: Handles AND/OR logic

#### `DocumentMatchingService`
- Matches documents from storage_index
- Supports multiple matching strategies
- Applies selection strategies (latest, all, etc.)

**Key Methods:**
- `findMatchingDocuments()`: Main matching entry point
- `matchByReferenceKey()`: Match by reference_key
- `matchByMetadataFields()`: JSONB metadata matching
- `matchByComposite()`: Combine multiple conditions

#### `DocumentEnquiryService`
- Main orchestration service
- Processes all templates
- Merges results
- Handles pagination and sorting

**Key Methods:**
- `getDocuments()`: Main entry point
- `processTemplate()`: Process a single template
- `mergeAndSortResults()`: Combine all results

### 4. Repository Layer

#### `MasterTemplateDefinitionRepository`
```java
public interface MasterTemplateDefinitionRepository
    extends ReactiveCrudRepository<MasterTemplateDefinitionEntity, UUID> {

    Flux<MasterTemplateDefinitionEntity> findByIsActiveTrue();

    Flux<MasterTemplateDefinitionEntity> findByTemplateType(String templateType);

    @Query("SELECT * FROM master_template_definition " +
           "WHERE is_active = true " +
           "AND (start_date IS NULL OR start_date <= :currentDate) " +
           "AND (end_date IS NULL OR end_date >= :currentDate)")
    Flux<MasterTemplateDefinitionEntity> findActiveTemplates(Long currentDate);
}
```

#### `StorageIndexRepository`
```java
public interface StorageIndexRepository
    extends ReactiveCrudRepository<StorageIndexEntity, UUID> {

    Flux<StorageIndexEntity> findByAccountKey(UUID accountKey);

    Flux<StorageIndexEntity> findByReferenceKeyAndReferenceKeyType(
        String referenceKey,
        String referenceKeyType
    );

    @Query("SELECT * FROM storage_index " +
           "WHERE template_type = :templateType " +
           "AND template_version = :templateVersion " +
           "AND doc_metadata @> :metadataConditions::jsonb")
    Flux<StorageIndexEntity> findByMetadataFields(
        String templateType,
        Integer templateVersion,
        String metadataConditions
    );
}
```

### 5. Supporting Components

#### `JsonPathExtractor`
- Extracts data from JSON using JSONPath expressions
- Handles arrays, filters, and complex paths

#### `PlaceholderResolver`
- Resolves ${...} placeholders in URLs, cache keys
- Supports nested paths (e.g., ${customer.address.state})

#### `CacheManager`
- Redis-based caching
- Configurable TTL per data source
- Cache key pattern support

#### `ExternalServiceClient`
- WebClient wrapper
- Handles timeouts, retries, circuit breakers
- Request/response logging

---

## Execution Flow

### High-Level Flow

```
1. Request: POST /documents-enquiry
   Body: {
     "customerId": "uuid",
     "accountId": ["uuid"],
     "referenceKey": "D164",
     "pageNumber": 1,
     "pageSize": 20
   }

2. DocumentEnquiryService.getDocuments()

3. Fetch Active Templates
   └─ TemplateRepository.findActiveTemplates()

4. For Each Template:

   A. Parse data_extraction_config
      └─ Convert JSON to DataExtractionConfig POJO

   B. Execute Extraction Strategy
      └─ DataExtractionEngine.executeExtractionStrategy()
         ├─ Step 1: Execute first data source
         │  ├─ Check cache
         │  ├─ Make HTTP call
         │  ├─ Extract data with JSONPath
         │  ├─ Validate extracted data
         │  ├─ Store in ExtractionContext
         │  └─ Check nextCalls conditions
         ├─ Step 2: Execute dependent data source
         │  └─ Use variables from Step 1
         └─ Continue for all steps...

   C. Evaluate Access Control
      └─ RuleEvaluationService.evaluateEligibility()
         ├─ Load eligibilityCriteria
         ├─ Evaluate each rule against ExtractionContext
         ├─ Apply logic (AND/OR)
         └─ Return true/false

   D. If Eligible: Match Documents
      └─ DocumentMatchingService.findMatchingDocuments()
         ├─ Parse documentMatchingStrategy
         ├─ Resolve valueSource from ExtractionContext
         ├─ Query storage_index with conditions
         └─ Apply selectionStrategy

   E. Collect Documents

5. Merge All Results
   └─ Combine documents from all templates
   └─ Remove duplicates

6. Sort Documents
   └─ Apply sortOrder from request

7. Apply Pagination
   └─ pageNumber, pageSize

8. Build Response
   └─ DocumentRetrievalResponse with HATEOAS links

9. Return Response
```

### Detailed Extraction Flow Example

**Template**: CardholderAgreement (2-step chain)

```
Input Request:
{
  "accountId": ["123e4567-e89b-12d3-a456-426614174000"],
  "customerId": "223e4567-e89b-12d3-a456-426614174000"
}

Step-by-Step Execution:

1. Initialize ExtractionContext
   context.variables = {
     "$input.accountId": "123e4567-e89b-12d3-a456-426614174000",
     "$input.customerId": "223e4567-e89b-12d3-a456-426614174000"
   }

2. Execute Step 1: getAccountArrangements

   A. Resolve URL
      Template: "/creditcard/accounts/${$input.accountId}/arrangements"
      Resolved: "/creditcard/accounts/123e4567-e89b-12d3-a456-426614174000/arrangements"

   B. Check cache
      Key: "arrangements:123e4567-e89b-12d3-a456-426614174000"
      Result: MISS

   C. Make HTTP Call
      GET /creditcard/accounts/123e4567-e89b-12d3-a456-426614174000/arrangements

   D. Response:
      {
        "content": [
          {
            "domain": "PRICING",
            "domainId": "PRICING_12345",
            "status": "ACTIVE"
          },
          {
            "domain": "PRODUCT",
            "domainId": "CREDIT_CARD"
          }
        ]
      }

   E. Extract with JSONPath
      pricingId: "$.content[?(@.domain == 'PRICING' && @.status == 'ACTIVE')].domainId | [0]"
      Result: "PRICING_12345"

      accountType: "$.content[?(@.domain == 'PRODUCT')].domainId | [0]"
      Result: "CREDIT_CARD"

   F. Validate
      pricingId: required=true, type=string → PASS

   G. Store in context
      context.variables["pricingId"] = "PRICING_12345"
      context.variables["accountType"] = "CREDIT_CARD"

   H. Cache response
      Key: "arrangements:123e4567-e89b-12d3-a456-426614174000"
      TTL: 1800 seconds

   I. Evaluate nextCalls
      Condition: pricingId is notNull → TRUE
      Trigger: getPricingData

3. Execute Step 2: getPricingData

   A. Check dependencies
      Required: ["pricingId"]
      Available: context.variables["pricingId"] = "PRICING_12345"
      Result: PASS

   B. Resolve URL
      Template: "/enterprise/.../prices/${pricingId}"
      Resolved: "/enterprise/.../prices/PRICING_12345"

   C. Check cache
      Key: "pricing:PRICING_12345"
      Result: MISS

   D. Make HTTP Call
      GET /enterprise/.../prices/PRICING_12345

   E. Response:
      {
        "cardholderAgreementsTncCode": "DISC_CC_2024_V1",
        "version": "2024.1"
      }

   F. Extract with JSONPath
      disclosureCode: "$.cardholderAgreementsTncCode"
      Result: "DISC_CC_2024_V1"

      pricingVersion: "$.version"
      Result: "2024.1"

   G. Validate
      disclosureCode: pattern=^DISC_[A-Z0-9_]+$ → PASS

   H. Store in context
      context.variables["disclosureCode"] = "DISC_CC_2024_V1"
      context.variables["pricingVersion"] = "2024.1"

   I. Cache response
      Key: "pricing:PRICING_12345"
      TTL: 3600 seconds

4. Evaluate Access Control

   Rule: accountType IN ["credit_card", "secured_card"]
   Value: context.variables["accountType"] = "CREDIT_CARD"
   Result: PASS

5. Match Documents

   Condition 1: reference_key = ${disclosureCode}
   Resolved: reference_key = "DISC_CC_2024_V1"

   Condition 2: doc_metadata->>'pricing_version' = ${pricingVersion}
   Resolved: doc_metadata->>'pricing_version' = "2024.1"

   Query:
   SELECT * FROM storage_index
   WHERE reference_key = 'DISC_CC_2024_V1'
     AND reference_key_type = 'DISCLOSURE_CODE'
     AND template_type = 'CardholderAgreement'
     AND doc_metadata @> '{"pricing_version": "2024.1"}'::jsonb

   Result: 1 document found

6. Return Document
```

---

## Performance Optimizations

### 1. Caching Strategy

**Cache Levels:**
- **API Response Cache**: Redis cache for external API responses
- **Template Cache**: In-memory cache for active templates
- **Rule Evaluation Cache**: Cache evaluation results per user session

**Cache Keys:**
- Use configurable patterns: `arrangements:${accountId}`
- Support composite keys: `customer:${customerId}:segment`
- TTL per data source (configurable)

### 2. Parallel Execution

**Template Processing:**
- Process multiple templates in parallel (reactive Flux)
- Each template independently extracts data

**Data Source Execution:**
- Mark independent data sources with `executionMode: parallel`
- Execute non-dependent API calls concurrently

### 3. Database Optimization

**JSONB Indexes:**
```sql
CREATE INDEX idx_doc_metadata_gin ON storage_index USING GIN (doc_metadata);
CREATE INDEX idx_template_extraction_config ON master_template_definition USING GIN (data_extraction_config);
```

**Composite Indexes:**
```sql
CREATE INDEX idx_storage_template_type_version
ON storage_index (template_type, template_version, is_shared);

CREATE INDEX idx_storage_reference_key
ON storage_index (reference_key, reference_key_type);
```

### 4. WebClient Connection Pooling

```yaml
spring:
  webflux:
    client:
      max-connections: 500
      max-pending-acquires: 1000
```

---

## Error Handling Strategy

### 1. API Call Failures

**Strategies:**
- `fail-fast`: Stop execution immediately
- `skip`: Skip this data source, continue with others
- `use-default`: Use default value from config
- `retry`: Retry with exponential backoff

### 2. Validation Failures

**Actions:**
- Log validation errors
- Mark template as ineligible
- Continue with other templates

### 3. Circuit Breaker

**Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      accountService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
```

### 4. Timeout Handling

**Timeouts:**
- Per data source timeout (default: 5000ms)
- Global request timeout (default: 30000ms)

---

## Testing Strategy

### 1. Unit Tests

**Test Components:**
- DataExtractionEngine: Mock WebClient responses
- RuleEvaluationService: Test all rule types
- DocumentMatchingService: Test all matching strategies
- PlaceholderResolver: Test variable resolution

### 2. Integration Tests

**Test Scenarios:**
- End-to-end document retrieval
- Multi-step extraction chains
- Cache hit/miss scenarios
- Error handling and fallbacks

### 3. Test Data

**Sample Templates:**
```sql
-- Simple statement
INSERT INTO master_template_definition ...

-- Location-based privacy policy
INSERT INTO master_template_definition ...

-- Multi-step cardholder agreement
INSERT INTO master_template_definition ...
```

**Sample Documents:**
```sql
-- Account-specific statement
INSERT INTO storage_index ...

-- Shared privacy policy for CA
INSERT INTO storage_index ...

-- Disclosure-based agreement
INSERT INTO storage_index ...
```

### 4. Mock Services

**MockWebServer:**
- Mock external API responses
- Test chaining scenarios
- Test error conditions

---

## Deployment Considerations

### 1. Configuration Management

**Externalize Configuration:**
- Integration service URLs: environment variables
- Database connection: Vault or Secret Manager
- Cache settings: ConfigMap

### 2. Observability

**Logging:**
- Structured logging (JSON format)
- Log extraction execution flow
- Log API call latencies
- Log cache hit rates

**Metrics:**
- API call success/failure rates
- Extraction execution time
- Cache hit/miss ratios
- Document retrieval latency

**Tracing:**
- Distributed tracing with correlation IDs
- Trace API call chains
- Trace rule evaluation

### 3. Security

**API Security:**
- OAuth 2.0 for external service calls
- API keys in headers
- mTLS for service-to-service

**Data Security:**
- Encrypt sensitive data in doc_metadata
- Mask PII in logs
- Access control on document retrieval

---

## Future Enhancements

### 1. Admin UI for Configuration

**Features:**
- Visual rule builder
- Extraction strategy designer
- Template configuration editor
- Test/preview functionality

### 2. Advanced Rule Engine

**Features:**
- Scripting support (Groovy/JavaScript)
- Custom rule evaluators
- Machine learning-based eligibility

### 3. Document Versioning

**Features:**
- Track document version history
- Rollback capability
- Version comparison

### 4. Analytics

**Features:**
- Document access patterns
- Popular templates
- Performance bottlenecks
- User behavior analysis

---

## Glossary

| Term | Definition |
|------|------------|
| **Template** | A document type definition (e.g., Statement, Privacy Policy) |
| **Extraction Strategy** | Multi-step configuration for fetching data from external services |
| **Access Control** | Rules that determine document eligibility |
| **Document Matching** | Logic to find specific documents in storage_index |
| **Chaining** | Sequential execution where output of one step becomes input to next |
| **Placeholder** | Variable in configuration (e.g., ${pricingId}) |
| **JSONPath** | Query language for extracting data from JSON |
| **Eligibility** | Whether a user can see a particular document type |
| **Shared Document** | Document visible to multiple users based on rules |
| **Selection Strategy** | How to pick documents when multiple matches exist |

---

## References

- **Existing Implementation**: `./document-hub-service` - Reference for multi-step extraction
- **OpenAPI Spec**: `./poc/doc-hub.yaml` - API contract
- **Database Schema**: `./poc/database.sql` - Database structure
- **Requirements**: `./poc/requirement.md` - Original requirements

---

**Document Version**: 1.0
**Last Updated**: 2025-11-24
**Author**: Architecture Team
