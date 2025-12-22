# OpenAPI Client Library Strategy
## Executive Summary (5-min Pitch)

---

## The Problem

**Today:** Every team manually writes API clients

```
App A ─┬─▶ Manual ECMS client (500 lines)
App B ─┼─▶ Manual ECMS client (500 lines)  ═══▶ 2000+ lines duplicated
App C ─┼─▶ Manual ECMS client (500 lines)       Different implementations
App D ─┘─▶ Manual ECMS client (500 lines)       Drift from actual API
```

**Pain Points:**
- 2-5 days per integration
- Inconsistent error handling
- No versioning strategy
- Manual updates on API changes

---

## The Solution

**Proposed:** API teams publish generated client JARs

```
                    ┌──────────────┐
   OpenAPI Spec ───▶│ CI Pipeline  │───▶ Nexus
                    └──────────────┘       │
                                           ▼
                    ┌──────────────────────────────┐
                    │  <dependency>                │
                    │    ecms-client:2.1.0         │
                    │  </dependency>               │
                    │                              │
                    │  // One line to call API     │
                    │  ecmsApi.uploadDocument(...) │
                    └──────────────────────────────┘
```

---

## Benefits at a Glance

| Metric | Before | After |
|--------|--------|-------|
| Integration time | 2-5 days | 2-4 hours |
| Lines of code | ~500/app | ~10/app |
| API sync | Manual | Automatic |
| Versioning | None | SemVer |
| Error handling | Inconsistent | Standardized |

---

## Effort & Timeline

### Initial Setup: 8 days (one-time)
- Standards definition: 3 days
- Nexus/CI setup: 3 days
- Documentation: 2 days

### Per API: 3 days
- Project setup + generation + testing

### ROI Example (5 consumers)
- **Manual approach:** 25 days initial + 15 days/year maintenance
- **Generated library:** 3 days initial + 1 day/year maintenance
- **Savings:** 22 days initial, 14 days/year

---

## Pilot Proposal

**Candidate:** ECMS API
**Why:**
- Active development (Document Hub)
- Clear ownership
- Multiple consumers planned
- Low risk, easy rollback

**Timeline:** 4 weeks

---

## Ask

1. **Approval** to proceed with ECMS pilot
2. **Nexus repository** access for client libraries
3. **30 mins** with ECMS team to align

---

## Questions?

Full proposal: `docs/proposals/openapi-client-library-proposal.md`
