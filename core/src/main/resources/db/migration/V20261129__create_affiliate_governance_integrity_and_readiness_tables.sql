-- ============================================================================
-- Flyway Migration: V20261129__create_affiliate_governance_integrity_and_readiness_tables.sql
-- Module 20: Affiliate Management & Partner Ecosystem
-- Step 06: Final Governance, Integrity & Cross-Module Readiness
-- PostgreSQL 14+ Row-Level Security Enabled & Forced
-- ============================================================================

-- 1. AFFILIATE INTEGRATION READINESS SNAPSHOTS TABLE
--    One row per (tenant_id, affiliate_id). Holds the most recent readiness state
--    computed by the AffiliateGovernanceIntegrityEngine. This is the authoritative
--    record that Modules 21–24 may reference to gate their own operations.
CREATE TABLE IF NOT EXISTS affiliate_integration_readiness (
    tenant_id                        VARCHAR(64)  NOT NULL,
    affiliate_id                     VARCHAR(64)  NOT NULL,

    -- Step 01 gates
    profile_exists                   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_identity_verified             BOOLEAN      NOT NULL DEFAULT FALSE,
    is_agreement_accepted            BOOLEAN      NOT NULL DEFAULT FALSE,
    is_account_active                BOOLEAN      NOT NULL DEFAULT FALSE,
    is_tax_compliant                 BOOLEAN      NOT NULL DEFAULT FALSE,
    is_business_verified             BOOLEAN      NOT NULL DEFAULT FALSE,
    is_fully_eligible                BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Step 02 gates
    has_active_enrollment            BOOLEAN      NOT NULL DEFAULT FALSE,
    has_at_least_one_enrollment      BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Step 03 gates
    has_operational_profile          BOOLEAN      NOT NULL DEFAULT FALSE,
    profile_completeness_score       INT          NOT NULL DEFAULT 0 CHECK (profile_completeness_score BETWEEN 0 AND 100),
    has_verified_documents           BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Step 04 gates
    has_accepted_notification_prefs  BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Step 05 gates
    has_no_open_urgent_work_items    BOOLEAN      NOT NULL DEFAULT TRUE,
    has_clear_governance_queue       BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Derived downstream readiness flags (Modules 21–24)
    is_ready_for_attribution         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_ready_for_commission          BOOLEAN      NOT NULL DEFAULT FALSE,
    is_ready_for_payout              BOOLEAN      NOT NULL DEFAULT FALSE,
    is_ready_for_analytics           BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Metadata
    readiness_score                  INT          NOT NULL DEFAULT 0 CHECK (readiness_score BETWEEN 0 AND 100),
    assessed_at                      BIGINT       NOT NULL,
    assessed_by                      VARCHAR(128) NOT NULL,
    integrity_hash                   VARCHAR(128) NOT NULL,

    CONSTRAINT pk_affiliate_integration_readiness PRIMARY KEY (tenant_id, affiliate_id),
    CONSTRAINT fk_affiliate_readiness_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_affiliate_readiness_attribution
    ON affiliate_integration_readiness(tenant_id, is_ready_for_attribution);
CREATE INDEX IF NOT EXISTS idx_affiliate_readiness_commission
    ON affiliate_integration_readiness(tenant_id, is_ready_for_commission);
CREATE INDEX IF NOT EXISTS idx_affiliate_readiness_payout
    ON affiliate_integration_readiness(tenant_id, is_ready_for_payout);
CREATE INDEX IF NOT EXISTS idx_affiliate_readiness_analytics
    ON affiliate_integration_readiness(tenant_id, is_ready_for_analytics);

-- Enable & Force Row-Level Security for affiliate_integration_readiness
ALTER TABLE affiliate_integration_readiness ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_integration_readiness FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_readiness_tenant_isolation_policy ON affiliate_integration_readiness;
CREATE POLICY affiliate_readiness_tenant_isolation_policy ON affiliate_integration_readiness
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));


-- 2. AFFILIATE LIFECYCLE INTEGRITY CHECKS TABLE (APPEND-ONLY)
--    Stores the full history of cross-step integrity assessments. Each row is
--    a deterministic snapshot of the affiliate's aggregate health at a point in time.
--    Records must NEVER be deleted or updated (append-only governance log).
CREATE TABLE IF NOT EXISTS affiliate_lifecycle_integrity_checks (
    tenant_id          VARCHAR(64)  NOT NULL,
    check_id           VARCHAR(64)  NOT NULL,
    affiliate_id       VARCHAR(64)  NOT NULL,
    is_integrity_valid BOOLEAN      NOT NULL,
    critical_count     INT          NOT NULL DEFAULT 0,
    high_count         INT          NOT NULL DEFAULT 0,
    medium_count       INT          NOT NULL DEFAULT 0,
    low_count          INT          NOT NULL DEFAULT 0,
    summary            TEXT         NOT NULL,
    violations_json    TEXT,        -- JSON array of AffiliateIntegrityViolation details
    checked_at         BIGINT       NOT NULL,
    checked_by         VARCHAR(128) NOT NULL,
    result_hash        VARCHAR(128) NOT NULL,

    CONSTRAINT pk_affiliate_lifecycle_integrity_checks PRIMARY KEY (tenant_id, check_id),
    CONSTRAINT fk_affiliate_integrity_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    CONSTRAINT chk_affiliate_integrity_counts CHECK (
        critical_count >= 0 AND high_count >= 0 AND medium_count >= 0 AND low_count >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_affiliate_integrity_tenant_affiliate
    ON affiliate_lifecycle_integrity_checks(tenant_id, affiliate_id, checked_at);
CREATE INDEX IF NOT EXISTS idx_affiliate_integrity_tenant_valid
    ON affiliate_lifecycle_integrity_checks(tenant_id, is_integrity_valid);

-- Enable & Force Row-Level Security for affiliate_lifecycle_integrity_checks
ALTER TABLE affiliate_lifecycle_integrity_checks ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_lifecycle_integrity_checks FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_integrity_checks_tenant_isolation_policy ON affiliate_lifecycle_integrity_checks;
CREATE POLICY affiliate_integrity_checks_tenant_isolation_policy ON affiliate_lifecycle_integrity_checks
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.current_project_id', true), ''), current_setting('app.current_tenant_id', true)));
