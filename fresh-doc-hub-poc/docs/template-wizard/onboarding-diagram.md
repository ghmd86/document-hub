# Template Onboarding Process - Mermaid Diagrams

## Swimlane Flowchart

```mermaid
flowchart TB
    subgraph Phase1["Phase 1: Request"]
        A([Start]) --> B[Submit Template Request]
        B --> C[Define Requirements]
        C --> D[Provide Sample Document]
    end

    subgraph Phase2["Phase 2: Configuration"]
        D --> E[Use Template Wizard]
        E --> F[Define Access Rules]
        F --> G[Generate Config/SQL]
        G --> H{IT Review}
        H -->|Issues| E
        H -->|Approved| I[Insert to DEV DB]
    end

    subgraph Phase3["Phase 3: Technical Setup & UAT"]
        I --> J[Configure Vendor Mapping]
        J --> K[Run Integration Tests]
        K --> L{Tests Pass?}
        L -->|No| K
        L -->|Yes| M[Load Test Data]
        M --> N[Business UAT]
        N --> O{UAT Approved?}
        O -->|No| E
        O -->|Yes| P[Create Migration Script]
    end

    subgraph Phase4["Phase 4: Deployment"]
        P --> Q[Deploy to PROD]
        Q --> R[Smoke Test]
        R --> S[Go-Live Confirmation]
        S --> T([End])
    end

    style Phase1 fill:#e8f5e9,stroke:#4caf50
    style Phase2 fill:#e3f2fd,stroke:#2196f3
    style Phase3 fill:#fff3e0,stroke:#ff9800
    style Phase4 fill:#f3e5f5,stroke:#9c27b0
```

## Sequence Diagram (Interactions)

```mermaid
sequenceDiagram
    autonumber
    participant BIZ as Business Team
    participant WIZ as Template Wizard
    participant IT as IT Team
    participant DEV as DEV Environment
    participant PROD as PROD Environment

    rect rgb(232, 245, 233)
        Note over BIZ: Phase 1: Request
        BIZ->>BIZ: Submit Template Request
        BIZ->>BIZ: Define Requirements
        BIZ->>BIZ: Provide Sample Document
    end

    rect rgb(227, 242, 253)
        Note over BIZ,IT: Phase 2: Configuration
        BIZ->>WIZ: Open Template Wizard
        WIZ->>BIZ: Step 1-6 Configuration
        BIZ->>WIZ: Define Access Rules
        WIZ->>BIZ: Generate JSON + SQL
        BIZ->>IT: Submit for Review
        IT->>IT: Validate Configuration
        alt Config Issues
            IT-->>BIZ: Request Changes
            BIZ->>WIZ: Update Configuration
        end
        IT->>DEV: Execute SQL Insert
    end

    rect rgb(255, 243, 224)
        Note over IT,DEV: Phase 3: Technical Setup
        IT->>DEV: Configure Vendor Mapping
        IT->>DEV: Run Integration Tests
        DEV-->>IT: Test Results
        IT->>DEV: Load Test Data
        IT->>BIZ: Ready for UAT
        BIZ->>DEV: Perform UAT Testing
        alt UAT Failed
            BIZ-->>IT: Report Issues
            IT->>DEV: Fix Issues
        end
        BIZ->>IT: UAT Sign-off
    end

    rect rgb(243, 229, 245)
        Note over IT,PROD: Phase 4: Deployment
        IT->>IT: Create Flyway Migration
        IT->>PROD: Deploy to Production
        IT->>PROD: Run Smoke Tests
        PROD-->>IT: Tests Passed
        IT->>BIZ: Deployment Complete
        BIZ->>BIZ: Notify Stakeholders
    end
```

## State Diagram (Template Status)

```mermaid
stateDiagram-v2
    [*] --> Draft: Request Submitted

    Draft --> InReview: Submit for Review
    InReview --> Draft: Changes Requested
    InReview --> Approved: IT Approved

    Approved --> DevDeployed: Insert to DEV DB
    DevDeployed --> Testing: Run Tests

    Testing --> DevDeployed: Tests Failed
    Testing --> UATReady: Tests Passed

    UATReady --> UATInProgress: Business Starts UAT
    UATInProgress --> UATReady: Issues Found
    UATInProgress --> UATApproved: UAT Passed

    UATApproved --> ProdDeployed: Deploy to PROD
    ProdDeployed --> Live: Smoke Tests Passed

    Live --> [*]

    note right of Draft: Business configures\nusing wizard
    note right of InReview: IT validates\nconfiguration
    note right of Testing: Integration &\nAPI tests
    note right of UATInProgress: Business\nvalidation
    note right of Live: Template\nactive in prod
```

## Gantt Chart (Timeline)

```mermaid
gantt
    title Template Onboarding Timeline
    dateFormat  YYYY-MM-DD

    section Phase 1: Request
    Submit Request           :a1, 2024-01-01, 1d
    Define Requirements      :a2, after a1, 1d

    section Phase 2: Configuration
    Use Template Wizard      :b1, after a2, 1d
    IT Review               :b2, after b1, 1d

    section Phase 3: Setup & UAT
    DEV Deployment          :c1, after b2, 1d
    Integration Tests       :c2, after c1, 1d
    Business UAT            :c3, after c2, 1d

    section Phase 4: Deployment
    Create Migration        :d1, after c3, 1d
    PROD Deployment         :d2, after d1, 1d
    Go-Live                 :milestone, after d2, 0d
```

## RACI Matrix

```mermaid
flowchart LR
    subgraph Legend
        R[R = Responsible]
        A[A = Accountable]
        C[C = Consulted]
        I[I = Informed]
    end

    subgraph Activities
        direction TB
        T1[Submit Request] --> T2[Define Requirements]
        T2 --> T3[Use Wizard]
        T3 --> T4[Review Config]
        T4 --> T5[Deploy DEV]
        T5 --> T6[Run Tests]
        T6 --> T7[UAT]
        T7 --> T8[Deploy PROD]
    end

    style R fill:#4caf50,color:#fff
    style A fill:#2196f3,color:#fff
    style C fill:#ff9800,color:#fff
    style I fill:#9e9e9e,color:#fff
```

| Activity | Business | IT | Management |
|----------|----------|-----|------------|
| Submit Request | R/A | I | I |
| Define Requirements | R/A | C | I |
| Use Template Wizard | R | C | I |
| Review Configuration | C | R/A | I |
| Deploy to DEV | I | R/A | I |
| Run Tests | I | R/A | I |
| UAT Testing | R/A | C | I |
| Deploy to PROD | I | R/A | A |
| Go-Live Confirmation | R | I | A |

## Simple Process Flow

```mermaid
graph LR
    A[Request] --> B[Configure]
    B --> C[Review]
    C --> D[Test]
    D --> E[UAT]
    E --> F[Deploy]
    F --> G[Live]

    style A fill:#4caf50,color:#fff
    style B fill:#2196f3,color:#fff
    style C fill:#ff9800,color:#fff
    style D fill:#9c27b0,color:#fff
    style E fill:#e91e63,color:#fff
    style F fill:#00bcd4,color:#fff
    style G fill:#8bc34a,color:#fff
```

---

## How to View These Diagrams

1. **GitHub/GitLab**: Diagrams render automatically in README files
2. **VS Code**: Install "Markdown Preview Mermaid Support" extension
3. **Online**: Use [Mermaid Live Editor](https://mermaid.live)
4. **Export**: Use Mermaid CLI to export as PNG/SVG
