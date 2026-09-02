# SUCHARU PRO — BACKEND API ARCHITECTURE & INTEGRATION SPECIFICATION
**Document**: `docs/backend-api-architecture.md`  
**Stage**: `INFRA-02 → STEP 04`  
**Classification**: Authoritative Backend API Architecture  

---

## 1. Executive Architecture Summary

Sucharu Pro implements a secure, modular, and cloud-native client-server boundary. The Android application acts as a presentation client communicating exclusively over HTTPS REST APIs with the authoritative backend persistence runtime. Direct PostgreSQL connections from the Android client or bundling of JDBC credentials into the mobile APK are strictly prohibited.

```
+-----------------------------------------------------------------------------------+
|                        SUCHARU PRO CLIENT APPLICATION                             |
|  (Android Client / Unified Sucharu Graphics App / Customer / Affiliate / Staff)   |
+-----------------------------------------------------------------------------------+
                                         |
                                         | HTTPS (Bearer Token Auth)
                                         v
+-----------------------------------------------------------------------------------+
|                          SECURE BACKEND API BOUNDARY                              |
|  - Rate Limiting & Abuse Prevention                                               |
|  - Authentication & Token Verification -> AuthenticatedPrincipal                  |
|  - Authorization & Capability-Based RBAC (GUEST, CUSTOMER, AFFILIATE, STAFF)      |
|  - Authoritative Server-Side Tenant Resolution (Zero Client Tenant Spoofing)       |
|  - Request Validation & Idempotency Key Scoping                                   |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                         APPLICATION & USE CASE SERVICES                           |
|  (CustomerUseCases, OrderUseCases, FinanceUseCases, AffiliateUseCases)            |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                           DOMAIN REPOSITORY BOUNDARY                              |
|  (CustomerRepository, OrderRepository, FinancialTransactionRepository)           |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                 TRANSACTION MANAGEMENT & POSTGRESQL DATASOURCES                   |
|  - DefaultPostgresTransactionManager                                              |
|  - Session Context Binding: SELECT set_config('app.current_project_id', ?, true)  |
|  - Session Cleansing on Pooled Connection Release                                 |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                           POSTGRESQL 16 DATABASE ENGINE                           |
|  - Multi-Tenant Row Level Security (FORCE RLS)                                    |
|  - Tenant-Aware Composite Foreign Keys & Unique Constraints                       |
|  - Deferred Journal Balance Enforcement (Debit = Credit at COMMIT)                |
|  - Optimistic Concurrency Control (OCC version column)                            |
+-----------------------------------------------------------------------------------+
```

---

## 2. API Versioning & URL Topology

All backend API routes follow the `/api/v1/` versioned namespace:

| Category | Route Pattern | Authentication | Authorized Roles |
| :--- | :--- | :--- | :--- |
| **System Health** | `GET /health/live` | None | Public |
| **System Health** | `GET /health/ready` | None | Public |
| **Public API** | `GET /api/v1/public/*` | None | Guest / Public |
| **Auth API** | `POST /api/v1/auth/login` | None | Public |
| **Auth API** | `GET /api/v1/auth/me` | Bearer Token | Authenticated (Any) |
| **Customer API** | `GET /api/v1/customer/profile` | Bearer Token | `CUSTOMER`, `STAFF`, `ADMIN` |
| **Customer API** | `GET /api/v1/customer/orders` | Bearer Token | `CUSTOMER`, `STAFF`, `ADMIN` |
| **Customer API** | `GET /api/v1/customer/orders/{orderId}` | Bearer Token | `CUSTOMER`, `STAFF`, `ADMIN` (Own order) |
| **Customer API** | `GET /api/v1/customer/invoices` | Bearer Token | `CUSTOMER`, `STAFF`, `ADMIN` |
| **Customer API** | `GET /api/v1/customer/delivery-status` | Bearer Token | `CUSTOMER`, `STAFF`, `ADMIN` |
| **Customer API** | `POST /api/v1/customer/orders` | Bearer Token | `CUSTOMER`, `STAFF`, `ADMIN` (Idempotent) |
| **Affiliate API**| `GET /api/v1/affiliate/profile` | Bearer Token | `AFFILIATE`, `STAFF`, `ADMIN` |
| **Affiliate API**| `GET /api/v1/affiliate/referrals` | Bearer Token | `AFFILIATE`, `STAFF`, `ADMIN` (Own) |
| **Affiliate API**| `GET /api/v1/affiliate/commission` | Bearer Token | `AFFILIATE`, `STAFF`, `ADMIN` (Own) |
| **Affiliate API**| `GET /api/v1/affiliate/payouts` | Bearer Token | `AFFILIATE`, `STAFF`, `ADMIN` (Own) |

---

## 3. Server-Authoritative Tenant & Identity Resolution

### A. Anti-Spoofing Rule
The client cannot supply a `projectId`, `userId`, or `role` in the request body/header to override authenticated server context.
The server verifies the bearer token, establishes the `AuthenticatedPrincipal`, and derives:
```kotlin
val tenantContext = TenantContext(principal.projectId)
```

### B. Data Ownership Enforcement
A customer can only query their own data:
```kotlin
if (principal.role == UserRole.CUSTOMER && requestedCustomerId != principal.userId) {
    throw ForbiddenException("Access denied: You cannot access data belonging to another customer.")
}
```
An affiliate can only query their own referral and commission data:
```kotlin
if (principal.role == UserRole.AFFILIATE && requestedAffiliateId != principal.userId) {
    throw ForbiddenException("Access denied: You cannot access data belonging to another affiliate.")
}
```

---

## 4. Transaction & Connection Pool Scoping

Every protected request delegates to `DefaultPostgresTransactionManager` using the authenticated `TenantContext`. Upon releasing the connection back to HikariCP, session variables are cleared to prevent context leakage across recycled pooled connections.
