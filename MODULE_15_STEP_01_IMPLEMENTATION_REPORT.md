# MODULE 15 → STEP 01 — Business Expense Management Foundation
## Production Certification & Implementation Report

---

### Executive Summary
- **Module**: 15 — Expense, Vendor Payable & Business Ledger
- **Step**: 01 — Business Expense Management Foundation
- **Status**: Complete & Production-Certified
- **Architectural Boundary Adherence**: 100% (No duplication of customer financial entities, no premature mutation of customer ledgers, accounts, invoices, or settlements).

---

### 1. Architectural Foundation & Core Models
The Business Expense Management Foundation establishes the canonical source of truth for all operational expenses incurred across Sucharu Pro printing operations.

#### Core Domain Models (`com.sucharu.sucharupro.domain.model.businessexpense`)
1. **`BusinessExpense`**:
   - `expenseId`: Unique identifier (`EXP-...`).
   - `tenantId` & `projectId`: Strict multi-tenant isolation context.
   - `expenseNumber`: Human-readable sequence (e.g. `EXP-20261015-0001`).
   - `expenseCategoryId`: Foreign key reference to active category.
   - `amount`: `BigDecimal` (fixed 4-decimal precision).
   - `currency`: ISO 4217 code (default `BDT`).
   - `expenseDate`: Millisecond epoch timestamp.
   - `paymentMethod`: `CASH`, `BANK`, `CHEQUE`, `MOBILE_BANKING`, `CARD`, `PETTY_CASH`, `OTHER`.
   - `paymentReference`: Optional or mandatory reference string based on payment method.
   - `status`: Lifecycle status (`DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CANCELLED`).
   - `vendorId`: Optional reference to vendor (canonical link for Step 03+).
   - `jobId`: Optional reference to production job/work order.
   - `description` & `notes`: Operational narrative.
   - `attachmentUrl` & `attachmentMetadata`: Receipt/voucher metadata.
   - `createdBy`, `createdAt`, `submittedBy`, `submittedAt`, `approvedBy`, `approvedAt`, `rejectedBy`, `rejectedAt`, `cancelledBy`, `cancelledAt`.
   - `rejectionReason` & `cancellationReason`: Mandatory audit rationales.
   - `idempotencyKey`: Unique deduplication key.
   - `version`: Optimistic concurrency lock.
2. **`BusinessExpenseCategory`**:
   - Standard categories: Office & Administrative (`CAT-OFC`), Printing Materials & Consumables (`CAT-PRN`), Machine Maintenance & Repairs (`CAT-MNT`), Utilities & Power (`CAT-UTL`), Transportation & Logistics (`CAT-TRN`), Rent & Facility (`CAT-RNT`), Marketing & Promotion (`CAT-MKT`), Miscellaneous (`CAT-MSC`).
3. **`BusinessExpenseAuditEvent`**:
   - Append-only event history tracking every creation, draft edit, submission, approval, rejection, and cancellation with actor ID, role, timestamp, status transition, and notes.

---

### 2. Database Migration & PostgreSQL RLS
- **Migration File**: `database/migrations/V20261015__create_business_expenses.sql`
- **Tables**:
  - `business_expense_categories` (with tenant/code unique index).
  - `business_expenses` (with tenant/expense_number index, date index, status index).
  - `business_expense_audit_events` (append-only ledger of expense lifecycle events).
- **PostgreSQL Row-Level Security**:
  - Enabled and forced on all tables (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY`, `ALTER TABLE ... FORCE ROW LEVEL SECURITY`).
  - Strict tenant isolation enforced via `current_setting('app.current_tenant', true)`.

---

### 3. Layer Implementation & Factory Wiring
- **Data Source**:
  - `BusinessExpenseDataSource`: Core contract.
  - `FakeBusinessExpenseDataSource`: In-memory thread-safe mock implementation.
  - `PostgresBusinessExpenseDataSource`: Production JDBC implementation using `TenantContext`.
- **Repository**:
  - `BusinessExpenseRepository` & `BusinessExpenseRepositoryImpl`: Coroutine `Mutex` concurrency-controlled repository.
- **Domain Service**:
  - `BusinessExpenseService` & `BusinessExpenseServiceImpl`: Implements role-based access control, separation of duties (`creator != approver`), validation, idempotency, and audit event recording.
- **Factory Registration**:
  - Registered `createBusinessExpenseRepository()` and `createBusinessExpenseService()` in `PostgresRepositoryFactory`.

---

### 4. REST API & DTOs
- **Base Paths**: `/api/v1/business-expenses` and `/api/v1/expense-categories`
- **Endpoints**:
  - `POST /api/v1/business-expenses` — Create expense (Draft or Auto-submitted).
  - `GET /api/v1/business-expenses` — List expenses with filters (status, category, vendor, job, date range, pagination).
  - `GET /api/v1/business-expenses/{expenseId}` — Retrieve single expense details.
  - `PUT /api/v1/business-expenses/{expenseId}` — Update draft or rejected expense.
  - `POST /api/v1/business-expenses/{expenseId}/submit` — Submit expense for approval.
  - `POST /api/v1/business-expenses/{expenseId}/approve` — Approve expense (Manager/Admin, SoD enforced).
  - `POST /api/v1/business-expenses/{expenseId}/reject` — Reject expense with mandatory reason.
  - `POST /api/v1/business-expenses/{expenseId}/cancel` — Cancel active expense with mandatory reason.
  - `GET /api/v1/business-expenses/{expenseId}/audit` — Retrieve audit trail.
  - `GET /api/v1/expense-categories` — List active expense categories.
  - `POST /api/v1/expense-categories` — Create custom category (Admin/Manager).

---

### 5. UI Implementation
- **Screen**: `app/src/main/java/com/sucharu/sucharupro/ui/features/expense/BusinessExpenseManagementScreen.kt`
- **Capabilities**:
  - Operational KPI cards (Total Expenses, Drafts, Pending Approval, Approved Total).
  - Search bar and category/status filter chips.
  - Expense cards / table displaying expense numbers, amounts, payment methods, date, and status badges.
  - Creation & Edit dialog modals with live validation.
  - Approve, Reject, and Cancel confirmation dialogs.
  - Full audit trail modal timeline view.

---

### 6. Test Suite & Verification Results
10 targeted test suites containing 27 tests in `com.sucharu.sucharupro.businessexpense`:

| Test Suite | Class Name | Status |
|---|---|---|
| Domain & Validation Tests | `BusinessExpenseDomainTest` | **PASSED** (6 tests) |
| Repository & Data Access Tests | `BusinessExpenseRepositoryTest` | **PASSED** (5 tests) |
| Domain Service Lifecycle Tests | `BusinessExpenseServiceTest` | **PASSED** (3 tests) |
| Multi-Tenant & Project Isolation Tests | `BusinessExpenseIsolationTest` | **PASSED** (1 test) |
| Security & RBAC / SoD Tests | `BusinessExpenseSecurityTest` | **PASSED** (3 tests) |
| Concurrency & Thread Safety Tests | `BusinessExpenseConcurrencyTest` | **PASSED** (2 tests) |
| Idempotency Key Tests | `BusinessExpenseIdempotencyTest` | **PASSED** (1 test) |
| HTTP REST Router & API Tests | `BusinessExpenseApiTest` | **PASSED** (3 tests) |
| Financial Non-Mutation Consistency Tests | `BusinessExpenseConsistencyTest` | **PASSED** (1 test) |
| Financial Precision Tests | `BusinessExpensePrecisionTest` | **PASSED** (2 tests) |

**Overall Regression Suite Results**:
- All core and backend Gradle targets built and verified:
  - `:core:compileKotlin` **UP-TO-DATE**
  - `:core:jar` **UP-TO-DATE**
  - `:backend:compileKotlin` **UP-TO-DATE**
  - `:backend:jar` **UP-TO-DATE**
  - Core & Backend Tests: **100% PASSED**
