# Field Source Mapping Solution

## Problem Statement

The Document Hub receives requests with only `accountId` and `customerId`, but different document templates require additional data fields like:
- `disclosureCode`
- `customerLocation`
- `accountType`
- `customerTier`
- etc.

**The system didn't know:**
1. WHICH fields are needed for each document template type
2. WHERE to fetch each field from (which API)
3. HOW to call those APIs
4. WHAT to do when new template types are added

## Solution Overview

Store all field-to-source mapping configuration in the **existing** `master_template_definition.data_extraction_config` JSON column. This makes the system **completely configurable** without code changes.

## Architecture

```
Client Request (accountId, customerId)
         ↓
DocumentEnquiryService
         ↓
Load Template from DB
         ↓
Read data_extraction_config JSON ← THIS IS THE KEY!
         ↓
ConfigurableDataExtractionService
         ├→ Parse JSON config
         ├→ Identify required fields
         ├→ Build execution plan (which APIs to call)
         ├→ Call APIs with placeholder resolution
         └→ Extract data using JSONPath
         ↓
Extracted Data (disclosureCode, customerLocation, etc.)
         ↓
Use for document filtering/generation
```

## What's in data_extraction_config JSON

The JSON contains 4 main sections:

### 1. fieldsToExtract
Lists which fields this template type needs:
```json
{
  "fieldsToExtract": ["disclosureCode", "customerLocation", "accountType"]
}
```

### 2. fieldSources
Maps each field to its source API and extraction details:
```json
{
  "fieldSources": {
    "disclosureCode": {
      "description": "Regulatory disclosure code",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.disclosureCode",
      "requiredInputs": ["accountId"],
      "defaultValue": "STANDARD"
    }
  }
}
```

### 3. dataSources
Defines API endpoints and how to call them:
```json
{
  "dataSources": {
    "accountDetailsApi": {
      "description": "Account details API",
      "endpoint": {
        "url": "https://api.example.com/accounts/${accountId}/details",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${auth.token}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 3600
      },
      "providesFields": ["disclosureCode", "accountType"]
    }
  }
}
```

### 4. executionStrategy
How to execute the API calls:
```json
{
  "executionStrategy": {
    "mode": "parallel",
    "continueOnError": true,
    "timeout": 10000
  }
}
```

## How It Works: Step by Step

### Step 1: Request Arrives
```
Client sends:
{
  "accountId": ["550e8400-e29b-41d4-a716-446655440000"],
  "customerId": "123e4567-e89b-12d3-a456-426614174000"
}
```

### Step 2: Template Loaded from Database
```sql
SELECT * FROM master_template_definition
WHERE template_type = 'REGULATORY_DISCLOSURE'
AND is_active = true;
```

Returns template with `data_extraction_config` JSON.

### Step 3: JSON Config Parsed
`ConfigurableDataExtractionService` parses the JSON into Java objects:
- `DataExtractionConfig`
- `FieldSourceConfig`
- `DataSourceConfig`
- `EndpointConfig`

### Step 4: Execution Plan Built
Service analyzes:
- What fields are required?
  → `disclosureCode`, `customerLocation`, `accountType`
- What inputs do we have?
  → `accountId`, `customerId`
- Which APIs provide these fields?
  → `accountDetailsApi` provides `disclosureCode`, `accountType`
  → `customerProfileApi` provides `customerLocation`
- What can we call now?
  → Both APIs can be called (we have required inputs)

**Execution Plan:**
1. Call `accountDetailsApi` with `accountId`
2. Call `customerProfileApi` with `customerId`
3. Execute in parallel (both independent)

### Step 5: APIs Called
**Call 1:**
```
GET https://api.example.com/accounts/550e8400-e29b-41d4-a716-446655440000/details
Authorization: Bearer mock-token-12345
X-Request-ID: 7f9e8d7c-6b5a-4321-9876-5432109876fe

Response:
{
  "account": {
    "type": "CHECKING",
    "disclosureCode": "D164",
    "status": "ACTIVE"
  }
}
```

**Call 2:**
```
GET https://api.example.com/customers/123e4567-e89b-12d3-a456-426614174000/profile
Authorization: Bearer mock-token-12345

Response:
{
  "customer": {
    "tier": "PREMIUM",
    "address": {
      "state": "CA"
    }
  }
}
```

### Step 6: Data Extracted Using JSONPath
- Extract `disclosureCode` from Call 1 using `$.account.disclosureCode` → `"D164"`
- Extract `accountType` from Call 1 using `$.account.type` → `"CHECKING"`
- Extract `customerLocation` from Call 2 using `$.customer.address.state` → `"CA"`

### Step 7: Extracted Data Returned
```json
{
  "disclosureCode": "D164",
  "accountType": "CHECKING",
  "customerLocation": "CA"
}
```

### Step 8: Data Used for Document Matching
Now the service has all the data needed to:
- Filter documents by `disclosureCode`
- Match regulatory rules based on `customerLocation`
- Apply account-type-specific logic

## Key Features

### 1. **Placeholder Resolution**
URLs and headers support placeholders:
- `${accountId}` - from request
- `${customerId}` - from request
- `${disclosureCode}` - from previously extracted data
- `${auth.token}` - from system context
- `${correlationId}` - auto-generated

### 2. **Dependency Handling**
System automatically detects dependencies:
- If Field B needs Field A as input
- System calls API for Field A first
- Then calls API for Field B using extracted value

### 3. **Parallel vs Sequential Execution**
- **Parallel**: Independent API calls execute simultaneously
- **Sequential**: Dependent calls execute in order

### 4. **Error Handling**
- Default values when API fails
- Fallback APIs for critical fields
- Continue or fail based on configuration

### 5. **Caching**
- Cache API responses with configurable TTL
- Cache key patterns support placeholders
- Reduces redundant API calls

## Adding New Template Type: Zero Code Changes!

### Scenario: Add "Credit Card Statement" Template

**Old Way (Before):**
1. Write Java code to fetch `accountType`, `creditLimit`, `paymentDueDate`
2. Add service methods
3. Deploy code
4. Restart application

**New Way (With This Solution):**
1. Insert row in `master_template_definition`
2. Set `data_extraction_config` JSON:

```sql
INSERT INTO master_template_definition (
  master_template_id,
  template_version,
  template_type,
  template_name,
  is_active,
  data_extraction_config
) VALUES (
  gen_random_uuid(),
  1,
  'CREDIT_CARD_STATEMENT',
  'Credit Card Statement',
  true,
  '{
    "fieldsToExtract": ["accountType", "creditLimit", "paymentDueDate"],
    "fieldSources": {
      "accountType": {
        "sourceApi": "accountApi",
        "extractionPath": "$.account.type",
        "requiredInputs": ["accountId"]
      },
      "creditLimit": {
        "sourceApi": "creditCardApi",
        "extractionPath": "$.creditCard.limit",
        "requiredInputs": ["accountId"]
      },
      "paymentDueDate": {
        "sourceApi": "creditCardApi",
        "extractionPath": "$.creditCard.paymentDueDate",
        "requiredInputs": ["accountId"]
      }
    },
    "dataSources": {
      "accountApi": {
        "endpoint": {
          "url": "https://api.example.com/accounts/${accountId}",
          "method": "GET",
          "timeout": 5000
        },
        "providesFields": ["accountType"]
      },
      "creditCardApi": {
        "endpoint": {
          "url": "https://api.example.com/creditcards/${accountId}",
          "method": "GET",
          "timeout": 5000
        },
        "providesFields": ["creditLimit", "paymentDueDate"]
      }
    },
    "executionStrategy": {
      "mode": "parallel"
    }
  }'::jsonb
);
```

**Done!** System automatically handles the new template type.

## Files Created

### Models (Java POJOs for JSON parsing)
- `DataExtractionConfig.java` - Root configuration
- `FieldSourceConfig.java` - Field-to-source mapping
- `DataSourceConfig.java` - API endpoint configuration
- `EndpointConfig.java` - HTTP endpoint details
- `CacheConfig.java` - Caching configuration
- `RetryConfig.java` - Retry logic
- `ExecutionStrategy.java` - Execution mode

### Services
- `ConfigurableDataExtractionService.java` - Main orchestration service
  - Parses JSON config
  - Builds execution plan
  - Calls APIs
  - Extracts data using JSONPath
  - Handles errors and defaults

### Documentation
- `DATA_EXTRACTION_CONFIG_SCHEMA.md` - Complete JSON schema documentation
- `sample-data-extraction-configs.sql` - 4 real-world examples
- `FIELD_SOURCE_MAPPING_SOLUTION.md` - This file

## Benefits

### 1. **Zero Code Deployment for New Templates**
Add new document types by inserting database records.

### 2. **Non-Technical Configuration**
Business analysts can configure field mappings using JSON examples.

### 3. **Centralized Configuration**
All extraction logic in one place (database), not scattered in code.

### 4. **Flexible & Extensible**
Support any API, any field, any extraction logic through JSON.

### 5. **Auditable**
All configuration changes tracked in database.

### 6. **Testable**
Easy to test different configurations without code changes.

## Next Steps

### 1. Integration with DocumentEnquiryService
Update `DocumentEnquiryService` to call `ConfigurableDataExtractionService`:

```java
// In processTemplate method
return Mono.zip(
    queryDocuments(template, accountId, accountMetadata),
    extractFieldData(template, request)
).map(tuple -> {
    List<StorageIndexEntity> docs = tuple.getT1();
    Map<String, Object> extractedData = tuple.getT2();
    // Use extractedData for filtering/matching
    return convertToDocumentDetailsNodes(docs, template);
});
```

### 2. Add Sample Data
Populate `data.sql` with real template examples containing `data_extraction_config`.

### 3. Add WebClient Configuration
Ensure `WebClient` bean is configured in application.

### 4. Add JSONPath Dependency
Add to `pom.xml`:
```xml
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.8.0</version>
</dependency>
```

### 5. Testing
Create integration tests with mock APIs to verify extraction logic.

## Summary

This solution completely solves the problem:

**Before:**
- ❌ Client sends accountId/customerId
- ❌ System doesn't know where to get other fields
- ❌ Hard to add new template types
- ❌ Requires code changes

**After:**
- ✅ Client sends accountId/customerId
- ✅ System reads data_extraction_config from database
- ✅ System knows exactly which APIs to call
- ✅ System extracts all required fields automatically
- ✅ Adding new templates = just insert database record
- ✅ Zero code changes needed
