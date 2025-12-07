#!/usr/bin/env python3
"""
Convert CSV template onboarding files to SQL INSERT statements.
Usage: python convert_to_sql.py [output_file]
"""

import csv
import json
import uuid
import sys
from datetime import datetime

def read_csv(filename):
    """Read CSV file and return list of dictionaries."""
    with open(filename, 'r', encoding='utf-8') as f:
        return list(csv.DictReader(f))

def generate_uuid():
    """Generate a new UUID."""
    return str(uuid.uuid4())

def escape_sql(value):
    """Escape single quotes for SQL."""
    if value is None:
        return 'NULL'
    return value.replace("'", "''")

def convert_templates_to_sql(templates, extraction_configs, eligibility_criteria):
    """Convert templates CSV to SQL INSERT statements."""
    sql_lines = []
    sql_lines.append("-- Master Template Definitions")
    sql_lines.append("-- Generated: " + datetime.now().isoformat())
    sql_lines.append("")

    for template in templates:
        template_id = generate_uuid()
        template_type = template['template_type']

        # Build data_extraction_config JSON
        extraction_fields = [ec for ec in extraction_configs if ec['template_type'] == template_type]
        data_extraction = {"fields": []}
        for field in extraction_fields:
            data_extraction["fields"].append({
                "name": field['field_name'],
                "path": field.get('field_path', ''),
                "type": field['data_type'],
                "required": field['required'].upper() == 'TRUE',
                "label": field.get('display_label', field['field_name'])
            })

        # Build template_config JSON with eligibility_criteria
        eligibility = [ec for ec in eligibility_criteria if ec['template_type'] == template_type]
        template_config = {}
        if eligibility:
            criteria_list = []
            for ec in eligibility:
                criteria = {
                    "field": ec['criteria_field'],
                    "source": ec['criteria_source'],
                    "operator": ec['operator'],
                    "values": ec['criteria_values'].split(',')
                }
                if ec.get('api_endpoint'):
                    criteria["api_endpoint"] = ec['api_endpoint']
                criteria_list.append(criteria)
            template_config["eligibility_criteria"] = criteria_list

        # Build SQL INSERT
        sql = f"""INSERT INTO document_hub.master_template_definition (
    template_id, template_type, template_version, template_category,
    display_name, description, line_of_business, shared_flag,
    mock_api_url, data_extraction_config, template_config, accessible_flag
) VALUES (
    '{template_id}',
    '{escape_sql(template_type)}',
    {template['template_version']},
    '{escape_sql(template['template_category'])}',
    '{escape_sql(template['display_name'])}',
    '{escape_sql(template['description'])}',
    '{escape_sql(template['line_of_business'])}',
    {template['shared_flag'].lower()},
    {f"'{escape_sql(template['mock_api_url'])}'" if template.get('mock_api_url') else 'NULL'},
    '{json.dumps(data_extraction)}',
    '{json.dumps(template_config)}',
    true
);"""
        sql_lines.append(sql)
        sql_lines.append("")

    return "\n".join(sql_lines)

def convert_documents_to_sql(documents):
    """Convert documents CSV to SQL INSERT statements."""
    sql_lines = []
    sql_lines.append("")
    sql_lines.append("-- Storage Index (Documents)")
    sql_lines.append("-- Generated: " + datetime.now().isoformat())
    sql_lines.append("")

    for doc in documents:
        doc_id = generate_uuid()

        # Build doc_metadata JSON from extracted_* columns
        doc_metadata = {}
        for key, value in doc.items():
            if key.startswith('extracted_') and value:
                field_name = key.replace('extracted_', '')
                doc_metadata[field_name] = value

        # Parse dates
        valid_from = f"'{doc['valid_from']}'" if doc.get('valid_from') else 'NULL'
        valid_until = f"'{doc['valid_until']}'" if doc.get('valid_until') else 'NULL'

        # Handle nullable fields
        account_key = f"'{doc['account_key']}'" if doc.get('account_key') else 'NULL'
        customer_key = f"'{doc['customer_key']}'" if doc.get('customer_key') else 'NULL'
        reference_key = f"'{escape_sql(doc['reference_key'])}'" if doc.get('reference_key') else 'NULL'
        reference_key_type = f"'{escape_sql(doc['reference_key_type'])}'" if doc.get('reference_key_type') else 'NULL'

        sql = f"""INSERT INTO document_hub.storage_index (
    storage_index_id, template_type, template_version, file_name, file_location,
    account_key, customer_key, reference_key, reference_key_type,
    doc_creation_date, valid_from, valid_until, shared_flag, accessible_flag, doc_metadata
) VALUES (
    '{doc_id}',
    '{escape_sql(doc['template_type'])}',
    {doc['template_version']},
    '{escape_sql(doc['file_name'])}',
    '{escape_sql(doc['file_location'])}',
    {account_key},
    {customer_key},
    {reference_key},
    {reference_key_type},
    NOW(),
    {valid_from},
    {valid_until},
    {str(not doc.get('account_key') and not doc.get('customer_key')).lower()},
    true,
    '{json.dumps(doc_metadata)}'
);"""
        sql_lines.append(sql)
        sql_lines.append("")

    return "\n".join(sql_lines)

def main():
    output_file = sys.argv[1] if len(sys.argv) > 1 else 'generated_data.sql'

    print("Reading CSV files...")
    templates = read_csv('1_Templates.csv')
    extraction_configs = read_csv('2_DataExtractionConfig.csv')
    eligibility_criteria = read_csv('3_EligibilityCriteria.csv')
    documents = read_csv('4_Documents.csv')

    print(f"Found {len(templates)} templates")
    print(f"Found {len(extraction_configs)} extraction configs")
    print(f"Found {len(eligibility_criteria)} eligibility criteria")
    print(f"Found {len(documents)} documents")

    print("\nGenerating SQL...")
    sql_output = []
    sql_output.append("-- Auto-generated SQL from CSV onboarding files")
    sql_output.append(f"-- Generated: {datetime.now().isoformat()}")
    sql_output.append("-- ================================================")
    sql_output.append("")

    sql_output.append(convert_templates_to_sql(templates, extraction_configs, eligibility_criteria))
    sql_output.append(convert_documents_to_sql(documents))

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("\n".join(sql_output))

    print(f"\nSQL written to: {output_file}")
    print("Done!")

if __name__ == '__main__':
    main()
