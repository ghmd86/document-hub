# Interactive Template Builder - Concept Design

## The Problem

Non-technical users cannot:
- Write JSONPath expressions (`$.header.statementDate`)
- Understand data extraction configurations
- Define eligibility criteria in JSON format

## Solution: Multi-Modal Interactive Builder

### Approach 1: Interview-Style Wizard

A conversational interface that asks simple questions:

```
Step 1: Basic Information
━━━━━━━━━━━━━━━━━━━━━━━━━

Q: What type of document is this?
   ○ Statement (monthly bills, account summaries)
   ○ Agreement/Contract (terms, legal documents)
   ○ Tax Form (1099, W-2, etc.)
   ○ Regulatory (privacy policy, disclosures)
   ○ Other: [_______________]

Q: What business line does this belong to?
   ○ Credit Card
   ○ Mortgage/Home Loan
   ○ Banking
   ○ Investments
   ○ Insurance

                                    [Next →]
```

```
Step 2: Document Ownership
━━━━━━━━━━━━━━━━━━━━━━━━━━

Q: Who should see this document?

   ○ Specific account holders only
     (e.g., monthly statements for their account)

   ○ All customers
     (e.g., general terms applicable to everyone)

   ○ Customers meeting certain criteria
     (e.g., only platinum members, only CA residents)

                          [← Back]  [Next →]
```

```
Step 3: Field Selection (AI-Assisted)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Upload a sample document so we can detect extractable fields:

   [📎 Upload Sample Document]

   ─── OR ───

   Select from common fields for "Credit Card Statement":

   ☑ Statement Date
   ☑ Account Number (masked)
   ☑ Balance Due
   ☑ Minimum Payment
   ☑ Due Date
   ☐ Previous Balance
   ☐ Payments Received
   ☐ New Charges

   [+ Add Custom Field]

                          [← Back]  [Next →]
```

```
Step 4: Access Rules (Plain Language)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Q: Are there any conditions for who can see this document?

   ☑ Yes, there are conditions
   ○ No, anyone with access to the account can see it

   Define conditions (in plain language):
   ┌─────────────────────────────────────────────────────────┐
   │ Show this document only to customers who have           │
   │ "Platinum" or "Gold" membership tier                    │
   └─────────────────────────────────────────────────────────┘

   🤖 AI Translation:
   ┌─────────────────────────────────────────────────────────┐
   │ ✓ Check membership_tier from Credit Info API            │
   │ ✓ Allow if tier is "PLATINUM" or "GOLD"                │
   └─────────────────────────────────────────────────────────┘

                          [← Back]  [Next →]
```

```
Step 5: Review & Create
━━━━━━━━━━━━━━━━━━━━━━━━

Template Summary:
─────────────────
Name:           Platinum Card Agreement
Category:       Agreement/Contract
Business Line:  Credit Card
Visibility:     Conditional (Platinum/Gold members only)

Fields to Extract:
─────────────────
• Statement Date (required)
• Account Number (required)
• Membership Tier (required)

Access Rules:
─────────────────
• Check membership tier via Credit Info API
• Allow PLATINUM or GOLD tier only

                [← Back]  [Create Template ✓]
```

---

### Approach 2: AI Document Analyzer

Uses AI/LLM to analyze uploaded documents and auto-extract structure.

**How it works:**

1. User uploads sample PDF/image
2. AI (Claude/GPT) analyzes the document
3. AI identifies:
   - Document type
   - Key fields and their locations
   - Suggested extraction paths
4. User reviews and confirms

**Example AI Prompt:**

```
Analyze this document and identify:
1. What type of document is this?
2. What are the key data fields?
3. For each field, provide:
   - Field name
   - Sample value found
   - Data type (text, date, number, currency)
   - Is it required?

Return as structured JSON.
```

**AI Response:**

```json
{
  "document_type": "Credit Card Statement",
  "detected_fields": [
    {
      "name": "statement_date",
      "sample_value": "January 31, 2024",
      "data_type": "DATE",
      "required": true,
      "confidence": 0.95
    },
    {
      "name": "account_balance",
      "sample_value": "$1,234.56",
      "data_type": "CURRENCY",
      "required": true,
      "confidence": 0.98
    }
  ]
}
```

---

### Approach 3: Pre-Built Template Library

For common document types, provide ready-made templates:

```
┌─────────────────────────────────────────────────────────────────┐
│  📚 Template Library                                            │
│                                                                  │
│  Credit Card                                                     │
│  ├── Monthly Statement ............................ [Use]       │
│  ├── Terms and Conditions ......................... [Use]       │
│  ├── Cardholder Agreement ......................... [Use]       │
│  └── Annual Fee Disclosure ........................ [Use]       │
│                                                                  │
│  Mortgage                                                        │
│  ├── Monthly Statement ............................ [Use]       │
│  ├── Escrow Analysis .............................. [Use]       │
│  └── Annual Tax Summary ........................... [Use]       │
│                                                                  │
│  Tax Documents                                                   │
│  ├── 1099-INT (Interest Income) ................... [Use]       │
│  ├── 1099-DIV (Dividends) ......................... [Use]       │
│  └── 1098 (Mortgage Interest) ..................... [Use]       │
│                                                                  │
│  [+ Create Custom Template]                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Implementation Options

### Option A: Simple Web Form (Low Effort)

- Static HTML form with dropdowns
- Pre-defined field options per document type
- Generates CSV/SQL on submit
- **Effort:** 1-2 days

### Option B: React Wizard (Medium Effort)

- Step-by-step guided flow
- Dynamic field suggestions
- Preview before submit
- **Effort:** 1 week

### Option C: AI-Powered Builder (Higher Effort)

- Document upload and analysis
- LLM integration for field detection
- Natural language eligibility rules
- **Effort:** 2-3 weeks

---

## Recommended Approach for POC

**Hybrid: Template Library + Simple Wizard**

1. Pre-build common templates (statements, tax forms, agreements)
2. Simple web form for customization
3. Plain-language eligibility builder
4. Auto-generate technical config

**Benefits:**
- 80% of use cases covered by library
- Non-technical users can customize
- No JSONPath knowledge required
- Can add AI later as enhancement

---

## Technical Implementation Notes

### Field Path Generation

Instead of asking for JSONPath, use:

1. **Pre-mapped fields** per document type:
   ```
   CREDIT_CARD_STATEMENT:
     statement_date -> $.header.statementDate
     balance -> $.summary.currentBalance
     due_date -> $.summary.paymentDueDate
   ```

2. **Visual field picker** (for complex documents):
   - Upload sample document
   - Click on field to select
   - System records position/label

3. **AI extraction** (for new document types):
   - Upload sample
   - AI detects and names fields
   - System generates paths

### Eligibility Rules Builder

Natural language → Technical config translation:

| User Input | Generated Config |
|------------|------------------|
| "Only platinum members" | `{"field": "membership_tier", "operator": "EQUALS", "values": ["PLATINUM"]}` |
| "Customers in CA, TX, or NY" | `{"field": "state", "operator": "IN", "values": ["CA", "TX", "NY"]}` |
| "Balance over $10,000" | `{"field": "balance", "operator": "GREATER_THAN", "values": ["10000"]}` |

---

## Next Steps

1. **Define scope** - Which approach fits timeline/resources?
2. **Build template library** - Pre-configure common document types
3. **Create simple form** - Web-based template creator
4. **Add AI later** - Enhance with document analysis
