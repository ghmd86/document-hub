# Template Management API Specification Review

**File Reviewed:** `docs/api-specs/document_template.yml`
**Version:** 0.0.4
**Review Date:** 2026-01-05

## Summary

The specification defines a template management system with versioning and vendor mapping capabilities. The API covers:
- Template CRUD operations with version support
- Template-vendor mapping management
- Pagination and filtering

---

## Critical Issues

### 1. Invalid Property Name (Line 763)

**Issue:** Space in property name is invalid YAML/JSON.

```yaml
# Current (INVALID)
notification required:
  type: boolean

# Should be
notificationRequired:
  type: boolean
```

### 2. Path Parameter Mismatch

**Issue:** Paths use `{templateId}` but parameter definitions use `masterTemplateId`.

| Location | Path Variable | Parameter Name |
|----------|---------------|----------------|
| Line 139 | `{templateId}` | - |
| Line 584-591 | - | `masterTemplateId` |

**Fix:** Either rename path variables to `{masterTemplateId}` or rename parameter to `templateId`.

### 3. Malformed Schema Nesting (Lines 1007-1043)

**Issue:** `DataSourceConfig` has incorrect YAML structure where `endpoint` is nested inside `description` property.

```yaml
# Current (INCORRECT)
DataSourceConfig:
  type: object
  properties:
    description:
      type: string
      endpoint:           # Incorrectly nested
        type: object
        properties:
          url:
            type: string

# Should be
DataSourceConfig:
  type: object
  properties:
    description:
      type: string
    endpoint:             # Sibling to description
      type: object
      properties:
        url:
          type: string
```

### 4. Inconsistent Path Naming (Lines 179 vs 217)

**Issue:** Path naming is inconsistent between endpoints.

```yaml
# Inconsistent paths
/templates/{templateId}/versions/{templateVersion}/   # plural + trailing slash
/templates/{templateId}/version/{templateVersion}     # singular, no trailing slash
```

**Recommendation:** Standardize to `/templates/{templateId}/versions/{templateVersion}` (plural, no trailing slash).

---

## Moderate Issues

### 5. HTTP Status Codes for POST Operations

**Issue:** Create operations return `200 OK` instead of `201 Created`.

| Endpoint | Current | Expected |
|----------|---------|----------|
| `POST /templates` | 200 | 201 |
| `POST /templates/vendors` | 200 | 201 |

### 6. Response Schema Inconsistencies

**Issue:** Page response schemas use different property names and structures.

```yaml
# TemplatePageResponse (Line 699-705)
TemplatePageResponse:
  properties:
    content:          # Array of templates
      type: array
      items:
        $ref: '#/components/schemas/TemplateResponse'

# TemplateVendorPageResponse (Line 1154-1160)
TemplateVendorPageResponse:
  properties:
    items:            # NOT an array - missing type declaration
        $ref: '#/components/schemas/TemplateVendorResponse'
```

**Fix:** Standardize to use `content` with proper array type:
```yaml
TemplateVendorPageResponse:
  properties:
    content:
      type: array
      items:
        $ref: '#/components/schemas/TemplateVendorResponse'
```

### 7. UUID Format Inconsistency

**Issue:** UUID format specified inconsistently.

| Location | Format Used |
|----------|-------------|
| Line 591 | `format: uuid` |
| Line 615 | `format: UUID` |

**Recommendation:** Use lowercase `uuid` consistently per OpenAPI specification.

### 8. Enum Values with Special Characters (Lines 748-752)

**Issue:** `templateType` enum contains values with spaces and parentheses which can cause code generation issues.

```yaml
templateType:
  enum:
    - monthly_statement
    - disclosure
    - electronic_disclosure
    - SV0019 (Indexed)    # Problematic for code generation
```

**Recommendation:** Use consistent snake_case or SCREAMING_SNAKE_CASE:
```yaml
templateType:
  enum:
    - monthly_statement
    - disclosure
    - electronic_disclosure
    - sv0019_indexed
```

---

## Minor Issues

### 9. Non-Standard Schema Properties (Lines 1052-1061)

**Issue:** `AccessControlEntry` contains `Roles:` and `Actions:` which are not valid OpenAPI schema properties.

```yaml
# Current
AccessControlEntry:
  type: object
  Roles:              # Not valid OpenAPI
    - customer: '...'
  Actions:            # Not valid OpenAPI
    - views: '...'
```

**Fix:** Move to `description` field as documentation.

### 10. Typos

| Line | Current | Corrected |
|------|---------|-----------|
| 762 | `oprations` | `operations` |
| 968 | `he promotional` | `The promotional` |

### 11. Missing Security Definitions

**Issue:** No `securitySchemes` defined despite authentication headers being used (`X-requestor-Id`, `X-requestor-type`).

**Recommendation:** Add security scheme definition:
```yaml
components:
  securitySchemes:
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-requestor-Id
```

### 12. Inconsistent Example Formats

**Issue:** Some examples use JSON strings while others use proper YAML structures.

```yaml
# String format (Line 872)
example: '[ { "type": "EMAIL", "enabled": true }]'

# YAML format (Lines 863-867)
example:
  - role: customer
    actions: [View, Download]
```

**Recommendation:** Use consistent YAML format for all examples.

---

## Summary Table

| Priority | Count | Issues |
|----------|-------|--------|
| Critical | 4 | Invalid property name, path mismatch, schema nesting, inconsistent paths |
| Moderate | 4 | HTTP status codes, response schemas, UUID format, enum values |
| Minor | 4 | Non-standard properties, typos, missing security, inconsistent examples |

---

## Recommended Actions

1. **Immediate:** Fix critical issues before implementation
2. **Before Development:** Address moderate issues to ensure consistent API behavior
3. **Before Release:** Clean up minor issues for production readiness
