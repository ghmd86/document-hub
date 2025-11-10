# Shared Document Eligibility Catalog - Project Management

This directory contains tools and templates for managing the Shared Document Eligibility Catalog.

## Files in This Directory

### `Shared_Document_Eligibility_Catalog.xlsx`
**Ready-to-use Excel workbook** for collecting shared document requirements from SMEs and business stakeholders.

**Contains 7 worksheets:**

1. **INSTRUCTIONS** - Step-by-step guide for using the workbook
2. **DOCUMENT_LIST** - Master inventory of all shared documents (quick reference)
3. **DOCUMENT_DETAILS** - Complete metadata for each document (business context, ownership, dates)
4. **ELIGIBILITY_CONDITIONS** - Row-per-condition for building eligibility rules
5. **DATA_SOURCES** - API endpoint mappings with JSON paths
6. **TEST_SCENARIOS** - Test cases with mock data and expected results
7. **LOOKUPS** - Reference data for all dropdowns (operators, statuses, types)

**Features:**
- Data validation dropdowns prevent errors
- Auto-populated fields using VLOOKUP formulas
- Color-coded tabs and status indicators
- Frozen panes and professional formatting
- Sample row in DOCUMENT_LIST to demonstrate structure

## How to Use

### Step 1: Interview SMEs
Use the questionnaire in `docs/guides/SHARED_DOCUMENT_SME_QUESTIONNAIRE.md` to conduct structured interviews with Subject Matter Experts.

### Step 2: Fill Excel Template
Open `Shared_Document_Eligibility_Catalog.xlsx` and:
1. Read the INSTRUCTIONS sheet
2. Add documents to DOCUMENT_LIST
3. Complete DOCUMENT_DETAILS for each
4. Define ELIGIBILITY_CONDITIONS (for custom_rule scope only)
5. Map DATA_SOURCES (APIs and field extractions)
6. Create TEST_SCENARIOS (at least 2 per document)

### Step 3: Convert to YAML
Run the conversion script:
```bash
python scripts/convert_excel_to_yaml.py \
    docs/project-management/Shared_Document_Eligibility_Catalog.xlsx \
    output_yaml/
```

This generates individual YAML files (one per document) in the `output_yaml/` directory.

### Step 4: Review and Import
1. Review generated YAML files for accuracy
2. Manually adjust `rule_type` and `logic_operator` if needed
3. Validate against schema
4. Import JSON into database `master_template_definition.data_extraction_schema` column

## Related Documentation

### Guides
- **`docs/guides/SHARED_DOCUMENT_ELIGIBILITY_CATALOG.md`** - Complete reference with template structure, 70+ questions, 5 examples, operator reference
- **`docs/guides/SHARED_DOCUMENT_SME_QUESTIONNAIRE.md`** - 45-minute structured interview guide for SMEs
- **`docs/guides/SHARED_DOCUMENT_CATALOG_EXAMPLE.md`** - Full worked example (Premium Travel Insurance)
- **`docs/guides/SHARED_DOCUMENT_CATALOG_EXCEL_TEMPLATE.md`** - Detailed specification for Excel workbook

### Scripts
- **`scripts/create_excel_template.py`** - Regenerates the Excel workbook (if needed)
- **`scripts/convert_excel_to_yaml.py`** - Converts Excel → YAML catalog entries

## Workflow Overview

```
┌─────────────────┐
│  SME Interview  │ (Use questionnaire guide)
└────────┬────────┘
         │
         v
┌─────────────────┐
│  Fill Excel     │ (Shared_Document_Eligibility_Catalog.xlsx)
│  - DOCUMENT_LIST│
│  - DETAILS      │
│  - CONDITIONS   │
│  - DATA_SOURCES │
│  - TEST_CASES   │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Convert to YAML │ (convert_excel_to_yaml.py)
└────────┬────────┘
         │
         v
┌─────────────────┐
│  Review YAML    │ (Manual validation)
└────────┬────────┘
         │
         v
┌─────────────────┐
│  Import to DB   │ (data_extraction_schema JSONB column)
└────────┬────────┘
         │
         v
┌─────────────────┐
│ CustomRuleEngine│ (Evaluates rules at runtime)
│   evaluates     │
└─────────────────┘
```

## Example: Premium Travel Insurance

See `docs/guides/SHARED_DOCUMENT_CATALOG_EXAMPLE.md` for a complete worked example showing:
- SME interview notes
- Filled Excel entries
- Generated YAML
- Test scenarios
- Implementation JSON

## Regenerating the Excel Template

If you need to recreate the Excel workbook from scratch:

```bash
python scripts/create_excel_template.py \
    docs/project-management/Shared_Document_Eligibility_Catalog.xlsx
```

This will overwrite the existing file with a fresh template.

## Support

- **Questions about catalog structure?** See `SHARED_DOCUMENT_ELIGIBILITY_CATALOG.md`
- **Questions about interviewing SMEs?** See `SHARED_DOCUMENT_SME_QUESTIONNAIRE.md`
- **Questions about implementation?** See existing Java code in `CustomRuleEngine.java`
- **Technical issues?** Contact the Document Hub development team

---

**Created:** 2025-11-09
**Version:** 1.0
**Owner:** Document Hub Technical Team
