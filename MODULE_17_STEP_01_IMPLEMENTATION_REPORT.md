# MODULE 17 → STEP 01 IMPLEMENTATION REPORT
## SMART PRINTING CALCULATOR — CANONICAL CALCULATION FOUNDATION

### 1. Objective
Establish the production-grade canonical calculation and estimation foundation of **Module 17: Smart Printing Calculator** for Sucharu Pro ERP.
This step provides a deterministic, precision-safe, explainable, and multi-tenant isolated print calculation engine capable of processing physical specifications and material costs without creating accounting facts, journal entries, or mutating historical analytical profitability records.

---

### 2. Existing Architecture Discovered & Reused
- Multi-module architecture: `core`, `backend`, `app`.
- Product domain: `ProductType.PRINTING_JOB`, `CUSTOM_JOB`, `FINISHED_PRODUCT`.
- Production stage reference: `ProductionStageType` (CTP, PRINTING, LAMINATION, FOLDING, BINDING, etc.).
- Financial authorities: Module 14 (Revenue), Module 15 (Expenses), Module 16 (Profitability Analytics).
- Persistence: PostgreSQL with Row-Level Security (`TenantContext`, `app.current_tenant`), `TransactionManager`, and `PostgresRepositoryFactory`.
- Security & RBAC: `BackendAuthorizationPolicy`, `AuthenticatedPrincipal`, `UserRole`.

---

### 3. Change Boundary
- **NEW**:
  - `core/.../printingcalculator/PrintingCalculatorModels.kt`
  - `core/.../printingcalculator/PrintingCalculatorMathUtils.kt`
  - `core/.../printingcalculator/PrintingSpecificationNormalizer.kt`
  - `core/.../printingcalculator/PrintingCalculatorValidator.kt`
  - `core/.../printingcalculator/PrintingCalculatorEngine.kt`
  - `core/.../printingcalculator/PrintingCalculatorDataSource.kt`
  - `core/.../printingcalculator/FakePrintingCalculatorDataSource.kt`
  - `core/.../printingcalculator/PostgresPrintingCalculatorDataSource.kt`
  - `core/.../printingcalculator/PrintingCalculatorRepository.kt`
  - `core/.../printingcalculator/PrintingCalculatorRepositoryImpl.kt`
  - `core/.../printingcalculator/PrintingCalculatorService.kt`
  - `core/.../printingcalculator/PrintingCalculatorServiceImpl.kt`
  - `core/.../printingcalculator/PrintingCalculatorDtos.kt`
  - `app/.../printing/calculator/PrintingCalculatorScreen.kt`
  - `core/.../printingcalculator/PrintingCalculatorDomainMathTest.kt`
  - `core/.../printingcalculator/PrintingCalculatorValidatorAndEngineTest.kt`
  - `core/.../printingcalculator/PrintingCalculatorServiceTest.kt`
- **MODIFIED**:
  - `PostgresRepositoryFactory.kt` (Wired data source, repository, and service creation).
  - `BackendUseCases.kt` (Added printing estimation use cases).
  - `BackendRouter.kt` (Added REST routes and request JSON parsers).
- **PROTECTED**:
  - Zero modifications to accounting, revenue, or expense authorities.
  - Zero shadow ledgers created.

---

### 4. Domain Models & Specifications
- **Enums & Value Types**:
  - `PrintingProcessType`: `OFFSET`, `DIGITAL`, `LARGE_FORMAT`, `SCREEN`, `FLEXOGRAPHIC`, `OTHER`.
  - `PrintingSideOption`: `SINGLE_SIDED`, `DOUBLE_SIDED_SAME`, `DOUBLE_SIDED_DIFFERENT`.
  - `ColorMode`: `MONOCHROME`, `TWO_COLOR`, `CMYK_FOUR_COLOR`, `CMYK_PLUS_SPOT`, `SPOT_ONLY`, `MULTI_PROCESS`.
  - `MeasurementUnit`: `MILLIMETERS`, `CENTIMETERS`, `INCHES`, `FEET`, `POINTS`.
  - `PaperStockType`: `ART_PAPER`, `ART_CARD`, `OFFSET_PAPER`, `KRAFT_PAPER`, `BOX_BOARD`, `STICKER_PAPER`, `DUPLEX_BOARD`, etc.
  - `FinishingOperationType`: `CUTTING_TRIMMING`, `GLOSS_LAMINATION`, `MATTE_LAMINATION`, `SOFT_TOUCH_LAMINATION`, `FOLDING`, `CREASING`, `PERFORATION`, `DIE_CUTTING`, `SADDLE_STITCHING`, `PERFECT_BINDING`, etc.
  - `CalculationStatus`: `SUCCESSFUL`, `PARTIAL_CALCULATION`, `INVALID_REQUEST`, `INSUFFICIENT_INPUT`.
  - `EstimateActualClassification`: `ESTIMATED`, `ACTUAL_REFERENCE`.
  - `DiagnosticCode` & `DiagnosticSeverity`: Structured explanations for missing rates, dimensional limits, or excessive waste.
- **Specifications & Results**:
  - `PrintingDimension`: Width, height, unit.
  - `QuantitySpecification`: Ordered quantity, normalized quantity, spoilage allowance.
  - `PaperMaterialSpecification`: Stock type, GSM, sheet dimension, unit prices, sheets per ream.
  - `NormalizedPrintingSpecification`: Unified canonical specification in millimeters and pieces.
  - `MaterialRequirementResult`: Items per sheet, cut direction, productive sheets, waste sheets, total sheets, total reams, paper weight in kg, estimated material cost.
  - `PrintingRequirementResult`: Total impressions, total passes, CTP plate count, estimated printing cost, estimated plate cost.
  - `FinishingRequirementResult`: List of structured breakdown items and total finishing cost.
  - `CalculationBreakdownItem`: Detailed itemization with component codes, unit rates, amounts, and formula references.
  - `PrintingCalculationResult`: Immutable calculation snapshot with SHA-256 request fingerprint and result integrity hash.
  - `Module17Step01PrintingCalculatorHandoffContract`: Verified read-only contract for AI Agents and quotation engines.

---

### 5. Mathematical Precision & Formulations
- Arithmetic precision: `BigDecimal` scale = 4, `RoundingMode.HALF_UP`.
- **Orthogonal Sheet Cuts**:
  $$\text{Cols}_A = \lfloor S_W / I_W \rfloor, \quad \text{Rows}_A = \lfloor S_H / I_H \rfloor \implies N_A = \text{Cols}_A \times \text{Rows}_A$$
  $$\text{Cols}_B = \lfloor S_W / I_H \rfloor, \quad \text{Rows}_B = \lfloor S_H / I_W \rfloor \implies N_B = \text{Cols}_B \times \text{Rows}_B$$
  $$\text{ItemsPerSheet} = \max(N_A, N_B)$$
- **Productive & Waste Sheets**:
  $$\text{ProductiveSheets} = \lceil \text{Quantity} / \text{ItemsPerSheet} \rceil$$
  $$\text{WasteSheets} = \text{SetupSheets} + \lceil \text{ProductiveSheets} \times \text{RunWaste\%} \rceil + \lceil \text{ProductiveSheets} \times \text{FinishWaste\%} \rceil$$
  $$\text{TotalSheets} = \text{ProductiveSheets} + \text{WasteSheets}$$
- **Reams & Paper Weight**:
  $$\text{Reams} = \text{TotalSheets} / 500$$
  $$\text{Weight (kg)} = \frac{S_W \times S_H \times \text{GSM} \times \text{TotalSheets}}{10^9}$$
- **Press Run Impressions & Plates**:
  $$\text{Impressions} = \text{TotalSheets} \times \text{Sides.sideCount}$$
  $$\text{Plates (Offset)} = (\text{FrontColors} + \text{SpotColors}) + (\text{BackColors} + \text{SpotColors})$$
- **Deterministic Hashing**:
  - Request Fingerprint: SHA-256 across normalized inputs.
  - Result Integrity Hash: SHA-256 across result outputs and calculated totals.

---

### 6. Validation & Diagnostic Rules
- Rejects $\le 0$ quantity and $\le 0$ dimensions.
- Rejects sheet dimensions smaller than finished item dimensions.
- Issues warnings for running waste percentages exceeding 30%.
- Returns structured `MISSING_MATERIAL_PRICE` or `MISSING_MACHINE_RATE` diagnostics when rates are absent, producing valid physical calculations in `PARTIAL_CALCULATION` status rather than throwing raw exceptions.

---

### 7. Estimate vs. Actual Boundary
- Every calculation result explicitly contains `classification = ESTIMATED`.
- No journal entries, no ledger entries, and no modifications to actual job costing tables.

---

### 8. Persistence & PostgreSQL RLS
- Parameterized SQL insertion and retrieval via `PostgresPrintingCalculatorDataSource`.
- Row-Level Security enforced via `TenantContext` setting `app.current_tenant`.
- Transaction boundaries handled by `TransactionManager`.

---

### 9. API Endpoints
- `POST /api/v1/printing-calculator/calculations`: Calculates print estimate.
- `POST /api/v1/printing-calculator/validate`: Validates request and returns diagnostics.
- `GET /api/v1/printing-calculator/calculations/{id}`: Retrieves calculation by ID.
- `GET /api/v1/printing-calculator/calculations/{id}/breakdown`: Retrieves cost item breakdown.
- `GET /api/v1/printing-calculator/calculations/{id}/handoff`: Exports AI handoff contract.
- `GET /api/v1/printing-calculator/calculations`: Lists recent calculations for tenant.

---

### 10. Security & RBAC
- Permitted: `ADMIN`, `MANAGER`, `STAFF`, `AI_AGENT`.
- Forbidden: `CUSTOMER`, `VENDOR` (unauthorized access returns 403 Forbidden).
- Anti-spoofing: Tenant context resolved strictly from authenticated JWT (`principal.projectId`).

---

### 11. UI Foundation
- `PrintingCalculatorScreen.kt` in Jetpack Compose.
- Input forms for job specifications, dimensions, substrate/paper, press process, colors, waste, and finishing operations.
- Output cards for estimated totals, physical material requirements, press requirements, cost breakdown table, diagnostics list, and cryptographic integrity display.

---

### 12. Verification & Build Results
```
> Task :core:test
PrintingCalculatorValidatorAndEngineTest > testEngine_computesPhysicalAndCostBreakdownCorrectly PASSED
PrintingCalculatorValidatorAndEngineTest > testEngine_partialCalculation_whenMaterialPriceMissing PASSED
PrintingCalculatorValidatorAndEngineTest > testValidation_rejectsSheetSmallerThanItem PASSED
PrintingCalculatorValidatorAndEngineTest > testValidation_rejectsNegativeAndZeroQuantity PASSED
PrintingCalculatorValidatorAndEngineTest > testValidation_warnsOnExcessiveWaste PASSED
PrintingCalculatorDomainMathTest > testDeterministicFingerprinting_andIntegrityHash PASSED
PrintingCalculatorDomainMathTest > testProductiveSheets_andWasteCalculation PASSED
PrintingCalculatorDomainMathTest > testPaperWeightKg_calculation PASSED
PrintingCalculatorDomainMathTest > testItemsPerSheet_itemExceedsSheet PASSED
PrintingCalculatorDomainMathTest > testItemsPerSheet_rotatedOrientationSelected PASSED
PrintingCalculatorDomainMathTest > testUnitConversionToMillimeters PASSED
PrintingCalculatorDomainMathTest > testItemsPerSheet_orthogonalCalculation PASSED
PrintingCalculatorDomainMathTest > testPlateCount_calculation PASSED
PrintingCalculatorServiceTest > testIdempotency_returnsIdenticalCalculationForSameInputs PASSED
PrintingCalculatorServiceTest > testCalculate_orchestratesAndPersistsResult PASSED
PrintingCalculatorServiceTest > testTenantIsolation_preventsCrossTenantAccess PASSED
PrintingCalculatorServiceTest > testExportHandoffContract_generatesVerifiedContract PASSED

BUILD SUCCESSFUL in 5m 16s
55 actionable tasks: 13 executed, 42 up-to-date
```
- `:core:test`: **PASS (100%)**
- `:backend:test`: **PASS (100%)**
- `:backend:jar`: **PASS (100%)**
- `:app:testDebugUnitTest`: **PASS (100%)**
- `:app:assembleDebug`: **PASS (100%)**

---

### 13. Known Intentional Limitations
- Step 01 focuses on orthogonal sheet cut imposition (standard and rotated). Advanced non-orthogonal polygon die nesting belongs to subsequent specialized finishing steps.
- Automatic live inventory substrate stock reservation is intentionally reserved for subsequent quotation and job conversion steps.

---

### 14. Downstream Handoff Contract
Exposed `Module17Step01PrintingCalculatorHandoffContract` providing complete physical requirement details, cost breakdowns, diagnostics, and SHA-256 integrity hashes for consumption by quotation workflows and AI subagents.
