# Message Center Doc Flag - Implementation Options

**Date:** December 2024
**Status:** Pending Decision
**Priority:** High

---

## Background

### John's Requirement
> "Make sure document is actually assigned to the message center"

The `message_center_doc_flag` field exists in the `master_template_definition` table and indicates whether a template/document should be displayed in the web message center. Currently, this flag is **not filtered** in any repository queries.

### Current State

| Item | Status |
|------|--------|
| Database column | `message_center_doc_flag` (Boolean) |
| Entity field | `messageCenterDocFlag` in `MasterTemplateDefinitionEntity` |
| Repository filter | **NOT IMPLEMENTED** |
| API parameter | **NOT AVAILABLE** |

### Why This Matters

Without this filter:
- Documents not intended for web display may appear in customer message centers
- Internal-only documents could be exposed to customers
- Print-only documents (letters) would incorrectly show in web queries

---

## Option 1: Add to ALL Existing Queries (Recommended)

### Approach
Add `AND message_center_doc_flag = true` to all template queries in `MasterTemplateRepository`. This makes the filter mandatory for all document enquiry operations.

### Implementation

```java
// MasterTemplateRepository.java - Modify ALL queries

// Example: findActiveTemplatesByLineOfBusiness
@Query("SELECT * FROM document_hub.master_template_definition " +
       "WHERE active_flag = true " +
       "AND message_center_doc_flag = true " +  // ADD THIS LINE
       "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
       "AND (start_date IS NULL OR start_date <= :currentDate) " +
       "AND (end_date IS NULL OR end_date >= :currentDate)")
Flux<MasterTemplateDefinitionEntity> findActiveTemplatesByLineOfBusiness(
    String lineOfBusiness,
    Long currentDate
);
```

### Queries to Modify

| Query Method | Current Filters | Add Filter |
|--------------|-----------------|------------|
| `findActiveTemplates` | active_flag, date range | message_center_doc_flag = true |
| `findActiveSharedTemplates` | active_flag, shared_document_flag, date range | message_center_doc_flag = true |
| `findActiveTemplatesByLineOfBusiness` | active_flag, line_of_business, date range | message_center_doc_flag = true |
| `findActiveSharedTemplatesByLineOfBusiness` | active_flag, shared_document_flag, line_of_business, date range | message_center_doc_flag = true |
| `findActiveTemplatesByLineOfBusinessAndTypes` | active_flag, line_of_business, template_type, date range | message_center_doc_flag = true |

### Pros
- Simple, consistent filtering across all queries
- All templates returned are guaranteed to be message center eligible
- No additional API parameters needed
- No OpenAPI spec changes required
- Backward compatible - existing clients work unchanged
- Aligns with John's requirement

### Cons
- Assumes all document enquiry calls are for message center
- No flexibility for future use cases (e.g., admin queries that need all documents)
- Would need separate queries/endpoints for admin tools

### Effort Estimate
- **Development:** ~15 minutes
- **Testing:** ~30 minutes
- **Risk:** Low

---

## Option 2: Conditional Filter via API Parameter

### Approach
Add an optional `messageCenterOnly` boolean parameter to the `DocumentListRequest`. Default to `true` for backward compatibility and security.

### Implementation

**1. OpenAPI Spec Change (`doc-hub.yaml`):**
```yaml
DocumentListRequest:
  type: object
  properties:
    # ... existing properties ...
    messageCenterOnly:
      type: boolean
      description: "Filter to only include documents flagged for message center display. Defaults to true for web/mobile clients."
      default: true
      example: true
      x-data-classification: public
```

**2. Service Logic (`DocumentEnquiryService.java`):**
```java
// Extract parameter (default true)
boolean messageCenterOnly = request.getMessageCenterOnly() != null
    ? request.getMessageCenterOnly()
    : true;

// Choose query based on parameter
if (messageCenterOnly) {
    return templateRepository.findActiveTemplatesForMessageCenter(...);
} else {
    return templateRepository.findActiveTemplates(...);
}
```

**3. Repository (`MasterTemplateRepository.java`):**
```java
// Add new methods with message_center_doc_flag filter
@Query("SELECT * FROM document_hub.master_template_definition " +
       "WHERE active_flag = true " +
       "AND message_center_doc_flag = true " +
       "AND (line_of_business = :lineOfBusiness OR line_of_business = 'ENTERPRISE') " +
       "AND (start_date IS NULL OR start_date <= :currentDate) " +
       "AND (end_date IS NULL OR end_date >= :currentDate)")
Flux<MasterTemplateDefinitionEntity> findActiveTemplatesForMessageCenterByLineOfBusiness(
    String lineOfBusiness,
    Long currentDate
);
```

### Pros
- Flexible - can query all documents or message center only
- Useful for admin/internal tools (set `messageCenterOnly: false`)
- Backward compatible with `default: true`
- Self-documenting API
- Single endpoint serves multiple use cases

### Cons
- Requires OpenAPI spec change and model regeneration
- More complex service logic with conditional branching
- More repository methods to maintain
- Potential security concern if clients can bypass the filter

### Effort Estimate
- **Development:** ~1 hour
- **Testing:** ~1 hour
- **Risk:** Medium (API change)

---

## Option 3: Channel-Based Filtering

### Approach
Add a `channel` enum parameter that determines which document flags to check. Different channels have different display requirements.

### Implementation

**1. OpenAPI Spec Change (`doc-hub.yaml`):**
```yaml
DocumentListRequest:
  type: object
  properties:
    # ... existing properties ...
    channel:
      type: string
      description: "The channel requesting documents. Determines which documents are eligible."
      enum:
        - WEB
        - MOBILE
        - AGENT
        - PRINT
        - ALL
      default: WEB
      example: WEB
      x-data-classification: public
```

**2. Channel-to-Filter Mapping:**

| Channel | message_center_doc_flag | regulatory_flag | Other Filters |
|---------|------------------------|-----------------|---------------|
| WEB | = true | any | - |
| MOBILE | = true | any | - |
| AGENT | any | any | - |
| PRINT | any | any | template_type IN ('Letter') |
| ALL | any | any | - |

**3. Service Logic:**
```java
private Flux<MasterTemplateDefinitionEntity> getTemplatesForChannel(
    String channel,
    String lineOfBusiness,
    Long currentDate
) {
    switch (channel) {
        case "WEB":
        case "MOBILE":
            return templateRepository.findForMessageCenter(lineOfBusiness, currentDate);
        case "AGENT":
            return templateRepository.findForAgent(lineOfBusiness, currentDate);
        case "PRINT":
            return templateRepository.findForPrint(lineOfBusiness, currentDate);
        case "ALL":
        default:
            return templateRepository.findAll(lineOfBusiness, currentDate);
    }
}
```

### Pros
- Future-proof for multi-channel support
- Clear semantic meaning - channel dictates behavior
- Can add more channel-specific flags later (e.g., `mobile_display_flag`)
- Extensible architecture
- Aligns with omnichannel document strategy

### Cons
- Most complex to implement
- Requires defining and maintaining channel-to-filter mappings
- May be over-engineered for current requirements
- Requires OpenAPI spec change

### Effort Estimate
- **Development:** ~2-3 hours
- **Testing:** ~2 hours
- **Risk:** Medium-High (more complex)

---

## Option 4: Hybrid - Default ON with Header Override

### Approach
Filter `message_center_doc_flag = true` by default in all queries, but allow an internal header override for admin/debugging purposes.

### Implementation

**1. Repository (always filters):**
```java
// All queries include message_center_doc_flag = true by default
@Query("SELECT * FROM document_hub.master_template_definition " +
       "WHERE active_flag = true " +
       "AND message_center_doc_flag = true " +
       "AND ...")
```

**2. Add internal header for override:**
```yaml
# Header parameter (internal use only)
X-Include-All-Templates:
  in: header
  description: "Internal use only. Set to 'true' to bypass message_center_doc_flag filter."
  required: false
  schema:
    type: boolean
    default: false
```

**3. Service Logic:**
```java
// Check for override header
boolean includeAll = "true".equals(headers.get("X-Include-All-Templates"));

if (includeAll) {
    // Use queries without message_center_doc_flag filter
    return templateRepository.findActiveTemplatesAll(...);
} else {
    // Use default queries with message_center_doc_flag = true
    return templateRepository.findActiveTemplates(...);
}
```

### Pros
- Safe default for all customer-facing calls
- Flexibility for admin/internal tools via header
- Minimal API contract changes (header vs body parameter)
- Easy to audit who uses the override (via logging)

### Cons
- Header-based overrides can be confusing
- Two sets of repository methods needed
- Security consideration - header could be spoofed
- Not discoverable via OpenAPI spec (unless documented)

### Effort Estimate
- **Development:** ~1.5 hours
- **Testing:** ~1 hour
- **Risk:** Medium

---

## Comparison Matrix

| Criteria | Option 1 | Option 2 | Option 3 | Option 4 |
|----------|----------|----------|----------|----------|
| **Complexity** | Low | Medium | High | Medium |
| **API Changes** | None | Yes | Yes | Header only |
| **Flexibility** | Low | High | Very High | Medium |
| **Security** | High | Medium | Medium | High |
| **Future-proof** | Low | Medium | High | Medium |
| **Effort** | ~45 min | ~2 hours | ~5 hours | ~2.5 hours |
| **Risk** | Low | Medium | Medium-High | Medium |

---

## Recommendation

### For Immediate Implementation: **Option 1**

**Rationale:**
1. **John's feedback was explicit:** Documents must be assigned to message center
2. **Document enquiry API is specifically for web/message center** - that's its primary purpose
3. **Simplest implementation** with lowest risk
4. **No API changes required** - backward compatible
5. **Can always evolve to Option 2 or 3 later** if admin requirements emerge

### For Future Consideration: **Option 3**

If the platform expands to support multiple channels (web, mobile, agent desktop, print fulfillment), Option 3 provides the most extensible architecture. Consider this for Phase 2.

---

## Decision Log

| Date | Decision | Rationale | Decided By |
|------|----------|-----------|------------|
| TBD | | | |

---

## Questions to Clarify with John

Before implementing, the following questions should be clarified:

### Message Center Doc Flag

| # | Question | Context | Impact |
|---|----------|---------|--------|
| 1 | **Should `message_center_doc_flag` always be required for document enquiry?** | Currently the flag exists but isn't filtered. Is every document enquiry call intended for web/message center? | Determines if Option 1 (always filter) or Option 2 (conditional) is correct |
| 2 | **Are there use cases where agents or internal tools need to see non-message-center documents via this API?** | If yes, we need a bypass mechanism | Determines if we need Option 2, 3, or 4 |
| 3 | **What should happen if `message_center_doc_flag` is NULL in the database?** | Treat as false (exclude)? Or treat as true (include)? | Affects query logic: `= true` vs `!= false` |
| 4 | **Should mobile apps use the same filtering as web?** | Mobile may have different display requirements | May need channel-based approach (Option 3) |

### Related Flags

| # | Question | Context | Impact |
|---|----------|---------|--------|
| 5 | **How should `regulatory_flag` interact with `message_center_doc_flag`?** | John said regulatory docs bypass do-not-contact. Should regulatory docs always appear regardless of message_center_doc_flag? | May need special handling for regulatory documents |
| 6 | **What about `accessible_flag`?** | This field exists but isn't being filtered. Is it separate from message_center_doc_flag? | Another filter to potentially add |
| 7 | **Is there a `print_only_flag` or similar for documents that should NOT appear in message center?** | Some documents may be print-only (letters) | May need inverse filtering |

### Document Lifecycle

| # | Question | Context | Impact |
|---|----------|---------|--------|
| 8 | **Can `message_center_doc_flag` change after a template is created?** | If a template is later enabled for message center, should existing documents under that template become visible? | Affects whether we filter at template or document level |
| 9 | **Is there a separate approval workflow before enabling message_center_doc_flag?** | Security/compliance consideration | May need audit trail |

### API Design

| # | Question | Context | Impact |
|---|----------|---------|--------|
| 10 | **Should the response indicate WHY a document is visible?** | e.g., "This document is visible because message_center_doc_flag=true" | Debugging/transparency |
| 11 | **Should we expose `message_center_doc_flag` in the response metadata?** | Clients might want to know | API response schema change |

---

## Quick Decision Guide

If John answers:

| If... | Then... |
|-------|---------|
| "Document enquiry is only for message center, no exceptions" | **Option 1** - Add filter to all queries |
| "Agents need to see everything" | **Option 2** or **Option 4** - Conditional filter |
| "Different channels have different needs" | **Option 3** - Channel-based filtering |
| "Just default to message center, but allow override" | **Option 4** - Hybrid approach |

---

## References

- John's feedback: "Make sure document is actually assigned to the message center"
- Entity: `MasterTemplateDefinitionEntity.messageCenterDocFlag`
- Database: `document_hub.master_template_definition.message_center_doc_flag`
