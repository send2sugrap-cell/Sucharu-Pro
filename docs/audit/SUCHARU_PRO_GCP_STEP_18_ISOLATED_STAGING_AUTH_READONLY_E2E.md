# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP 18 — ISOLATED STAGING DATABASE SANDBOX & AUTHENTICATED READ-ONLY BROWSER E2E AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro`  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This audit report documents the evaluation and readiness for establishing an isolated **Non-Production Staging Database Sandbox** to execute browser authenticated read-only E2E testing for Sucharu Pro.

To maintain non-negotiable production safety, creating test accounts or running test Flyway data seeds in the production PostgreSQL database (`sucharu_pro`) was strictly prohibited. The audit established that provisioning a second GCP Cloud SQL staging instance (`sucharu-postgres-db-staging`) and Cloud Run staging service (`sucharu-backend-staging`) requires explicit operator GCP infrastructure provisioning.

Security negative probes confirmed that unauthenticated requests to protected endpoints return `HTTP 401 UNAUTHENTICATED` with structured correlation IDs (`req-34d2063fc088ab15`). The production backend revision `sucharu-backend-server-00003-rt6` remains 100% active, healthy (`HTTP 200 OK`), and protected against unauthorized data mutations.

---

## 2. Production Baseline Re-Verification

Prior to staging database sandbox evaluation, current live production status was verified via `gcloud` CLI commands:

| Parameter | Live GCP Value / Evidence | Status |
| :--- | :--- | :--- |
| **GCP Project** | `sucharu-pro` (`89696832110`) | `VERIFIED` |
| **Cloud Run Service** | `sucharu-backend-server` | `VERIFIED` |
| **Active Known-Good Revision** | `sucharu-backend-server-00003-rt6` | `VERIFIED` |
| **Traffic Allocation** | `100%` on `00003-rt6` | `VERIFIED` |
| **HTTPS Probes Status** | `/health`, `/health/live`, `/ready`, `/health/ready` -> `HTTP 200 OK` | `VERIFIED` |

---

## 3. Staging Website Baseline Re-Verification

* **Staging Host Provider:** Google Cloud Storage (Static Website Bucket)
* **Staging URL:** `https://storage.googleapis.com/sucharu-pro-web-staging/index.html`
* **HTTP Probe Status:** `HTTP/1.1 200 OK` (`Content-Type: text/html`)
* **Static Assets Status:** `sucharu_web.js`, Wasm binaries (`*.wasm`), and Material3 fonts load cleanly.

---

## 4. Staging Database Sandbox Audit & Isolation Strategy

* **Production Cloud SQL Instance:** `sucharu-postgres-db` (Dedicated to production database `sucharu_pro`).
* **Staging Database Isolation Status:** **`EXTERNAL_DEPENDENCY`**  
  *Creating a separate GCP Cloud SQL instance (`sucharu-postgres-db-staging`) and Cloud Run staging service (`sucharu-backend-staging`) requires GCP project owner infrastructure provisioning.*
* **Safe Staging Test Identity Status:** `SAFE_TEST_IDENTITY = NOT_AVAILABLE_IN_PRODUCTION_DB`
* **Execution Status:** **`BROWSER_AUTHENTICATION_E2E = NOT_EXECUTED / DEFERRED_TO_STAGING_INSTANCE_PROVISIONING`**

---

## 5. Security Negative Test Evidence

Live HTTP request executed against protected backend API endpoint:

* **HTTP Request:** `GET https://sucharu-backend-server-89696832110.asia-southeast1.run.app/api/v1/business-cost-centers`
* **HTTP Response Status:** `HTTP/1.1 401 Unauthorized`
* **Correlation ID:** `x-correlation-id: req-34d2063fc088ab15`
* **Response Payload:** `{"success":false,"errorCode":"UNAUTHENTICATED","message":"Authorization header is missing.","correlationId":"req-34d2063fc088ab15"}`
* **Security Result:** `VERIFIED` (Production endpoints reject unauthenticated requests with structured error contracts).

---

## 6. Architecture, CORS & Session Evidence

* **Client Transport:** `HttpBackendApiClient` (`core` module)
* **Token Storage:** `AuthTokenStorage` (Injects `Authorization: Bearer <token>`)
* **Correlation ID:** Injected via `X-Correlation-ID: req-...`
* **CORS Policy:** `VERIFIED` (Backend accepts Bearer headers and correlation IDs over HTTPS).

---

## 7. Production Data Safety Reconciliation

Zero production business mutations were performed during Step 18:
* `0` Customers created or modified
* `0` Orders or Quotations confirmed
* `0` Payments or Refunds processed
* `0` Inventory movements recorded
* `0` Financial or Accounting entries altered

---

## 8. Cloud Run Revision & Traffic Safety

Cloud Run service `sucharu-backend-server` status re-confirmed:
* **Active Revision:** `sucharu-backend-server-00003-rt6`
* **Traffic Allocation:** `100%`
* **Service Ready State:** `True`

---

## 9. Final Status & Next Controlled Step Gate

* **STEP 18 STATUS:** **`VERIFIED_WITH_GAPS`**
* **EXACT GAPS:** Authenticated user session E2E deferred until a separate GCP Cloud SQL staging instance (`sucharu-postgres-db-staging`) and Cloud Run staging service (`sucharu-backend-staging`) are provisioned.
* **NEXT CONTROLLED STEP READINESS:** **`YES`**
