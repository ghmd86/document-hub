# Seed Data Management

This directory contains YAML-based seed data that generates `data.sql`.

## Why YAML?

| Approach | Readability | Diff-friendly | Adding Columns |
|----------|-------------|---------------|----------------|
| Raw SQL  | Poor        | Hard          | Update every INSERT |
| CSV      | Medium      | Good          | Add column header |
| **YAML** | **Excellent** | **Good**    | **Update generator once** |

## Files

```
seed-data/
├── templates.yaml      # Template definitions
├── documents.yaml      # Document/storage index entries
├── accounts.yaml       # Account metadata (optional)
├── generate_sql.py     # YAML → SQL generator
└── README.md
```

## Usage

### Generate data.sql

```bash
cd src/main/resources/seed-data
python generate_sql.py > ../data.sql
```

### Adding a New Template

Edit `templates.yaml`:

```yaml
templates:
  - id: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    template_type: MyNewTemplate
    template_name: My New Template
    template_description: Description here
    line_of_business: CREDIT_CARD
    # ... other fields
```

### Adding a New Column

1. Add field to YAML templates
2. Update `generate_sql.py` to include the column
3. Regenerate: `python generate_sql.py > ../data.sql`

## YAML Structure

### Template Definition

```yaml
- id: "uuid-here"
  template_type: TemplateName
  template_name: Human Readable Name
  template_description: Description
  template_category: Statement | Regulatory | Promotional | Onboarding
  line_of_business: CREDIT_CARD | ENTERPRISE | digital_banking

  # Flags
  active_flag: true
  shared_document_flag: false
  single_document_flag: true
  message_center_doc_flag: true

  # Sharing
  sharing_scope: ALL | CUSTOM_RULES | credit_card

  # JSON configs (each in dedicated column)
  template_config:
    reprint_policy:
      cooldown_period_days: 30

  eligibility_criteria:
    operator: AND
    rules:
      - field: customerSegment
        operator: EQUALS
        value: VIP

  document_matching_config:
    matchBy: reference_key
    referenceKeyField: disclosureCode
    referenceKeyType: DISCLOSURE_CODE

  data_extraction_config:
    fieldsToExtract: [field1, field2]
    # ... extraction details
```

### Document Definition

```yaml
- id: "uuid-here"
  template_id: "template-uuid"
  template_type: TemplateName
  shared_flag: false
  account_key: "account-uuid"      # For account-specific docs
  customer_key: "customer-uuid"    # For customer-specific docs
  reference_key: "REF-123"         # For reference-based matching
  reference_key_type: DISCLOSURE_CODE
  storage_document_key: "ecms-key"
  file_name: document.pdf
  doc_creation_date: 1704067200000  # Epoch millis
  doc_metadata:
    key: value
```

## Benefits

1. **Readable**: YAML is human-friendly, supports comments
2. **Maintainable**: One source of truth, easy to update
3. **Diff-friendly**: Changes are clear in git diffs
4. **Flexible**: Adding columns = update generator once
5. **Validated**: Python catches syntax errors early
