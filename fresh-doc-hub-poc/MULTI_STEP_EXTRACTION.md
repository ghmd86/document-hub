# Multi-Step Data Extraction - Chained API Calls

## Overview

Support **sequential multi-step data extraction** where:
1. Call API 1 to get Field A
2. Use Field A to call API 2 to get Field B
3. Use Field B to call API 3 to get Field C
4. And so on...

## Use Cases

### Use Case 1: Account → Product → Regulatory Document
```
Step 1: Call Account API with accountId → get productCode
Step 2: Call Product API with productCode → get productCategory
Step 3: Call Regulatory API with productCategory + customerLocation → get disclosureRequirements
```

### Use Case 2: Customer → Segment → Entitlements
```
Step 1: Call Customer API with customerId → get customerSegment
Step 2: Call Segment API with customerSegment → get benefitTier
Step 3: Call Benefits API with benefitTier → get availableDocuments
```

### Use Case 3: Account → Branch → Regional Rules
```
Step 1: Call Account API with accountId → get branchCode
Step 2: Call Branch API with branchCode → get region
Step 3: Call Regional Rules API with region → get applicableRules
```

## Enhanced JSON Schema

### Key Changes

1. **`requiredInputs` supports extracted fields**: Fields from previous API calls can be used as inputs
2. **Automatic dependency resolution**: System determines execution order based on field dependencies
3. **Sequential execution**: APIs execute in order when dependencies exist
4. **Parallel execution within levels**: Independent APIs at the same dependency level run in parallel

### Example: Multi-Step Configuration

```json
{
  "requiredFields": [
    "productCode",
    "productCategory",
    "regulatoryRegion",
    "disclosureRequirements"
  ],
  "fieldSources": {
    "productCode": {
      "description": "Product code from account",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.productCode",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    },
    "productCategory": {
      "description": "Product category derived from product code",
      "sourceApi": "productCatalogApi",
      "extractionPath": "$.product.category",
      "requiredInputs": ["productCode"],
      "fieldType": "string"
    },
    "regulatoryRegion": {
      "description": "Regulatory region based on customer location",
      "sourceApi": "regulatoryMappingApi",
      "extractionPath": "$.mapping.region",
      "requiredInputs": ["customerLocation"],
      "fieldType": "string"
    },
    "disclosureRequirements": {
      "description": "Required disclosures for this product in this region",
      "sourceApi": "disclosureRulesApi",
      "extractionPath": "$.rules.disclosures",
      "requiredInputs": ["productCategory", "regulatoryRegion"],
      "fieldType": "array"
    },
    "customerLocation": {
      "description": "Customer's state",
      "sourceApi": "customerProfileApi",
      "extractionPath": "$.customer.address.state",
      "requiredInputs": ["customerId"],
      "fieldType": "string"
    }
  },
  "dataSources": {
    "accountDetailsApi": {
      "description": "Get account details including product code",
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
      "providesFields": ["productCode"]
    },
    "customerProfileApi": {
      "description": "Get customer profile with location",
      "endpoint": {
        "url": "https://api.example.com/customers/${customerId}/profile",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${auth.token}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 7200
      },
      "providesFields": ["customerLocation"]
    },
    "productCatalogApi": {
      "description": "Get product details from product code",
      "endpoint": {
        "url": "https://api.example.com/products/${productCode}/details",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${auth.token}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 86400
      },
      "providesFields": ["productCategory"]
    },
    "regulatoryMappingApi": {
      "description": "Map customer location to regulatory region",
      "endpoint": {
        "url": "https://api.example.com/regulatory/region-mapping",
        "method": "POST",
        "timeout": 3000,
        "headers": {
          "Authorization": "Bearer ${auth.token}",
          "Content-Type": "application/json"
        },
        "body": "{\"state\": \"${customerLocation}\"}"
      },
      "providesFields": ["regulatoryRegion"]
    },
    "disclosureRulesApi": {
      "description": "Get disclosure rules for product category in region",
      "endpoint": {
        "url": "https://api.example.com/regulatory/disclosures?category=${productCategory}&region=${regulatoryRegion}",
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
      "providesFields": ["disclosureRequirements"]
    }
  },
  "executionStrategy": {
    "mode": "auto",
    "continueOnError": false,
    "timeout": 30000
  }
}
```

## Execution Flow

### Dependency Graph
```
Input: accountId, customerId

Level 0 (Initial inputs):
  - accountId
  - customerId

Level 1 (Parallel - both can run immediately):
  ├─ accountDetailsApi(accountId) → productCode
  └─ customerProfileApi(customerId) → customerLocation

Level 2 (Parallel - both can run after Level 1):
  ├─ productCatalogApi(productCode) → productCategory
  └─ regulatoryMappingApi(customerLocation) → regulatoryRegion

Level 3 (Sequential - needs both Level 2 results):
  └─ disclosureRulesApi(productCategory, regulatoryRegion) → disclosureRequirements
```

### Step-by-Step Execution

**Step 1: Level 0 → Level 1** (Parallel)
```
Call 1: GET /accounts/{accountId}/details
  Response: { "account": { "productCode": "CC-PREMIUM-001" } }
  Extract: productCode = "CC-PREMIUM-001"

Call 2: GET /customers/{customerId}/profile
  Response: { "customer": { "address": { "state": "CA" } } }
  Extract: customerLocation = "CA"
```

**Step 2: Level 1 → Level 2** (Parallel)
```
Call 3: GET /products/CC-PREMIUM-001/details
  Response: { "product": { "category": "CREDIT_CARD" } }
  Extract: productCategory = "CREDIT_CARD"

Call 4: POST /regulatory/region-mapping
  Body: {"state": "CA"}
  Response: { "mapping": { "region": "US_WEST" } }
  Extract: regulatoryRegion = "US_WEST"
```

**Step 3: Level 2 → Level 3** (Sequential)
```
Call 5: GET /regulatory/disclosures?category=CREDIT_CARD&region=US_WEST
  Response: { "rules": { "disclosures": ["TILA", "CARD_ACT", "CA_DISCLOSURE"] } }
  Extract: disclosureRequirements = ["TILA", "CARD_ACT", "CA_DISCLOSURE"]
```

**Final Result:**
```json
{
  "productCode": "CC-PREMIUM-001",
  "productCategory": "CREDIT_CARD",
  "customerLocation": "CA",
  "regulatoryRegion": "US_WEST",
  "disclosureRequirements": ["TILA", "CARD_ACT", "CA_DISCLOSURE"]
}
```

## POST Requests with Dynamic Body

The `endpoint.body` field supports placeholders for POST/PUT requests:

```json
{
  "endpoint": {
    "url": "https://api.example.com/validate",
    "method": "POST",
    "headers": {
      "Content-Type": "application/json"
    },
    "body": "{\"accountId\": \"${accountId}\", \"productCode\": \"${productCode}\", \"region\": \"${regulatoryRegion}\"}"
  }
}
```

Body placeholders are resolved from:
- Initial request inputs (`accountId`, `customerId`)
- Previously extracted fields (`productCode`, `regulatoryRegion`)
- System variables (`correlationId`, `auth.token`)

## Execution Modes

### Mode: "auto" (Recommended)
System automatically determines execution strategy:
- Analyzes field dependencies
- Builds dependency graph
- Executes levels sequentially
- Within each level, executes APIs in parallel

### Mode: "sequential"
All APIs execute one by one in the order they appear in config

### Mode: "parallel"
All APIs execute simultaneously (only works if no dependencies)

## Error Handling in Multi-Step

### Strategy 1: Fail Fast
```json
{
  "executionStrategy": {
    "mode": "auto",
    "continueOnError": false
  }
}
```
- If any API fails, entire extraction stops
- Use when all fields are critical

### Strategy 2: Best Effort
```json
{
  "executionStrategy": {
    "mode": "auto",
    "continueOnError": true
  }
}
```
- Failed APIs use default values
- Dependent APIs are skipped if inputs missing
- Use when some fields are optional

## Advanced Example: 5-Step Chain

```json
{
  "requiredFields": [
    "accountDetails",
    "branchCode",
    "regionCode",
    "regionalCompliance",
    "applicableDocuments"
  ],
  "fieldSources": {
    "accountDetails": {
      "sourceApi": "accountApi",
      "extractionPath": "$",
      "requiredInputs": ["accountId"]
    },
    "branchCode": {
      "sourceApi": "accountApi",
      "extractionPath": "$.account.branchCode",
      "requiredInputs": ["accountId"]
    },
    "regionCode": {
      "sourceApi": "branchApi",
      "extractionPath": "$.branch.regionCode",
      "requiredInputs": ["branchCode"]
    },
    "regionalCompliance": {
      "sourceApi": "complianceApi",
      "extractionPath": "$.compliance.rules",
      "requiredInputs": ["regionCode"]
    },
    "applicableDocuments": {
      "sourceApi": "documentRulesApi",
      "extractionPath": "$.documents",
      "requiredInputs": ["regionalCompliance", "accountType"]
    },
    "accountType": {
      "sourceApi": "accountApi",
      "extractionPath": "$.account.type",
      "requiredInputs": ["accountId"]
    }
  },
  "dataSources": {
    "accountApi": {
      "endpoint": {
        "url": "https://api.example.com/accounts/${accountId}",
        "method": "GET"
      },
      "providesFields": ["accountDetails", "branchCode", "accountType"]
    },
    "branchApi": {
      "endpoint": {
        "url": "https://api.example.com/branches/${branchCode}",
        "method": "GET"
      },
      "providesFields": ["regionCode"]
    },
    "complianceApi": {
      "endpoint": {
        "url": "https://api.example.com/compliance/region/${regionCode}",
        "method": "GET"
      },
      "providesFields": ["regionalCompliance"]
    },
    "documentRulesApi": {
      "endpoint": {
        "url": "https://api.example.com/document-rules",
        "method": "POST",
        "headers": {
          "Content-Type": "application/json"
        },
        "body": "{\"compliance\": ${regionalCompliance}, \"accountType\": \"${accountType}\"}"
      },
      "providesFields": ["applicableDocuments"]
    }
  }
}
```

**Execution Levels:**
```
Level 1: accountApi → accountDetails, branchCode, accountType
Level 2: branchApi(branchCode) → regionCode
Level 3: complianceApi(regionCode) → regionalCompliance
Level 4: documentRulesApi(regionalCompliance, accountType) → applicableDocuments
```

## Benefits

1. **Complex Workflows**: Support any multi-step data gathering workflow
2. **Optimized Execution**: Parallel execution where possible, sequential only when needed
3. **Flexible Dependencies**: Fields can depend on other fields at any level
4. **Smart Orchestration**: System figures out the optimal execution plan
5. **No Code Changes**: Configure complex workflows entirely through JSON

## Implementation Notes

The `ConfigurableDataExtractionService` already supports this through:
- **Iterative dependency resolution**: Loops through fields multiple times, adding APIs when inputs become available
- **Context building**: Each API call adds its results to the context for subsequent calls
- **Placeholder resolution**: URLs and bodies can reference any field in the context
