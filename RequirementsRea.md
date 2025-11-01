Prompt: Design a Hybrid PostgreSQL Schema for Document Hub (Denormalized Template Versioning)

Context:

We are designing a Document Hub system that acts as an indexing and fast retrieval layer over an existing Enterprise Content Management System (ECMS).

Existing ECMS System

Stores document files in Amazon S3.

Maintains a database tracking document metadata for bank customers.

Provides REST APIs to fetch, store, update, delete, and search files or metadata.

Returns a temporary S3 link for file retrieval (view/download).

New System — Document Hub

Purpose:
Acts as a metadata index and retrieval layer for documents stored in ECMS. It will not store files, but will:

Store document metadata, including identifiers from ECMS.

There could be few document associated to document category/document-type like customer notices, privacy policies, change of agreement that will be have be sent/associated to all or certain customers/account. We should store one copy of document in the document table the there should be an indicator that might say that it's a shared document. When we extract the data from table we should be able to query both customer specific and shared documents which apply to this customer at that given timeline. 

Template should also dictate the blueprint of how the documents would be stored, Is it shared document or is it customer or account specific.

Support high-performance queries by customerId, accountId, documentType, documentCategory, and templateId.

Map documents to templates, which define structure and associated business rules.
Serve as the entry point for uploads:

File uploaded → Hub forwards to ECMS → ECMS returns ecms_document_id → Hub stores metadata and mapping.

Core Design Principles

Hybrid Normalization Approach

Documents and their key metadata are denormalized for query speed (customerId, accountId, documentType, documentCategory, etc.).

Templates and template rules are normalized to allow reusability and flexible rule management.

Template versioning is denormalized, meaning each new version of a template is stored as a new row in the same table rather than in a separate version table.

Scalability

Must handle millions of document records efficiently.

Supports indexing and partitioning strategies (likely partitioned by customer_id or account_id).

Audit & Tracking

Every table captures created_at, updated_at, and created_by fields.

Schema Requirements

Design a PostgreSQL schema that includes the following entities and relationships:

Documents (denormalized)

Stores all searchable metadata (customerId, accountId, documentType, documentCategory, templateId, ecms_document_id, etc.)

Supports flexible filtering and high-performance retrieval.

Templates (denormalized versioning)

Each row represents a version of a template.

Includes version number, status (active/deprecated), document type, and category.

Linked to TemplateRules (normalized).

TemplateRules

Defines reusable rules associated with templates.

Includes rule name, expression/condition, and scope.

Audit/Tracking

Maintain timestamps and user actions across all tables.

Key Relationships

One Template → Many Documents

One Template → Many TemplateRules

Template Versioning: Each template version stored as a new row (same template_code, incremented version_number).

Documents linked to a specific template version.

Ask:

Generate a PostgreSQL schema (DDL) that includes:

Table definitions for:

documents

templates

template_rules

Appropriate primary keys, foreign keys, and constraints.

Indexing strategy (including composite indexes).

Partitioning strategy suggestions (e.g., by customer_id or account_id).

Sample SQL queries for frequent retrievals:

Fetch by customerId + documentType

Fetch by accountId + documentCategory

Fetch all documents linked to a given template_code or template_version

Also include:

Explanation of denormalized versioning logic (why stored in same table).

Optional: suggestions for caching (e.g., Redis or materialized views).