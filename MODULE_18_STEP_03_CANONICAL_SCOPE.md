# MODULE 18 STEP 03 — CANONICAL SCOPE REPORT
## Dynamic Nesting, Sheet Utilization & Wastage Minimization

---

### Module:
**Module 18 — Advanced Dynamic Imposition & Gang-Run Optimizer Engine**

### Step:
**Step 03 — Dynamic Nesting, Sheet Utilization & Wastage Minimization**

### Canonical Source:
- `DEMO_MODULE_ACCESS_MATRIX.md` (Line 26: `Module 18 — Advanced Dynamic Imposition & Gang-Run`)
- `MODULE_18_FORENSIC_AUDIT_REPORT.md` (Lines 48, 96, 118–121)
- `MODULE_18_READINESS_GATE.md` (Lines 16–21: "Substrate Utilization Maximization")
- `FINAL_MODULE_00_18_FINAL_GATE.md` (Lines 11–17)

---

### Canonical Scope:
Implement a high-performance, deterministic **Dynamic Nesting Engine** for dissimilar and mixed rectangular items on parent press sheets. The engine maximizes substrate sheet utilization percentage, performs 2D bin-packing and placement optimization with candidate orientation exploration (0° / 90°), calculates usable and unallocated areas, identifies recoverable offcut remnants, enforces prepress margins/bleed/gutters, computes required parent sheets, seals specifications with cryptographic SHA-256 integrity hashes, persists aggregates in PostgreSQL under strict RLS, exposes secure REST endpoints, provides an interactive Android Command Center with 2D layout canvas rendering, and emits a clean downstream handoff contract for Module 19 substrate reservation.

---

### Inputs:
1. **List of Candidate Items (`NestingCandidateItem`)**:
   - `jobId`, `orderId`, `orderItemId`, `productName`
   - `finishedDimension`: width & height in mm
   - `requiredQuantity`: positive integer
   - `allowRotation`: boolean (permits 0° and 90° rotation)
   - `paperStockType`, `gsm`
2. **Parent Sheet Dimension (`PrintingDimension`)**:
   - `widthMm`, `heightMm` (e.g. 635.0000mm x 914.4000mm / 25" x 36")
3. **Margins & Spacing Specs**:
   - `ImpositionMarginSpec`: `topMm` (gripper), `bottomMm` (tail), `leftMm`, `rightMm`
   - `ImpositionSpacingSpec`: `bleedMm`, `horizontalGutterMm`, `verticalGutterMm`
4. **Nesting Strategy & Policies**:
   - `NestingOrientationPolicy`: `ALLOW_ROTATION`, `FORCE_STANDARD`, `FORCE_ROTATED`, `PROHIBIT_ROTATION`
   - `NestingPlacementStrategy`: `BOTTOM_LEFT_FILL`, `BEST_AREA_FIT`, `GUILLOTINE_CUT_FIRST`
   - `minOffcutDimensionMm`: minimum width & height for offcut remnant classification (default: 100.0000mm)

---

### Outputs:
1. **Authoritative Aggregate (`DynamicNestingSpecification`)**:
   - `nestingId`, `tenantId`, `name`, `status` (`DRAFT`, `OPTIMIZED`, `APPLIED_TO_PLANNING`, `SUPERSEDED`, `CANCELLED`)
   - `parentSheetDimension`, `marginSpec`, `spacingSpec`
   - `placements`: List of `NestingItemPlacement` with exact `(xMm, yMm, placedWidthMm, placedHeightMm, orientation, assignedCopies)`
   - `offcutRemnants`: List of `NestingOffcutRemnant` with `(xMm, yMm, widthMm, heightMm, areaMm2, isRecoverable)`
   - Area metrics: `totalSheetAreaMm2`, `usableAreaMm2`, `occupiedAreaMm2`, `wasteAreaMm2`, `recoverableOffcutAreaMm2`
   - Efficiency KPIs: `utilizationPercentage`, `usableYieldPercentage`, `offcutRecoveryPercentage`
   - Production metrics: `requiredSheets`, `totalProducedItems`, `totalOverageItems`
   - Cryptographic `integrityHash` (SHA-256) and `version`
2. **Downstream Handoff Contract (`Module18Step03NestingHandoffContract`)**:
   - Sealed payload for Module 19 substrate reservation.

---

### Dependencies:
- **Module 18 Step 01 & Step 02**: Reuses `PrintingDimension`, `MeasurementUnit`, `ImpositionMarginSpec`, `ImpositionSpacingSpec`, `ImpositionLayoutOrientation`, `ImpositionMathUtils`.
- **Module 17**: Reuses `PaperStockType`, `ColorMode`, `PrintingSideOption`.
- **Core Security**: `AuthenticatedPrincipal`, `UserRole`, `BackendSecurityContext`.
- **Database**: PostgreSQL 15+ with Flyway (`V20261116__create_dynamic_nesting_tables.sql`), `FORCE ROW LEVEL SECURITY`.

---

### Required Integrations:
- **Module 17 (Production/Calculator)**: Consumes item dimensions and quantities cleanly.
- **Module 18 Step 01 / Step 02**: Reuses mathematical primitives and margin/spacing specifications.
- **Module 19 (Substrate Stock Auto-Reservation)**: Emits `Module18Step03NestingHandoffContract` (Module 19 remains on HOLD).

---

### Required Persistence:
- Flyway Migration: `V20261116__create_dynamic_nesting_tables.sql`
- Tables:
  - `dynamic_nesting_specifications`
  - `dynamic_nesting_placements`
  - `dynamic_nesting_offcuts`
  - `dynamic_nesting_audit_events`
- `FORCE ROW LEVEL SECURITY` with tenant isolation policy (`tenant_id = CURRENT_SETTING('app.current_tenant', true)`).

---

### Required API:
- `POST /api/v1/imposition/nesting/optimize`
- `GET /api/v1/imposition/nesting/specifications`
- `GET /api/v1/imposition/nesting/specifications/{nestingId}`
- `POST /api/v1/imposition/nesting/specifications/{nestingId}/status`
- `GET /api/v1/imposition/nesting/specifications/{nestingId}/handoff`

---

### Required Android/UI:
- `DynamicNestingCommandCenterScreen.kt` in Jetpack Compose:
  - 2D Canvas rendering nested cut rectangles (with item names & dimensions) and offcut remnant regions.
  - Candidate Job Pool manager (add/remove items, toggle rotation, configure dimensions).
  - KPI Dashboard (Utilization %, Usable Yield %, Offcut Area, Required Sheets).
  - Historical Specification Drawer and Status Transition Controls.
- Wired in `AppDestination.kt` and `InternalWorkspaceShells.kt`.

---

### Required AI/Event/Automation:
- Read-only AI handoff contract structure (`Module18Step03NestingHandoffContract`).
- Auditable state transition log.

---

### Explicitly Out of Scope:
- **Module 18 Step 04**: Signature Layouts, Page Imposition & Work-and-Turn / Tumble.
- **Module 18 Step 05**: Pre-Press Marks, Gripper/Gutter/Bleed Allocation & Plate Prep.
- **Module 18 Step 06**: Imposition Audit Trail, Production Job Interlock & Full AI Agent Orchestration.
- **Module 19**: Physical inventory stock reservation (Module 19 remains on HOLD).
- **Module 06**: Physical warehouse inventory balance mutations.
- **Module 15**: General Ledger financial postings.

---

### Future Steps:
- **Step 04**: Signature Layouts, Page Imposition & Work-and-Turn / Tumble
- **Step 05**: Pre-Press Marks, Gripper/Gutter/Bleed Allocation & Plate Prep
- **Step 06**: Imposition Audit Trail, Production Job Interlock & AI Handoff
