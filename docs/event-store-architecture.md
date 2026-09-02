# Sucharu Pro Event Store Architecture

## 1. Overview
The **Sucharu Pro Persistent Event Store** (`event_store`) is an append-only, tenant-partitioned ledger that records all domain events occurring across the printing ERP ecosystem.

## 2. Immutability & Append-Only Invariants
1. **Zero Updates & Zero Deletes**: Event records once inserted cannot be modified or deleted.
2. **Server-Authoritative Context**: `projectId`, `actorId`, `role`, and `principalType` are populated from verified server-side security context (`TenantContext` / `AuthenticatedPrincipal`), not client requests.
3. **Deterministic Aggregate Stream Ordering**: Enforced via PostgreSQL composite unique constraint:
   ```sql
   UNIQUE (project_id, aggregate_type, aggregate_id, aggregate_version)
   ```
4. **Tenant Isolation with RLS**: PostgreSQL Row-Level Security ensures that tenant queries cannot leak cross-tenant events even if application code omits WHERE clauses.

## 3. Class Structure & Contracts
- **Contract Interface**: `com.sucharu.sucharupro.domain.event.store.EventStore`
- **Persistent Implementation**: `com.sucharu.sucharupro.data.event.postgres.PostgresEventStore`
- **Database Table**: `event_store`

## 4. Query Capabilities
- `getById(eventId, tenantContext)`: O(1) single-event retrieval.
- `getByAggregate(aggregateType, aggregateId, tenantContext)`: Chronologically ordered aggregate event stream for event sourcing or audit replay.
- `getByCorrelationId(correlationId, tenantContext)`: Distributed causal trace retrieval across multi-step business transactions.
- `getByEventType(eventType, tenantContext)`: Time-series query for specific domain lifecycle signals.
