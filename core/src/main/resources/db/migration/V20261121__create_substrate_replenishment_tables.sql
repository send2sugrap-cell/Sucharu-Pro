-- Module 19: Substrate Stock Auto-Reservation
-- Step 04: Auto-Replenishment Triggers & Supplier Reorder Alerts Persistence Tables

CREATE TABLE IF NOT EXISTS substrate_replenishment_evaluations (
    evaluation_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    material_name VARCHAR(255) NOT NULL,
    stock_type VARCHAR(64) NOT NULL,
    gsm NUMERIC(10, 4) NOT NULL,
    sheet_width_mm NUMERIC(10, 4) NOT NULL,
    sheet_height_mm NUMERIC(10, 4) NOT NULL,
    warehouse_id VARCHAR(64) NOT NULL,
    warehouse_name VARCHAR(255) NOT NULL,
    on_hand_physical_sheets BIGINT NOT NULL,
    active_reserved_sheets BIGINT NOT NULL,
    available_sheets BIGINT NOT NULL,
    pending_inbound_sheets BIGINT NOT NULL DEFAULT 0,
    planned_demand_sheets BIGINT NOT NULL DEFAULT 0,
    net_projected_availability_sheets BIGINT NOT NULL,
    safety_stock_sheets BIGINT NOT NULL,
    reorder_point_sheets BIGINT NOT NULL,
    target_stock_sheets BIGINT NOT NULL,
    is_reorder_required BOOLEAN NOT NULL DEFAULT FALSE,
    projected_shortfall_sheets BIGINT NOT NULL DEFAULT 0,
    recommended_reorder_sheets BIGINT NOT NULL DEFAULT 0,
    recommended_reorder_reams NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    trigger_state VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    primary_reason VARCHAR(64) NOT NULL,
    policy_id VARCHAR(64) NOT NULL,
    policy_version VARCHAR(32) NOT NULL,
    primary_vendor_id VARCHAR(64),
    primary_vendor_name VARCHAR(255),
    deduplication_fingerprint VARCHAR(64) NOT NULL,
    master_integrity_hash VARCHAR(64) NOT NULL,
    evaluated_by VARCHAR(64) NOT NULL,
    evaluated_at BIGINT NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sre_tenant_sku ON substrate_replenishment_evaluations(tenant_id, sku);
CREATE INDEX IF NOT EXISTS idx_sre_tenant_state ON substrate_replenishment_evaluations(tenant_id, trigger_state);
CREATE INDEX IF NOT EXISTS idx_sre_tenant_fingerprint ON substrate_replenishment_evaluations(tenant_id, deduplication_fingerprint);

CREATE TABLE IF NOT EXISTS substrate_replenishment_supplier_recommendations (
    candidate_id VARCHAR(64) PRIMARY KEY,
    evaluation_id VARCHAR(64) NOT NULL REFERENCES substrate_replenishment_evaluations(evaluation_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    vendor_code VARCHAR(64) NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    rank INT NOT NULL,
    suitability_score NUMERIC(8, 4) NOT NULL,
    estimated_lead_time_days INT NOT NULL,
    quoted_cost_per_sheet NUMERIC(12, 4) NOT NULL,
    minimum_order_quantity_sheets BIGINT NOT NULL,
    standard_pack_size INT NOT NULL DEFAULT 500,
    primary_contact_email VARCHAR(255),
    primary_contact_phone VARCHAR(64),
    is_approved_supplier BOOLEAN NOT NULL DEFAULT TRUE,
    selection_rationale TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_srsr_evaluation ON substrate_replenishment_supplier_recommendations(evaluation_id);
CREATE INDEX IF NOT EXISTS idx_srsr_tenant_vendor ON substrate_replenishment_supplier_recommendations(tenant_id, vendor_id);

CREATE TABLE IF NOT EXISTS substrate_supplier_reorder_alerts (
    alert_id VARCHAR(64) PRIMARY KEY,
    evaluation_id VARCHAR(64) NOT NULL REFERENCES substrate_replenishment_evaluations(evaluation_id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    vendor_code VARCHAR(64) NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    material_name VARCHAR(255) NOT NULL,
    requested_sheets BIGINT NOT NULL,
    requested_reams NUMERIC(12, 4) NOT NULL,
    target_delivery_timestamp BIGINT,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUPPLIER_ALERT_SENT',
    alert_payload_json TEXT,
    dispatched_by VARCHAR(64) NOT NULL,
    dispatched_at BIGINT NOT NULL,
    acknowledged_at BIGINT,
    purchase_requisition_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ssra_evaluation ON substrate_supplier_reorder_alerts(evaluation_id);
CREATE INDEX IF NOT EXISTS idx_ssra_tenant_vendor ON substrate_supplier_reorder_alerts(tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_ssra_tenant_status ON substrate_supplier_reorder_alerts(tenant_id, status);

CREATE TABLE IF NOT EXISTS substrate_replenishment_audit_events (
    audit_id VARCHAR(64) PRIMARY KEY,
    evaluation_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    previous_state VARCHAR(32) NOT NULL,
    new_state VARCHAR(32) NOT NULL,
    trigger_action VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_srae_evaluation ON substrate_replenishment_audit_events(evaluation_id);
CREATE INDEX IF NOT EXISTS idx_srae_tenant ON substrate_replenishment_audit_events(tenant_id);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) ENFORCEMENT
-- ============================================================================

ALTER TABLE substrate_replenishment_evaluations ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_replenishment_evaluations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_substrate_replenishment_evaluations ON substrate_replenishment_evaluations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE substrate_replenishment_supplier_recommendations ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_replenishment_supplier_recommendations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_substrate_replenishment_supplier_recommendations ON substrate_replenishment_supplier_recommendations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE substrate_supplier_reorder_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_supplier_reorder_alerts FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_substrate_supplier_reorder_alerts ON substrate_supplier_reorder_alerts
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE substrate_replenishment_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE substrate_replenishment_audit_events FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_substrate_replenishment_audit_events ON substrate_replenishment_audit_events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
