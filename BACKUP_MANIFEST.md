# SUCHARU PRO — GITHUB BACKUP MANIFEST

## Master ERP & Unified Graphics Platform

---

### 1. General Backup Metadata

| Metadata Field | Value |
| :--- | :--- |
| **Backup Timestamp** | `2026-09-02T19:10:00+06:00` |
| **Target Repository** | `send2sugrap-cell/Sucharu-Pro` |
| **Remote URL** | `https://github.com/send2sugrap-cell/Sucharu-Pro.git` |
| **Default Branch** | `main` |
| **Commit SHA** | `5bff514efa8475f21a1ffa45cdef3b43e733faf6` |
| **Local Project Path** | `e:\App\Sucharu Pro` |
| **Project Platform** | Kotlin Multi-Module (Android / Jetpack Compose + Core Domain + Server Backend) |
| **Build System** | Gradle 8.11.1 / Kotlin 2.0.21 / JVM 21 / Android SDK 35 |
| **Backup Status** | 🟢 **COMPLETE** |
| **Build Status** | 🟢 **PASS** |
| **Test Verification** | 🟢 **PASS (100% Green across all modules)** |

---

### 2. Modules & Capabilities Backed Up

| Module Identifier | Scope & Name | Production Status |
| :--- | :--- | :--- |
| **Module 00** | Architectural Core, Domain Envelopes, Multi-Tenant Context & Common Types | 🟢 CERTIFIED |
| **Module 01** | User Identity Lifecycle, Authentication, PBKDF2 Password History & OCC Profiles | 🟢 CERTIFIED |
| **Module 02** | Customer CRM, Customer Accounts & Credit Risk Assessment | 🟢 CERTIFIED |
| **Module 03** | Product Catalog, Paper Stock Specs, GSM Matrices & Item Definitions | 🟢 CERTIFIED |
| **Module 04** | Commercial Estimation, Dynamic Quotation & Cost Calculation Engine | 🟢 CERTIFIED |
| **Module 05** | Commercial Orders, Job Proofing Lifecycle & Order Milestone State Machine | 🟢 CERTIFIED |
| **Module 06** | Prepress Job Orchestration, File Ingestion & Production Artwork Verification | 🟢 CERTIFIED |
| **Module 07** | Production Job Planning, Scheduling & Machine Allocation | 🟢 CERTIFIED |
| **Module 08** | Shop-Floor Tracking, Live Machine Telemetry & Operator Station Dispatch | 🟢 CERTIFIED |
| **Module 09** | Quality Assurance, Final QC Inspection, Packaging & Release Governance | 🟢 CERTIFIED |
| **Module 10** | Logistics, Delivery Dispatch, Proof of Delivery (POD) & Shipping Run Engine | 🟢 CERTIFIED |
| **Module 11** | Customer Financial Billing, Invoicing Lifecycle & Commercial Settlements | 🟢 CERTIFIED |
| **Module 12** | General Ledger, Chart of Accounts, Double-Entry Journaling & Financial Reports | 🟢 CERTIFIED |
| **Module 13** | External Vendor Portal, Supplier Management, RFQs & Subcontracting | 🟢 CERTIFIED |
| **Module 14** | Inventory Master, Raw Material Warehouse & Stock Movement Ledger | 🟢 CERTIFIED |
| **Module 15** | Commercial Commitment, Quote-to-Order Conversion & Multi-Tier Approvals | 🟢 CERTIFIED |
| **Module 16** | Actual Job Costing, Period End Financials & Profitability Intelligence Engine | 🟢 CERTIFIED |
| **Module 17** | Production Job Closure, Governance Auditing & Financial Closure Engine | 🟢 CERTIFIED |
| **Module 18** | Dynamic Imposition, Gang-Run, 2D Nesting, Signature & CTP Output Engine | 🟢 CERTIFIED |
| **Module 19** | Substrate Stock Auto-Reservation, Soft/Hard Holds & Batch/Grain Matching | 🟢 CERTIFIED |
| **INFRA 01–05** | PostgreSQL RLS, Observability, Log Sanitization, Security Edges & Deploy Ops | 🟢 CERTIFIED |

---

### 3. Excluded & Sanitized Artifacts

- **Excluded Local Build Files**: `build/`, `app/build/`, `core/build/`, `.gradle/`, `.kotlin/`, `captures/`, `.externalNativeBuild/`, `.cxx/`.
- **Excluded IDE & OS Cache Files**: `.idea/`, `.artifacts/`, `.DS_Store`, `Thumbs.db`.
- **Excluded Logs & JVM Crash Dumps**: `*.log`, `hs_err_pid*`, `replay_pid*`, `test_output.txt`.
- **Excluded Machine Credentials**: `local.properties`, local `.env` files.
- **Sanitized Config Templates Preserved**:
  - `deploy/.env.production.example` (All credentials configured as `YOUR_SECRET_HERE` placeholders)
  - `deploy/docker-compose.production.yml`
  - `deploy/docker-compose.yml`
  - `deploy/Dockerfile.backend`
  - `deploy/nginx/`

---

### 4. Database Migrations & Schemas Backed Up

All SQL Flyway migrations located in `database/migrations/` and `core/src/main/resources/db/migration/` are fully preserved:
- `V20260830__create_auth_and_session_tables.sql`
- `V20260901__create_customer_and_credit_tables.sql`
- `V20260903__create_product_and_pricing_tables.sql`
- `V20260905__create_commercial_and_quote_tables.sql`
- `V20260908__create_production_and_job_tables.sql`
- `V20260910__create_inventory_and_stock_tables.sql`
- `V20260913__force_row_level_security.sql`
- ...
- `V20261118__create_substrate_reservation_tables.sql`
- `V20261119__create_substrate_soft_hard_reservation_tables.sql`
- `V20261120__create_substrate_batch_lot_selection_tables.sql`
(All enforcing PostgreSQL Row Level Security).

---

### 5. Architectural Documents & Forensic Reports Backed Up

- `docs/` complete technical architecture documentation suite.
- All 80+ root historical verification and certification markdown reports:
  - `MODULE_18_FINAL_GATE_REPORT.md`
  - `MODULE_18_MODULE_19_INTEGRATION_AUDIT.md`
  - `MODULE_19_STEP_01_IMPLEMENTATION_REPORT.md`
  - `MODULE_19_STEP_02_IMPLEMENTATION_REPORT.md`
  - `MODULE_19_STEP_03_IMPLEMENTATION_REPORT.md`
  - `MODULE_19_STEP_03_READINESS_GATE.md`
  - All Module 00–17, INFRA 01–05, and Audit Matrix reports.

---

### 6. Git Snapshot Summary

- **Repository**: `https://github.com/send2sugrap-cell/Sucharu-Pro.git`
- **Branch**: `main`
- **Commit Message**: `chore(backup): complete Sucharu Pro ERP project backup`
- **Integrity**: Full clean working tree with comprehensive multi-module tree snapshot.
