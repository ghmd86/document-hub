-- ========================================
-- Sample data_extraction_config Examples
-- ========================================
-- These are example JSON configurations for the data_extraction_config column
-- They demonstrate how to configure field-to-source mappings for different template types

-- Example 1: Account Statement Template
-- Requires: accountType, accountStatus, accountBalance
-- Sources: Single API (Account API)
-- Execution: Parallel
/*
{
  "fieldsToExtract": ["accountType", "accountStatus", "accountBalance"],
  "fieldSources": {
    "accountType": {
      "description": "Type of account (CHECKING, SAVINGS, CREDIT)",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.type",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    },
    "accountStatus": {
      "description": "Account status (ACTIVE, CLOSED, SUSPENDED)",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.status",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    },
    "accountBalance": {
      "description": "Current account balance",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.balance.current",
      "requiredInputs": ["accountId"],
      "fieldType": "decimal",
      "defaultValue": 0.0
    }
  },
  "dataSources": {
    "accountDetailsApi": {
      "description": "Account details API",
      "endpoint": {
        "url": "https://api.example.com/accounts/${accountId}",
        "method": "GET",
        "timeout": 5000,
        "headers": {
          "Authorization": "Bearer ${auth.token}",
          "X-Request-ID": "${correlationId}"
        }
      },
      "cache": {
        "enabled": true,
        "ttlSeconds": 1800,
        "keyPattern": "account:${accountId}:details"
      },
      "retry": {
        "maxAttempts": 2,
        "delayMs": 1000
      },
      "providesFields": ["accountType", "accountStatus", "accountBalance"]
    }
  },
  "executionStrategy": {
    "mode": "parallel",
    "continueOnError": true,
    "timeout": 10000
  }
}
*/

-- Example 2: Regulatory Disclosure Document
-- Requires: disclosureCode, customerLocation, accountType
-- Sources: Two APIs (Account API + Customer API)
-- Execution: Parallel (independent calls)
/*
{
  "fieldsToExtract": ["disclosureCode", "customerLocation", "accountType"],
  "fieldSources": {
    "disclosureCode": {
      "description": "Regulatory disclosure code",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.disclosureCode",
      "requiredInputs": ["accountId"],
      "fieldType": "string",
      "defaultValue": "STANDARD",
      "validationPattern": "^[A-Z0-9]{3,10}$"
    },
    "customerLocation": {
      "description": "Customer's state/region",
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
    }
  },
  "dataSources": {
    "accountDetailsApi": {
      "description": "Account details and disclosure information",
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
      "providesFields": ["disclosureCode", "accountType"]
    },
    "customerProfileApi": {
      "description": "Customer profile and location",
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
      "providesFields": ["customerLocation"]
    }
  },
  "executionStrategy": {
    "mode": "parallel",
    "continueOnError": false,
    "timeout": 10000
  }
}
*/

-- Example 3: VIP Customer Welcome Letter
-- Requires: customerTier, customerLocation, accountType, productCode
-- Sources: Two APIs (Account API + Customer API)
-- Execution: Parallel
/*
{
  "fieldsToExtract": ["customerTier", "customerLocation", "accountType", "productCode"],
  "fieldSources": {
    "customerTier": {
      "description": "Customer segment (VIP, PREMIUM, STANDARD)",
      "sourceApi": "customerProfileApi",
      "extractionPath": "$.customer.tier",
      "requiredInputs": ["customerId"],
      "fieldType": "string",
      "defaultValue": "STANDARD"
    },
    "customerLocation": {
      "description": "Customer's state",
      "sourceApi": "customerProfileApi",
      "extractionPath": "$.customer.address.state",
      "requiredInputs": ["customerId"],
      "fieldType": "string"
    },
    "accountType": {
      "description": "Account type",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.type",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    },
    "productCode": {
      "description": "Product code",
      "sourceApi": "accountDetailsApi",
      "extractionPath": "$.account.productCode",
      "requiredInputs": ["accountId"],
      "fieldType": "string"
    }
  },
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
      "providesFields": ["accountType", "productCode"]
    },
    "customerProfileApi": {
      "description": "Customer profile API",
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
      "providesFields": ["customerTier", "customerLocation"]
    }
  },
  "executionStrategy": {
    "mode": "parallel",
    "continueOnError": true,
    "timeout": 10000
  }
}
*/

-- Example 4: Complex Document with Chained API Calls
-- Requires: customerLocation, regulatoryRegion (depends on customerLocation)
-- Sources: Two APIs with dependencies
-- Execution: Sequential (customerLocation must be fetched first)
/*
{
  "fieldsToExtract": ["customerLocation", "regulatoryRegion"],
  "fieldSources": {
    "customerLocation": {
      "description": "Customer's state",
      "sourceApi": "customerProfileApi",
      "extractionPath": "$.customer.address.state",
      "requiredInputs": ["customerId"],
      "fieldType": "string"
    },
    "regulatoryRegion": {
      "description": "Regulatory region based on state",
      "sourceApi": "regulatoryApi",
      "extractionPath": "$.region",
      "requiredInputs": ["customerLocation"],
      "fieldType": "string"
    }
  },
  "dataSources": {
    "customerProfileApi": {
      "description": "Customer profile API",
      "endpoint": {
        "url": "https://api.example.com/customers/${customerId}/profile",
        "method": "GET",
        "timeout": 5000
      },
      "providesFields": ["customerLocation"]
    },
    "regulatoryApi": {
      "description": "Regulatory region lookup",
      "endpoint": {
        "url": "https://api.example.com/regulatory/region?state=${customerLocation}",
        "method": "GET",
        "timeout": 3000
      },
      "providesFields": ["regulatoryRegion"]
    }
  },
  "executionStrategy": {
    "mode": "sequential",
    "continueOnError": false,
    "timeout": 10000
  }
}
*/
