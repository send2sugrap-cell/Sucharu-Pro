# SUCHARU PRO — BACKEND API SECURITY SPECIFICATION
**Document**: `docs/api-security.md`  
**Stage**: `INFRA-02 → STEP 04`  
**Classification**: Backend API Security Architecture  

---

## 1. Threat Model & Defense-in-Depth

Sucharu Pro enforces a multi-layered security architecture:

1. **Transport Layer**: TLS 1.3 encryption for all external communication.
2. **Network Layer**: PostgreSQL listener is restricted to the internal container network; no direct public internet exposure.
3. **Authentication Layer**: Cryptographically signed bearer tokens containing `userId`, `projectId`, and `role`.
4. **Authorization / RBAC Layer**: Capability-based permissions checking and strict resource ownership verification.
5. **Application / Use Case Layer**: Business invariants and domain validation before persistence.
6. **Persistence Layer**: Parameterized PreparedStatements with 100% parameter injection defense.
7. **Database Engine Layer**: PostgreSQL `FORCE ROW LEVEL SECURITY` with `set_config('app.current_project_id', ?, true)` session scoping.

---

## 2. Standard Error Response Model

Errors are sanitized before being returned to clients to prevent reconnaissance:

```json
{
  "success": false,
  "errorCode": "FORBIDDEN",
  "message": "Access denied to the requested resource.",
  "correlationId": "req-98234-a1",
  "timestamp": 1755940000000
}
```

### Prohibited Response Leaks:
- Raw SQL query text or parameter dumps.
- Database error stack traces (e.g. `org.postgresql.util.PSQLException`).
- Internal database hostnames, ports, user accounts, or catalog names.
- File system paths.

---

## 3. Standard Error Code Mapping

| HTTP Status | Error Code | Description |
| :--- | :--- | :--- |
| `400 Bad Request` | `VALIDATION_ERROR` | Request payload failed schema or business validation |
| `401 Unauthorized` | `UNAUTHENTICATED` | Missing, expired, or invalid bearer token |
| `403 Forbidden` | `FORBIDDEN` | Principal lacks required role or ownership permission |
| `404 Not Found` | `NOT_FOUND` | Target entity does not exist in authenticated tenant |
| `409 Conflict` | `OPTIMISTIC_CONCURRENCY_CONFLICT` | Resource version mismatch (stale update rejected) |
| `409 Conflict` | `IDEMPOTENCY_CONFLICT` | Duplicate request with mismatched payload hash |
| `429 Too Many Requests` | `RATE_LIMITED` | Request rate exceeded allowed tier |
| `500 Internal Error` | `INTERNAL_ERROR` | Unexpected server failure (details logged securely) |
| `503 Unavailable` | `DATABASE_UNAVAILABLE` | Database health probe failure or connection timeout |
