# SUCHARU PRO — ARCHITECTURE BOUNDARY AUDIT
## PHASE 07 → MODULE 19: RESERVATION / ALLOCATION ENGINE

---

### 1. Executive Statement of Architectural Truth
Module 19 (**Substrate Stock Auto-Reservation Engine**) is the canonical reservation and allocation orchestration layer for commercial printing jobs in Sucharu Pro ERP. It operates strictly as an availability reservation engine on top of Module 06/07 Inventory Master and introduces **zero shadow inventory** or duplicate stock ledgers.

---

### 2. Module Ownership Matrix

| ERP Capability | Canonical Owner | Module 19 Role | Boundary Invariant |
| :--- | :--- | :--- | :--- |
| **Physical Stock Balances** | **Module 06 / 07** (`InventoryRepository`) | Reads on-hand physical stock; holds soft/hard reservations; decrements on floor issue | **Zero Shadow Inventory** |
| **Product Master Catalog** | **Module 06 / 07** (`inventory_products`) | Matches paper substrate requirements against `category = 'PAPER'` inventory products | **Single Product Master Authority** |
| **Customer Orders** | **Module 03** (`OrderRepository`) | Reads order metadata & attaches reservation ID | **Module 03 owns Orders** |
| **Production Execution Jobs** | **Module 04 / 17** (`ProductionExecutionRepository`) | Binds hard allocations to scheduled production jobs | **Module 04/17 owns Jobs** |
| **Imposition Layouts & Sheet Demand** | **Module 18** (`ImpositionRepository`) | Consumes parent sheet size, GSM, and gross sheet requirements | **Module 18 owns Imposition** |
| **Delivery Challans & Dispatch** | **Module 08** (`DeliveryRepository`) | Fulfills allocated goods via delivery challans and shipment PODs | **Module 08 owns Delivery** |
| **Financial General Ledger** | **Module 15** (`BusinessFinancialLedgerRepository`) | Emits material commitment valuations; posts zero general ledger entries until physical issue | **Zero Shadow Ledger** |

---

### 3. "Substrate" Definition & Classification
In commercial printing ERP terminology and Sucharu Pro's Master Architecture:
- **Substrate** refers to the parent paper stock material (e.g. Art Card 300 GSM, Offset Paper 80 GSM, Duplex Board, Kraft Paper) specified by sheet dimensions, GSM, and grain direction for printing jobs.
- Paper substrates are registered in Module 06/07 Inventory Master under `category = 'PAPER'`.
- Finished goods created upon job completion (Phase 06 Step 01) are registered under `category = 'FINISHED_GOODS'` with `productType = FINISHED_PRODUCT`.
- **Verdict**: There is zero architectural contradiction. Module 07 tracks physical stock across categories (`PAPER`, `FINISHED_GOODS`, etc.), while Module 19 manages soft/hard availability holds for paper substrates.

---

### 4. Non-Overlapping Boundary Rules
1. **NO SHADOW INVENTORY**: Available stock is calculated dynamically as $\text{Available} = \text{OnHand} - \text{TotalActiveHolds}$.
2. **NO SHADOW LEDGER**: Capitalization occurs upon physical stock dispatch to the shop floor (Module 17 Step 09 / Module 15).
3. **NO SHADOW ORDERS**: Reservations must anchor to valid Order and OrderItem IDs.
