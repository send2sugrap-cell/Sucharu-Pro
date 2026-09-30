# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP 15 — BROWSER ERP UI (WASM/JS) BUILD & STAGING DEPLOYMENT AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro`  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This report documents the build, compilation, static web packaging, and safe staging deployment of the **Sucharu Pro Browser ERP UI** (`:web_app` module).

The Browser ERP UI is constructed using JetBrains Compose Multiplatform targeting WebAssembly (`wasmJs`). The application was compiled using Gradle 8.10 / JDK 17 (`:web_app:wasmJsBrowserDistribution`), producing a self-contained web distribution (`index.html`, `sucharu_web.js`, Wasm binaries, and Material3 font resources).

The compiled web distribution was deployed to Google Cloud Storage static website bucket `gs://sucharu-pro-web-staging` in `asia-southeast1`. The staging URL (`https://storage.googleapis.com/sucharu-pro-web-staging/index.html`) is live, serving HTML5 `<canvas id="ComposeTarget">` and WebAssembly bundles over HTTPS. The Cloud Run production backend (`sucharu-backend-server-00003-rt6`) remains 100% active, healthy (`HTTP 200 OK`), and protected against unauthorized data mutations.

---

## 2. Production Backend Baseline Re-Verification

Prior to staging web deployment, current live production status was verified via `gcloud` CLI commands:

| Parameter | Live GCP Value / Evidence | Status |
| :--- | :--- | :--- |
| **GCP Project** | `sucharu-pro` (`89696832110`) | `VERIFIED` |
| **Cloud Run Service** | `sucharu-backend-server` | `VERIFIED` |
| **Active Known-Good Revision** | `sucharu-backend-server-00003-rt6` | `VERIFIED` |
| **Traffic Allocation** | `100%` on `00003-rt6` | `VERIFIED` |
| **HTTPS Probes Status** | `/health`, `/ready` -> `HTTP/1.1 200 OK` | `VERIFIED` |

---

## 3. `:web_app` Module Architecture Evidence

* **Module Location:** `E:\App\Sucharu Pro\web_app`
* **Entry Point:** `E:\App\Sucharu Pro\web_app\src\wasmJsMain\kotlin\com\sucharu\sucharupro\web\main.kt`
* **Technology Stack:** Kotlin Multiplatform Wasm/JS JetBrains Compose Multiplatform (`wasmJs`)
* **Core Dependencies:** `:core`, `:shared_ui`, `compose.material3`, `compose.foundation`
* **UI Components Included:**
  1. `SharedAdminDashboardWorkspace` (Enterprise ERP Admin Workspaces Module 00 - Module 24)
  2. `SharedPrintingCalculatorWorkspace` (Printing Quote & Commercial Pricing Engine)
  3. `SharedPublicWallWorkspace` (Server-Driven Public Wall Platform)
  4. `SharedTheme` (Jetpack Compose Material3 Dark Theme)

---

## 4. Web Build Task & Execution Result

* **Gradle Build Task:** `:web_app:wasmJsBrowserDistribution`
* **Gradle Execution Command:** `./gradlew :web_app:wasmJsBrowserDistribution`
* **Gradle Execution Result:** `BUILD SUCCESSFUL`

---

## 5. Generated Distribution Artifacts & Metrics

Generated output path: `E:\App\Sucharu Pro\web_app\build\dist\wasmJs\productionExecutable\`

| Artifact File | Size | Description | Status |
| :--- | :--- | :--- | :--- |
| **`index.html`** | `1.0 KB` | HTML5 entry point (`<canvas id="ComposeTarget">`, Noto Sans Bengali font links) | `VERIFIED` |
| **`sucharu_web.js`** | `528 KB` | JavaScript WebAssembly glue code | `VERIFIED` |
| **`dd568dbcd078c0adf7cf.wasm`** | `7.9 MB` | Core WebAssembly compiled application binary | `VERIFIED` |
| **`ba2109c0ca343cf8e371.wasm`** | `2.4 MB` | Helper WebAssembly binary | `VERIFIED` |
| **`composeResources/`** | `2.3 MB` | Material3 font resources & typography assets | `VERIFIED` |

Total self-contained web distribution size: **`13.1 MB`**

---

## 6. API Endpoint Configuration

* **Backend Gateway URL:** `https://sucharu-backend-server-89696832110.asia-southeast1.run.app`
* **Transport Client:** `HttpBackendApiClient` (`core` module)
* **Hardcoded Production Secrets:** `NONE` (Zero secrets or credentials embedded in JS/Wasm bundles)

---

## 7. Staging Hosting Target & Deployment Execution

* **Hosting Provider:** Google Cloud Storage (Static Website Bucket)
* **GCP Project:** `sucharu-pro`
* **Bucket Name:** `gs://sucharu-pro-web-staging`
* **Location:** `asia-southeast1`
* **Website Configuration:** `--web-main-page-suffix=index.html --web-error-page=index.html`
* **IAM Policy:** `roles/storage.objectViewer` granted to `allUsers`
* **Staging Website URL:** `https://storage.googleapis.com/sucharu-pro-web-staging/index.html`

---

## 8. Staging HTTP & Runtime Verification

Live HTTP request executed against the staging static web endpoint:

* **HTTP Request:** `GET https://storage.googleapis.com/sucharu-pro-web-staging/index.html`
* **HTTP Response Status:** `HTTP/1.1 200 OK`
* **Content-Type:** `text/html`
* **Cache-Control:** `public, max-age=3600`
* **Response Payload Snippet:**
  ```html
  <!DOCTYPE html>
  <html lang="bn">
  <head>
      <meta charset="UTF-8">
      <title>Sucharu Pro - Commercial Printing ERP & Calculator</title>
      ...
  </head>
  <body>
  <canvas id="ComposeTarget"></canvas>
  </body>
  </html>
  ```

---

## 9. Security Negative Test

* **Unauthenticated Request Probe:** `GET /api/v1/business-cost-centers`
* **HTTP Status:** `HTTP/1.1 401 Unauthorized`
* **Response Payload:** `{"success":false,"errorCode":"UNAUTHENTICATED","message":"Authorization header is missing.","correlationId":"req-..."}`
* **Security Result:** `VERIFIED` (Production endpoints enforce JWT Bearer authentication).

---

## 10. Production Backend & Data Safety Reconciliation

Zero production business mutations were performed during Step 15:
* `0` Customers created or modified
* `0` Orders or Quotations confirmed
* `0` Payments or Refunds processed
* `0` Inventory movements recorded
* `0` Financial or Accounting entries altered

Cloud Run service `sucharu-backend-server` remains on revision `sucharu-backend-server-00003-rt6` serving **100% traffic**.

---

## 11. Remaining Web & Hosting Gaps

1. **Custom Domain Name Mapping:** Pending user domain purchase/registration (e.g. `api.sucharu.pro` / `erp.sucharu.pro`).
2. **CDN Custom Caching & SPA Routing:** Firebase Hosting / Cloud CDN custom domain SSL setup.
3. **Third-Party Payment / Webhook E2E:** Live bank payment gateway onboarding.

---

## 12. Final Status & Step 16 Readiness Gate

* **STEP 15 STATUS:** **`VERIFIED_WITH_GAPS`**
* **BROWSER ERP UI STAGING STATUS:** **`STAGED_AND_VERIFIED`**
* **NEXT CONTROLLED STEP READINESS:** **`YES`**
