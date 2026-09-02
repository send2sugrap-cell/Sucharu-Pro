# INFRA-03 STEP 05 — UNIFIED AUTHENTICATION ENTRY & APP SHELL ARCHITECTURE

## Executive Summary
This document establishes the production-grade client-side authentication integration layer for **Sucharu Pro — Commercial Printing ERP**. It bridges the server-authoritative authentication & security infrastructure (INFRA-03 Steps 01–04) with the unified mobile/desktop Jetpack Compose application shell.

---

## Architectural Principles & Core Decisions

### 1. Single Unified Brand Experience
Sucharu Pro maintains a single unified application shell (`Sucharu Graphics Commercial Printing ERP`). The client app binary does NOT separate into distinct apps (`CustomerApp`, `StaffApp`, etc.). Instead, the app adapts its workspace views dynamically based on the server-authoritative `AuthenticatedPrincipal`.

### 2. Public Browsing Without Mandatory Login
Unauthenticated users can freely browse public enterprise services, offset/digital printing products, custom packaging solutions, corporate gift items, company info, and preview the Public AI Assistant. Authentication is required only when performing protected actions (e.g. placing orders, requesting quotes, viewing account history).

### 3. Server-Authoritative Identity & Authority
Client state machine (`AppEntryState`) and workspace routing (`PostLoginRouter`) never trust local role strings or stored preferences. Authority is derived exclusively from signed short-lived JWT access tokens validated against the backend `AuthenticatedPrincipal` and PostgreSQL Row-Level Security (RLS).

---

## Client Entry State Machine (`AppEntryState`)

| State | Trigger | Action / View |
| :--- | :--- | :--- |
| `INITIALIZING` | App Launch | Restore session, attempt `getMyProfile()`. |
| `PUBLIC` | No valid session / Logout | Display `PublicExperienceView` showcase. |
| `AUTHENTICATING` | Login / Register Submit | Show loading indicator, await backend API response. |
| `AUTHENTICATED` | Success `getMyProfile()` & `ACTIVE` | Render role-specific workspace (`AuthenticatedWorkspaceContainer`). |
| `VERIFICATION_REQUIRED` | Account status `PENDING` | Present `VerificationScreen` (Email/Phone OTP). |
| `SESSION_EXPIRED` | Refresh token failure / Revocation | Show session expired banner & prompt Sign In. |
| `ACCOUNT_UNAVAILABLE` | Account `LOCKED` / `SUSPENDED` | Display sanitized status alert without security leakage. |
| `RECOVERY_FLOW` | Password recovery requested | Present password reset confirmation form. |

---

## Single-Flight Token Refresh & 401 Interceptor

To prevent token rotation race conditions and request storms:
1. `AuthenticationSessionManager` utilizes Kotlin `Mutex` (`refreshMutex`) to wrap `refreshSession()`.
2. Exactly one coroutine executes backend `/api/v1/auth/refresh`.
3. Upon successful refresh, both `accessToken` and rotated `refreshToken` are atomically updated in `ISecureSessionStore`.
4. `executeWith401Retry` catches HTTP 401 `UNAUTHENTICATED`, invokes `refreshSession()`, and retries the original API call exactly once.

---

## Role-Aware Workspace Routing & Security Boundaries

```
[ App Launch ] -> [ AppEntryState.Initializing ]
                       |
             +---------+---------+
             |                   |
    [ No Local Session ]   [ Valid Token ]
             |                   |
             v                   v
     [ PUBLIC VIEW ]     [ GET /api/v1/auth/me ]
                                 |
                     +-----------+-----------+
                     |                       |
             [ Status ACTIVE ]       [ Status PENDING/LOCKED ]
                     |                       |
                     v                       v
         [ PostLoginRouter ]         [ Status UI Screen ]
                     |
     +---------------+---------------+---------------+---------------+
     |               |               |               |               |
[ CUSTOMER ]   [ AFFILIATE ]     [ STAFF ]      [ MANAGER ]      [ ADMIN ]
  Workspace      Workspace       Workspace       Workspace       Workspace
```

- **`AI_AGENT` Security Boundary**: Machine principals (`PrincipalType.AI_AGENT` / `UserRole.AI_AGENT`) are explicitly blocked from human interactive dashboards and routed to public fallback.
- **Ownership Isolation**: RLS policies enforce `projectId` and `userId`/`customerId` boundaries.
