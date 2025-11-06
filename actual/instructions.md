## Requirements
Develop the /documents-enquiry endpoint to retrieve all document records associated with a specific account, as well as any shared documents available under the applicable sharing scope.
Endpoint features:
## Data Model Overview
We have the following core tables:
1. master_template_definition: Stores master metadata and configuration details of each document template.
2. template_vendor_mapping: Maintains vendor-specific configurations for document generation (for future use).
3. storage_index: Stores actual document-related data and indexing information.
Refer to database_schema.md for table details.

## Endpoint definition:
	POST /documents-enquiry: Other details are in the schema.yml file.

## Endpoint Features
### Account-Specific Documents

- Fetch all documents from storage_index where account_id = :accountId.
- Join with master_template_definition to enrich metadata (template name, description, etc.).

### Shared Documents

- Identify templates from master_template_definition where isSharedDocument = true.
- Evaluate sharing_scope:
	- All → Include for everyone.
	- credit_card_account_only → Include for credit card account holders.
	- digital_bank_customer_only → Include only if the accountId belongs to digital banking customers.
	- enterprise_customer_only → Include only if the accountId belongs to enterprise accounts.
	- custom_rule → Apply rules from metadata (e.g., region-based or account-type based).
  		- For custom rules, we have to look at the extractor logic that will be associated with each category+doc_type in the master table.
    	- Based on the logic, we need to determine if we need to pick a document for this accountId/customer or not.
- Merge the shared documents with account-specific ones, avoiding duplicates.

