# MODULE 15 → STEP 04: BUSINESS COST CENTER, COST CATEGORY & JOB/PROJECT COST TRACKING & ALLOCATION MANAGEMENT FOUNDATION

## Executive Summary
Module 15 Step 04 establishes the enterprise operational cost allocation and tracking engine for Sucharu Pro ERP. It introduces hierarchical **Business Cost Centers**, structured **Business Cost Categories**, and operational **Job/Project Cost Tracking** that links directly to canonical financial sources (`BUSINESS_EXPENSE`, `VENDOR_PAYABLE`, `BUSINESS_LEDGER_POSTING`, and `MANUAL_OPERATIONAL_REFERENCE`) without duplicating financial account balances or mutating canonical general ledger postings.

---

## Architecture & Implementation Matrix

### 1. Database Schema & Flyway Migration
- **Script**: `V20261018__create_business_cost_centers_and_tracking.sql`
- **Tables**:
  - `business_cost_centers`: Hierarchical cost center tree (`parent_cost_center_id`, `code`, `name`, `is_active`, `version`, `created_at`, `updated_at`).
  - `business_cost_categories`: Standardized operational categories (`is_system_defined`, `parent_category_id`, `code`, `name`).
  - `business_cost_tracking`: Operational cost items linked to canonical sources with `DECIMAL(18,4)` scale arithmetic.
  - `business_cost_classification_audit_events`: Append-only audit trail capturing state changes, actor role, idempotency keys, and mandatory reclassification reasons.
- **Security & RLS**: Enabled PostgreSQL Row-Level Security (`sucharu_rls_policy`) filtering strictly by `tenant_id` and `project_id`.

### 2. Domain Models & Validation
- **Package**: `com.sucharu.sucharupro.domain.model.businesscost` & `com.sucharu.sucharupro.domain.validation.businesscost`
- **Invariants**:
  - Code & name validation with minimum lengths and uppercase sanitization.
  - Self-parenting cycle prevention.
  - Exact 4-decimal precision verification (`DECIMAL(18,4)`).
  - Supported currency verification (`BDT`, `USD`, `EUR`, `GBP`, `INR`).
  - Reclassification requires mandatory justifications (min 3 characters).

### 3. Data Access & Repositories
- **DataSources**: `PostgresBusinessCostManagementDataSource` (with robust JDBC SQL operations) and `FakeBusinessCostManagementDataSource` (in-memory for instant isolation and concurrency testing).
- **Repository Interface & Implementation**: `BusinessCostManagementRepository` / `BusinessCostManagementRepositoryImpl`.
- **Factory Wiring**: Registered in `PostgresRepositoryFactory.kt` and `BackendUseCases.kt` (Section 27).

### 4. Domain Service & Separation of Duties
- **Service**: `BusinessCostManagementServiceImpl`
- **Access Control Matrix**:
  - `ADMIN` & `MANAGER`: Full authority (create/update/activate/deactivate cost centers/categories, classify, reclassify, summarize).
  - `STAFF`: Operational ingestion & initial classification (`trackOperationalCost`, `classifyCost`, list/view).
  - `CUSTOMER`, `VENDOR`, `AFFILIATE`, `GUEST`: Completely restricted (403/Forbidden error returned).
- **Immutability of Ledger Postings**: Operational tracking and reclassifications emit immutable audit records without modifying underlying general ledger postings or financial balances.

### 5. Backend HTTP Routing & REST API
- **Router**: `BackendRouter.kt`
- **Endpoints**:
  - `GET /api/v1/business-cost-centers`: List cost centers with active filters.
  - `POST /api/v1/business-cost-centers`: Create cost center.
  - `GET /api/v1/business-cost-centers/{id}`: Fetch cost center by ID.
  - `PUT /api/v1/business-cost-centers/{id}`: Update cost center.
  - `POST /api/v1/business-cost-centers/{id}/activate`: Activate cost center.
  - `POST /api/v1/business-cost-centers/{id}/deactivate`: Deactivate cost center.
  - `GET /api/v1/business-cost-categories`: List cost categories.
  - `POST /api/v1/business-cost-categories`: Create cost category.
  - `GET /api/v1/business-cost-categories/{id}`: Fetch cost category by ID.
  - `PUT /api/v1/business-cost-categories/{id}`: Update cost category.
  - `POST /api/v1/business-cost-categories/{id}/activate`: Activate category.
  - `POST /api/v1/business-cost-categories/{id}/deactivate`: Deactivate category.
  - `GET /api/v1/business-cost-tracking`: List tracked costs with filters (`sourceType`, `costCenterId`, `costCategoryId`, `jobId`, `allocationStatus`).
  - `POST /api/v1/business-cost-tracking`: Track operational cost from canonical source.
  - `GET /api/v1/business-cost-tracking/{id}`: Get tracked cost details.
  - `POST /api/v1/business-cost-tracking/{id}/classify`: Classify unallocated cost to job.
  - `POST /api/v1/business-cost-tracking/{id}/reclassify`: Reclassify cost with audit reason.
  - `GET /api/v1/business-cost-tracking/summary`: Summary KPIs of operational cost allocations.
  - `GET /api/v1/business-cost-tracking/job/{jobId}/summary`: Detailed job cost breakdown.
  - `GET /api/v1/business-cost-tracking/audit-events`: Query audit logs.

### 6. Jetpack Compose UI
- **Screen**: `BusinessCostManagementScreen.kt` in `app/src/main/java/com/sucharu/sucharupro/ui/features/cost/`
- **Features**:
  - Live KPI metric cards (Total Tracked, Allocated, Unallocated, Active Centers).
  - Multi-tab navigation: Cost Tracking Overview, Cost Centers Directory, Cost Categories, Job Cost Analytics.
  - Ingestion Modal for capturing operational costs linked to expenses/payables.
  - Reclassification Modal with mandatory reason requirement.
  - Cyber-ERP high-density visual theme with animated progress and color indicators.

---

## Test Verification Summary
All 14 dedicated test suites passed with 100% success (alongside 942+ repo-wide tests):
1. `BusinessCostCenterDomainTest` — Cost center validation, self-parenting prevention, and code formatting.
2. `BusinessCostCategoryDomainTest` — Cost category properties and constraints.
3. `BusinessCostTrackingDomainTest` — Mathematical precision, positive bounds, and currency validation.
4. `BusinessCostManagementRepositoryTest` — CRUD operations, optimistic locking, and audit log persistence.
5. `BusinessCostManagementServiceTest` — Service orchestration with canonical source reconciliation.
6. `BusinessCostManagementSecurityTest` — RBAC enforcement, staff vs manager boundaries, and role blacklisting.
7. `BusinessCostManagementIsolationTest` — Multi-tenant data segregation across organizations.
8. `BusinessCostManagementConcurrencyTest` — High-throughput parallel cost tracking without race conditions.
9. `BusinessCostManagementIdempotencyTest` — Deduplication on idempotent tracking and classification requests.
10. `BusinessCostManagementPrecisionTest` — 4-decimal currency arithmetic and aggregation without floating-point error.
11. `BusinessCostManagementConsistencyTest` — State machine lifecycle: `UNALLOCATED` -> `FULLY_ALLOCATED` -> `RECLASSIFIED`.
12. `BusinessCostManagementApiTest` — End-to-end HTTP request handling and JWT security verification.
13. `BusinessCostClassificationTest` — Direct job assignment of unallocated operational costs.
14. `BusinessCostReclassificationTest` — Reclassification validation, historical audit recording, and version incrementing.

---

## Status: COMPLETE
Module 15 Step 04 is verified, fully tested, and ready for production deployment.
