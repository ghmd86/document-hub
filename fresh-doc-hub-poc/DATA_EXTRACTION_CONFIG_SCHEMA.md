# Data Extraction Config JSON Schema

## Overview

The `data_extraction_config` column in `master_template_definition` table stores a JSON object that defines:

1. **Which fields are required** for this document template type
2. **Where to fetch each field** (API endpoint configuration)
3. **How to extract data** from API responses (JSONPath)
4. **Dependencies between fields** (execution order)

This allows complete configurability without code changes when new template types are added.

---

## JSON Structure

```json
{
  "fieldsToExtract": [
    "disclosureCode",
    "customerLocation",
    "accountType",
    "customerTier"
  ],
  "fieldSources": {
    "disclosureCode": {
      "description": "Regulatory disclosure code for the account",
      "sourceApi": "accountDetailsApi",
      "fallbackApi": "accountSummaryApi",
      "extractionPath": "$.account.disclosureCode",
      "requiredInputs": ["accountId"],
      "fieldType": "string",
      "defaultValue": "STANDARD",
      "validationPattern": "^[A-Z0-9]{3,10}$"
    },
    "customerLocation": {
      "description": "Customer's primary location/state",
      "sourceApi": "customerProfileApi",
      "extractionPath": "$.customer.address.state",
      "requiredInputs": ["customerId"],
      "fieldType": "string",
      "defaultValue": "UNKNOWN"
    },
    "accountType": {
      "description": "Type of account",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.type",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    },
    "customerTier": {
      "description": "Customer segment",
      "sourceApi": "customerProfileApi",
      "extractionPath": "$.customer.tier",
      "requiredInputs": ["customerId"],
      "fieldType": "string",
      "defaultValue": "STANDARD"
    }
  },
  "dataSources": {
    "accountDetailsApi": {
      "description": "Retrieves account details including type, disclosure code",
      "endpoint": {
        "url": "https://api.example.com/accounts/${accountId}/details",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${auth.token}",
          "X-Request-ID": "${correlationId}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 3600,
        "keyPattern": "account:${accountId}:details"
      },
      "retry": {
        "maxAttempts": 2,
        "delayMs": 1000
      },
      "providesFields": [
        "disclosureCode",
        "accountType",
        "accountStatus",
        "productCode"
      ]
    },
    "customerProfileApi": {
      "description": "Retrieves customer profile and location info",
      "endpoint": {
        "url": "https://api.example.com/customers/${customerId}/profile",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${auth.token}",
          "X-Request-ID": "${correlationId}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 7200,
        "keyPattern": "customer:${customerId}:profile"
      },
      "retry": {
        "maxAttempts": 2,
        "delayMs": 1000
      },
      "providesFields": [
        "customerLocation",
        "customerTier",
        "customerFullName"
      ]
    }
  },
  "executionStrategy": {
    "mode": "parallel",
    "continueOnError": true,
    "timeout": 10000
  }
}
```

---

## Field Definitions

### Top Level

| Field | Type | Description |
|-------|------|-------------|
| `fieldsToExtract` | Array[String] | List of field names needed for this template type |
| `fieldSources` | Object | Maps each field name to its source configuration |
| `dataSources` | Object | Maps API ID to endpoint configuration |
| `executionStrategy` | Object | How to execute API calls (parallel/sequential) |

### fieldSources.{fieldName}

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | String | No | What this field represents |
| `sourceApi` | String | Yes | Primary API ID to fetch from (references dataSources key) |
| `fallbackApi` | String | No | Fallback API if primary fails |
| `extractionPath` | String | Yes | JSONPath to extract value from API response |
| `requiredInputs` | Array[String] | Yes | Input fields needed (e.g., ["accountId"]) |
| `fieldType` | String | No | Data type: string, integer, decimal, date, boolean |
| `defaultValue` | Any | No | Value to use if extraction fails |
| `validationPattern` | String | No | Regex pattern for validation |

### dataSources.{apiId}

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | String | No | What this API provides |
| `endpoint` | Object | Yes | API endpoint configuration |
| `endpoint.url` | String | Yes | URL with placeholders ${fieldName} |
| `endpoint.method` | String | Yes | HTTP method (GET, POST, etc.) |
| `endpoint.timeout` | Integer | No | Timeout in milliseconds (default: 5000) |
| `endpoint.headers` | Object | No | Request headers with placeholder support |
| `cache` | Object | No | Caching configuration |
| `cache.enabled` | Boolean | No | Enable caching (default: false) |
| `cache.ttlSeconds` | Integer | No | Cache TTL in seconds |
| `cache.keyPattern` | String | No | Cache key with placeholders |
| `retry` | Object | No | Retry configuration |
| `retry.maxAttempts` | Integer | No | Max retry attempts (default: 0) |
| `retry.delayMs` | Integer | No | Delay between retries in ms |
| `providesFields` | Array[String] | Yes | List of fields this API provides |

### executionStrategy

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `mode` | String | "sequential" | "parallel" or "sequential" |
| `continueOnError` | Boolean | true | Continue if one API fails |
| `timeout` | Integer | 10000 | Overall execution timeout in ms |

---

## Placeholder Syntax

Placeholders use `${variableName}` syntax and can reference:

- **Input fields**: `${accountId}`, `${customerId}`
- **Extracted fields**: `${disclosureCode}` (from previous API calls)
- **System variables**: `${correlationId}`, `${auth.token}`

---

## Example Use Cases

### Example 1: Simple Account Statement Template

```json
{
  "fieldsToExtract": ["accountType", "accountStatus"],
  "fieldSources": {
    "accountType": {
      "sourceApi": "accountApi",
      "extractionPath": "$.type",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    },
    "accountStatus": {
      "sourceApi": "accountApi",
      "extractionPath": "$.status",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    }
  },
  "dataSources": {
    "accountApi": {
      "endpoint": {
        "url": "https://api.example.com/accounts/${accountId}",
        "method": "GET",
        "timeout": 3000
      },
      "providesFields": ["accountType", "accountStatus"]
    }
  },
  "executionStrategy": {
    "mode": "parallel"
  }
}
```

### Example 2: Complex Regulatory Document with Dependencies

```json
{
  "fieldsToExtract": ["disclosureCode", "customerLocation", "regulatoryRegion"],
  "fieldSources": {
    "disclosureCode": {
      "sourceApi": "accountApi",
      "extractionPath": "$.disclosureCode",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    },
    "customerLocation": {
      "sourceApi": "customerApi",
      "extractionPath": "$.address.state",
      "requiredInputs": ["customerId"],
      "fieldType": "string"
    },
    "regulatoryRegion": {
      "sourceApi": "regulatoryApi",
      "extractionPath": "$.region",
      "requiredInputs": ["customerLocation"],
      "fieldType": "string"
    }
  },
  "dataSources": {
    "accountApi": {
      "endpoint": {
        "url": "https://api.example.com/accounts/${accountId}",
        "method": "GET",
        "timeout": 5000
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 3600
      },
      "providesFields": ["disclosureCode"]
    },
    "customerApi": {
      "endpoint": {
        "url": "https://api.example.com/customers/${customerId}",
        "method": "GET",
        "timeout": 5000
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 7200
      },
      "providesFields": ["customerLocation"]
    },
    "regulatoryApi": {
      "endpoint": {
        "url": "https://api.example.com/regulatory/region?state=${customerLocation}",
        "method": "GET",
        "timeout": 5000
      },
      "providesFields": ["regulatoryRegion"]
    }
  },
  "executionStrategy": {
    "mode": "sequential",
    "continueOnError": false
  }
}
```

---

## How It Solves the Problem

### Before (Problem)
- Client sends: `accountId`, `customerId`
- System doesn't know where to get: `disclosureCode`, `customerLocation`, etc.

### After (Solution)
1. Template record loaded from DB contains `data_extraction_config` JSON
2. System reads `fieldsToExtract` → knows WHAT to fetch
3. System reads `fieldSources` → knows WHERE each field comes from (which API)
4. System reads `dataSources` → knows HOW to call each API
5. System automatically orchestrates API calls based on dependencies
6. All data extracted and ready for document generation

### Adding New Template Type
1. Admin inserts new row in `master_template_definition`
2. Sets `data_extraction_config` JSON with field mappings
3. **No code deployment needed** - system automatically handles the new type!
