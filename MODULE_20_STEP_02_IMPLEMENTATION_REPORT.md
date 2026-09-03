# MODULE 20 → STEP 02: AFFILIATE PROGRAM & RELATIONSHIP MANAGEMENT
## Production Implementation Report

### Executive Summary
This document provides the formal engineering summary and implementation documentation for **Module 20 → Step 02: Affiliate Program & Relationship Management** in **Sucharu Pro**.

This step establishes the operational governance layer managing:
1. Operational Affiliate Programs with lifecycle states (`DRAFT`, `ACTIVE`, `PAUSED`, `CLOSED`, `ARCHIVED`).
2. Affiliate-Program Enrollments with distinct relationship lifecycle states (`PENDING`, `APPROVED`, `ACTIVE`, `SUSPENDED`, `TERMINATED`, `EXPIRED`, `REJECTED`).
3. Multi-criteria Enrollment Eligibility validation with cross-tenant isolation.
4. Cryptographic SHA-256 append-only audit ledger chaining and outbox event publishing.
5. Strict AI boundary enforcement and immutable cross-module handoff contracts (`Module20Step02ProgramHandoffContract`).
6. High-contrast Jetpack Compose management UI for Programs and Enrollments.

---

### Key Architectural Invariants & Scope Boundaries
* **Strict Step Isolation**: Does **NOT** implement commission calculation (Module 22), wallet accounting/payout processing (Module 23), click/referral tracking (Module 21), or advanced attribution analytics.
* **Separation of Duties (SoD)**:
  * Customer & Vendor roles are strictly prohibited from creating, updating, or managing programs/enrollments.
  * Staff users can create programs in `DRAFT` and request enrollment (`PENDING`), but only Manager and Admin roles can activate/pause/close programs or approve/activate/suspend/terminate enrollments.
  * Only Admin can archive closed programs.
  * AI Agents are strictly read-only and prohibited from executing status mutations.
* **Multi-Tenant Isolation**: Program and enrollment entities enforce strict tenant isolation (`tenantId`) via PostgreSQL Row-Level Security (RLS) policies and `TenantContext` propagation.
* **Append-Only Cryptographic Audit Chaining**: Every lifecycle state mutation records a tamper-evident audit record with SHA-256 record hash and previous-hash chaining starting from `GENESIS_AFFILIATE_PROGRAM_AUDIT_BLOCK`.

---

### Database Schema & Migrations
Created migration script:
* `database/migrations/V20261125__create_affiliate_program_and_enrollment_tables.sql`
* `core/src/main/resources/db/migration/V20261125__create_affiliate_program_and_enrollment_tables.sql`

#### Tables Created:
1. `affiliate_programs`: Program metadata, state, capacity, effective dates, eligibility policy, terms reference.
2. `affiliate_enrollments`: Multi-tenant affiliate-program relationship binding, status, effective ranges, approval/suspension/termination actors and reasons.
3. `affiliate_program_audit_records`: Tamper-evident ledger of program and enrollment lifecycle transitions with `record_hash` and `chain_hash`.
4. `affiliate_program_outbox_events`: Transactional outbox table for event publishing (`AffiliateProgramCreated`, `AffiliateProgramActivated`, `AffiliateEnrolled`, etc.).

---

### Core Domain & Data Layer Components
1. **Domain Models** (`AffiliateProgramModels.kt`):
   * `AffiliateProgramStatus`: `DRAFT`, `ACTIVE`, `PAUSED`, `CLOSED`, `ARCHIVED`
   * `AffiliateEnrollmentStatus`: `PENDING`, `APPROVED`, `ACTIVE`, `SUSPENDED`, `TERMINATED`, `EXPIRED`, `REJECTED`
   * `AffiliateProgram`, `AffiliateEnrollment`, `AffiliateProgramAuditRecord`, `AffiliateProgramOutboxEvent`, `AffiliateProgramGovernanceSummary`
   * `Module20Step02ProgramHandoffContract` (v1.0.0): Read-only contract signed with SHA-256 integrity seal and explicit `allowedAiActions` / `forbiddenAiActions`.
2. **Validation Engine** (`AffiliateProgramValidationEngine.kt`):
   * Program code normalization and format validation (`^[A-Z0-9_-]{3,32}$`).
   * Deterministic program and enrollment state machine transition validation.
   * Multi-criteria enrollment eligibility checks (cross-tenant isolation, program active state, date validity, max participant capacity, affiliate active status, and affiliate Step 01 eligibility).
   * Cryptographic SHA-256 audit record and chain hash calculation.
3. **Persistence & Repositories**:
   * `AffiliateProgramRepository.kt` & `AffiliateProgramRepositoryImpl.kt`
   * `AffiliateProgramDataSource.kt`, `PostgresAffiliateProgramDataSource.kt`, `FakeAffiliateProgramDataSource.kt`
   * `PostgresRepositoryFactory.kt` updated with Step 02 factory methods.
4. **Service Layer** (`AffiliateProgramService.kt` & `AffiliateProgramServiceImpl.kt`):
   * Atomic operations with transactional audit logging and outbox event recording.
   * Cross-tenant and duplicate enrollment protection.
5. **Backend Use Cases & Router**:
   * `BackendUseCases.kt` Section 82: Complete integration of Program and Enrollment use cases with RBAC verification.
   * `BackendRouter.kt`: High-performance REST routing for:
     * `POST /api/v1/affiliate-programs` (Create Program)
     * `GET /api/v1/affiliate-programs/overview` (Governance Summary)
     * `GET /api/v1/affiliate-programs/code/{code}` (Lookup by Code)
     * `POST /api/v1/affiliate-programs/{id}/activate`
     * `POST /api/v1/affiliate-programs/{id}/pause`
     * `POST /api/v1/affiliate-programs/{id}/close`
     * `POST /api/v1/affiliate-programs/{id}/archive`
     * `GET /api/v1/affiliate-programs/{id}`
     * `PATCH /api/v1/affiliate-programs/{id}`
     * `GET /api/v1/affiliate-programs`
     * `POST /api/v1/affiliate-enrollments` (Enroll Affiliate)
     * `POST /api/v1/affiliate-enrollments/{id}/approve`
     * `POST /api/v1/affiliate-enrollments/{id}/reject`
     * `POST /api/v1/affiliate-enrollments/{id}/activate`
     * `POST /api/v1/affiliate-enrollments/{id}/suspend`
     * `POST /api/v1/affiliate-enrollments/{id}/resume`
     * `POST /api/v1/affiliate-enrollments/{id}/terminate`
     * `GET /api/v1/affiliate-enrollments/{id}/audit`
     * `GET /api/v1/affiliate-enrollments/{id}/handoff` (AI Handoff Contract)
     * `GET /api/v1/affiliate-enrollments/{id}`
     * `GET /api/v1/affiliate-enrollments`

---

### Android UI Command Center
* `AffiliateManagementUiState.kt`: Reactive UI state holding program governance statistics, filter queries, active counts, and selected contracts.
* `AffiliateManagementViewModel.kt`: Coroutine-driven ViewModel managing affiliate, program, and enrollment commands.
* `AffiliateManagementCommandCenterScreen.kt`:
  * Tabbed Navigation (`Overview`, `Affiliates`, `Programs`, `Enrollments`, `Pending`, `Active & Suspended`, `Profile & Eligibility`, `AI Handoff`).
  * Modal Dialogs for Program Creation and Affiliate Enrollment.
  * Operational confirmation dialogs for all lifecycle state mutations.

---

### Verification & Test Results
All unit and integration tests across `:core` and `:app` modules pass with 100% success:

* **Core Test Suites**:
  * `AffiliateProgramValidationEngineTest`: 6/6 tests passed.
  * `AffiliateProgramServiceTest`: 4/4 tests passed.
  * `AffiliateProgramSecurityEdgeTest`: 5/5 tests passed.
  * `AffiliateValidationEngineTest`: 6/6 tests passed.
  * `AffiliateServiceTest`: 4/4 tests passed.
  * `AffiliateSecurityEdgeTest`: 6/6 tests passed.
* **App Test Suites**:
  * `AffiliateManagementViewModelTest`: 5/5 tests passed.
* **Build Verification**:
  * `.\gradlew.bat test assembleDebug`: **BUILD SUCCESSFUL**.
