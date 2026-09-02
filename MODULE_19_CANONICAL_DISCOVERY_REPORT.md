# MODULE 19 CANONICAL DISCOVERY & DEVELOPMENT READINESS AUDIT

## Canonical Definition, Cross-Module Dependency, Domain Authority & Readiness Gate

**VERSION:** Module 19 Discovery Gate v1.0  
**MODE:** READ-ONLY — NO CODE CHANGES  
**AUDIT DATE:** September 2, 2026  

---

### 1. Canonical Module 19 Definition

```text
Module Number:           Module 19
Canonical Module Title:  Substrate Stock Auto-Reservation
Canonical Source File:   DEMO_MODULE_ACCESS_MATRIX.md
Source Location:         e:\App\Sucharu Pro\DEMO_MODULE_ACCESS_MATRIX.md (Line 27)
Version / Revision:      Master ERP Roadmap Baseline v1.0
Status:                  Planned / Development Ready
```

#### Step Structure Analysis:
In the canonical roadmap (`DEMO_MODULE_ACCESS_MATRIX.md`), **Module 19** is established as the dedicated commercial printing **Substrate Stock Auto-Reservation Engine**. It bridges upstream imposition & job planning (Modules 17 & 18) with authoritative substrate inventory (Module 06).

```text
Module 19 — Substrate Stock Auto-Reservation
├── Step 01 — Substrate Requirement Resolution & Inventory Interlock
├── Step 02 — Real-Time Soft/Hard Stock Reservation & Allocation Engine
├── Step 03 — Batch/Lot Selection, Grain Direction & Sheet Dimension Matching
├── Step 04 — Auto-Replenishment Triggers & Supplier Reorder Alerts
├── Step 05 — Job Cancellation, Revision & Substrate Release Governance
└── Step 06 — Enterprise Reservation Audit, RLS & Cross-Module AI Handoff
```

---

### 2. Module 18 ↔ Module 19 Cross-Module Interface

| Module 18 / 17 Authority | Module 19 Consumer | Interface / Payload | Risk | Verification Status |
| :--- | :--- | :--- | :--- | :--- |
| **Imposition Sheet Specs** (Mod 18 / Mod 17 Step 01) | Substrate Requirement Resolver | Required parent sheet size (e.g. 25"x36"), paper GSM, brand, finish, total gross sheet quantity (including setup/wastage allowance). | Mismatched dimensions causing zero allocation. | **VERIFIED CLEAN** |
| **Commercial Commitment & Order** (Mod 03 / Mod 17 Step 03) | Auto-Reservation Trigger | Order ID, Order Item ID, customer priority tier, target delivery deadline. | Orphan reservations without valid order anchor. | **VERIFIED CLEAN** |
| **Production Planning Snapshot** (Mod 17 Step 04) | Hard Reservation Gate | Planned production job ID, machine assignment, scheduled run time. | Parallel allocation of the same physical reams. | **VERIFIED CLEAN** |
| **Canonical Inventory Authority** (Mod 06 / 07) | Substrate Stock Ledger | SKU ID, warehouse location ID, on-hand balance, allocated balance, available balance. | **SHADOW INVENTORY RISK** (Must strictly modify canonical inventory via repository). | **VERIFIED SAFE (Module 06 Sole Authority)** |

---

### 3. Existing Implementation Check

| Component Category | Exists in Repo? | Location | Completeness | Canonical Status |
| :--- | :--- | :--- | :--- | :--- |
| **Module 19 Domain Models** | No | `core/.../domain/model/reservation/` | Greenfield (0%) | Planned |
| **Module 19 Services & Repos** | No | `core/.../domain/service/reservation/` | Greenfield (0%) | Planned |
| **Module 19 PostgreSQL Tables** | No | `database/migrations/` | Greenfield (0%) | Planned |
| **Module 19 REST Endpoints** | No | `core/.../data/api/server/` | Greenfield (0%) | Planned |
| **Module 19 Android UI** | No | `app/.../ui/features/` | Greenfield (0%) | Planned |
| **Module 06 Substrate Inventory** | **YES** | `core/.../domain/model/inventory/` | **COMPLETE (100%)** | **CANONICAL PROTECTED BASELINE** |
| **Module 17 Job Planning** | **YES** | `core/.../domain/model/productionplanning/` | **COMPLETE (100%)** | **CANONICAL PROTECTED BASELINE** |

---

### 4. Dependency Graph

```text
MODULE 19: SUBSTRATE STOCK AUTO-RESERVATION
    │
    ├──► Depends on MODULE 00: System Foundation (Tenant RLS Context, Event Outbox, Session Security)
    ├──► Depends on MODULE 02: Customer Identity (Customer Priority Tiering for Allocation)
    ├──► Depends on MODULE 03: Order Lifecycle (Canonical Order & Order Item IDs)
    ├──► Depends on MODULE 06: Inventory & Paper Substrates (Canonical Stock Balance & Warehouse SKUs)
    ├──► Depends on MODULE 17: Smart Printing Calculator & Planning (Gross Sheet Quantities & GSM Specs)
    ├──► Interlocks with MODULE 18: Advanced Imposition (Gang-Run Shared Substrate Optimization)
    └──► Emits Handoffs to MODULE 15: Finance & MODULE 16: Profitability (Material Commitment Valuations)
```

---

### 5. Authority & Anti-Duplication Check

| Business Concept | Canonical Authority | Module 19 Role | New Authority Needed? |
| :--- | :--- | :--- | :--- |
| **Physical Stock Balances** | **Module 06** (`InventoryRepository`) | Decrement available stock / Increment allocated stock | **NO (Zero Shadow Inventory)** |
| **Canonical Orders** | **Module 03** (`OrderRepository`) | Read order metadata & attach reservation ID | **NO (Module 03 owns Orders)** |
| **Production Jobs** | **Module 17** (`ProductionExecutionRepository`) | Link substrate release to Work Order Start | **NO (Module 17 owns Jobs)** |
| **Financial Ledger** | **Module 15** (`BusinessFinancialLedgerRepository`) | Read material standard cost / emit commit event | **NO (Zero Shadow Ledger)** |
| **Tenant Isolation & Security** | **Module 00** (`PostgresTransactionManager`) | Enforce `app.current_tenant` & RBAC capabilities | **NO (Module 00 Base Authority)** |

---

### 6. Boundary Specifications for Future Implementation

#### 6.1 Financial Boundary
- Estimated substrate cost is derived directly from Module 17 Step 02 / Module 06 SKU purchase cost.
- Substrate reservations do **not** post journal entries; capitalization occurs upon actual consumption (Module 17 Step 09 / Module 15).
- **Rule**: NO SHADOW LEDGER.

#### 6.2 Inventory Boundary
- Module 19 introduces `substrate_reservations` records linking `order_id` to `inventory_item_id`.
- The physical on-hand stock remains in Module 06.
- Available stock is calculated as: `Available = OnHand - TotalActiveReservations`.
- **Rule**: NO SHADOW INVENTORY.

#### 6.3 Security Boundary
- Multi-tenant RLS mandatory on all reservation tables (`FORCE ROW LEVEL SECURITY`).
- Permitted Roles: `STAFF`, `MANAGER`, `ADMIN`, `AI_AGENT` (read-only).
- Forbidden Roles: `GUEST`, `CUSTOMER`, `VENDOR` (`403 Forbidden`).

---

### 7. Gap Analysis

| Subsystem Area | Required for Module 19 | Existing in Repo | Gap Identified | Severity |
| :--- | :--- | :--- | :--- | :--- |
| **Substrate Master Data** | Paper GSM, sizes, brands, ream weights | Module 06 Inventory Data | None | **INFO** |
| **Imposition Sheet Demand** | Gross sheets with wastage | Module 17 Planning Snapshot | None | **INFO** |
| **Reservation Entity & State** | `SubstrateReservation`, `ReservationStatus` | Not implemented | Greenfield component required | **INFO** |
| **Concurrency Locking** | Safe reservation during concurrent orders | `PostgresTransactionManager` ready | Multi-row transactional lock needed in service | **LOW** |

---

### 8. Development Readiness Gate Decision

## 🟢 READY FOR MODULE 19 DEVELOPMENT

### Recommended Next Action:
Initiate Module 19 architectural design and implementation planning, maintaining strict compliance with the zero-shadow-inventory and PostgreSQL RLS invariants established across Modules 00–18.
