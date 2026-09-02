# INFRA-03 STEP 06 — Unified Navigation Architecture

## Status: VERIFIED & PRODUCTION CERTIFIED

---

## 1. Overview

INFRA-03 Step 06 completes the **One Unified Sucharu Graphics Application** shell by delivering:

- A canonical, type-safe `AppDestination` hierarchy for all roles
- A server-authoritative `DeepLinkAuthorizer` with anti-spoofing protection
- Capability-aware UI filtering (`CapabilityAwareNavigation`)
- A stateful `AppNavigationManager` coordinating session and routing
- Role-specific workspace shells under a single branded `SucharuGraphicsAppShell`
- 33 passing unit tests covering navigation security, deep link authorization, back-stack safety, and role routing

---

## 2. Architecture Principles

| Principle | Implementation |
|---|---|
| One App, Many Roles | `SucharuGraphicsAppShell` dispatches to role-specific workspace shells |
| Server-Authoritative | `DeepLinkAuthorizer` ignores all client-supplied hints |
| Zero Trust Client | `AuthenticatedPrincipal` from server JWT is the sole authority |
| AI Agent Isolation | `UserRole.AI_AGENT` is hard-blocked from all interactive human destinations |
| Public-First | Guest browsing of products, services, portfolio, and public AI is supported without login |
| Capability-Gated UI | `CapabilityAwareNavigation` hides menu items the principal lacks capability for |

---

## 3. Navigation Hierarchy

```
AppDestination (sealed class)
├── Public          → Home, Services, Products, Portfolio, Offers, AI, Auth flows
├── Customer        → Dashboard, Orders, Invoices, Payments, Delivery, Returns, AI
├── Affiliate       → Dashboard, Referrals, Commission, Payouts, Performance, AI
├── Staff           → AssignedWork, Production, QC, Inventory, Delivery
├── Manager         → Operations, Approvals, Production, Finance, Reports
├── Admin           → SystemControl, Users, Roles, Security, Config, Finance, Reports
└── Security        → VerificationRequired, AccountUnavailable, SessionExpired, Forbidden, NotFound
```

---

## 4. Deep Link Authorization Flow

```
Incoming Deep Link Route
        │
        ▼
 Sanitize & Normalize Route
        │
        ▼
 ┌──────────────────────┐
 │  Is route public?    │──YES──▶ Return public destination (no auth required)
 └──────────────────────┘
        │ NO
        ▼
 ┌──────────────────────┐
 │  principal == null?  │──YES──▶ Redirect to Login
 └──────────────────────┘
        │ NO
        ▼
 ┌──────────────────────────────┐
 │  principal.role == AI_AGENT? │──YES──▶ Return Forbidden
 └──────────────────────────────┘
        │ NO
        ▼
 Anti-Spoofing Firewall
 (clientSuppliedRole / userId / projectId are DISCARDED)
        │
        ▼
 Role-Based Route Evaluation
 ├── customer/* → role must be CUSTOMER or ADMIN
 ├── affiliate/* → role must be AFFILIATE or ADMIN
 ├── staff/*    → must have STAFF_READ_ORDERS capability
 ├── manager/*  → must have MANAGER_VIEW_OPERATIONAL_ANALYTICS capability
 └── admin/*    → role must be ADMIN
        │
        ▼
 Ownership Check (where applicable)
 └── e.g. customer/orders/{id} verified against principal ownership
        │
        ▼
 Return Authorized AppDestination or Security.Forbidden
```

---

## 5. Workspace Shells

| Role | Shell | Entry Destination |
|---|---|---|
| GUEST | `PublicExperienceView` | `AppDestination.Public.Home` |
| CUSTOMER | `CustomerWorkspaceShell` | `AppDestination.Customer.Home` |
| AFFILIATE | `AffiliateWorkspaceShell` | `AppDestination.Affiliate.Home` |
| STAFF | `InternalWorkspaceShells.StaffShell` | `AppDestination.Staff.AssignedWork` |
| MANAGER | `InternalWorkspaceShells.ManagerShell` | `AppDestination.Manager.Operations` |
| ADMIN | `InternalWorkspaceShells.AdminShell` | `AppDestination.Admin.FullAdministration` |
| AI_AGENT | **BLOCKED** — `Security.Forbidden` | N/A |

---

## 6. Test Coverage

| Test Class | Scope |
|---|---|
| `AppDestinationTest` | Route strings, public flags, capability assignments |
| `DeepLinkAuthorizerTest` | Authorization decisions across all roles |
| `DeepLinkAntiSpoofingTest` | Client-supplied hint rejection |
| `AiAgentNavigationBoundaryTest` | AI_AGENT machine principal blocking |
| `GuestNavigationTest` | Unauthenticated route protection |
| `RoleWorkspaceRoutingTest` | Default workspace entry per role |
| `CapabilityAwareNavigationTest` | Visibility filtering by capability |
| `LogoutBackStackSecurityTest` | Post-logout back navigation prevention |
| `AppNavigationManagerTest` | Session state machine transitions |

**Total: 33 tests — all PASSED**
