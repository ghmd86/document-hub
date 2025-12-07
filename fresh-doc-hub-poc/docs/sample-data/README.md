# Template Onboarding Sample Data

This folder contains sample CSV files for onboarding document templates to the Document Hub.

## Files

| File | Description |
|------|-------------|
| `1_Templates.csv` | Template definitions (type, category, line of business) |
| `2_DataExtractionConfig.csv` | Fields to extract from documents |
| `3_EligibilityCriteria.csv` | Conditional access rules |
| `4_Documents.csv` | Sample documents to index |
| `convert_to_sql.py` | Python script to convert CSVs to SQL |
| `generate_sql.bat` | Windows batch file to run converter |
| `generated_data.sql` | Output SQL (generated) |

## Quick Start

### Option 1: Edit CSVs in Excel

1. Open the CSV files in Excel
2. Add/modify your template data
3. Save as CSV (keep same filenames)
4. Run `generate_sql.bat` to create SQL
5. Apply SQL to database

### Option 2: Create Excel Workbook

1. Create new Excel workbook
2. Create 4 sheets named: `Templates`, `DataExtractionConfig`, `EligibilityCriteria`, `Documents`
3. Copy headers from sample CSVs
4. Fill in your data
5. Export each sheet as CSV with names `1_Templates.csv`, etc.
6. Run `generate_sql.bat`

## Workflow

```
[Excel/CSV Files] --> [convert_to_sql.py] --> [generated_data.sql] --> [PostgreSQL]
```

## Column Reference

### Templates Sheet
- `template_type`: Unique identifier (e.g., `CREDIT_CARD_STATEMENT`)
- `template_version`: Integer version number
- `template_category`: Grouping category (Statement, Legal, Tax, etc.)
- `line_of_business`: Business line (CREDIT_CARD, MORTGAGE, TAX, SHARED)
- `shared_flag`: TRUE if visible to all users

### DataExtractionConfig Sheet
- `field_name`: Field identifier (e.g., `statement_date`)
- `field_path`: JSONPath for extraction (e.g., `$.header.date`)
- `data_type`: STRING, DATE, NUMBER, or BOOLEAN

### EligibilityCriteria Sheet
- `criteria_source`: API_CALL, REQUEST_CONTEXT, or DOCUMENT_METADATA
- `operator`: EQUALS, IN, GREATER_THAN, LESS_THAN, CONTAINS

### Documents Sheet
- Either `account_key` OR `customer_key` required (unless shared)
- `extracted_*` columns for pre-extracted field values

## Running the Converter

```bash
# Using batch file
generate_sql.bat

# Using Python directly
python convert_to_sql.py output.sql
```

## Applying to Database

```bash
# Windows (set password first)
set PGPASSWORD=1qaz#EDC
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d document_hub -f generated_data.sql
```

See `../Template_Onboarding_Guide.md` for full documentation.
