-- ============================================================================
-- SUCHARU PRO ERP — DATABASE MIGRATION
-- Module 13 Step 10: Vendor Portal Analytics, Notifications & Search
-- ============================================================================

-- 1. Vendor Portal Notifications
CREATE TABLE IF NOT EXISTS vendor_portal_notifications (
    notification_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(32) NOT NULL DEFAULT 'UNREAD',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(64),
    related_entity_id VARCHAR(64),
    deep_link_target VARCHAR(255),
    created_at BIGINT NOT NULL,
    read_at BIGINT,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_vp_notif_tenant_vendor ON vendor_portal_notifications (tenant_id, project_id, vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_vp_notif_created ON vendor_portal_notifications (created_at DESC);

-- 2. Vendor Portal Notification Preferences
CREATE TABLE IF NOT EXISTS vendor_portal_notification_preferences (
    preference_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    important_only_mode BOOLEAN NOT NULL DEFAULT FALSE,
    disabled_categories_json TEXT NOT NULL DEFAULT '[]',
    min_severity VARCHAR(32) NOT NULL DEFAULT 'LOW',
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_vp_notif_pref_vendor UNIQUE (tenant_id, project_id, vendor_id)
);

CREATE INDEX IF NOT EXISTS idx_vp_notif_pref_vendor ON vendor_portal_notification_preferences (tenant_id, project_id, vendor_id);

-- 3. Vendor Portal Analytics Snapshots (Optional Cached Snapshots)
CREATE TABLE IF NOT EXISTS vendor_portal_analytics_snapshots (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    vendor_id VARCHAR(64) NOT NULL,
    period VARCHAR(32) NOT NULL,
    calculation_version INT NOT NULL DEFAULT 1,
    metrics_json TEXT NOT NULL,
    calculated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vp_analytics_snap ON vendor_portal_analytics_snapshots (tenant_id, project_id, vendor_id, period);

-- 4. Enable and Force Row Level Security (RLS)
ALTER TABLE vendor_portal_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_notifications FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_portal_notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_notification_preferences FORCE ROW LEVEL SECURITY;

ALTER TABLE vendor_portal_analytics_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendor_portal_analytics_snapshots FORCE ROW LEVEL SECURITY;

-- 5. RLS Policies
DROP POLICY IF EXISTS tenant_isolation_vp_notifications ON vendor_portal_notifications;
CREATE POLICY tenant_isolation_vp_notifications ON vendor_portal_notifications
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true));

DROP POLICY IF EXISTS tenant_isolation_vp_notif_prefs ON vendor_portal_notification_preferences;
CREATE POLICY tenant_isolation_vp_notif_prefs ON vendor_portal_notification_preferences
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true));

DROP POLICY IF EXISTS tenant_isolation_vp_analytics_snapshots ON vendor_portal_analytics_snapshots;
CREATE POLICY tenant_isolation_vp_analytics_snapshots ON vendor_portal_analytics_snapshots
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true));
