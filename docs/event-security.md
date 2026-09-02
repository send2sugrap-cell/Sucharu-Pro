# Domain Event Security Policy

## 1. Zero Secret Invariant
Domain events must never contain credentials, passwords, cryptographic keys, raw JWT/refresh tokens, session secrets, or full credit card PANs in either `payload` or `metadata`.

## 2. Server-Authoritative Actor & Tenant Scoping
- `actorId`, `actorType`, and `projectId` are always populated from the server-validated `AuthenticatedPrincipal` or `TenantContext`.
- Client-side attempts to inject or spoof actor identities or tenant projects are rejected during use-case execution.

## 3. Public API Protection
- There is no generic `POST /events` endpoint exposed to clients.
- Domain events can only be emitted internally by verified backend use cases after business invariants and authorization rules succeed.
