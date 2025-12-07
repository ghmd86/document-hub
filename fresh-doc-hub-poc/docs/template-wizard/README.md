# Document Template Builder Wizard

An interactive HTML wizard for creating document templates without requiring technical knowledge of JSONPath or complex configuration formats.

## Quick Start

1. Double-click `open-wizard.bat` to open the wizard in your browser
2. Follow the 6-step process to create your template
3. Download the generated SQL or copy the JSON configuration

## Features

### Step 1: Basic Information
- Template name and description
- Document category selection (Statement, Legal, Tax, Regulatory, Notice)
- Line of business selection

### Step 2: Document Ownership
- Account-specific documents
- Customer-wide documents
- Shared/public documents
- Conditional access documents

### Step 3: Extractable Fields
- Pre-populated fields based on document category
- Add custom fields as needed
- Automatic field type detection

### Step 4: Source APIs
Three ways to configure source APIs for eligibility checks:

#### Option A: Use Existing API
Select from pre-defined APIs:
- Credit Info API - membership tier, credit limit, account status
- Account Info API - balance, account type, customer tenure
- Arrangements API - pricing ID, product type, disclosure codes

#### Option B: Upload API Spec (NEW)
1. Upload an OpenAPI 3.0 or Swagger 2.0 specification (JSON format)
2. Select the endpoint that provides eligibility data
3. Choose response fields to use for access rules
4. The wizard automatically generates the field paths (JSONPath)

**Supported formats:**
- OpenAPI 3.0 (JSON)
- Swagger 2.0 (JSON)
- YAML files should be converted to JSON first

**Sample API spec:** Use `sample-api-spec.json` as a reference

#### Option C: Add Custom API
Manually register a new API with:
- API name and endpoint URL
- HTTP method
- Available fields

### Step 5: Access Rules
- Visual rule builder with dropdowns
- Plain language description option
- Automatic field population from selected API

### Step 6: Review & Generate
- Summary of all configurations
- Generated JSON configuration
- Downloadable SQL insert statement

## Files

| File | Description |
|------|-------------|
| `index.html` | The main wizard application |
| `open-wizard.bat` | Windows batch file to open the wizard |
| `sample-api-spec.json` | Sample OpenAPI 3.0 spec for testing |
| `README.md` | This documentation file |

## Generated Output

### JSON Configuration
```json
{
  "template_type": "CREDIT_CARD_STATEMENT",
  "template_version": 1,
  "template_category": "Statement",
  "display_name": "Credit Card Statement",
  "line_of_business": "CREDIT_CARD",
  "shared_flag": false,
  "mock_api_url": "/api/v1/customers/{customerId}/credit-info",
  "data_extraction_config": {
    "fields": [
      { "name": "statement_date", "type": "DATE", "required": true }
    ]
  },
  "template_config": {
    "eligibility_criteria": [
      {
        "field": "membershipTier",
        "field_path": "$.membershipTier",
        "source": "API_CALL",
        "operator": "IN",
        "values": ["PLATINUM", "GOLD"],
        "api_endpoint": "/api/v1/customers/{customerId}/credit-info"
      }
    ]
  },
  "api_spec_source": {
    "title": "Customer Credit Info API",
    "method": "GET",
    "path": "/api/v1/customers/{customerId}/credit-info",
    "selected_fields": [
      { "name": "membershipTier", "path": "$.membershipTier", "type": "string" }
    ]
  }
}
```

### SQL Output
The wizard generates a complete INSERT statement for `document_hub.master_template_definition`.

## Technical Notes

- The wizard is a standalone HTML file with no external dependencies
- All processing happens client-side in the browser
- OpenAPI spec parsing supports $ref resolution for component schemas
- Field paths are automatically generated in JSONPath format

## Limitations

- YAML API specs require conversion to JSON
- Complex schema compositions (oneOf, anyOf) have limited support
- Maximum recursion depth for nested objects is 5 levels
