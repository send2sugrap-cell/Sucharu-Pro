-- =============================================================================
-- Migration: V20261113__extend_substrate_reservations_soft_hard_allocation.sql
-- Module 19: Substrate Stock Auto-Reservation
-- Step 02: Real-Time Soft/Hard Stock Reservation & Allocation Engine
-- =============================================================================

-- Add Step 02 columns to substrate_reservations
ALTER TABLE substrate_reservations ADD COLUMN IF NOT EXISTS reservation_mode VARCHAR(32) NOT NULL DEFAULT 'SOFT';
ALTER TABLE substrate_reservations ADD COLUMN IF NOT EXISTS soft_hold_expires_at BIGINT;
ALTER TABLE substrate_reservations ADD COLUMN IF NOT EXISTS promoted_at BIGINT;
ALTER TABLE substrate_reservations ADD COLUMN IF NOT EXISTS promoted_by VARCHAR(128);

-- Create physical allocation sources table for hard reservations
CREATE TABLE IF NOT EXISTS substrate_reservation_allocations (
    allocation_id VARCHAR(64) PRIMARY KEY,
    reservation_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    warehouse_id VARCHAR(64) NOT NULL,
    location_id VARCHAR(64),
    batch_number VARCHAR(128),
    allocated_sheets BIGINT NOT NULL,
    allocated_reams NUMERIC(10, 4) NOT NULL,
    allocated_weight_kg NUMERIC(10, 4) NOT NULL,
    allocated_at BIGINT NOT NULL,
    allocated_by VARCHAR(128) NOT NULL,
    CONSTRAINT fk_alloc_sub_res FOREIGN KEY (reservation_id) REFERENCES substrate_reservations (reservation_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sub_res_alloc_tenant_res ON substrate_reservation_allocations (tenant_id, reservation_id);
CREATE INDEX IF NOT EXISTS idx_sub_res_alloc_tenant_wh ON substrate_reservation_allocations (tenant_id, warehouse_id);

ALTER TABLE substrate_reservation_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_reservation_allocations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS substrate_reservation_allocations_tenant_isolation ON substrate_reservation_allocations;
CREATE POLICY substrate_reservation_allocations_tenant_isolation ON substrate_reservation_allocations
    USING (tenant_id = CURRENT_SETTING('app.current_tenant', true))
    WITH CHECK (tenant_id = CURRENT_SETTING('app.current_tenant', true));
