# SUCHARU PRO ERP — MODULE 16 STEP 04: CUSTOMER-WISE PROFITABILITY & CONTRIBUTION ANALYSIS ENGINE
## Comprehensive Implementation & Verification Report

---

### Executive Summary

Module 16 Step 04 implements the **Customer-Wise Profitability & Contribution Analysis Engine** for Sucharu Pro ERP. It calculates, analyzes, tracks, ranks, and compares customer profitability and contribution metrics across all canonical business dimensions while preserving strict source-of-truth invariants.

---

### 1. Architectural Foundation & Canonical Authority Map

- **Zero Second Ledger**: Operates strictly as an analytical, attribution, projection, and reconciliation layer without creating duplicate financial ledgers or altering customer balances.
- **Revenue Authority**: Directly consumes canonical recognized revenue from **Module 14 — Customer Accounts & Payment** (invoices, invoice lines, payments, credits, and adjustments).
- **Actual Cost Authority**: Consumes authoritative job actual cost snapshots and cost components from **Module 16 Step 02**.
- **Product Profitability Cross-Check**: Interfaces with **Module 16 Step 03** for product-level profitability and unit economics cross-checks.
- **Overhead Allocation Authority**: Integrates with **Module 15** for business expenses and approved cost center allocations.
- **Customer Identity**: Preserves canonical Customer identities from **Module 02**.

---

### 2. Attribution Hierarchy & Precedence Rules

Deterministic attribution hierarchy:
1. **Priority 1 — Direct Customer Attribution**: Direct attribution when a canonical financial record references `customerId`.
2. **Priority 2 — Order Attribution**: Attributed via `orderId -> customerId`.
3. **Priority 3 — Job Attribution**: Attributed via `jobId -> orderId -> customerId` consuming Step 02 actual job cost components.
4. **Priority 4 — Product Attribution**: Cross-checks product-level profitability against customer lines.
5. **Priority 5 — Approved Indirect Allocation**: Allocates indirect overhead using approved basis (`revenue ratio`, `order count`, `production quantity`, etc.).
6. **Unattributed Diagnostics**: Explicitly detects and records `UNATTRIBUTED_REVENUE` and `UNATTRIBUTED_COST` rather than silently assigning them to arbitrary customers.

---

### 3. Core Financial & Unit Economics Formulas

All calculations use `BigDecimal` with 4-decimal scale (`SCALE = 4`, `RoundingMode.HALF_UP`) and zero-safe division:
- **Gross Profit**:
  $$\text{Gross Profit} = \text{Recognized Revenue} - \text{Total Actual Cost}$$
- **Gross Margin %**:
  $$\text{Gross Margin \%} = \frac{\text{Gross Profit}}{\text{Recognized Revenue}} \times 100 \quad (\text{null if } \text{Revenue} \le 0)$$
- **Contribution Amount**:
  $$\text{Contribution Amount} = \text{Recognized Revenue} - \text{Attributable Variable Cost}$$
- **Contribution Margin %**:
  $$\text{Contribution Margin \%} = \frac{\text{Contribution Amount}}{\text{Recognized Revenue}} \times 100 \quad (\text{null if } \text{Revenue} \le 0)$$
- **Cost to Revenue %**:
  $$\text{Cost to Revenue \%} = \frac{\text{Total Actual Cost}}{\text{Recognized Revenue}} \times 100$$
- **Average Order Value (AOV)**:
  $$\text{AOV} = \frac{\text{Recognized Revenue}}{\text{Order Count}} \quad (\text{null if } \text{Order Count} \le 0)$$
- **Average Job Value (AJV)**:
  $$\text{AJV} = \frac{\text{Recognized Revenue}}{\text{Job Count}} \quad (\text{null if } \text{Job Count} \le 0)$$
- **Average Revenue Per Unit (ARPU)**:
  $$\text{ARPU} = \frac{\text{Recognized Revenue}}{\text{Total Quantity Sold}} \quad (\text{null if } \text{Quantity} \le 0)$$
- **Average Cost Per Unit**:
  $$\text{Average Cost Per Unit} = \frac{\text{Total Actual Cost}}{\text{Total Quantity Sold}} \quad (\text{null if } \text{Quantity} \le 0)$$
- **Average Profit Per Unit**:
  $$\text{Average Profit Per Unit} = \frac{\text{Gross Profit}}{\text{Total Quantity Sold}} \quad (\text{null if } \text{Quantity} \le 0)$$

---

### 4. Classification, Trends & Concentration Intelligence

1. **7-Tier Profitability Classification**:
   - `HIGHLY_PROFITABLE`: Gross Margin $\ge 30.00\%$
   - `PROFITABLE`: $15.00\% \le \text{Gross Margin} < 30.00\%$
   - `LOW_MARGIN`: $0.00\% < \text{Gross Margin} < 15.00\%$
   - `BREAK_EVEN`: $\text{Gross Margin} == 0.00\%$
   - `LOSS_MAKING`: $\text{Gross Margin} < 0.00\%$
   - `NO_REVENUE`: Revenue $= 0$ and Cost $= 0$
   - `INSUFFICIENT_DATA`: Source not ready or missing

2. **6-Tier Directional Profitability Trend**:
   - `STRONGLY_IMPROVING`: Margin change $> +5.0\%$
   - `IMPROVING`: $+1.0\% \le \text{Margin change} \le +5.0\%$
   - `STABLE`: $-1.0\% \le \text{Margin change} \le +1.0\%$
   - `DECLINING`: $-5.0\% \le \text{Margin change} < -1.0\%$
   - `STRONGLY_DECLINING`: Margin change $< -5.0\%$
   - `INSUFFICIENT_DATA`: Missing previous period baseline

3. **Customer Concentration & Dependency Risk**:
   - Top 1, Top 5, Top 10 Revenue and Profit share percentages.
   - `CONCENTRATION_HIGH`: Top 1 share $> 25\%$ or Top 5 share $> 60\%$
   - `CONCENTRATION_MODERATE`: Top 1 share $> 10\%$ or Top 5 share $> 30\%$
   - `CONCENTRATION_LOW`: Diversified revenue base

---

### 5. Provenance, Deduplication & Non-Mutating Reconciliation

- **Deterministic SHA-256 Fingerprint**:
  $$\text{tenantId} : \text{customerId} : \text{sourceModule} : \text{sourceEntityType} : \text{sourceEntityId} : \text{sourceTransactionId} : \text{componentType}$$
- **Snapshot Integrity Hash**: Sorted SHA-256 checksum over components, attributions, and financial totals.
- **Reconciliation Engine (`CustomerProfitabilityReconciliationServiceImpl.kt`)**:
  - Verifies $\sum \text{Revenue Attributions} == \text{Snapshot Revenue}$
  - Verifies $\sum \text{Cost Component Amounts} == \text{Snapshot Total Cost}$
  - Verifies $\text{Revenue} - \text{Total Cost} == \text{Gross Profit}$
  - Verifies $\text{Revenue} - \text{Variable Cost} == \text{Contribution Amount}$
  - Emits non-mutating reconciliation and audit events.

---

### 6. Database Schema & Flyway Migration

Flyway migration `V20261027__create_customer_profitability_contribution_analysis.sql`:
1. `customer_profitability_snapshots`
2. `customer_profitability_components`
3. `customer_profitability_revenue_attributions`
4. `customer_profitability_cost_attributions`
5. `customer_profitability_reconciliation_events`
6. `customer_profitability_audit_events`
7. `customer_profitability_unattributed_items`
- All tables have `ENABLE ROW LEVEL SECURITY` and `FORCE ROW LEVEL SECURITY` with `app.current_tenant` isolation policies.

---

### 7. REST APIs (`BackendRouter.kt` & `BackendUseCases.kt`)

| Method | Route | Description | RBAC |
|---|---|---|---|
| `POST` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/calculate` | Calculate customer profitability snapshot | Admin, Manager |
| `GET` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability` | Get latest customer snapshot | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/components` | Get 12 cost component breakdown | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/provenance` | Get revenue & cost provenance trace | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/orders` | Get order-level profitability attribution | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/jobs` | Get job-level actual cost attribution | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/products` | Get product contribution to customer | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/trend` | Get customer profitability trend | Admin, Manager, Staff |
| `POST` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/reconcile` | Run non-mutating reconciliation | Admin, Manager |
| `GET` | `/api/v1/profit-cost-analysis/customers/{customerId}/profitability/audit` | Get customer audit event history | Admin, Manager |
| `GET` | `/api/v1/profit-cost-analysis/customers/ranking` | Rank customers by criteria | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/loss-making` | Filter loss-making customers | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/low-margin` | Filter low-margin customers | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/concentration` | Get customer concentration intelligence | Admin, Manager, Staff |
| `POST` | `/api/v1/profit-cost-analysis/customers/comparison` | Compare selected customers side by side | Admin, Manager, Staff |
| `GET` | `/api/v1/profit-cost-analysis/customers/unattributed` | Get unattributed diagnostics | Admin, Manager |
| `GET` | `/api/v1/profit-cost-analysis/customers/profitability` | List/filter customer profitability snapshots | Admin, Manager, Staff |

---

### 8. Jetpack Compose UI (12 Screens)

Located in `app/src/main/java/com/sucharu/sucharupro/ui/features/profitability/customer/`:
1. `CustomerProfitabilityHubScreen.kt` — Central hub with financial summary and analytical exploration links.
2. `CustomerProfitabilityDetailsScreen.kt` — Comprehensive metadata, margins, variables, and integrity status.
3. `CustomerProfitabilityCostBreakdownScreen.kt` — 12 canonical cost components with visual progress bars.
4. `CustomerProfitabilityTrendScreen.kt` — Directional trajectory and period-over-period margin comparison.
5. `CustomerProfitabilityOrdersScreen.kt` — Order-level revenue, cost, and margin attribution.
6. `CustomerProfitabilityJobsScreen.kt` — Job-level production cost attribution.
7. `CustomerProfitabilityProductsScreen.kt` — Product-level contribution to customer profitability.
8. `CustomerProfitabilityRankingScreen.kt` — Customer rankings by revenue, profit, margin, and contribution.
9. `CustomerProfitabilityComparisonScreen.kt` — Side-by-side multi-customer comparative cards.
10. `CustomerProfitabilityReconciliationScreen.kt` — Verification checklist and discrepancy reporting.
11. `CustomerProfitabilityConcentrationScreen.kt` — Top 1/5/10 concentration stats and dependency risk.
12. `CustomerProfitabilityProvenanceScreen.kt` — SHA-256 fingerprint traceability to canonical sources.

---

### 9. Verification & Build Results

- `:core:test` — **100% Passed (BUILD SUCCESSFUL)**
- `:backend:test` — **100% Passed (BUILD SUCCESSFUL)**
- `:backend:jar` — **100% Passed (BUILD SUCCESSFUL)**
- `:app:testDebugUnitTest` — **100% Passed (BUILD SUCCESSFUL)**
- `:app:assembleDebug` — **100% Passed (BUILD SUCCESSFUL)**

---

### 10. Module 16 Step 05 Handoff Contract

Module 16 Step 04 produces customer profitability snapshots, contribution metrics, trends, rankings, concentration intelligence, cost breakdowns, provenance traces, and reconciliation events ready for consumption by **Module 16 Step 05: Vendor / Outsource Profitability & Cost Efficiency Engine** and executive analytical reporting.
