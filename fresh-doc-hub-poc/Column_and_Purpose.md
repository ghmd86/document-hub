We have required field column named: `required_fields` which can be used to store the 
required fields that are needed
uploading and storing in `doc_meta` column in `storage_index` table. 
One such example how we can store these fields:

**Example 1:**
```json5
{
    "required_fields": [
        {"field": "issueDate", "type": "date", "required": true},
        {"field": "mimeType", "type": "string", "required": true},
        {"field": "account_Id", "type": "string", "required": true},
        {"field": "customer_id", "type": "string", "required": true},
        {"field": "statement_id", "type": "string", "required": true},
        {"field": "statement_month", "type": "string", "required": true},
        {
            "field": "statement_naming",
            "type": "string",
            "required": true,
            "format": "Satement {0-1} [4].pdf"
        }
    ]
}
```

**Example 2:**
```json5
{
    "required_fields": [
        {"field": "pricingCode",       "type": "string",  "required": true},
        {"field": "disclosureVersion", "type": "integer", "required": true},
        {"field": "effectiveDate",     "type": "Date",    "required": true},
        {"field": "disclosureCode",    "type": "string",  "required": true}
    ]
}

```

**Example 3:**

```json5
{
    "required_fields": [
        {"field": "customer_id",     "type": "string",  "required": true},
        {"field": "account_id",      "type": "integer", "required": true},
        {"field": "mimeType",        "type": "string",  "required": true},
        {"field": "template_engine", "type": "string",  "required": true}
    ]
}
```

We added this as part of `data_extraction_config` which we can separate.
When uploading a new document instance we need to make sure these things are stored in the storage index table.

**Access Control**
- I want to understand what we are doing with `access_control` column information? 
- Original purpose of this was to restrict the access to the document. When we implement it.
- I had envisioned the column content as following:
```json5
  [
      { "role": "admin", "actions": ["View","Update","Delete"] },
      { "role": "customer", "actions": ["View","Download"] },
      { "role": "agent", "actions": ["View"] },
      { "role": "backOffice", "actions": ["View","Update","Download"] },
      { "role": "system", "actions": ["View","Update","Delete","Download"] }
  ]
```
- Right now we would keep the column as empty and that indicates anyone can access it.
- In future if the required we might include the access part too.

**Channels** 

- Different delivery channels: eg: Print, Email, Web, Mobile, SMS. 
```json5
[
    { "type": "EMAIL", "enabled": true, "config": {"priority": "high"} },
    { "type": "PRINT", "enabled": true, "config": {"duplex": true}     },
    { "type": "WEB",   "enabled": true                                 }
]

```
**template_config**
- Configuration related to the template
***Example content***
```json5
{
  "reprint_policy": {"cooldown_period_days": 12},
  "print_policy"  : {"default": "{template_vendor_id}"}
}
```

**template_variables**
- Variables and their API source required to generate the document
- I would like to store variable source information like API and how we are going to extract the information from response.
- Give me ideas how we can achieve this?