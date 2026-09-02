# SUCHARU PRO — POSTGRESQL PRODUCTION ARCHITECTURE & OPERATIONS GUIDE
**Document**: `docs/postgresql-production.md`  
**Stage**: `INFRA-02 → STEP 03`  
**Classification**: Production Infrastructure Architecture  

---

## 1. Architecture Overview

Sucharu Pro employs a hardened, multi-tenant PostgreSQL 16 persistence platform designed for commercial printing operations. The architecture strictly isolates application business domains from raw infrastructure while enforcing row-level security, deferred double-entry bookkeeping invariants, and optimistic concurrency.

```
+-----------------------------------------------------------------------------------+
|                        SUCHARU PRO UNIFIED PRODUCT ECOSYSTEM                      |
| (Guest -> Customer Portal -> Affiliate -> Staff -> Manager -> Admin Workspace)    |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                            DOMAIN REPOSITORY INTERFACES                           |
|       (CustomerRepository, OrderRepository, FinancialTransactionRepository)       |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                        POSTGRESQL PERSISTENCE DATA SOURCES                        |
|   (PostgresCustomerDataSource, PostgresOrderDataSource, PostgresReturnDataSource) |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                 TRANSACTION & CONNECTION POOL MANAGEMENT (HikariCP)                |
|      DefaultPostgresTransactionManager  <-->  PostgresConnectionProvider          |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                           POSTGRESQL 16 DATABASE ENGINE                           |
|  - Flyway Migrations: V1__canonical_schema.sql -> V20260824_indexes.sql           |
|  - Row Level Security: FORCE RLS using app.current_project_id session config      |
|  - Tenant Composite Foreign Keys & Unique Constraints                             |
|  - Deferred Journal Balance Trigger: Sum(Debit) = Sum(Credit) at COMMIT           |
+-----------------------------------------------------------------------------------+
```

---

## 2. 12-Factor Environment Variables

| Variable | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `DATABASE_HOST` | String | `localhost` | Hostname or DNS address of PostgreSQL cluster |
| `DATABASE_PORT` | Integer | `5432` | PostgreSQL listener port |
| `DATABASE_NAME` | String | `sucharu_pro_db` | Database catalog name |
| `DATABASE_USER` | String | `sucharu_app` | Least-privilege runtime database user |
| `DATABASE_PASSWORD` | String | *Required* | Injected runtime secret |
| `DATABASE_SSL_MODE` | String | `require` | SSL mode (`require` or `verify-full` in prod) |
| `DATABASE_POOL_SIZE` | Integer | `20` | Maximum connection pool capacity |
| `DATABASE_MIN_IDLE` | Integer | `5` | Minimum warm idle pooled connections |
| `DATABASE_CONN_TIMEOUT_MS` | Long | `30000` | Connection lease acquisition timeout (ms) |
| `DATABASE_IDLE_TIMEOUT_MS` | Long | `600000` | Idle connection eviction limit (ms) |
| `DATABASE_MAX_LIFETIME_MS` | Long | `1800000` | Connection retirement lifetime (ms) |
| `DATABASE_LEAK_DETECTION_MS`| Long | `10000` | Unreturned connection leak detector (ms) |

---

## 3. Multi-Tenant Isolation & Row-Level Security

- **Session Context**: Every leased connection executes `SELECT set_config('app.current_project_id', ?, true)`. The 3rd parameter `is_local = true` binds the tenant context strictly to the transaction boundary.
- **Connection Return Cleansing**: Upon returning connection to the pool, the pool explicitly resets session variables via `SELECT set_config('app.current_project_id', '', false)` to prevent cross-tenant state leakage during connection reuse.
- **Database RLS**: Every application table enforces `FORCE ROW LEVEL SECURITY` with `USING (project_id = current_setting('app.current_project_id', true))` policy.

---

## 4. Flyway Migration Strategy

- **Location**: `app/src/main/resources/db/migration/`
- **History Table**: `flyway_schema_history`
- **Strict Immutability Rule**: Never edit or overwrite already applied migrations. Any post-deployment schema additions must be introduced as a new sequential version (e.g. `V20260824__...sql`).
- **Dedicated Migration Job**: In production, schema migrations are executed by a dedicated deployment CI/CD job prior to rolling out new application pods.

---

## 5. Health Probes & Observability

- **Liveness (`/health/liveness`)**: Verifies the internal application process and connection pool are alive.
- **Readiness (`/health/readiness`)**: Executes `SELECT current_database()` with timeout protection (default 3000ms).
- **Metrics**: Tracks active connections, idle connections, total acquisitions, acquisition failure count, and transaction latency.
- **Structured Logging**: Emits structured JSON events (`DATABASE_CONNECTION_INITIALIZED`, `DATABASE_TRANSACTION_COMMIT`, `DATABASE_TRANSACTION_ROLLBACK`, `DATABASE_MIGRATION_COMPLETED`) with credentials and PII automatically sanitized.

---

## 6. Backup, Restore & Disaster Recovery

- **Logical Backups**: Generated daily using `pg_dump -Fc --no-owner --no-privileges` and encrypted at rest with AES-256.
- **Point-in-Time Recovery (PITR)**: Enabled via Continuous WAL Archiving to Cloud Object Storage.
- **Recovery Objectives**:
  - **RPO (Recovery Point Objective)**: $\le 5\text{ minutes}$ (via streaming WAL).
  - **RTO (Recovery Time Objective)**: $\le 30\text{ minutes}$ (via automated container restore).
