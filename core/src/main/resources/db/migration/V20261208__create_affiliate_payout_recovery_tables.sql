-- V20261208__create_affiliate_payout_recovery_tables.sql
-- Module 23 Step 08: Affiliate Payout Recovery, Reversal & Reconciliation Tables
-- Schema with Multi-Tenant Row-Level Security (RLS)

CREATE TABLE IF NOT EXISTS affiliate_payout_reversals (
    reversal_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL REFERENCES affiliate_payout_requests(request_id) ON DELETE CASCADE,
    wallet_id VARCHAR(64) NOT NULL REFERENCES affiliate_wallets(wallet_id) ON DELETE CASCADE,
    affiliate_id VARCHAR(64) NOT NULL,
    reversed_amount NUMERIC(18, 2) NOT NULL CHECK (reversed_amount > 0),
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    original_ledger_entry_id VARCHAR(64) REFERENCES affiliate_wallet_ledger_entries(entry_id),
    compensating_ledger_entry_id VARCHAR(64) NOT NULL REFERENCES affiliate_wallet_ledger_entries(entry_id),
    reversal_reason TEXT NOT NULL,
    reversed_by VARCHAR(64) NOT NULL,
    reversed_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS affiliate_payout_reconciliations (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL REFERENCES affiliate_payout_requests(request_id) ON DELETE CASCADE,
    wallet_id VARCHAR(64) NOT NULL REFERENCES affiliate_wallets(wallet_id) ON DELETE CASCADE,
    internal_status VARCHAR(32) NOT NULL,
    provider_status VARCHAR(32),
    provider_transaction_ref VARCHAR(128),
    ledger_entry_id VARCHAR(64),
    reconciliation_status VARCHAR(32) NOT NULL,
    reconciliation_notes TEXT NOT NULL,
    reconciled_by VARCHAR(64) NOT NULL,
    reconciled_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_affiliate_payout_rev_tenant_req ON affiliate_payout_reversals (tenant_id, request_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_payout_rec_tenant_req ON affiliate_payout_reconciliations (tenant_id, request_id);

-- Enable RLS for recovery tables
ALTER TABLE affiliate_payout_reversals ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_payout_reversals FORCE ROW LEVEL SECURITY;

ALTER TABLE affiliate_payout_reconciliations ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_payout_reconciliations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_payout_reversals_tenant_policy ON affiliate_payout_reversals;
CREATE POLICY affiliate_payout_reversals_tenant_policy ON affiliate_payout_reversals
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        OR tenant_id = CURRENT_SETTING('app.current_project_id', true)
    );

DROP POLICY IF EXISTS affiliate_payout_reconciliations_tenant_policy ON affiliate_payout_reconciliations;
CREATE POLICY affiliate_payout_reconciliations_tenant_policy ON affiliate_payout_reconciliations
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        OR tenant_id = CURRENT_SETTING('app.current_project_id', true)
    );
