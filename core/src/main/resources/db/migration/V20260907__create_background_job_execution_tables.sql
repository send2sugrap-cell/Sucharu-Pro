-- ============================================================
-- SUCHARU PRO — INFRA-04 STEP 04
-- Background Jobs, Executions, Schedules, Dependencies & Dead Letters with RLS
-- Migration: V20260907__create_background_job_execution_tables.sql
-- ============================================================

-- 1. Main background jobs table
CREATE TABLE IF NOT EXISTS background_jobs (
    job_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_type VARCHAR(64) NOT NULL,
    job_version VARCHAR(16) NOT NULL DEFAULT 'v1',
    trigger_type VARCHAR(32) NOT NULL,
    priority INT NOT NULL DEFAULT 3, -- 1=CRITICAL, 2=HIGH, 3=NORMAL, 4=LOW
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    claimed_by_worker VARCHAR(64),
    claimed_at TIMESTAMPTZ,
    lease_expires_at TIMESTAMPTZ,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(64) NOT NULL,
    causation_id VARCHAR(64),
    request_id VARCHAR(64),
    actor_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    actor_id VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    principal_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    source VARCHAR(128) NOT NULL DEFAULT 'sucharu-pro-backend',
    last_error_code VARCHAR(64),
    last_error_message TEXT,
    failure_classification VARCHAR(32),
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_background_jobs PRIMARY KEY (project_id, job_id)
);

-- Unique idempotency constraint per project
CREATE UNIQUE INDEX IF NOT EXISTS uq_background_jobs_idempotency
    ON background_jobs (project_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Index for concurrent worker claiming (SKIP LOCKED query optimization)
CREATE INDEX IF NOT EXISTS idx_background_jobs_claiming
    ON background_jobs (project_id, status, available_at, priority ASC, created_at ASC);

-- Index for correlation tracing
CREATE INDEX IF NOT EXISTS idx_background_jobs_correlation
    ON background_jobs (project_id, correlation_id);

-- Index for job type lookups
CREATE INDEX IF NOT EXISTS idx_background_jobs_type
    ON background_jobs (project_id, job_type);

-- 2. Immutable execution history table
CREATE TABLE IF NOT EXISTS job_executions (
    execution_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    worker_id VARCHAR(64) NOT NULL,
    attempt_number INT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    duration_ms BIGINT,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    error_message TEXT,
    failure_classification VARCHAR(32),
    output_metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_job_executions PRIMARY KEY (project_id, execution_id)
);

CREATE INDEX IF NOT EXISTS idx_job_executions_job
    ON job_executions (project_id, job_id, attempt_number DESC);

-- 3. Recurring schedules table
CREATE TABLE IF NOT EXISTS job_schedules (
    schedule_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_type VARCHAR(64) NOT NULL,
    cron_expression VARCHAR(64),
    fixed_interval_ms BIGINT,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Dhaka',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_run_at TIMESTAMPTZ,
    next_run_at TIMESTAMPTZ NOT NULL,
    schedule_version VARCHAR(16) NOT NULL DEFAULT 'v1',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_job_schedules PRIMARY KEY (project_id, schedule_id)
);

CREATE INDEX IF NOT EXISTS idx_job_schedules_polling
    ON job_schedules (project_id, is_enabled, next_run_at ASC);

-- 4. Job dependencies table (DAG workflows)
CREATE TABLE IF NOT EXISTS job_dependencies (
    dependency_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    depends_on_job_id VARCHAR(64) NOT NULL,
    required_status VARCHAR(32) NOT NULL DEFAULT 'SUCCEEDED',
    is_satisfied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_job_dependencies PRIMARY KEY (project_id, dependency_id),
    CONSTRAINT uq_job_dependency_pair UNIQUE (project_id, job_id, depends_on_job_id)
);

CREATE INDEX IF NOT EXISTS idx_job_dependencies_parent
    ON job_dependencies (project_id, depends_on_job_id);

CREATE INDEX IF NOT EXISTS idx_job_dependencies_child
    ON job_dependencies (project_id, job_id);

-- 5. Job dead-letter quarantine table
CREATE TABLE IF NOT EXISTS job_dead_letters (
    dead_letter_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    job_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    attempt_count INT NOT NULL,
    failure_classification VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    error_message TEXT,
    first_failure_at TIMESTAMPTZ NOT NULL,
    final_failure_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(64) NOT NULL,
    causation_id VARCHAR(64),
    request_id VARCHAR(64),
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    replayed_at TIMESTAMPTZ,
    replayed_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_job_dead_letters PRIMARY KEY (project_id, dead_letter_id)
);

CREATE INDEX IF NOT EXISTS idx_job_dead_letters_unresolved
    ON job_dead_letters (project_id, is_resolved, final_failure_at DESC);

-- Enable Multi-Tenant Row-Level Security (RLS) on all tables
ALTER TABLE background_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_dependencies ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_dead_letters ENABLE ROW LEVEL SECURITY;

CREATE POLICY background_jobs_tenant_isolation ON background_jobs
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

CREATE POLICY job_executions_tenant_isolation ON job_executions
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

CREATE POLICY job_schedules_tenant_isolation ON job_schedules
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

CREATE POLICY job_dependencies_tenant_isolation ON job_dependencies
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));

CREATE POLICY job_dead_letters_tenant_isolation ON job_dead_letters
    FOR ALL
    USING (project_id = CURRENT_SETTING('app.current_project_id', true));
