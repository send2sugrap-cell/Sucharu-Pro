-- V20261205__create_affiliate_wallet_hold_tables.sql
-- Module 23 Step 04: Affiliate Wallet Holds & Reservations Tables
-- Schema with Multi-Tenant Row-Level Security (RLS)

CREATE TABLE IF NOT EXISTS affiliate_wallet_holds (
    hold_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    wallet_id VARCHAR(64) NOT NULL REFERENCES affiliate_wallets(wallet_id) ON DELETE CASCADE,
    affiliate_id VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    hold_type VARCHAR(32) NOT NULL,
    hold_reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    reference_id VARCHAR(128),
    created_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    released_by VARCHAR(64),
    released_at BIGINT,
    release_reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_affiliate_holds_tenant_wallet ON affiliate_wallet_holds (tenant_id, wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_affiliate_holds_tenant_affiliate ON affiliate_wallet_holds (tenant_id, affiliate_id);

-- Enable RLS for affiliate_wallet_holds
ALTER TABLE affiliate_wallet_holds ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_wallet_holds FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_wallet_holds_tenant_isolation_policy ON affiliate_wallet_holds;
CREATE POLICY affiliate_wallet_holds_tenant_isolation_policy ON affiliate_wallet_holds
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        OR tenant_id = CURRENT_SETTING('app.current_project_id', true)
    );
