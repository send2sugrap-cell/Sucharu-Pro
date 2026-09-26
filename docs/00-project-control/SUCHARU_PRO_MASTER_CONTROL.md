# SUCHARU PRO — MASTER CONTROL DOCUMENT
### Primary Project Continuity & Source-of-Truth Control Plane

---

## 1. PROJECT IDENTITY
- **Project Name**: Sucharu Pro (Unified Commercial Printing ERP & Server-Driven Wall Platform)
- **Organization / Brand**: Sucharu Graphics & Printing
- **Business Scope**: Commercial Printing, Offsets, Digital Fast Printing, Packaging & Boxes, Gift/Promotional Merchandise, Publishing, Subcontracting, Preflight, and Affiliate Governance.

---

## 2. REPOSITORY IDENTITY
- **Root Directory**: `E:\App\Sucharu Pro`
- **GitHub Remote**: `https://github.com/send2sugrap-cell/Sucharu-Pro.git`
- **Primary Branch**: `main`
- **Current Active Development Branch**: `feature/wall-ui-redesign`
- **Current Active HEAD Commit**: `e1334e053c3933ce851ac1818a737c9d0f2a74e7` (`e1334e0`)

---

## 3. CANONICAL ARCHITECTURE HIERARCHY
```
1. Actual Source Code / PostgreSQL Migrations / Domain Logic (Highest Authority)
2. Verified Implementation Evidence / Automated Test Reports
3. Locked Master Audit Reports (`docs/audit/MASTER_AUDIT_MODULE_00_24_FINAL.md`)
4. Project Control Documents (`docs/00-project-control/*`)
5. Historical Step Implementation Reports (`docs/infrastructure/*`)
6. Conversation / Prompt Context (Lowest Authority - Cannot Override Repository Evidence)
```

---

## 4. MODULES 00–24 STATUS SUMMARY
All 25 canonical ERP modules (Modules 00–24) are fully implemented and locked in `docs/audit/MASTER_AUDIT_MODULE_00_24_FINAL.md`.
- **Module 00**: Architecture Core (VERIFIED)
- **Module 01**: Authentication & RBAC (VERIFIED)
- **Module 02**: Customer Management (VERIFIED)
- **Module 03**: Quotation & Order (VERIFIED)
- **Module 04**: Production Execution & 13 Stages (VERIFIED)
- **Module 05**: Design & Approval (VERIFIED)
- **Module 06**: Prepress & Quality Control (VERIFIED)
- **Module 07**: Finished Goods Inventory (VERIFIED)
- **Module 08**: Delivery & Dispatch (VERIFIED)
- **Module 09**: Finance & Receipts (VERIFIED)
- **Module 10**: Communication & Notifications (VERIFIED)
- **Module 11**: Returns & Replacements (VERIFIED)
- **Module 12**: Vendor Subcontracting (VERIFIED)
- **Module 13**: Procurement (VERIFIED)
- **Module 14**: Customer Financial Accounts (VERIFIED)
- **Module 15**: General Ledger & Cost Allocations (VERIFIED)
- **Module 16**: Human Resources (VERIFIED)
- **Module 17**: Fixed Assets (VERIFIED)
- **Module 18**: Pricing & Rate Cards (VERIFIED)
- **Module 19**: Stock Reservation (VERIFIED)
- **Module 20**: Affiliate Program (VERIFIED)
- **Module 21**: Machine Telemetry & OEE (VERIFIED)
- **Module 22**: Preflight Engine (VERIFIED)
- **Module 23**: Affiliate Wallet & Payouts (VERIFIED)
- **Module 24**: Reports, Analytics & Audit (VERIFIED - All 15 Categories & 138 Reports)

---

## 5. FORMS 01–06 STATUS SUMMARY
Forms 01–06 form the canonical Server-Driven Presentation, Design, Offer, Commercial Pricing, Order Orchestration, and Wall Publishing Layer:
- **FORM 01 — Content & Product Foundation**: **IMPLEMENTATION COMPLETE, RUNTIME VERIFICATION BLOCKED** (Commit `f60c234`)
- **FORM 02 — Full Admin Visual Design Studio**: **IMPLEMENTATION COMPLETE, RUNTIME VERIFICATION BLOCKED** (Commit `30098f3` / `a1e4dd3`)
- **FORM 03 — Offer & Audience Eligibility**: **IMPLEMENTATION COMPLETE, RUNTIME VERIFICATION BLOCKED** (Commit `155eaa0`)
- **FORM 04 — Pricing & Commercial Rules**: **IMPLEMENTATION COMPLETE, RUNTIME VERIFICATION BLOCKED** (Commit `13dcc27`)
- **FORM 05 — ERP / Order / Fulfillment Integration**: **IMPLEMENTATION COMPLETE, RUNTIME VERIFICATION BLOCKED** (Commit `ed8a249`)
- **FORM 06 — Wall / Section / Publishing Control**: **IMPLEMENTATION COMPLETE, RUNTIME VERIFICATION BLOCKED** (Commit `e1334e0`)

---

## 6. CURRENT ACTIVE WORK
- **Active Task**: CONTROL-01 — Project Continuity System Creation (Creating `docs/00-project-control/*`).
- **Next Task**: Maintenance & evidence lock audits for server-driven wall presentation.

---

## 7. COMPLETED / LOCKED WORK
- Modules 00–24 ERP Platform (Commit `9ba295c`)
- Form 01 Content Foundation (Commit `f60c234`)
- Form 02 Visual Design Studio (Commit `30098f3` / `a1e4dd3`)
- Form 03 Offer & Audience Eligibility (Commit `155eaa0`)
- Form 04 Pricing & Commercial Rules (Commit `13dcc27`)
- Form 05 ERP Integration (Commit `ed8a249`)
- Form 06 Server-Driven Wall Control (Commit `e1334e0`)

---

## 8. PROTECTED WORK & INVARIANTS
1. **No Module 25**: Strict Modules 00–24 boundary.
2. **Single Product Master**: `InventoryProduct` (`Module 07`) is the single canonical source of truth for products.
3. **Single Category Master**: `InventoryProductCategory` (`Module 07`) is the single canonical source of truth for categories.
4. **Offer Eligibility $\neq$ Wall Visibility**:
   - Offer Eligibility determines *who can REDEEM an offer*.
   - Wall Visibility determines *where an offer is DISPLAYED*.
5. **Historical Price Snapshot Immutability**: Order price snapshots saved in Form 04 (`OrderPriceSnapshot`) must remain 100% UNCHANGED even if Master Price changes in Form 04 later.
6. **Production Stage Pipeline**: 13 canonical production stages (`DESIGN` $\rightarrow$ `DELIVERED`) are strictly separated from `OrderStatusType`.
7. **PostgreSQL & Flyway**: Canonical relational database and migration system.

---

## 9. CURRENT GIT BASELINES
- **Module 00–24 Baseline**: `9ba295c`
- **Form 01 Baseline**: `f60c234`
- **Form 02 Baseline**: `30098f3` / `a1e4dd3`
- **Form 03 Baseline**: `155eaa0`
- **Form 04 Baseline**: `13dcc27`
- **Form 05 Baseline**: `ed8a249`
- **Form 06 Baseline**: `e1334e0`

---

## 10. DATABASE & FLYWAY BASELINE
- **Flyway Migrations Range**: `V20260801` through `V20261216`.
- **Latest Form Migrations**:
  - `V20261209__create_content_product_foundation_tables.sql` (Form 01)
  - `V20261210__create_visual_design_studio_tables.sql` (Form 02)
  - `V20261211__create_visual_design_versions_table.sql` (Form 02 Versions)
  - `V20261212__add_semantic_design_properties.sql` (Form 02 Semantic Properties)
  - `V20261213__create_offer_and_audience_eligibility_tables.sql` (Form 03)
  - `V20261214__create_pricing_and_commercial_rules_tables.sql` (Form 04)
  - `V20261215__create_erp_workflow_orchestration_tables.sql` (Form 05)
  - `V20261216__create_wall_section_publishing_tables.sql` (Form 06)

---

## 11. SECURITY ARCHITECTURE
- **Authentication**: JWT token authentication via `BackendSecurityContext`.
- **Authorization**: Capability-based RBAC via `RoleCapabilityMatrix` and `ResourceOwnershipGuard`.
- **Multi-Tenancy & RLS**: Every table enforces `project_id REFERENCES tenants(project_id)` and PostgreSQL Row-Level Security policies via `TenantContext`.

---

## 12. KNOWN RUNTIME GAPS
- **Docker / Testcontainers Runtime Verification**: Local host environment lacked an active Docker daemon during offline execution. Source code, Flyway SQL DDL, unit test suites (`:core:jvmTest`), and Gradle builds (`assembleDebug`) are 100% verified, while live Testcontainers database execution is marked **DATABASE RUNTIME VERIFICATION BLOCKED — Docker Engine unavailable**.
- **Physical Factory Equipment & Mobile Hardware**: Physical offset presses, CTP equipment, and physical Android mobile hardware are marked **PENDING / EXTERNAL HARDWARE GAP** in CI.

---

## 13. CONTINUITY PRINCIPLE
All AI agents, developers, and tools working on Sucharu Pro must inspect this control plane to maintain architectural invariants, protect completed baselines, and prevent duplicate business logic or regressions.
