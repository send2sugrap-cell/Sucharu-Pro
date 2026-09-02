# MASTER ERP — MODULE 00 → MODULE 17 SECURITY & MULTI-TENANT AUDIT

## PostgreSQL Row-Level Security, RBAC Capability Matrix, SSRF, & Session Defense

---

### 1. PostgreSQL Row-Level Security (RLS) Audit

- **Tenant Isolation Policy**:
  - Every table in PostgreSQL across Modules 00 through 17 features:
    - `tenant_id VARCHAR(64) NOT NULL` (or `project_id`)
    - `ALTER TABLE <table_name> ENABLE ROW LEVEL SECURITY;`
    - `ALTER TABLE <table_name> FORCE ROW LEVEL SECURITY;`
    - `CREATE POLICY <table_name>_tenant_isolation ON <table_name> USING (tenant_id = CURRENT_SETTING('app.current_tenant', true)) WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));`
- **Server-Side Tenant Resolution**:
  - Client-supplied tenant IDs are strictly ignored in authorization queries; tenant context is established from the authenticated principal token during session verification.

---

### 2. Role-Based Access Control (RBAC) & Capability Matrix

| Role | Permitted Actions | Prohibited / Blocked Actions | Verification Status |
| :--- | :--- | :--- | :--- |
| **GUEST** | View Public Landing, Public Calculator (Preview) | Any internal dashboard, order creation, production mutation | **PASS (401/403)** |
| **CUSTOMER** | View own orders, create quotations, view invoices, request returns | Access other customers' data, access shop floor, job costing, admin ledger | **PASS (403)** |
| **AFFILIATE** | View own referral dashboard, commission statements | Access customer orders, internal production, ERP ledger | **PASS (403)** |
| **VENDOR** | View assigned RFQs, submit vendor quotes, submit delivery notes | Modify customer pricing, close production jobs, access ERP financials | **PASS (403)** |
| **STAFF** | View assigned work, record operator time, perform stage handovers, view QC | Financial ledger modification, job closure & master seal, system configuration | **PASS (403)** |
| **MANAGER** | Approve quotes, schedule production, release finished goods, close & seal jobs | System-level security configuration, full tenant deletion | **PASS (200/403)** |
| **ADMIN** | Full administrative oversight, system config, user management, financial governance | None (Authorized root) | **PASS (200)** |
| **AI_AGENT** | Read-only analytics, explainable recommendations via handoff contracts | Direct database write, bypassing transaction manager or RLS | **PASS (Enforced)** |

---

### 3. Security Invariants Verification Results

- **IDOR Defense**: All entity lookups require matching `tenant_id` and ownership context.
- **SSRF Defense**: External webhook invocations and callbacks are validated against strict IP and protocol whitelists.
- **Separation of Duties (SoD)**: Critical financial and production sign-offs (e.g. self-approval of quotes, unverified job closures) are blocked at the domain service level.
