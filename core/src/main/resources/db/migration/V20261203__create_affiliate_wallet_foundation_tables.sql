-- V20261203__create_affiliate_wallet_foundation_tables.sql
-- Module 23 Step 01: Affiliate Wallet Foundation
-- Canonical PostgreSQL Schema with Multi-Tenant Row-Level Security (RLS)

CREATE TABLE IF NOT EXISTS affiliate_wallets (
    wallet_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    affiliate_id VARCHAR(64) NOT NULL REFERENCES affiliates(affiliate_id) ON DELETE CASCADE,
    currency VARCHAR(8) NOT NULL DEFAULT 'BDT',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_affiliate_wallet_tenant_affiliate_currency UNIQUE (tenant_id, affiliate_id, currency)
);

CREATE INDEX IF NOT EXISTS idx_affiliate_wallets_tenant_affiliate ON affiliate_wallets (tenant_id, affiliate_id);
CREATE INDEX IF NOT EXISTS idx_affiliate_wallets_tenant_status ON affiliate_wallets (tenant_id, status);

-- Enable RLS for affiliate_wallets
ALTER TABLE affiliate_wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE affiliate_wallets FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS affiliate_wallets_tenant_isolation_policy ON affiliate_wallets;
CREATE POLICY affiliate_wallets_tenant_isolation_policy ON affiliate_wallets
    FOR ALL
    USING (
        tenant_id = CURRENT_SETTING('app.current_tenant_id', true)
        OR tenant_id = CURRENT_SETTING('app.current_project_id', true)
    );
