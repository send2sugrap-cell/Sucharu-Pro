# MODULE 18 STEP 01 — PRODUCTION IMPLEMENTATION REPORT
## Automated Sheet Layout & Single-Job Dynamic Imposition Engine

---

### 1. Executive Summary & Verification Certification
This document certifies the complete, production-grade implementation and regression verification of **Module 18 → Step 01: Automated Sheet Layout & Single-Job Dynamic Imposition Engine** for the Sucharu Pro ERP.
All domain calculations, deterministic candidate evaluators, multi-tenant row-level security migrations, persistence layers, backend APIs, Jetpack Compose presentation command centers, and test suites have been executed with 100% success.
Zero regressions were introduced into Modules 00 through 17 or Module 19 Steps 01–02.

---

### 2. Module & Step Identity
* **Canonical Module Title:** Module 18 — Advanced Dynamic Imposition & Gang-Run Optimizer Engine
* **Canonical Step Title:** Step 01 — Automated Sheet Layout & Single-Job Dynamic Imposition Engine
* **Step Scope Boundary:** SINGLE-JOB ONLY: ONE JOB → ONE PRODUCT → ONE IMPOSITION LAYOUT. Multi-job gang-run batching, dynamic nesting, signature book imposition, and CTP plate outputs are explicitly deferred to future steps (Steps 02 through 06).

---

### 3. Canonical Architecture & Architectural Invariants
* **Strict Separation of Concerns:** Imposition calculations are purely orthogonal geometric packaging evaluations and do not manipulate physical inventory balances or general ledger accounting entries.
* **Module 06 Authority:** Physical inventory remains strictly governed by Module 06.
* **Module 19 Compatibility:** Material reservations remain owned by Module 19 (`SubstrateReservationService`). Module 18 exports a cryptographic handoff contract (`Module18Step01ImpositionHandoffContract`) consumed seamlessly by Module 19 Step 01's `SubstrateRequirementResolver`.
* **Zero Shadow Balances:** Zero shadow inventory registers and zero shadow accounting ledgers created.

---

### 4. Precision Arithmetic & Mathematical Formulations
All dimensional, area, and quantity calculations are performed using `BigDecimal(scale = 4, RoundingMode.HALF_UP)`:
1. **Usable Area Dimensions:**
   $$\text{usableWidth} = \text{sheetWidth} - (\text{marginLeft} + \text{marginRight})$$
   $$\text{usableHeight} = \text{sheetHeight} - (\text{marginTop} + \text{marginBottom})$$
2. **Orthogonal Packing Grid:**
   $$\text{cols} = \left\lfloor \frac{\text{usableWidth} + \text{hGutter}}{\text{itemWidth} + 2\cdot\text{bleed} + \text{hGutter}} \right\rfloor$$
   $$\text{rows} = \left\lfloor \frac{\text{usableHeight} + \text{vGutter}}{\text{itemHeight} + 2\cdot\text{bleed} + \text{vGutter}} \right\rfloor$$
   $$\text{copiesPerSheet} = \text{cols} \times \text{rows}$$
3. **Required Parent Sheets:**
   $$\text{requiredSheets} = \left\lceil \frac{\text{requiredQuantity}}{\text{copiesPerSheet}} \right\rceil$$
4. **Produced Capacity & Overage:**
   $$\text{totalProducedCapacity} = \text{requiredSheets} \times \text{copiesPerSheet}$$
   $$\text{overageQuantity} = \text{totalProducedCapacity} - \text{requiredQuantity}$$
5. **Yield & Waste Percentage:**
   $$\text{occupiedArea} = \text{copiesPerSheet} \times (\text{itemWidth} \times \text{itemHeight})$$
   $$\text{wasteArea} = \text{usableArea} - \text{occupiedArea}$$
   $$\text{yieldPercentage} = \frac{\text{occupiedArea}}{\text{usableArea}} \times 100$$

---

### 5. Deterministic Orientation Ranking Matrix
When `orientationPolicy` is `AUTO_OPTIMAL`, the engine evaluates both STANDARD (0°) and ROTATED (90°) candidates and ranks them deterministically:
1. Candidate with strictly greater `copiesPerSheet` wins.
2. If tied, candidate with lowest `wasteAreaMm2` wins.
3. If tied, candidate with highest `yieldPercentage` wins.
4. If still tied, `STANDARD` (0°) orientation is chosen as the deterministic tiebreaker.

---

### 6. Cryptographic Determinism & Integrity Hashing
Every generated specification computes an invariant SHA-256 integrity hash:
$$\text{SHA-256}(\text{tenantId} \parallel \text{orderId} \parallel \text{orderItemId} \parallel \text{itemW} \parallel \text{itemH} \parallel \text{sheetW} \parallel \text{sheetH} \parallel \text{orientation} \parallel \text{cols} \parallel \text{rows} \parallel \text{copies} \parallel \text{requiredSheets})$$
This guarantees reproducible verification across distributed shop-floor tablets and server clusters.

---

### 7. Database Migration & Row-Level Security (RLS)
* **Migration File:** `V20261114__create_imposition_layout_tables.sql`
* **Tables Created:**
  * `imposition_specifications`
  * `imposition_audit_events`
* **Multi-Tenant RLS Enforcement:**
  ```sql
  ALTER TABLE imposition_specifications ENABLE ROW LEVEL SECURITY;
  ALTER TABLE imposition_specifications FORCE ROW LEVEL SECURITY;
  CREATE POLICY imposition_specifications_tenant_isolation ON imposition_specifications
      USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
      WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
  ```

---

### 8. Persistence Architecture
* **Interface:** `ImpositionDataSource`
* **Implementations:**
  * `FakeImpositionDataSource`: High-speed thread-safe in-memory store for isolated unit tests.
  * `PostgresImpositionDataSource`: Production JDBC implementation using `TransactionManager.inTransaction(TenantContext(tenantId))`.
* **Repository:** `ImpositionRepository` & `ImpositionRepositoryImpl`
* **Factory Integration:** Integrated cleanly in `PostgresRepositoryFactory` via `createImpositionDataSource`, `createImpositionRepository`, and `createImpositionService`.

---

### 9. Service Layer
* **Interface:** `ImpositionService`
* **Implementation:** `ImpositionServiceImpl`
* **Core Capabilities:**
  * `calculateOptimalLayout`: Executes purely mathematical optimization.
  * `calculateAndSave`: Validates, optimizes, and atomically persists imposition specification.
  * `getImpositionSpecification`: Tenant-isolated specification retrieval.
  * `listImpositionsByJob`: Retrieves all specifications created for a specific job.
  * `listAllImpositions`: Paginated retrieval for tenant dashboards.
  * `updateImpositionStatus`: Governs lifecycle transitions (`OPTIMIZED`, `APPLIED_TO_PLANNING`, `REJECTED`, `SUPERSEDED`, `LOCKED_FOR_PRODUCTION`) with audit event capture.
  * `exportHandoffContract`: Exports standardized handoff contract for downstream modules.

---

### 10. REST API Specification & Router Endpoints
The following endpoints were registered in `BackendRouter.kt` and `BackendUseCases.kt`:
1. `POST /api/v1/imposition/calculate`: Calculates dynamic layout, optionally saving it.
2. `GET /api/v1/imposition/specifications`: Lists all imposition specifications for the tenant.
3. `GET /api/v1/imposition/specifications/{impositionId}`: Retrieves specific imposition specification.
4. `GET /api/v1/imposition/jobs/{jobId}`: Retrieves imposition specifications linked to a specific job.
5. `POST /api/v1/imposition/specifications/{impositionId}/status`: Updates imposition lifecycle status.
6. `GET /api/v1/imposition/specifications/{impositionId}/handoff`: Exports Module 19-compatible handoff contract.

---

### 11. Security & RBAC Enforcement
* Authorized Roles: `ADMIN`, `MANAGER`, `STAFF`, `AI_AGENT`.
* Unauthorized Roles: `CUSTOMER` and `VENDOR` requests are rejected with `403 FORBIDDEN` (`ForbiddenException`).
* Unauthenticated requests are rejected with `401 UNAUTHENTICATED` (`UnauthenticatedException`).

---

### 12. Android Jetpack Compose Presentation
* **Navigation Route:** `AppDestination.Staff.Imposition`, `AppDestination.Manager.Imposition`, and `AppDestination.Admin.Imposition` registered at `"staff/imposition"`, `"manager/imposition"`, and `"admin/imposition"`.
* **Workspace Shells:** Filter chips integrated in `InternalWorkspaceShells.kt` for Staff, Manager, and Admin workspaces.
* **UI Command Center:** `ImpositionCommandCenterScreen.kt` featuring:
  * Tab 0: Real-time Layout Visualizer with interactive copies-per-sheet grid, KPI summary cards (Yield %, Required Sheets, Waste Area mm²), and integrity hash display.
  * Tab 1: Parameter Configuration Form (Job ID, item dimensions, parent sheet dimensions, bleed, gutters, margins, required quantity).
  * Tab 2: Historical Imposition Specifications List with quick-selection inspection.
* **ViewModel:** `ImpositionViewModel.kt` backed by `SingleJobImpositionEngine` for instantaneous offline/online layout simulation.

---

### 13. Unit Test Verification Results
All unit and security tests passed with 0 failures:
1. `SingleJobImpositionEngineTest`:
   * `testStandardA4On25x36Sheet_CalculatesOptimalLayout` — **PASSED**
   * `testRotatedOrientationPreferredWhenYieldHigher` — **PASSED**
   * `testForceStandardOrientationPolicy` — **PASSED**
   * `testItemLargerThanUsableSheet_ThrowsException` — **PASSED**
   * `testZeroQuantity_ThrowsException` — **PASSED**
   * `testDeterministicIntegrityHash_IsIdenticalForSameInputs` — **PASSED**
2. `ImpositionServiceTest`:
   * `testCalculateAndSave_PersistsAndRetrievesSpecification` — **PASSED**
   * `testExportHandoffContract_GeneratesModule19CompatibleHandoff` — **PASSED**
   * `testUpdateImpositionStatus_UpdatesLifecycleCorrectly` — **PASSED**
3. `ImpositionSecurityEdgeTest`:
   * `testTenantIsolation_CrossTenantAccessReturnsNullOrEmpty` — **PASSED**
   * `testUnauthorizedRole_CustomerRejectedFromImpositionCalculation` — **PASSED**

---

### 14. Full Suite Regression Testing
* **Command:** `.\gradlew.bat test`
* **Duration:** 5m 15s
* **Result:** **BUILD SUCCESSFUL**
* **Modules Verified:** Modules 00 through 17 baseline tests + Module 18 Step 01 tests + Module 19 Step 01–02 tests (100% green).
* **Android Compilation:** `.\gradlew.bat :app:compileDebugKotlin` — **BUILD SUCCESSFUL** (1m 10s).

---

### 15. Certification Statement
Module 18 Step 01 is completely implemented, verified, deterministic, tenant-isolated, and production-ready.
