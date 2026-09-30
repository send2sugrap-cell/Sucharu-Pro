# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP 05 — CLOUD SQL PROVISION & BASELINE CONFIGURATION AUDIT REPORT

**Date:** 2026-09-29  
**Repository:** `E:\App\Sucharu Pro`  
**Target Project:** `sucharu-pro`  

---

## 1. Foundation & SQL Admin API Recheck

Prior to Cloud SQL provisioning, project foundation, billing, and API enablement were re-verified:

| Property | Value / Live Evidence | Status |
| :--- | :--- | :--- |
| **Authenticated Account** | `send2sugrap@gmail.com` | `VERIFIED` |
| **Project ID** | `sucharu-pro` | `VERIFIED` |
| **Project Number** | `89696832110` | `VERIFIED` |
| **Billing Account** | `billingAccounts/01D240-4A080E-104AB8` | `VERIFIED` |
| **Billing Enabled** | `true` | `VERIFIED` |
| **Cloud SQL Admin API** | `sqladmin.googleapis.com` (`ENABLED`) | `VERIFIED` |

---

## 2. Cloud SQL Instance Provisioning & Baseline Matrix

Cloud SQL instance `sucharu-postgres-db` was created in region `asia-southeast1`:

`gcloud sql instances create sucharu-postgres-db --database-version=POSTGRES_16 --edition=ENTERPRISE --tier=db-f1-micro --region=asia-southeast1 --project=sucharu-pro`

### Instance Configuration Evidence Matrix

| Property | Target Specification | Live GCP Configuration | Status |
| :--- | :--- | :--- | :--- |
| **Instance ID** | `sucharu-postgres-db` | `sucharu-postgres-db` | `VERIFIED` |
| **Connection Name** | `sucharu-pro:asia-southeast1:sucharu-postgres-db` | `sucharu-pro:asia-southeast1:sucharu-postgres-db` | `VERIFIED` |
| **Database Engine** | PostgreSQL | PostgreSQL (`POSTGRES_16_15`) | `VERIFIED` |
| **Engine Version** | PostgreSQL 16 | `POSTGRES_16` | `VERIFIED` |
| **Region / Zone** | `asia-southeast1` | `asia-southeast1` (`asia-southeast1-c`) | `VERIFIED` |
| **Instance State** | `RUNNABLE` | `RUNNABLE` | `VERIFIED` |
| **Machine Tier** | Baseline Tier | `db-f1-micro` | `VERIFIED` |
| **Allocated Storage** | Baseline Storage | `10 GB` (`PD_SSD`) | `VERIFIED` |
| **Auto Storage Increase** | Enabled | `true` (Unlimited limit) | `VERIFIED` |
| **Availability Type** | Zonal Baseline | `ZONAL` | `VERIFIED` |
| **Edition** | Enterprise Edition | `ENTERPRISE` | `VERIFIED` |

---

## 3. Production Application Database & User Status

* **Canonical Database Name:** `sucharu_pro` (Sourced from `BackendConfig.kt`)
* **Database State:** `CREATED` (`charset: UTF8`, `collation: en_US.UTF8`)
* **Intended Application User:** `sucharu_app`
* **User Creation State:** `DEFERRED` (Awaiting operator population of `DATABASE_PASSWORD` secret version in Secret Manager). No fake passwords were generated or used.
* **Password Exposure:** `NO` (Zero passwords printed, stored, or exposed).

---

## 4. Backup, PITR & Encryption Configuration

| Feature Category | Parameter | Actual Live Configuration |
| :--- | :--- | :--- |
| **Automated Backups** | Enabled Status | `false` (Default baseline) |
| **Point-In-Time Recovery** | PITR Status | `false` (Default baseline) |
| **Backup Retention** | Retention Unit / Count | `7 days` (`COUNT`) |
| **Disk Encryption** | Key Management | `Google-managed key` |

---

## 5. Networking State

| Networking Parameter | Live State | Details |
| :--- | :--- | :--- |
| **Public IPv4 Address** | `34.21.131.221` | `ipv4Enabled: true` |
| **Private IP (PSA)** | `NOT_CONFIGURED` | Deferred to Step 07 (VPC & Serverless VPC Access Connector) |
| **VPC Connector Created?**| `NO` | VPC networking provisioning belongs to Step 07 |
| **SSL/TLS Policy** | `ALLOW_UNENCRYPTED_AND_ENCRYPTED` | Server CA cert generated (`sucharu-postgres-db`) |

---

## 6. Flyway & Data Safety Verification

* **Flyway Migration Execution:** `NOT PERFORMED` (Flyway execution belongs to Step 09).
* **Production Data Migration:** `NOT PERFORMED`.
* **Schema / SQL Imports:** `NOT PERFORMED`.

---

## 7. Source Code & Git Safety Verification

* **Branch:** `feature/wall-ui-redesign`
* **HEAD Commit:** `afb6534cb0f39999b160376dd96dc3260b86c8fd`
* **Application Source Code:** Unmodified (`0` changes)
* **Gradle / Docker / Migration Specs:** Unmodified (`0` changes)

---

## 8. Verification Summary & Step 06 Gate

Cloud SQL PostgreSQL 16 instance `sucharu-postgres-db` is `RUNNABLE` in region `asia-southeast1` with canonical application database `sucharu_pro`.

* **FINAL STATUS:** **`VERIFIED_WITH_GAPS`** *(Gaps: Private IP networking deferred to Step 07; application user creation deferred pending secret population; automated backups/PITR pending capacity tuning)*
* **STEP 06 READY:** **`YES`**
