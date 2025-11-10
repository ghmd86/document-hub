# Claude Session Instructions

**Purpose:** This file provides instructions for Claude to follow at the start of every session to understand project context quickly without reading all files.

---

## Session Startup Protocol

### Step 1: Read Key Files First (In Order)

**ALWAYS read these files at the start of EVERY session:**

1. **SESSION_SUMMARY.md** (THIS FILE FIRST!)
   - Location: `C:\Users\ghmd8\Documents\AI\SESSION_SUMMARY.md`
   - Contains: What was done in the last session and what needs to happen next
   - **READ THIS BEFORE DOING ANYTHING ELSE**

2. **FILE_LOCATION_INDEX.md** (SECOND!)
   - Location: `C:\Users\ghmd8\Documents\AI\FILE_LOCATION_INDEX.md`
   - Contains: Where all important files are located
   - Updated after every major change

3. **CURRENT_TASK.md** (THIRD!)
   - Location: `C:\Users\ghmd8\Documents\AI\CURRENT_TASK.md`
   - Contains: Active tasks and immediate next steps
   - Updated when tasks are added or completed

### Step 2: Understand Context

After reading the three key files above, you should know:
- ✅ What was accomplished in previous sessions
- ✅ What the current state of the project is
- ✅ What needs to be done next
- ✅ Where all important files are located

### Step 3: Confirm Understanding

When the user starts a new session, respond with:
1. Brief summary of where we left off (from SESSION_SUMMARY.md)
2. Current active tasks (from CURRENT_TASK.md)
3. Ask the user which task they want to work on, or if there's a new task

---

## Session Completion Protocol

### When You Finish Any Significant Work:

**ALWAYS update these files:**

1. **Update SESSION_SUMMARY.md**
   ```markdown
   ## Session [Date/Number]: [Brief Title]

   ### What Was Done
   - Bullet point list of completed tasks
   - Files created/modified
   - Key decisions made

   ### Current State
   - Where the project stands now
   - What's working
   - What's pending

   ### Next Steps
   - Prioritized list of what should be done next
   - Dependencies or blockers
   - Recommendations
   ```

2. **Update FILE_LOCATION_INDEX.md**
   - Add new files created
   - Update descriptions of modified files
   - Remove deleted files
   - Keep the index organized by category

3. **Update CURRENT_TASK.md**
   - Mark completed tasks as DONE
   - Add new tasks discovered during work
   - Update task priorities
   - Add any blockers or dependencies

4. **Create Detailed Summary Document (if major work)**
   - For significant features/changes, create a dedicated summary file
   - Location: `docs/session-summaries/[YYYY-MM-DD]_[task-name].md`
   - Include: What was built, how it works, testing results, next steps

---

## File Naming Conventions

### Session Summary Files
- Format: `docs/session-summaries/YYYY-MM-DD_brief-description.md`
- Example: `docs/session-summaries/2025-11-09_jira-tickets-creation.md`

### Test Reports
- Format: `docs/testing/[feature-name]_test_report.md`
- Example: `docs/testing/document_enquiry_api_test_report.md`

### Documentation
- Format: `docs/[category]/[descriptive-name].md`
- Example: `docs/guides/template_config_future_enhancements.md`

---

## Important Project Rules

### 1. Compliance Requirements
- This is a PROTOTYPE that cannot be used in production
- All work validates technical approach only
- Company will re-implement following their standards
- Document everything clearly for knowledge transfer

### 2. Testing Approach
- Always test database changes before modifying code
- Create test reports for all significant features
- Document test results with screenshots/examples

### 3. Documentation Standards
- Create JIRA tickets for features to be re-implemented
- Maintain sequence diagrams for complex flows
- Keep API specifications up to date
- Document all configuration schemas (JSONB fields)

### 4. Git Commit Messages
- Use descriptive commit messages
- Always include "🤖 Generated with Claude Code" footer
- Reference related files/features in commit message

---

## Common Project Patterns

### When Adding a New Feature

1. **Database First**
   - Design schema changes
   - Create migration script (Flyway)
   - Test SQL queries directly
   - Document in database_schema.md

2. **API Specification**
   - Update OpenAPI spec (schema.yaml)
   - Add to Postman collection
   - Create sequence diagram if complex

3. **Implementation**
   - Create entity models
   - Create DTOs
   - Create service layer
   - Create controller
   - Add error handling

4. **Testing**
   - Database-level SQL tests
   - REST endpoint tests
   - Create test report

5. **Documentation**
   - Update FILE_LOCATION_INDEX.md
   - Update SESSION_SUMMARY.md
   - Create JIRA tickets for company implementation

### When Fixing a Bug

1. Write test that reproduces the bug
2. Fix the issue
3. Verify test passes
4. Document the fix in SESSION_SUMMARY.md
5. Update relevant test reports

---

## Project Structure Overview

```
C:\Users\ghmd8\Documents\AI\
├── SESSION_SUMMARY.md              # Read this FIRST every session
├── FILE_LOCATION_INDEX.md          # Read this SECOND every session
├── CURRENT_TASK.md                 # Read this THIRD every session
├── .claude/
│   └── instructions.md             # This file
├── document-hub-service/           # Main Spring Boot application
│   ├── src/main/java/
│   ├── src/main/resources/
│   │   └── db/migration/           # Flyway migration scripts
│   └── pom.xml
├── actual/
│   ├── api/
│   │   ├── schema.yaml             # OpenAPI 3.0 specification
│   │   └── Document_Hub_API.postman_collection.json
│   └── database/
│       └── database_schema.md
├── docs/
│   ├── project-management/         # JIRA tickets, project summaries
│   ├── testing/                    # Test reports
│   ├── sequence-diagrams/          # Flow diagrams
│   ├── guides/                     # Implementation guides
│   └── session-summaries/          # Detailed session summaries
└── [other directories...]
```

---

## Key Technologies Used

- **Backend:** Spring Boot 3.x with WebFlux (Reactive)
- **Database:** PostgreSQL 16+ with JSONB support
- **Build:** Maven
- **Migration:** Flyway
- **API Spec:** OpenAPI 3.0
- **Testing:** JUnit 5, Mockito, WebTestClient

---

## Quick Reference: What This Project Does

**Document Hub API** - Unified document retrieval system

**Core Features:**
1. Document Enquiry API - Retrieve customer documents
2. Template Management - Manage document templates with versioning
3. Shared Document Eligibility - Dynamic rules for document visibility
4. Generic Data Extraction Engine - Execute custom eligibility rules
5. Multi-Vendor Support - Flexible vendor configuration (print, email)

**Key Innovation:**
- JSONB-based configuration (template_config, data_extraction_schema)
- No code deployments needed for rule changes
- Dynamic document eligibility based on customer/account data

---

## Emergency Procedures

### If SESSION_SUMMARY.md Is Missing
1. Check git history: `git log --all --full-history -- SESSION_SUMMARY.md`
2. Check recent commits for context
3. Read FILE_LOCATION_INDEX.md
4. Ask user what they want to work on

### If Files Are Corrupted
1. Check git status
2. Review recent commits
3. Ask user if they want to restore from git

### If Unsure What To Do
1. Read the three key files again
2. Check CURRENT_TASK.md for active tasks
3. Ask the user for clarification

---

## Update History

- **2025-11-09:** Created instructions for session management
  - Added SESSION_SUMMARY.md protocol
  - Added FILE_LOCATION_INDEX.md maintenance
  - Added CURRENT_TASK.md tracking
  - Defined session startup and completion protocols

---

**Remember:** The goal is to minimize context-reading time at the start of each session while maintaining continuity. Always update the three key files (SESSION_SUMMARY.md, FILE_LOCATION_INDEX.md, CURRENT_TASK.md) when finishing work.
