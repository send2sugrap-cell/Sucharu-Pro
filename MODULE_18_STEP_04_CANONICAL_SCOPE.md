# MODULE 18 STEP 04 — CANONICAL SCOPE REPORT
## Signature Layouts, Page Imposition & Work-and-Turn / Tumble

---

### Module:
**Module 18 — Advanced Dynamic Imposition & Gang-Run Optimizer Engine**

### Step:
**Step 04 — Signature Layouts, Page Imposition & Work-and-Turn / Tumble**

### Canonical Source:
- `DEMO_MODULE_ACCESS_MATRIX.md` (Line 26: `Module 18 — Advanced Dynamic Imposition & Gang-Run`)
- `MODULE_18_FORENSIC_AUDIT_REPORT.md` (Lines 49, 97, 123–126)
- `MODULE_18_READINESS_GATE.md` (Lines 16–21: "Automated Sheet Layout, Dynamic Gang-Run Combining, Print Form Optimization")
- `FINAL_MODULE_00_18_FINAL_GATE.md` (Lines 11–17: "Signature Layouts, Page Imposition")

---

### Canonical Scope:
Implement a deterministic, high-precision **Signature Layout & Multi-Page Imposition Engine** for publications, catalogs, booklets, and multi-page commercial print products. The engine computes mathematical folding sequence schemes (4pp, 8pp, 12pp, 16pp, 24pp, 32pp signatures), manages front and back plate page pairing (outer and inner forms), supports press feeding and turning methods (`SHEETWISE`, `WORK_AND_TURN`, `WORK_AND_TUMBLE`, `PERFECTING`), calculates progressive creep / shingling compensation based on substrate caliper/GSM and binding style (`SADDLE_STITCH`, `PERFECT_BOUND`, `SECTION_SEWN`, `SPIRAL_WIRE_O`), allocates spine, head, and foot gutters, determines common parent sheet run counts, seals specifications with cryptographic SHA-256 integrity hashes, persists aggregates in PostgreSQL under strict RLS, exposes secure REST endpoints, provides an interactive Android Command Center with front/back signature sheet visualizers and fold sequencing, and emits a clean downstream handoff contract for Module 19 substrate reservation.

---

### Inputs:
1. **Publication Product Specifications**:
   - `jobId`, `orderId`, `orderItemId`, `productName`
   - `totalPages`: positive integer (must be divisible by signature page count, with automatic blank page padding if needed)
   - `pageDimension`: finished trimmed page width & height in mm (`PrintingDimension`)
   - `requiredQuantity`: positive integer
   - `paperStockType`, `gsm`, `caliperMm` (optional or calculated from GSM/stock formula: $\text{caliperMm} \approx \frac{\text{gsm}}{1000} \times \text{bulkFactor}$)
2. **Signature & Folding Parameters**:
   - `signaturePageCount`: 4, 8, 12, 16, 24, 32 pages per sheet
   - `bindingMethod`: `SADDLE_STITCH`, `PERFECT_BOUND`, `SECTION_SEWN`, `SPIRAL_WIRE_O`, `FOLDED_LEAFLET`
   - `sheetTurningMethod`: `SHEETWISE`, `WORK_AND_TURN`, `WORK_AND_TUMBLE`, `PERFECTING`
   - `foldingScheme`: `HALF_FOLD`, `LETTER_FOLD`, `ROLL_FOLD`, `Z_FOLD`, `FRENCH_RIGHT_ANGLE_4PP`, `RIGHT_ANGLE_8PP`, `RIGHT_ANGLE_16PP`, `DOUBLE_RIGHT_ANGLE_32PP`
3. **Parent Sheet Dimension (`PrintingDimension`)**:
   - `widthMm`, `heightMm` (e.g. 635.0000mm x 914.4000mm / 25" x 36")
4. **Margins & Spacing Specs**:
   - `ImpositionMarginSpec`: `topMm` (gripper), `bottomMm` (tail), `leftMm`, `rightMm`
   - `SignatureGutterSpec`: `spineGutterMm` (grind-off/milling allowance), `headGutterMm` (head-to-head), `footGutterMm`, `faceTrimMm`, `bleedMm`
   - `enableCreepCompensation`: boolean (applies shingling displacement to inner signature folios)

---

### Outputs:
1. **Authoritative Aggregate (`SignatureImpositionSpecification`)**:
   - `signatureImpositionId`, `tenantId`, `name`, `status` (`DRAFT`, `OPTIMIZED`, `APPLIED_TO_PLANNING`, `SUPERSEDED`, `CANCELLED`)
   - `totalPages`, `signaturePageCount`, `totalSignaturesCount`, `bindingMethod`, `sheetTurningMethod`, `foldingScheme`
   - `parentSheetDimension`, `pageDimension`, `marginSpec`, `gutterSpec`
   - `signatures`: List of `SignatureForm` each containing:
     - `signatureIndex` (1-based signature number)
     - `formSide` (`FRONT_SIDE_OUTER` / `BACK_SIDE_INNER` / `WORK_AND_TURN_COMBINED`)
     - `pagePlacements`: List of `SignaturePagePlacement` with exact coordinates $(X, Y)$, orientation (0°, 90°, 180°, 270° / Head-to-Head), page number, and creep offset $(dx, dy)$
     - `columns`, `rows`, `pagesPerSide`
   - `creepSummary`: `totalCreepMm`, `creepPerSheetMm`, `innerMarginShiftMm`
   - `commonRequiredSheets`, `totalProducedCopies`, `overageCopies`
   - `totalSheetAreaMm2`, `usableAreaMm2`, `occupiedAreaMm2`, `sheetUtilizationPercentage`, `usableYieldPercentage`
   - Cryptographic `integrityHash` (SHA-256) and `version`
2. **Downstream Handoff Contract (`Module18Step04SignatureHandoffContract`)**:
   - Sealed payload for Module 19 substrate reservation.

---

### Dependencies:
- **Module 18 Step 01, 02 & 03**: Reuses `PrintingDimension`, `MeasurementUnit`, `ImpositionMarginSpec`, `ImpositionSpacingSpec`, `ImpositionLayoutOrientation`, `ImpositionMathUtils`.
- **Module 17**: Reuses `PaperStockType`, `ColorMode`, `PrintingSideOption`.
- **Core Security**: `AuthenticatedPrincipal`, `UserRole`, `BackendSecurityContext`.
- **Database**: PostgreSQL 15+ with Flyway (`V20261117__create_signature_imposition_tables.sql`), `FORCE ROW LEVEL SECURITY`.

---

### Required Integrations:
- **Module 17 (Production/Calculator)**: Consumes page dimensions, page counts, paper stock, and quantities cleanly.
- **Module 18 Step 01 / 02 / 03**: Reuses mathematical primitives, margin/spacing specifications, and hashing logic.
- **Module 19 (Substrate Stock Auto-Reservation)**: Emits `Module18Step04SignatureHandoffContract` (Module 19 remains on HOLD).

---

### Required Persistence:
- Flyway Migration: `V20261117__create_signature_imposition_tables.sql`
- Tables:
  - `signature_imposition_specifications`
  - `signature_forms`
  - `signature_page_allocations`
  - `signature_imposition_audit_events`
- `FORCE ROW LEVEL SECURITY` with tenant isolation policy (`tenant_id = CURRENT_SETTING('app.current_tenant', true)`).

---

### Required API:
- `POST /api/v1/imposition/signature/optimize`
- `GET /api/v1/imposition/signature/specifications`
- `GET /api/v1/imposition/signature/specifications/{signatureImpositionId}`
- `POST /api/v1/imposition/signature/specifications/{signatureImpositionId}/status`
- `GET /api/v1/imposition/signature/specifications/{signatureImpositionId}/handoff`

---

### Required Android/UI:
- `SignatureImpositionCommandCenterScreen.kt` in Jetpack Compose:
  - Interactive Front / Back Signature Sheet Page Grid Visualizer with page numbers, head-to-head orientation arrows, spine fold lines, and trim marks.
  - Signature & Folding Configurator (Page Count, Binding Type, Work-and-Turn / Tumble mode, Creep Compensation toggle).
  - Creep & Shingling Diagnostics card (caliper, total creep, progressive folio offsets).
  - Historical Signature Imposition Specifications & Lifecycle Status Controls.
- Wired in `AppDestination.kt` and `InternalWorkspaceShells.kt`.

---

### Required AI/Event/Automation:
- Read-only AI handoff contract structure (`Module18Step04SignatureHandoffContract`).
- Auditable state transition log.

---

### Explicitly Out of Scope:
- **Module 18 Step 05**: Pre-Press Marks, Gripper/Gutter/Bleed Allocation & Plate Prep (CTP plate export).
- **Module 18 Step 06**: Imposition Audit Trail, Production Job Interlock & Full AI Agent Orchestration.
- **Module 19**: Physical inventory stock reservation (Module 19 remains on HOLD).
- **Module 06**: Physical warehouse inventory balance mutations.
- **Module 15**: General Ledger financial postings.

---

### Future Steps:
- **Step 05**: Pre-Press Marks, Gripper/Gutter/Bleed Allocation & Plate Prep
- **Step 06**: Imposition Audit Trail, Production Job Interlock & AI Handoff
