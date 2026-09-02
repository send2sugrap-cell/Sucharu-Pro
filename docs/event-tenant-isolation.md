# Domain Event Multi-Tenant Isolation

## 1. Strict Tenant Scoping
In Sucharu Pro, all events belong to a single authoritative tenant (`projectId`), except for explicit system-wide maintenance notices.

## 2. Invariants
- `EventStore` operations require a valid `TenantContext`.
- Event ingestion verifies that `envelope.projectId == tenantContext.projectId`.
- Event retrieval queries strictly partition by `projectId` ensuring Tenant A can never view, query, or receive events from Tenant B.
