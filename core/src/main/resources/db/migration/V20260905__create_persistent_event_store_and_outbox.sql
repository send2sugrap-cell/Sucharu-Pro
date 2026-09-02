-- =====================================================================
-- SUCHARU PRO — MIGRATION V20260905
-- PRODUCTION-GRADE PERSISTENT EVENT STORE, TRANSACTIONAL OUTBOX,
-- IDEMPOTENCY & DEAD-LETTER QUARANTINE SCHEMA (INFRA-04 STEP 02)
-- =====================================================================

-- 1. PERSISTENT EVENT STORE (Append-Only Immutable Event Log)
CREATE TABLE IF NOT EXISTS event_store (
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    event_id VARCHAR(36) NOT NULL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    aggregate_version BIGINT NOT NULL DEFAULT 1,
    actor_type VARCHAR(30) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    principal_type VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    causation_id VARCHAR(64),
    request_id VARCHAR(64),
    source VARCHAR(100) NOT NULL DEFAULT 'sucharu-pro-backend',
    payload JSONB NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_store_tenant_aggregate ON event_store(project_id, aggregate_type, aggregate_id, aggregate_version ASC);
CREATE INDEX IF NOT EXISTS idx_event_store_correlation ON event_store(project_id, correlation_id);
CREATE INDEX IF NOT EXISTS idx_event_store_causation ON event_store(project_id, causation_id) WHERE causation_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_event_store_event_type ON event_store(project_id, event_type);
CREATE INDEX IF NOT EXISTS idx_event_store_occurred_at ON event_store(project_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_event_store_actor ON event_store(project_id, actor_id);

-- 2. TRANSACTIONAL EVENT OUTBOX
CREATE TABLE IF NOT EXISTS event_outbox (
    outbox_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    event_id VARCHAR(36) NOT NULL UNIQUE REFERENCES event_store(event_id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    aggregate_version BIGINT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'RETRY_SCHEDULED', 'DEAD_LETTER', 'CANCELLED')),
    attempt_count INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    claimed_by_worker VARCHAR(100),
    claimed_at TIMESTAMPTZ,
    lease_expires_at TIMESTAMPTZ,
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_attempt_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    last_error_code VARCHAR(50),
    last_error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    payload JSONB NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(64) NOT NULL,
    causation_id VARCHAR(64),
    request_id VARCHAR(64),
    actor_type VARCHAR(30) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    principal_type VARCHAR(30) NOT NULL,
    source VARCHAR(100) NOT NULL DEFAULT 'sucharu-pro-backend'
);

CREATE INDEX IF NOT EXISTS idx_event_outbox_claim ON event_outbox(project_id, status, available_at) WHERE status IN ('PENDING', 'RETRY_SCHEDULED');
CREATE INDEX IF NOT EXISTS idx_event_outbox_lease_expiry ON event_outbox(status, lease_expires_at) WHERE status = 'PROCESSING';
CREATE INDEX IF NOT EXISTS idx_event_outbox_aggregate ON event_outbox(project_id, aggregate_type, aggregate_id, aggregate_version);

-- 3. PERSISTENT IDEMPOTENCY PROCESSING RECORDS
CREATE TABLE IF NOT EXISTS event_processing_records (
    processing_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    event_id VARCHAR(36) NOT NULL,
    consumer_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PROCESSED' CHECK (status IN ('IN_FLIGHT', 'PROCESSED', 'FAILED', 'SKIPPED')),
    failure_reason TEXT,
    execution_duration_ms BIGINT NOT NULL DEFAULT 0,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, consumer_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_event_processing_lookup ON event_processing_records(project_id, event_id, consumer_id);

-- 4. DEAD-LETTER QUARANTINE TABLE
CREATE TABLE IF NOT EXISTS event_dead_letters (
    dead_letter_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    outbox_id VARCHAR(36) NOT NULL REFERENCES event_outbox(outbox_id) ON DELETE CASCADE,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    failure_classification VARCHAR(30) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    attempt_count INT NOT NULL,
    first_failure_at TIMESTAMPTZ NOT NULL,
    final_failure_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(64) NOT NULL,
    causation_id VARCHAR(64),
    request_id VARCHAR(64),
    replayed_at TIMESTAMPTZ,
    replayed_by VARCHAR(100),
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dead_letters_tenant_type ON event_dead_letters(project_id, event_type, is_resolved);
CREATE INDEX IF NOT EXISTS idx_dead_letters_correlation ON event_dead_letters(project_id, correlation_id);

-- 5. ROW-LEVEL SECURITY ENFORCEMENT
ALTER TABLE event_store ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_processing_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_dead_letters ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_event_store ON event_store;
CREATE POLICY tenant_isolation_event_store ON event_store
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));

DROP POLICY IF EXISTS tenant_isolation_event_outbox ON event_outbox;
CREATE POLICY tenant_isolation_event_outbox ON event_outbox
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));

DROP POLICY IF EXISTS tenant_isolation_event_processing_records ON event_processing_records;
CREATE POLICY tenant_isolation_event_processing_records ON event_processing_records
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));

DROP POLICY IF EXISTS tenant_isolation_event_dead_letters ON event_dead_letters;
CREATE POLICY tenant_isolation_event_dead_letters ON event_dead_letters
    FOR ALL
    USING (project_id = current_setting('app.current_project_id', true));
