# Questions to Clarify with John

**Date:** December 2024
**Timeline:** John returns January 3rd
**Purpose:** Consolidate all open questions and decisions needed before implementation can proceed

---

## RESOLVED - Day 3 Answers

These questions were answered by John in the Day 3 session.

### Message Center Doc Flag - RESOLVED

| # | Question | John's Answer |
|---|----------|---------------|
| 1.1 | Should `message_center_doc_flag = true` always be required? | **Add as API parameter with default = true** |
| 1.2 | Use cases for non-message-center documents? | **If false is passed, return BOTH types** |
| 1.3 | What if flag is NULL? | **Default to true** (not explicitly answered but implied) |
| 1.4 | Mobile same as web? | **Not discussed** - still open |

**Implementation Decision:** Add `message_center_doc_flag` as an **API request parameter** with:
- Default: `true` (90% of calls are from message center)
- If `false`: Return both message center and non-message center documents
- Work with Kushan to follow API standards

### Line of Business Default - RESOLVED

| Question | John's Answer |
|----------|---------------|
| What should default be? | **Default: CREDIT_CARD** (99% of usage) |

### Communication Type - RESOLVED

| Question | John's Answer |
|----------|---------------|
| What is communication type? | **Same as template_type** (Letter, Email, SMS, Push) |
| What should default be? | **Default: LETTER** |
| Should it be in API? | **Yes, add as parameter** |

### Deleted Records - RESOLVED

| Question | John's Answer |
|----------|---------------|
| Should agents be able to query deleted records? | **No - deleted means not returned** |
| How to recover if needed? | **Flip the deleted flag off manually** |
| Purpose of soft delete? | **Audit purposes only** |

### Posted Date Filter - RESOLVED

| Question | John's Answer |
|----------|---------------|
| Which column for postedFromDate/postedToDate? | **Use `doc_creation_date`** (not created_timestamp) |
| Why? | Documents received from external systems (e.g., statements) have different creation vs insert dates |

### X-Version Header - RESOLVED

| Question | John's Answer |
|----------|---------------|
| What is X-version for? | **API version number** |
| Should all APIs have it? | **Yes** - per EA standard |

### Date Overlap Handling - RESOLVED

| Question | John's Answer |
|----------|---------------|
| Can queries return multiple documents? | **Current queries must return only ONE** |
| How to prevent overlaps? | **Add validation on upload** to prevent overlapping date ranges |

---

## Priority 1: Remaining Blocking Questions

These questions are still blocking current development work.

### 1. Message Center Doc Flag - Remaining Questions

**Context:** Most questions answered, but some remain.

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 1.4 | Should mobile apps use the same filtering as web? | Yes / No (different rules) | May need channel-based filtering |

**See:** `docs/architecture/message-center-doc-flag-options.md` for detailed implementation options.

---

### 2. Accessible Flag

**Context:** The `accessible_flag` field exists in the entity but is not filtered.

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 2.1 | What does `accessible_flag` mean? | Document is accessible / Template is accessible / Something else | Need to understand before filtering |
| 2.2 | Should `accessible_flag = true` be required in all queries? | Yes / No | Security consideration |
| 2.3 | How does `accessible_flag` differ from `active_flag`? | Different purposes / Redundant | May simplify if redundant |
| 2.4 | Who/what sets `accessible_flag`? | Admin / System / Workflow | Understanding data flow |

---

### 3. Regulatory Flag Interaction

**Context:** John explained regulatory flag bypasses do-not-contact, but unclear how it interacts with other flags.

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 3.1 | Should regulatory documents (`regulatory_flag = true`) always appear regardless of `message_center_doc_flag`? | Yes (override) / No (both required) | Special handling needed |
| 3.2 | Does regulatory flag affect which HATEOAS actions are available? | Yes / No | Response building logic |
| 3.3 | Is there a "do-not-contact" field we need to check? | Yes (which field?) / No | Additional filtering |

**John's quote:** "If we go to print this and they're in a do-not-contact state, regulatory still has to get printed."

---

### 4. HATEOAS Links & Actions

**Context:** John said we're "giving all the actions every time" which doesn't match template metadata.

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 4.1 | Which template metadata field controls allowed actions? | `access_control` JSON / Other field | Need to parse and apply |
| 4.2 | What is the structure of the access control metadata? | Need schema | Implementation |
| 4.3 | Should download links have an expiration timestamp? | Yes (how long?) / No | Add `expires_at` to links |
| 4.4 | Should delete link NEVER appear in document enquiry response? | Yes / Conditional | Remove from response builder |
| 4.5 | What actions can appear? | View, Download, Delete, Print, Share? | Define action enum |

**John's quotes:**
- "You're retrieving the links, but you don't have a lifetime object."
- "There should be no delete function in the get inquiry."
- "You should only be providing these items here that match that metadata."

---

## Priority 2: Clarification Needed

These don't block implementation but need answers for correctness.

### 5. Header Parameters

**Context:** John questioned the purpose of certain headers.

| # | Question | Context | John's Quote |
|---|----------|---------|--------------|
| 5.1 | What is `trace_id` for? | Header parameter | "What is that ID for? There's a trace ID, but what does that mean?" |
| 5.2 | What does `X-version` represent? | API versioning | "Is that the version of the API call? Is it the version of the trace ID?" |
| 5.3 | Is this a Customer 365 standard? | Header origin | "Is it part of customer 365?" |
| 5.4 | Should these go in the data dictionary? | Documentation | "You're gonna have to add this to the data dictionary." |

**John's directive:** "So let's research that to find out exactly what it is for. Otherwise, how do you implement it correctly?"

---

### 6. Template Type vs Communication Type

**Context:** John mentioned template_type should represent communication channel (Letter, Email, SMS, Push).

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 6.1 | Is `template_type` the same as communication type? | Yes / No (separate field needed) | Schema clarification |
| 6.2 | Current `template_type` values are document types (Statement, PrivacyNotice). Should these change? | Yes (rename to communication_type) / No (add new field) | API/DB changes |
| 6.3 | What are the valid communication types? | Letter, Email, SMS, Push / Others? | Enum definition |

**John's quote:** "We also talked about adding that field for the template type, like if it was a letter, e-mail, push, SMS."

---

### 7. Vendor Configuration

**Context:** Vendors should be categorized by communication type.

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 7.1 | Should vendor validation check communication type compatibility? | Yes / No | Validation logic |
| 7.2 | Where is vendor-to-communication-type mapping stored? | Config file / Database table / Hardcoded | Implementation |
| 7.3 | Is vendor a string or should it be validated against a list? | Free text / Validated list | Data integrity |

**John's quote:** "For letters you might have Smartcom. For emails you might have Salesforce. But you'll never have Salesforce for a letter."

---

### 8. Soft Delete Cascade

**Context:** When deleting templates, related records need soft delete too.

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 8.1 | When a template is soft-deleted, which related records should also be soft-deleted? | Vendors only / Vendors + Documents / All related | Cascade logic |
| 8.2 | Is there a vendor_mapping table that needs cascade? | Yes / No | Schema clarification |
| 8.3 | Should soft-deleted templates be completely hidden or filterable? | Hidden / Filterable with flag | Query changes |

**John's quote:** "Don't forget, since we're doing soft deletes, if you delete a template, they need to make sure you also soft delete all the vendors assigned to that template."

---

### 9. Multiple Documents / Effective Dates

**Context:** John's scenario about future-dated documents.

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 9.1 | When multiple documents match (e.g., current + future privacy policy), which to return? | Only current / All with dates / Configurable | Query/filter logic |
| 9.2 | Should future-effective documents be hidden until effective date? | Yes / No (show with indicator) | Visibility logic |
| 9.3 | What fields define document effective period? | `start_date`/`end_date` in metadata / Template dates / Both | Where to check |

**John's scenario:** "They have another privacy statement... They want it to go into production two months from now... But they don't want to use it yet."

---

### 10. Domain ID / Data Extraction

**Context:** John asked about domain_id in data extraction config.

| # | Question | Options | Impact |
|---|----------|---------|--------|
| 10.1 | What is domain_id and where does it come from? | Accounts API / Config / Other | Documentation |
| 10.2 | Is domain_id required for all data extractions? | Yes / No (optional) | Validation |
| 10.3 | Who owns/provides the domain_id values? | Accounts team / Doc Hub team | Coordination |

**John's quote:** "What is the domain ID? Where do we get that information from?"

---

## Priority 3: Future Planning

These are for roadmap planning, not immediate implementation.

### 11. Customer ID Filtering

| # | Question | Impact |
|---|----------|--------|
| 11.1 | How should customerId filter work when accountId is also provided? | AND vs OR logic |
| 11.2 | Can a customer have documents not associated with any account? | Query design |
| 11.3 | Should we support "all documents for customer across all accounts"? | Feature scope |

---

### 12. Posted Date Range Filter

| # | Question | Impact |
|---|----------|--------|
| 12.1 | Should `postedFromDate`/`postedToDate` filter on `doc_creation_date` in storage_index? | Field mapping |
| 12.2 | Is this inclusive or exclusive of boundary dates? | Query logic |
| 12.3 | What timezone should dates be interpreted in? | UTC / Local / Configurable |

---

### 13. Notification Needed Flag

| # | Question | Impact |
|---|----------|--------|
| 13.1 | What triggers when `notification_needed = true`? | Integration needed |
| 13.2 | Who receives the notification? | Customer / Agent / System |
| 13.3 | Is this handled by Document Hub or another service? | Responsibility |

---

## Summary: Critical Decisions Needed

| # | Topic | Question | Blocking? | Status |
|---|-------|----------|-----------|--------|
| 1 | message_center_doc_flag | Always filter or conditional? | Yes | ✅ RESOLVED - Add as parameter, default true |
| 2 | accessible_flag | What does it mean? Filter it? | Yes | ❓ Still open |
| 3 | regulatory_flag | Does it override message_center_doc_flag? | Yes | ❓ Still open |
| 4 | HATEOAS actions | Which metadata controls allowed actions? | Yes | ❓ Still open |
| 5 | Link expiration | How long should download links be valid? | Medium | ❓ Still open |
| 6 | Delete in response | Remove delete link from enquiry? | Medium | ❓ Still open |
| 7 | Header purpose | What are trace_id and X-version for? | Low | ✅ RESOLVED - X-version is API version |
| 8 | Vendor validation | Check communication type compatibility? | Low | ❓ Still open |
| 9 | Soft delete cascade | Which tables to cascade? | Low | ❓ Still open |
| 10 | Multiple documents | Return current only or all? | Medium | ✅ RESOLVED - Only ONE for current queries |
| 11 | Posted date field | Which column to use? | Medium | ✅ RESOLVED - Use doc_creation_date |
| 12 | Deleted records | Allow querying deleted? | Low | ✅ RESOLVED - No, flip flag manually |
| 13 | Communication type | Add to API? Default? | Yes | ✅ RESOLVED - Yes, default LETTER |
| 14 | LOB default | What default? | Medium | ✅ RESOLVED - Default CREDIT_CARD |

---

## Suggested Meeting Agenda

1. **Message Center Doc Flag** (10 min) - Questions 1.1-1.4
2. **Accessible Flag** (5 min) - Questions 2.1-2.4
3. **HATEOAS & Actions** (10 min) - Questions 4.1-4.5
4. **Regulatory Flag** (5 min) - Questions 3.1-3.3
5. **Quick Clarifications** (10 min) - Headers, Vendors, Cascade
6. **Future Planning** (10 min) - Customer filtering, Date ranges

**Total:** ~50 minutes

---

---

## New Items from Day 3

These are new requirements/fields identified in Day 3 that need to be implemented:

### 1. Workflow Field for Templates

**Requirement:** Add `workflow` field to master_template_definition

| Field | Purpose | Values |
|-------|---------|--------|
| `workflow` | WCM workflow to use | "2_EYES", "4_EYES", etc. |

**Terminology:**
- **2 eyes** = One person approval
- **4 eyes** = Two person approval

### 2. Date Overlap Validation

**Requirement:** On document upload, validate that date ranges don't overlap with existing documents of the same type.

### 3. Storage Index Date Fields

**Requirement:** Investigate whether `start_date` and `end_date` should be columns or in JSON for storage_index table.

**Decision criteria:** Which is faster for queries?

---

## Document History

| Date | Author | Changes |
|------|--------|---------|
| Dec 2024 | Team | Initial creation from demo feedback |
| Dec 2024 | Team | Updated with Day 3 answers - many questions resolved |
