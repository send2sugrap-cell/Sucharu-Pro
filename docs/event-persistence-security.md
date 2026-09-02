# Sucharu Pro Event Persistence Security & Machine Principal Boundary

## 1. Zero Trust Multi-Tenant Isolation
- **Row-Level Security (RLS)**: Enforced across all four tables:
  - `event_store`
  - `event_outbox`
  - `event_dead_letters`
  - `event_processing_records`
- **Application Validation**: Every repository operation verifies that the caller's `TenantContext.projectId` matches the entity's `projectId`. Any cross-tenant attempt throws `IllegalArgumentException` or `SecurityException`.

## 2. Server-Authoritative Identity
- Principal information (`actorId`, `actorType`, `principalType`, `role`) is extracted directly from the verified session token and server security context.
- Client payloads cannot spoof actor identity or permissions.

## 3. AI Agent Machine Principal Boundaries
- AI Agents are identified as `PrincipalType.AI_AGENT`.
- AI Agents can emit domain events via authenticated commands (e.g., job scheduling recommendations), but are strictly forbidden from:
  1. Reading or modifying raw dead-letter quarantine entries (`event_dead_letters`).
  2. Mutating audit or authentication security streams.
  3. Bypassing tenant RLS boundaries.
