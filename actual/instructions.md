Work on the documents-enquiry endpoint that will fetch documents that are specific to a account and shared documents.
-  We have three tables:
	- master_template_definition (Master template data that will store template information of documents)
	- template_vendor_mapping (Vendor specific mapping for generating document. This is going to be future use)
	- storage_index (Stores the actual document related data)
Endpoint Features:	
- This endpoint returns accountId specific documents and
- Also this endpoint also determine if any shared documents need to be picked.
	- master_template_definition.isSharedDocument flag determines if document is shared or not.
	- Picking is done by the metadata present in template related data.
		There are different sharing scope:
			- All
			- All_accounts
			- digital_bank_customer_only
			- enterprise_customer_only
			- custom_rule
