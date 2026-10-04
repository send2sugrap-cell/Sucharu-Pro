# SUCHARU PRO — CHANGE CONTROL POLICY & GOVERNANCE

**Effective Date:** 2026-10-01  
**Project:** Sucharu Pro ERP — Master Project  
**Status:** `ACTIVE`  

---

## 1. Objective & Mandate

This policy defines the mandatory change-control lifecycle for all future modifications to the Sucharu Pro codebase, database schema, AI orchestration, and GCP Cloud infrastructure following the **Production Baseline Lock** (`docs/00-project-control/SUCHARU_PRO_PRODUCTION_BASELINE.md`).

---

## 2. Mandatory Change Lifecycle

Every change must strictly follow this lifecycle:

```text
REQUEST
   │
   ▼
SCOPE & CLASSIFICATION
   │
   ▼
IMPACT ANALYSIS
   │
   ▼
BASELINE PROTECTION CHECK
   │
   ▼
IMPLEMENTATION
   │
   ▼
TARGETED UNIT/INTEGRATION TEST
   │
   ▼
REGRESSION CHECK
   │
   ▼
RUNTIME EVIDENCE VERIFICATION
   │
   ▼
ACCEPT / REJECT
   │
   ▼
BASELINE LEDGER UPDATE
```

---

## 3. Change Classification Categories

Every change request must be explicitly classified into one of:
1. **Change Request (CR):** New functionality or workflow enhancement.
2. **Bug Fix (BF):** Defect repair for an existing feature.
3. **Security Fix (SEC):** Vulnerability patch or security hardening.
4. **Performance Improvement (PERF):** Optimization backed by runtime profiling evidence.
5. **Infrastructure Maintenance (INFRA):** Cloud Run, Cloud SQL, or Secret Manager maintenance.
6. **Data/Configuration Change (CONFIG):** Non-destructive configuration or seed data update.

---

## 4. Layer Impact Analysis Matrix

Before implementation, the affected architectural layers must be identified:

| Layer | Possible Impact Area |
| :--- | :--- |
| **UI** | Mobile Jetpack Compose screens, Admin Studio, Public Wall |
| **ViewModel** | StateFlow, LiveData, UI state management |
| **API / Router** | REST endpoints (`BackendRouter`, `BackendCopilotRouter`, `BackendReportingRouter`) |
| **Backend Service** | Domain services, business rules, use cases |
| **Repository / DataSource**| Data access layer, PostgreSQL connection management |
| **Database / Flyway** | DDL migration scripts, indexes, constraints, RLS policies |
| **GCP Infrastructure** | Cloud Run, Cloud SQL, Secret Manager, Artifact Registry, GCS |
| **AI / Orchestrator** | `SucharuAiContextOrchestrator`, `FirebaseAiLogicProvider`, Gemini LLM, RAG |
| **Security / Auth** | `BackendSecurityContext`, `TenantContext`, `RoleCapabilityMatrix`, JWT |

---

## 5. Non-Negotiable Protection Boundaries

The following core boundaries **MUST NOT** be broken by any change:
* **No Unnecessary Resource Duplication:** Never create duplicate Cloud Run services, Cloud SQL instances, or Secret Manager secrets.
* **No Hardcoded AI Answers:** Never reintroduce code-defined Bengali or English static response prose.
* **No Bypass of RLS or RBAC:** Never disable PostgreSQL Row-Level Security (`app.current_tenant_id`) or skip capability checks.
* **No Direct Client Database Access:** Mobile clients connect exclusively through secure HTTPS REST API Gateways (`HttpBackendApiClient`).
* **Zero Secret Leakage:** Never commit passwords, tokens, API keys, or credentials into source code or APK build fields.

---

## 6. Acceptance & Evidence Requirement

A Change Request is considered `VERIFIED` only when backed by concrete runtime evidence:
- Unit/Integration test execution
- Gradle build success (`:app:assembleDebug`)
- Cloud Run health probe status (`HTTP 200 OK`)
- Physical device execution evidence (where mobile UI is affected)
- Zero production business ledger mutations (`PRODUCTION BUSINESS MUTATIONS = 0`)
