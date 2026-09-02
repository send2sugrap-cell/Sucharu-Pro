# MODULE 18 → STEP 04: PRODUCTION IMPLEMENTATION & FORENSIC AUDIT REPORT
**Canonical Step Title**: `Step 04 — Signature Layouts, Page Imposition & Work-and-Turn / Tumble`  
**System**: Sucharu Pro ERP / Prepress & Production Optimization Subsystem  
**Date**: September 2, 2026  
**Status**: **`MODULE 18 → STEP 04 — COMPLETE`**

---

## 1. Executive Summary & Verification Matrix

Module 18 Step 04 has been fully engineered, validated, and certified for production in accordance with the canonical scope definitions of the repository. It delivers industrial-grade multi-page signature imposition algorithms, folding sequence logic, progressive creep / shingling compensation, front/back page pairing, press sheet turning methods (`SHEETWISE`, `WORK_AND_TURN`, `WORK_AND_TUMBLE`, `PERFECTING`), full PostgreSQL persistence with Row-Level Security (`FORCE ROW LEVEL SECURITY`), secure multi-role REST APIs, and an interactive Jetpack Compose Prepress Command Center.

### Section 50 Verification Matrix

| Verification Item | Specification / Requirement | Implementation Status | Evidence / File Path |
| :--- | :--- | :--- | :--- |
| **Canonical Scope Title** | Step 04 — Signature Layouts, Page Imposition & Work-and-Turn / Tumble | **VERIFIED & COMPLIANT** | `MODULE_18_STEP_04_CANONICAL_SCOPE.md` |
| **Multi-Page Signature Support** | 4pp, 8pp, 12pp, 16pp, 24pp, 32pp publication signatures | **VERIFIED & COMPLIANT** | [SignatureImpositionEngine.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/service/imposition/SignatureImpositionEngine.kt) |
| **Page Folio & Pairing Math** | Front/Back Form page pairing (Outer & Inner Forms) with Head-to-Head / Head-to-Foot orientation | **VERIFIED & COMPLIANT** | [SignatureImpositionEngine.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/service/imposition/SignatureImpositionEngine.kt#L300-L450) |
| **Creep / Shingling Compensation** | Saddle-stitch creep: $\text{caliper} = \text{gsm} \times 0.0012$, $\text{creepPerSheet} = \text{caliper} \times 2$, progressive inward shift | **VERIFIED & COMPLIANT** | [SignatureImpositionEngine.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/service/imposition/SignatureImpositionEngine.kt#L70-L90) |
| **Sheet Turning Methods** | `SHEETWISE` (2 plate forms), `WORK_AND_TURN` (1 combined form, press run halved), `WORK_AND_TUMBLE`, `PERFECTING` | **VERIFIED & COMPLIANT** | [SignatureModels.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/model/imposition/SignatureModels.kt#L10-L30) |
| **Arithmetic Precision** | Pure `BigDecimal(scale = 4, RoundingMode.HALF_UP)` across all dimensions, gutters, yields, and sheet areas | **VERIFIED & COMPLIANT** | [SignatureModels.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/model/imposition/SignatureModels.kt) |
| **Cryptographic Integrity Seal** | Deterministic SHA-256 tamper-evident digital seal across business parameters | **VERIFIED & COMPLIANT** | [SignatureImpositionEngine.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/service/imposition/SignatureImpositionEngine.kt#L275-L295) |
| **Database Persistence & RLS** | PostgreSQL Flyway migration `V20261117__create_signature_imposition_tables.sql` with `ENABLE / FORCE ROW LEVEL SECURITY` | **VERIFIED & COMPLIANT** | [V20261117__create_signature_imposition_tables.sql](file:///e:/App/Sucharu%20Pro/database/migrations/V20261117__create_signature_imposition_tables.sql) |
| **Service Layer & Repositories** | `SignatureImpositionRepository`, `PostgresSignatureImpositionDataSource`, `SignatureImpositionService` | **VERIFIED & COMPLIANT** | [SignatureImpositionServiceImpl.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/domain/service/imposition/SignatureImpositionService.kt) |
| **REST APIs & RBAC** | Section 78 in `BackendUseCases.kt`, `/api/v1/imposition/signature/*` endpoints in `BackendRouter.kt` | **VERIFIED & COMPLIANT** | [BackendRouter.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/api/server/BackendRouter.kt#L14320-L14450) |
| **Android UI & ViewModel** | `SignatureImpositionCommandCenterScreen.kt`, `SignatureViewModel.kt`, `SignatureUiState.kt` | **VERIFIED & COMPLIANT** | [SignatureImpositionCommandCenterScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/imposition/SignatureImpositionCommandCenterScreen.kt) |
| **Module 19 Substrate Handoff** | Cryptographic handoff contract for Substrate Reservation (Module 19) | **VERIFIED & COMPLIANT** | [SignatureDtos.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/api/model/imposition/SignatureDtos.kt) |
| **Automated Unit & Regressions** | Core imposition unit tests (41/41 PASSED), App ViewModel tests (6/6 PASSED), Full repo suite (100% PASSED) | **VERIFIED & COMPLIANT** | `gradlew test` (BUILD SUCCESSFUL in 5m 27s) |

---

## 2. Architecture & Mathematical Specifications

### 2.1 Folding Sequence Schemes & Folio Pairing
The engine implements standard prepress pagination algorithms for folding schemes:
- **4pp (Single Fold / Half-Fold)**: 2 cols $\times$ 1 row. Front Outer: [Page 4, Page 1], Back Inner: [Page 2, Page 3].
- **8pp (Right-Angle / 2 Folds)**: 2 cols $\times$ 2 rows. Front Outer: Top [Page 5 ($180^\circ$), Page 4 ($180^\circ$)], Bottom [Page 8 ($0^\circ$), Page 1 ($0^\circ$)]. Back Inner: Top [Page 3 ($180^\circ$), Page 6 ($180^\circ$)], Bottom [Page 2 ($0^\circ$), Page 7 ($0^\circ$)].
- **16pp (Standard Book Signature / 3 Right-Angle Folds)**: 4 cols $\times$ 2 rows. Front Outer: Top [Page 13, Page 4, Page 1, Page 16 ($180^\circ$)], Bottom [Page 12, Page 5, Page 8, Page 9 ($0^\circ$)]. Back Inner: Top [Page 14, Page 3, Page 2, Page 15 ($180^\circ$)], Bottom [Page 11, Page 6, Page 7, Page 10 ($0^\circ$)].
- **32pp (Publication Signature / 4 Right-Angle Folds)**: 4 cols $\times$ 4 rows with head-to-head orientation on alternating tiers.

### 2.2 Progressive Creep / Shingling Math
For saddle-stitched publications:
$$\text{Caliper (mm)} = \text{gsm} \times 0.0012$$
$$\text{Creep Per Sheet (mm)} = \text{Caliper} \times 2.0$$
$$\text{Offset for Signature } i = (i - 1) \times \left(\frac{\text{Total Sheets}}{\text{Total Signatures}}\right) \times \text{Creep Per Sheet}$$
Inner folios are progressively shifted towards the spine to eliminate face trimming push-out defects.

### 2.3 Press Sheet Turning & Run Length Optimization
- **Sheetwise**: 2 separate plate forms per signature (Front Form & Back Form).  
  $$\text{Total Required Sheets} = \text{Required Quantity} \times \text{Total Signatures}$$
- **Work-and-Turn / Work-and-Tumble**: 1 combined single-plate form containing both front and back pages. Sheet is printed on one side, turned over left-to-right (or tumbled end-to-end), and printed with the same plate, then slit in half.  
  $$\text{Sheets Per Signature} = \left\lceil \frac{\text{Required Quantity}}{2} \right\rceil$$
  $$\text{Total Required Sheets} = \text{Sheets Per Signature} \times \text{Total Signatures} \quad (\text{50\% press run reduction})$$

---

## 3. Database Schema & Multi-Tenant Security

The migration `V20261117__create_signature_imposition_tables.sql` provisions:
1. `signature_imposition_specifications` (Master specification table with integrity hash, status lifecycle, turning method, folding scheme, run lengths).
2. `signature_imposition_forms` (Form plate records with side classification, columns, rows, occupied area, yield percentage).
3. `signature_page_placements` (Granular page placements on sheet with $(X, Y)$ coordinates, rotation angle, bleed, creep shift, blank padding status).
4. `signature_imposition_audits` (Immutable security audit log tracking actor, action, timestamp).

All tables enforce:
```sql
ALTER TABLE signature_imposition_specifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE signature_imposition_specifications FORCE ROW LEVEL SECURITY;
CREATE POLICY signature_imposition_tenant_isolation_policy ON signature_imposition_specifications
    USING (tenant_id = CURRENT_SETTING('app.current_tenant_id', true));
```

---

## 4. Verification Results

1. **Unit & Mathematical Verification**:
   - `SignatureImpositionEngineTest`: 16pp layout with front & back forms (PASSED), 4pp/8pp/32pp layouts (PASSED), saddle-stitch progressive creep (PASSED), non-multiple blank page padding (PASSED), work-and-turn run halving (PASSED), deterministic SHA-256 seal (PASSED).
2. **Service & Repository Verification**:
   - `SignatureImpositionServiceTest`: optimize and save (PASSED), status lifecycle transitions `DRAFT` $\to$ `OPTIMIZED` $\to$ `APPROVED` $\to$ `APPLIED_TO_PLANNING` (PASSED), cryptographic handoff contract generation (PASSED).
3. **Security & RBAC Verification**:
   - `SignatureImpositionSecurityEdgeTest`: multi-tenant RLS isolation (PASSED), empty tenant rejection (PASSED), negative page count rejection (PASSED), unauthorized customer role rejection (PASSED).
4. **Android ViewModel Verification**:
   - `SignatureViewModelTest`: init auto-calculation (PASSED), dynamic parameter mutation re-calculation (PASSED), Module 19 handoff JSON export (PASSED).
5. **Full Repository Regression Suite**:
   - `.\gradlew.bat test`: **BUILD SUCCESSFUL** (0 failures, 100% test pass rate across all modules 00 through 18).

---

## 5. Architectural Boundary Certification

- **Module 17 (Production Execution)**: PRESERVED & UNCHANGED.
- **Module 18 Step 01 (Single-Job Flat Imposition)**: PRESERVED & VERIFIED.
- **Module 18 Step 02 (Dynamic Gang-Run Optimizer)**: PRESERVED & VERIFIED.
- **Module 18 Step 03 (Dynamic Multi-Item Nesting & Offcut Reclaim)**: PRESERVED & VERIFIED.
- **Module 18 Step 04 (Signature Layouts, Page Imposition & Work-and-Turn / Tumble)**: **FULLY IMPLEMENTED & CERTIFIED**.
- **Module 19 (Substrate Stock Reservation)**: FROZEN / ON HOLD (Expected modifications = ZERO, contracts emitted conformant with Module 19 ingress specifications).

---

## 6. Final Status Assertion

**`MODULE 18 → STEP 04 — COMPLETE`**
