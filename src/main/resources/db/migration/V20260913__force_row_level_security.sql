-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- PRE-INFRA-05 REMEDIATION GATE: FORCE ROW LEVEL SECURITY ENFORCEMENT (V20260913)
-- Ensures table owners and non-superuser application roles cannot bypass RLS.
-- ====================================================================================

DO $$
DECLARE
    t text;
    tenant_tables text[] := ARRAY[
        -- Module 00-11 Core Tables (V1)
        'users', 'domain_activity_events', 'idempotency_keys', 'customers',
        'customer_addresses', 'customer_contacts', 'inquiries', 'inquiry_items',
        'quotations', 'quotation_items', 'orders', 'order_items', 'design_projects',
        'design_artworks', 'production_jobs', 'job_stages', 'stage_outputs',
        'qc_inspections', 'inventory_products', 'warehouses', 'location_bins',
        'inventory_stock_lots', 'stock_movement_ledger', 'delivery_orders',
        'delivery_challans', 'delivery_challan_items', 'proof_of_deliveries',
        'accounting_periods', 'customer_receivables', 'customer_payments',
        'financial_transactions', 'journal_lines', 'communications', 'task_items',
        'return_requests', 'return_items', 'return_inspections', 'return_settlements',
        'return_exceptions',

        -- Auth & Session Tables (V20260830)
        'auth_accounts', 'auth_sessions', 'auth_audit_events',

        -- Identity Lifecycle Tables (V20260901)
        'user_profiles', 'user_verification_tokens', 'password_history',

        -- Persistent Event Store & Outbox (V20260905)
        'event_store', 'event_outbox', 'event_processing_records', 'event_dead_letters',

        -- Integration Delivery (V20260906)
        'integration_delivery_records',

        -- Background Jobs (V20260907)
        'background_jobs', 'job_executions', 'job_schedules', 'job_dependencies', 'job_dead_letters',

        -- Workflow Orchestration & Approvals (V20260908)
        'workflow_definitions', 'workflow_versions', 'workflow_instances', 'workflow_steps',
        'workflow_step_executions', 'workflow_transitions', 'workflow_variables',
        'workflow_compensations', 'workflow_approval_policies', 'workflow_approval_requests',
        'workflow_approval_decisions', 'workflow_escalations',

        -- Notification Security (V20260910)
        'notification_security_audit', 'notification_suppressions', 'notification_rate_limit_state',

        -- AI Agent Boundary (V20260911)
        'ai_notification_action_records', 'ai_notification_confirmations', 'ai_notification_audit',

        -- Observability Alerts (V20260912)
        'operational_alerts'
    ];
BEGIN
    FOREACH t IN ARRAY tenant_tables
    LOOP
        IF to_regclass(t) IS NOT NULL THEN
            EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY;', t);
        END IF;
    END LOOP;
END $$;
