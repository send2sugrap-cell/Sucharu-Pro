# Identity Lifecycle Architecture & Security Specification

## 1. Overview
The **User Identity Lifecycle Foundation** for **Sucharu Pro — Commercial Printing ERP** provides a server-authoritative, multi-tenant, zero-trust identity system. It establishes canonical user identity management across all 7 platform roles (`GUEST`, `CUSTOMER`, `AFFILIATE`, `STAFF`, `MANAGER`, `ADMIN`, `AI_AGENT`), managing account creation, activation state transitions, profile metadata updates with optimistic concurrency control (OCC), password policy enforcement & history tracking, single-use contact verification tokens, and active session listing & remote device revocation.

---

## 2. Core Architectural Components

### 2.1 Database Schema (`V20260901__user_identity_lifecycle_and_verification_tables.sql`)
- **`user_profiles`**: Stores extended demographic metadata (`full_name`, `phone_number`, `avatar_url`, `language`, `timezone`, `preferred_currency`, `custom_attributes`) tied to `user_identities`. Enforces Optimistic Concurrency Control (OCC) via `version` column. Isolated per tenant via `tenant_id` and PostgreSQL Row-Level Security (RLS).
- **`user_verification_tokens`**: Stores high-entropy, single-use cryptographically secure tokens (`EMAIL_VERIFICATION`, `PHONE_VERIFICATION`, `PASSWORD_RESET`, `ACCOUNT_RECOVERY`, `TWO_FACTOR_SETUP`). Tokens store SHA-256 hashes to prevent token exposure in database breaches, enforcing consumption states (`PENDING`, `USED`, `EXPIRED`, `REVOKED`) and max-use counters.
- **`password_history`**: Tracks historical password hashes (`password_hash`, `salt_hex`, `algorithm`, `iterations`) per user to prevent password reuse across configurable lookback windows (default: last 5 passwords).

### 2.2 Domain & Data Layer Integration
- **`UserIdentity` & `UserProfile` Models**: High-level representations of user identities, profile states, account statuses, and verification tokens.
- **`UserIdentityService`**: Server-authoritative service handling identity lookups, account status transitions, profile OCC updates, single-use verification token generation & confirmation, password changes with historical checks, and session listing/revocation.
- **`PostgresAuthDataSources`**: RLS-enforced database persistence layer setting `app.current_project_id` on transaction connections to guarantee strict multi-tenant boundary separation.

---

## 3. Account Lifecycle State Machine

```
[ PENDING ] --------------> [ ACTIVE ] <--------------> [ SUSPENDED ]
     |                        |   |                         |
     v                        |   v                         v
[ DEACTIVATED ] <-------------+ [ LOCKED ] ---------> [ SECURITY_REVIEW ]
     |                            |                         |
     +----------------------------+-------------------------+
                                  |
                                  v
                             [ DELETED ] (Terminal)
```

### Valid Status Transitions
- `PENDING` -> `ACTIVE`, `DEACTIVATED`, `SECURITY_REVIEW`
- `ACTIVE` -> `LOCKED`, `SUSPENDED`, `DEACTIVATED`, `SECURITY_REVIEW`, `INACTIVE`, `DELETED`
- `SUSPENDED` -> `ACTIVE`, `DEACTIVATED`, `SECURITY_REVIEW`
- `LOCKED` -> `ACTIVE`, `SECURITY_REVIEW`, `DEACTIVATED`
- `SECURITY_REVIEW` -> `ACTIVE`, `SUSPENDED`, `DEACTIVATED`, `LOCKED`
- `DEACTIVATED` -> `ACTIVE` (Strict explicit re-activation)
- `DELETED` -> **Terminal State** (No transitions out allowed)

---

## 4. REST API Endpoint Mapping

| Endpoint | Method | Required Capability | Description |
|---|---|---|---|
| `/api/v1/auth/me` | GET | `READ_OWN_IDENTITY` | Fetches authenticated principal identity & account status |
| `/api/v1/auth/profile` | GET | `READ_OWN_PROFILE` | Fetches user profile metadata & current OCC version |
| `/api/v1/auth/profile` | PUT | `UPDATE_OWN_PROFILE` | Updates profile metadata enforcing OCC version matching |
| `/api/v1/auth/password/change` | POST | `CHANGE_OWN_PASSWORD` | Changes user password enforcing history lookback & session termination |
| `/api/v1/auth/verification/request` | POST | `VERIFY_OWN_CONTACT` | Generates a single-use verification token (`EMAIL`/`PHONE`/`PASSWORD_RESET`) |
| `/api/v1/auth/verification/confirm` | POST | None (Token Protected) | Consumes a single-use verification token |
| `/api/v1/auth/sessions` | GET | `READ_OWN_SESSIONS` | Lists active sessions for current user |
| `/api/v1/auth/sessions/{sessionId}/revoke` | DELETE | `REVOKE_OWN_SESSION` | Revokes a specific user session |
| `/api/v1/auth/sessions/revoke-all` | POST | `REVOKE_ALL_SESSIONS` | Revokes all active sessions except the caller's session |
| `/api/v1/admin/users/{userId}/status` | PUT | `ADMIN_MANAGE_USERS` | Administrative account status transition with audit reason |

---

## 5. Security & Isolation Verification
- **Multi-Tenant RLS**: Database queries automatically enforce `tenant_id = current_setting('app.current_project_id')`.
- **Anti-Spoofing Security**: User endpoints derive target `userId` directly from verified server-authoritative `AuthenticatedPrincipal` tokens, ignoring caller-supplied headers.
- **Audit Compliance**: All identity events (`ACCOUNT_STATUS_CHANGED`, `PROFILE_UPDATED`, `PASSWORD_CHANGED`, `SESSION_REVOKED`, `VERIFICATION_TOKEN_CREATED`, `VERIFICATION_TOKEN_CONSUMED`) generate structured, immutable security audit records.
