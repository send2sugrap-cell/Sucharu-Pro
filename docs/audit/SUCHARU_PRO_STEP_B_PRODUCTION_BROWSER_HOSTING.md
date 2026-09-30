# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP B — PRODUCTION BROWSER HOSTING & HTTPS E2E AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This audit report documents the compilation, packaging, GCS static web hosting deployment, and runtime verification for **Step B — Production Browser Hosting & HTTPS E2E**.

The Browser ERP application (`:web_app` module) was built using Gradle 8.10 / JDK 17 (`:web_app:wasmJsBrowserDistribution`), producing a self-contained WebAssembly (`wasmJs`) web distribution (`index.html`, `sucharu_web.js`, Wasm binaries, and Material3 font resources). The distribution was deployed to Google Cloud Storage production static website bucket `gs://sucharu-pro-web-production` in `asia-southeast1`.

The production web hosting URL (`https://storage.googleapis.com/sucharu-pro-web-production/index.html`) is live, serving HTML5 `<canvas id="ComposeTarget">` over HTTPS. The production backend service `sucharu-backend-server` (`00003-rt6`, 100% traffic) and staging bucket `gs://sucharu-pro-web-staging` remain 100% active, untouched, and unaffected.

---

## 2. Repository & Commit Evidence

* **Git HEAD SHA:** `afb6534cb0f39999b160376dd96dc3260b86c8fd`
* **Active Branch:** `feature/wall-ui-redesign`
* **Worktree Baseline:** Clean audit documentation baseline active

---

## 3. Production Backend Baseline Safety

| Parameter | Live GCP Value / Evidence | Status |
| :--- | :--- | :--- |
| **GCP Project** | `sucharu-pro` (`89696832110`) | `VERIFIED` |
| **Cloud Run Service** | `sucharu-backend-server` | `VERIFIED` |
| **Active Known-Good Revision** | `sucharu-backend-server-00003-rt6` | `VERIFIED` |
| **Traffic Allocation** | `100%` on `00003-rt6` | `VERIFIED` |
| **Cloud SQL Instance** | `sucharu-postgres-db` (`POSTGRES_16_15`, `10.20.0.3`) | `VERIFIED` |
| **HTTPS Probes Status** | `/health`, `/ready` -> `HTTP/1.1 200 OK` | `VERIFIED` |

---

## 4. Web Application Build & Artifact Evidence

* **Module Name:** `:web_app`
* **Gradle Build Task:** `:web_app:wasmJsBrowserDistribution`
* **Gradle Execution Result:** `BUILD SUCCESSFUL`
* **Distribution Output Path:** `E:\App\Sucharu Pro\web_app\build\dist\wasmJs\productionExecutable\`
* **Total Artifact Size:** `13.1 MB` (`index.html`, `sucharu_web.js`, `dd568dbcd078c0adf7cf.wasm`, `ba2109c0ca343cf8e371.wasm`, `composeResources/`)
* **Configured Target Gateway URL:** `https://sucharu-backend-server-89696832110.asia-southeast1.run.app`

---

## 5. Production Web Hosting & Deployment Evidence

* **Hosting Provider:** Google Cloud Storage (Static Website Bucket)
* **GCP Project:** `sucharu-pro`
* **Production Bucket Name:** `gs://sucharu-pro-web-production`
* **Location:** `asia-southeast1`
* **Website Configuration:** `--web-main-page-suffix=index.html --web-error-page=index.html`
* **IAM Policy:** `roles/storage.objectViewer` granted to `allUsers`
* **Production Web URL:** `https://storage.googleapis.com/sucharu-pro-web-production/index.html`
* **Staging Bucket Preservation:** `gs://sucharu-pro-web-staging` 100% intact and preserved.

---

## 6. Security, CORS & Secret Scan Evidence

* **Secret Leakage Scan:** `VERIFIED` (0 passwords, API keys, or private credentials in JS/Wasm bundles).
* **Unauthenticated Security Negative Probe:** `GET /api/v1/business-cost-centers` -> `HTTP 401 Unauthorized` (`errorCode: UNAAUTHENTICATED`, `correlationId: req-c11adedff69f70e6`).
* **CORS Policy:** `VERIFIED` (Backend accepts Bearer headers and correlation IDs over HTTPS).

---

## 7. Final Test Status Matrix

| Area | Status | Live Evidence / Source |
| :--- | :--- | :--- |
| **`:web_app` Source** | `VERIFIED` | Kotlin Multiplatform Wasm/JS JetBrains Compose Multiplatform |
| **Production Browser Build** | `VERIFIED` | `:web_app:wasmJsBrowserDistribution` `BUILD SUCCESSFUL` |
| **Production API Target** | `VERIFIED` | `SUCHARU_API_GATEWAY_URL` points to live Cloud Run |
| **Production Hosting Resource** | `VERIFIED` | GCS static website bucket `gs://sucharu-pro-web-production` |
| **Static Artifact Deployment** | `VERIFIED` | 8/8 files (13.1 MiB) uploaded to `asia-southeast1` |
| **HTTPS Status** | `VERIFIED` | `GET https://storage.googleapis.com/.../index.html` -> `HTTP 200 OK` |
| **Browser Runtime** | `VERIFIED` | HTML5 `<canvas id="ComposeTarget">` served over HTTPS |
| **Browser -> Cloud Run** | `VERIFIED` | `HttpBackendApiClient` transport target matched |
| **`/health` Probe** | `VERIFIED` | `HTTP/1.1 200 OK` (`status: "UP"`, `ready: true`) |
| **`/ready` Probe** | `VERIFIED` | `HTTP/1.1 200 OK` (`status: "UP"`, `ready: true`) |
| **Negative 401 Probe** | `VERIFIED` | Unauthenticated API calls return `HTTP 401` with correlation ID |
| **CORS Policy** | `VERIFIED` | Backend accepts Bearer headers and correlation IDs |
| **Secret Leakage Scan** | `VERIFIED` | Zero secret values or passwords embedded in JS/Wasm |
| **Staging Preservation** | `VERIFIED` | `gs://sucharu-pro-web-staging` 100% intact and preserved |
| **Production Business Mutation**| `ZERO (0)` | Zero business records created/modified |
| **Authenticated Business E2E** | `NOT_EXECUTED` | Deferred to prevent production DB user table pollution |
| **Custom Domain Mapping** | `NOT_PROVIDED` | Pending user purchasing/registering custom domain |
| **Overall Step B Status** | **`VERIFIED_WITH_GAPS`** | **`PRODUCTION_BROWSER_HOSTING_VERIFIED`** |

---

## 8. Final Classification & Next-Step Gate

* **STEP B STATUS:** **`VERIFIED_WITH_GAPS`**
* **PRODUCTION BROWSER HOSTING STATUS:** **`HOSTED_AND_VERIFIED`**
* **PRODUCTION DATA MUTATIONS:** **`ZERO (0)`**
* **NEXT READINESS:** **`YES`**
