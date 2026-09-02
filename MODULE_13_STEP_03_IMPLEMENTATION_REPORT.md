# MODULE 13 — STEP 03: VENDOR RFQ / QUOTATION & BID MANAGEMENT
## IMPLEMENTATION & VERIFICATION REPORT

**Status**: COMPLETED & VERIFIED  
**Module**: Module 13 — Vendor Portal  
**Step**: Step 03 — Vendor RFQ / Quotation & Bid Management  
**Timestamp**: 2026-08-26  

---

### 1. Scope and Objective
Module 13 Step 03 establishes the canonical foundation for Request for Quotation (RFQ), Vendor Invitation, Vendor Quotation/Bid, Revision, Clarification, Bid Evaluation, Comparison Snapshot, and Awarding. It provides a secure workspace for vendors to bid on eligible opportunities while enabling internal staff to manage the RFQ lifecycle with robust Separation of Duties.

---

### 2. Architectural Adherence & Decisions
1. **Canonical Re-use & Zero Duplication**:
   - Reuses canonical `Vendor` (Module 12 Step 01), `VendorCapability` (Module 12 Step 02), and `VendorServiceRate` (Module 12 Step 03).
   - No parallel Vendor Master created.
2. **Controlled Immutability**:
   - Submitted quotations become immutable upon submission.
   - Subsequent changes create monotonic revisions (`VendorQuotationRevision`) with full historical item and financial snapshots preserved.
3. **Multi-Tenancy & Project Isolation**:
   - Every RFQ, Invitation, Quotation, Revision, Evaluation, and Audit event is scoped by `tenantId` and `projectId`.
   - PostgreSQL Row Level Security (RLS) is enabled and forced across all tables.
4. **Vendor Isolation**:
   - Vendor users can access ONLY RFQs for which their vendor has received an active `VendorRfqInvitation`.
   - Effective `vendorId` is derived exclusively from the authenticated security context.
5. **Separation of Duties (SoD)**:
   - Vendor users/quotation submitters cannot evaluate their own quotation.
   - Evaluator cannot approve their own evaluation scorecard.
   - Vendor users/quotation submitters cannot award the RFQ.
6. **Deterministic & Zero-Safe Calculation**:
   - All financial amounts use the `@JvmInline value class Money(val amount: BigDecimal)`.
   - Line totals, subtotals, taxes, discounts, and weighted scorecard sums use deterministic `RoundingMode.HALF_UP` arithmetic.
7. **Future-Proof Integration**:
   - RFQ award records the award decision and selected quotation reference without prematurely generating POs, preserving clean integration boundaries for Module 12 Step 05 / Module 13 Step 04.

---

### 3. State Machines

#### RFQ State Machine
- `DRAFT` → `PUBLISHED`, `CANCELLED`
- `PUBLISHED` → `OPEN`, `CANCELLED`, `EXPIRED`
- `OPEN` → `CLOSING`, `CLOSED`, `CANCELLED`, `EXPIRED`
- `CLOSING` → `CLOSED`, `CANCELLED`, `EXPIRED`
- `CLOSED` → `EVALUATION`, `CANCELLED`
- `EVALUATION` → `AWARDED`, `CANCELLED`

#### Invitation State Machine
- `INVITED` → `VIEWED`, `ACKNOWLEDGED`, `DECLINED`, `RESPONDED`, `EXPIRED`
- `VIEWED` → `ACKNOWLEDGED`, `DECLINED`, `RESPONDED`, `EXPIRED`
- `ACKNOWLEDGED` → `DECLINED`, `RESPONDED`, `EXPIRED`
- `RESPONDED` → `WITHDRAWN`, `EXPIRED`

#### Quotation State Machine
- `DRAFT` → `IN_PROGRESS`, `SUBMITTED`, `WITHDRAWN`
- `IN_PROGRESS` → `SUBMITTED`, `WITHDRAWN`
- `SUBMITTED` → `UNDER_REVIEW`, `REVISION_REQUESTED`, `WITHDRAWN`, `ACCEPTED`, `REJECTED`, `EXPIRED`
- `UNDER_REVIEW` → `REVISION_REQUESTED`, `WITHDRAWN`, `ACCEPTED`, `REJECTED`, `EXPIRED`
- `REVISION_REQUESTED` → `REVISED`, `WITHDRAWN`, `EXPIRED`
- `REVISED` → `UNDER_REVIEW`, `REVISION_REQUESTED`, `WITHDRAWN`, `ACCEPTED`, `REJECTED`, `EXPIRED`

---

### 4. Database Schema & Migration
**Migration**: `V20260926__create_vendor_rfq_quotation_bid_management.sql`
- `vendor_rfqs` (with RLS enabled & forced)
- `vendor_rfq_items` (with RLS enabled & forced)
- `vendor_rfq_invitations` (with RLS enabled & forced)
- `vendor_quotations` (with RLS enabled & forced)
- `vendor_quotation_items` (with RLS enabled & forced)
- `vendor_quotation_revisions` (with RLS enabled & forced)
- `vendor_rfq_clarifications` (with RLS enabled & forced)
- `vendor_rfq_evaluations` (with RLS enabled & forced)
- `vendor_rfq_audit_events` (with RLS enabled & forced)

---

### 5. API Endpoints Mounted in BackendRouter
- Internal Management:
  - `POST /api/v1/vendor-rfqs`
  - `GET /api/v1/vendor-rfqs`
  - `GET /api/v1/vendor-rfqs/{rfqId}`
  - `POST /api/v1/vendor-rfqs/{rfqId}/publish`
  - `POST /api/v1/vendor-rfqs/{rfqId}/open`
  - `POST /api/v1/vendor-rfqs/{rfqId}/close`
  - `POST /api/v1/vendor-rfqs/{rfqId}/cancel`
  - `POST /api/v1/vendor-rfqs/{rfqId}/extend-deadline`
  - `POST /api/v1/vendor-rfqs/{rfqId}/invitations`
  - `GET /api/v1/vendor-rfqs/{rfqId}/invitations`
  - `GET /api/v1/vendor-rfqs/{rfqId}/quotations`
  - `GET /api/v1/vendor-rfqs/{rfqId}/comparison`
  - `POST /api/v1/vendor-rfqs/{rfqId}/evaluate`
  - `POST /api/v1/vendor-rfqs/{rfqId}/evaluations/{id}/approve`
  - `POST /api/v1/vendor-rfqs/{rfqId}/award`
  - `GET /api/v1/vendor-rfqs/{rfqId}/audit`
  - `GET /api/v1/vendor-rfqs/{rfqId}/clarifications`
  - `POST /api/v1/vendor-rfqs/{rfqId}/clarifications/{id}/answer`
- Vendor Portal:
  - `GET /api/v1/vendor-portal/rfqs`
  - `GET /api/v1/vendor-portal/rfqs/{rfqId}`
  - `POST /api/v1/vendor-portal/rfqs/{rfqId}/acknowledge`
  - `POST /api/v1/vendor-portal/rfqs/{rfqId}/decline`
  - `POST /api/v1/vendor-portal/rfqs/{rfqId}/quotations`
  - `GET /api/v1/vendor-portal/quotations/{quotationId}`
  - `PUT /api/v1/vendor-portal/quotations/{quotationId}`
  - `POST /api/v1/vendor-portal/quotations/{quotationId}/submit`
  - `POST /api/v1/vendor-portal/quotations/{quotationId}/withdraw`
  - `POST /api/v1/vendor-portal/quotations/{quotationId}/request-revision`
  - `POST /api/v1/vendor-portal/quotations/{quotationId}/revise`
  - `GET /api/v1/vendor-portal/quotations/{quotationId}/revisions`
  - `POST /api/v1/vendor-portal/rfqs/{rfqId}/clarifications`
  - `GET /api/v1/vendor-portal/rfqs/{rfqId}/clarifications`

---

### 6. Jetpack Compose UI Screens
- `VendorPortalRfqListScreen.kt`: Browse available RFQs with status filtering and deadline indicators.
- `VendorPortalRfqDetailsScreen.kt`: Detailed view of RFQ requirements, specs, and acknowledgment/decline actions.
- `VendorPortalQuotationEditorScreen.kt`: Line-item pricing, real-time total calculations, draft saving, and formal submission.
- `VendorPortalQuotationDetailsScreen.kt`: Review submitted bids, request revisions, or withdraw.
- `VendorPortalQuotationRevisionScreen.kt`: Monotonic revision audit history.
- `VendorPortalClarificationScreen.kt`: RFQ question and answer forum.
- `VendorRfqComparisonScreen.kt`: Multi-vendor bid comparison matrix.
- `VendorRfqEvaluationScreen.kt`: Weighted criterion scorecard evaluation and award execution.

---

### 7. Verification Summary
- **Total Backend Tests**: 395
- **Passed**: 395
- **Failed**: 0
- **Skipped**: 0
- **Regression**: 0
- **JAR Output**: `backend/build/libs/sucharu-server.jar` (24,203,163 bytes)
