-- V20261207__create_affiliate_payout_disbursement_tables.sql
-- Module 23 Step 07: Affiliate Payout Disbursement Tables
-- Schema with Multi-Tenant Row-Level Security (RLS)

CREATE TABLE IF NOT EXISTS affiliate_payout_disbursements (
    disbursement_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL REFERENCES affiliate_payout_requests(request_id) ON DELETE CASCADE,
    wallet_id VARCHAR(64) NOT NULL REFERENCES affiliate_wallets(wallet_id) ON DELETE CASCADE,
    affiliate_id VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    provider_name VARCHAR(128) NOT NULL,
    provider_transaction_ref VARCHAR(128),
    provider_status VARCHAR(32) NOT NULL,
    provider_response_code VARCHAR(64),
    failure_reason TEXT,
    ledger_entry_id VARCHAR(64) REFERENCES affiliate_wallet_ledger_entries(entry_id),
    processed_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_affiliate_disbursement_tenant_req ON affiliate_payout_disbursements (tenant_id, request_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_disbursement_tenant_wallet ON affiliate_payout_disbursements (tenant_id, wallet_id);

-- Enable RLS for affiliate_payout_disbursements
ALTER TABLE affiliate_payout_disbursements ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_payout_disbursements FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_payout_disbursements_tenant_isolation_policy ON affiliate_payout_disbursements;
CREATE POLICY affiliate_payout_disbursements_tenant_isolation_policy ON affiliate_payout_disbursements
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        OR tenant_id = CURRENT_SETTING('app.current_project_id', true)
    );
