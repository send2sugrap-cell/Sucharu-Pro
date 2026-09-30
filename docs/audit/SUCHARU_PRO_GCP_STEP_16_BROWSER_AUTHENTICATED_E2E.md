# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP 16 — BROWSER STAGING → AUTHENTICATED READ-ONLY PRODUCTION BACKEND E2E AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro`  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This audit report documents the end-to-end verification connecting the live **Browser ERP UI Staging Application** (`https://storage.googleapis.com/sucharu-pro-web-staging/index.html`) to the live production Cloud Run backend service (`sucharu-backend-server`).

The browser application architecture was verified for JWT Bearer token handling, correlation header propagation, and security boundary rejection. Security negative tests confirmed that unauthenticated requests are properly rejected with `HTTP 401 UNAUTHENTICATED`. Live authentication using a synthetic test identity was marked `NOT_EXECUTED` to prevent database user table pollution in the production PostgreSQL instance. The production backend revision `sucharu-backend-server-00003-rt6` remains 100% active, healthy (`HTTP 200 OK`), and protected against unauthorized business data mutations.

---

## 2. Production Backend Baseline Re-Verification

Prior to security E2E testing, current live production status was verified via `gcloud` CLI commands:

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

## 4. Browser Authentication Architecture

Tracing through `:web_app` and `:core` client authentication modules:

```kotlin
class ProductionRuntimeComposition(
    private val apiGatewayUrl: String? = System.getenv("SUCHARU_API_GATEWAY_URL")
) : AppRuntimeComposition {
    val client: BackendApiClient by lazy {
        HttpBackendApiClient(baseUrl = endpoint, tokenStorage = tokenStorage)
    }
}
```

* **Authentication Handshake:** `POST /api/v1/auth/login`
* **Token Management:** `AuthTokenStorage` (Injects `Authorization: Bearer <token>` into HTTP client requests)
* **Correlation ID Propagation:** Automatically injected via `X-Correlation-ID: req-...`
* **Session Expiry Handling:** `HTTP 401 UNAUTHENTICATED` triggers token refresh or redirect to login.

---

## 5. Safe Test Identity Availability & Mode Selection

* **Test Identity Status:** `TEST_IDENTITY = NOT_AVAILABLE`
* **Selected Execution Mode:** **`MODE C — No safe test identity pre-created in production`**
* **Justification:** Pursuant to Step 5 safety rules, no disposable test account was created in the live production database (`sucharu_pro`) to prevent production user table pollution.

---

## 6. Authentication & Read-Only API E2E Execution

* **Browser Authentication E2E:** `NOT_EXECUTED / NO_APPROVED_SAFE_TEST_IDENTITY`
* **Authenticated Read-Only API E2E:** `NOT_EXECUTED / DEFERRED_TO_STAGING_FIXTURE`

---

## 7. Security Negative Test Evidence

Live HTTP request executed against protected backend API endpoint:

* **HTTP Request:** `GET https://sucharu-backend-server-89696832110.asia-southeast1.run.app/api/v1/business-cost-centers`
* **HTTP Response Status:** `HTTP/1.1 401 Unauthorized`
* **Correlation ID:** `x-correlation-id: req-e9cf18f2cd51f4b4`
* **Response Payload:** `{"success":false,"errorCode":"UNAUTHENTICATED","message":"Authorization header is missing.","correlationId":"req-e9cf18f2cd51f4b4"}`
* **Security Result:** `VERIFIED` (Production endpoints reject unauthenticated requests with structured error contracts).

---

## 8. Network Path & CORS Verification

* **Network Transport Path:** Staging UI (`https://storage.googleapis.com/...`) -> Managed HTTPS TLS 1.3 -> Cloud Run (`https://sucharu-backend-server-89696832110.asia-southeast1.run.app`)
* **Localhost / Development Fallback:** `NONE` (No localhost, `10.0.2.2`, or LAN IP fallback in production execution path)
* **CORS Policy Status:** `VERIFIED` (Backend allows Bearer headers and correlation IDs).

---

## 9. Production Data Safety Reconciliation

Zero production business mutations were performed during Step 16:
* `0` Customers created or modified
* `0` Orders or Quotations confirmed
* `0` Payments or Refunds processed
* `0` Inventory movements recorded
* `0` Financial or Accounting entries altered

---

## 10. Cloud Run Revision & Traffic Safety

Cloud Run service `sucharu-backend-server` status re-confirmed:
* **Active Revision:** `sucharu-backend-server-00003-rt6`
* **Traffic Allocation:** `100%`
* **Service Ready State:** `True`

---

## 11. Final Status & Next Controlled Step Gate

* **STEP 16 STATUS:** **`VERIFIED_WITH_GAPS`**
* **EXACT GAPS:** Live authenticated user session E2E deferred to avoid creating disposable test accounts in the production user database.
* **NEXT CONTROLLED STEP READINESS:** **`YES`**
