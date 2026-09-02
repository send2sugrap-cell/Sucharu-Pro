-- ====================================================================================
-- MODULE 14 STEP 05: CUSTOMER RECONCILIATIONS & LEDGER AUDIT FOUNDATION
-- Migration: V20261009__create_customer_reconciliations.sql
-- ====================================================================================

CREATE TABLE IF NOT EXISTS customer_reconciliations (
    reconciliation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    customer_financial_account_id VARCHAR(64) NOT NULL,
    reconciled_at BIGINT NOT NULL,
    reconciled_by VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    invoice_total_receivable NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    ledger_calculated_balance NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    available_credit_balance NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    difference NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    is_consistent BOOLEAN NOT NULL DEFAULT TRUE,
    discrepancy_count INT NOT NULL DEFAULT 0,
    discrepancies_json TEXT,
    notes TEXT,
    created_at BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_customer_reconciliations_tenant_proj_cust
    ON customer_reconciliations (tenant_id, project_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_customer_reconciliations_reconciled_at
    ON customer_reconciliations (reconciled_at);

ALTER TABLE customer_reconciliations ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_reconciliations FORCE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'customer_reconciliations' AND policyname = 'customer_reconciliations_tenant_isolation_policy'
    ) THEN
        CREATE POLICY customer_reconciliations_tenant_isolation_policy ON customer_reconciliations
            FOR ALL
            USING (tenant_id = current_setting('app.current_tenant_id', true) AND project_id = current_setting('app.current_project_id', true))
            WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true) AND project_id = current_setting('app.current_project_id', true));
    END IF;
END $$;
