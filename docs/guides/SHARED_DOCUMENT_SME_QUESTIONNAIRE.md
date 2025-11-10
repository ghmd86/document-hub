# Shared Document SME Questionnaire

**Purpose:** Interview guide for gathering shared document eligibility requirements from Subject Matter Experts
**Duration:** 30-45 minutes per document type
**Participants:** Product Owners, Business Analysts, Compliance, Marketing

---

## Pre-Interview Checklist

- [ ] Schedule 45-minute meeting with SME
- [ ] Send this questionnaire 24 hours in advance
- [ ] Prepare examples from SHARED_DOCUMENT_ELIGIBILITY_CATALOG.md
- [ ] Have access to API documentation
- [ ] Bring list of available data sources

---

## Interview Script

### Introduction (5 minutes)

**Say:**
> "Thank you for your time. We're building a comprehensive catalog of all shared documents and their eligibility rules. This will help us:
> - Automate document selection based on customer attributes
> - Ensure the right documents are shown to the right customers
> - Make it easy to update rules without code changes
>
> I'll ask questions about [DOCUMENT NAME]. Feel free to ask for clarification at any time."

---

## Section 1: Document Identification (5 minutes)

### Q1: Basic Information

**1.1 What is the official name of this document?**
```
Answer: _________________________________

Notes:
- Internal name vs customer-facing name?
- Any aliases or previous names?
```

**1.2 How would you classify this document?**
```
Type: [ ] Disclosure  [ ] Agreement  [ ] Notice  [ ] Statement
      [ ] Policy      [ ] Terms      [ ] Marketing [ ] Regulatory
      [ ] Other: _______________

Category: _________________________________
(Examples: Benefits, Fees, Terms, Privacy, Compliance)
```

**1.3 Which line of business does this apply to?**
```
[ ] Credit Card
[ ] Digital Banking
[ ] Enterprise Banking
[ ] All lines of business
[ ] Multiple (specify): _________________
```

**1.4 Is this a regulatory document?**
```
[ ] Yes → Which regulation(s)? _________________________________
         (Examples: TILA, CCPA, GDPR, Dodd-Frank, SOX, FCRA)

         What's the compliance requirement? _____________________

         Who must receive it? __________________________________

[ ] No  → Skip regulatory questions
```

**1.5 Which department owns/maintains this document?**
```
Department: _________________________________

Contact Person: _________________________________

Last Updated: _________________________________
```

---

## Section 2: Target Audience (10 minutes)

### Q2: Who Should See This Document?

**2.1 In an ideal world, who should see this document?**
```
[ ] Everyone (all customers)

[ ] Specific customer segment
    → Describe: _________________________________________________

[ ] Based on account type
    → Which types: [ ] Credit Card  [ ] Checking  [ ] Savings
                   [ ] Loan         [ ] Investment [ ] Other: ____

[ ] Based on specific criteria
    → Describe below
```

**2.2 What makes a customer eligible to see this document?**
```
Free form answer:
________________________________________________________________
________________________________________________________________
________________________________________________________________

Probe questions:
- Is it based on account balance?
- Customer tier/status?
- Time with company (tenure)?
- Transaction activity?
- Product enrollment?
- Geographic location?
- Credit score or risk rating?
```

**2.3 Are there customers who should NEVER see this document?**
```
Yes/No: _____

If yes, describe who and why:
________________________________________________________________
________________________________________________________________
```

**2.4 Approximately what percentage of customers should see this?**
```
Estimated %: _________

This helps us understand:
- Performance impact (how many API calls)
- Business impact (reach)
- Testing scope
```

**2.5 Why is it important to show this only to eligible customers?**
```
Business reason:
________________________________________________________________
________________________________________________________________

Is it:
[ ] Legal requirement (explain): _______________________________
[ ] Business strategy (explain): _______________________________
[ ] Customer experience (explain): _____________________________
[ ] Cost optimization (explain): _______________________________
```

---

## Section 3: Eligibility Criteria Deep Dive (15 minutes)

### Q3: Specific Conditions

**For each condition, ask:**

#### Condition #1

**3.1.1 What specific piece of information determines eligibility?**
```
Field name: _________________________________
(Examples: accountBalance, customerTier, creditScore, transactionCount)

Where does this data come from?
[ ] Account Service API
[ ] Customer Service API
[ ] Transaction Service API
[ ] Database table: _________________________
[ ] Other: _________________________________
```

**3.1.2 What is the specific rule for this field?**
```
The customer is eligible if [field name] is:

[ ] Equal to: _________________________________
[ ] Not equal to: _____________________________
[ ] Greater than: _____________________________
[ ] Less than: ________________________________
[ ] Greater than or equal to: _________________
[ ] Less than or equal to: ____________________
[ ] One of these values: ______________________
[ ] Not one of these values: __________________
[ ] Contains: _________________________________
[ ] Matches pattern: __________________________
```

**3.1.3 Can you provide an example?**
```
Example 1 (Eligible):
  [Field] = [Value] → SHOULD see document
  Because: ___________________________________________

Example 2 (Not Eligible):
  [Field] = [Value] → SHOULD NOT see document
  Because: ___________________________________________
```

**3.1.4 Is this condition REQUIRED or OPTIONAL?**
```
[ ] Required (must be true)
[ ] Optional (nice to have)
[ ] One of several options (can be satisfied by alternative condition)
```

---

**Repeat for each condition (Condition #2, #3, etc.)**

---

### Q3.2: Combining Multiple Conditions

**3.2.1 If you have multiple conditions, how should they be combined?**
```
[ ] ALL conditions must be true (AND logic)
    Example: Balance > $10K AND Status = Active AND Tier = Gold

[ ] ANY condition can be true (OR logic)
    Example: Balance > $50K OR Tier = Platinum

[ ] Complex combination
    Describe: _______________________________________________
    Example: (Balance > $10K AND Status = Active) OR (Tier = Platinum)
```

**3.2.2 Which conditions are most important (if we had to prioritize)?**
```
Priority 1 (Critical): _______________________________________
Priority 2 (Important): ______________________________________
Priority 3 (Nice to have): ___________________________________
```

---

## Section 4: Data Requirements (5 minutes)

### Q4: Data Sources

**4.1 For each data point, what is the source?**

| Data Field | Source System | API/Table | Update Frequency | Critical? |
|------------|---------------|-----------|------------------|-----------|
| Example: accountBalance | Account Service | GET /accounts/{id} | Real-time | Yes |
| | | | | |
| | | | | |
| | | | | |

**4.2 How fresh does the data need to be?**
```
[ ] Real-time (must be current within seconds)
[ ] Near real-time (within 5 minutes)
[ ] Recent (within 1 hour)
[ ] Daily (updated overnight is fine)
[ ] On-demand (calculate when needed)
```

**4.3 What if the data is unavailable or the API fails?**
```
[ ] Do not show the document (safe default)
[ ] Show the document anyway (optimistic)
[ ] Use cached/stale data (specify how old: _____)
[ ] Show error message to customer
[ ] Use default/fallback value: _________________
```

---

## Section 5: Edge Cases & Exceptions (5 minutes)

### Q5: What Could Go Wrong?

**5.1 What happens at boundary conditions?**
```
Example: If rule is "balance > $10,000", what about:
- Exactly $10,000.00?
  Answer: _________________________________________________

- $10,000.01?
  Answer: _________________________________________________

- Balance is negative?
  Answer: _________________________________________________
```

**5.2 What if a customer is in transition?**
```
Examples:
- Customer is upgrading from Gold to Platinum (in progress)
  Answer: _________________________________________________

- Account is being closed but not yet fully closed
  Answer: _________________________________________________

- Data is being migrated/updated
  Answer: _________________________________________________
```

**5.3 Are there any known exceptions to the rule?**
```
Yes/No: _____

If yes, describe:
________________________________________________________________
________________________________________________________________

How should we handle these?
________________________________________________________________
```

**5.4 What about seasonal or time-based variations?**
```
[ ] This document is shown year-round (no time restriction)

[ ] Only during specific dates
    From: _______________ To: _______________

[ ] Relative to an event
    Example: 30 days before account anniversary
    Describe: ___________________________________________

[ ] Recurring schedule
    Example: Quarterly, annually
    Describe: ___________________________________________
```

---

## Section 6: Testing & Validation (3 minutes)

### Q6: Test Scenarios

**6.1 Can you provide 3 real examples?**

**Example A: Typical Eligible Customer**
```
Customer Profile:
- Account Balance: _______________
- Customer Tier: ________________
- Account Status: _______________
- [Other relevant fields]: ______

Expected Result: ☐ SHOW document  ☐ DO NOT SHOW document

Reason: _______________________________________________________
```

**Example B: Typical Ineligible Customer**
```
Customer Profile:
- Account Balance: _______________
- Customer Tier: ________________
- Account Status: _______________
- [Other relevant fields]: ______

Expected Result: ☐ SHOW document  ☐ DO NOT SHOW document

Reason: _______________________________________________________
```

**Example C: Edge Case**
```
Customer Profile:
- Account Balance: _______________
- Customer Tier: ________________
- Account Status: _______________
- [Other relevant fields]: ______

Expected Result: ☐ SHOW document  ☐ DO NOT SHOW document

Reason: _______________________________________________________
```

---

## Section 7: Compliance & Legal (5 minutes)

### Q7: Legal & Regulatory Requirements

**7.1 Are there legal requirements for SHOWING this document?**
```
[ ] Yes → Cite regulation/law: ______________________________
         Requirement: _________________________________________
         Penalty for non-compliance: _________________________

[ ] No
```

**7.2 Are there legal requirements for NOT showing this document?**
```
[ ] Yes → Explain (e.g., state restrictions, age requirements):
         ________________________________________________________

[ ] No
```

**7.3 What is the disclosure timing requirement?**
```
[ ] Immediate (real-time when customer becomes eligible)
[ ] Within 24 hours
[ ] Within 5 business days
[ ] Within 30 days
[ ] Annually
[ ] No specific timing requirement
```

**7.4 Do we need to audit/log who sees this document?**
```
[ ] Yes → What to log: [ ] Customer ID  [ ] Timestamp
                       [ ] Eligibility reason  [ ] Account details
                       [ ] Document version  [ ] Delivery channel

         Retention period: _____ years

[ ] No
```

**7.5 Who needs to approve changes to this eligibility rule?**
```
Must approve: _________________________________________________
(Examples: Legal, Compliance, Product Owner, Risk Management)

Should review: ________________________________________________

Can make minor updates: _______________________________________
```

---

## Section 8: Performance & Impact (3 minutes)

### Q8: System Impact

**8.1 How often will this eligibility check be performed?**
```
[ ] Every time customer logs in
[ ] When viewing documents section
[ ] Daily batch process
[ ] On-demand only
[ ] Other: _______________________

Estimated volume: ____________ checks per day
```

**8.2 What is acceptable response time?**
```
[ ] Must be instant (< 100ms)
[ ] Fast (< 500ms)
[ ] Acceptable (< 2 seconds)
[ ] Can be slow (> 2 seconds, but show loading indicator)
```

**8.3 What's more important if we must choose?**
```
[ ] Accuracy (always correct, even if slower)
[ ] Speed (fast response, accept some staleness)
[ ] Balance both

Explain: ______________________________________________________
```

---

## Section 9: Future Changes (2 minutes)

### Q9: Maintenance & Updates

**9.1 How often does this rule change?**
```
[ ] Rarely (once a year or less)
[ ] Occasionally (few times a year)
[ ] Regularly (monthly)
[ ] Frequently (weekly or more)
```

**9.2 What types of changes are most common?**
```
[ ] Threshold values (e.g., $10K → $15K)
[ ] Add/remove conditions
[ ] Change logic (AND → OR)
[ ] Add new data sources
[ ] Complete redesign
```

**9.3 Who typically requests changes?**
```
Requester: ____________________________________________________
Approval process: _____________________________________________
Typical lead time: ____________________________________________
```

---

## Post-Interview

### Action Items

- [ ] **Validate data sources exist**
  - Check with IT: Are these APIs available?
  - Confirm field names and data formats
  - Test API response times

- [ ] **Create test data**
  - Based on examples provided
  - Cover all scenarios (positive, negative, edge cases)

- [ ] **Document the rule**
  - Write formal rule definition
  - Include in catalog
  - Create JSON schema for implementation

- [ ] **Get approval**
  - Business owner sign-off
  - Compliance review (if regulatory)
  - IT/Architecture review
  - Security review

- [ ] **Schedule follow-up**
  - If clarifications needed
  - After implementation (demo/validation)

---

## Interview Notes

**Meeting Date:** _______________
**Participants:** _______________________________________________
**Document:** __________________________________________________

### Key Takeaways
```
1. ________________________________________________________________

2. ________________________________________________________________

3. ________________________________________________________________
```

### Open Questions
```
1. ________________________________________________________________

2. ________________________________________________________________
```

### Next Steps
```
- [ ] ____________________________________________________________
      Due: ___________ Owner: _______________

- [ ] ____________________________________________________________
      Due: ___________ Owner: _______________
```

---

## Template Summary Output

After completing this questionnaire, you should be able to create:

```yaml
document_id: "SHARED-DOC-XXX"
document_name: "[From Q1.1]"
sharing_scope: "[all | account_type_based | custom_rule]"

eligibility_rule:
  conditions:
    - field: "[From Q3.1.1]"
      operator: "[From Q3.1.2]"
      value: "[From Q3.1.2]"

  data_sources:
    - source_id: "[From Q4.1]"
      endpoint: "[From Q4.1]"

test_scenarios:
  - scenario: "[From Q6.1 Example A]"
    expected_result: true/false
```

This goes directly into the **SHARED_DOCUMENT_ELIGIBILITY_CATALOG.md** file.

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Owner:** Product/Technical Team
