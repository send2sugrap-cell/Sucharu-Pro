# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP 06 — IAM / SERVICE ACCOUNTS / LEAST-PRIVILEGE BINDINGS AUDIT REPORT

**Date:** 2026-09-29  
**Repository:** `E:\App\Sucharu Pro`  
**Target Project:** `sucharu-pro`  

---

## 1. Foundation & IAM APIs Recheck

Prior to IAM service account creation, project foundation, billing, and IAM APIs enablement were re-verified:

| Property | Value / Live Evidence | Status |
| :--- | :--- | :--- |
| **Authenticated Account** | `send2sugrap@gmail.com` | `VERIFIED` |
| **Project ID** | `sucharu-pro` | `VERIFIED` |
| **Project Number** | `89696832110` | `VERIFIED` |
| **Billing Account** | `billingAccounts/01D240-4A080E-104AB8` | `VERIFIED` |
| **Billing Enabled** | `true` | `VERIFIED` |
| **IAM API** | `iam.googleapis.com` (`ENABLED`) | `VERIFIED` |
| **IAM Credentials API** | `iamcredentials.googleapis.com` (`ENABLED`) | `VERIFIED` |

---

## 2. Cloud Run Runtime Service Account

Runtime identity `sucharu-backend-sa` was created for project `sucharu-pro`:

* **Service Account ID:** `sucharu-backend-sa`
* **Canonical Email:** `sucharu-backend-sa@sucharu-pro.iam.gserviceaccount.com`
* **Display Name:** `Sucharu Pro Backend Runtime`
* **Description:** `Runtime identity for Sucharu Pro Cloud Run backend`
* **Creation Status:** `CREATED`

---

## 3. Cloud Build Service Identity

The Google-managed Cloud Build service identity associated with project `sucharu-pro` was identified:

* **Service Identity Email:** `89696832110@cloudbuild.gserviceaccount.com`
* **Creation Status:** `VERIFIED` / `ACTIVE`

---

## 4. Live IAM Least-Privilege Matrix

IAM policy bindings were applied and verified live via `gcloud projects get-iam-policy`:

| Identity Member | Applied Role | Architecture Purpose | Live Status |
| :--- | :--- | :--- | :--- |
| `sucharu-backend-sa@sucharu-pro.iam.gserviceaccount.com` | `roles/cloudsql.client` | Connect to Cloud SQL PostgreSQL instance | `VERIFIED` |
| `sucharu-backend-sa@sucharu-pro.iam.gserviceaccount.com` | `roles/secretmanager.secretAccessor` | Access production secrets at runtime | `VERIFIED` |
| `sucharu-backend-sa@sucharu-pro.iam.gserviceaccount.com` | `roles/artifactregistry.reader` | Pull backend Docker container images | `VERIFIED` |
| `89696832110@cloudbuild.gserviceaccount.com` | `roles/artifactregistry.writer` | Push Docker images to `sucharu-pro-repo` | `VERIFIED` |
| `89696832110@cloudbuild.gserviceaccount.com` | `roles/run.developer` | Deploy Cloud Run services | `VERIFIED` |
| `89696832110@cloudbuild.gserviceaccount.com` | `roles/iam.serviceAccountUser` | Impersonate `sucharu-backend-sa` runtime SA | `VERIFIED` |

---

## 5. Security & Least-Privilege Audit Results

| Security Control Check | Audit Target | Live Verification Result | Compliance |
| :--- | :--- | :--- | :--- |
| **No Owner Role** | `sucharu-backend-sa` / `89696832110@cloudbuild` | Neither identity granted `roles/owner` | `PASSED` |
| **No Editor Role** | `sucharu-backend-sa` / `89696832110@cloudbuild` | Neither identity granted `roles/editor` | `PASSED` |
| **No Service Account Keys** | `sucharu-backend-sa` | `0` user-managed JSON key files created | `PASSED` |
| **No Broad IAM Admin Roles**| `sucharu-backend-sa` / `89696832110@cloudbuild` | No `roles/iam.admin` or `roles/resourcemanager.*` | `PASSED` |
| **No Compute Admin Role** | `sucharu-backend-sa` / `89696832110@cloudbuild` | No `roles/compute.admin` granted | `PASSED` |
| **No Cloud SQL Admin Role** | `sucharu-backend-sa` | Restricted to `roles/cloudsql.client` only | `PASSED` |

---

## 6. Database User Status

* **Application Database User (`sucharu_app`):** `DEFERRED` (Awaiting operator population of `DATABASE_PASSWORD` secret version in Secret Manager).
* Administrative identity `postgres` is NOT used as the runtime application user.

---

## 7. Source Code & Git Safety Verification

* **Branch:** `feature/wall-ui-redesign`
* **HEAD Commit:** `afb6534cb0f39999b160376dd96dc3260b86c8fd`
* **Application Source Code:** Unmodified (`0` changes)
* **Gradle / Docker / Migration Specs:** Unmodified (`0` changes)

---

## 8. Verification Summary & Step 07 Gate

Runtime service account `sucharu-backend-sa@sucharu-pro.iam.gserviceaccount.com` and Cloud Build service identity `89696832110@cloudbuild.gserviceaccount.com` exist with least-privilege IAM bindings verified live. Zero JSON service account key files were generated.

* **FINAL STATUS:** **`VERIFIED`**
* **STEP 07 READY:** **`YES`**
