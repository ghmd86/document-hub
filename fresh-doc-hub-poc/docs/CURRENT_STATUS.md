# Document Hub POC - Current Status

**Last Updated:** 2024-12-07 (LOB/template_type corrections)

## Quick Start

```bash
# Start the application
cd test-scripts && restart-app.bat

# Test API
test-api-enquiry.bat

# Open Template Wizard
cd docs/template-wizard && open-wizard.bat
```

## Recent Changes (Dec 7, 2024)

### 1. Template Builder Wizard Enhanced
- Added **API Spec Upload** feature (OpenAPI 3.0/Swagger 2.0 JSON)
- Users can upload API spec → select endpoint → pick response fields
- JSONPath is auto-generated - no technical knowledge needed
- Added **Reference Key Configuration** to capture what goes in `reference_key` column

### 2. Database Schema Cleaned Up
- Archived old/outdated SQL files to `archive/` folder
- Only `data.sql` remains in `src/main/resources/`
- Single source of truth for schema and seed data

### 3. Column Naming Clarified
- `doc_metadata` (not `extracted_data`) - stores extracted values from documents
- `reference_key` + `reference_key_type` - for eligibility matching
- `template_config` - contains `reference_key_config` and `eligibility_criteria`

## Project Structure

```
fresh-doc-hub-poc/
├── src/main/resources/
│   └── data.sql                    # Schema + seed data (ONLY SQL file)
├── docs/
│   ├── architecture/               # Architecture documentation
│   │   ├── README.md               # Architecture overview
│   │   ├── document-enquiry-flow.md # Detailed flow diagrams
│   │   └── use-cases-design-review.md # Use cases & design analysis
│   ├── template-wizard/            # Interactive HTML wizard
│   │   ├── index.html              # Main wizard
│   │   ├── sample-api-spec.json    # Sample OpenAPI spec
│   │   └── open-wizard.bat         # Launch wizard
│   ├── sample-data/                # CSV-based onboarding
│   │   ├── 1_Templates.csv
│   │   ├── 2_DataExtractionConfig.csv
│   │   ├── 3_EligibilityCriteria.csv
│   │   ├── 4_Documents.csv
│   │   └── convert_to_sql.py       # CSV → SQL converter
│   └── CURRENT_STATUS.md           # This file
├── test-scripts/                   # Testing utilities
│   ├── restart-app.bat
│   ├── test-api-enquiry.bat
│   ├── check-db-counts.bat
│   └── reload-db-data.bat
└── archive/                        # Archived files
    ├── old-schema/                 # Old database.sql, schema.sql
    └── old-sql-scripts/            # Old test/fix scripts
```

## Key Data Model

### master_template_definition
```
template_type           → e.g., "CREDIT_CARD_STATEMENT"
data_extraction_config  → Defines what fields go into doc_metadata
template_config         → Contains:
  ├── reference_key_config  → What reference_key to store
  │     ├── reference_key_type: "DISCLOSURE_CODE"
  │     └── source: "api" | "document" | "metadata"
  └── eligibility_criteria  → Access rules
```

### storage_index (per document)
```
reference_key           → e.g., "D164" (for matching)
reference_key_type      → e.g., "DISCLOSURE_CODE"
doc_metadata            → {"statement_date": "2024-01-31", "balance": 1234.56}
```

## Template Onboarding Options

### Option 1: HTML Wizard (Recommended for POC demo)
1. Open `docs/template-wizard/open-wizard.bat`
2. Follow 6-step wizard
3. Download generated SQL

### Option 2: CSV Import (Bulk onboarding)
1. Edit CSV files in `docs/sample-data/`
2. Run `python convert_to_sql.py`
3. Apply generated SQL to database

## API Testing

```bash
# Test with Customer ID and Account ID
curl -X POST "http://localhost:8080/api/v1/documents-enquiry" \
  -H "Content-Type: application/json" \
  -H "X-requestor-id: cccc0000-0000-0000-0000-000000000001" \
  -H "X-requestor-type: CUSTOMER" \
  -d '{"customerId":"cccc0000-0000-0000-0000-000000000001","accountId":["aaaa0000-0000-0000-0000-000000000001"]}'
```

## Database Connection

```
Host: localhost
Port: 5432
Database: document_hub
Schema: document_hub
User: postgres
Password: 1qaz#EDC
```

## Architecture Documentation

See the [architecture](./architecture/) folder for detailed documentation:

- **[Document Enquiry Flow](./architecture/document-enquiry-flow.md)** - Detailed sequence diagrams, flowcharts, and decision trees for the document retrieval logic
- **[Use Cases & Design Review](./architecture/use-cases-design-review.md)** - Complete analysis of all 10 use cases, database schema review, and recommendations

## Next Steps / Pending Items

1. Update documentation files that still reference old schema files
2. Add YAML support to API spec upload (currently JSON only)
3. Consider adding document preview in wizard
4. Integrate wizard output with actual database insert API
5. Implement download/upload/delete endpoints (currently spec only)
6. Add circuit breaker for external API calls
