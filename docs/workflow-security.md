# Workflow Security & Multi-Tenant Isolation

## Defense-in-Depth Security Controls

### 1. PostgreSQL 16 Row-Level Security (RLS)
All 12 workflow orchestration and approval tables enforce tenant isolation policies:
```sql
CREATE POLICY tenant_isolation_policy ON workflow_instances
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', TRUE))
    WITH CHECK (project_id = CURRENT_SETTING('app.current_project_id', TRUE));
```

### 2. Server-Authoritative Identity Validation
- All workflow operations (`startWorkflow`, `advanceWorkflow`, `decideApproval`, `escalate`, `replayDeadLetter`) strictly require a validated `TenantContext` and `AuthenticatedPrincipal`.
- Principals cannot impersonate other users or tenants.

### 3. Separation of Duties Enforcement
- When `allowSelfApproval == false`, the engine ensures `request.requesterId != principal.userId`.

### 4. Machine Principal Containment
- `PrincipalType.AI_AGENT` cannot approve or decide workflows.
- Non-whitelisted workflow definitions cannot be invoked by machine principals.
- High-impact workflows require cryptographic human approval metadata.

### 5. Zero-Trust Audit Logging
- Audited actions (`WORKFLOW_STARTED`, `STEP_COMPLETED`, `APPROVAL_DECIDED`, `COMPENSATION_EXECUTED`, `DEAD_LETTER_REPLAYED`) record timestamps, actor IDs, roles, principal types, and IP addresses with automatic credential scrubbing.
