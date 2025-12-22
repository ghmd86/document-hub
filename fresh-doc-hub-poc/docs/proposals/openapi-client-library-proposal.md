# Proposal: Centralized OpenAPI Client Library Generation

**Author:** [Your Name]
**Date:** December 2024
**Status:** Draft
**Stakeholders:** Architecture Team, Platform Engineering, API Teams

---

## Executive Summary

This proposal recommends establishing a standardized process for generating and publishing client libraries from OpenAPI specifications. Instead of each application team manually implementing API clients, API-owning teams would publish versioned client JARs to our artifact repository, ensuring consistency, reducing duplication, and improving API governance.

---

## 1. Problem Statement

### Current State

| Issue | Impact |
|-------|--------|
| **Manual client implementation** | Each team writes their own HTTP clients, DTOs, and error handling |
| **Code duplication** | Same API client code exists in 5-10+ applications |
| **Inconsistent implementations** | Different teams handle errors, retries, and timeouts differently |
| **Drift from API spec** | Manual clients often fall out of sync with actual API |
| **High maintenance cost** | API changes require updates across all consuming applications |
| **No versioning strategy** | Breaking changes cause unexpected production issues |

### Example: Current ECMS Integration

```
Application A ──┐
Application B ──┼──▶ Each manually implements ECMS client (~500 lines each)
Application C ──┤
Application D ──┘
                     Total: ~2000 lines of duplicated code
```

---

## 2. Proposed Solution

### Target State Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         API Provider Team                           │
│                                                                     │
│  ┌────────────┐     ┌─────────────┐     ┌───────────────────────┐  │
│  │  OpenAPI   │     │   CI/CD     │     │   Artifact Repository │  │
│  │   Spec     │────▶│  Pipeline   │────▶│   (Nexus/Artifactory) │  │
│  │            │     │             │     │                       │  │
│  │ ecms.yaml  │     │ - Validate  │     │  com.company:         │  │
│  │            │     │ - Generate  │     │    ecms-client:2.1.0  │  │
│  │            │     │ - Test      │     │                       │  │
│  │            │     │ - Publish   │     │                       │  │
│  └────────────┘     └─────────────┘     └───────────┬───────────┘  │
│                                                     │              │
└─────────────────────────────────────────────────────│──────────────┘
                                                      │
              ┌───────────────────────────────────────┼───────────────┐
              │                                       ▼               │
              │  ┌─────────────────────────────────────────────────┐  │
              │  │              Consumer Applications              │  │
              │  │                                                 │  │
              │  │   <dependency>                                  │  │
              │  │     <groupId>com.company</groupId>              │  │
              │  │     <artifactId>ecms-client</artifactId>        │  │
              │  │     <version>2.1.0</version>                    │  │
              │  │   </dependency>                                 │  │
              │  │                                                 │  │
              │  │   // Ready-to-use, type-safe API calls          │  │
              │  │   ecmsApi.uploadDocument(file, metadata);       │  │
              │  └─────────────────────────────────────────────────┘  │
              │                    Consumer Teams                     │
              └───────────────────────────────────────────────────────┘
```

### Key Components

| Component | Description |
|-----------|-------------|
| **OpenAPI Spec** | Single source of truth for API contract |
| **Generator Pipeline** | Automated code generation on spec changes |
| **Client Library** | Generated JAR with models, API classes, and configuration |
| **Artifact Repository** | Versioned storage (Nexus/Artifactory) |
| **Consumer Integration** | Simple Maven/Gradle dependency |

---

## 3. Benefits

### 3.1 Business Benefits

| Benefit | Quantifiable Impact |
|---------|---------------------|
| **Reduced development time** | 2-3 days saved per integration |
| **Faster onboarding** | New apps integrate in hours, not days |
| **Reduced defects** | Fewer manual coding errors |
| **Faster API adoption** | Lower barrier to consume new APIs |

### 3.2 Technical Benefits

| Benefit | Description |
|---------|-------------|
| **Type Safety** | Compile-time validation of API contracts |
| **Consistency** | All consumers use identical client code |
| **Automatic Sync** | Client always matches API spec |
| **Built-in Best Practices** | Retry, timeout, error handling included |
| **Versioning** | SemVer for safe dependency management |
| **Reduced Code** | ~80% less boilerplate per integration |

### 3.3 Governance Benefits

| Benefit | Description |
|---------|-------------|
| **API Ownership** | Provider team controls client behavior |
| **Change Management** | Breaking changes are versioned |
| **Deprecation Path** | Clear upgrade path for consumers |
| **Usage Tracking** | Artifact downloads show adoption |

---

## 4. Technical Implementation

### 4.1 Client Library Project Structure

```
ecms-client-library/
├── pom.xml
├── src/
│   └── main/
│       ├── resources/
│       │   └── openapi/
│       │       └── ecms.yaml           # OpenAPI specification
│       └── java/
│           └── com/company/ecms/
│               └── config/
│                   └── EcmsClientConfig.java  # Custom configuration
├── .github/
│   └── workflows/
│       └── publish.yml                 # CI/CD pipeline
└── README.md                           # Usage documentation
```

### 4.2 Maven Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <groupId>com.company.api</groupId>
    <artifactId>ecms-client</artifactId>
    <version>1.0.0</version>

    <build>
        <plugins>
            <!-- OpenAPI Generator -->
            <plugin>
                <groupId>org.openapitools</groupId>
                <artifactId>openapi-generator-maven-plugin</artifactId>
                <version>7.10.0</version>
                <executions>
                    <execution>
                        <goals><goal>generate</goal></goals>
                        <configuration>
                            <inputSpec>${project.basedir}/src/main/resources/openapi/ecms.yaml</inputSpec>
                            <generatorName>java</generatorName>
                            <library>webclient</library>
                            <apiPackage>com.company.ecms.api</apiPackage>
                            <modelPackage>com.company.ecms.model</modelPackage>
                            <generateApiTests>true</generateApiTests>
                            <configOptions>
                                <dateLibrary>java8</dateLibrary>
                                <useSpringBoot3>false</useSpringBoot3>
                                <performBeanValidation>true</performBeanValidation>
                            </configOptions>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <!-- Publish to Nexus -->
    <distributionManagement>
        <repository>
            <id>nexus-releases</id>
            <url>https://nexus.company.com/repository/maven-releases/</url>
        </repository>
        <snapshotRepository>
            <id>nexus-snapshots</id>
            <url>https://nexus.company.com/repository/maven-snapshots/</url>
        </snapshotRepository>
    </distributionManagement>
</project>
```

### 4.3 CI/CD Pipeline (GitHub Actions)

```yaml
name: Build and Publish Client Library

on:
  push:
    branches: [main]
    paths:
      - 'src/main/resources/openapi/**'
      - 'pom.xml'
  release:
    types: [created]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Validate OpenAPI Spec
        run: npx @openapitools/openapi-generator-cli validate -i src/main/resources/openapi/ecms.yaml

      - name: Build and Test
        run: mvn clean verify

      - name: Publish to Nexus
        if: github.event_name == 'release'
        run: mvn deploy -DskipTests
        env:
          NEXUS_USERNAME: ${{ secrets.NEXUS_USERNAME }}
          NEXUS_PASSWORD: ${{ secrets.NEXUS_PASSWORD }}
```

### 4.4 Consumer Usage

```xml
<!-- Application pom.xml -->
<dependency>
    <groupId>com.company.api</groupId>
    <artifactId>ecms-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
// Application code - simple, type-safe usage
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentsApi ecmsApi;  // Auto-configured

    public Mono<DocumentResponse> uploadDocument(byte[] content, String fileName) {
        DocumentUploadRequest request = new DocumentUploadRequest()
            .fileName(fileName)
            .content(content);

        return ecmsApi.uploadDocument(request);
    }
}
```

---

## 5. Versioning Strategy

### Semantic Versioning

| Version Change | When | Example |
|----------------|------|---------|
| **MAJOR** (X.0.0) | Breaking API changes | Removed endpoint, changed request structure |
| **MINOR** (1.X.0) | New features, backward compatible | New optional field, new endpoint |
| **PATCH** (1.0.X) | Bug fixes, documentation | Fixed typo in description |

### Version Lifecycle

```
1.0.0 ──▶ 1.1.0 ──▶ 1.2.0 ──▶ 2.0.0 (breaking change)
  │         │         │
  │         │         └── Current stable
  │         └── Supported (security fixes)
  └── Deprecated (6 month sunset)
```

---

## 6. Rollout Plan

### Phase 1: Pilot (4 weeks)
| Week | Activity |
|------|----------|
| 1 | Set up client library project for ECMS API |
| 2 | Configure CI/CD pipeline and Nexus publishing |
| 3 | Migrate Document Hub to use generated client |
| 4 | Document learnings, refine process |

### Phase 2: Expansion (8 weeks)
| Week | Activity |
|------|----------|
| 5-6 | Onboard 2-3 additional API teams |
| 7-8 | Create templates and documentation |
| 9-10 | Training sessions for development teams |
| 11-12 | Establish governance and standards |

### Phase 3: Enterprise Adoption
- Mandate for all new API integrations
- Migration path for existing integrations
- Self-service tooling for API teams

---

## 7. Effort Estimation

### Initial Setup (One-time)

| Task | Effort | Owner |
|------|--------|-------|
| Define standards and templates | 3 days | Architecture |
| Set up Nexus repository structure | 1 day | Platform |
| Create CI/CD pipeline templates | 2 days | DevOps |
| Documentation and training materials | 2 days | Architecture |
| **Total** | **8 days** | |

### Per API Client Library

| Task | Effort | Owner |
|------|--------|-------|
| Initial project setup | 0.5 days | API Team |
| Configure generation | 0.5 days | API Team |
| Custom configuration (if needed) | 1 day | API Team |
| Testing and validation | 1 day | API Team |
| **Total** | **3 days** | |

### Comparison: Manual vs Generated

| Approach | Initial Effort | Maintenance/Year |
|----------|---------------|------------------|
| Manual client (per consumer) | 5 days | 3 days |
| Generated library (shared) | 3 days | 1 day |
| **Savings (5 consumers)** | **22 days** | **14 days** |

---

## 8. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Generator produces suboptimal code | Medium | Low | Use templates, add wrapper layer |
| Teams resist adoption | Medium | Medium | Show clear benefits, provide support |
| Breaking changes cause issues | Low | High | Strict versioning, deprecation policy |
| Nexus availability | Low | High | Mirror/backup strategy |
| Learning curve | Medium | Low | Documentation, training, examples |

---

## 9. Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Adoption rate | 80% of new integrations | Nexus download stats |
| Time to integrate | < 1 day | Team surveys |
| Duplicate code reduction | 70% | Code analysis |
| Integration defects | 50% reduction | Jira tracking |
| Developer satisfaction | > 4/5 | Quarterly survey |

---

## 10. Recommendation

We recommend proceeding with **Phase 1 (Pilot)** using the ECMS API as the first candidate because:

1. **Active development** - Document Hub is currently integrating with ECMS
2. **Clear ownership** - ECMS team maintains the OpenAPI spec
3. **Multiple consumers** - 3+ applications will need ECMS integration
4. **Low risk** - Non-critical path, easy rollback

### Immediate Next Steps

1. [ ] Architecture review and approval
2. [ ] Identify ECMS API team point of contact
3. [ ] Set up pilot project repository
4. [ ] Configure Nexus repository for client libraries

---

## Appendix A: Technology Options

### Code Generators

| Generator | Pros | Cons |
|-----------|------|------|
| **OpenAPI Generator** | Most popular, many languages | Large, complex |
| Swagger Codegen | Original, stable | Less active development |
| NSwag | .NET focused | Limited Java support |

### HTTP Client Libraries

| Library | Best For |
|---------|----------|
| **WebClient** | Spring WebFlux (reactive) |
| RestTemplate | Spring MVC (legacy) |
| Feign | Declarative clients |
| OkHttp | Android, lightweight |

### Artifact Repositories

| Repository | Pros | Cons |
|------------|------|------|
| **Nexus** | Enterprise features, widely used | License cost |
| Artifactory | Universal, good UI | License cost |
| GitHub Packages | Free for public, integrated | Limited features |

---

## Appendix B: References

- [OpenAPI Generator Documentation](https://openapi-generator.tech/)
- [Semantic Versioning Specification](https://semver.org/)
- [Netflix API Client Patterns](https://netflixtechblog.com/)
- [Google API Client Libraries](https://developers.google.com/api-client-library)

---

**Document History**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 0.1 | Dec 2024 | [Your Name] | Initial draft |
