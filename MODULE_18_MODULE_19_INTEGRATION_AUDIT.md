# MODULE 18 ↔ MODULE 19 INTEGRATION AUDIT

**Project**: Sucharu Pro — Master ERP & Unified Graphics Platform  
**Integration Boundary**: Module 18 (Advanced Dynamic Imposition) ↔ Module 19 (Substrate Stock Auto-Reservation)  
**Audit Target**: Contract Compatibility, Authority Separation, Concurrency Safety, Anti-Duplication  
**Audit Date**: September 2, 2026  
**Integration Status**: **🟢 CLEAN & VERIFIED COMPATIBLE**  

---

## 1. Executive Summary

This integration audit verifies the contract boundaries and data flow between **Module 18 (Advanced Dynamic Imposition & Gang-Run Optimizer Engine)** and **Module 19 (Substrate Stock Auto-Reservation Engine)**.

The audit confirms that:
1. **Module 18 is the sole Prepress & Imposition authority**: It calculates geometric layout, sheet packing, nesting, folding sequencing, plate separation, and total parent sheet demand (including press setup and running waste allowances).
2. **Module 19 is the sole Substrate Reservation authority**: It manages reservation state machines (`REQUESTED`, `RESERVED_SOFT`, `ALLOCATED_HARD`, `ISSUED_TO_FLOOR`, `CANCELLED`), holding timeouts, warehouse/lot allocation sources, and atomic promotion under row-level locking.
3. **Module 06 is the sole Physical Inventory authority**: Module 19 reads on-hand inventory from Module 06 and calculates available stock as `Available = OnHand - TotalActiveReservations`. Zero shadow inventory or duplicate stock ledgers exist.
4. **Module 15 is the sole Financial Ledger authority**: Substrate reservations post zero general ledger journal entries; financial capitalization occurs solely upon physical stock issue (Module 17 Step 09 / Module 15).

---

## 2. Module 18 → Module 19 Data Contract Compatibility Matrix

| Field / Concept | Module 18 Source Model | Module 19 Target Consumer | Contract Compatibility | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Job ID** | `jobId: String?` | `jobId: String?` | Direct String Mapping | **COMPATIBLE** |
| **Order ID** | `orderId: String` | `orderId: String` | Canonical Order Anchor | **COMPATIBLE** |
| **Order Item ID** | `orderItemId: String` | `orderItemId: String` | Canonical Order Item Anchor | **COMPATIBLE** |
| **Tenant ID** | `tenantId: String` | `tenantId: String` | Multi-Tenant RLS Header | **COMPATIBLE** |
| **Substrate Type** | `paperStockType: PaperStockType` | `paperStockType: PaperStockType` | Canonical Enum Reference | **COMPATIBLE** |
| **Substrate Weight (GSM)** | `gsm: BigDecimal` | `gsm: BigDecimal` | Pure `BigDecimal` (scale = 4) | **COMPATIBLE** |
| **Parent Sheet Width** | `parentSheetDimension.width` (mm) | `sheetWidthMm: BigDecimal` | Metric Millimeters | **COMPATIBLE** |
| **Parent Sheet Height** | `parentSheetDimension.height` (mm) | `sheetHeightMm: BigDecimal` | Metric Millimeters | **COMPATIBLE** |
| **Gross Sheet Quantity** | `requiredSheets: Long` (includes setup & waste) | `requiredGrossSheets: Long` | Direct Long Mapping | **COMPATIBLE** |
| **Net Item Produced Qty** | `totalProducedQuantity: Long` | `producedCopies: Long` | Direct Long Mapping | **COMPATIBLE** |
| **Yield / Utilization** | `sheetUtilizationPercentage: BigDecimal` | `yieldPercentage: BigDecimal` | Scale = 4, HALF_UP | **COMPATIBLE** |
| **Wastage Percentage** | `wastePercentage: BigDecimal` | `wastePercentage: BigDecimal` | Scale = 4, HALF_UP | **COMPATIBLE** |
| **Imposition Spec Ref** | `step01ImpositionId`, `step04SignatureId`, etc. | `impositionSpecificationId: String?` | Reference ID Tracking | **COMPATIBLE** |
| **Readiness Score** | `readinessScore: BigDecimal` | `prepressReadinessScore: BigDecimal?` | 0–100 Quality Index | **COMPATIBLE** |
| **Cryptographic Hash** | `masterIntegrityHash: String` (SHA-256) | `integrityHash: String` | SHA-256 Immutable Seal | **COMPATIBLE** |
| **Plan Version** | `version: Int` | `planVersion: Int` | Version Control | **COMPATIBLE** |

---

## 3. Authority Ownership & Separation Matrix

| Business Concept | Canonical Authority Owner | Interaction in Module 18 | Interaction in Module 19 | Shadow Risk Check |
| :--- | :--- | :--- | :--- | :--- |
| **Customer Orders** | **Module 03** (`OrderRepository`) | Reads order metadata | Reads order metadata | **ZERO SHADOW ORDERS** |
| **Manufacturing Jobs** | **Module 17** (`ProductionExecutionRepository`) | References `jobId` | Links hard reservation to Work Order | **ZERO SHADOW JOBS** |
| **Imposition Layouts** | **Module 18** (`ImpositionRepository`, `PrepressOrchestrationRepository`) | Owns calculation & layout | Consumes sheet demand | **ZERO SHADOW IMPOSITION** |
| **CTP Prepress Plates** | **Module 18** (`CtpOutputRepository`) | Owns plate packages & marks | Read-only reference | **ZERO SHADOW CTP** |
| **Substrate Reservations** | **Module 19** (`SubstrateReservationRepository`) | Emits demand contract | Owns reservation lifecycle & soft/hard states | **ZERO SHADOW RESERVATIONS** |
| **Physical Stock Balances** | **Module 06** (`InventoryRepository`) | Not accessed | Queries on-hand balance; decrements on floor issue | **ZERO SHADOW INVENTORY** |
| **Financial Ledgers** | **Module 15** (`BusinessFinancialLedgerRepository`) | Zero entries | Zero entries | **ZERO SHADOW LEDGERS** |
| **Profitability Intelligence** | **Module 16** (`ProfitabilityRepository`) | Emits waste metrics | Emits material cost commitments | **ZERO SHADOW PROFITABILITY** |

---

## 4. Communication & Event Boundary

The inter-module communication between Module 18 and Module 19 uses **validated cryptographic handoff contracts** backed by REST endpoints and direct domain services:
- **Contract Formats**:
  - `Module18Step01ImpositionHandoffContract`
  - `Module18Step02GangRunHandoffContract`
  - `Module18Step03NestingHandoffContract`
  - `Module18Step04SignatureHandoffContract`
  - `Module18Step05CtpHandoffContract`
  - `Module18Step06PrepressOrchestrationHandoffContract`
- **Replay Safety & Idempotency**:
  - Module 19 uses SHA-256 idempotency nonces: `SHA-256(tenantId : orderId : orderItemId : sku)`.
  - Duplicate handoff submissions resolve to existing reservations without double-booking physical reams.

---

## 5. Security & Multi-Tenant Isolation

1. **Row Level Security**:
   - Both Module 18 tables (`prepress_orchestration_plans`, etc.) and Module 19 tables (`substrate_reservations`, `substrate_reservation_allocations`, `substrate_reservation_audit_events`) enforce PostgreSQL RLS.
2. **Role Boundaries**:
   - `CUSTOMER` and `VENDOR` roles are explicitly forbidden from initiating substrate reservations or modifying prepress orchestration (`403 Forbidden`).
   - `STAFF`, `MANAGER`, and `ADMIN` are authorized to manage reservations and approve plans.
   - `AI_AGENT` receives read-only intelligence contracts and cannot mutate database state directly.

---

## 6. Conclusion & Verdict

**Module 18 ↔ Module 19 boundary is verified, clean, non-duplicative, and fully compatible.**  
**Status: 🟢 PASS**
