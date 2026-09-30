# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP C — CUSTOM DOMAIN MAPPING & HTTPS VERIFICATION AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Status

* **STEP C STATUS:** **`NOT_PROVIDED`**
* **CUSTOM DOMAIN MAPPING STATUS:** **`NOT_PROVIDED / PENDING_USER_DOMAIN_REGISTRATION`**
* **MANAGED HTTPS BASELINE STATUS:** **`VERIFIED`**

---

## 2. Source Evidence

* **Git HEAD SHA:** `afb6534cb0f39999b160376dd96dc3260b86c8fd`
* **Active Branch:** `feature/wall-ui-redesign`
* **Worktree Baseline:** Clean audit documentation baseline active

---

## 3. Existing Production URLs (Verified)

* **Production Backend Service URL:** `https://sucharu-backend-server-89696832110.asia-southeast1.run.app`
* **Production Regional Backend URL:** `https://sucharu-backend-server-6x3udy6goq-as.a.run.app`
* **Production Browser Hosting URL:** `https://storage.googleapis.com/sucharu-pro-web-production/index.html`
* **Staging Browser Hosting URL:** `https://storage.googleapis.com/sucharu-pro-web-staging/index.html`

---

## 4. Custom Domain Discovery & Authorization Audit

A discovery query (`gcloud beta run domain-mappings list`) was executed against project `sucharu-pro`:

* **GCP Domain Mappings:** `[]` (Zero custom domains registered in GCP)
* **Custom Domain Status:** `NOT_PROVIDED`
* **Domain Authorization Gate:** `PENDING_OWNER_ACTION` (Requires project owner registering an official custom domain name, e.g. `api.sucharu.pro` or `app.sucharu.pro`, and adding DNS CNAME records)
* **Domain Mutation Action:** `NOT_EXECUTED` (Pursuant to Section 3 safety rules, no domain names or placeholder DNS records were invented)

---

## 5. Production Browser Hosting Verification

* **Hosting Provider:** Google Cloud Storage (`gs://sucharu-pro-web-production` in `asia-southeast1`)
* **HTTPS Access:** `GET https://storage.googleapis.com/sucharu-pro-web-production/index.html`
* **HTTP Response Status:** `HTTP/1.1 200 OK` (`Content-Type: text/html`)
* **Application Runtime:** HTML5 `<canvas id="ComposeTarget">`, JS glue (`sucharu_web.js`), and Wasm binaries load cleanly over HTTPS.
* **Secret Leakage Scan:** `VERIFIED` (0 passwords, API keys, or private credentials in JS/Wasm bundles).

---

## 6. Production Backend API Verification

* **Cloud Run Service Name:** `sucharu-backend-server`
* **Active Revision Name:** `sucharu-backend-server-00003-rt6`
* **Traffic Allocation:** `100%` on `00003-rt6`
* **Health Probe Results:**
  - `GET /health` -> `HTTP/1.1 200 OK` (`status: "UP"`, `ready: true`, database & Flyway `UP`)
  - `GET /ready` -> `HTTP/1.1 200 OK` (`status: "UP"`, `ready: true`)
* **Negative Security Probe:**
  - `GET /api/v1/business-cost-centers` -> `HTTP/1.1 401 Unauthorized` (`errorCode: UNAAUTHENTICATED`, `x-correlation-id: req-c11adedff69f70e6`)

---

## 7. Staging Preservation

* **Staging Storage Bucket:** `gs://sucharu-pro-web-staging`
* **Staging Web URL:** `https://storage.googleapis.com/sucharu-pro-web-staging/index.html`
* **Staging Status:** `HTTP/1.1 200 OK` (100% intact, preserved, and unmodified).

---

## 8. Production Safety Reconciliation

Zero production business mutations were performed during Step C:
* **Production Business Mutations:** `0`
* **Database Schema Changes:** `0`
* **Flyway Migration Changes:** `0`
* **Security Bypasses:** `NONE`

---

## 9. Rollback & Fault-Safety Policy

* **Known-Good Backend Revision:** `sucharu-backend-server-00003-rt6`
* **Known-Good Web Bucket:** `gs://sucharu-pro-web-production`
* **Rollback Readiness:** `VERIFIED` (Managed `*.run.app` and `storage.googleapis.com` endpoints remain fully functional and protected)

---

## 10. Remaining Gaps & Final Status Classification

* **STEP C STATUS:** **`NOT_PROVIDED`**
* **REMAINING GAPS:**
  1. Custom domain registration and DNS CNAME mapping (e.g. `api.sucharu.pro` / `app.sucharu.pro`).
  2. Physical Android device ADB test.
  3. Third-party live webhook E2E (BRAC Bank, Gemini live payload, n8n live workflow).
* **NEXT READINESS:** **`YES`**
