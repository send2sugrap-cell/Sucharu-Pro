# MODULE 19 STEP 01 — PRE-IMPLEMENTATION ARCHITECTURE & CONTRACT AUDIT

## Substrate Requirement Resolution & Inventory Interlock

**VERSION:** Pre-Implementation Architecture Audit v1.0  
**MODE:** READ-ONLY — NO CODE CHANGES  
**AUDIT DATE:** September 2, 2026  

---

### 1. Canonical Identity & Protected Baseline

```text
Canonical Module:        Module 19 — Substrate Stock Auto-Reservation
Canonical Step:          Step 01 — Substrate Requirement Resolution & Inventory Interlock
Canonical Roadmap File:  e:\App\Sucharu Pro\DEMO_MODULE_ACCESS_MATRIX.md
Status:                  Verified & Ready for Design / Implementation
Protected Baseline:      Modules 00 → 18 (100% Intact & Untouched)
```

---

### 2. Module 18 / 17 Input Contract Discovery

Module 18 and Module 17 (Steps 01, 02, 04) produce structured imposition, material demand, and cutting layout outputs.

| Module 18 / 17 Output Field | Data Type | Canonical Source Class | Business Meaning | Module 19 Step 01 Usage |
| :--- | :--- | :--- | :--- | :--- |
| `stockType` | `PaperStockType` | `PaperMaterialSpecification` | Canonical substrate category (e.g. `ART_PAPER`, `ART_CARD`, `BOX_BOARD`). | Category filtering in inventory. |
| `materialCode` / `materialName` | `String` | `PaperMaterialSpecification` | Canonical substrate name/code. | Primary SKU resolution key. |
| `gsm` | `BigDecimal` | `PaperMaterialSpecification` | Paper weight in grams per square meter (e.g. `300.0000`). | Exact weight matching. |
| `sheetDimension` | `PrintingDimension` | `PaperMaterialSpecification` | Parent sheet dimensions (e.g. 25" x 36" in mm). | Sheet size matching. |
| `productiveSheetsRequired` | `Long` | `MaterialRequirementResult` | Net sheets required for finished quantity. | Net demand accounting. |
| `wasteSheetsRequired` | `Long` | `MaterialRequirementResult` | Makeready, setup & running waste sheets. | Waste allowance buffer. |
| `totalSheetsRequired` | `Long` | `MaterialRequirementResult` | Gross sheets required (`productive + waste`). | **Total reservation demand (Units/Sheets)**. |
| `totalReamsRequired` | `BigDecimal` | `MaterialRequirementResult` | Gross quantity expressed in 500-sheet reams. | Wholesale ream allocation. |
| `totalWeightKg` | `BigDecimal` | `MaterialRequirementResult` | Total physical tonnage/weight in kilograms. | Warehouse logistics limit. |
| `calculationId` / `orderId` | `String` | `PrintingCalculationRequest` | Unique job & commercial anchor IDs. | Nonce & idempotency key. |

---

### 3. Module 06 Canonical Inventory Contract Discovery

Module 06 owns the physical inventory, product master, and warehouse locations.

```text
Existing Canonical Inventory Authority:   YES
Owner Module:                             Module 06 (Inventory & Paper Substrate Stocks)
Product Master Entity:                    InventoryProduct (sku, normalizedSku, unitOfMeasure, productType)
Warehouse & Locations:                    InventoryWarehouse, InventoryLocation
Stock Out / Issue Document:               InventoryStockOut, InventoryStockOutLine (IssueType.PRODUCTION)
Stock Level Monitoring Policy:            InventoryStockLevelPolicy (reorderPoint, minimumStockLevel)
Existing Reservation State Machine:       Dedicated reservation entity NOT yet present in Module 06.
```

#### Discovery Finding:
Module 06 manages stock-in records, stock-out issues, physical locations, and reorder policies. **Module 19 provides the specialized Pre-Production Reservation & Allocation State Machine**, interlocking directly with Module 06's `InventoryProduct` SKUs without creating a duplicate stock ledger.

---

### 4. Requirement Resolution & SKU Matching Engine

```text
                     [Module 18 / 17 Imposition Output]
                       (Material Demand Specification)
                                      ↓
                     ┌────────────────────────────────┐
                     │ Step 01 Requirement Resolver   │
                     │  - Gross Sheets (Total Demand) │
                     │  - GSM & Paper Stock Type      │
                     │  - Sheet Dimensions (WxH)      │
                     └────────────────┬───────────────┘
                                      ↓
                     ┌────────────────────────────────┐
                     │ Canonical SKU Matcher          │
                     │  1. Exact Match: Normalized SKU│
                     │  2. Spec Match: StockType+GSM  │
                     │  3. Fallback: Compatible Sheet │
                     └────────────────┬───────────────┘
                                      ↓
                      [Module 06 Inventory Repository]
                      (Query On-Hand & Active Holds)
                                      ↓
                     ┌────────────────────────────────┐
                     │ Availability Evaluator         │
                     │ Available = OnHand - Reserved  │
                     └────────────────┬───────────────┘
                                      ↓
             ┌────────────────────────┴────────────────────────┐
             ▼                                                 ▼
      [Stock Available]                               [Stock Deficit]
 ┌─────────────────────────┐                     ┌─────────────────────────┐
 │ Substrate Reservation   │                     │ Auto-Replenishment /    │
 │ State: RESERVED_SOFT    │                     │ Reorder Alert Trigger   │
 └─────────────────────────┘                     └─────────────────────────┘
```

#### SKU Matching Rules:
1. **Exact Match**: Match `InventoryProduct.normalizedSku == request.materialCode.trim().uppercase()`.
2. **Specification Match**: Match on `PaperStockType` + `gsm` (±0.0000) + Parent Sheet Dimension (within ±1mm).
3. **No Match**: Return structured diagnostic `SUBSTRATE_SKU_NOT_FOUND`.

---

### 5. Inventory Availability & Concurrency Defense

#### 5.1 Availability Calculation
$$\text{Available Stock} = \text{OnHand Physical Stock} - \sum \text{Active Unreleased Reservations}$$
- Physical stock is never reduced upon initial reservation; it is marked as `Reserved / Allocated`.
- Physical stock deduction happens only upon physical dispatch to shop-floor via Module 06 `InventoryStockOut` (IssueType `PRODUCTION`).

#### 5.2 Concurrency & Race Condition Prevention
To prevent over-reservation during simultaneous order spikes:
- The reservation service executes inside `inTransaction(TenantContext(tenantId))`.
- Employs **PostgreSQL Row-Level Locking** (`SELECT ... FOR UPDATE`) on the target `inventory_products` record during allocation evaluation.
- An atomic condition verifies:
  $$\text{Current Available} \ge \text{Requested Sheets}$$
  If false, rolls back cleanly and raises `InsufficientSubstrateStockException`.

---

### 6. Idempotency & Tenant RLS Boundary

- **Idempotency Key**: Compound key derived from `tenantId:orderId:orderItemId:materialCode` or explicit `idempotencyKey`.
- **Tenant Isolation**:
  - All Module 19 queries and reservation records enforce `tenant_id = CURRENT_SETTING('app.current_tenant', true)`.
  - `FORCE ROW LEVEL SECURITY` mandatory on new reservation tables.
  - Zero cross-tenant reservation leakage.

---

### 7. RBAC & Capability Matrix

| Role | Permission on Substrate Reservation | Reason |
| :--- | :--- | :--- |
| **GUEST** | `DENIED (401 / 403)` | Unauthenticated / No access to internal inventory. |
| **CUSTOMER** | `DENIED (403)` | Customers view quotation prices, not raw warehouse allocations. |
| **VENDOR** | `DENIED (403)` | Subcontractors cannot manipulate host warehouse reservations. |
| **STAFF** | `READ & REQUEST (200)` | Operators and estimators can check availability and create reservations. |
| **MANAGER** | `FULL AUTHORITY (200)` | Production managers can override, reallocate, or cancel reservations. |
| **ADMIN** | `FULL AUTHORITY (200)` | System administrators have root authority. |
| **AI_AGENT** | `READ-ONLY HANDOFF (200)` | Read-only access to substrate allocation contracts. |

---

### 8. Substrate Reservation State Machine

```text
[REQUESTED] ──► [RESERVED_SOFT] ──► [ALLOCATED_HARD] ──► [ISSUED_TO_FLOOR] (Terminal Success)
      │                │                    │
      ▼                ▼                    ▼
 [DEFICIT]        [CANCELLED]          [CANCELLED]
                       ▲                    ▲
                       └──── [EXPIRED] ─────┘
```

- **`REQUESTED`**: Initial requirement resolved; availability check in progress.
- **`RESERVED_SOFT`**: Stock locked for commercial quote / pre-production hold (time-bounded).
- **`ALLOCATED_HARD`**: Order confirmed & job scheduled; physical pallets tagged for work order.
- **`ISSUED_TO_FLOOR`**: Material moved to pressroom via Module 06 stock-out (Completed).
- **`CANCELLED` / `EXPIRED`**: Reservation unlocked; stock returned to available pool.

---

### 9. Database, API & Event Architecture Impact

#### 9.1 Database Impact (New Migration)
- Migration: `V20261112__create_substrate_stock_reservation_tables.sql`
- Tables:
  - `substrate_reservations`: Core reservation records (IDs, orderId, jobId, SKU, reservedSheets, status, timestamps).
  - `substrate_reservation_audit_events`: Immutable audit timeline.
  - Both tables enabled with `FORCE ROW LEVEL SECURITY`.

#### 9.2 API Impact (New Endpoints)
- `POST /api/v1/substrate-reservations/resolve-requirement`: Resolves Module 18 imposition specs to Module 06 SKUs.
- `POST /api/v1/substrate-reservations/reserve`: Creates a soft/hard reservation with concurrency locking.
- `GET  /api/v1/substrate-reservations/jobs/{jobId}`: Retrieves active reservation for a production job.
- `POST /api/v1/substrate-reservations/{reservationId}/release`: Unlocks or commits reservation.

#### 9.3 Financial & Inventory Boundaries
- **Financial**: Substrate reservation creates **zero** general ledger entries (Module 15 is unaffected).
- **Inventory**: Module 06 remains sole canonical stock authority. Module 19 maintains the reservation ledger without duplicating stock balances.

---

### 10. Failure Scenarios & Handling

| Failure Case | Detection Mechanism | System Response |
| :--- | :--- | :--- |
| **1. SKU Not Found** | SKU lookup returns null in Module 06. | Returns 404 with diagnostic `SUBSTRATE_SKU_NOT_FOUND`. |
| **2. GSM / Dimension Mismatch** | Inventory SKU properties differ from job specs. | Flags `SUBSTRATE_SPEC_MISMATCH` with alternative suggestions. |
| **3. Insufficient Stock** | `Available < Requested`. | Creates `RESERVATION_DEFICIT` state & emits Reorder Alert event. |
| **4. Concurrent Race Condition** | Two requests compete for remaining stock. | Row lock serializes requests; second request cleanly fails with 409 Conflict. |
| **5. Duplicate Request** | Unique idempotency key exists. | Returns existing reservation idempotently without double-reserving. |
| **6. Cross-Tenant Attempt** | Tenant ID mismatch in token context. | Blocked by PostgreSQL RLS with empty result / 403 Forbidden. |

---

### 11. Test Strategy for Step 01

1. **Domain Tests**: Test requirement resolution math, sheet-to-ream conversions, GSM matching, and state transitions.
2. **Concurrency Tests**: Simulate simultaneous parallel coroutines reserving identical inventory SKUs to prove zero over-reservation.
3. **Security Edge Tests**: Multi-tenant isolation tests and RBAC verification (Staff vs Customer/Vendor).
4. **Integration Tests**: Module 18 imposition input -> Module 19 resolver -> Module 06 repository interlock.
5. **Full Regression**: Continuous validation of `.\gradlew.bat test` across Modules 00–18.

---

### 12. Pre-Implementation Gate Decision

## 🟢 READY FOR STEP 01 IMPLEMENTATION

All integration contracts between **Module 18**, **Module 19**, and **Module 06** are fully discoverable, verified against repository architecture, and certified free of shadow authorities or security regressions.
