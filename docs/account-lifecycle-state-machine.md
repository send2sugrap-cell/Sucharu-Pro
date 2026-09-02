# Account Lifecycle State Machine Specification

## 1. Overview
The **Account Status State Machine** governs account state transitions, access capabilities, administrative controls, and system behavior across the lifecycle of a user identity in **Sucharu Pro — Commercial Printing ERP**.

---

## 2. State Definitions

| Account Status | Description | Access Rights |
|---|---|---|
| `PENDING` | Account created, awaiting primary email/contact verification | Limited / Restricted |
| `ACTIVE` | Account fully verified and active | Full Role Capabilities |
| `LOCKED` | Temporarily locked due to repeated failed logins or security alerts | Blocked (Authentication Denied) |
| `SUSPENDED` | Account suspended administratively for policy/billing violations | Blocked (Authentication Denied) |
| `DEACTIVATED` | Account deactivated voluntarily by user or staff | Blocked (Authentication Denied) |
| `SECURITY_REVIEW` | Account flagged for manual security review due to anomalous activity | Blocked (Authentication Denied) |
| `INACTIVE` | Account inactive due to prolonged idle period | Blocked (Re-authentication required) |
| `DELETED` | Account permanently deleted/anonymized (Terminal state) | Blocked (Permanent) |

---

## 3. Transition Rules Matrix

| From State | Allowed Target States | Disallowed Target States | Triggers / Authority |
|---|---|---|---|
| `PENDING` | `ACTIVE`, `DEACTIVATED`, `SECURITY_REVIEW` | `LOCKED`, `SUSPENDED`, `INACTIVE` | Email verification, Admin action |
| `ACTIVE` | `LOCKED`, `SUSPENDED`, `DEACTIVATED`, `SECURITY_REVIEW`, `INACTIVE`, `DELETED` | `PENDING` | Brute force, Admin action, User self-deactivation |
| `LOCKED` | `ACTIVE`, `SECURITY_REVIEW`, `DEACTIVATED` | `PENDING`, `SUSPENDED`, `INACTIVE` | Password reset, Admin unlock |
| `SUSPENDED` | `ACTIVE`, `DEACTIVATED`, `SECURITY_REVIEW` | `PENDING`, `LOCKED`, `INACTIVE` | Admin resolution |
| `DEACTIVATED` | `ACTIVE` | All others | Explicit Admin / User reactivation flow |
| `SECURITY_REVIEW` | `ACTIVE`, `SUSPENDED`, `DEACTIVATED`, `LOCKED` | `PENDING`, `INACTIVE` | Security team clearance |
| `INACTIVE` | `ACTIVE`, `DEACTIVATED` | All others | Re-verification / Admin action |
| `DELETED` | None (**Terminal State**) | All | Permanent |

---

## 4. Enforcement & Session Invalidation Logic
- **`AccountStatus.isValidTransitionTo(targetStatus)`**: Kotlin domain logic method that validates any proposed status change before execution.
- If invalid transition is attempted, `UserIdentityService` throws `IllegalArgumentException("Invalid account status transition from $currentStatus to $targetStatus.")`.
- Any transition into a non-active state (`LOCKED`, `SUSPENDED`, `DEACTIVATED`, `SECURITY_REVIEW`, `DELETED`) automatically executes `authDataSource.revokeAllSessions(userId)` to instantly kill all live tokens.
