# MODULE 20 — STEP 01: AFFILIATE MANAGEMENT FOUNDATION
## Comprehensive Engineering & Production Certification Report

**Module**: Module 20 — Affiliate Management & Partner Ecosystem  
**Step**: Step 01 — Affiliate Management Foundation  
**Version**: `1.0.0`  
**Status**: `COMPLETED & CERTIFIED`  
**Compliance Standard**: Zero Mocks in Production Code, Deterministic Hashing, Multi-Tenant Row-Level Security, Strict Separation of Concerns, Anti-Duplication Identity Reference Architecture.

---

## 1. Executive Summary

Module 20 Step 01 establishes the **canonical, server-authoritative Affiliate Management Foundation** for Sucharu Pro. It introduces the authoritative domain entity (`AffiliateProfile`), the state machine governing onboarding and partner lifecycle, row-level multi-tenant isolation, cryptographic audit logging with SHA-256 hash chaining, transactional outbox events, and an enterprise Jetpack Compose Command Center.

This foundational step deliberately isolates affiliate identity, status, eligibility, and governance without coupling or mutating future tracking, attribution, commission calculation, wallet management, or payout engines (which remain strictly reserved for subsequent steps).

---

## 2. Key Architecture Invariants & Design Principles

```
+-----------------------------------------------------------------------------------+
|                        SUCHARU PRO — MODULE 20 STEP 01                            |
|                        AFFILIATE MANAGEMENT FOUNDATION                            |
+-----------------------------------------------------------------------------------+
                                          |
                +-------------------------+-------------------------+
                |                                                   |
                v                                                   v
   +--------------------------+                        +--------------------------+
   |  Authoritative Entities  |                        |   Non-Owned Entities     |
   |--------------------------|                        |--------------------------|
   | • AffiliateProfile       |                        | • User Identity Master   |
   | • AffiliateStatus FSM    |                        | • Customer CRM Profile   |
   | • AffiliateType          |                        | • Order / Commercial Doc |
   | • OnboardingState        |                        | • Financial Ledger       |
   | • VerificationState      |                        | • Commission Engine      |
   | • AffiliateEligibility   |                        | • Wallet Balances        |
   | • Append-Only Audit Trail|                        | • Payout Distributions   |
   +--------------------------+                        +--------------------------+
```

1. **Anti-Duplication Identity Reference Architecture**:
   - The affiliate record references the primary authentication `userId` (and optional `customerId` for customer-referrals) via foreign key references without duplicating user accounts.
2. **Deterministic Alphanumeric Slug Standardization**:
   - Affiliate referral codes are strictly normalized uppercase alphanumeric slugs (`^[A-Z0-9_-]{3,32}$`).
3. **Multi-Tenant Row-Level Security (RLS)**:
   - PostgreSQL RLS enabled and forced across all four tables (`affiliates`, `affiliate_eligibility_records`, `affiliate_audit_records`, `affiliate_outbox_events`) using `tenant_id = current_setting('app.current_project_id', true)`.
4. **Append-Only Cryptographic Audit Ledger**:
   - Every creation, profile update, status transition, agreement acceptance, and eligibility evaluation appends an immutable audit record with individual SHA-256 record hashes and genesis-anchored chain hashes.
5. **AI Handoff Boundary (`Module20Step01AffiliateHandoffContract`)**:
   - Explicit read-only governance handoff with allowed actions (`INSPECT_AFFILIATE_PROFILE`, `QUERY_ELIGIBILITY`, `READ_AUDIT_LOGS`) and forbidden mutation actions (`ACTIVATE_AFFILIATE`, `SUSPEND_AFFILIATE`, `BYPASS_ROW_LEVEL_SECURITY`).

---

## 3. Database Schema & Migration Specification

Migration script: `V20261124__create_affiliate_management_foundation_tables.sql`

| Table Name | Description | Constraints & Indexes |
| :--- | :--- | :--- |
| `affiliates` | Primary affiliate entity storage | PK (`tenant_id`, `affiliate_id`), Unique (`tenant_id`, `user_id`), Unique (`tenant_id`, `affiliate_code`), FK to `tenants(tenant_id)` |
| `affiliate_eligibility_records` | Multi-dimensional eligibility evaluations | PK (`tenant_id`, `evaluation_id`), Index on (`tenant_id`, `affiliate_id`, `evaluated_at`) |
| `affiliate_audit_records` | Append-only immutable audit trail | PK (`tenant_id`, `audit_id`), SHA-256 `record_hash` & `chain_hash` |
| `affiliate_outbox_events` | Transactional event outbox | PK (`tenant_id`, `event_id`), Index on (`tenant_id`, `status`) |

---

## 4. REST Endpoints & Route Matrix

All endpoints require standard `Bearer <token>` authentication with tenant context extracted server-side:

| Method | Endpoint Route | Required Authority / RBAC | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/affiliates` | ADMIN, MANAGER, STAFF, AFFILIATE | Create affiliate profile |
| `GET` | `/api/v1/affiliates/me` | AFFILIATE | Retrieve authenticated user's affiliate profile |
| `GET` | `/api/v1/affiliates/overview` | ADMIN, MANAGER, STAFF, AI_AGENT | Get tenant aggregate governance summary |
| `GET` | `/api/v1/affiliates/code/{code}` | ADMIN, MANAGER, STAFF, AI_AGENT | Lookup affiliate by unique code |
| `GET` | `/api/v1/affiliates/{id}` | ADMIN, MANAGER, STAFF, AFFILIATE (own), AI_AGENT | Get affiliate profile details |
| `PATCH` | `/api/v1/affiliates/{id}` | ADMIN, MANAGER, STAFF, AFFILIATE (own) | Update profile details |
| `POST` | `/api/v1/affiliates/{id}/activate` | ADMIN, MANAGER | Activate pending/inactive affiliate |
| `POST` | `/api/v1/affiliates/{id}/suspend` | ADMIN, MANAGER | Suspend affiliate |
| `POST` | `/api/v1/affiliates/{id}/reactivate`| ADMIN, MANAGER | Reactivate suspended affiliate |
| `POST` | `/api/v1/affiliates/{id}/reject` | ADMIN, MANAGER | Reject pending affiliate application |
| `POST` | `/api/v1/affiliates/{id}/terminate`| ADMIN | Terminate affiliate partnership |
| `POST` | `/api/v1/affiliates/{id}/agreement`| ADMIN, MANAGER, STAFF, AFFILIATE (own) | Record agreement terms acceptance |
| `GET` | `/api/v1/affiliates/{id}/eligibility`| ADMIN, MANAGER, STAFF, AFFILIATE (own), AI_AGENT | Evaluate and get eligibility status |
| `GET` | `/api/v1/affiliates/{id}/audit` | ADMIN, MANAGER, STAFF, AFFILIATE (own), AI_AGENT | Retrieve append-only audit trail |
| `GET` | `/api/v1/affiliates/{id}/handoff` | ADMIN, MANAGER, STAFF, AFFILIATE (own), AI_AGENT | Export sealed AI Handoff Contract |
| `GET` | `/api/v1/affiliates` | ADMIN, MANAGER, STAFF, AI_AGENT | Query/Filter directory list |

---

## 5. UI Command Center Features

The Jetpack Compose Command Center (`AffiliateManagementCommandCenterScreen.kt`) features:
1. **Dark Navy & Cyan Glow Aesthetics** (`#0A0E17`, `#131B2E`, `#00E5FF`).
2. **6 Interactive Navigation Tabs**:
   - **Overview**: Real-time KPI cards and category distribution breakdown.
   - **Directory**: Full searchable list with instant code/name filtering.
   - **Pending Approvals**: Streamlined triage desk with one-click approve/reject actions.
   - **Active & Suspended**: Governance console for suspension, reactivation, and termination.
   - **Profile & Eligibility**: Deep diagnostic view showing 5-point eligibility verification (Identity, Agreement, Account Active, Tax Compliance, Business Verification) alongside the audit trail.
   - **AI Handoff**: Visual display of sealed handoff contract, SHA-256 seal, and guardrail constraints.

---

## 6. Verification & Test Suite Matrix

| Test Suite | Module | Test Count | Status |
| :--- | :--- | :--- | :--- |
| `AffiliateValidationEngineTest` | `:core` | 6 Tests | `PASSED` |
| `AffiliateServiceTest` | `:core` | 4 Tests | `PASSED` |
| `AffiliateSecurityEdgeTest` | `:core` | 6 Tests | `PASSED` |
| `AffiliateManagementViewModelTest`| `:app` | 4 Tests | `PASSED` |
| **Full Regression Suite (`test`)** | All Modules | 500+ Tests | `PASSED (100%)` |
| **Android Build (`assembleDebug`)**| `:app` | Build | `SUCCESSFUL` |

---

## 7. Next Step Readiness

Module 20 Step 01 is complete and certified. The affiliate domain is ready for downstream tracking, attribution, and commission integrations in Step 02+.
