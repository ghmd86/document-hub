#!/usr/bin/env python3
"""
Convert Shared Document Catalog Excel file to YAML format.

This script reads the Excel workbook and generates individual YAML files
for each shared document, following the catalog template structure.

Usage:
    python convert_excel_to_yaml.py input.xlsx output_directory/

Requirements:
    pip install pandas openpyxl pyyaml
"""

import pandas as pd
import yaml
import json
import sys
import os
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Any


class ExcelToYAMLConverter:
    """Convert Excel catalog to YAML files."""

    def __init__(self, excel_file: str, output_dir: str):
        """
        Initialize converter.

        Args:
            excel_file: Path to Excel workbook
            output_dir: Directory to write YAML files
        """
        self.excel_file = excel_file
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        # Load all sheets
        print(f"Loading Excel file: {excel_file}")
        self.sheets = pd.read_excel(
            excel_file,
            sheet_name=[
                'DOCUMENT_LIST',
                'DOCUMENT_DETAILS',
                'ELIGIBILITY_CONDITIONS',
                'DATA_SOURCES',
                'TEST_SCENARIOS'
            ],
            engine='openpyxl'
        )

        print("✓ Excel file loaded successfully")

    def convert_all(self):
        """Convert all documents to YAML."""
        doc_list = self.sheets['DOCUMENT_LIST']

        total = len(doc_list)
        print(f"\nConverting {total} documents to YAML...\n")

        for idx, row in doc_list.iterrows():
            doc_id = row['Document ID']
            print(f"[{idx + 1}/{total}] Converting {doc_id}: {row['Document Name']}")

            try:
                yaml_content = self.convert_document(doc_id)
                output_file = self.output_dir / f"{doc_id}.yaml"

                with open(output_file, 'w', encoding='utf-8') as f:
                    f.write(yaml_content)

                print(f"  ✓ Written to {output_file}")

            except Exception as e:
                print(f"  ✗ Error: {str(e)}")

        print(f"\n✓ Conversion complete. Files written to {self.output_dir}")

    def convert_document(self, doc_id: str) -> str:
        """
        Convert single document to YAML.

        Args:
            doc_id: Document ID to convert

        Returns:
            YAML string
        """
        # Get document details
        details = self._get_document_details(doc_id)

        # Build YAML structure
        doc = {
            'document_id': doc_id,
            'document_name': details['Document Name'],
            'document_type': details['Document Type'],
            'category': details['Category'],
            'line_of_business': details['Line of Business'],
            'regulatory': details['Regulatory'] == 'Yes',
            'owner_department': details['Owner Dept'],
        }

        # Add optional fields
        if pd.notna(details.get('Subcategory')):
            doc['subcategory'] = details['Subcategory']

        if details['Regulatory'] == 'Yes' and pd.notna(details.get('Regulation Name')):
            doc['regulation'] = details['Regulation Name']

        # Sharing configuration
        doc['sharing_scope'] = self._get_sharing_scope(doc_id)
        doc['effective_date'] = self._format_date(details.get('Effective Date'))

        if pd.notna(details.get('Valid Until')):
            doc['valid_until'] = self._format_date(details['Valid Until'])
        else:
            doc['valid_until'] = None

        # Eligibility rule (if custom_rule)
        if doc['sharing_scope'] == 'custom_rule':
            doc['eligibility_rule'] = self._build_eligibility_rule(doc_id)

        # Business context
        doc['business_justification'] = self._format_multiline(
            details.get('Business Justification', '')
        )
        doc['target_audience'] = details.get('Target Audience', '')

        if pd.notna(details.get('Expected Volume')):
            doc['expected_volume'] = int(details['Expected Volume'])

        # Test scenarios
        doc['test_scenarios'] = self._get_test_scenarios(doc_id)

        # Metadata
        doc['version'] = details.get('Version', 1.0)
        doc['created_by'] = details.get('Created By', '')
        doc['created_date'] = self._format_date(details.get('Created Date'))

        if pd.notna(details.get('Approved By')):
            doc['approved_by'] = details['Approved By']
            doc['approved_date'] = self._format_date(details.get('Approved Date'))

        doc['last_modified'] = self._format_date(details.get('Last Modified'))

        # Convert to YAML
        yaml_str = yaml.dump(
            doc,
            default_flow_style=False,
            sort_keys=False,
            allow_unicode=True,
            indent=2
        )

        # Add document separator
        yaml_str = "---\n" + yaml_str + "---\n"

        return yaml_str

    def _get_document_details(self, doc_id: str) -> Dict:
        """Get document details row."""
        details_df = self.sheets['DOCUMENT_DETAILS']
        row = details_df[details_df['Document ID'] == doc_id]

        if row.empty:
            raise ValueError(f"Document {doc_id} not found in DOCUMENT_DETAILS")

        return row.iloc[0].to_dict()

    def _get_sharing_scope(self, doc_id: str) -> str:
        """Get sharing scope from DOCUMENT_LIST."""
        doc_list = self.sheets['DOCUMENT_LIST']
        row = doc_list[doc_list['Document ID'] == doc_id]

        if row.empty:
            return 'custom_rule'  # default

        return row.iloc[0]['Sharing Scope']

    def _build_eligibility_rule(self, doc_id: str) -> Dict:
        """Build eligibility rule from conditions and data sources."""
        conditions_df = self.sheets['ELIGIBILITY_CONDITIONS']
        data_sources_df = self.sheets['DATA_SOURCES']

        # Get conditions for this document
        doc_conditions = conditions_df[conditions_df['Document ID'] == doc_id]

        if doc_conditions.empty:
            return {}

        rule = {
            'rule_name': f"{doc_id.lower()}_eligibility",
            'rule_type': 'composite',  # Can be overridden manually
            'logic_operator': 'AND',   # Default, can be overridden
        }

        # Build data sources
        data_sources = []
        unique_sources = doc_conditions['Data Source'].unique()

        for source_id in unique_sources:
            if pd.isna(source_id):
                continue

            source_rows = data_sources_df[data_sources_df['Data Source ID'] == source_id]

            if not source_rows.empty:
                source_info = source_rows.iloc[0]

                data_source = {
                    'source_id': source_id,
                    'source_type': source_info.get('Source Type', 'REST_API'),
                }

                if source_info.get('Source Type') == 'REST_API':
                    data_source['endpoint_config'] = {
                        'url': source_info.get('API Endpoint', ''),
                        'method': source_info.get('HTTP Method', 'GET'),
                        'timeout_ms': int(source_info.get('Timeout (ms)', 5000))
                    }

                    if pd.notna(source_info.get('Request Body')):
                        try:
                            data_source['endpoint_config']['body_template'] = json.loads(
                                source_info['Request Body']
                            )
                        except:
                            pass

                # Build response mapping
                response_mapping = {}
                for _, row in source_rows.iterrows():
                    field_name = row.get('Field Name')
                    json_path = row.get('JSON Path')

                    if pd.notna(field_name) and pd.notna(json_path):
                        response_mapping[field_name] = json_path

                if response_mapping:
                    data_source['response_mapping'] = response_mapping

                data_sources.append(data_source)

        if data_sources:
            rule['data_sources'] = data_sources

        # Build conditions
        conditions = []
        for _, row in doc_conditions.iterrows():
            condition = {
                'field': row['Field Name'],
                'operator': row['Operator'],
            }

            # Parse value based on type
            value_type = row.get('Value Type', 'String')
            raw_value = row['Value']

            if value_type == 'Number':
                try:
                    condition['value'] = float(raw_value)
                except:
                    condition['value'] = raw_value
            elif value_type == 'Boolean':
                condition['value'] = str(raw_value).lower() in ['true', 'yes', '1']
            elif value_type == 'Array':
                try:
                    condition['value'] = json.loads(raw_value)
                except:
                    condition['value'] = [v.strip() for v in str(raw_value).split(',')]
            else:
                condition['value'] = raw_value

            # Logical operator
            if pd.notna(row.get('Logical Operator')):
                condition['logical_operator'] = row['Logical Operator']

            conditions.append(condition)

        if conditions:
            rule['conditions'] = conditions

        # Error handling
        rule['error_handling'] = {
            'on_api_failure': 'exclude',
            'fallback_result': False
        }

        return rule

    def _get_test_scenarios(self, doc_id: str) -> List[Dict]:
        """Get test scenarios for document."""
        scenarios_df = self.sheets['TEST_SCENARIOS']
        doc_scenarios = scenarios_df[scenarios_df['Document ID'] == doc_id]

        scenarios = []
        for _, row in doc_scenarios.iterrows():
            scenario = {
                'scenario': row.get('Scenario Name', ''),
                'scenario_type': row.get('Scenario Type', 'Positive'),
            }

            # Parse test data JSON
            if pd.notna(row.get('Test Data (JSON)')):
                try:
                    scenario['test_data'] = json.loads(row['Test Data (JSON)'])
                except:
                    scenario['test_data'] = row['Test Data (JSON)']

            scenario['expected_result'] = row.get('Expected Result', '') == 'SHOW'
            scenario['reason'] = row.get('Reason', '')

            if pd.notna(row.get('Validation Steps')):
                # Split multi-line validation steps
                steps = str(row['Validation Steps']).split('\n')
                scenario['validation_steps'] = [s.strip() for s in steps if s.strip()]

            scenarios.append(scenario)

        return scenarios

    def _format_date(self, date_val) -> str:
        """Format date to YYYY-MM-DD string."""
        if pd.isna(date_val):
            return None

        if isinstance(date_val, str):
            return date_val

        if isinstance(date_val, datetime):
            return date_val.strftime('%Y-%m-%d')

        return str(date_val)

    def _format_multiline(self, text: str) -> str:
        """Format multiline text for YAML literal style."""
        if pd.isna(text):
            return ''

        return str(text).strip()


def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print("Usage: python convert_excel_to_yaml.py <excel_file> [output_directory]")
        print()
        print("Example:")
        print("  python convert_excel_to_yaml.py Shared_Document_Catalog.xlsx output/")
        sys.exit(1)

    excel_file = sys.argv[1]
    output_dir = sys.argv[2] if len(sys.argv) > 2 else 'catalog_yaml_output'

    if not os.path.exists(excel_file):
        print(f"Error: Excel file not found: {excel_file}")
        sys.exit(1)

    print("=" * 80)
    print("  Shared Document Catalog: Excel → YAML Converter")
    print("=" * 80)
    print()

    try:
        converter = ExcelToYAMLConverter(excel_file, output_dir)
        converter.convert_all()

        print()
        print("=" * 80)
        print("  Conversion Complete!")
        print("=" * 80)
        print()
        print(f"YAML files created in: {output_dir}/")
        print()
        print("Next steps:")
        print("  1. Review generated YAML files")
        print("  2. Manually adjust rule_type and logic_operator if needed")
        print("  3. Validate against schema")
        print("  4. Import into database (data_extraction_schema column)")
        print()

    except Exception as e:
        print()
        print(f"Error: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
