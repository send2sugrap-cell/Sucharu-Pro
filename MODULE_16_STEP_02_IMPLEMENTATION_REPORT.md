# SUCHARU PRO ERP — MODULE 16 → STEP 02 IMPLEMENTATION REPORT
## JOB-WISE ACTUAL COST CALCULATION ENGINE

---

### 1. EXECUTIVE SUMMARY
**Module 16 (Profit & Cost Analysis) — Step 02** establishes the authoritative, production-grade **Job-Wise Actual Cost Calculation Engine** for Sucharu Pro ERP.

The engine computes the true actual cost of an individual production Job by aggregating, attributing, deduplicating, calculating, reconciling, and snapshotting operational and financial cost data across canonical ERP modules without creating a second ledger or shadow balances.

---

### 2. CANONICAL AUTHORITY MAP

| Canonical Module | Authoritative Records | Module 16 Step 02 Role |
|---|---|---|
| **Module 04 (Job Card & Production)** | Production stages, operator workloads, machine executions, output records | Labour, Machine, and Production Operation Cost Attributions |
| **Module 06 (QC & Rework)** | `QcCostEntry`, rework records, defect evidence | Rework and Quality Cost Attributions |
| **Module 08 (Inventory & Movements)** | `InventoryStockOut`, material consumption lines, unit costs | Material & Wastage Cost Attributions |
| **Module 12 (Vendor Management)** | Vendor Work Orders, Purchase Orders, Outsource assignments | Vendor / Outsource Cost Attributions |
| **Module 14 (Customer Accounts)** | Invoices, receivables, receipts | Read-only Customer Context |
| **Module 15 (Expense & Ledger)** | Business Expenses, Vendor Payables, Cost Allocations, Financial Periods | Direct Expenses, Vendor Liabilities, Overhead Allocations |
| **Module 16 Step 01 (Foundation)** | Precision math, financial handoff adapter, source registry, reconciliation framework | Extended by Step 02 Job Cost Engine |
| **Module 17 (Smart Printing Calculator)** | Estimated baseline cost, quote assumptions | Consumed via `JobCostEstimationBaselineProvider` |

---

### 3. 12-COMPONENT JOB COST MODEL

The calculation engine models 12 distinct cost categories with explicit directness classification:

1. `MATERIAL_COST` (DIRECT) — Raw paper, ink, plates, and materials consumed by the Job.
2. `LABOUR_COST` (DIRECT) — Direct operator/staff labor time applied to the Job.
3. `MACHINE_COST` (DIRECT) — Direct machine runtime hours applied to the Job.
4. `PRODUCTION_OPERATION_COST` (DIRECT) — Specific manufacturing process/step costs.
5. `VENDOR_OUTSOURCE_COST` (DIRECT) — Specialized subcontracting and vendor services.
6. `REWORK_COST` (DIRECT) — Corrective labor, material, and vendor rework expenses.
7. `WASTAGE_COST` (DIRECT) — Material and process scrap costs.
8. `FINISHING_COST` (DIRECT) — Cutting, binding, lamination, foiling charges.
9. `PACKAGING_COST` (DIRECT) — Boxing, wrapping, strapping materials.
10. `TRANSPORT_COST` (DIRECT) — Job-specific delivery and logistics charges.
11. `OTHER_DIRECT_COST` (DIRECT) — Miscellaneous direct expenses.
12. `ALLOCATED_INDIRECT_COST` (INDIRECT) — Approved shared facility/overhead cost pools.

---

### 4. DIRECT VS. INDIRECT COST ALLOCATION

- **Direct Costs**: Directly linked to `jobId`.
- **Indirect Overhead**: Allocated according to approved allocation bases:
  - `MACHINE_HOURS`
  - `LABOUR_HOURS`
  - `PRODUCTION_QUANTITY`
  - `DIRECT_COST_RATIO`
  - `CUSTOM_APPROVED_RATIO`
- **Unallocated Safety**: If no canonical allocation basis exists, marked as `UNALLOCATED` instead of inventing arbitrary overhead values.

---

### 5. PROVENANCE & DEDUPLICATION FINGERPRINTING

Every cost component retains full provenance:
- `sourceModule`, `sourceEntityType`, `sourceEntityId`, `sourceTransactionId`, `sourceReference`.
- Deterministic fingerprint hash:
  $$\text{Fingerprint} = \text{sourceModule} : \text{sourceEntityType} : \text{sourceEntityId} : \text{sourceTransactionId} : \text{componentType}$$
- **Duplicate Prevention**: If a vendor commitment appears as both an estimated Work Order and a matched Vendor Payable, the engine prioritizes the final payable and deduplicates the transaction to prevent double counting.

---

### 6. MATHEMATICAL PRECISION & INVARIANTS

Implemented in `JobCostMathUtils.kt`:
- **Scale & Rounding**: Strict `BigDecimal` (scale 4, `RoundingMode.HALF_UP`).
- **Total Direct Cost**: $\text{Total Direct Cost} = \sum \text{Direct Components}$
- **Total Actual Cost**: $\text{Total Actual Cost} = \text{Total Direct Cost} + \text{Approved Allocated Indirect Cost}$
- **Cost Variance**: $\text{Cost Variance} = \text{Actual Cost} - \text{Estimated Cost}$
- **Cost Variance %**: $\text{Cost Variance \%} = \left(\frac{\text{Actual Cost} - \text{Estimated Cost}}{\text{Estimated Cost}}\right) \times 100$
- **Zero Baseline Guard**: When estimated baseline is null or $\le 0$, safely returns `BASELINE_UNAVAILABLE` without division by zero.
- **Classification Policy**:
  - `UNDER_BUDGET` ($\text{Variance \%} < -2.00\%$)
  - `ON_TARGET` ($-2.00\% \le \text{Variance \%} \le +2.00\%$)
  - `OVER_BUDGET` ($\text{Variance \%} > +2.00\%$)
  - `BASELINE_UNAVAILABLE`

---

### 7. SHA-256 INTEGRITY HASH

Computed over stable normalized fields:
$$\text{SHA-256}(\text{tenantId} \mid \text{projectId} \mid \text{jobId} \mid \text{version} \mid \text{actualCost} \mid \text{directCost} \mid \text{indirectCost} \mid \text{sortedComponentHashes})$$
Ensures snapshot tamper-evidence without blockchain overhead.

---

### 8. NON-MUTATING RECONCILIATION ENGINE

`JobCostReconciliationService` performs verification:
1. Verifies $\sum \text{Component Amounts} == \text{Total Actual Cost}$ ($\Delta \le 0.0001$).
2. Verifies $\sum \text{Provenance Attributed Amounts} == \text{Total Actual Cost}$ ($\Delta \le 0.0001$).
3. Checks duplicate counts and unresolved sources.
4. Emits `JobCostReconciliationEvent` recording discrepancies without modifying canonical data.

---

### 9. MULTI-TENANT SECURITY & RLS

- PostgreSQL Row-Level Security (`ENABLE ROW LEVEL SECURITY`, `FORCE ROW LEVEL SECURITY`) with `tenant_id = CURRENT_SETTING('app.current_tenant', true)`.
- Server-side token context determines tenant and project boundaries.
- RBAC Enforcement:
  - `ADMIN`, `MANAGER`, `STAFF`, `ACCOUNTS`: Allowed.
  - `CUSTOMER`, `VENDOR`: Explicitly blocked (HTTP 403 Forbidden).

---

### 10. DATABASE SCHEMA & FLYWAY MIGRATION

Flyway migration `V20261025__create_job_wise_actual_cost_engine.sql` added to:
- `core/src/main/resources/db/migration/`
- `database/migrations/`

Tables:
- `job_cost_snapshots`
- `job_cost_components`
- `job_cost_provenance_records`
- `job_cost_reconciliation_events`
- `job_cost_audit_events`

---

### 11. REST API ENDPOINTS

| Method | Endpoint | Description | Auth Roles |
|---|---|---|---|
| `POST` | `/api/v1/profit-cost-analysis/jobs/{jobId}/actual-cost/calculate` | Execute Job actual cost calculation | `ADMIN`, `MANAGER`, `STAFF` |
| `GET` | `/api/v1/profit-cost-analysis/jobs/{jobId}/actual-cost` | Get latest Job actual cost snapshot | `ADMIN`, `MANAGER`, `STAFF` |
| `GET` | `/api/v1/profit-cost-analysis/jobs/{jobId}/actual-cost/components` | List 12 cost components breakdown | `ADMIN`, `MANAGER`, `STAFF` |
| `GET` | `/api/v1/profit-cost-analysis/jobs/{jobId}/actual-cost/provenance` | List source provenance records | `ADMIN`, `MANAGER`, `STAFF` |
| `GET` | `/api/v1/profit-cost-analysis/jobs/{jobId}/actual-cost/variance` | Get cost variance against baseline | `ADMIN`, `MANAGER`, `STAFF` |
| `POST` | `/api/v1/profit-cost-analysis/jobs/{jobId}/actual-cost/reconcile` | Run non-mutating reconciliation | `ADMIN`, `MANAGER` |
| `GET` | `/api/v1/profit-cost-analysis/jobs/{jobId}/actual-cost/audit` | List Job cost audit trail logs | `ADMIN`, `MANAGER` |

---

### 12. JETPACK COMPOSE UI WORKSPACE

Created `JobActualCostAnalysisScreen.kt` in `app/src/main/java/com/sucharu/sucharupro/ui/features/profitability/`:
- Dark navy visual styling (`0xFF0B132B`, `0xFF1C2541`, `0xFF9ECAFF`).
- Header with Job details and readiness badge (`COMPLETE`, `PARTIAL`, `UNALLOCATED`, `CONFLICTED`).
- KPI Cards: Actual Cost, Estimated Baseline, Direct Cost, Indirect Overhead.
- Cost Variance Card with color-coded classification (`UNDER_BUDGET`, `ON_TARGET`, `OVER_BUDGET`).
- 12-component breakdown list with percentages, rates, and directness badges.
- Source warning and deduplication alert drawer.

---

### 13. MODULE 17 ESTIMATION HANDOFF CONTRACT

Created `JobCostEstimationBaselineProvider` interface allowing future Module 17 (Smart Printing Calculator) to supply estimated cost baselines for variance computation without gaining authority over actual financial ledger records.
