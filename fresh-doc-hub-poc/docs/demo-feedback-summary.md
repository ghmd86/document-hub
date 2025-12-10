# Demo Feedback Summary

This document summarizes feedback from demo sessions and tracks action items.

---

## Demo Sessions Overview

| Session | Date | Participants |
|---------|------|--------------|
| Day 1 | Early December 2024 | Taher, John Drum, Prachi, Satheesh, Murali, Jatish |
| Day 2 | Early December 2024 | Taher, John Drum, Jagadeesh, Pankaj, Satheesh, Murali |

---

## Day 1 Feedback

### Topics Demonstrated

1. **Rules Engine** - Dynamic document sharing based on eligibility rules
2. **File Upload/Download** - Document storage and retrieval flow

### Discussion Points

#### Rules Engine Demo
- Demonstrated the rules engine for dynamically picking shared documents
- Showed how documents are filtered based on customer/account attributes
- Covered different sharing scopes (ALL, ACCOUNT_TYPE, CUSTOM_RULES)

#### File Upload/Download
- **File naming consistency verified:**
  - Filename passed as form parameter in body (not header)
  - Upload file with name "demo.pdf"
  - Download returns same filename
- **Flow tested:** Upload → Get Document ID → Download by ID

### Day 1 Action Items

| # | Item | Status |
|---|------|--------|
| 1 | Verify file naming consistency | ✅ Verified working |
| 2 | Document upload/download flow | ⚠️ Pending |

---

## Day 2 Feedback

### Changes Demonstrated

Jagadeesh presented API modifications:

1. **Added tracking headers to all endpoints:**
   - Two header parameters for auditory/tracking purposes
   - Applied to all API endpoints

2. **Template API changes:**
   - Removed `template_version` from POST request (create template)
   - Version auto-assigned on creation

### John Drum's Feedback

#### Positive
- "It's a good start"
- "Don't get me wrong, I don't want to beat you up on it"

#### Areas for Improvement

| Feedback | Interpretation |
|----------|----------------|
| "Needs to be cleaned up" | Documentation and code need refinement |
| "Show the entire looping operation" | Document the complete flow of how templates are processed, rules evaluated, and documents matched |
| "If you tried to give this to a developer, they wouldn't be able to do anything with it" | API documentation insufficient for developer onboarding |
| "We need to make it useful" | Focus on practical, implementable documentation |

#### Specific Request
> "Show the entire looping operation because... if you tried to give this to a developer, they wouldn't be able to do anything with it."

This refers to documenting:
1. How the system loops through templates
2. How eligibility rules are evaluated for each template
3. How documents are matched based on rules
4. The complete data extraction → rule evaluation → document matching flow

### Day 2 Action Items

| # | Item | Owner | Status |
|---|------|-------|--------|
| 1 | Add tracking headers to all endpoints | Jagadeesh | ✅ Complete |
| 2 | Remove template_version from POST | Jagadeesh | ✅ Complete |
| 3 | Clean up API documentation | Team | ⚠️ In Progress |
| 4 | Document entire looping operation | Team | ✅ Complete (see architecture docs) |
| 5 | Make documentation developer-friendly | Team | ⚠️ In Progress |

### Timeline Discussion
- John returns: January 3rd
- Plan: Final review before holiday break
- Priority: Unblock developers to continue API work

---

## Consolidated Action Items

### High Priority (Must Complete)

| # | Action Item | Description | Status | Notes |
|---|-------------|-------------|--------|-------|
| 1 | Document the looping operation | Show how templates are processed in sequence, rules evaluated, documents matched | ✅ Done | See `docs/architecture/document-enquiry-flow.md` |
| 2 | Clean up API spec | Make OpenAPI spec complete and accurate | ⚠️ Pending | `doc-hub.yaml` needs review |
| 3 | Developer onboarding guide | Step-by-step guide for new developers | ⚠️ Pending | |
| 4 | Add tracking headers | X-correlation-id, X-requestor-id on all endpoints | ✅ Done | |

### Medium Priority

| # | Action Item | Description | Status |
|---|-------------|-------------|--------|
| 5 | Document upload/download flow | Show complete file lifecycle | ⚠️ Pending |
| 6 | API examples with Postman | Ready-to-use request examples | ⚠️ Pending |
| 7 | Error handling documentation | Common errors and resolutions | ⚠️ Pending |

### Low Priority

| # | Action Item | Description | Status |
|---|-------------|-------------|--------|
| 8 | Video walkthrough | Record demo for async review | Not Started |
| 9 | Integration test suite | Automated tests for API | Not Started |

---

## Documentation Created to Address Feedback

The following documentation was created to address John's feedback about "showing the entire looping operation":

### Architecture Documentation (`docs/architecture/`)

| Document | Purpose | Addresses Feedback |
|----------|---------|-------------------|
| `document-enquiry-flow.md` | 6 detailed diagrams showing complete flow | "Show the entire looping operation" |
| `use-cases-design-review.md` | 10 use cases with implementation details | "Make it useful for developers" |
| `business-use-cases-comprehensive.md` | Full 53 use cases for roadmap | Future planning |
| `gap-analysis.md` | Current vs target implementation | Prioritization |
| `schema-analysis-versioning-vendor.md` | Database design analysis | Technical reference |

### Key Diagrams Created

1. **High-Level Sequence Diagram** - Shows interaction between all components
2. **Data Extraction Chain Flow** - Multi-step API extraction process
3. **Rule Evaluation Logic** - AND/OR operator processing
4. **Document Matching Strategy** - Reference key and conditional matching
5. **Sharing Scope Decision Tree** - Complete decision logic
6. **End-to-End Flow** - Full request-to-response process

---

## Remaining Gaps

### Documentation Gaps

| Gap | Impact | Recommendation |
|-----|--------|----------------|
| No developer quickstart guide | New devs can't onboard quickly | Create `QUICKSTART.md` |
| API spec incomplete | Download/delete not implemented | Complete OpenAPI spec |
| No Postman collection | Manual testing difficult | Create/update collection |
| No error code reference | Debugging difficult | Document error responses |

### Implementation Gaps

| Gap | Impact | Recommendation |
|-----|--------|----------------|
| Download endpoint not implemented | Can't retrieve documents | Implement `GET /documents/{id}` |
| Delete endpoint not implemented | Can't remove documents | Implement `DELETE /documents/{id}` |
| Upload endpoint not implemented | Can't add documents | Implement `POST /documents` |
| Template CRUD not implemented | Can't manage templates via API | Implement template management |

---

## Next Steps

### Before January 3rd (John's Return)

1. **Complete developer onboarding documentation**
   - Quickstart guide
   - API usage examples
   - Common patterns

2. **Review and clean up API spec**
   - Verify all endpoints documented
   - Add examples to OpenAPI spec
   - Ensure consistency

3. **Prepare demo improvements**
   - Clean up demo flow
   - Prepare talking points
   - Document demo scenarios

### Q1 2025 Priorities

1. Implement missing CRUD endpoints
2. Complete template management API
3. Add approval workflow (Phase 2)

---

## Feedback Tracking

| Date | From | Feedback | Action Taken | Status |
|------|------|----------|--------------|--------|
| Day 1 | Team | File naming verification needed | Tested upload/download | ✅ Done |
| Day 2 | John | Add tracking headers | Added to all endpoints | ✅ Done |
| Day 2 | John | Remove template_version from POST | Removed | ✅ Done |
| Day 2 | John | Clean up documentation | Created architecture docs | ✅ Done |
| Day 2 | John | Show looping operation | Created flow diagrams | ✅ Done |
| Day 2 | John | Make useful for developers | In progress | ⚠️ Ongoing |

---

## John's Questions About Variables/Fields

These are specific questions John raised about fields and variables that need clarification or documentation:

### Day 1 - Header Parameters

| Question | Context | Action Required |
|----------|---------|-----------------|
| "What is that ID for?" | About `trace_id` header parameter | Research and document the purpose of trace_id |
| "There's a trace ID, but what does that mean?" | Header parameters for tracking | Define in data dictionary |
| "Is that the version of the API call? Is it the version of the trace ID?" | About `X-version` header | Clarify X-version purpose |
| "Is it part of customer 365?" | Header parameter origin | Research if it's a C365 standard |

**John's directive:** "So let's research that to find out exactly what it is for. Otherwise, how do you implement it correctly?"

### Day 1 - Template & Communication Fields

| Question | Context | Action Required |
|----------|---------|-----------------|
| "We also talked about adding that field for the template type, like if it was a letter, e-mail, push, SMS" | Template filtering | Add `communication_type` field to template table |
| "You need to have a filter condition so when you get it, you don't get a mixture of different types of templates" | GET templates endpoint | Add `communication_type` query parameter |
| "That vendor needs to be also categorized by the communication type" | Vendor validation | Group vendors by communication type in config |

### Day 1 - Vendor Configuration

| Question | Context | Action Required |
|----------|---------|-----------------|
| "Stop that vendor. That's not a enumeration or anything, right?" | Vendor field type | Confirm vendor is string (not enum) |
| "There could be other ones like Salesforce and stuff that could be the vendor. We don't want to restrict it" | Vendor flexibility | Keep vendor as configurable string |
| "If you're going to put a check, that vendor needs to be also categorized by the communication type" | Vendor validation | Map vendors to communication types |

**John's key point:** "For letters you might have Smartcom, Send as Handlebars. For emails you might have Salesforce. But you'll never have Salesforce for a letter."

### Day 1 - Soft Delete Cascade

| Question | Context | Action Required |
|----------|---------|-----------------|
| "Don't forget, since we're doing soft deletes, if you delete a template, they need to make sure you also soft delete all the vendors assigned to that template" | Template deletion | Implement cascade soft delete |

### Day 1 - Line of Business vs Sharing Scope Clarification

John clarified the relationship between `line_of_business` and `sharing_scope`:

| John's Question | Clarification |
|-----------------|---------------|
| "So how is sharing scope different than line of business then?" | They are separate concepts and should not be conflated |
| "It sounds like the same thing" | Taher was incorrectly using sharing_scope values (ALL, ACCOUNT_TYPE) as line of business values |
| "Your sharing scope ALL is like enterprise" | `sharing_scope=ALL` means "shared with everyone" (like enterprise-wide) |
| "I thought originally sharing scope was who could access the file" | Sharing scope should define access level, not line of business |

**John's Key Clarifications:**

1. **Line of Business values:** `CREDIT_CARD`, `DIGITAL_BANK`, `ENTERPRISE` (enterprise means ALL lines of business)
2. **Sharing Scope values:** Should define WHO can access:
   - `NULL` = Account-specific (only that account)
   - `ALL` = Everyone (enterprise-wide access)
   - `CUSTOM_RULES` = Complex eligibility rules

**Note:** `ACCOUNT_TYPE` was removed from sharing_scope as it is redundant with `line_of_business` filtering.

**John's directive:** "It needs to be defined what it is and it shouldn't be something that we already have like line of business."

**Implementation:**
- `line_of_business` is applied in STEP 1: filters which templates to load
- `sharing_scope` is applied in STEP 2: determines who can access within those templates
- These are two separate, sequential filters

### Day 2 - Document Inquiry Fields

| Question | Context | Action Required |
|----------|---------|-----------------|
| "What is the domain ID? Where do we get that information from?" | Data extraction config | Document domain_id source (Accounts team API) |
| "What does the regulatory flag signify?" | Template metadata | Document: regulatory=true bypasses do-not-contact |
| "There's a message center doc flag that has to be true" | Template filtering | Filter documents by `message_center_doc_flag` |
| "Why are you filtering on [template type]?" | Storage index query | Clarify filtering strategy |

**John's explanation of regulatory flag:** "If we go to print this and they're in a do-not-contact state, regulatory still has to get printed. Where if regulatory is false and they have do-not-contact, then we don't actually print it."

### Day 2 - Effective Date Handling

| Question | Context | Action Required |
|----------|---------|-----------------|
| "What if it returns more than one document?" | Date range filtering | Handle multiple valid documents |
| "I didn't see a date range. I only seen one date" | Effective date fields | Implement `valid_from` and `valid_until` range |
| "What do you do when you get more than one returned?" | Future documents | Return only currently effective document |

**John's scenario:** "They have another privacy statement that's just got. They want it to go into production two months from now, so they add it to the document. That effective date is later than the effective date for the one they're currently using. But they don't want to use it yet."

### Day 2 - HATEOAS Links

| Question | Context | Action Required |
|----------|---------|-----------------|
| "What's the Hatos links?" | API response links | Document HATEOAS implementation |
| "You're retrieving the links, but you don't have a lifetime object. How do they know how long that link's good for?" | Link expiration | Add expiration timestamp to links |
| "There should be no delete function in the get inquiry" | Web inquiry response | Remove delete link from inquiry response |

### Day 2 - Access Control

| Question | Context | Action Required |
|----------|---------|-----------------|
| "It looks like you're just giving all the actions every time, which doesn't match what we have in our metadata for the template" | Template access control | Filter actions based on template metadata |

**John's directive:** "You should only be providing these items here [links] that match that metadata that we have."

---

## Document Enquiry Filters - Complete Reference

Based on John's feedback, here are all the filters that should be applied in the document enquiry flow:

### Filter Application Order

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 1: TEMPLATE FILTERING (which templates to consider)                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ 1. line_of_business    - CREDIT_CARD, DIGITAL_BANK, or ENTERPRISE          │
│ 2. template_type       - Letter, Email, SMS, Push (communication type)     │
│ 3. message_center_doc_flag - Must be true for web/message center display   │
│ 4. active_flag         - Only active templates                              │
│ 5. accessible_flag     - Only accessible templates                          │
│ 6. start_date/end_date - Template validity period                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 2: ACCESS CONTROL (who can access within filtered templates)          │
├─────────────────────────────────────────────────────────────────────────────┤
│ 7. sharing_scope       - NULL (account-specific), ALL, CUSTOM_RULES        │
│ 8. eligibility_criteria - Custom rules evaluation (for CUSTOM_RULES)       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ STEP 3: DOCUMENT FILTERING (which documents within matched templates)       │
├─────────────────────────────────────────────────────────────────────────────┤
│ 9.  accountId          - Filter by account(s)                               │
│ 10. customerId         - Filter by customer                                 │
│ 11. reference_key      - Match by disclosure code, etc.                    │
│ 12. valid_from/valid_until - Document validity dates                       │
│ 13. postedFromDate/postedToDate - Document creation date range             │
│ 14. documentTypeCategoryGroup - Filter by template_type + category         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Filter Details

| # | Filter | Location | Values/Type | Status | John's Feedback |
|---|--------|----------|-------------|--------|-----------------|
| 1 | `line_of_business` | Template | `CREDIT_CARD`, `DIGITAL_BANK`, `ENTERPRISE` | ✅ Implemented | "Line of business is enterprise, credit card, digital banking" |
| 2 | `template_type` | Template | `Letter`, `Email`, `SMS`, `Push` | ⚠️ Needs filter | "Add filter so you don't get mixture of different types" |
| 3 | `message_center_doc_flag` | Template | Boolean | ❌ Not implemented | "Make sure document is assigned to message center" |
| 4 | `active_flag` | Template | Boolean | ✅ Implemented | - |
| 5 | `accessible_flag` | Template | Boolean | ⚠️ Partial | Should be filtered in query |
| 6 | `start_date`/`end_date` | Template | Epoch time | ✅ Implemented | - |
| 7 | `sharing_scope` | Template | `NULL`, `ALL`, `CUSTOM_RULES` | ✅ Implemented | Separate from line_of_business |
| 8 | `eligibility_criteria` | Template config | JSON rules | ✅ Implemented | - |
| 9 | `accountId` | Request | UUID[] | ✅ Implemented | - |
| 10 | `customerId` | Request | UUID | ⚠️ Partial | In request, not fully used |
| 11 | `reference_key` | Request/Extracted | String | ✅ Implemented | Used for document matching |
| 12 | `valid_from`/`valid_until` | Document metadata | Date | ✅ Implemented | "Check effective date range" |
| 13 | `postedFromDate`/`postedToDate` | Request | Epoch | ❌ Not implemented | Filter by doc creation date |
| 14 | `documentTypeCategoryGroup` | Request | Array | ❌ Not implemented | Filter by type + category |

### Special Flags

| Flag | Purpose | John's Explanation |
|------|---------|-------------------|
| `regulatory_flag` | Bypass do-not-contact | "If regulatory is true, it still has to get printed even if do-not-contact" |
| `message_center_doc_flag` | Web display eligibility | "Make sure document is actually assigned to the message center" |
| `notification_needed` | Trigger notifications | Customer notification required |

### template_type Values (Communication Type)

John clarified that `template_type` represents the communication channel:

| template_type | Description | Example Vendors |
|---------------|-------------|-----------------|
| `Letter` | Physical mail | Smartcom, Handlebars |
| `Email` | Electronic mail | Salesforce |
| `SMS` | Text message | Twilio, etc. |
| `Push` | Push notification | Firebase, etc. |

**John's key point:** "For letters you might have Smartcom. For emails you might have Salesforce. But you'll never have Salesforce for a letter."

---

## Detailed Action Items from Transcripts

### Day 1 - API Specification Actions

| # | Action | Owner | Status | Source |
|---|--------|-------|--------|--------|
| 1 | Research trace_id and X-version header purpose | Team | ⚠️ Pending | John's question |
| 2 | Add trace_id and X-version to data dictionary | Team | ⚠️ Pending | John's requirement |
| 3 | Add communication_type field to template table | Taher | ⚠️ Pending | John's request |
| 4 | Add communication_type filter to GET templates | Jagadeesh | ⚠️ Pending | John's request |
| 5 | Categorize vendors by communication type | Team | ⚠️ Pending | John's request |
| 6 | Implement soft delete cascade for vendors | Jagadeesh | ⚠️ Pending | John's requirement |
| 7 | Decide on copy function for vendor mappings | Team | ❌ Not needed | John talked himself out of it |

### Day 1 - Document Inquiry Actions

| # | Action | Owner | Status | Source |
|---|--------|-------|--------|--------|
| 8 | Separate queries for account-specific vs shared docs | Taher | ⚠️ Pending | John's architecture feedback |
| 9 | Add message_center_doc_flag filter | Team | ⚠️ Pending | John's requirement |
| 10 | Filter actions based on template access control | Team | ⚠️ Pending | John's requirement |
| 11 | Add link expiration timestamp to HATEOAS links | Team | ⚠️ Pending | John's requirement |
| 12 | Remove delete link from inquiry response | Team | ⚠️ Pending | John's requirement |

### Day 2 - Data Extraction Actions

| # | Action | Owner | Status | Source |
|---|--------|-------|--------|--------|
| 13 | Document domain_id source and purpose | Team | ⚠️ Pending | John's question |
| 14 | Implement effective date range filtering | Taher | ✅ Done (on branch) | John's scenario |
| 15 | Create flow/sequence diagram for rules engine | Taher | ✅ Done | John's request |
| 16 | Show looping operation in documentation | Taher | ✅ Done | John's primary feedback |

---

## Appendix: Original Transcript References

### Day 1 Key Quote
> "When you download this file, you need to get that same name." - Taher

### Day 1 Key Quotes from John

> "What is that ID for? There's a trace ID, but what does that mean?"

> "We don't want to add stuff that we don't know what they're actually for. Because you're gonna have to add this to the data dictionary."

> "So let's research that to find out exactly what it is for. Otherwise, how do you implement it correctly?"

> "We also talked about adding that field for the template type, like if it was a letter, e-mail, push, SMS."

> "That vendor needs to be also categorized by the communication type."

> "Don't forget, since we're doing soft deletes, if you delete a template, they need to make sure you also soft delete all the vendors assigned to that template."

> "There are two separate operations, one to get the one to one assigned documents. And then two, to get the list of the shared documents and then see if this person has any of those."

> "You're not looking for every active template in the bloody database, right? You're looking for the ones that this person necessarily has."

> "You don't show a loop operation going on here."

### Day 2 Key Quotes from John

> "I have added these two header parameters for all the requests... for the tracking purpose." - Jagadeesh

> "It's a start, but it needs to be cleaned up and then show the entire looping operation because... if you tried to give this to a developer, they wouldn't be able to do anything with it." - John Drum

> "We need to make it useful." - John Drum

> "Focus on the things that they need to be able to continue work." - John Drum

> "What is the domain ID? Where do we get that information from?"

> "What does the regulatory flag signify? If we go to print this and they're in a do-not-contact state, regulatory still has to get printed."

> "You're retrieving the links, but you don't have a lifetime object. So how do they know how long that link's good for?"

> "It looks like you're just giving all the actions every time, which doesn't match what we have in our metadata for the template."
