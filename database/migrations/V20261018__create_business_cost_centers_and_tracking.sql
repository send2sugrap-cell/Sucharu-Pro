-- =========================================================================
-- MODULE 15 STEP 04: BUSINESS COST CENTER, COST CATEGORY & JOB COST TRACKING
-- Canonical PostgreSQL Schema & Row-Level Security (RLS)
-- Migration: V20261018__create_business_cost_centers_and_tracking.sql
-- =========================================================================

-- 1. BUSINESS COST CENTERS TABLE (Hierarchical)
CREATE TABLE IF NOT EXISTS business_cost_centers (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_cost_center_id VARCHAR(64),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT uq_cost_center_code UNIQUE (tenant_id, project_id, code),
    CONSTRAINT fk_cost_center_parent FOREIGN KEY (parent_cost_center_id) REFERENCES business_cost_centers(id) ON DELETE SET NULL,
    CONSTRAINT chk_cost_center_code CHECK (char_length(trim(code)) >= 2),
    CONSTRAINT chk_cost_center_name CHECK (char_length(trim(name)) >= 2)
);

-- 2. BUSINESS COST CATEGORIES TABLE (Hierarchical & Configurable)
CREATE TABLE IF NOT EXISTS business_cost_categories (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_category_id VARCHAR(64),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system_defined BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT uq_cost_category_code UNIQUE (tenant_id, project_id, code),
    CONSTRAINT fk_cost_category_parent FOREIGN KEY (parent_category_id) REFERENCES business_cost_categories(id) ON DELETE SET NULL,
    CONSTRAINT chk_cost_category_code CHECK (char_length(trim(code)) >= 2),
    CONSTRAINT chk_cost_category_name CHECK (char_length(trim(name)) >= 2)
);

-- 3. BUSINESS OPERATIONAL COST TRACKING TABLE
CREATE TABLE IF NOT EXISTS business_cost_tracking (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    ledger_posting_id VARCHAR(64),
    cost_center_id VARCHAR(64) NOT NULL,
    cost_category_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    allocation_status VARCHAR(64) NOT NULL DEFAULT 'UNALLOCATED',
    classification_status VARCHAR(64) NOT NULL DEFAULT 'CLASSIFIED',
    notes TEXT,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT fk_tracking_cost_center FOREIGN KEY (cost_center_id) REFERENCES business_cost_centers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tracking_cost_category FOREIGN KEY (cost_category_id) REFERENCES business_cost_categories(id) ON DELETE RESTRICT,
    CONSTRAINT chk_tracking_amount CHECK (amount >= 0),
    CONSTRAINT chk_tracking_currency CHECK (char_length(currency) = 3)
);

-- 4. COST CLASSIFICATION AUDIT EVENTS TABLE (Append-Only)
CREATE TABLE IF NOT EXISTS business_cost_classification_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    tracking_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    previous_state_json TEXT,
    new_state_json TEXT,
    reason TEXT NOT NULL,
    correlation_id VARCHAR(128),
    idempotency_key VARCHAR(128),
    timestamp BIGINT NOT NULL,

    CONSTRAINT chk_audit_reason CHECK (char_length(trim(reason)) >= 3)
);

-- =========================================================================
-- INDEXES FOR PERFORMANCE AND FILTERING
-- =========================================================================

CREATE INDEX IF NOT EXISTS idx_cost_center_tenant_project ON business_cost_centers(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_cost_center_parent ON business_cost_centers(parent_cost_center_id);
CREATE INDEX IF NOT EXISTS idx_cost_center_active ON business_cost_centers(is_active);

CREATE INDEX IF NOT EXISTS idx_cost_category_tenant_project ON business_cost_categories(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_cost_category_parent ON business_cost_categories(parent_category_id);
CREATE INDEX IF NOT EXISTS idx_cost_category_active ON business_cost_categories(is_active);

CREATE INDEX IF NOT EXISTS idx_cost_tracking_tenant_project ON business_cost_tracking(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_cost_tracking_source ON business_cost_tracking(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_cost_tracking_job ON business_cost_tracking(job_id);
CREATE INDEX IF NOT EXISTS idx_cost_tracking_center ON business_cost_tracking(cost_center_id);
CREATE INDEX IF NOT EXISTS idx_cost_tracking_category ON business_cost_tracking(cost_category_id);
CREATE INDEX IF NOT EXISTS idx_cost_tracking_allocation_status ON business_cost_tracking(allocation_status);

CREATE INDEX IF NOT EXISTS idx_cost_audit_tracking ON business_cost_classification_audit_events(tracking_id);
CREATE INDEX IF NOT EXISTS idx_cost_audit_tenant_project ON business_cost_classification_audit_events(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_cost_audit_timestamp ON business_cost_classification_audit_events(timestamp DESC);

-- =========================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =========================================================================

ALTER TABLE business_cost_centers ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_centers FORCE ROW LEVEL SECURITY;

CREATE POLICY rls_business_cost_centers ON business_cost_centers
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE business_cost_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_categories FORCE ROW LEVEL SECURITY;

CREATE POLICY rls_business_cost_categories ON business_cost_categories
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE business_cost_tracking ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_tracking FORCE ROW LEVEL SECURITY;

CREATE POLICY rls_business_cost_tracking ON business_cost_tracking
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE business_cost_classification_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_cost_classification_audit_events FORCE ROW LEVEL SECURITY;

CREATE POLICY rls_business_cost_classification_audit_events ON business_cost_classification_audit_events
    USING (tenant_id = current_setting('app.current_tenant_id', true));
