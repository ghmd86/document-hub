# SME Meeting - Legacy System Discovery Questionnaire

## Meeting Objective
Understand how the legacy ECMS stores and retrieves shared documents to inform the design of the new Document Hub system.

**Date**: _____________
**Attendees**: _____________
**Duration**: 90-120 minutes

---

## Table of Contents

1. [System Overview & Architecture](#1-system-overview--architecture)
2. [Shared Document Storage Patterns](#2-shared-document-storage-patterns)
3. [Document Types & Categories](#3-document-types--categories)
4. [Data Volume & Scale](#4-data-volume--scale)
5. [Retrieval Patterns & Performance](#5-retrieval-patterns--performance)
6. [Business Rules & Workflows](#6-business-rules--workflows)
7. [Customer/Account Association](#7-customeraccount-association)
8. [Timeline & Versioning](#8-timeline--versioning)
9. [Search & Filtering](#9-search--filtering)
10. [Security & Access Control](#10-security--access-control)
11. [Integration Points](#11-integration-points)
12. [Pain Points & Challenges](#12-pain-points--challenges)
13. [Future Requirements](#13-future-requirements)
14. [Data Migration](#14-data-migration)

---

## 1. System Overview & Architecture

### Current System Architecture

**Q1.1:** What is the current ECMS platform/technology being used?
- Vendor/product name?
- Version?
- On-premise or cloud?

**Q1.2:** How are documents currently stored?
- File system, S3, database BLOBs, or other?
- Storage location(s)?
- File organization structure?

**Q1.3:** What database technology is used for metadata?
- Relational (Oracle, SQL Server, PostgreSQL)?
- NoSQL (MongoDB, DynamoDB)?
- Schema structure overview?

**Q1.4:** How many environments do you have?
- Dev, QA, Staging, Production?
- Data sync between environments?

---

## 2. Shared Document Storage Patterns

### Current Implementation

**Q2.1:** How are "shared documents" currently stored in the legacy system?

**Option A: Duplicated Storage**
- [ ] One copy per customer (duplicated)
- If yes: How many duplicate copies for a typical privacy policy?
- Storage impact of duplication?

**Option B: Single Storage with Mapping**
- [ ] One copy with customer associations
- If yes: How is the mapping stored?
- Mapping table structure?

**Option C: Hybrid Approach**
- [ ] Different approaches for different document types?
- What determines which approach is used?

**Q2.2:** What types of documents are considered "shared" today?
- Privacy policies?
- Terms of service?
- Regulatory notices?
- Marketing materials?
- Disclosures?
- Other?

**Q2.3:** How do you distinguish between customer-specific and shared documents?
- Is there a flag/indicator field?
- Naming convention?
- Separate tables/buckets?
- Template-driven?

**Q2.4:** Can you show us examples of:
- A shared document record in the database?
- A customer-specific document record?
- The mapping/association table (if exists)?

---

## 3. Document Types & Categories

### Document Classification

**Q3.1:** How many unique document types exist in the system?
- Total count?
- List of top 20 most common types?

**Q3.2:** How many shared document types are there?
- Privacy policies: ___
- Regulatory notices: ___
- Disclosures: ___
- Marketing materials: ___
- Other: ___

**Q3.3:** How are document types categorized?
- Hierarchical (category > type > subtype)?
- Flat structure?
- Industry-standard taxonomy?

**Q3.4:** Are document templates used?
- How many templates?
- How are they versioned?
- Who manages template changes?

---

## 4. Data Volume & Scale

### Current Data Volumes

**Q4.1:** Total number of documents in the system?
- Customer-specific: ___
- Shared: ___
- Total: ___

**Q4.2:** How many customers in the system?
- Active customers: ___
- Total (including inactive): ___

**Q4.3:** Average documents per customer?
- Median: ___
- 95th percentile: ___
- Max for a single customer: ___

**Q4.4:** For shared documents specifically:
- Total unique shared documents: ___
- Average customers per shared document: ___
- Largest shared document distribution (e.g., privacy policy to how many customers): ___

**Q4.5:** Document growth rate?
- Documents created per month: ___
- Annual growth percentage: ___
- Seasonal variations?

**Q4.6:** Storage footprint?
- Total storage used: ___
- Average document size: ___
- Largest document size: ___

---

## 5. Retrieval Patterns & Performance

### How Documents Are Retrieved

**Q5.1:** What are the most common document retrieval queries?

**By Customer:**
- "Get all documents for customer X"
  - How often: ___ times/day
  - Average response time: ___ ms
  - Includes shared docs automatically? Yes/No

**By Account:**
- "Get all documents for account Y"
  - How often: ___ times/day
  - Average response time: ___ ms

**By Document Type:**
- "Get all LOAN_APPLICATION documents for customer X"
  - How often: ___ times/day
  - Average response time: ___ ms

**By Date Range:**
- "Get documents for customer X between dates"
  - How often: ___ times/day
  - Average response time: ___ ms

**Shared Document Queries:**
- "Get all shared documents applicable to customer X"
  - How is this currently done?
  - Performance issues?

**Q5.2:** What is the current query performance?
- Average query time: ___ ms
- 95th percentile: ___ ms
- 99th percentile: ___ ms
- Slowest queries (> 1 second)? Examples?

**Q5.3:** Are there performance issues today?
- During peak hours?
- With specific query types?
- Database timeouts?

**Q5.4:** What are the peak usage times?
- Time of day: ___
- Day of week: ___
- Month end/quarter end spikes?

**Q5.5:** Concurrent users?
- Average concurrent users: ___
- Peak concurrent users: ___
- Max requests per second: ___

---

## 6. Business Rules & Workflows

### Document Lifecycle

**Q6.1:** What is the lifecycle of a shared document?

**Creation:**
- Who creates shared documents?
- Approval process?
- How long does approval take?

**Publication:**
- How is a shared document "published"?
- Effective date management?
- Notification process?

**Distribution:**
- How are customers notified of new shared documents?
- Automatic assignment rules?
- Manual assignment process?

**Expiration:**
- Do shared documents expire?
- What happens when they expire?
- Archival process?

**Q6.2:** Are there different types of shared document scopes?

**All Customers:**
- [ ] Documents that apply to all customers
- Examples: ___
- How many of these: ___

**Customer Segment:**
- [ ] Documents for specific customer segments (e.g., "California residents")
- Examples: ___
- How are segments defined?
- How many segments: ___

**Account Type:**
- [ ] Documents for specific account types (e.g., "Mortgage customers")
- Examples: ___
- How many account types: ___

**Specific Customer List:**
- [ ] Documents for explicit customer lists
- Examples: ___
- How are lists managed?
- Average list size: ___

**Q6.3:** Can a shared document have an effective date range?
- Start date (effective from)?
- End date (effective to)?
- How is this used?

**Q6.4:** What happens when a shared document is updated?
- New version created?
- Old version archived?
- How do customers see version history?

---

## 7. Customer/Account Association

### How Customers Are Linked to Shared Documents

**Q7.1:** How is the association between customers and shared documents managed?

**If All Customers:**
- Is there a record for each customer? (1M customers = 1M records?)
- Or is it query logic only? (No records, just filtered in query?)

**If Specific Customers:**
- Mapping table structure?
- Fields in mapping table?
- When are mappings created?
- Who creates mappings?

**Q7.2:** Can a customer be explicitly excluded from a shared document?
- Even if they match the criteria (e.g., account type)?
- How is exclusion tracked?

**Q7.3:** Can shared documents be associated at different levels?
- Customer level?
- Account level?
- Other levels?

**Q7.4:** What metadata is tracked for the association?
- Date assigned?
- Assigned by (user)?
- Reason/notes?
- Acknowledgment/viewed status?

---

## 8. Timeline & Versioning

### Time-Based Queries

**Q8.1:** Do you need to query documents "as of a specific date"?
- Example: "What documents applied to customer X on June 15, 2023?"
- How often is this needed?
- Use cases?

**Q8.2:** Are historical versions of shared documents kept?
- How long are they retained?
- Can customers access old versions?
- Audit requirements?

**Q8.3:** When a new version of a shared document is published:
- Does it replace the old version immediately?
- Is there a transition period?
- Can both versions coexist?

**Q8.4:** Are there compliance requirements for document retention?
- Minimum retention period?
- Maximum retention period?
- Regulatory requirements (SOX, GDPR, etc.)?

---

## 9. Search & Filtering

### Search Capabilities

**Q9.1:** What search capabilities exist today?

**Search by:**
- [ ] Customer ID/Name
- [ ] Account ID/Number
- [ ] Document name/title
- [ ] Document type
- [ ] Document category
- [ ] Date range
- [ ] Full-text search in content
- [ ] Tags/keywords
- [ ] Metadata fields
- [ ] Other: ___

**Q9.2:** Which search queries are most common?
- Top 5 search patterns?
- Performance of each?

**Q9.3:** Do users search across both customer-specific and shared documents?
- How is this currently handled?
- Are results merged?

**Q9.4:** Are there search performance issues?
- Slow searches?
- Timeout issues?
- Index problems?

---

## 10. Security & Access Control

### Security & Permissions

**Q10.1:** Who can access shared documents?

**Customers:**
- Can customers see all shared documents that apply to them?
- Are some shared documents internal-only?

**Employees:**
- Which roles can access shared documents?
- Read-only vs. read-write access?

**Administrators:**
- Who can create/publish shared documents?
- Approval workflows?

**Q10.2:** Are there different security classifications?
- Public?
- Confidential?
- Restricted?
- How is this enforced?

**Q10.3:** Are there audit requirements?
- Document access logging?
- Change tracking?
- Compliance reporting?

**Q10.4:** Are documents encrypted?
- At rest?
- In transit?
- Encryption keys management?

**Q10.5:** PII (Personally Identifiable Information) in documents?
- How is PII handled?
- Redaction requirements?
- Data privacy regulations (GDPR, CCPA)?

---

## 11. Integration Points

### System Integrations

**Q11.1:** What systems integrate with the document system?

**Upstream (provides data):**
- Customer management system?
- Account management system?
- Template management system?
- Document generation system?

**Downstream (consumes data):**
- Customer portal?
- Mobile app?
- Contact center application?
- Reporting/BI tools?

**Q11.2:** How do integrations retrieve shared documents?
- API calls?
- Database queries?
- File transfers?
- Message queues?

**Q11.3:** What are the SLAs for these integrations?
- Response time requirements?
- Availability requirements?
- Data freshness requirements?

**Q11.4:** Are there batch processes?
- Nightly jobs?
- Monthly processes?
- What do they do?

---

## 12. Pain Points & Challenges

### Current Problems

**Q12.1:** What are the biggest pain points with the current system?

**Performance:**
- Slow queries?
- Timeouts?
- Peak hour issues?

**Storage:**
- Running out of space?
- Costly storage?
- Duplicate data waste?

**Operational:**
- Difficult to maintain?
- Hard to add new features?
- Complex deployment?

**User Experience:**
- Poor search?
- Difficult navigation?
- Missing features?

**Q12.2:** What breaks or fails regularly?
- Common errors?
- System outages?
- Data consistency issues?

**Q12.3:** What manual workarounds exist?
- What should be automated?
- How much time do workarounds take?

**Q12.4:** What complaints do you hear from users?
- Customers?
- Internal users?
- Support team?

**Q12.5:** What features are missing today?
- Most requested features?
- Competitive gaps?

---

## 13. Future Requirements

### Planned Enhancements

**Q13.1:** What new features are needed for shared documents?

**Distribution:**
- [ ] Automated assignment based on rules
- [ ] Self-service document access for customers
- [ ] Email notifications for new documents
- [ ] Push notifications (mobile)

**Management:**
- [ ] Bulk operations (assign to 10,000 customers at once)
- [ ] Document workflow/approval process
- [ ] A/B testing for different document versions
- [ ] Multi-language support

**Analytics:**
- [ ] Document view tracking
- [ ] Customer acknowledgment tracking
- [ ] Compliance reporting
- [ ] Usage analytics

**Q13.2:** Expected growth in next 3 years?
- Customer growth: ___% per year
- Document volume growth: ___% per year
- New document types: ___

**Q13.3:** Are there planned regulatory changes?
- New compliance requirements?
- Industry standards?
- Data residency requirements?

**Q13.4:** Future integrations planned?
- New systems to integrate?
- API requirements?

---

## 14. Data Migration

### Migration from Legacy System

**Q14.1:** How much historical data needs to be migrated?
- All documents or just recent?
- How many years of history?
- Total data volume to migrate: ___

**Q14.2:** What is the data quality like?
- Incomplete records?
- Inconsistent data?
- Orphaned records?
- Duplicate data?

**Q14.3:** Are there data cleansing needs?
- What needs to be cleaned?
- Who will clean the data?

**Q14.4:** Migration timeline expectations?
- When should migration happen?
- Acceptable downtime?
- Phased migration or big bang?

**Q14.5:** Migration validation?
- How to verify migration success?
- Reconciliation process?
- Rollback plan?

**Q14.6:** Can the legacy system run in parallel?
- Dual-run period?
- Data sync requirements during transition?

---

## 15. Technical Deep Dive

### Database & Schema

**Q15.1:** Can you provide:
- [ ] Sample database schema (ERD)
- [ ] Example SQL queries for shared documents
- [ ] Sample data records (sanitized)
- [ ] API documentation (if available)

**Q15.2:** What are the key tables?
- Documents table structure?
- Customer table structure?
- Mapping/association table structure?
- Template table structure?

**Q15.3:** What indexes exist?
- On which fields?
- Composite indexes?
- Index performance?

**Q15.4:** Are there stored procedures or functions?
- For retrieving shared documents?
- For assignment logic?
- Can we review them?

**Q15.5:** How is data partitioned (if at all)?
- Table partitioning strategy?
- Benefits observed?

---

## 16. Specific Scenarios

### Walk Through Real Examples

**Scenario 1: New Privacy Policy**

**Q16.1:** Walk us through creating a new privacy policy that applies to all customers:
1. Who initiates?
2. What system is used?
3. Approval steps?
4. How is it published?
5. How do customers become aware?
6. How long does the whole process take?

**Scenario 2: State-Specific Regulatory Notice**

**Q16.2:** Walk us through creating a regulatory notice for California customers only:
1. How do you identify California customers?
2. How is the document assigned to them?
3. What if a customer moves to California later?
4. What if they move out of California?

**Scenario 3: Customer Portal Access**

**Q16.3:** Walk us through a customer logging in and viewing their documents:
1. What query runs?
2. How are shared documents included?
3. How are they sorted/displayed?
4. Can customer filter shared vs. personal documents?

**Scenario 4: Expiring Document**

**Q16.4:** Walk us through a document that expires:
1. What triggers expiration?
2. What happens to customer access?
3. Is it archived or deleted?
4. Can it be retrieved later?

---

## 17. Success Metrics

### Current KPIs

**Q17.1:** How do you measure success today?
- Document retrieval speed?
- System uptime?
- Storage costs?
- User satisfaction?

**Q17.2:** What are acceptable performance metrics?
- Query response time: ___ ms
- System availability: ___ %
- Document upload success rate: ___ %

**Q17.3:** What would make the new system a success?
- Performance improvements?
- Cost reductions?
- Feature additions?
- User satisfaction improvements?

---

## 18. Open Discussion

### Additional Questions

**Q18.1:** Is there anything we haven't asked about that we should know?

**Q18.2:** Are there any unique or unusual aspects of your shared document handling?

**Q18.3:** Are there any "gotchas" or edge cases we should be aware of?

**Q18.4:** Who else should we talk to?
- Business stakeholders?
- Technical team members?
- End users?

**Q18.5:** Can we schedule follow-up sessions if needed?

**Q18.6:** Can we get access to:
- Test environment?
- Sample data?
- Documentation?
- Architecture diagrams?

---

## Post-Meeting Action Items

### Follow-Up Tasks

**For SME Team:**
- [ ] Provide database schema export
- [ ] Share sample SQL queries
- [ ] Export sample data (anonymized)
- [ ] Schedule system demo/walkthrough
- [ ] Provide access to documentation
- [ ] Identify additional stakeholders

**For Design Team:**
- [ ] Analyze responses
- [ ] Document findings
- [ ] Create gap analysis (legacy vs. new system)
- [ ] Update design based on findings
- [ ] Prepare questions for follow-up
- [ ] Create data migration plan

---

## Appendix: Question Prioritization

### Critical (Must Answer)
- Q2.1 - How shared documents are stored
- Q4.1-Q4.4 - Data volumes
- Q5.1-Q5.2 - Retrieval patterns and performance
- Q6.2 - Shared document scopes
- Q7.1 - Customer association mechanism

### High Priority (Should Answer)
- Q3.1-Q3.2 - Document types
- Q5.3-Q5.5 - Performance issues and scale
- Q6.1 - Document lifecycle
- Q8.1 - Timeline queries
- Q12.1-Q12.2 - Pain points

### Medium Priority (Nice to Answer)
- Q9.1-Q9.2 - Search capabilities
- Q10.1-Q10.3 - Security
- Q11.1-Q11.2 - Integrations
- Q13.1 - Future requirements

### Low Priority (If Time Permits)
- Q14.1-Q14.6 - Migration details
- Q17.1-Q17.3 - Success metrics

---

## Tips for the Meeting

1. **Record the session** (with permission) - you'll miss details otherwise
2. **Bring a technical person** who can understand database/system details
3. **Ask for screen shares** - seeing the actual system is invaluable
4. **Get examples** - real data (anonymized) is better than descriptions
5. **Don't rush** - take time to understand complex areas
6. **Ask "why"** - understand the business reason behind technical decisions
7. **Take notes** on pain points - these drive the new design
8. **Get artifacts** - schemas, queries, diagrams, documentation
9. **Schedule follow-up** - you won't get everything in one session
10. **Confirm understanding** - repeat back what you heard

---

## Document Control

**Version**: 1.0
**Created**: [Date]
**Owner**: [Your Name]
**Reviewers**: [Team Members]
**Next Review**: After SME meeting
