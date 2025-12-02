You are assisting in building a Proof of Concept (POC) for a Document Hub service.
Use the following project structure and artifacts:

Project Files

`./poc/doc-hub.yaml` → OpenAPI 3 specification
`./poc/database.sql` → DB schema for document storage. This is the approved database design.

Project Location

`/poc/doc-hub-poc/`

Current Status

A project was generated using OpenAPI Codegen based on doc-hub.yaml.

Basic scaffolding exists (models, controllers, API interfaces).

🎯 Objective

Implement a fully functional POC for the `/documents-enquire` endpoint.

📝 Endpoint Description

The `/documents-enquire` endpoint must:

Return documents that belong to a specific account and shared documents.

Shared documents can be of different types depending on the rules.

**Document Types**

Documents are classified using the is_shared flag:

✔ 1. Account-Specific Documents

`is_shared = false`

Retrieved using `account_id`.

✔ 2. Shared Documents (`is_shared` = true)

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

My goal is to build a customizable data and rule evaluation engine that can be easily configured by non-technical team.

Technical stack
- Java 17
- Spring Boot 
- WebFlux