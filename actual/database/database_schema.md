Column Name	Column Type	Description	Examples
template_id	UUID	ID for master template	9a4cda5b-12b5-5e03-822a-7d33af73b023
version	int	Template Version	
legacy_template_id	varchar	Legacy template ID	
template_name	varchar	Unique name for the template	
description	varchar	Stores the description	
line_of_business	varchar	Stores document belongs to which line of business. 	credit_card|enterprise|digital_banking
category	varchar	Stores the document category which it belongs to	"1. Statements
2. General Communications
3. Payments Confirmation Notice
4. Credit Line Increase
5. Change In Terms Notice
6. Annual Fee Notice
7. Privacy Policy
8. Electronic Disclosure and Communications Consent Agreement
9. Cardholder Agreement
10. Credit Protection
11. Balance Transfers
12. Disputed Transactions"
doc_type	varchar	Stores the document type	monthly_statement, disclosure, electronic_disclosure,SV0019
language_code	varchar	Language code	en_us, esp. etc
owning_dept	varchar	Department owner	Accounts|Payments|Marketing
notification_needed	bit	Stores information on if this needs and event to be published for notification	
doc_supporting_data	jsonb	Stores semi-structured configuration details for managing digital documents, such as security rules, data validation, and archival policies. This allows for flexibility and future changes to the configuration without requiring a database schema modification.	"{
    ""access_control"": [
        {
            ""role"": ""admin"",
            ""actions"": [
                ""View"",
                ""Update"",
                ""Delete""
            ]
        },
        {
            ""role"": ""backOffice"",
            ""actions"": [
                ""View"",
                ""Download""
            ]
        },
        {
            ""role"": ""customer"",
            ""actions"": [
                ""View"",
                ""Download""
            ]
        },
        {
            ""role"": ""agent"",
            ""actions"": [
                ""View""
            ]
        }
    ],
    ""required_fields"": [
        {
            ""field"": ""pricingCode"",
            ""type"": ""string"",
            ""required"": true
        },
        {
            ""field"": ""disclosureVersion"",
            ""type"": ""integer"",
            ""required"": true
        },
        {
            ""field"": ""effectiveDate"",
            ""type"": ""Date"",
            ""required"": true
        },
        {
            ""field"": ""disclosureCode"",
            ""type"": ""string"",
            ""required"": true
        }
    ],
""retention_policy"":""365""

}"
is_regulatory	bit	Flag indicating if the template is regulatory or not.	
is_message_center_doc	bit	Flag indicating if the document is displayed in message center or not	
document_channel	jsonb	Different delivery channels	Print, email, web, mobile, SMS
template_variables	jsonb	Variables and their API source required to generate the document	
template_status	enum	Status of template	Draft/Approved/Rejected/Pending
effective_date	long	Date from which this version is active	
valid_until	long	Date at which this version is discontinued	
created_by	varchar	The user or process that created the record. (DBA Required column)	
created_timestamp	timestamp	The date and time when the record was initially created. (DBA Required column)	
update_by	varchar	The user or process that last modified the record. (DBA Required column)	
updated_timestamp	timestamp 	The date and time when the record was last modified. (DBA Required column)	
archive_indicator	bit	A flag indicating if the record has been archived. (DBA Required column)	
archive_timestamp	timestamp	The date and time when the record was marked as archived. (DBA Required column)	
version_number	bigint	A numeric value that is incremented with each change to the record. (DBA Required column)	
record_status	bigint	(DBA Required column)	
isSharedDocument	bit	Flag to indicate if the documents of this kind is shared	
sharing_scope	varchar	"Scope of sharing: all, credit_card_accounts_only,
 digital_bank_customer_only, enterprise_customer_only, custom_rule."	
data_extraction_schema	varchar	Data extraction configuration	
<img width="822" height="1667" alt="image" src="https://github.com/user-attachments/assets/e372d4b1-127e-495d-a639-7658713cb48d" />


Column Name	Column Type	Description	Examples
storage_index_id	UUID	Id representing the index id (index key)	3b70305e-1aae-4059-8ec0-95a71a67a63a
template_id	UUID	FK Relation to the master_template_defination table (On which document has been uploaded)	9a4cda5b-12b5-5e03-822a-7d33af73b023
doc_type	varchar	Stores the document type	monthly_statement, disclosure, electronic_disclosure,SV0019
storage_vendor	varchar	Storage solution where the document is stored. (ECMS)	ECMS
reference_key	varchar	stores id/value of key type, can be any type. 	
reference_key_type	varchar	Used to store what type of key this in above column (reference_key)	(accountid, disclosure_code, threadid,corelationid)
account_key	UUID	AccountId to which this doc belongs	
customer_key	UUID	Customer id to which this doc belongs	
storage_document_key	UUID	Key to be retrived from ECMS	
file_name	varchar	File name of the document	
doc_creation_date	long	Epoch date time 	
is_accessible	bit	Stores if document is still active or archived	
last_referenced	long	Last referenced datetime	
time_referenced	int	Counter of how many times document is viewed	
doc_info	jsonb	Extra information related to document, (cycle date for statement, statement_id, disclosure_startdate)	
created_by	varchar	The user or process that created the record. (DBA Required column)	
created_timestamp	timestamp	The date and time when the record was initially created. (DBA Required column)	
update_by	varchar	The user or process that last modified the record. (DBA Required column)	
updated_timestamp	timestamp 	The date and time when the record was last modified. (DBA Required column)	
archive_indicator	bit	A flag indicating if the record has been archived. (DBA Required column)	
archive_timestamp	timestamp	The date and time when the record was marked as archived. (DBA Required column)	
version_number	bigint	A numeric value that is incremented with each change to the record. (DBA Required column)	
record_status	bigint	(DBA Required column)	
<img width="1032" height="721" alt="image" src="https://github.com/user-attachments/assets/102ddd85-ff94-45ec-ae94-9768f05fe8db" />


Column Name	Column Type	Description	Examples
template_vendor_id	UUID	Unique Identifier for tempate generation vendor mapping	
template_id	UUID	FK from template definition	
vendor	enum	SmartComm|Assentis|Handlebar	
vendor_template_key	varchar	Vendor Specific template ID/key	
reference_key_type	varchar	Used to store what type of key this in above column	
consumer_id	UUID	Used by consumers reading from Kafka	
template_content	blob	The raw content of the template(e.g., HTML, markdown, JSON) or the link to ECMS.	
effective_date	long	Date from which this version is active	
valid_until	long	Expiry Date	
version	int	Version	
is_primary	bit	Flag Active/Inactive	
schema_info	jsonb	Schema variables for given template	
api_endpoint	varchar(max)	Vendor related API endpoint	
is_active	bit	Indicates template version is active or not 	
created_by	varchar	The user or process that created the record. (DBA Required column)	
created_timestamp	timestamp	The date and time when the record was initially created. (DBA Required column)	
update_by	varchar	The user or process that last modified the record. (DBA Required column)	
updated_timestamp	timestamp 	The date and time when the record was last modified. (DBA Required column)	
archive_indicator	bit	A flag indicating if the record has been archived. (DBA Required column)	
archive_timestamp	timestamp	The date and time when the record was marked as archived. (DBA Required column)	
version_number	bigint	A numeric value that is incremented with each change to the record. (DBA Required column)	
record_status	bigint	(DBA Required column)	
<img width="790" height="621" alt="image" src="https://github.com/user-attachments/assets/868c8d4e-f092-4d55-8ae0-7d4e31f0e4e1" />

