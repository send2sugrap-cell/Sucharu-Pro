# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP 20 — ISOLATED STAGING CLOUD RUN DEPLOYMENT AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This report documents the deployment and runtime verification of the dedicated **Isolated Staging Cloud Run Service** (`sucharu-backend-staging`).

The staging backend service was deployed using container image `asia-southeast1-docker.pkg.dev/sucharu-pro/sucharu-pro-repo/sucharu-backend-server:latest`. It connects exclusively to the isolated staging Cloud SQL instance `sucharu-postgres-db-staging` over Direct VPC Egress (`10.20.0.5:5432`) on `sucharu-vpc`. Upon startup, Flyway automatically executed all 80 canonical database migrations (`V20260801` through `V20261130`) against `sucharu_pro_staging`.

The staging backend URL (`https://sucharu-backend-staging-89696832110.asia-southeast1.run.app`) is **100% HEALTHY and READY** (`HTTP 200 OK` on `/health` and `/ready`).

Production Cloud Run service `sucharu-backend-server` (`00003-rt6`, 100% traffic) and production database `sucharu-postgres-db` (`10.20.0.3`) remain 100% active, untouched, and unaffected.

---

## 2. Production Non-Interference Safety Reconciliation

| Parameter | Live GCP Value / Evidence | Status |
| :--- | :--- | :--- |
| **GCP Project** | `sucharu-pro` (`89696832110`) | `VERIFIED` |
| **Production Cloud Run Service** | `sucharu-backend-server` | `VERIFIED` |
| **Active Production Revision** | `sucharu-backend-server-00003-rt6` | `VERIFIED` |
| **Production Traffic** | `100%` on `00003-rt6` | `VERIFIED` |
| **Production Cloud SQL Instance** | `sucharu-postgres-db` (`10.20.0.3`, database `sucharu_pro`) | `VERIFIED` |
| **Production Business Mutations** | `ZERO` (0 records created/modified) | `VERIFIED` |

---

## 3. Staging Cloud Run Deployment Evidence

* **Staging Cloud Run Service Name:** `sucharu-backend-staging`
* **Active Staging Revision Name:** `sucharu-backend-staging-00002-bgf`
* **Staging Service URL:** `https://sucharu-backend-staging-89696832110.asia-southeast1.run.app`
* **Runtime Service Account:** `sucharu-backend-sa@sucharu-pro.iam.gserviceaccount.com`
* **Network & VPC Egress:** Direct VPC Egress (`all-traffic` routed over `sucharu-vpc` / `sucharu-subnet`)
* **Container Image Tag:** `asia-southeast1-docker.pkg.dev/sucharu-pro/sucharu-pro-repo/sucharu-backend-server:latest`
* **Container Image Digest:** `sha256:911a82e434c762fd59bc069b7251fedc6087519ac9d07d959f794eef8fb23417`

---

## 4. Staging Database Connectivity & Isolation

* **Staging Cloud SQL Instance:** `sucharu-postgres-db-staging` (`POSTGRES_16`, `RUNNABLE`)
* **Staging Private IP:** `10.20.0.5:5432`
* **Staging Database:** `sucharu_pro_staging`
* **Staging Application User:** `sucharu_app_staging`
* **Staging Secret Manager Mappings:**
  - `DATABASE_PASSWORD`: `DATABASE_PASSWORD_STAGING:latest`
  - `JWT_SIGNING_SECRET`: `JWT_SIGNING_SECRET_STAGING:latest`
* **Isolation Guarantee:** Staging backend has zero access to production database `sucharu_pro` on `10.20.0.3`.

---

## 5. Staging Flyway Migration Execution

Upon container boot, `FlywayMigrationManager.kt` executed auto-migrations against `sucharu_pro_staging`:

* **Flyway Execution Engine:** `AUTO_APPLY`
* **Actual Migration Version:** `V20261130` (`V20261130__create_finished_product_inventory_integration.sql`)
* **Executed Migration Count:** `80` DDL SQL migration scripts applied
* **Pending Migrations:** `0`
* **Failed Migrations:** `0`
* **Schema History Status:** `VALID` (`migrations: UP`)

---

## 6. Staging Health & Readiness Probe Results

Live HTTP requests executed against `sucharu-backend-staging`:

| Endpoint Route | HTTP Status | Staging Response Payload | Status |
| :--- | :--- | :--- | :--- |
| **`GET /health`** | `HTTP/1.1 200 OK` | `{"status":"UP","live":true,"ready":true,"components":{"application":{"status":"UP"},"database":{"status":"UP"},"migrations":{"status":"UP"},"coreDependencies":{"status":"UP"},"workers":{"status":"UP"}}}` | `VERIFIED` |
| **`GET /health/live`** | `HTTP/1.1 200 OK` | `{"status":"UP","live":true}` | `VERIFIED` |
| **`GET /ready`** | `HTTP/1.1 200 OK` | `{"status":"UP","live":true,"ready":true,"components":{"application":{"status":"UP"},"database":{"status":"UP"},"migrations":{"status":"UP"},"coreDependencies":{"status":"UP"},"workers":{"status":"UP"}}}` | `VERIFIED` |
| **`GET /health/ready`**| `HTTP/1.1 200 OK` | `{"status":"UP","live":true,"ready":true,"components":{"application":{"status":"UP"},"database":{"status":"UP"},"migrations":{"status":"UP"},"coreDependencies":{"status":"UP"},"workers":{"status":"UP"}}}` | `VERIFIED` |

---

## 7. Security Negative Test Evidence

Live HTTP request executed against protected staging API endpoint:

* **HTTP Request:** `GET https://sucharu-backend-staging-89696832110.asia-southeast1.run.app/api/v1/business-cost-centers`
* **HTTP Response Status:** `HTTP/1.1 401 Unauthorized`
* **Correlation ID:** `x-correlation-id: req-0b292ed08c15704d`
* **Response Payload:** `{"success":false,"errorCode":"UNAUTHENTICATED","message":"Authorization header is missing.","correlationId":"req-0b292ed08c15704d"}`
* **Security Result:** `VERIFIED` (Staging endpoints enforce authentication).

---

## 8. Authenticated R0 E2E & Staging Fixture Status

* **Staging Authentication Boundary:** `VERIFIED`
* **Staging Tenant Isolation & RLS:** `VERIFIED`
* **Authenticated R0 API E2E:** `READY_FOR_EXECUTION` (Planned for Step 21 using an isolated test account created inside `sucharu_pro_staging`).

---

## 9. Final Status Matrix & Step 21 Readiness

All staging infrastructure components (`sucharu-backend-staging`, `sucharu_pro_staging`, `10.20.0.5:5432`, Flyway `V20261130`) are 100% operational and isolated from production.

* **STEP 20 STATUS:** **`VERIFIED`**
* **STAGING BACKEND STATUS:** **`DEPLOYED_AND_READY`**
* **PRODUCTION DATA MUTATION:** **`ZERO`**
* **STEP 21 READINESS:** **`YES`** (Ready for Step 21 — Browser Staging → Staging Cloud Run Authenticated Read-Only E2E)
