# INFRA-04 Step 02: Production-Grade Persistent Event Store & Transactional Outbox Foundation

## Executive Summary
This document specifies the architecture, data models, dispatch mechanics, and security controls for the **Sucharu Pro Production-Grade Persistent Event Store and Transactional Outbox** (INFRA-04 Step 02). 

Building directly upon the verified domain contracts of INFRA-04 Step 01, this infrastructure guarantees:
1. **Zero Dual-Write Inconsistency**: Business mutations and transactional outbox entries are committed atomically in the same PostgreSQL transaction.
2. **Deterministic Append-Only Event Store**: Immutable log of domain event envelopes with Row-Level Security (RLS) tenant isolation.
3. **High-Throughput Outbox Dispatch**: Concurrent, non-blocking worker polling using `SELECT ... FOR UPDATE SKIP LOCKED` and lease expiration recovery.
4. **Resilient Retry & Quarantine**: Exponential backoff with full jitter, deterministic classification (TRANSIENT vs NON_RETRYABLE), and dead-letter quarantine repository.
5. **Decoupled Integration**: Safe, asynchronous event consumption boundaries for AI Agents, n8n webhook automation, SMS/WhatsApp notifications, and real-time frontend streaming.

---

## 1. Database Schema & Migration (`V20260905__create_persistent_event_store_and_outbox.sql`)

### 1.1 `event_store` Table
Append-only log of domain event envelopes.
- `project_id VARCHAR(64) NOT NULL`: Partition/tenant identifier.
- `event_id VARCHAR(64) NOT NULL`: Globally unique UUIDv4 identifier.
- `event_type VARCHAR(128) NOT NULL`: Canonical event type enum name.
- `event_version VARCHAR(32) NOT NULL DEFAULT 'v1'`: Schema version.
- `occurred_at TIMESTAMPTZ NOT NULL`: Event creation timestamp.
- `published_at TIMESTAMPTZ NOT NULL`: Event persistence timestamp.
- `aggregate_type VARCHAR(64) NOT NULL`: Business aggregate category (e.g. `ORDER`, `PRODUCTION`).
- `aggregate_id VARCHAR(64) NOT NULL`: Aggregate identifier (e.g. `ORD-2026-001`).
- `aggregate_version BIGINT NOT NULL DEFAULT 1`: Monotonically increasing aggregate stream version.
- `actor_type VARCHAR(32) NOT NULL`: Principal category (`HUMAN`, `AI_AGENT`, `SYSTEM`, etc.).
- `actor_id VARCHAR(64) NOT NULL`: Server-authoritative actor ID.
- `principal_type VARCHAR(32) NOT NULL`: Authenticated principal type.
- `correlationId VARCHAR(64) NOT NULL`: Root causal trace ID.
- `causationId VARCHAR(64)`: Immediate parent event/command ID.
- `requestId VARCHAR(64)`: Inbound HTTP/client request ID.
- `source VARCHAR(128) NOT NULL`: Emitting component.
- `payload JSONB NOT NULL`: Fully serialized immutable domain event payload.
- `metadata JSONB NOT NULL DEFAULT '{}'::jsonb`: Contextual headers.
- **Constraints & Indexes**:
  - `PRIMARY KEY (project_id, event_id)`
  - `UNIQUE (project_id, aggregate_type, aggregate_id, aggregate_version)` (guarantees strict stream ordering)
  - `CREATE INDEX idx_event_store_correlation ON event_store(project_id, correlation_id);`
  - `CREATE INDEX idx_event_store_event_type ON event_store(project_id, event_type, occurred_at);`

### 1.2 `event_outbox` Table
Staged publication table for guaranteed at-least-once delivery.
- `outbox_id VARCHAR(64) NOT NULL PRIMARY KEY`: Unique outbox row UUID.
- `status VARCHAR(32) NOT NULL`: State machine status (`PENDING`, `PROCESSING`, `PUBLISHED`, `RETRY_SCHEDULED`, `DEAD_LETTER`, `CANCELLED`).
- `attempt_count INT NOT NULL DEFAULT 0`: Number of dispatch attempts.
- `claimed_by_worker VARCHAR(64)`: ID of active dispatcher worker.
- `claimed_at TIMESTAMPTZ`: Timestamp when worker claimed lease.
- `lease_expires_at TIMESTAMPTZ`: Timestamp when worker lease expires.
- `available_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`: Earliest timestamp for next dispatch.
- **Indexes**:
  - `CREATE INDEX idx_event_outbox_claim ON event_outbox(project_id, status, available_at, aggregate_version, created_at);`

### 1.3 `event_dead_letters` Table
Quarantine storage for non-retryable and permanently failed events.
- `dead_letter_id VARCHAR(64) NOT NULL PRIMARY KEY`
- `project_id VARCHAR(64) NOT NULL`
- `outbox_id VARCHAR(64) NOT NULL`
- `event_id VARCHAR(64) NOT NULL`
- `failure_classification VARCHAR(32) NOT NULL` (`TRANSIENT`, `NON_RETRYABLE`, `CORRUPT_PAYLOAD`, `UNSUPPORTED_VERSION`, `DOWNSTREAM_UNAVAILABLE`)
- `error_code VARCHAR(64)`
- `error_message TEXT`
- `attempt_count INT NOT NULL`
- `first_failure_at TIMESTAMPTZ NOT NULL`
- `final_failure_at TIMESTAMPTZ NOT NULL`
- `is_resolved BOOLEAN NOT NULL DEFAULT FALSE`
- `replayed_at TIMESTAMPTZ`
- `replayed_by VARCHAR(64)`

### 1.4 `event_processing_records` Table
Consumer idempotency and duplicate deduplication store.
- `processing_id VARCHAR(64) NOT NULL PRIMARY KEY`
- `project_id VARCHAR(64) NOT NULL`
- `event_id VARCHAR(64) NOT NULL`
- `consumer_id VARCHAR(64) NOT NULL`
- `status VARCHAR(32) NOT NULL` (`PROCESSING`, `PROCESSED`, `FAILED`, `DEAD_LETTERED`, `IGNORED_DUPLICATE`)
- `processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
- **Constraint**: `UNIQUE (project_id, consumer_id, event_id)`

---

## 2. Row-Level Security (RLS) Isolation
Every table (`event_store`, `event_outbox`, `event_dead_letters`, `event_processing_records`) enforces multi-tenant RLS:
```sql
ALTER TABLE event_store ENABLE ROW LEVEL SECURITY;
CREATE POLICY event_store_tenant_isolation ON event_store
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));
```
Cross-tenant access is rejected at both the application layer (`TenantContext` verification) and the database engine layer.

---

## 3. Outbox State Machine & Worker Claiming
The outbox record lifecycle adheres strictly to the following transition graph:

```
          ┌─────────────┐
          │   PENDING   │
          └──────┬──────┘
                 │ (claimPendingRecords with SKIP LOCKED)
                 ▼
          ┌─────────────┐
          │ PROCESSING  │
          └──┬───┬───┬──┘
             │   │   │
  ┌──────────┘   │   └──────────┐
  │ (Success)    │ (Retryable)  │ (Permanent / Max Retries)
  ▼              ▼              ▼
┌───────────┐ ┌───────────────┐ ┌─────────────┐
│ PUBLISHED │ │RETRY_SCHEDULED│ │ DEAD_LETTER │
└───────────┘ └───────┬───────┘ └──────┬──────┘
 (Terminal)           │                │
                      │ (available_at) │ (Manual Replay)
                      └───────►────────┘
                              │
                              ▼
                       (re-claims to PROCESSING)
```

### Worker Claiming SQL
```sql
SELECT outbox_id FROM event_outbox
WHERE project_id = ? 
  AND (
    (status IN ('PENDING', 'RETRY_SCHEDULED') AND available_at <= NOW())
    OR (status = 'PROCESSING' AND lease_expires_at IS NOT NULL AND lease_expires_at < NOW())
  )
ORDER BY aggregate_version ASC, created_at ASC
LIMIT ?
FOR UPDATE SKIP LOCKED
```
Workers never block each other, and dead workers automatically release their leases for recovery.

---

## 4. Exponential Backoff & Jitter
Retries calculate delay using:
$$\text{Delay} = \min(\text{maxDelayMs}, \text{baseDelayMs} \times 2^{\text{attempt}-1}) \times (1 - \text{jitterFactor} + \text{random} \times 2 \times \text{jitterFactor})$$
- Default Base: 1,000 ms
- Multiplier: 2.0
- Max Delay: 300,000 ms (5 mins)
- Jitter Factor: 0.20 (±20% spread)
- Max Attempts: 5

---

## 5. Security & Machine Principal Boundary
- **AI Agent Principal Isolation**: AI agents run as machine principals (`PrincipalType.AI_AGENT`) and can only emit commands; they cannot directly read internal dead-letter quarantine tables or tamper with audit event logs.
- **Server Authoritative Identity**: All event headers (`projectId`, `actorId`, `role`, `principalType`) originate strictly from verified server-side security context (`AuthenticatedPrincipal` / `TenantContext`).
