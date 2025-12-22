#!/usr/bin/env python3
"""
Generate data.sql from YAML seed data files.

Usage:
    python generate_sql.py                    # Output to stdout
    python generate_sql.py > ../data.sql     # Write to data.sql

Benefits:
    - YAML is more readable than embedded JSON in SQL
    - Single source of truth for seed data
    - Adding columns only requires updating this script
    - Easy to diff and review changes
"""

import yaml
import json
import sys
from datetime import datetime
from typing import Any, Dict, List, Optional


def load_yaml(filename: str) -> Dict:
    """Load YAML file."""
    with open(filename, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


def json_value(value: Any) -> str:
    """Convert Python value to SQL JSON string."""
    if value is None:
        return 'NULL'
    return "'" + json.dumps(value, indent=2).replace("'", "''") + "'"


def sql_value(value: Any, quote: bool = True) -> str:
    """Convert Python value to SQL value."""
    if value is None:
        return 'NULL'
    if isinstance(value, bool):
        return 'true' if value else 'false'
    if isinstance(value, (int, float)):
        return str(value)
    if quote:
        return "'" + str(value).replace("'", "''") + "'"
    return str(value)


def generate_template_insert(template: Dict, defaults: Dict) -> str:
    """Generate INSERT statement for a template."""

    # Merge with defaults
    t = {**defaults, **template}

    # Build column list and values
    columns = []
    values = []

    # Required columns
    columns.append('master_template_id')
    values.append(sql_value(t['id']))

    columns.append('template_version')
    values.append(sql_value(t.get('template_version', 1), quote=False))

    columns.append('template_type')
    values.append(sql_value(t['template_type']))

    columns.append('template_name')
    values.append(sql_value(t['template_name']))

    columns.append('template_description')
    values.append(sql_value(t.get('template_description')))

    columns.append('template_category')
    values.append(sql_value(t.get('template_category')))

    columns.append('line_of_business')
    values.append(sql_value(t.get('line_of_business')))

    columns.append('language_code')
    values.append(sql_value(t.get('language_code', 'EN_US')))

    columns.append('active_flag')
    values.append(sql_value(t.get('active_flag', True), quote=False))

    columns.append('shared_document_flag')
    values.append(sql_value(t.get('shared_document_flag', False), quote=False))

    if t.get('sharing_scope'):
        columns.append('sharing_scope')
        values.append(sql_value(t['sharing_scope']))

    columns.append('single_document_flag')
    values.append(sql_value(t.get('single_document_flag', True), quote=False))

    columns.append('message_center_doc_flag')
    values.append(sql_value(t.get('message_center_doc_flag', True), quote=False))

    columns.append('communication_type')
    values.append(sql_value(t.get('communication_type', 'LETTER')))

    # JSON columns - each in its own dedicated column
    if t.get('template_config'):
        columns.append('template_config')
        values.append(json_value(t['template_config']))
    else:
        columns.append('template_config')
        values.append("'{}'")

    if t.get('eligibility_criteria'):
        columns.append('eligibility_criteria')
        values.append(json_value(t['eligibility_criteria']))

    if t.get('document_matching_config'):
        columns.append('document_matching_config')
        values.append(json_value(t['document_matching_config']))

    if t.get('data_extraction_config'):
        columns.append('data_extraction_config')
        values.append(json_value(t['data_extraction_config']))

    if t.get('access_control'):
        columns.append('access_control')
        values.append(json_value(t['access_control']))

    # Timestamps
    if t.get('start_date'):
        columns.append('start_date')
        values.append(sql_value(t['start_date'], quote=False))

    columns.append('created_by')
    values.append(sql_value(t.get('created_by', 'system')))

    columns.append('created_timestamp')
    values.append('NOW()')

    # Format SQL
    col_str = ',\n    '.join(columns)
    val_str = ',\n    '.join(values)

    return f"""INSERT INTO document_hub.master_template_definition (
    {col_str}
) VALUES (
    {val_str}
);"""


def generate_document_insert(doc: Dict) -> str:
    """Generate INSERT statement for a document."""
    columns = []
    values = []

    columns.append('storage_index_id')
    values.append(sql_value(doc['id']))

    columns.append('master_template_id')
    values.append(sql_value(doc['template_id']))

    columns.append('template_version')
    values.append(sql_value(doc.get('template_version', 1), quote=False))

    columns.append('template_type')
    values.append(sql_value(doc['template_type']))

    columns.append('shared_flag')
    values.append(sql_value(doc.get('shared_flag', False), quote=False))

    if doc.get('account_key'):
        columns.append('account_key')
        values.append(sql_value(doc['account_key']))

    if doc.get('customer_key'):
        columns.append('customer_key')
        values.append(sql_value(doc['customer_key']))

    if doc.get('reference_key'):
        columns.append('reference_key')
        values.append(sql_value(doc['reference_key']))

    if doc.get('reference_key_type'):
        columns.append('reference_key_type')
        values.append(sql_value(doc['reference_key_type']))

    columns.append('storage_vendor')
    values.append(sql_value(doc.get('storage_vendor', 'ecms')))

    columns.append('storage_document_key')
    values.append(sql_value(doc['storage_document_key']))

    columns.append('file_name')
    values.append(sql_value(doc['file_name']))

    if doc.get('doc_creation_date'):
        columns.append('doc_creation_date')
        values.append(sql_value(doc['doc_creation_date'], quote=False))

    columns.append('accessible_flag')
    values.append(sql_value(doc.get('accessible_flag', True), quote=False))

    # Temporal fields for document versioning
    if doc.get('start_date'):
        columns.append('start_date')
        values.append(sql_value(doc['start_date'], quote=False))

    if doc.get('end_date'):
        columns.append('end_date')
        values.append(sql_value(doc['end_date'], quote=False))

    if doc.get('doc_metadata'):
        columns.append('doc_metadata')
        values.append(json_value(doc['doc_metadata']))

    columns.append('created_by')
    values.append(sql_value(doc.get('created_by', 'system')))

    columns.append('created_timestamp')
    values.append('NOW()')

    col_str = ',\n    '.join(columns)
    val_str = ',\n    '.join(values)

    return f"""INSERT INTO document_hub.storage_index (
    {col_str}
) VALUES (
    {val_str}
);"""


def main():
    print(f"""-- =============================================================================
-- DOCUMENT HUB SEED DATA
-- =============================================================================
-- Auto-generated from YAML files
-- Generated: {datetime.now().isoformat()}
--
-- Source files:
--   - templates.yaml
--   - documents.yaml (if exists)
--   - accounts.yaml (if exists)
--
-- To regenerate: python generate_sql.py > ../data.sql
-- =============================================================================

""")

    # Load templates
    try:
        data = load_yaml('templates.yaml')
        defaults = data.get('defaults', {})
        templates = data.get('templates', [])

        print("-- ====================================================================")
        print("-- MASTER TEMPLATE DEFINITIONS")
        print("-- ====================================================================")
        print()

        for template in templates:
            template_name = template.get('template_name', template.get('template_type'))
            print(f"-- Template: {template_name}")
            print(generate_template_insert(template, defaults))
            print()

    except FileNotFoundError:
        print("-- templates.yaml not found", file=sys.stderr)

    # Load documents
    try:
        docs_data = load_yaml('documents.yaml')
        documents = docs_data.get('documents', [])

        print("-- ====================================================================")
        print("-- STORAGE INDEX (DOCUMENTS)")
        print("-- ====================================================================")
        print()

        for doc in documents:
            print(f"-- Document: {doc.get('file_name')}")
            print(generate_document_insert(doc))
            print()

    except FileNotFoundError:
        pass  # documents.yaml is optional

    # Load accounts
    try:
        accounts_data = load_yaml('accounts.yaml')
        # Generate account metadata inserts...
    except FileNotFoundError:
        pass  # accounts.yaml is optional


if __name__ == '__main__':
    main()
