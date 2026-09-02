# FINAL API INTEGRITY AUDIT — MODULE 00 → MODULE 18

## REST Endpoint Catalog, Contract Invariance, Status Codes, Idempotency & Auth

---

### 1. Authoritative API Inventory

| Method | Endpoint Path | Module Scope | Required Role | Tenant Safe | Idempotent | Contract DTO | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Module 00 | Public | N/A (Global) | Yes | `LoginRequestDto` -> `AuthTokenResponse` | **200 OK** |
| `GET` | `/api/v1/customers/{id}` | Module 02 | Customer / Admin | Yes (RLS) | Yes | `CustomerResponseDto` | **200 OK** |
| `POST` | `/api/v1/orders` | Module 03 | Customer / Manager | Yes (RLS) | Yes (Key) | `CreateOrderRequestDto` -> `OrderDto` | **201 Created** |
| `GET` | `/api/v1/orders/{id}` | Module 03 | Authenticated | Yes (RLS) | Yes | `OrderDto` | **200 OK** |
| `POST` | `/api/v1/printing-calculator/calculate` | Module 17 Step 01 | Public / Customer | Yes (RLS) | Yes | `CalculatePrintingCostRequestDto` | **200 OK** |
| `POST` | `/api/v1/printing-quotes` | Module 17 Step 02 | Customer / Staff | Yes (RLS) | Yes | `CreatePrintingQuoteRequestDto` | **201 Created** |
| `POST` | `/api/v1/commercial-commitments` | Module 17 Step 03 | Customer / Manager | Yes (RLS) | Yes | `CreateCommercialCommitmentRequestDto` | **201 Created** |
| `POST` | `/api/v1/production-planning/snapshots` | Module 17 Step 04 | Manager / Admin | Yes (RLS) | Yes | `CreateProductionPlanningSnapshotRequestDto` | **201 Created** |
| `POST` | `/api/v1/production-jobs` | Module 17 Step 05 | Manager / Admin | Yes (RLS) | Yes | `CreateProductionJobRequestDto` | **201 Created** |
| `POST` | `/api/v1/production-scheduling/jobs/{id}/schedule` | Module 17 Step 06 | Manager / Admin | Yes (RLS) | Yes | `CreateScheduleForJobRequestDto` | **201 Created** |
| `POST` | `/api/v1/shop-floor-tracking/jobs/{id}/time-records` | Module 17 Step 07 | Staff / Operator | Yes (RLS) | Yes | `RecordOperatorTimeRequestDto` | **201 Created** |
| `POST` | `/api/v1/final-qc/jobs/{id}/inspections` | Module 17 Step 08 | QC Inspector / Staff | Yes (RLS) | Yes | `CreateFinalQcInspectionRequestDto` | **201 Created** |
| `POST` | `/api/v1/job-costing/jobs/{id}/calculate` | Module 17 Step 09 | Manager / Admin | Yes (RLS) | Yes | `CalculateActualJobCostRequestDto` | **200 OK** |
| `POST` | `/api/v1/job-closure/jobs/{id}/close-and-seal` | Module 17 Step 10 | Manager / Admin | Yes (RLS) | Yes | `CloseAndSealJobRequestDto` | **201 Created** |
| `GET` | `/api/v1/job-closure/jobs/{id}/ai-handoff` | Module 17 Step 10 | AI Agent / Manager | Yes (RLS) | Yes | `Module17Step10JobClosureGovernanceHandoffContractDto` | **200 OK** |

---

### 2. Contract Invariance Findings
- No broken, orphan, or deprecated endpoints detected.
- All request parsers in `BackendRouter.kt` perform null-safe type coercions.
- Response payloads strictly encapsulated in `ApiSuccessResponse` with unique `correlationId`.
