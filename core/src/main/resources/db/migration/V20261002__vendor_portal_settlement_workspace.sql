-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- MODULE 13 STEP 09: VENDOR SETTLEMENT, RECONCILIATION & FINANCIAL COLLABORATION
-- Production-grade schema migration with Force Row Level Security (RLS) enforcement.
-- ====================================================================================

-- 1. Settlement Acknowledgements
CREATE TABLE IF NOT EXISTS vendor_portal_settlement_acknowledgements (
    acknowledgement_id VARCHAR(50) NOT NULL,
    settlement_id VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(50) NOT NULL DEFAULT 'PRJ-001',
    vendor_id VARCHAR(50) NOT NULL,
    acknowledged_by VARCHAR(100) NOT NULL,
    acknowledged_at BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('VIEW_ONLY', 'ACKNOWLEDGED', 'ACKNOWLEDGED_WITH_DISCREPANCY', 'DECLINED')),
    idempotency_key VARCHAR(120) NOT NULL,
    discrepancy_flag BOOLEAN NOT NULL DEFAULT FALSE,
    discrepancy_notes TEXT,
    evidence_references TEXT[] NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    PRIMARY KEY (project_id, acknowledgement_id),
    CONSTRAINT uq_settlement_ack_idempotency UNIQUE (project_id, idempotency_key),
    CONSTRAINT uq_settlement_ack_vendor_settlement UNIQUE (project_id, vendor_id, settlement_id)
);

CREATE INDEX IF NOT EXISTS idx_vp_settlement_ack_lookup
    ON vendor_portal_settlement_acknowledgements(project_id, vendor_id, settlement_id);

-- 2. Reconciliation Cases
CREATE TABLE IF NOT EXISTS vendor_portal_reconciliation_cases (
    case_id VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(50) NOT NULL DEFAULT 'PRJ-001',
    vendor_id VARCHAR(50) NOT NULL,
    settlement_id VARCHAR(50),
    invoice_id VARCHAR(50),
    case_number VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'VENDOR_RESPONSE_REQUIRED', 'INTERNAL_RESPONSE_REQUIRED', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    claimed_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    system_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    variance_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    notes TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (project_id, case_id),
    CONSTRAINT uq_reconciliation_case_number UNIQUE (project_id, case_number)
);

CREATE INDEX IF NOT EXISTS idx_vp_reconciliation_cases_vendor
    ON vendor_portal_reconciliation_cases(project_id, vendor_id, status);

CREATE INDEX IF NOT EXISTS idx_vp_reconciliation_cases_settlement
    ON vendor_portal_reconciliation_cases(project_id, settlement_id);

-- 3. Reconciliation Events
CREATE TABLE IF NOT EXISTS vendor_portal_reconciliation_events (
    event_id VARCHAR(50) NOT NULL,
    case_id VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    remarks TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE INDEX IF NOT EXISTS idx_vp_reconciliation_events_case
    ON vendor_portal_reconciliation_events(case_id, timestamp ASC);

-- 4. Financial Disputes
CREATE TABLE IF NOT EXISTS vendor_portal_financial_disputes (
    dispute_id VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(50) NOT NULL DEFAULT 'PRJ-001',
    vendor_id VARCHAR(50) NOT NULL,
    settlement_id VARCHAR(50),
    invoice_id VARCHAR(50),
    dispute_number VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    status VARCHAR(50) NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'RESPONSE_REQUIRED', 'RESOLUTION_PROPOSED', 'RESOLVED', 'CLOSED', 'REJECTED')),
    disputed_amount NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    proposed_resolution_amount NUMERIC(18, 4),
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    reason TEXT NOT NULL,
    resolution_notes TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    resolved_by VARCHAR(100),
    resolved_at BIGINT,
    PRIMARY KEY (project_id, dispute_id),
    CONSTRAINT uq_financial_dispute_number UNIQUE (project_id, dispute_number)
);

CREATE INDEX IF NOT EXISTS idx_vp_financial_disputes_vendor
    ON vendor_portal_financial_disputes(project_id, vendor_id, status);

CREATE INDEX IF NOT EXISTS idx_vp_financial_disputes_settlement
    ON vendor_portal_financial_disputes(project_id, settlement_id);

-- 5. Financial Dispute Events
CREATE TABLE IF NOT EXISTS vendor_portal_financial_dispute_events (
    event_id VARCHAR(50) NOT NULL,
    dispute_id VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    remarks TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE INDEX IF NOT EXISTS idx_vp_financial_dispute_events_dispute
    ON vendor_portal_financial_dispute_events(dispute_id, timestamp ASC);

-- 6. Financial Evidence
CREATE TABLE IF NOT EXISTS vendor_portal_financial_evidence (
    evidence_id VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(50) NOT NULL DEFAULT 'PRJ-001',
    vendor_id VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    evidence_type VARCHAR(50) NOT NULL CHECK (evidence_type IN ('SETTLEMENT_STATEMENT', 'BANK_ADVICE', 'RECONCILIATION_PROOF', 'DISPUTE_JUSTIFICATION', 'TAX_WITHHOLDING_CERT', 'CREDIT_MEMO', 'OTHER')),
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    checksum VARCHAR(100),
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT 'application/pdf',
    description TEXT,
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at BIGINT NOT NULL,
    PRIMARY KEY (project_id, evidence_id)
);

CREATE INDEX IF NOT EXISTS idx_vp_financial_evidence_entity
    ON vendor_portal_financial_evidence(project_id, vendor_id, entity_type, entity_id);

-- 7. Financial Collaboration Threads
CREATE TABLE IF NOT EXISTS vendor_portal_financial_threads (
    thread_id VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(50) NOT NULL DEFAULT 'PRJ-001',
    vendor_id VARCHAR(50) NOT NULL,
    context_type VARCHAR(50) NOT NULL,
    context_id VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    message_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (project_id, thread_id)
);

CREATE INDEX IF NOT EXISTS idx_vp_financial_threads_context
    ON vendor_portal_financial_threads(project_id, vendor_id, context_type, context_id);

-- 8. Financial Messages
CREATE TABLE IF NOT EXISTS vendor_portal_financial_messages (
    message_id VARCHAR(50) NOT NULL,
    thread_id VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(50) NOT NULL DEFAULT 'PRJ-001',
    vendor_id VARCHAR(50) NOT NULL,
    sender_id VARCHAR(100) NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    evidence_references TEXT[] NOT NULL DEFAULT '{}',
    timestamp BIGINT NOT NULL,
    PRIMARY KEY (project_id, message_id)
);

CREATE INDEX IF NOT EXISTS idx_vp_financial_messages_thread
    ON vendor_portal_financial_messages(project_id, thread_id, timestamp ASC);

-- 9. Financial Audit Events
CREATE TABLE IF NOT EXISTS vendor_portal_financial_audit_events (
    activity_id VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'TENANT-001',
    project_id VARCHAR(50) NOT NULL DEFAULT 'PRJ-001',
    vendor_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    occurred_at BIGINT NOT NULL,
    metadata_json TEXT DEFAULT '{}',
    PRIMARY KEY (project_id, activity_id)
);

CREATE INDEX IF NOT EXISTS idx_vp_financial_audit_vendor
    ON vendor_portal_financial_audit_events(project_id, vendor_id, occurred_at DESC);

-- ====================================================================================
-- ROW LEVEL SECURITY (RLS) ENFORCEMENT
-- ====================================================================================

DO $$
DECLARE
    t text;
    step09_tables text[] := ARRAY[
        'vendor_portal_settlement_acknowledgements',
        'vendor_portal_reconciliation_cases',
        'vendor_portal_financial_disputes',
        'vendor_portal_financial_evidence',
        'vendor_portal_financial_threads',
        'vendor_portal_financial_messages',
        'vendor_portal_financial_audit_events'
    ];
BEGIN
    FOREACH t IN ARRAY step09_tables
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY;', t);
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I;', t || '_tenant_isolation', t);
        EXECUTE format('CREATE POLICY %I ON %I FOR ALL USING (project_id = CURRENT_SETTING(''app.current_project_id'', true));', t || '_tenant_isolation', t);
    END LOOP;
END $$;
