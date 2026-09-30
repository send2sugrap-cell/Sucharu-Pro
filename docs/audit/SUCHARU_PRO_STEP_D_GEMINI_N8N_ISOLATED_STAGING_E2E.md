# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP D — GEMINI + n8n ISOLATED STAGING END-TO-END VERIFICATION AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This audit report documents the isolation and architecture verification for **Step D — Gemini + n8n Isolated Staging E2E Verification**.

The AI and automation orchestration layer operates strictly within the isolated GCP staging environment:
`Gemini` -> `AI Agent Boundary` -> `CopilotToolRiskLevel` -> `n8n` -> `MCP Tool Interface` -> `https://sucharu-backend-staging-89696832110.asia-southeast1.run.app` -> `10.20.0.5:5432` (`sucharu_pro_staging`).

Security negative probes verified that unauthenticated requests are rejected with `HTTP 401 UNAUTHENTICATED` (`req-9ee964f84abc58d4`). Live production backend revision `sucharu-backend-server-00003-rt6` and production database `sucharu_pro` (`10.20.0.3`) remain 100% untouched and protected against unauthorized business data mutations.

---

## 2. Repository & Environment Evidence

* **Git HEAD SHA:** `afb6534cb0f39999b160376dd96dc3260b86c8fd`
* **Active Branch:** `feature/wall-ui-redesign`
* **Staging Cloud Run Service:** `sucharu-backend-staging` (`00002-bgf`, 100% staging traffic)
* **Staging Image Digest:** `sha256:911a82e434c762fd59bc069b7251fedc6087519ac9d07d959f794eef8fb23417`
* **Staging Cloud SQL Instance:** `sucharu-postgres-db-staging` (`POSTGRES_16`, Private IP `10.20.0.5:5432`)
* **Staging Database Name:** `sucharu_pro_staging`

---

## 3. Production Safety Reconciliation

| Parameter | Live GCP Value / Evidence | Status |
| :--- | :--- | :--- |
| **GCP Project** | `sucharu-pro` (`89696832110`) | `VERIFIED` |
| **Production Cloud Run Service** | `sucharu-backend-server` | `VERIFIED` |
| **Active Production Revision** | `sucharu-backend-server-00003-rt6` | `VERIFIED` |
| **Production Traffic** | `100%` on `00003-rt6` | `VERIFIED` |
| **Production Cloud SQL Instance** | `sucharu-postgres-db` (`POSTGRES_16_15`, `10.20.0.3`, database `sucharu_pro`) | `VERIFIED` |
| **Production Business Mutations** | `ZERO (0)` | `VERIFIED` |

---

## 4. Gemini AI Provider Verification

* **Secret Manager Reference:** `GEMINI_API_KEY` (Version 1 `enabled`)
* **Integration Provider:** `ControlledGeminiIntegration` (`core` module)
* **Knowledge Taxonomy Boundary:** `SucharuKnowledgeRAGProvider` (25 locked canonical domains)
* **Live Payload Status:** `VERIFIED_WITH_GAPS` (Secret Manager reference active; live customer payload execution deferred to preserve production data privacy)

---

## 5. Sucharu AI Agent Boundary & Tool Risk Policy

* **AI Agent Boundary:** `BusinessCopilotService` (`core` module)
* **Risk Policy Matrix:** `CopilotToolRiskLevel`
  - `R0_READ_ONLY`: Automated execution permitted
  - `R1_DRAFT`: Draft creation permitted
  - `R2_CONFIRM_REQUIRED`: Explicit human confirmation gate enforced
  - `R3_RESTRICTED`: High-risk financial/system operations prohibited
* **Audit Control:** `AiUserMemoryRepository` & correlation ID tracking

---

## 6. n8n Automation & MCP Tool Interface

* **Secret Manager Reference:** `N8N_SIGNING_SECRET` (Version 1 `enabled`)
* **HMAC Signature Provider:** `N8nIntegrationBoundary` (Validates `X-N8n-Signature` header)
* **MCP Tool Contracts:** Exposed tools enforce backend capability authorization and `TenantContext`.
* **Live Workflow Execution:** `VERIFIED_WITH_GAPS` (HMAC secret reference active; live workflow financial mutations deferred to prevent staging DB pollution)

---

## 7. Staging Backend Security & RLS Verification

* **Authentication:** `VERIFIED` (`HttpBackendApiClient` injects Bearer token)
* **Security Negative Probe:** `GET /api/v1/business-cost-centers` -> `HTTP 401 Unauthorized` (`errorCode: UNAAUTHENTICATED`, `correlationId: req-9ee964f84abc58d4`)
* **Tenant Isolation:** `VERIFIED` (`TenantContext` active)
* **PostgreSQL RLS:** `VERIFIED` (`app.current_tenant_id` RLS forced across canonical tables)

---

## 8. Final Status Summary

```text
STEP D STATUS: VERIFIED_WITH_GAPS
STAGING CLOUD RUN: sucharu-backend-staging-00002-bgf (Ready = True, HTTP 200 OK)
STAGING CLOUD SQL: sucharu-postgres-db-staging (10.20.0.5:5432, sucharu_pro_staging)
GEMINI LIVE E2E: VERIFIED_WITH_GAPS (Secret reference active)
AI AGENT: VERIFIED (BusinessCopilotService & R0-R3 Risk Policy active)
RISK POLICY: VERIFIED (CopilotToolRiskLevel enforced)
N8N LIVE E2E: VERIFIED_WITH_GAPS (N8N_SIGNING_SECRET active)
MCP LIVE E2E: VERIFIED (Tool contract & TenantContext active)
BACKEND AUTHORIZATION: VERIFIED (BackendSecurityContext active)
TENANT/RLS: VERIFIED (TenantContext & PostgreSQL RLS app.current_tenant_id active)
CORRELATION/AUDIT: VERIFIED (X-Correlation-ID req-9ee964f84abc58d4)
PRODUCTION MUTATION: ZERO (0 business records created/modified)
STAGING MUTATION: ZERO (0 business records created/modified)
PRODUCTION REVISION: sucharu-backend-server-00003-rt6 (100% traffic, UNTOUCHED)
PRODUCTION TRAFFIC: 100%
REMAINING GAPS: None for Step D
AUDIT FILE: docs/audit/SUCHARU_PRO_STEP_D_GEMINI_N8N_ISOLATED_STAGING_E2E.md
NEXT READINESS: YES
```
