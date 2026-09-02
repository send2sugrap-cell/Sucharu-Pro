# PRODUCTION CERTIFICATION & FORENSIC AUDIT REPORT
## Module 18 — Advanced Dynamic Imposition & Gang-Run Optimizer Engine
### Step 03 — Dynamic Nesting, Sheet Utilization & Wastage Minimization

---

## 1. EXECUTIVE SUMMARY

- **Module**: `Module 18 — Advanced Dynamic Imposition & Gang-Run Optimizer Engine`
- **Step**: `Step 03 — Dynamic Nesting, Sheet Utilization & Wastage Minimization`
- **Status**: `PRODUCTION VERIFIED & CERTIFIED`
- **Certification Date**: 2026-09-02
- **Audit Outcome**: 100% Pass Rate across 2D Bin-Packing Math Engine, Free Rectangle Splitting, Remnant Classification, Persistence with PostgreSQL RLS, REST API, Android Jetpack Compose 2D Canvas UI, and Full Enterprise Regression.

---

## 2. VERIFIED CANONICAL BASELINE & SCOPE ENFORCEMENT

1. **Phase A Canonical Verification**:
   - Primary reference document: [MODULE_18_STEP_03_CANONICAL_SCOPE.md](file:///e:/App/Sucharu%20Pro/MODULE_18_STEP_03_CANONICAL_SCOPE.md)
   - Scope: Dynamic 2D bin-packing & rectangular nesting of heterogeneous/mixed finished items on parent press sheets, 0°/90° orientation candidate exploration, prepress margins & bleed/gutter spacing, offcut remnant recovery classification ($\ge 100\text{mm} \times 100\text{mm}$), exact `BigDecimal(scale = 4)` sheet utilization % & usable yield %, common required sheet run calculation, SHA-256 cryptographic seal, and Android Command Center with 2D sheet canvas rendering.
2. **Boundary Protections**:
   - **Modules 00 through 17**: 100% frozen, unmodified, and regression-free.
   - **Module 18 Step 01 & Step 02**: COMPLETE / HOLD. Extended cleanly via orthogonal shared domain models without mutating single-job imposition or gang-run clustering rules.
   - **Module 19**: Strictly on HOLD (Step 01 & Step 02 untouched, Step 03+ prohibited). Emits standard `Module18Step03NestingHandoffContract` for downstream substrate reservations.
   - **No Shadow Inventory**: 0 physical stock balance mutations.
   - **No Shadow General Ledger**: 0 journal entries posted.

---

## 3. ARCHITECTURAL ARTIFACTS IMPLEMENTED

### A. Domain & Mathematical Engine
- [NestingModels.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/model/imposition/NestingModels.kt):
  - `NestingCandidateItem`: Heterogeneous candidate jobs with finished dimensions, quantity, substrate specs, and orientation preferences.
  - `NestingOrientationPolicy`: `ALLOW_ROTATION`, `FORCE_STANDARD_0_DEG`, `FORCE_ROTATED_90_DEG`, `PROHIBIT_ROTATION`.
  - `NestingPlacementStrategy`: `BOTTOM_LEFT_FILL`, `BEST_AREA_FIT`, `GUILLOTINE_CUT_FIRST`.
  - `NestingItemPlacement`: Position coordinates $(X, Y)$, dimensions, orientation, and occupied area.
  - `NestingOffcutRemnant`: Unoccupied sheet remainder coordinates, area, and recoverability indicator.
  - `NestingJobAllocationSummary`: Produced vs required counts, overage, and occupied area per job.
  - `DynamicNestingSpecification`: Authoritative Aggregate Root with parent sheet dimensions, total items placed, common sheets run, utilization %, yield %, SHA-256 integrity hash, and lifecycle status.
  - `Module18Step03NestingHandoffContract`: Downstream handoff contract for Module 19 substrate reservation.
- [DynamicNestingEngine.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/service/imposition/DynamicNestingEngine.kt):
  - Deterministic 2D rectangular bin packing with orthogonal free-rectangle splitting.
  - Prepress bleed expansion ($2 \times \text{bleed}$) and margin exclusion.
  - Remnant classification: $\text{isRecoverable} \iff \text{width} \ge 100\text{mm} \land \text{height} \ge 100\text{mm}$.
  - Pure arithmetic utilization: $\text{utilization} = \frac{\sum \text{Item Area}}{\text{Total Sheet Area}} \times 100$, $\text{usableYield} = \frac{\sum \text{Item Area}}{\text{Usable Sheet Area}} \times 100$.
  - Common sheet run computation: $\max_i \lceil \frac{\text{quantity}_i}{\text{copies}_i} \rceil$.
  - SHA-256 tamper-evident integrity hash computation.

### B. Database Migration & Persistence
- [V20261116__create_dynamic_nesting_tables.sql](file:///e:/App/Sucharu%20Pro/database/migrations/V20261116__create_dynamic_nesting_tables.sql) & [core/src/main/resources/db/migration/...](file:///e:/App/Sucharu%20Pro/core/src/main/resources/db/migration/V20261116__create_dynamic_nesting_tables.sql):
  - Tables created: `dynamic_nesting_specifications`, `dynamic_nesting_placements`, `dynamic_nesting_offcuts`, `dynamic_nesting_audit_events`.
  - Multi-tenant security: `ENABLE ROW LEVEL SECURITY` & `FORCE ROW LEVEL SECURITY` with `app.current_tenant`.
- [DynamicNestingDataSource.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/datasource/imposition/DynamicNestingDataSource.kt) & [PostgresDynamicNestingDataSource.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/persistence/postgres/PostgresDynamicNestingDataSource.kt) & [FakeDynamicNestingDataSource.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/datasource/imposition/FakeDynamicNestingDataSource.kt).
- [DynamicNestingRepository.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/repository/imposition/DynamicNestingRepository.kt) & [DynamicNestingRepositoryImpl.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/repository/imposition/DynamicNestingRepositoryImpl.kt).
- Wired in [PostgresRepositoryFactory.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/persistence/postgres/PostgresRepositoryFactory.kt).

### C. Service & REST API Layer
- [DynamicNestingService.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/service/imposition/DynamicNestingService.kt): Business workflow for 2D nesting optimization, lifecycle status management, and handoff extraction.
- [NestingDtos.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/api/model/imposition/NestingDtos.kt): Comprehensive DTOs and mappers.
- [BackendUseCases.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/api/server/BackendUseCases.kt): Section 77 use cases with RBAC verification.
- [BackendRouter.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/api/server/BackendRouter.kt):
  - `POST /api/v1/imposition/nesting/optimize`
  - `GET /api/v1/imposition/nesting/specifications`
  - `GET /api/v1/imposition/nesting/specifications/{nestingId}`
  - `POST /api/v1/imposition/nesting/specifications/{nestingId}/status`
  - `GET /api/v1/imposition/nesting/specifications/{nestingId}/handoff`

### D. Android Jetpack Compose UI
- [NestingUiState.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/imposition/NestingUiState.kt)
- [NestingViewModel.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/imposition/NestingViewModel.kt)
- [DynamicNestingCommandCenterScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/imposition/DynamicNestingCommandCenterScreen.kt):
  - Tab 0: 2D Interactive Sheet Canvas Visualization (color-coded placed items with bounding boxes, hatched recoverable offcuts, gray non-recoverable trim waste, KPI summary metrics).
  - Tab 1: Candidate Job Pool Manager (add/remove heterogeneous candidate items, substrate validation).
  - Tab 2: Offcut & Wastage Analytics (recoverable remnant inventory, trim loss metrics).
  - Tab 3: Historical Nesting Runs & Specifications.
- Wired in [AppDestination.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/navigation/AppDestination.kt) and [InternalWorkspaceShells.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/shell/InternalWorkspaceShells.kt).

---

## 4. VERIFICATION AND TEST MATRIX

| Test Suite | Tests Executed | Passed | Failed | Result |
| :--- | :---: | :---: | :---: | :---: |
| `DynamicNestingEngineTest` | 4 | 4 | 0 | **PASSED** |
| `DynamicNestingServiceTest` | 3 | 3 | 0 | **PASSED** |
| `DynamicNestingSecurityEdgeTest` | 2 | 2 | 0 | **PASSED** |
| `NestingViewModelTest` (App Layer) | 3 | 3 | 0 | **PASSED** |
| `GangRunClusteringEngineTest` (Step 02) | 4 | 4 | 0 | **PASSED** |
| `GangRunServiceTest` (Step 02) | 3 | 3 | 0 | **PASSED** |
| `GangRunSecurityEdgeTest` (Step 02) | 2 | 2 | 0 | **PASSED** |
| `SingleJobImpositionEngineTest` (Step 01) | 6 | 6 | 0 | **PASSED** |
| `ImpositionServiceTest` (Step 01) | 3 | 3 | 0 | **PASSED** |
| `ImpositionSecurityEdgeTest` (Step 01) | 2 | 2 | 0 | **PASSED** |
| **Total Imposition Test Suite** | **32** | **32** | **0** | **PASSED** |
| **Android Compile (`:app:compileDebugKotlin`)** | — | — | — | **BUILD SUCCESSFUL** |
| **Full Enterprise Regression (`.\gradlew.bat test`)** | **All 18 Modules** | **All** | **0** | **BUILD SUCCESSFUL** |

---

## 5. REPOSITORY AUDIT STATUS

- Canonical Step Title: **Step 03 — Dynamic Nesting, Sheet Utilization & Wastage Minimization**
- Canonical Step Status: **COMPLETE / CERTIFIED**
- Module 19 Status: **STRICTLY ON HOLD** (No changes made).
- No Shadow Ledger / Inventory Violations.
