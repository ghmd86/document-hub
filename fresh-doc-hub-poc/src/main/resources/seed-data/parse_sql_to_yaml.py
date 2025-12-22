#!/usr/bin/env python3
"""
Parse existing data.sql and convert to YAML format.

Usage:
    python parse_sql_to_yaml.py

This script reads ../data.sql and generates:
    - templates.yaml
    - documents.yaml
"""

import re
import json
import yaml
from typing import Dict, List, Any, Optional
from pathlib import Path


def clean_sql_value(value: str) -> Any:
    """Convert SQL value to Python type."""
    value = value.strip()

    # NULL
    if value.upper() == 'NULL':
        return None

    # Boolean
    if value.lower() == 'true':
        return True
    if value.lower() == 'false':
        return False

    # NOW() -> skip (handled by generator)
    if value.upper() == 'NOW()':
        return None

    # String (remove quotes)
    if value.startswith("'") and value.endswith("'"):
        value = value[1:-1]
        # Unescape single quotes
        value = value.replace("''", "'")

        # Try to parse as JSON
        if value.startswith('{') or value.startswith('['):
            try:
                return json.loads(value)
            except json.JSONDecodeError:
                return value

        return value

    # Number
    try:
        if '.' in value:
            return float(value)
        return int(value)
    except ValueError:
        return value


def parse_insert_statement(sql: str) -> Optional[Dict[str, Any]]:
    """Parse a single INSERT statement into a dict."""

    # Extract table name
    table_match = re.search(r'INSERT INTO\s+([\w.]+)\s*\(', sql, re.IGNORECASE)
    if not table_match:
        return None

    table_name = table_match.group(1)

    # Extract column names
    columns_match = re.search(r'\(\s*([^)]+)\s*\)\s*VALUES', sql, re.IGNORECASE | re.DOTALL)
    if not columns_match:
        return None

    columns_str = columns_match.group(1)
    columns = [c.strip() for c in columns_str.split(',')]

    # Extract values - find the VALUES clause
    values_start = sql.upper().find('VALUES')
    if values_start == -1:
        return None

    values_part = sql[values_start + 6:].strip()

    # Remove outer parentheses and trailing semicolon
    if values_part.startswith('('):
        values_part = values_part[1:]
    if values_part.endswith(');'):
        values_part = values_part[:-2]
    elif values_part.endswith(')'):
        values_part = values_part[:-1]

    # Parse values (handling nested JSON with commas)
    values = []
    current_value = ''
    paren_depth = 0
    brace_depth = 0
    bracket_depth = 0
    in_string = False
    escape_next = False

    for char in values_part:
        if escape_next:
            current_value += char
            escape_next = False
            continue

        if char == '\\':
            escape_next = True
            current_value += char
            continue

        if char == "'" and not escape_next:
            in_string = not in_string
            current_value += char
            continue

        if in_string:
            current_value += char
            continue

        if char == '(':
            paren_depth += 1
            current_value += char
        elif char == ')':
            paren_depth -= 1
            current_value += char
        elif char == '{':
            brace_depth += 1
            current_value += char
        elif char == '}':
            brace_depth -= 1
            current_value += char
        elif char == '[':
            bracket_depth += 1
            current_value += char
        elif char == ']':
            bracket_depth -= 1
            current_value += char
        elif char == ',' and paren_depth == 0 and brace_depth == 0 and bracket_depth == 0:
            values.append(clean_sql_value(current_value.strip()))
            current_value = ''
        else:
            current_value += char

    if current_value.strip():
        values.append(clean_sql_value(current_value.strip()))

    # Create dict
    if len(columns) != len(values):
        print(f"Warning: column count ({len(columns)}) != value count ({len(values)})")
        print(f"Columns: {columns}")
        print(f"Values: {values[:5]}...")
        return None

    result = {'_table': table_name}
    for col, val in zip(columns, values):
        col = col.strip()
        if val is not None:  # Skip NULL and NOW()
            result[col] = val

    return result


def split_sql_statements(sql_content: str) -> List[str]:
    """Split SQL file into individual INSERT statements."""
    statements = []
    current = ''

    for line in sql_content.split('\n'):
        line = line.strip()

        # Skip comments and empty lines
        if line.startswith('--') or not line:
            continue

        current += ' ' + line

        # Check if statement is complete
        if line.endswith(');'):
            if 'INSERT INTO' in current.upper():
                statements.append(current.strip())
            current = ''

    return statements


def convert_template_to_yaml(data: Dict) -> Dict:
    """Convert parsed template dict to YAML-friendly format."""
    result = {}

    # ID
    result['id'] = data.get('master_template_id')

    # Basic fields
    field_map = {
        'template_type': 'template_type',
        'template_version': 'template_version',
        'template_name': 'template_name',
        'template_description': 'template_description',
        'template_category': 'template_category',
        'line_of_business': 'line_of_business',
        'language_code': 'language_code',
        'active_flag': 'active_flag',
        'shared_document_flag': 'shared_document_flag',
        'sharing_scope': 'sharing_scope',
        'single_document_flag': 'single_document_flag',
        'message_center_doc_flag': 'message_center_doc_flag',
        'communication_type': 'communication_type',
        'start_date': 'start_date',
        'workflow': 'workflow',
    }

    for sql_field, yaml_field in field_map.items():
        if sql_field in data and data[sql_field] is not None:
            result[yaml_field] = data[sql_field]

    # JSON fields
    if data.get('template_config') and data['template_config'] != {}:
        result['template_config'] = data['template_config']

    if data.get('eligibility_criteria'):
        result['eligibility_criteria'] = data['eligibility_criteria']

    if data.get('document_matching_config'):
        result['document_matching_config'] = data['document_matching_config']

    if data.get('data_extraction_config'):
        result['data_extraction_config'] = data['data_extraction_config']

    if data.get('access_control'):
        result['access_control'] = data['access_control']

    return result


def convert_document_to_yaml(data: Dict) -> Dict:
    """Convert parsed document dict to YAML-friendly format."""
    result = {}

    result['id'] = data.get('storage_index_id')
    result['template_id'] = data.get('master_template_id')

    field_map = {
        'template_type': 'template_type',
        'template_version': 'template_version',
        'shared_flag': 'shared_flag',
        'account_key': 'account_key',
        'customer_key': 'customer_key',
        'reference_key': 'reference_key',
        'reference_key_type': 'reference_key_type',
        'storage_vendor': 'storage_vendor',
        'storage_document_key': 'storage_document_key',
        'file_name': 'file_name',
        'doc_creation_date': 'doc_creation_date',
        'accessible_flag': 'accessible_flag',
        'start_date': 'start_date',
        'end_date': 'end_date',
    }

    for sql_field, yaml_field in field_map.items():
        if sql_field in data and data[sql_field] is not None:
            result[yaml_field] = data[sql_field]

    if data.get('doc_metadata'):
        result['doc_metadata'] = data['doc_metadata']

    return result


class YamlDumper(yaml.SafeDumper):
    """Custom YAML dumper for better formatting."""
    pass


def str_representer(dumper, data):
    """Use literal style for multi-line strings."""
    if '\n' in data:
        return dumper.represent_scalar('tag:yaml.org,2002:str', data, style='|')
    return dumper.represent_scalar('tag:yaml.org,2002:str', data)


YamlDumper.add_representer(str, str_representer)


def main():
    # Read data.sql
    data_sql_path = Path(__file__).parent.parent / 'data.sql'
    print(f"Reading {data_sql_path}...")

    with open(data_sql_path, 'r', encoding='utf-8') as f:
        sql_content = f.read()

    # Split into statements
    statements = split_sql_statements(sql_content)
    print(f"Found {len(statements)} INSERT statements")

    # Parse each statement
    templates = []
    documents = []

    for stmt in statements:
        parsed = parse_insert_statement(stmt)
        if not parsed:
            continue

        table = parsed.get('_table', '')

        if 'master_template_definition' in table:
            templates.append(convert_template_to_yaml(parsed))
        elif 'storage_index' in table:
            documents.append(convert_document_to_yaml(parsed))

    print(f"Parsed {len(templates)} templates, {len(documents)} documents")

    # Write templates.yaml
    templates_data = {
        'defaults': {
            'template_version': 1,
            'active_flag': True,
            'language_code': 'EN_US',
            'created_by': 'system',
            'communication_type': 'LETTER',
        },
        'templates': templates
    }

    with open('templates.yaml', 'w', encoding='utf-8') as f:
        f.write("# =============================================================================\n")
        f.write("# MASTER TEMPLATE DEFINITIONS\n")
        f.write("# =============================================================================\n")
        f.write("# Auto-generated from data.sql - Review and clean up as needed\n")
        f.write("# =============================================================================\n\n")
        yaml.dump(templates_data, f, Dumper=YamlDumper, default_flow_style=False,
                  allow_unicode=True, sort_keys=False, width=120)

    print(f"Written templates.yaml ({len(templates)} templates)")

    # Write documents.yaml
    documents_data = {
        'documents': documents
    }

    with open('documents.yaml', 'w', encoding='utf-8') as f:
        f.write("# =============================================================================\n")
        f.write("# STORAGE INDEX (DOCUMENTS)\n")
        f.write("# =============================================================================\n")
        f.write("# Auto-generated from data.sql - Review and clean up as needed\n")
        f.write("# =============================================================================\n\n")
        yaml.dump(documents_data, f, Dumper=YamlDumper, default_flow_style=False,
                  allow_unicode=True, sort_keys=False, width=120)

    print(f"Written documents.yaml ({len(documents)} documents)")
    print("\nDone! Review the generated YAML files and clean up as needed.")


if __name__ == '__main__':
    main()
