# Template Creation & Publishing Flow - Sequence Diagram

**Note:** Authentication is handled by the API Gateway before requests reach the API.

This sequence diagram illustrates the complete template lifecycle including creation, rule management, versioning, and publishing in the Document Hub API.

## Mermaid Sequence Diagram - Template Creation

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Client
    participant API as Document Hub API
    participant TemplateService as Template<br/>Service
    participant RuleService as Rule<br/>Service
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: Create New Template with Rules

    Admin->>Client: Create New Template<br/>(form input)
    Client->>+API: POST /templates<br/>Authorization: Bearer <token><br/>{template_code, name, type,<br/>category, rules[]}

    API->>API: Check Permissions<br/>(requires: template:create)

    alt Insufficient Permissions
        API-->>Client: 403 Forbidden<br/>{error: "INSUFFICIENT_PERMISSIONS"}
        Client-->>Admin: Access Denied
    end

    API->>API: Validate Request Body<br/>- template_code: 3-100 chars<br/>- template_name: required<br/>- document_type: required<br/>- rules: valid expressions

    alt Validation Failed
        API-->>Client: 400 Bad Request<br/>{error: "VALIDATION_ERROR",<br/>details: [...]}
        Client-->>Admin: Show Validation Errors
    end

    API->>+TemplateService: Create Template
    TemplateService->>+DB: Check if template_code exists<br/>SELECT FROM templates<br/>WHERE template_code = ?

    alt Template Code Already Exists
        DB-->>TemplateService: Template Exists
        TemplateService-->>API: Duplicate Template Code
        API-->>Client: 409 Conflict<br/>{error: "DUPLICATE_TEMPLATE_CODE"}
        Client-->>Admin: Template Code Taken
    end

    DB-->>-TemplateService: No Conflict

    TemplateService->>+DB: BEGIN TRANSACTION

    TemplateService->>DB: INSERT INTO templates<br/>(template_id, template_code,<br/>template_name, document_type,<br/>status='draft', version_number=1,<br/>configuration, created_by)
    DB-->>TemplateService: Template Created<br/>(template_id)

    loop For Each Rule in rules[]
        TemplateService->>+RuleService: Create Rule
        RuleService->>RuleService: Validate Rule Expression<br/>Parse & Check Syntax
        RuleService->>DB: INSERT INTO template_rules<br/>(rule_id, template_id,<br/>rule_name, rule_expression,<br/>execution_order, severity)
        DB-->>RuleService: Rule Created
        RuleService-->>-TemplateService: Rule Added
    end

    TemplateService->>DB: COMMIT TRANSACTION
    DB-->>-TemplateService: Transaction Success

    TemplateService-->>-API: Template Created<br/>(template_id, rules_count)

    API-->>-Client: 201 Created<br/>{template_id, template_code,<br/>version_number: 1,<br/>status: "draft",<br/>rules_count: 5,<br/>created_at}

    Client-->>Admin: Template Created Successfully<br/>(Status: Draft)

    Note over Admin,DB: Template in Draft Status
```

## Mermaid Sequence Diagram - Template Publishing

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Client
    participant API as Document Hub API
    participant TemplateService as Template<br/>Service
    participant VersionService as Version<br/>Service
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: Publish Draft Template

    Admin->>Client: Publish Template<br/>(make active)
    Client->>+API: POST /templates/{templateId}/publish<br/>Authorization: Bearer <token><br/>{create_new_version: true,<br/>deprecate_previous: true}

    API->>API: Check Permissions<br/>(requires: template:publish)

    API->>+TemplateService: Get Template
    TemplateService->>+DB: SELECT FROM templates<br/>WHERE template_id = ?
    DB-->>-TemplateService: Template Record<br/>(status: "draft")

    alt Template Not Found
        TemplateService-->>API: Not Found
        API-->>Client: 404 Not Found
        Client-->>Admin: Template Not Found
    end

    alt Template Not in Draft Status
        TemplateService-->>API: Invalid Status
        API-->>Client: 400 Bad Request<br/>{error: "TEMPLATE_NOT_DRAFT"}
        Client-->>Admin: Can Only Publish Draft Templates
    end

    TemplateService->>+VersionService: Check for Active Version<br/>(same template_code)
    VersionService->>+DB: SELECT FROM templates<br/>WHERE template_code = ?<br/>AND status = 'active'
    DB-->>-VersionService: Active Version Found<br/>(version_number: 3)

    alt No Active Version (First Publish)
        VersionService-->>TemplateService: No Active Version
        Note over TemplateService: Simply activate the draft
    end

    Note over VersionService,DB: Active Version Exists - Create New Version

    VersionService->>+DB: BEGIN TRANSACTION

    alt deprecate_previous = true
        VersionService->>DB: UPDATE templates<br/>SET status = 'deprecated'<br/>WHERE template_code = ?<br/>AND status = 'active'
        DB-->>VersionService: Previous Version Deprecated
    end

    VersionService->>DB: UPDATE templates<br/>SET status = 'active',<br/>published_at = NOW(),<br/>published_by = ?<br/>WHERE template_id = ?
    DB-->>VersionService: Template Published

    VersionService->>DB: INSERT INTO template_version_history<br/>(template_id, version_number,<br/>status_change, changed_by)
    DB-->>VersionService: History Recorded

    VersionService->>DB: COMMIT TRANSACTION
    DB-->>-VersionService: Transaction Success

    VersionService-->>-TemplateService: Version Published<br/>(new_version: 4,<br/>previous_deprecated: true)

    TemplateService-->>-API: Publish Success<br/>(template details)

    API-->>-Client: 200 OK<br/>{template_id, template_code,<br/>version_number: 4,<br/>status: "active",<br/>previous_version: {<br/>  template_id,<br/>  version_number: 3,<br/>  status: "deprecated"<br/>},<br/>published_at, published_by}

    Client-->>Admin: Template Published<br/>(Version 4 Active)

    Note over Admin,DB: Template Now Active for Document Uploads
```

## Mermaid Sequence Diagram - Template Update & Versioning

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client as Admin Client
    participant API as Document Hub API
    participant TemplateService as Template<br/>Service
    participant VersionService as Version<br/>Service
    participant DB as PostgreSQL<br/>Database

    Note over Admin,DB: Update Active Template (Creates New Version)

    Admin->>Client: Edit Template Settings
    Client->>+API: PUT /templates/{templateId}<br/>{template_name: "Updated Name",<br/>configuration: {...}}

    API->>+TemplateService: Get Template
    TemplateService->>+DB: SELECT FROM templates<br/>WHERE template_id = ?
    DB-->>-TemplateService: Template<br/>(status: "active", version: 3)

    alt Template is Active
        Note over TemplateService,VersionService: Active templates create new version on update

        TemplateService->>+VersionService: Create New Draft Version
        VersionService->>+DB: BEGIN TRANSACTION

        VersionService->>DB: Clone template to new version<br/>INSERT INTO templates<br/>(SELECT * with new template_id,<br/>version_number = 4,<br/>status = 'draft',<br/>updated fields)
        DB-->>VersionService: New Draft Created<br/>(new_template_id, version: 4)

        VersionService->>DB: Copy all rules to new version<br/>INSERT INTO template_rules<br/>(SELECT * with new rule_ids,<br/>new template_id)
        DB-->>VersionService: Rules Copied

        VersionService->>DB: COMMIT TRANSACTION
        DB-->>-VersionService: Version Created

        VersionService-->>-TemplateService: New Draft Version<br/>(version 4)
    end

    alt Template is Draft
        Note over TemplateService: Draft templates update in-place
        TemplateService->>DB: UPDATE templates<br/>SET updated fields<br/>WHERE template_id = ?
        DB-->>TemplateService: Updated
    end

    TemplateService-->>-API: Template Updated

    API-->>-Client: 200 OK<br/>{template_id, version_number: 4,<br/>status: "draft",<br/>template_name: "Updated Name",<br/>message: "New draft version created"}

    Client-->>Admin: New Draft Version Created<br/>(Must publish to activate)

    Note over Admin,DB: Original Version 3 Still Active Until New Version Published
```

## Flow Descriptions

### Template Creation Flow

1. **Request & Authorization** (Step 1)
   - Admin submits new template with rules
   - API Gateway validates JWT token and checks for `template:create` permission before request reaches API
   - If unauthorized → 403 Forbidden

2. **Request Validation** (Steps 2-3)
   - Validate all required fields
   - Check template_code format (3-100 chars)
   - Validate rule expressions
   - If invalid → 400 Bad Request

3. **Duplicate Check** (Steps 4-6)
   - Check if template_code already exists
   - If exists → 409 Conflict

4. **Template Creation** (Steps 7-10)
   - Begin database transaction
   - Create template record with `status='draft'` and `version_number=1`
   - Insert all rules with execution order
   - Commit transaction

5. **Response** (Steps 11-12)
   - Return 201 Created with template details
   - Template remains in draft status

### Template Publishing Flow

1. **Authorization & Retrieval** (Step 1)
   - Admin requests to publish template
   - API Gateway validates permissions before request reaches API
   - Retrieve template from database
   - Verify template is in draft status

2. **Version Check** (Steps 2-4)
   - Check if an active version exists for this template_code
   - If no active version, simply activate the draft
   - If active version exists, proceed with versioning

3. **Version Management** (Steps 5-10)
   - Begin transaction
   - Optionally deprecate previous active version
   - Update draft template to `status='active'`
   - Record version history
   - Commit transaction

4. **Response** (Steps 11-13)
   - Return published template details
   - Include information about previous version
   - Template now active for document uploads

### Template Update Flow

1. **Update Active Template** (Step 1)
   - Admin updates an active template
   - System detects template is active

2. **Create New Draft Version** (Steps 2-7)
   - Clone template to new version (version_number + 1)
   - New version has `status='draft'`
   - Copy all rules to new version
   - Apply updates to the new draft

3. **Preserve Active Version**
   - Original active version remains unchanged
   - New draft must be published separately
   - Zero-downtime updates

4. **Update Draft Template**
   - If template is already draft, update in-place
   - No versioning needed for drafts

## API Endpoint Details

### Create Template

```
POST /api/v1/templates
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "template_code": "LOAN_APPLICATION",
  "template_name": "Loan Application Form",
  "description": "Standard loan application template",
  "document_type": "APPLICATION",
  "document_category": "LENDING",
  "retention_period_days": 2555,
  "requires_signature": true,
  "configuration": {
    "max_file_size_mb": 10,
    "allowed_extensions": ["pdf", "docx"]
  },
  "rules": [
    {
      "rule_name": "Validate File Size",
      "rule_type": "validation",
      "rule_expression": "file_size_bytes <= 10485760",
      "execution_order": 1,
      "severity": "error",
      "error_message": "File size must not exceed 10 MB"
    }
  ]
}
```

**Success Response (201 Created):**
```json
{
  "template_id": "550e8400-e29b-41d4-a716-446655440000",
  "template_code": "LOAN_APPLICATION",
  "version_number": 1,
  "template_name": "Loan Application Form",
  "status": "draft",
  "rules_count": 5,
  "created_at": "2024-11-01T10:30:00Z",
  "created_by": "admin@example.com"
}
```

### Publish Template

```
POST /api/v1/templates/{templateId}/publish
Authorization: Bearer <token>
Content-Type: application/json

Request Body (Optional):
{
  "create_new_version": true,
  "deprecate_previous": true
}
```

**Success Response (200 OK):**
```json
{
  "template_id": "990e8400-e29b-41d4-a716-446655440010",
  "template_code": "LOAN_APPLICATION",
  "version_number": 4,
  "status": "active",
  "previous_version": {
    "template_id": "550e8400-e29b-41d4-a716-446655440000",
    "version_number": 3,
    "status": "deprecated"
  },
  "published_at": "2024-11-01T11:15:00Z",
  "published_by": "admin@example.com"
}
```

### Update Template

```
PUT /api/v1/templates/{templateId}
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "template_name": "Updated Loan Application Form",
  "description": "Updated description",
  "configuration": {
    "max_file_size_mb": 15
  }
}
```

**Success Response (200 OK) - Active Template:**
```json
{
  "template_id": "new-template-id-uuid",
  "template_code": "LOAN_APPLICATION",
  "version_number": 4,
  "template_name": "Updated Loan Application Form",
  "status": "draft",
  "message": "New draft version created. Original version remains active until new version is published.",
  "updated_at": "2024-11-01T11:00:00Z"
}
```

## Template Versioning Strategy

### Version States
1. **Draft** - Being edited, not used for documents
2. **Active** - Currently used for document uploads
3. **Deprecated** - No longer active, but documents may reference it

### Versioning Rules
- Only one **active** version per `template_code`
- Multiple **deprecated** versions can exist
- Multiple **draft** versions can exist (rare)
- Documents always reference `template_id` (specific version)

### Version Lifecycle
```
Create → Draft (v1)
         ↓ Publish
      Active (v1)
         ↓ Update
      Active (v1) + Draft (v2)
         ↓ Publish v2
      Deprecated (v1) + Active (v2)
         ↓ Update
      Deprecated (v1) + Active (v2) + Draft (v3)
```

## Error Scenarios

| Scenario | HTTP Status | Error Code | Action |
|----------|-------------|------------|--------|
| Authentication (401 Unauthorized) | 401 | UNAUTHORIZED | Handled by API Gateway; not shown in this diagram |
| Insufficient permissions | 403 | INSUFFICIENT_PERMISSIONS | Need admin role |
| Invalid request body | 400 | VALIDATION_ERROR | Fix validation errors |
| Duplicate template_code | 409 | DUPLICATE_TEMPLATE_CODE | Use different code |
| Template not found | 404 | TEMPLATE_NOT_FOUND | Check template ID |
| Template not draft | 400 | TEMPLATE_NOT_DRAFT | Only drafts can be published |
| Template in use | 409 | TEMPLATE_IN_USE | Cannot delete |

## Best Practices

1. **Always Test in Draft**
   - Create template as draft
   - Test validation rules
   - Publish only when ready

2. **Version Control**
   - Use semantic versioning in descriptions
   - Document changes between versions
   - Never delete deprecated versions

3. **Rule Management**
   - Order rules by execution priority
   - Test rule expressions before publishing
   - Use clear error messages

4. **Zero-Downtime Updates**
   - Update creates new draft version
   - Original version stays active
   - Publish new version when ready
   - No interruption to document uploads
