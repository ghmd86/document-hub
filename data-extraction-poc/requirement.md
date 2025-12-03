You are assisting in building a Proof of Concept (POC) for a Document Hub service.
Use the following project structure and artifacts:

Project Files

`./document-hub-schema.yaml` → OpenAPI 3 specification
`./schema.sql` → DB schema for document storage. This is the approved database design.
`./effective-pom.xml` -> Effective pom.xml that is being used by the project.

Current Status

We did a poc under `../poc` folder for the same. 
We also did `../swagger-codegen-filepart-sample` project to generate code using OpenAPI specs.
We can take references from these two projects for building a fresh service.

🎯 Objective

Implement a fully functional POC for the `/documents-enquire` endpoint.
You can refer to the poc and swagger gen code to build a fresh working project to satisfy the requirement for above endpoint.

📝 Endpoint Description

The `/documents-enquire` endpoint must:

Return documents that belong to a specific account and shared documents.

Shared documents can be of different types depending on the rules.

**Document Types**

Documents are classified using the is_shared flag:

✔ 1. Account-Specific Documents

`shared_document_flag = false`

Retrieved using `account_id`.

✔ 2. Shared Documents (`shared_document_flag` = true)

Shared documents have subcategories:

**Shared with All**

- Accessible to all accounts regardless of type 
- Example: Terms and conditions v1, product brochures, compliance notices

**Shared with Specific Account Types**

- e.g., `credit_card`, `digital_bank`, `enterprise`, etc. 
- Must match the requesting account's type 
- Account type lookup needed (you may query customer/account table)

**Shared With Custom Rules**

Rules may include:

- disclosure_code match
- Customer’s region / country / state
- Customer segment (VIP, enterprise, etc.)
- Account tenure (e.g., > 1 year)
- Document validity date ranges
- Document versioning (fetch latest or active version only)

Data & Rule Evaluation

- Implement a rules evaluation layer:
  - match account metadata 
  - match document metadata 
  - combine conditions using AND/OR logic 
- If custom rule definitions exist in DB → read and evaluate dynamically.

Technical Requirement

- We would not cache any information as of now
- data_extraction_config jsonb column would be used to store the extraction information for shared documents

My goal is to build a customizable data and rule evaluation engine that can be easily configured by non-technical team.

Technical stack
- Java 17
- Spring Boot 
- WebFlux