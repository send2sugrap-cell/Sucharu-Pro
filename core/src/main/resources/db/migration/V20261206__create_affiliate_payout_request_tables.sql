-- V20261206__create_affiliate_payout_request_tables.sql
-- Module 23 Step 05: Affiliate Payout Request Tables
-- Schema with Multi-Tenant Row-Level Security (RLS)

CREATE TABLE IF NOT EXISTS affiliate_payout_requests (
    request_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    wallet_id VARCHAR(64) NOT NULL REFERENCES affiliate_wallets(wallet_id) ON DELETE CASCADE,
    affiliate_id VARCHAR(64) NOT NULL,
    requested_amount NUMERIC(18, 2) NOT NULL CHECK (requested_amount > 0),
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    payout_method_type VARCHAR(32) NOT NULL,
    payout_method_account_name VARCHAR(128) NOT NULL,
    payout_method_account_number VARCHAR(128) NOT NULL,
    payout_method_provider VARCHAR(128),
    payout_method_branch_routing VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    reservation_hold_id VARCHAR(64) REFERENCES affiliate_wallet_holds(hold_id),
    payout_reference VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128),
    rejection_reason TEXT,
    review_notes TEXT,
    requested_by VARCHAR(64) NOT NULL,
    requested_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_payout_request_tenant_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_affiliate_payout_req_tenant_wallet ON affiliate_payout_requests (tenant_id, wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_affiliate_payout_req_tenant_affiliate ON affiliate_payout_requests (tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_payout_req_reference ON affiliate_payout_requests (tenant_id, payout_reference);

-- Enable RLS for affiliate_payout_requests
ALTER TABLE affiliate_payout_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_payout_requests FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_payout_requests_tenant_isolation_policy ON affiliate_payout_requests;
CREATE POLICY affiliate_payout_requests_tenant_isolation_policy ON affiliate_payout_requests
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        OR tenant_id = CURRENT_SETTING('app.current_project_id', true)
    );
