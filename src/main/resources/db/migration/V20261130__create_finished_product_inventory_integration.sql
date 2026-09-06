-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- PHASE 06 STEP 01: FINISHED PRODUCT INVENTORY INTEGRATION (V20261130)
-- Connects Production Job & QC Release to Finished Goods Inventory Receipt
-- ====================================================================================

CREATE TABLE IF NOT EXISTS finished_product_inventory_receipts (
    receipt_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    execution_job_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    bin_id VARCHAR(50),
    received_quantity NUMERIC(15, 2) NOT NULL CHECK (received_quantity > 0),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    unit_cost NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    qc_inspection_id VARCHAR(50),
    release_id VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    received_by VARCHAR(50) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, receipt_id),
    CONSTRAINT uq_finished_goods_receipt_job UNIQUE (project_id, execution_job_id)
);

CREATE INDEX IF NOT EXISTS idx_finished_product_receipts_order ON finished_product_inventory_receipts(project_id, order_id);
CREATE INDEX IF NOT EXISTS idx_finished_product_receipts_product ON finished_product_inventory_receipts(project_id, product_id);

ALTER TABLE finished_product_inventory_receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE finished_product_inventory_receipts FORCE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'finished_product_inventory_receipts'
          AND policyname = 'finished_product_inventory_receipts_tenant_isolation'
    ) THEN
        CREATE POLICY finished_product_inventory_receipts_tenant_isolation ON finished_product_inventory_receipts
            FOR ALL
            USING (project_id = current_setting('app.current_tenant_id', true))
            WITH CHECK (project_id = current_setting('app.current_tenant_id', true));
    END IF;
END $$;
