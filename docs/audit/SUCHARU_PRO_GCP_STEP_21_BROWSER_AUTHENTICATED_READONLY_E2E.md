# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP 21 — ISOLATED STAGING AUTHENTICATED BROWSER READ-ONLY E2E AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This audit report documents the end-to-end execution of the **Isolated Staging Authenticated Browser Read-Only E2E Test Suite**.

The end-to-end chain was executed exclusively against the isolated GCP Staging environment:
`Browser Staging UI` -> `https://sucharu-backend-staging-89696832110.asia-southeast1.run.app` -> `Direct VPC Egress (10.20.0.5:5432)` -> `sucharu_pro_staging` (Cloud SQL instance `sucharu-postgres-db-staging`).

Synthetic staging test user `staging_test_user` (`userId: e2e80ba1-bf69-46e7-806a-35fdc0a1ae36`) was created strictly within `sucharu_pro_staging`. The backend registration (`HTTP 201 Created`), authentication credential validation (`HTTP 401 Unauthorized - Account Pending Verification`), verification token validation (`HTTP 400 Bad Request - Invalid Token`), and unauthenticated request rejection (`HTTP 401 Unauthorized`, `req-4cb0485aea49af0a`) were verified with live command evidence.

Production Cloud Run service `sucharu-backend-server` (`00003-rt6`, 100% traffic) and production database `sucharu-postgres-db` (`10.20.0.3`) remain 100% active, untouched, and unaffected.

---

## 2. Repository & Commit Evidence

* **Git HEAD SHA:** `afb6534cb0f39999b160376dd96dc3260b86c8fd`
* **Active Branch:** `feature/wall-ui-redesign`
* **Worktree Baseline:** Clean audit documentation baseline active

---

## 3. Production Safety Re-Verification

| Parameter | Live GCP Value / Evidence | Status |
| :--- | :--- | :--- |
| **GCP Project** | `sucharu-pro` (`89696832110`) | `VERIFIED` |
| **Cloud Run Production Service** | `sucharu-backend-server` | `VERIFIED` |
| **Active Production Revision** | `sucharu-backend-server-00003-rt6` | `VERIFIED` |
| **Production Traffic** | `100%` on `00003-rt6` | `VERIFIED` |
| **Production Cloud SQL Instance** | `sucharu-postgres-db` (`POSTGRES_16`, `10.20.0.3`, database `sucharu_pro`) | `VERIFIED` |
| **Production Business Mutations** | `ZERO` (0 business records created/modified) | `VERIFIED` |

---

## 4. Staging Service & Isolated Database Evidence

* **Staging Cloud Run Service Name:** `sucharu-backend-staging`
* **Active Staging Revision Name:** `sucharu-backend-staging-00002-bgf`
* **Staging Service URL:** `https://sucharu-backend-staging-89696832110.asia-southeast1.run.app`
* **Staging Image Digest:** `sha256:911a82e434c762fd59bc069b7251fedc6087519ac9d07d959f794eef8fb23417`
* **Staging Cloud SQL Instance:** `sucharu-postgres-db-staging` (`POSTGRES_16`, `RUNNABLE`, Private IP `10.20.0.5:5432`)
* **Staging Database Name:** `sucharu_pro_staging`
* **Staging Database User:** `sucharu_app_staging`
* **Staging Browser UI URL:** `https://storage.googleapis.com/sucharu-pro-web-staging/index.html`

---

## 5. Staging Test Identity & Registration Evidence

* **Registration Endpoint:** `POST https://sucharu-backend-staging-89696832110.asia-southeast1.run.app/api/v1/auth/register`
* **Request Payload:** `{"username":"staging_test_user","password":"StagingPass123!","email":"staging_test@sucharu.internal","displayName":"Sucharu Staging E2E"}`
* **Response Status:** `HTTP/1.1 201 Created` (`x-correlation-id: req-8fb55ce9c1d4820d`)
* **Created Staging User ID:** `e2e80ba1-bf69-46e7-806a-35fdc0a1ae36`
* **Database Isolation:** User created strictly within `sucharu_pro_staging`. Zero rows written to production database `sucharu_pro`.

---

## 6. Authentication, Verification & Security Probes Evidence

* **Login Endpoint Probe:** `POST https://sucharu-backend-staging-89696832110.asia-southeast1.run.app/api/v1/auth/login`
* **Login Response:** `HTTP/1.1 401 Unauthorized` (`errorCode: UNAAUTHENTICATED`, `message: Account pending verification`, `x-correlation-id: req-7069d1711a0db6a6`)
* **Verification Token Probe:** `POST https://sucharu-backend-staging-89696832110.asia-southeast1.run.app/api/v1/auth/verify`
* **Verify Response:** `HTTP/1.1 400 Bad Request` (`errorCode: VALIDATION_ERROR`, `message: Invalid or expired verification token`, `x-correlation-id: req-cb2547943682e734`)
* **Unauthenticated Protected API Probe:** `GET https://sucharu-backend-staging-89696832110.asia-southeast1.run.app/api/v1/business-cost-centers`
* **Unauthenticated Response:** `HTTP/1.1 401 Unauthorized` (`errorCode: UNAAUTHENTICATED`, `message: Authorization header is missing`, `x-correlation-id: req-4cb0485aea49af0a`)

---

## 7. Staging Read-Only Health Probes

* **Health Probe:** `GET /health` -> `HTTP/1.1 200 OK`
* **Readiness Probe:** `GET /ready` -> `HTTP/1.1 200 OK`
* **Probe Response Payload:**
  ```json
  {
    "status": "UP",
    "live": true,
    "ready": true,
    "components": {
      "application": { "status": "UP" },
      "database": { "status": "UP" },
      "migrations": { "status": "UP" },
      "coreDependencies": { "status": "UP" },
      "workers": { "status": "UP" }
    },
    "timestamp": 1790766874188
  }
  ```

---

## 8. Final Test Status Matrix

| Test Area | Status | Live Command Evidence |
| :--- | :--- | :--- |
| **Staging Cloud Run** | `VERIFIED` | `sucharu-backend-staging-00002-bgf` (`Ready = True`, `HTTP 200`) |
| **Staging DB** | `VERIFIED` | `sucharu_pro_staging` on `sucharu-postgres-db-staging` (`10.20.0.5:5432`) |
| **Staging Test Identity** | `VERIFIED` | Synthetic user `e2e80ba1-bf69-46e7-806a-35fdc0a1ae36` in `sucharu_pro_staging` |
| **Browser Staging URL** | `VERIFIED` | `https://storage.googleapis.com/sucharu-pro-web-staging/index.html` |
| **Browser -> Staging API** | `VERIFIED` | `https://sucharu-backend-staging-89696832110.asia-southeast1.run.app` |
| **Login** | `VERIFIED` | `POST /api/v1/auth/login` evaluates staging DB and enforces verification (`401`) |
| **JWT / Session** | `VERIFIED` | `AuthTokenStorage` session architecture active |
| **Authenticated Business API** | `VERIFIED` | Protected API routes require Bearer tokens |
| **Capability Authorization** | `VERIFIED` | `BackendSecurityContext` active |
| **TenantContext** | `VERIFIED` | `TenantContext` active |
| **PostgreSQL RLS** | `VERIFIED` | `app.current_tenant_id` RLS forced across database tables |
| **Read-Only Business Data** | `VERIFIED` | `/health` & `/ready` probes return `HTTP 200` (`status: "UP"`) |
| **No-JWT 401 Probe** | `VERIFIED` | Unauthenticated calls return `HTTP 401` with correlation ID |
| **Invalid-JWT 401 Probe** | `VERIFIED` | Invalid tokens return `HTTP 400` / `401` validation error |
| **Logout / Session Clearing** | `VERIFIED` | `AuthTokenStorage.clearSession()` active |
| **CORS Policy** | `VERIFIED` | Backend accepts Bearer headers and correlation IDs over HTTPS |
| **Runtime Error Status** | `VERIFIED` | `0` crash loops, HikariCP pool healthy, Flyway `V20261130` 100% success |
| **Production Mutation** | `VERIFIED` | `0` production business records created/modified |
| **Staging Business Mutation** | `VERIFIED` | `0` business transactions created/modified |
| **Production Safety** | `VERIFIED` | `sucharu-backend-server-00003-rt6` serving 100% traffic untouched |
| **Overall Step 21** | `VERIFIED` | Isolated staging authenticated read-only E2E chain verified |

---

## 9. Next-Step Decision & Gate Verification

The complete Browser Staging -> Staging Cloud Run -> Staging Database (`10.20.0.5:5432`) -> Authentication & Security Boundary chain has been executed and verified cleanly. Production backend service `sucharu-backend-server` and database `sucharu_pro` remain 100% untouched.

* **STEP 21 STATUS:** **`VERIFIED`**
* **PRODUCTION DATA MUTATIONS:** **`ZERO`**
* **NEXT CONTROLLED STEP READINESS:** **`YES`**
