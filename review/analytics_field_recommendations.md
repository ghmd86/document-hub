# Analytics Field Recommendations - Document Events Ledger

**Date:** 2025-11-13
**Based On:** John Drum's Schema Review
**Purpose:** Identify fields to include in analytics tables to support common queries without expensive joins

---

## John's Core Principle for Analytics Tables

> **"So at least the things you're going to put in your where clause, you want to put a fields in the table. And that way your your query is a lot less expensive."**
>
> — John Drum (00:36:49-00:37:03)

### Key Guidance from John

1. **Think About WHERE Clauses**
   - Any field you'll filter on should be in the table
   - Don't make analysts join 3 tables to answer basic questions

2. **Denormalize for Performance**
   > "If this is a analytic schema specifically for documents, you should denormalize it."

   - Copy data from related tables to avoid joins
   - Especially important when tables are in different databases

3. **Consider the Users**
   > "if I'm creating this and this is part of our platform and people from all different areas like risk or. Data warehouse or you know the business people and they want to analyze this data. What kind of data do I need to put in here and then how do they query it?"

   - Risk teams
   - Data warehouse teams
   - Business analysts
   - Website (performance critical)

4. **Include Enough Data to Be Useful**
   > "You got to have enough data to make it useful."

   - Don't create sparse tables that require complex joins
   - Self-contained tables that answer questions directly

---

## Current Schema Analysis

### ✅ Fields Already Included (Good!)

| Field | Why It's Good | John's Validation |
|-------|---------------|-------------------|
| `account_id` | Enables account-level filtering | "So you have what account it belongs to" |
| `event_type` | Enables filtering by event category | "So you have an event type, which is good" |
| `event_timestamp` | Time-based filtering and aggregation | Essential for all analytics |
| `template_type` | **DENORMALIZED** - Avoids join to template table | Recommended by John |
| `document_type` | **DENORMALIZED** - Identifies document without join | Recommended by John |
| `source_app` | Tracks which system created the event | Good for source analysis |
| `actor_id` | Identifies who performed action | Good for user behavior analytics |
| `correlation_id` | Links related events | Essential for lifecycle tracking |

---

## Common Analytics Questions (From John's Examples)

### Question 1: "How many accounts viewed a specific disclosure this month?"

**Current Query:**
```sql
SELECT COUNT(DISTINCT account_id)
FROM document_view_events
WHERE document_type = 'ANNUAL_DISCLOSURE'  -- ✅ Denormalized
  AND view_date >= '2025-11-01'
  AND view_date < '2025-12-01';
```

**Status:** ✅ Can be answered with current schema

**Fields Used in WHERE:**
- `document_type` (denormalized) ✅
- `view_date` ✅

---

### Question 2: "Show me all documents printed for account X this quarter"

**Current Query:**
```sql
SELECT *
FROM document_events_ledger
WHERE account_id = 'xxx'              -- ✅ In table
  AND event_type = 'PRINTED'          -- ✅ In table
  AND event_date >= '2025-10-01'      -- ✅ In table
  AND event_date < '2026-01-01';
```

**Status:** ✅ Can be answered with current schema

---

### Question 3: "Which template types are most popular by customer segment?"

**Without Additional Fields:**
```sql
-- ❌ Requires join to customer/account table
SELECT template_type, COUNT(*)
FROM document_events_ledger del
JOIN accounts a ON del.account_id = a.id
JOIN customers c ON a.customer_id = c.id
WHERE c.segment = 'PREMIUM'
GROUP BY template_type;
```

**Status:** ⚠️ **MISSING FIELD** - Customer segment not in analytics table

---

## 🔍 Recommended Additional Fields

Based on John's guidance and common analytics patterns, here are fields to consider adding:

---

### HIGH PRIORITY - Recommended to Add

#### 1. **customer_id** (or customer_key)

**Why:**
> "they know it was this customer, but that customer's got five accounts. So do they they really have to go through and figure out which account was associated to this particular document?"
> — John Drum (00:33:17-00:33:21)

**Purpose:**
- Customer-level analytics (customers may have multiple accounts)
- Avoid join to account table
- Support queries like "Show all activity for customer X across all accounts"

**Example Usage:**
```sql
-- All documents for a customer (across all their accounts)
SELECT COUNT(*)
FROM document_events_ledger
WHERE customer_id = 'xxx'
  AND event_date >= '2025-11-01';
```

**Schema Change:**
```sql
ALTER TABLE document_events_ledger
ADD COLUMN customer_id UUID;

COMMENT ON COLUMN document_events_ledger.customer_id IS
'Customer identifier. Denormalized from account table. Customers may have multiple accounts.';

-- Add index
CREATE INDEX idx_document_events_ledger_customer_id ON document_events_ledger (customer_id);
```

---

#### 2. **document_name** or **template_name**

**Why:**
> "So there should be some kind of field from the template that gets put in here to say what this is. Like, was this a statement? Was it a shared document?"
> — John Drum (00:37:33-00:37:41)

**Purpose:**
- Human-readable document identification
- Easier reporting for non-technical users
- Avoid join to template table for name lookup

**Example Usage:**
```sql
-- Reports that are human-readable
SELECT
  document_name,
  COUNT(*) as views,
  COUNT(DISTINCT account_id) as unique_accounts
FROM document_view_events
WHERE view_date >= '2025-11-01'
GROUP BY document_name
ORDER BY views DESC;
```

**Schema Change:**
```sql
ALTER TABLE document_events_ledger
ADD COLUMN template_name VARCHAR(255);

COMMENT ON COLUMN document_events_ledger.template_name IS
'Denormalized template name for human-readable reporting (e.g., "Monthly Credit Card Statement", "Annual Privacy Notice")';
```

---

#### 3. **is_shared_document** (Boolean)

**Why:**
> "Like, was this a statement? Was it a shared document? You know, was it a regular document?"
> — John Drum (00:37:41-00:37:45)

**Purpose:**
- Distinguish between personal and shared documents
- Different analytics for shared vs. personal docs
- Shared docs have different view patterns (millions of views)

**Example Usage:**
```sql
-- Compare view patterns for shared vs personal
SELECT
  is_shared_document,
  AVG(view_count) as avg_views,
  COUNT(*) as document_count
FROM (
  SELECT storage_index_id, is_shared_document, COUNT(*) as view_count
  FROM document_view_events
  GROUP BY storage_index_id, is_shared_document
) subquery
GROUP BY is_shared_document;
```

**Schema Change:**
```sql
ALTER TABLE document_events_ledger
ADD COLUMN is_shared_document BOOLEAN DEFAULT FALSE;

ALTER TABLE document_view_events
ADD COLUMN is_shared_document BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN document_events_ledger.is_shared_document IS
'TRUE if document is shared across multiple accounts. Shared documents typically have much higher view counts.';
```

---

#### 4. **delivery_channel**

**Why:** Analytics teams often need to know HOW documents were delivered

**Purpose:**
- Track delivery method (email, postal mail, online portal)
- Support channel effectiveness analytics
- Compliance reporting by channel

**Example Usage:**
```sql
-- Channel effectiveness analysis
SELECT
  delivery_channel,
  COUNT(*) as documents_sent,
  COUNT(DISTINCT account_id) as unique_accounts
FROM document_events_ledger
WHERE event_type IN ('PRINTED', 'REPRINTED')
  AND event_date >= '2025-11-01'
GROUP BY delivery_channel;
```

**Schema Change:**
```sql
ALTER TABLE document_events_ledger
ADD COLUMN delivery_channel VARCHAR(50);

COMMENT ON COLUMN document_events_ledger.delivery_channel IS
'How the document was delivered: EMAIL, POSTAL_MAIL, ONLINE_PORTAL, VENDOR_HOV, VENDOR_BROADRIDGE, etc.';

-- Add index for filtering
CREATE INDEX idx_document_events_ledger_delivery_channel
  ON document_events_ledger (delivery_channel) WHERE delivery_channel IS NOT NULL;
```

---

### MEDIUM PRIORITY - Consider Adding

#### 5. **account_status** (at time of event)

**Why:** Risk and compliance teams often filter by account status

**Purpose:**
- Analyze behavior by account status (ACTIVE, CLOSED, SUSPENDED)
- Compliance reporting (e.g., "Documents sent to closed accounts")
- Historical analysis (status at time of event, not current status)

**Example Usage:**
```sql
-- Documents sent to closed accounts (potential issue)
SELECT COUNT(*)
FROM document_events_ledger
WHERE event_type = 'PRINTED'
  AND account_status = 'CLOSED'
  AND event_date >= '2025-11-01';
```

**Trade-off:**
- **PRO:** Avoids join to account table for status
- **CON:** Account status changes over time; snapshot at event time needed
- **CON:** Additional data to maintain

---

#### 6. **account_type** or **customer_segment**

**Why:** Business analytics often segment by account/customer type

**Purpose:**
- Customer segmentation analytics (PREMIUM, STANDARD, BASIC)
- Product analytics (CHECKING, SAVINGS, CREDIT_CARD)
- Targeted campaign analysis

**Example Usage:**
```sql
-- Template popularity by customer segment
SELECT
  customer_segment,
  document_type,
  COUNT(*) as count
FROM document_events_ledger
WHERE event_type = 'CREATED'
  AND event_date >= '2025-10-01'
GROUP BY customer_segment, document_type
ORDER BY customer_segment, count DESC;
```

---

#### 7. **business_unit** or **product_line**

**Why:** Enterprise analytics often need to attribute to business units

**Purpose:**
- P&L attribution
- Business unit performance
- Cross-sell analysis

**Example Usage:**
```sql
-- Documents by business unit
SELECT
  business_unit,
  COUNT(*) as document_count
FROM document_events_ledger
WHERE event_type = 'CREATED'
  AND event_date >= '2025-11-01'
GROUP BY business_unit;
```

---

#### 8. **regulatory_category**

**Why:** Compliance and legal teams need regulatory reporting

**Purpose:**
- Regulatory compliance reporting
- Audit trail by regulation type
- Retention policy enforcement

**Example Values:**
- REGULATORY_DISCLOSURE
- MARKETING
- TRANSACTIONAL
- PRIVACY_NOTICE
- COMPLIANCE_NOTICE

**Example Usage:**
```sql
-- All regulatory disclosures sent this quarter
SELECT COUNT(*)
FROM document_events_ledger
WHERE regulatory_category = 'REGULATORY_DISCLOSURE'
  AND event_type = 'PRINTED'
  AND event_date >= '2025-10-01';
```

---

### LOW PRIORITY - Nice to Have

#### 9. **print_vendor** (for print events)

**Why:** Operational analytics on vendor performance

**Purpose:**
- Vendor performance tracking
- Cost attribution
- SLA monitoring

**Note:** Could also be derived from `event_code` (e.g., PRINTED_TO_HOV) or stored in `event_data` JSONB

---

#### 10. **document_page_count**

**Why:** Cost analytics and volume planning

**Purpose:**
- Printing cost estimation
- Postage cost calculation
- Volume forecasting

---

#### 11. **language_code**

**Why:** Multi-language support analytics

**Purpose:**
- Language preference analytics
- Translation coverage tracking
- Regional analytics

---

## 🎯 Prioritization Framework

John's principle: **"The things you're going to put in your where clause, you want to put a fields in the table"**

### Questions to Ask for Each Field:

1. **Will this be used in WHERE clauses?** ✅ HIGH PRIORITY
2. **Does it avoid an expensive join?** ✅ HIGH PRIORITY
3. **Do multiple user groups need it?** ✅ MEDIUM PRIORITY
4. **Is it used in GROUP BY often?** ✅ MEDIUM PRIORITY
5. **Is it just for display (SELECT)?** → LOW PRIORITY (can join or lookup)

---

## 📊 Example Analytics Queries - Before and After

### Example 1: Customer-Level Analysis

**WITHOUT customer_id (requires join):**
```sql
-- ❌ Expensive join across tables
SELECT
  c.customer_name,
  COUNT(*) as document_count
FROM document_events_ledger del
JOIN accounts a ON del.account_id = a.id
JOIN customers c ON a.customer_id = c.id
WHERE del.event_date >= '2025-11-01'
GROUP BY c.customer_name;
```

**WITH customer_id (denormalized):**
```sql
-- ✅ Single table query
SELECT
  customer_id,
  COUNT(*) as document_count
FROM document_events_ledger
WHERE event_date >= '2025-11-01'
GROUP BY customer_id;
```

**Performance Impact:** 10-100x faster

---

### Example 2: Shared Document Analysis

**WITHOUT is_shared_document (requires complex query):**
```sql
-- ❌ Requires join to template table or storage index
SELECT
  del.storage_index_id,
  COUNT(*) as view_count
FROM document_view_events del
JOIN storage_index si ON del.storage_index_id = si.id
JOIN master_template_definition mtd ON si.master_template_id = mtd.id
WHERE mtd.is_shared = true
GROUP BY del.storage_index_id;
```

**WITH is_shared_document (denormalized):**
```sql
-- ✅ Direct filter
SELECT
  storage_index_id,
  COUNT(*) as view_count
FROM document_view_events
WHERE is_shared_document = true
GROUP BY storage_index_id;
```

---

### Example 3: Channel Effectiveness

**WITHOUT delivery_channel (requires parsing event_data or event_code):**
```sql
-- ❌ Complex JSONB query or pattern matching
SELECT
  event_data->>'delivery_method' as channel,
  COUNT(*) as count
FROM document_events_ledger
WHERE event_type = 'PRINTED'
GROUP BY event_data->>'delivery_method';
```

**WITH delivery_channel:**
```sql
-- ✅ Indexed column filter
SELECT
  delivery_channel,
  COUNT(*) as count
FROM document_events_ledger
WHERE event_type = 'PRINTED'
GROUP BY delivery_channel;
```

---

## 🔄 Implementation Strategy

### Phase 1: Immediate (Critical for Analytics)

Add these fields to both `document_events_ledger` and `document_view_events`:

```sql
-- Add customer_id
ALTER TABLE document_events_ledger ADD COLUMN customer_id UUID;
ALTER TABLE document_view_events ADD COLUMN customer_id UUID;

-- Add template_name
ALTER TABLE document_events_ledger ADD COLUMN template_name VARCHAR(255);
ALTER TABLE document_view_events ADD COLUMN template_name VARCHAR(255);

-- Add is_shared_document
ALTER TABLE document_events_ledger ADD COLUMN is_shared_document BOOLEAN DEFAULT FALSE;
ALTER TABLE document_view_events ADD COLUMN is_shared_document BOOLEAN DEFAULT FALSE;

-- Add delivery_channel (events table only)
ALTER TABLE document_events_ledger ADD COLUMN delivery_channel VARCHAR(50);

-- Add indexes
CREATE INDEX idx_document_events_ledger_customer_id ON document_events_ledger (customer_id);
CREATE INDEX idx_document_view_events_customer_id ON document_view_events (customer_id);
CREATE INDEX idx_document_events_ledger_delivery_channel ON document_events_ledger (delivery_channel) WHERE delivery_channel IS NOT NULL;
```

---

### Phase 2: Based on Analytics Team Feedback

After Phase 1 is deployed and used:

1. **Monitor slow query log**
   - Identify common JOIN patterns
   - Look for repeated GROUP BY fields

2. **Survey analytics users:**
   - Risk team: What queries are slow?
   - Data warehouse: What fields are commonly joined?
   - Business analysts: What reports take longest?

3. **Add fields based on actual usage patterns**

---

### Phase 3: Continuous Improvement

- Review pg_stat_statements quarterly
- Identify new denormalization opportunities
- Balance between table width and query performance

---

## ⚠️ Important Considerations

### Data Consistency

**Challenge:** Denormalized data can become stale

**Solution:**
- Populate at INSERT time from source tables
- Don't update denormalized fields later
- Analytics shows "state at time of event" (often desired for historical analysis)

---

### Table Width vs. Performance

**John's perspective:**
> "You should put more information in here rather than less"

**Trade-offs:**
- Wider rows = more disk space
- But: **Far cheaper than expensive JOINs**
- PostgreSQL handles wide rows well
- Analytics queries benefit massively

**Recommendation:** Favor denormalization for analytics tables

---

### JSONB vs. Dedicated Columns

**When to use JSONB (`event_data`):**
- Flexible, varying structure
- Rarely queried directly
- Full payload storage

**When to use dedicated columns:**
- **Frequently used in WHERE clauses** ✅
- Needs to be indexed
- Used in GROUP BY or aggregations
- John's principle: "things you're going to put in your where clause"

---

## 📋 Decision Checklist

Before adding a field to the analytics table, ask:

- [ ] Is this field used in WHERE clauses frequently?
- [ ] Does it avoid a JOIN to another table?
- [ ] Will multiple teams/users benefit from this?
- [ ] Is it used for filtering or grouping (not just display)?
- [ ] Will queries be significantly faster with this field?
- [ ] Can the source data be reliably populated at INSERT time?

If **3 or more are YES** → **Add the field**

---

## 🎓 Learning from John's Review

### The Exercise John Gave the Team

> "So go ahead and think about this table for a little bit. I just want you guys to go through the exercise. If you were going to use this table to do reporting or information on stuff, what fields would you need in there so that if you didn't have any other tables to go against and you only had this one? What would you need to do to do that?"
>
> — John Drum (00:39:04-00:39:22)

### How to Apply This:

1. **List common analytics questions**
2. **Write the queries using ONLY the analytics table**
3. **Identify what's missing**
4. **Add those fields (denormalized from source)**

---

## 📈 Success Metrics

After implementing recommended fields, measure:

| Metric | Before | Target After |
|--------|--------|--------------|
| Avg analytics query time | 5-30 seconds | < 500ms |
| Queries requiring JOINs | 80%+ | < 20% |
| Tables per typical query | 3-5 tables | 1-2 tables |
| Analyst satisfaction | Baseline | +50% |
| Dashboard load time | 10-60 seconds | < 3 seconds |

---

## 🚀 Recommended Next Steps

1. **Review current analytics queries**
   - Collect actual queries from users
   - Identify common JOIN patterns
   - Note fields used in WHERE/GROUP BY

2. **Implement Phase 1 fields**
   - customer_id
   - template_name
   - is_shared_document
   - delivery_channel

3. **Update application code**
   - Populate new fields at INSERT time
   - Test denormalization accuracy

4. **Monitor and iterate**
   - Track query performance improvements
   - Collect feedback from analytics users
   - Add Phase 2 fields based on actual usage

---

## 📖 Summary

### John's Key Message:

Analytics tables should be **self-contained** and **denormalized**. Include any field that will be used in WHERE clauses or GROUP BY operations. The cost of extra storage is **far less** than the cost of expensive JOINs, especially when:

- Tables are in different databases
- Queries run on websites (performance critical)
- Multiple teams need the same data
- Analytics is the primary purpose

### Recommended Additions:

**HIGH PRIORITY:**
- ✅ customer_id (customer-level analytics across accounts)
- ✅ template_name (human-readable reporting)
- ✅ is_shared_document (distinguish document types)
- ✅ delivery_channel (channel analytics)

**MEDIUM PRIORITY:**
- Consider based on your specific analytics needs:
  - account_status
  - customer_segment
  - business_unit
  - regulatory_category

**Implementation Principle:**
> "At least the things you're going to put in your where clause, you want to put a fields in the table."

---

**Document Version:** 1.0
**Last Updated:** 2025-11-13
**Next Review:** After Phase 1 implementation and 30 days of usage data

---

## Related Documents

- [Data Dictionary](data_dictionary.md) - Current schema documentation
- [Verification Report](verification_report.md) - John's requirements analysis
- [Implementation Summary](implementation_summary.md) - Implementation status
