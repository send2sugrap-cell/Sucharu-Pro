-- =============================================================================
-- Migration: V20261112__create_substrate_stock_reservation_tables.sql
-- Module 19: Substrate Stock Auto-Reservation (Step 01 Foundation)
-- =============================================================================

CREATE TABLE IF NOT EXISTS substrate_reservations (
    reservation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    order_item_id VARCHAR(64) NOT NULL,
    execution_job_id VARCHAR(64),
    work_order_id VARCHAR(64),
    product_id VARCHAR(64) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(64) NOT NULL,
    location_id VARCHAR(64),
    stock_type VARCHAR(64) NOT NULL,
    gsm NUMERIC(10, 4) NOT NULL,
    sheet_width_mm NUMERIC(10, 4) NOT NULL,
    sheet_height_mm NUMERIC(10, 4) NOT NULL,
    reserved_sheets BIGINT NOT NULL,
    reserved_reams NUMERIC(10, 4) NOT NULL,
    reserved_weight_kg NUMERIC(10, 4) NOT NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'RESERVED_SOFT',
    idempotency_key VARCHAR(128) NOT NULL,
    expiry_timestamp BIGINT,
    reserved_by VARCHAR(128) NOT NULL,
    reserved_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    notes TEXT,
    CONSTRAINT uq_substrate_res_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_sub_res_tenant_order ON substrate_reservations (tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_sub_res_tenant_job ON substrate_reservations (tenant_id, execution_job_id);
CREATE INDEX IF NOT EXISTS idx_sub_res_tenant_sku ON substrate_reservations (tenant_id, sku);
CREATE INDEX IF NOT EXISTS idx_sub_res_tenant_status ON substrate_reservations (tenant_id, status);

ALTER TABLE substrate_reservations ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_reservations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS substrate_reservations_tenant_isolation ON substrate_reservations;
CREATE POLICY substrate_reservations_tenant_isolation ON substrate_reservations
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));

CREATE TABLE IF NOT EXISTS substrate_reservation_audit_events (
    event_id VARCHAR(64) PRIMARY KEY,
    reservation_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    previous_status VARCHAR(64),
    new_status VARCHAR(64) NOT NULL,
    quantity_change_sheets BIGINT NOT NULL,
    actor VARCHAR(128) NOT NULL,
    reason TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    CONSTRAINT fk_audit_sub_res FOREIGN KEY (reservation_id) REFERENCES substrate_reservations (reservation_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sub_res_audit_tenant_res ON substrate_reservation_audit_events (tenant_id, reservation_id);

ALTER TABLE substrate_reservation_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_reservation_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS substrate_reservation_audit_events_tenant_isolation ON substrate_reservation_audit_events;
CREATE POLICY substrate_reservation_audit_events_tenant_isolation ON substrate_reservation_audit_events
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
