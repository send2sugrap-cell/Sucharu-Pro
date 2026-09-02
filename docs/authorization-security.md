# SUCHARU PRO — INFRA-03 STEP 02: AUTHORIZATION SECURITY & DEFENSE MECHANISMS

## 1. Anti-Spoofing Defenses

Client HTTP requests can provide arbitrary headers (e.g., `X-Role: ADMIN`, `X-Customer-Id: CUST-999`, `X-Permissions: ADMIN_ALL`). Sucharu Pro enforces **zero-trust anti-spoofing**:
- ALL client-supplied headers or DTO claims representing authorization attributes (`role`, `permissions`, `customerId`, `affiliateId`, `projectId`) are strictly **ignored**.
- Authorization is evaluated exclusively against `AuthenticatedPrincipal` extracted from the server-signed cryptographically verified JWT access token.

---

## 2. Multi-Tenant Session Isolation

1. **Context Matching**: `context.targetProjectId` must equal `principal.projectId`. Any mismatch returns `TENANT_MISMATCH` denial.
2. **PostgreSQL RLS Integration**: When executing database operations via `PostgresRepositoryFactory` / `DefaultPostgresTransactionManager`, the active PostgreSQL connection executes `SET LOCAL app.current_tenant_id = '<projectId>'`.

---

## 3. Horizontal & Vertical Escalation Prevention

- **Horizontal Data Isolation**: Customers (`CUST-100`) cannot read or mutate data belonging to other customers (`CUST-200`). Affiliates (`AFF-100`) cannot view commission streams of other affiliates (`AFF-200`). Staff/Admin bypass is granted only when `principal.isStaff == true`.
- **Vertical Privilege Escalation**: Role hierarchy is strictly enforced via capability matrices. Customer roles attempting staff or admin capabilities trigger immediate `ForbiddenException` and record a high-severity security audit log.

---

## 4. Security Audit Logging

All authorization evaluations (`ALLOW` or `DENY`) are recorded asynchronously to `AuthAuditDataSource` via `BackendAuthorizationService`:
- `eventType`: `AUTHORIZATION_ALLOWED` or `AUTHORIZATION_DENIED`
- `projectId`: Active tenant ID
- `userId`: Principal ID
- `ipAddress`, `userAgent`: Context metadata
- `details`: Detailed JSON string containing `capability`, `action`, `reasonCode`, `targetCustomerId`, `targetAffiliateId`, and `isAiAgent`.

---

## 5. Standardized Error Sanitization

Security exceptions never expose internal database errors or authorization implementation details:
- `ForbiddenException`: `"Access denied: <sanitized message>"` (HTTP 403)
- `UnauthenticatedException`: `"Authentication required."` (HTTP 401)
