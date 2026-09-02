# Sucharu Pro Transactional Outbox Pattern

## 1. Dual-Write Problem Elimination
When a business operation mutates state (e.g. creating an order, updating inventory) and emits an event, performing two separate network calls (database update + message queue publish) risks inconsistency if one fails.

The **Transactional Outbox Pattern** eliminates dual-writes:
1. Business mutation SQL and outbox INSERT SQL execute within the exact same PostgreSQL database transaction (`inTransaction(tenantContext)`).
2. If the transaction commits, both the business data and the outbox event exist.
3. If the transaction rolls back, zero outbox records remain.

## 2. PostgreSQL Outbox Schema (`event_outbox`)
```sql
CREATE TABLE event_outbox (
    outbox_id VARCHAR(64) NOT NULL PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    event_version VARCHAR(32) NOT NULL DEFAULT 'v1',
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    aggregate_version BIGINT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    claimed_by_worker VARCHAR(64),
    claimed_at TIMESTAMPTZ,
    lease_expires_at TIMESTAMPTZ,
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_attempt_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    last_error_code VARCHAR(64),
    last_error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    payload JSONB NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(64) NOT NULL,
    causation_id VARCHAR(64),
    request_id VARCHAR(64),
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    principal_type VARCHAR(32) NOT NULL,
    source VARCHAR(128) NOT NULL
);
```

## 3. High-Concurrency Claiming Mechanics
- **`SELECT ... FOR UPDATE SKIP LOCKED`**: Enables multiple background workers to query and claim pending records concurrently without blocking or deadlocks.
- **Worker Lease**: Worker sets `claimed_by_worker = workerId` and `lease_expires_at = NOW() + leaseDuration`.
- **Automatic Lease Expiry Recovery**: If a worker node crashes or loses connectivity, `recoverExpiredLeases()` detects expired leases and resets their status back to `RETRY_SCHEDULED` so other active workers can proceed.
