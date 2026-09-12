-- V20261204__create_affiliate_wallet_ledger_tables.sql
-- Module 23 Step 02: Affiliate Wallet Ledger & Balance Tables
-- Immutable Ledger Schema with Multi-Tenant Row-Level Security (RLS)

CREATE TABLE IF NOT EXISTS affiliate_wallet_ledger_entries (
    entry_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    wallet_id VARCHAR(64) NOT NULL REFERENCES affiliate_wallets(wallet_id) ON DELETE CASCADE,
    affiliate_id VARCHAR(64) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    entry_type VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    reference_id VARCHAR(128),
    idempotency_key VARCHAR(128),
    reversal_of_entry_id VARCHAR(64) REFERENCES affiliate_wallet_ledger_entries(entry_id),
    reason TEXT,
    actor_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT uq_affiliate_ledger_tenant_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_affiliate_ledger_tenant_wallet ON affiliate_wallet_ledger_entries (tenant_id, wallet_id, created_at);
CREATE INDEX IF NOT EXISTS idx_affiliate_ledger_tenant_affiliate ON affiliate_wallet_ledger_entries (tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_ledger_reversal ON affiliate_wallet_ledger_entries (tenant_id, reversal_of_entry_id);

-- Enable RLS for affiliate_wallet_ledger_entries
ALTER TABLE affiliate_wallet_ledger_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_wallet_ledger_entries FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_wallet_ledger_tenant_isolation_policy ON affiliate_wallet_ledger_entries;
CREATE POLICY affiliate_wallet_ledger_tenant_isolation_policy ON affiliate_wallet_ledger_entries
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        OR tenant_id = CURRENT_SETTING('app.current_project_id', true)
    );
