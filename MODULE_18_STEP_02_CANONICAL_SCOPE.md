# MODULE 18 STEP 02 — CANONICAL SCOPE REPORT
## Multi-Job Gang-Run Batching & Compatibility Clustering

---

### Module
**Module 18 — Advanced Dynamic Imposition & Gang-Run Optimizer Engine**

### Canonical Step
**Step 02 — Multi-Job Gang-Run Batching & Compatibility Clustering**

### Canonical Source
* `DEMO_MODULE_ACCESS_MATRIX.md` (Line 26: Module 18 Advanced Dynamic Imposition & Gang-Run)
* `MODULE_18_READINESS_GATE.md` (Lines 11–17: Canonical Scope & Dynamic Gang-Run Combining)
* `MODULE_18_FORENSIC_AUDIT_REPORT.md` (Lines 47, 95, 113–116)
* `FINAL_MODULE_00_18_FINAL_GATE.md` (Lines 11–17)

---

### Canonical Scope Summary
Step 02 is authorized to implement **Multi-Job Gang-Run Batching & Compatibility Clustering**.
It evaluates candidate print jobs/items seeking to share a common parent sheet and groups them into deterministic, compatible gang-run clusters.
The engine:
1. Validates strict compatibility across candidate jobs (same substrate/paper stock type, matching GSM within tolerance, matching color processes / ink coverage profiles, matching grain direction constraints, compatible due dates/priorities, and feasible parent sheet dimensions).
2. Performs cluster formation and deterministic batch partitioning.
3. Formulates multi-job shared sheet layouts allocating fractional UP-slots (copies per sheet per job), calculating common required press sheet runs, producing item-level yield and overage.
4. Ranks candidate gang-run combinations deterministically (highest total sheet yield %, lowest aggregate waste area mm², minimum parent sheet runs, and stable tiebreaking on alphanumeric Cluster/Job IDs).
5. Computes invariant cryptographic SHA-256 integrity hashes for each gang-run batch specification.
6. Persists gang-run batches and clustered item allocations in PostgreSQL with `FORCE ROW LEVEL SECURITY`.
7. Exposes tenant-safe, RBAC-guarded REST endpoints (`/api/v1/imposition/gang-run/...`).
8. Provides an Android Jetpack Compose Command Center screen with interactive batch visualization, candidate clustering, and state management.
9. Emits a clean downstream handoff contract (`Module18Step02GangRunHandoffContract`) for Module 19 substrate reservation.

---

### Inputs
1. **List of Candidate Jobs / Items (`GangRunCandidateItem`)**:
   - `jobId`, `orderId`, `orderItemId`
   - `productName`
   - `finishedItemDimension` (width, height in mm)
   - `requiredQuantity`
   - `paperStockType` (e.g., `ART_CARD`, `ART_PAPER`)
   - `gsm` (e.g., `300.0000`)
   - `colorMode` (e.g., `CMYK_FOUR_COLOR`)
   - `printingSideOption` (e.g., `SINGLE_SIDED`, `DOUBLE_SIDED_SAME`)
   - `targetDueDateEpochMs` (optional for scheduling clustering)
2. **Parent Sheet Dimension (`PrintingDimension`)**: width, height in mm (e.g. 635mm x 914.4mm).
3. **Margins & Spacing Specs**:
   - `ImpositionMarginSpec` (top, bottom, left, right in mm)
   - `ImpositionSpacingSpec` (bleed, horizontal gutter, vertical gutter in mm)
4. **Clustering Strategy Policy (`GangRunClusteringPolicy`)**:
   - `STRICT_IDENTICAL_SUBSTRATE`: Requires exact match on `stockType` and `gsm`.
   - `MAX_ITEMS_PER_SHEET`: Limit of distinct jobs co-located on a single form (e.g., 2 to 8 items).
   - `ALLOW_DIFFERENT_QUANTITIES`: Allows unbalanced run lengths, calculating common sheet run and job overages.

---

### Outputs
1. **Compatibility Clusters (`GangRunCluster`)**:
   - Grouping of compatible jobs meeting strict physical criteria.
2. **Gang-Run Batch Specification Aggregate (`GangRunSpecification`)**:
   - `gangRunId`: Unique identifier (e.g. `GANG-xxxx`).
   - `tenantId`: Strict multi-tenant anchor.
   - `parentSheetDimension`: Usable and total sheet dimensions.
   - `clusterCriteria`: Matched substrate stock type, GSM, and process.
   - `allocatedItems`: List of `GangRunItemAllocation`:
     - `jobId`, `orderItemId`
     - `assignedSlots`: Number of UP slots allocated to this job on the plate.
     - `orientation`: `STANDARD` or `ROTATED`.
     - `producedQuantity`: `commonSheetRun * assignedSlots`.
     - `overageQuantity`: `producedQuantity - requiredQuantity`.
     - `yieldPercentage`: Relative sheet occupancy percentage for this item.
   - `commonRequiredSheets`: $\max_i \lceil \frac{\text{requiredQuantity}_i}{\text{assignedSlots}_i} \rceil$.
   - `totalSheetYieldPercentage`: Overall sheet utilization.
   - `totalSheetWasteAreaMm2`: Unallocated or cutting waste area.
   - `status`: Lifecycle (`DRAFT`, `OPTIMIZED`, `COMMITTED_TO_PRESS`, `CANCELLED`).
   - `integrityHash`: SHA-256 deterministic cryptographic hash.
3. **Downstream Handoff Contract (`Module18Step02GangRunHandoffContract`)**:
   - Total substrate requirement payload consumable by Module 19.

---

### Dependencies
* `Module 18 Step 01`: Reuses `PrintingDimension`, `MeasurementUnit`, `ImpositionMarginSpec`, `ImpositionSpacingSpec`, `ImpositionLayoutOrientation`, `ImpositionMathUtils`.
* `Module 17`: Reuses `PaperStockType`, `ColorMode`, `PrintingSideOption`.
* `PostgreSQL & Flyway`: Forward-only migration (`V20261115__create_gang_run_batch_tables.sql`) with RLS.
* `Core Security`: `AuthenticatedPrincipal`, `UserRole`, `BackendSecurityContext`.

---

### Explicitly Out of Scope
* Step 03: Dynamic Nesting & Non-Orthogonal Guillotine Packing. (Step 02 performs orthogonal slot partitioning across clustered compatible jobs).
* Step 04: Multi-Page Book Signatures & Work-and-Turn / Tumble Imposition.
* Step 05: Pre-Press Marks, Color Bars, and CTP Plate Output.
* Step 06: Production Job Interlock & AI Agent Orchestration.
* Module 19: Physical stock allocation or substrate reservation (Module 19 remains on HOLD).
* Module 06: Physical warehouse inventory balance mutations.
* Module 15: General Ledger financial postings.

---

### Future Steps
* **Step 03**: Dynamic Nesting, Sheet Utilization & Wastage Minimization
* **Step 04**: Signature Layouts, Page Imposition & Work-and-Turn / Tumble
* **Step 05**: Pre-Press Marks, Gripper/Gutter/Bleed Allocation & Plate Prep
* **Step 06**: Imposition Audit Trail, Production Job Interlock & AI Handoff
