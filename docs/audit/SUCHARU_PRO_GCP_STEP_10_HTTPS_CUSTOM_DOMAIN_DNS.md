# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP 10 — HTTPS / CUSTOM DOMAIN / DNS MAPPING AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target Project:** `sucharu-pro`  

---

## 1. Baseline Production State & Service Verification

Prior to domain evaluation, current production service state was re-verified via live `gcloud` CLI commands:

| Parameter | Live GCP Value / Evidence | Status |
| :--- | :--- | :--- |
| **Cloud Run Service** | `sucharu-backend-server` | `VERIFIED` |
| **Current Validated Revision** | `sucharu-backend-server-00002-jfx` | `VERIFIED` |
| **Traffic Allocation** | `100%` on `sucharu-backend-server-00002-jfx` | `VERIFIED` |
| **Revision Ready State** | `status: "True"` (`Ready`) | `VERIFIED` |
| **Container Image Digest** | `sha256:619d4e8afb222352cfe53b3dba17c98817f3aa2413bc9fdd8f1fb3348aee0d8f` | `VERIFIED` |
| **Git Commit SHA** | `afb6534cb0f39999b160376dd96dc3260b86c8fd` | `VERIFIED` |

---

## 2. Cloud Run Managed HTTPS Baseline Verification

Google Cloud Run automatically provisions managed TLS 1.3 certificates for service URLs:

* **Generated Primary URL:** `https://sucharu-backend-server-89696832110.asia-southeast1.run.app`
* **Generated Regional URL:** `https://sucharu-backend-server-6x3udy6goq-as.a.run.app`
* **TLS Certificate Status:** `VERIFIED` (Google-managed SAN SSL/TLS certificate issued for `*.asia-southeast1.run.app`)
* **Hostname Match:** `YES`

### Endpoint HTTPS Runtime Test Matrix

| Endpoint Route | HTTP Status | Response Payload Summary | Status |
| :--- | :--- | :--- | :--- |
| **`GET /health`** | `HTTP/1.1 200 OK` | `{"status":"UP","live":true,"ready":true,"components":{"application":{"status":"UP"},"database":{"status":"UP"},"migrations":{"status":"UP"},"coreDependencies":{"status":"UP"},"workers":{"status":"UP"}}}` | `VERIFIED` |
| **`GET /health/live`** | `HTTP/1.1 200 OK` | `{"status":"UP","live":true}` | `VERIFIED` |
| **`GET /ready`** | `HTTP/1.1 200 OK` | `{"status":"UP","live":true,"ready":true,"components":{"application":{"status":"UP"},"database":{"status":"UP"},"migrations":{"status":"UP"},"coreDependencies":{"status":"UP"},"workers":{"status":"UP"}}}` | `VERIFIED` |
| **`GET /health/ready`**| `HTTP/1.1 200 OK` | `{"status":"UP","live":true,"ready":true,"components":{"application":{"status":"UP"},"database":{"status":"UP"},"migrations":{"status":"UP"},"coreDependencies":{"status":"UP"},"workers":{"status":"UP"}}}` | `VERIFIED` |

---

## 3. Custom Domain Discovery & Authorization Evaluation

A discovery scan was executed across repository configuration files and GCP domain mappings (`gcloud beta run domain-mappings list`):

* **Custom Domain Specification:** `NOT_PROVIDED`
* **Domain Source:** `NOT_CONFIGURED` (No active custom domain name, e.g. `api.sucharu.pro`, registered in GCP)
* **Domain Authorization Gate:** `REQUIRES_EVIDENCE` (Custom domain mapping deferred until project owner registers an authorized domain name)
* **Domain Mapping Status:** `NOT_EXECUTED`

> [!NOTE]
> Pursuant to Section 3 of the Step 10 Specification, no domain name was invented or guessed. Managed Cloud Run HTTPS is 100% active and verified on the primary service URL.

---

## 4. Security & Revision Protection Verification

* **Cloud SQL Public Exposure:** `NO` (Database remains isolated on private IP `10.20.0.3` via Direct VPC Egress on `sucharu-vpc`).
* **Runtime IAM Permissions:** Unmodified (`roles/secretmanager.secretAccessor`, `roles/cloudsql.client`, `roles/artifactregistry.reader` on `sucharu-backend-sa`).
* **Row-Level Security & Multi-Tenancy:** Unmodified (`TenantContext` and RLS enforced across database tables).
* **Current Revision Protection:** `sucharu-backend-server-00002-jfx` remains serving 100% traffic without redeployment.

---

## 5. Verification Summary & Step 11 Readiness Gate

Cloud Run managed HTTPS endpoints are `VERIFIED` and responding with `HTTP 200 OK` across all health and readiness probes. Custom domain mapping remains `NOT_PROVIDED` pending operator domain registration.

* **STEP 10 STATUS:** **`VERIFIED_WITH_GAPS`** *(Gap: Custom domain mapping pending project owner domain registration)*
* **EXACT GAPS/BLOCKERS:** Custom domain mapping is `NOT_PROVIDED` pending project owner registering and authorizing a custom domain name (e.g. `api.sucharu.pro`). Managed Cloud Run HTTPS is 100% active and verified.
* **STEP 11 READINESS:** **`YES`**
