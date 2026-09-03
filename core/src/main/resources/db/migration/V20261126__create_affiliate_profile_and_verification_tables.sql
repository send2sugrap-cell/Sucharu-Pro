-- ============================================================================
-- Flyway Migration: V20261126__create_affiliate_profile_and_verification_tables.sql
-- Module 20: Affiliate Management & Partner Ecosystem
-- Step 03: Affiliate Profile, Verification & Governance Management
-- PostgreSQL 14+ Row-Level Security Enabled & Forced
-- ============================================================================

-- 1. AFFILIATE OPERATIONAL & BUSINESS PROFILES TABLE
CREATE TABLE IF NOT EXISTS affiliate_profiles (
    tenant_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    business_type VARCHAR(64) NOT NULL DEFAULT 'INDIVIDUAL',
    business_description TEXT,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(64),
    website VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(128),
    region VARCHAR(128),
    country VARCHAR(128),
    postal_code VARCHAR(32),
    tax_id_or_gst VARCHAR(64),
    tax_information_reference VARCHAR(255),
    profile_status VARCHAR(32) NOT NULL DEFAULT 'INCOMPLETE',
    completeness_score INTEGER NOT NULL DEFAULT 0,
    completeness_details_json TEXT,
    submitted_at BIGINT,
    verified_at BIGINT,
    suspended_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    metadata_json TEXT,
    CONSTRAINT pk_affiliate_profiles PRIMARY KEY (tenant_id, affiliate_id),
    CONSTRAINT fk_affiliate_profiles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT chk_affiliate_profiles_status CHECK (profile_status IN ('INCOMPLETE', 'SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'CHANGES_REQUIRED', 'SUSPENDED')),
    CONSTRAINT chk_affiliate_profiles_business_type CHECK (business_type IN ('INDIVIDUAL', 'BUSINESS', 'AGENCY', 'RESELLER', 'PARTNER', 'ORGANIZATION', 'OTHER')),
    CONSTRAINT chk_affiliate_profiles_score CHECK (completeness_score >= 0 AND completeness_score <= 100)
);

CREATE INDEX IF NOT EXISTS idx_affiliate_profiles_tenant_status ON affiliate_profiles(tenant_id, profile_status);
CREATE INDEX IF NOT EXISTS idx_affiliate_profiles_tenant_type ON affiliate_profiles(tenant_id, business_type);

-- Enable & Force Row-Level Security for affiliate_profiles
ALTER TABLE affiliate_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_profiles FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_profiles_tenant_isolation_policy ON affiliate_profiles;
CREATE POLICY affiliate_profiles_tenant_isolation_policy ON affiliate_profiles
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 2. AFFILIATE VERIFICATION RECORDS TABLE
CREATE TABLE IF NOT EXISTS affiliate_verification_records (
    tenant_id VARCHAR(64) NOT NULL,
    verification_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    verification_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
    submitted_at BIGINT,
    reviewed_at BIGINT,
    reviewer_user_id VARCHAR(64),
    reason TEXT,
    change_request_notes TEXT,
    metadata_reference VARCHAR(255),
    previous_verification_id VARCHAR(64),
    expires_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_affiliate_verification_records PRIMARY KEY (tenant_id, verification_id),
    CONSTRAINT fk_affiliate_verification_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT chk_affiliate_verification_status CHECK (status IN ('NOT_SUBMITTED', 'SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_affiliate_verification_type CHECK (verification_type IN ('IDENTITY', 'BUSINESS', 'TAX', 'CONTACT', 'ADDRESS', 'AGREEMENT', 'DOCUMENT', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_affiliate_verification_tenant_affiliate ON affiliate_verification_records(tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_verification_tenant_status ON affiliate_verification_records(tenant_id, status);

-- Enable & Force Row-Level Security for affiliate_verification_records
ALTER TABLE affiliate_verification_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_verification_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_verification_tenant_isolation_policy ON affiliate_verification_records;
CREATE POLICY affiliate_verification_tenant_isolation_policy ON affiliate_verification_records
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 3. AFFILIATE DOCUMENT REFERENCES TABLE
CREATE TABLE IF NOT EXISTS affiliate_document_references (
    tenant_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    verification_id VARCHAR(64),
    document_type VARCHAR(64) NOT NULL,
    storage_reference VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT,
    mime_type VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
    rejection_reason TEXT,
    uploaded_at BIGINT NOT NULL,
    expires_at BIGINT,
    verified_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_affiliate_document_references PRIMARY KEY (tenant_id, document_id),
    CONSTRAINT fk_affiliate_documents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT chk_affiliate_documents_status CHECK (status IN ('UPLOADED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_affiliate_documents_type CHECK (document_type IN ('IDENTITY_PROOF', 'BUSINESS_REGISTRATION', 'TAX_CERTIFICATE', 'ADDRESS_PROOF', 'BANK_STATEMENT', 'AGREEMENT_DOCUMENT', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_affiliate_documents_tenant_affiliate ON affiliate_document_references(tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_documents_tenant_verification ON affiliate_document_references(tenant_id, verification_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_documents_tenant_status ON affiliate_document_references(tenant_id, status);

-- Enable & Force Row-Level Security for affiliate_document_references
ALTER TABLE affiliate_document_references ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_document_references FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_documents_tenant_isolation_policy ON affiliate_document_references;
CREATE POLICY affiliate_documents_tenant_isolation_policy ON affiliate_document_references
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 4. AFFILIATE PROFILE & VERIFICATION AUDIT RECORDS TABLE
CREATE TABLE IF NOT EXISTS affiliate_profile_audit_records (
    tenant_id VARCHAR(64) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    entity_reference VARCHAR(128),
    previous_state VARCHAR(64),
    new_state VARCHAR(64) NOT NULL,
    reason TEXT,
    correlation_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64),
    record_hash VARCHAR(64) NOT NULL,
    previous_audit_hash VARCHAR(64),
    chain_hash VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    CONSTRAINT pk_affiliate_profile_audit_records PRIMARY KEY (tenant_id, audit_id),
    CONSTRAINT fk_affiliate_profile_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_affiliate_profile_audit_tenant_affiliate ON affiliate_profile_audit_records(tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_profile_audit_chain ON affiliate_profile_audit_records(tenant_id, chain_hash);

-- Enable & Force Row-Level Security for affiliate_profile_audit_records
ALTER TABLE affiliate_profile_audit_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_profile_audit_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_profile_audit_tenant_isolation_policy ON affiliate_profile_audit_records;
CREATE POLICY affiliate_profile_audit_tenant_isolation_policy ON affiliate_profile_audit_records
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 5. AFFILIATE PROFILE OUTBOX EVENTS TABLE
CREATE TABLE IF NOT EXISTS affiliate_profile_outbox_events (
    tenant_id VARCHAR(64) NOT NULL,
    outbox_id VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    correlation_id VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL,
    CONSTRAINT pk_affiliate_profile_outbox_events PRIMARY KEY (tenant_id, outbox_id),
    CONSTRAINT fk_affiliate_profile_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_affiliate_profile_outbox_status ON affiliate_profile_outbox_events(tenant_id, status);

-- Enable & Force Row-Level Security for affiliate_profile_outbox_events
ALTER TABLE affiliate_profile_outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_profile_outbox_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_profile_outbox_tenant_isolation_policy ON affiliate_profile_outbox_events;
CREATE POLICY affiliate_profile_outbox_tenant_isolation_policy ON affiliate_profile_outbox_events
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));
