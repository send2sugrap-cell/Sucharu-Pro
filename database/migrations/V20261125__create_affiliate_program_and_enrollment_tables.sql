-- ============================================================================
-- Flyway Migration: V20261125__create_affiliate_program_and_enrollment_tables.sql
-- Module 20: Affiliate Management & Partner Ecosystem
-- Step 02: Affiliate Program & Relationship Management
-- PostgreSQL 14+ Row-Level Security Enabled
-- ============================================================================

-- 1. AFFILIATE PROGRAMS TABLE
CREATE TABLE IF NOT EXISTS affiliate_programs (
    tenant_id VARCHAR(64) NOT NULL,
    program_id VARCHAR(64) NOT NULL,
    program_code VARCHAR(32) NOT NULL,
    program_name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    start_date BIGINT NOT NULL,
    end_date BIGINT,
    eligibility_policy VARCHAR(255) NOT NULL DEFAULT 'STANDARD',
    terms_reference VARCHAR(255),
    terms_version VARCHAR(32),
    max_participants INTEGER,
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    metadata_json TEXT,
    CONSTRAINT pk_affiliate_programs PRIMARY KEY (tenant_id, program_id),
    CONSTRAINT uk_affiliate_programs_code UNIQUE (tenant_id, program_code),
    CONSTRAINT fk_affiliate_programs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT chk_affiliate_programs_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT chk_affiliate_programs_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_affiliate_programs_tenant_status ON affiliate_programs(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_affiliate_programs_code ON affiliate_programs(tenant_id, program_code);

-- Enable & Force Row-Level Security for affiliate_programs
ALTER TABLE affiliate_programs ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_programs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_programs_tenant_isolation_policy ON affiliate_programs;
CREATE POLICY affiliate_programs_tenant_isolation_policy ON affiliate_programs
    FOR ALL
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 2. AFFILIATE ENROLLMENTS TABLE (Affiliate <-> Program Relationship)
CREATE TABLE IF NOT EXISTS affiliate_enrollments (
    tenant_id VARCHAR(64) NOT NULL,
    enrollment_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    program_id VARCHAR(64) NOT NULL,
    enrollment_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    effective_from BIGINT,
    effective_to BIGINT,
    enrollment_reason TEXT,
    requested_at BIGINT NOT NULL,
    approved_by VARCHAR(64),
    approved_at BIGINT,
    rejected_by VARCHAR(64),
    rejected_at BIGINT,
    rejection_reason TEXT,
    suspended_by VARCHAR(64),
    suspended_at BIGINT,
    suspension_reason TEXT,
    terminated_by VARCHAR(64),
    terminated_at BIGINT,
    termination_reason TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    metadata_json TEXT,
    CONSTRAINT pk_affiliate_enrollments PRIMARY KEY (tenant_id, enrollment_id),
    CONSTRAINT fk_affiliate_enrollments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_affiliate_enrollments_affiliate FOREIGN KEY (tenant_id, affiliate_id) REFERENCES affiliates(tenant_id, affiliate_id) ON DELETE CASCADE,
    CONSTRAINT fk_affiliate_enrollments_program FOREIGN KEY (tenant_id, program_id) REFERENCES affiliate_programs(tenant_id, program_id) ON DELETE CASCADE,
    CONSTRAINT chk_affiliate_enrollments_status CHECK (enrollment_status IN ('PENDING', 'APPROVED', 'ACTIVE', 'SUSPENDED', 'TERMINATED', 'EXPIRED', 'REJECTED')),
    CONSTRAINT chk_affiliate_enrollments_dates CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE INDEX IF NOT EXISTS idx_affiliate_enrollments_tenant_affiliate ON affiliate_enrollments(tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_enrollments_tenant_program ON affiliate_enrollments(tenant_id, program_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_enrollments_tenant_status ON affiliate_enrollments(tenant_id, enrollment_status);

-- Enable & Force Row-Level Security for affiliate_enrollments
ALTER TABLE affiliate_enrollments ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_enrollments FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_enrollments_tenant_isolation_policy ON affiliate_enrollments;
CREATE POLICY affiliate_enrollments_tenant_isolation_policy ON affiliate_enrollments
    FOR ALL
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 3. AFFILIATE PROGRAM & ENROLLMENT AUDIT RECORDS (Append-Only Cryptographic Hash Ledger)
CREATE TABLE IF NOT EXISTS affiliate_program_audit_records (
    tenant_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL, -- 'PROGRAM' or 'ENROLLMENT'
    entity_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    record_hash VARCHAR(64) NOT NULL,
    previous_audit_hash VARCHAR(64),
    chain_hash VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    CONSTRAINT pk_affiliate_program_audit_records PRIMARY KEY (tenant_id, audit_id),
    CONSTRAINT fk_affiliate_program_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_affiliate_program_audit_entity ON affiliate_program_audit_records(tenant_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_program_audit_timestamp ON affiliate_program_audit_records(tenant_id, timestamp);

-- Enable & Force Row-Level Security for affiliate_program_audit_records
ALTER TABLE affiliate_program_audit_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_program_audit_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_program_audit_tenant_isolation_policy ON affiliate_program_audit_records;
CREATE POLICY affiliate_program_audit_tenant_isolation_policy ON affiliate_program_audit_records
    FOR ALL
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));

-- 4. AFFILIATE PROGRAM OUTBOX EVENTS
CREATE TABLE IF NOT EXISTS affiliate_program_outbox_events (
    tenant_id VARCHAR(64) NOT NULL,
    outbox_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL, -- 'PROGRAM' or 'ENROLLMENT'
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    correlation_id VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL,
    CONSTRAINT pk_affiliate_program_outbox_events PRIMARY KEY (tenant_id, outbox_id),
    CONSTRAINT fk_affiliate_program_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_affiliate_program_outbox_status ON affiliate_program_outbox_events(tenant_id, status);

-- Enable & Force Row-Level Security for affiliate_program_outbox_events
ALTER TABLE affiliate_program_outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_program_outbox_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_program_outbox_tenant_isolation_policy ON affiliate_program_outbox_events;
CREATE POLICY affiliate_program_outbox_tenant_isolation_policy ON affiliate_program_outbox_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_project_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_project_id', true));
