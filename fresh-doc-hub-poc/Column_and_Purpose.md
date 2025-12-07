We have required field column named: `required_fields` which can be used to store the 
required fields that are needed
uploading and storing in `doc_meta` column in `storage_index` table. 
One such example how we can store these fields:

**Example 1:**
```json5
{
    "required_fields": [
        {"field": "issueDate", "type": "date", "required": true},
        {"field": "mimeType", "type": "string", "required": true},
        {"field": "account_Id", "type": "string", "required": true},
        {"field": "customer_id", "type": "string", "required": true},
        {"field": "statement_id", "type": "string", "required": true},
        {"field": "statement_month", "type": "string", "required": true},
        {
            "field": "statement_naming",
            "type": "string",
            "required": true,
            "format": "Satement {0-1} [4].pdf"
        }
    ]
}
```

**Example 2:**
```json5
{
    "required_fields": [
        {"field": "pricingCode",       "type": "string",  "required": true},
        {"field": "disclosureVersion", "type": "integer", "required": true},
        {"field": "effectiveDate",     "type": "Date",    "required": true},
        {"field": "disclosureCode",    "type": "string",  "required": true}
    ]
}

```

**Example 3:**

```json5
{
    "required_fields": [
        {"field": "customer_id",     "type": "string",  "required": true},
        {"field": "account_id",      "type": "integer", "required": true},
        {"field": "mimeType",        "type": "string",  "required": true},
        {"field": "template_engine", "type": "string",  "required": true}
    ]
}
```

We added this as part of `data_extraction_config` which we can separate.
When uploading a new document instance we need to make sure these things are stored in the storage index table.

**Access Control** (UPDATED - Role-Based Access)
- Purpose: Restrict access to documents based on user roles
- Empty column = anyone can access (no restrictions)
- JSON structure stored as array:
```json5
[
    { "role": "admin", "actions": ["View","Update","Delete","Download"] },
    { "role": "customer", "actions": ["View","Download"] },
    { "role": "agent", "actions": ["View"] },
    { "role": "backOffice", "actions": ["View","Update","Download"] },
    { "role": "system", "actions": ["View","Update","Delete","Download"] }
]
```

***Model Classes:***
- `AccessControl.java` - wrapper with helper methods (`hasPermission()`, `getActionsForRole()`)
- `RoleAccess.java` - individual role-action mapping

***Note:*** Eligibility criteria (who sees which documents based on account attributes) has been moved to `template_config.eligibility_criteria`

**Channels** 

- Different delivery channels: eg: Print, Email, Web, Mobile, SMS. 
```json5
[
    { "type": "EMAIL", "enabled": true, "config": {"priority": "high"} },
    { "type": "PRINT", "enabled": true, "config": {"duplex": true}     },
    { "type": "WEB",   "enabled": true                                 }
]

```
**template_config** (UPDATED)
- Configuration related to the template
- Now includes `eligibility_criteria` for determining who can see documents (moved from access_control)

***Example content***
```json5
{
  "reprint_policy": {"cooldown_period_days": 12},
  "print_policy"  : {"default": "{template_vendor_id}", "duplex": true},
  "eligibility_criteria": {
    "operator": "AND",
    "rules": [
      {"field": "accountType", "operator": "IN", "value": ["credit_card"]},
      {"field": "customerSegment", "operator": "EQUALS", "value": "VIP"},
      {"field": "region", "operator": "EQUALS", "value": "US_WEST"}
    ]
  }
}
```

***Supported Operators for eligibility_criteria rules:***
- `EQUALS`, `NOT_EQUALS` - exact match
- `IN`, `NOT_IN` - value in list
- `GREATER_THAN`, `LESS_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL` - numeric comparison
- `CONTAINS`, `STARTS_WITH`, `ENDS_WITH` - string operations

***Model Classes:***
- `TemplateConfig.java` - main config class
- `EligibilityCriteria.java` - eligibility rules container
- `Rule.java` - individual rule definition

---

## How data_extraction_config and eligibility_criteria Work Together

The `data_extraction_config` and `template_config.eligibility_criteria` work in tandem to enable dynamic, API-driven eligibility decisions.

### The Relationship

| Column | Purpose | When Used |
|--------|---------|-----------|
| `data_extraction_config` | **FETCH** data from external APIs | Before eligibility check |
| `template_config.eligibility_criteria` | **EVALUATE** rules using fetched data | After data extraction |

### Important: `fieldsToExtract` vs `required_fields`

| Property | Location | Purpose |
|----------|----------|---------|
| `fieldsToExtract` | `data_extraction_config` column | Fields to **extract from external APIs** for eligibility/document matching |
| `required_fields` | `required_fields` column | Fields required for **document upload validation** (stored in `doc_metadata`) |

These are **NOT the same** - they serve completely different purposes at different stages of the document lifecycle.

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Client Request                                               │
│     POST /documents-enquiry                                      │
│     { "customerId": "cust-123", "accountId": ["acct-456"] }     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. data_extraction_config                                       │
│     - Calls configured APIs (Customer API, Credit API, etc.)    │
│     - Extracts fields using JSONPath                            │
│     - Returns: { "zipcode": "10222", "creditScore": 785 }       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. Merge into Enhanced Context                                  │
│     {                                                            │
│       "customerId": "cust-123",    // from request              │
│       "zipcode": "10222",          // EXTRACTED                 │
│       "creditScore": 785           // EXTRACTED                 │
│     }                                                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. template_config.eligibility_criteria                         │
│     Evaluate rules using extracted + account data:              │
│     - zipcode IN ["10222", "12220"] → ✅ PASS                   │
│     - creditScore >= 700 → ✅ PASS                              │
│     - accountType == "credit_card" → ✅ PASS (from AccountMeta) │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. Result: Customer is ELIGIBLE → Return documents             │
└─────────────────────────────────────────────────────────────────┘
```

### Complete Example Configuration

**Scenario:** Promotional credit card offer only for customers with:
- Zipcode in specific list (fetched from Customer API)
- Credit score >= 700 (fetched from Credit Bureau API)
- Account type is credit_card (from AccountMetadata)

```sql
INSERT INTO master_template_definition (
    template_type,
    shared_document_flag,
    sharing_scope,
    data_extraction_config,
    template_config
) VALUES (
    'PROMO_OFFER_CC',
    true,
    'CUSTOM_RULES',

    -- data_extraction_config: WHERE to get the data
    '{
      "fieldsToExtract": ["zipcode", "creditScore"],
      "dataSources": {
        "customerApi": {
          "endpoint": {
            "url": "http://customer-service/customers/${customerId}",
            "method": "GET",
            "timeout": 5000
          },
          "providesFields": ["zipcode", "city"]
        },
        "creditApi": {
          "endpoint": {
            "url": "http://credit-service/score/${customerId}",
            "method": "GET"
          },
          "providesFields": ["creditScore"]
        }
      },
      "fieldSources": {
        "zipcode": {
          "sourceApi": "customerApi",
          "extractionPath": "$.address.zipcode"
        },
        "creditScore": {
          "sourceApi": "creditApi",
          "extractionPath": "$.score"
        }
      },
      "executionStrategy": {"mode": "parallel"}
    }',

    -- template_config: HOW to evaluate eligibility
    '{
      "eligibility_criteria": {
        "operator": "AND",
        "rules": [
          {"field": "zipcode", "operator": "IN", "value": ["10222", "12220", "90210"]},
          {"field": "creditScore", "operator": "GREATER_THAN_OR_EQUAL", "value": 700},
          {"field": "accountType", "operator": "EQUALS", "value": "credit_card"}
        ]
      }
    }'
);
```

### Field Resolution Priority

When evaluating eligibility rules, fields are resolved in this order:

1. **AccountMetadata fields** (from database/account service):
   - `accountType`, `region`, `state`, `customerSegment`, `lineOfBusiness`, `accountId`, `customerId`

2. **Extracted fields** (from data_extraction_config APIs):
   - Any field extracted via API calls (e.g., `zipcode`, `creditScore`, `annualIncome`)
   - Automatically available without prefix

3. **Request context** (from original request):
   - Fields passed in the request body

### Use Cases

| Scenario | data_extraction_config | eligibility_criteria |
|----------|----------------------|---------------------|
| Zipcode filtering | Fetch from Customer API | `zipcode IN [...]` |
| Credit score offers | Fetch from Credit Bureau | `creditScore >= 750` |
| Income-based products | Fetch from Income Verification | `annualIncome >= 150000` |
| Employment-specific docs | Fetch from HR/Profile API | `employmentType IN ["SELF_EMPLOYED"]` |
| Loyalty tier rewards | Fetch from Loyalty API | `loyaltyTier == "PLATINUM"` |

### Key Services

| Service | Responsibility |
|---------|---------------|
| `ConfigurableDataExtractionService` | Executes API calls, extracts fields |
| `RuleEvaluationService` | Evaluates eligibility rules |
| `DocumentEnquiryService` | Orchestrates the flow |

---

**template_variables**
- Variables and their API source required to generate the document
- I would like to store variable source information like API and how we are going to extract the information from response.
- Give me ideas how we can achieve this?

---

## Pending Implementation Tasks

### Task 1: Update data.sql with new JSON structure

**What needs to be done:**
1. Migrate eligibility criteria from `access_control` column to `template_config` column
2. Update `access_control` column to use role-based structure (or set to NULL/empty)

**Files to update:**
- `src/main/resources/data.sql`

**Current data.sql entries to migrate:**

| Template | Current access_control | New template_config |
|----------|----------------------|---------------------|
| Template 3 (Balance Transfer) | `{"eligibilityCriteria": {"operator": "AND", "rules": [{"field": "accountType", "operator": "IN", "value": ["credit_card"]}]}}` | Move to `template_config.eligibility_criteria` |
| Template 4 (VIP Offer) | `{"eligibilityCriteria": {"operator": "AND", "rules": [{"field": "customerSegment"...}]}}` | Move to `template_config.eligibility_criteria` |
| Template 5 (Regulatory) | `{"eligibilityCriteria": {"operator": "AND", "rules": [{"field": "$metadata.disclosure_code"...}]}}` | Move to `template_config.eligibility_criteria` |

**Example migration:**

Before (access_control):
```sql
access_control = '{"eligibilityCriteria": {"operator": "AND", "rules": [{"field": "accountType", "operator": "IN", "value": ["credit_card"]}]}}'
```

After:
```sql
access_control = NULL,  -- or role-based: '[{"role": "customer", "actions": ["View","Download"]}]'
template_config = '{"eligibility_criteria": {"operator": "AND", "rules": [{"field": "accountType", "operator": "IN", "value": ["credit_card"]}]}}'
```

---

### Task 2: Implement role-based access control logic

**What needs to be done:**
1. Create service method to evaluate role-based access
2. Integrate with API endpoints to check user role against `access_control`
3. Filter actions based on role permissions

**Files to create/update:**
- Create `AccessControlService.java` or add methods to existing service
- Update controllers to pass user role from request headers (e.g., `X-requestor-type`)

**Implementation approach:**

```java
// Example service method
public boolean canPerformAction(
    MasterTemplateDefinitionEntity template,
    String userRole,
    String action  // "View", "Update", "Delete", "Download"
) {
    if (template.getAccessControl() == null) {
        return true;  // No restrictions
    }

    AccessControl accessControl = objectMapper.readValue(
        template.getAccessControl().asString(),
        AccessControl.class
    );

    return accessControl.hasPermission(userRole, action);
}
```

**Integration points:**
- Document retrieval API - check "View" permission
- Document download API - check "Download" permission
- Document update API - check "Update" permission
- Document delete API - check "Delete" permission

**Request header to use:**
- `X-requestor-type` header already exists in `DocumentEnquiryController`
- Map `XRequestorType` enum values to roles

---

## Completed Tasks

1. Created `TemplateConfig.java` model class with eligibility_criteria
2. Created `RoleAccess.java` model class for role-action mapping
3. Refactored `AccessControl.java` for role-based structure with `@JsonCreator`/`@JsonValue`
4. Updated `RuleEvaluationService.java` to read eligibility from `TemplateConfig`
5. Updated `DocumentEnquiryService.java` - renamed `evaluateAccessControl()` to `evaluateTemplateEligibility()`
6. Added fallback in `RuleEvaluationService.getFieldValue()` to resolve extracted fields from requestContext
7. Created comprehensive unit tests in `RuleEvaluationServiceTest.java` covering 10 scenarios:
   - Zipcode filtering
   - Credit score eligibility
   - Income-based eligibility
   - Multi-criteria AND logic
   - Multi-criteria OR logic
   - Employment type eligibility
   - Account tenure eligibility
   - String pattern matching
   - Null/empty handling
   - Exclusion lists (NOT_IN)
8. Documented how `data_extraction_config` and `eligibility_criteria` work together
9. Renamed `requiredFields` to `fieldsToExtract` in `data_extraction_config` to avoid confusion with `required_fields` column