# P1 - HATEOAS Links Access Control - Implementation Specification

**Created:** December 2024
**Priority:** P1 (High - Required for MVP)
**Related Items:** P1-003, P1-004, P1-005 from TODO Backlog

---

## Summary

Filter HATEOAS action links based on:
1. **Requestor Type** (`X-requestor-type` header): CUSTOMER, AGENT, SYSTEM
2. **Template Access Control** (`access_control` JSON column in template)

**John's Feedback (Day 1):**
- "You're just giving all the actions every time, which doesn't match what we have in our metadata for the template"
- "You should only be providing these items here that match that metadata"
- "There should be no delete function in the get inquiry" (for web/customer)
- "If you're going to give a link that expires, you need to let them know when it expires"

---

## Current State

### What We Have

1. **`X-requestor-type` header** - Already in API spec
   ```yaml
   X-requestor-type:
     type: string
     enum:
       - CUSTOMER
       - AGENT
       - SYSTEM
   ```

2. **`access_control` JSON column** - Already in `master_template_definition`
   ```json
   [
     { "role": "admin", "actions": ["View", "Update", "Delete", "Download"] },
     { "role": "customer", "actions": ["View", "Download"] },
     { "role": "agent", "actions": ["View", "Download"] },
     { "role": "backOffice", "actions": ["View", "Update", "Delete", "Download"] },
     { "role": "system", "actions": ["View", "Update", "Delete", "Download"] }
   ]
   ```

3. **`AccessControl` model** - Already implemented with `hasPermission()` and `getActionsForRole()` methods

### What's Missing

1. **Link filtering** - Response builder returns ALL links regardless of permissions
2. **Role mapping** - No mapping from `X-requestor-type` to `access_control` roles
3. **Link expiration** - No `expires_at` timestamp on download links
4. **Delete link removal** - Delete link always appears in enquiry response

---

## Technical Specification

### 1. Role Mapping

Map `X-requestor-type` header to access control roles:

| X-requestor-type | access_control role |
|------------------|---------------------|
| CUSTOMER | customer |
| AGENT | agent |
| SYSTEM | system |

**Additional implicit mappings:**
- Back office users would come through as `AGENT` with elevated permissions (future: separate header?)
- Admin access controlled separately (not through this API)

### 2. Update Response Builder

**File:** `src/main/java/com/documenthub/service/DocumentEnquiryService.java`

Current code (likely) returns all links:
```java
// WRONG: Returns all actions
Links links = Links.builder()
    .download(buildDownloadLink(document))
    .delete(buildDeleteLink(document))
    .build();
```

Updated code should filter based on permissions:
```java
private Links buildLinksForDocument(
    StorageIndexEntity document,
    MasterTemplateDefinitionEntity template,
    String requestorType  // From X-requestor-type header
) {
    // Get permitted actions for this requestor type
    List<String> permittedActions = getPermittedActions(template, requestorType);

    Links.LinksBuilder linksBuilder = Links.builder();

    // Only add download link if permitted
    if (permittedActions.contains("Download") || permittedActions.contains("View")) {
        linksBuilder.download(buildDownloadLink(document));
    }

    // NEVER add delete link in enquiry response (per John)
    // Delete should only be available through separate endpoint
    // Even if permitted, don't include in document list response

    return linksBuilder.build();
}

private List<String> getPermittedActions(
    MasterTemplateDefinitionEntity template,
    String requestorType
) {
    // Parse access_control JSON
    AccessControl accessControl = parseAccessControl(template.getAccessControl());

    if (accessControl == null) {
        // No restrictions - return default based on requestor type
        return getDefaultActionsForRequestorType(requestorType);
    }

    // Map X-requestor-type to access_control role
    String role = mapRequestorTypeToRole(requestorType);

    return accessControl.getActionsForRole(role);
}

private String mapRequestorTypeToRole(String requestorType) {
    if (requestorType == null) {
        return "customer"; // Default to most restrictive
    }

    switch (requestorType.toUpperCase()) {
        case "CUSTOMER":
            return "customer";
        case "AGENT":
            return "agent";
        case "SYSTEM":
            return "system";
        default:
            return "customer";
    }
}

private List<String> getDefaultActionsForRequestorType(String requestorType) {
    switch (requestorType.toUpperCase()) {
        case "CUSTOMER":
            return List.of("View", "Download");
        case "AGENT":
            return List.of("View", "Download");
        case "SYSTEM":
            return List.of("View", "Update", "Delete", "Download");
        default:
            return List.of("View", "Download");
    }
}
```

### 3. Add Link Expiration (P1-003)

**File:** `src/main/resources/doc-hub.yaml`

Update `Links_download` schema:
```yaml
Links_download:
  type: object
  properties:
    href:
      type: string
      description: URL to download the document
    type:
      type: string
      example: GET
    rel:
      type: string
      example: download
    title:
      type: string
      example: Download this document
    responseTypes:
      type: array
      items:
        type: string
    expiresAt:
      type: integer
      format: int64
      description: |
        Epoch timestamp (seconds) when this link expires.
        Client should refresh if current time > expiresAt.
      example: 1740527443
```

**Service Implementation:**
```java
private LinksDownload buildDownloadLink(StorageIndexEntity document) {
    // Link valid for 10 minutes (configurable)
    long expiresAt = Instant.now().plusSeconds(600).getEpochSecond();

    return LinksDownload.builder()
        .href("/documents/" + encodeDocumentId(document))
        .type("GET")
        .rel("download")
        .title("Download this document")
        .responseTypes(List.of("application/pdf", "application/octet-stream"))
        .expiresAt(expiresAt)
        .build();
}
```

### 4. Remove Delete Link from Enquiry (P1-004)

**John's Decision:** "There should be no delete function in the get inquiry"

**Rule:** NEVER include delete link in `/documents-enquiry` response, regardless of permissions.

Delete action should only be available:
- Through direct `DELETE /documents/{documentId}` endpoint
- After additional authorization checks
- Only for SYSTEM or backOffice requestors

### 5. Sample Access Control Data

**Update `data.sql` with access control configurations:**

```sql
-- Privacy Policy - Customer can view/download, Agent can view/download
UPDATE document_hub.master_template_definition
SET access_control = '[
  {"role": "customer", "actions": ["View", "Download"]},
  {"role": "agent", "actions": ["View", "Download"]},
  {"role": "system", "actions": ["View", "Update", "Delete", "Download"]}
]'::jsonb
WHERE template_type = 'PrivacyPolicy';

-- Statement - Customer can view/download only
UPDATE document_hub.master_template_definition
SET access_control = '[
  {"role": "customer", "actions": ["View", "Download"]},
  {"role": "agent", "actions": ["View", "Download"]},
  {"role": "system", "actions": ["View", "Update", "Delete", "Download"]}
]'::jsonb
WHERE template_type = 'Statement';

-- Internal documents - Customer CANNOT see
UPDATE document_hub.master_template_definition
SET access_control = '[
  {"role": "customer", "actions": []},
  {"role": "agent", "actions": ["View"]},
  {"role": "system", "actions": ["View", "Update", "Delete", "Download"]}
]'::jsonb
WHERE template_category = 'Internal';
```

---

## API Response Examples

### Before (Current - Wrong)

```json
{
  "documentList": [
    {
      "documentId": "abc123",
      "displayName": "Privacy Policy 2024",
      "_links": {
        "download": {
          "href": "/documents/abc123",
          "type": "GET",
          "rel": "download"
        },
        "delete": {
          "href": "/documents/abc123",
          "type": "DELETE",
          "rel": "delete"
        }
      }
    }
  ]
}
```

### After (Correct - Customer Request)

```json
{
  "documentList": [
    {
      "documentId": "abc123",
      "displayName": "Privacy Policy 2024",
      "_links": {
        "download": {
          "href": "/documents/abc123",
          "type": "GET",
          "rel": "download",
          "expiresAt": 1740527443
        }
      }
    }
  ]
}
```

**Note:** No delete link, has expiration timestamp.

### After (Correct - System Request)

System requests may include additional links if template permits:

```json
{
  "documentList": [
    {
      "documentId": "abc123",
      "displayName": "Privacy Policy 2024",
      "_links": {
        "download": {
          "href": "/documents/abc123",
          "type": "GET",
          "rel": "download",
          "expiresAt": 1740527443
        }
      }
    }
  ]
}
```

**Note:** Even for SYSTEM, delete link is NOT included in enquiry response (per John's requirement).

---

## Implementation Steps

### Phase 1: Remove Delete Link (Quick Win)

1. Update response builder to never include delete link in enquiry response
2. Test all requestor types

### Phase 2: Add Link Expiration

1. Update OpenAPI spec with `expiresAt` field
2. Regenerate models
3. Update response builder to calculate and include expiration
4. Make expiration configurable (default: 10 minutes)

### Phase 3: Access Control Filtering

1. Pass `X-requestor-type` from controller to service
2. Implement role mapping logic
3. Parse `access_control` JSON from template
4. Filter links based on permitted actions
5. Update sample data with access control configurations

### Phase 4: Testing

1. Unit tests for role mapping
2. Unit tests for action filtering
3. Integration tests with different requestor types
4. Verify delete link never appears in enquiry

---

## Files to Modify

| File | Changes |
|------|---------|
| `doc-hub.yaml` | Add `expiresAt` to Links_download |
| `DocumentEnquiryController.java` | Pass requestorType to service |
| `DocumentEnquiryService.java` | Add link filtering logic |
| `data.sql` | Add access_control data to templates |

---

## Configuration

**application.properties:**
```properties
# Link expiration in seconds (default: 10 minutes)
app.links.download.expiration-seconds=600

# Default actions when no access_control defined
app.access.default.customer=View,Download
app.access.default.agent=View,Download
app.access.default.system=View,Update,Delete,Download
```

---

## Test Scenarios

| Scenario | X-requestor-type | access_control | Expected Links |
|----------|------------------|----------------|----------------|
| Customer, no restrictions | CUSTOMER | null | download (no delete) |
| Customer, restricted | CUSTOMER | `[{role:customer, actions:[View]}]` | None (View only, no download) |
| Agent, full access | AGENT | `[{role:agent, actions:[View,Download]}]` | download (no delete) |
| System, full access | SYSTEM | null | download (no delete) |
| Missing header | null | null | download (default to customer) |

---

## Acceptance Criteria

- [ ] Delete link NEVER appears in `/documents-enquiry` response
- [ ] Download links include `expiresAt` timestamp
- [ ] Links filtered based on `X-requestor-type` header
- [ ] Links filtered based on template `access_control` metadata
- [ ] Default behavior when no `access_control` defined
- [ ] Graceful handling of missing/invalid header
- [ ] Unit tests cover all scenarios
- [ ] Integration tests verify end-to-end

---

## Document History

| Date | Author | Changes |
|------|--------|---------|
| Dec 2024 | Team | Initial creation from John's Day 1 feedback |
