-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- CANONICAL POSTGRESQL RELATIONAL SCHEMA (V1)
-- Modules 00-11 Comprehensive Multi-Tenant DDL Specification
-- ====================================================================================

-- ------------------------------------------------------------------------------------
-- 0. EXTENSIONS & BASE SCHEMA
-- ------------------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------------------------------
-- MODULE 00: TENANTS, USERS, INFRASTRUCTURE & AUDIT
-- ------------------------------------------------------------------------------------

CREATE TABLE tenants (
    project_id VARCHAR(36) PRIMARY KEY,
    tenant_name VARCHAR(100) NOT NULL,
    company_code VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED')),
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    user_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    username VARCHAR(50) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'ACCOUNTS', 'QC_INSPECTOR', 'WAREHOUSE', 'STAFF', 'CUSTOMER', 'VENDOR')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE domain_activity_events (
    event_id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    actor_id VARCHAR(50) NOT NULL,
    actor_role VARCHAR(50),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    idempotency_key VARCHAR(128),
    correlation_id VARCHAR(36)
);

CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(128) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    endpoint_action VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64),
    response_payload JSONB NOT NULL,
    status_code INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (project_id, idempotency_key)
);

-- ------------------------------------------------------------------------------------
-- MODULE 01: CUSTOMER MANAGEMENT
-- ------------------------------------------------------------------------------------

CREATE TABLE customers (
    customer_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    customer_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    customer_type VARCHAR(30) NOT NULL DEFAULT 'INDIVIDUAL' CHECK (customer_type IN ('INDIVIDUAL', 'CORPORATE', 'INSTITUTIONAL', 'AGENCY')),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLACKLISTED')),
    primary_phone VARCHAR(30) NOT NULL,
    alternate_phone VARCHAR(30),
    email VARCHAR(100),
    contact_person_name VARCHAR(100),
    credit_limit_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    credit_limit_currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    credit_days INT NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, customer_id),
    UNIQUE (project_id, customer_code)
);

CREATE TABLE customer_addresses (
    address_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    address_type VARCHAR(30) NOT NULL DEFAULT 'BILLING' CHECK (address_type IN ('BILLING', 'SHIPPING', 'REGISTERED', 'FACTORY')),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(50) NOT NULL DEFAULT 'Bangladesh',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, address_id),
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE CASCADE
);

CREATE TABLE customer_contacts (
    contact_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    designation VARCHAR(100),
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(100),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, contact_id),
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------------------------
-- MODULE 02: COMMERCIAL MANAGEMENT (INQUIRIES & QUOTATIONS)
-- ------------------------------------------------------------------------------------

CREATE TABLE inquiries (
    inquiry_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    inquiry_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'QUOTED', 'ACCEPTED', 'CANCELLED', 'CLOSED')),
    notes TEXT,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, inquiry_id),
    UNIQUE (project_id, inquiry_number),
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE inquiry_items (
    inquiry_item_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    inquiry_id VARCHAR(50) NOT NULL,
    item_title VARCHAR(200) NOT NULL,
    specifications TEXT,
    quantity INT NOT NULL CHECK (quantity > 0),
    target_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, inquiry_item_id),
    FOREIGN KEY (project_id, inquiry_id) REFERENCES inquiries(project_id, inquiry_id) ON DELETE CASCADE
);

CREATE TABLE quotations (
    quotation_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    quotation_number VARCHAR(50) NOT NULL,
    inquiry_id VARCHAR(50),
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'EXPIRED', 'CONVERTED')),
    subtotal_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    valid_until TIMESTAMPTZ,
    notes TEXT,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, quotation_id),
    UNIQUE (project_id, quotation_number),
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE quotation_items (
    quotation_item_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    quotation_id VARCHAR(50) NOT NULL,
    item_description VARCHAR(255) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    specifications_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, quotation_item_id),
    FOREIGN KEY (project_id, quotation_id) REFERENCES quotations(project_id, quotation_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------------------------
-- MODULE 03: ORDER MANAGEMENT
-- ------------------------------------------------------------------------------------

CREATE TABLE orders (
    order_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    order_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    quotation_id VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('DRAFT', 'CONFIRMED', 'IN_DESIGN', 'IN_PRODUCTION', 'QC_PENDING', 'READY_FOR_DELIVERY', 'PARTIALLY_DELIVERED', 'DELIVERED', 'COMPLETED', 'CANCELLED')),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    subtotal_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    delivery_deadline TIMESTAMPTZ,
    job_handoff_status VARCHAR(30) NOT NULL DEFAULT 'NOT_READY',
    notes TEXT,
    confirmed_by VARCHAR(50),
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, order_id),
    UNIQUE (project_id, order_number),
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE order_items (
    order_item_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    product_title VARCHAR(200) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    line_subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    specifications_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, order_item_id),
    FOREIGN KEY (project_id, order_id) REFERENCES orders(project_id, order_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------------------------
-- MODULE 04: DESIGN & PRE-PRESS MANAGEMENT
-- ------------------------------------------------------------------------------------

CREATE TABLE design_projects (
    design_project_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    order_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'PROOF_SUBMITTED', 'REVISION_REQUESTED', 'APPROVED', 'REJECTED', 'PREPRESS_READY')),
    designer_id VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, design_project_id),
    FOREIGN KEY (project_id, order_id) REFERENCES orders(project_id, order_id) ON DELETE RESTRICT,
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE design_artworks (
    artwork_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    design_project_id VARCHAR(50) NOT NULL,
    version_number INT NOT NULL DEFAULT 1,
    file_uri VARCHAR(500) NOT NULL,
    file_checksum VARCHAR(64),
    approval_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    approved_by VARCHAR(50),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, artwork_id),
    FOREIGN KEY (project_id, design_project_id) REFERENCES design_projects(project_id, design_project_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------------------------
-- MODULE 05: PRODUCTION & JOB WORKFLOW
-- ------------------------------------------------------------------------------------

CREATE TABLE production_jobs (
    job_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    job_number VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'CANCELLED', 'REWORK_REQUIRED')),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    planned_quantity INT NOT NULL CHECK (planned_quantity > 0),
    produced_quantity INT NOT NULL DEFAULT 0,
    waste_quantity INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, job_id),
    UNIQUE (project_id, job_number),
    FOREIGN KEY (project_id, order_id) REFERENCES orders(project_id, order_id) ON DELETE RESTRICT
);

CREATE TABLE job_stages (
    stage_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    job_id VARCHAR(50) NOT NULL,
    stage_name VARCHAR(50) NOT NULL CHECK (stage_name IN ('PRE_PRESS', 'OFFSET_PRINTING', 'DIGITAL_PRINTING', 'LAMINATION', 'DIE_CUTTING', 'FOILING', 'BINDING', 'PACKAGING')),
    sequence_order INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    machine_id VARCHAR(50),
    operator_id VARCHAR(50),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, stage_id),
    FOREIGN KEY (project_id, job_id) REFERENCES production_jobs(project_id, job_id) ON DELETE CASCADE
);

CREATE TABLE stage_outputs (
    output_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    stage_id VARCHAR(50) NOT NULL,
    passed_quantity INT NOT NULL DEFAULT 0,
    defective_quantity INT NOT NULL DEFAULT 0,
    recorded_by VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, output_id),
    FOREIGN KEY (project_id, stage_id) REFERENCES job_stages(project_id, stage_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------------------------
-- MODULE 06: QUALITY CONTROL (QC)
-- ------------------------------------------------------------------------------------

CREATE TABLE qc_inspections (
    inspection_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    job_id VARCHAR(50) NOT NULL,
    stage_id VARCHAR(50),
    qc_type VARCHAR(30) NOT NULL CHECK (qc_type IN ('PRE_PRODUCTION', 'IN_PROCESS', 'FINAL_QC', 'RE_QC')),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PASSED', 'REJECTED', 'REWORK_ORDERED', 'WAIVED')),
    inspector_id VARCHAR(50) NOT NULL,
    sampled_quantity INT NOT NULL DEFAULT 0,
    accepted_quantity INT NOT NULL DEFAULT 0,
    rejected_quantity INT NOT NULL DEFAULT 0,
    notes TEXT,
    inspected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, inspection_id),
    FOREIGN KEY (project_id, job_id) REFERENCES production_jobs(project_id, job_id) ON DELETE RESTRICT
);

-- ------------------------------------------------------------------------------------
-- MODULE 07: INVENTORY & STOCK MANAGEMENT
-- ------------------------------------------------------------------------------------

CREATE TABLE inventory_products (
    product_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('PAPER', 'INK', 'PLATE', 'CHEMICAL', 'PACKAGING_MATERIAL', 'FINISHED_GOODS', 'SPARE_PARTS')),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    reorder_level INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, product_id),
    UNIQUE (project_id, product_code)
);

CREATE TABLE warehouses (
    warehouse_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    warehouse_code VARCHAR(50) NOT NULL,
    warehouse_name VARCHAR(100) NOT NULL,
    location_address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, warehouse_id),
    UNIQUE (project_id, warehouse_code)
);

CREATE TABLE location_bins (
    bin_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    bin_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, bin_id),
    UNIQUE (project_id, warehouse_id, bin_code),
    FOREIGN KEY (project_id, warehouse_id) REFERENCES warehouses(project_id, warehouse_id) ON DELETE CASCADE
);

CREATE TABLE inventory_stock_lots (
    lot_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    bin_id VARCHAR(50) NOT NULL,
    lot_number VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    unit_cost NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, lot_id),
    FOREIGN KEY (project_id, product_id) REFERENCES inventory_products(project_id, product_id) ON DELETE RESTRICT,
    FOREIGN KEY (project_id, bin_id) REFERENCES location_bins(project_id, bin_id) ON DELETE RESTRICT
);

CREATE TABLE stock_movement_ledger (
    ledger_id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    product_id VARCHAR(50) NOT NULL,
    lot_id VARCHAR(50),
    from_bin_id VARCHAR(50),
    to_bin_id VARCHAR(50),
    movement_type VARCHAR(40) NOT NULL CHECK (movement_type IN ('PURCHASE_RECEIVE', 'PRODUCTION_ISSUE', 'PRODUCTION_RETURN', 'TRANSFER', 'ADJUSTMENT_IN', 'ADJUSTMENT_OUT', 'SALES_DISPATCH', 'CUSTOMER_RETURN_STOCK_IN', 'SCRAP_WRITE_OFF')),
    quantity INT NOT NULL CHECK (quantity > 0),
    reference_id VARCHAR(50),
    actor_id VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------------------------------
-- MODULE 08: DELIVERY & DISPATCH MANAGEMENT
-- ------------------------------------------------------------------------------------

CREATE TABLE delivery_orders (
    delivery_order_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    order_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SCHEDULED', 'IN_TRANSIT', 'DELIVERED', 'PARTIALLY_DELIVERED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, delivery_order_id),
    FOREIGN KEY (project_id, order_id) REFERENCES orders(project_id, order_id) ON DELETE RESTRICT,
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE delivery_challans (
    challan_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    challan_number VARCHAR(50) NOT NULL,
    delivery_order_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PREPARED' CHECK (status IN ('PREPARED', 'DISPATCHED', 'ACKNOWLEDGED', 'RETURNED', 'CANCELLED')),
    dispatched_at TIMESTAMPTZ,
    dispatched_by VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, challan_id),
    UNIQUE (project_id, challan_number),
    FOREIGN KEY (project_id, delivery_order_id) REFERENCES delivery_orders(project_id, delivery_order_id) ON DELETE RESTRICT
);

CREATE TABLE delivery_challan_items (
    challan_item_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    challan_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, challan_item_id),
    FOREIGN KEY (project_id, challan_id) REFERENCES delivery_challans(project_id, challan_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id, product_id) REFERENCES inventory_products(project_id, product_id) ON DELETE RESTRICT
);

CREATE TABLE proof_of_deliveries (
    pod_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    challan_id VARCHAR(50) NOT NULL,
    receiver_name VARCHAR(100) NOT NULL,
    receiver_phone VARCHAR(30),
    signature_file_uri VARCHAR(500),
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, pod_id),
    FOREIGN KEY (project_id, challan_id) REFERENCES delivery_challans(project_id, challan_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------------------------
-- MODULE 09: FINANCE & GENERAL ACCOUNTING
-- ------------------------------------------------------------------------------------

CREATE TABLE accounting_periods (
    period_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    period_name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'LOCKED', 'CLOSED')),
    locked_by VARCHAR(50),
    locked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, period_id),
    UNIQUE (project_id, period_name)
);

CREATE TABLE customer_receivables (
    receivable_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    customer_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50),
    invoice_number VARCHAR(50),
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    paid_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    due_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    status VARCHAR(30) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'WRITTEN_OFF')),
    due_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, receivable_id),
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE customer_payments (
    payment_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    payment_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CHEQUE', 'MFS_BKASH', 'MFS_NAGAD', 'CREDIT_NOTE')),
    reference_number VARCHAR(100),
    paid_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    received_by VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, payment_id),
    UNIQUE (project_id, payment_number),
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE financial_transactions (
    transaction_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    period_id VARCHAR(50) NOT NULL,
    transaction_number VARCHAR(50) NOT NULL,
    transaction_type VARCHAR(40) NOT NULL CHECK (transaction_type IN ('SALES_INVOICE', 'CUSTOMER_PAYMENT', 'SUPPLIER_BILL', 'SUPPLIER_PAYMENT', 'EXPENSE', 'CREDIT_NOTE', 'REFUND', 'MANUAL_JOURNAL')),
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    notes TEXT,
    posted_by VARCHAR(50) NOT NULL,
    posted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, transaction_id),
    UNIQUE (project_id, transaction_number),
    FOREIGN KEY (project_id, period_id) REFERENCES accounting_periods(project_id, period_id) ON DELETE RESTRICT
);

CREATE TABLE journal_lines (
    line_id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    project_id VARCHAR(36) NOT NULL,
    transaction_id VARCHAR(50) NOT NULL,
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    FOREIGN KEY (project_id, transaction_id) REFERENCES financial_transactions(project_id, transaction_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------------------------
-- MODULE 10: COMMUNICATION & TASKS
-- ------------------------------------------------------------------------------------

CREATE TABLE communications (
    communication_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    channel VARCHAR(30) NOT NULL CHECK (channel IN ('INTERNAL', 'CUSTOMER_SMS', 'CUSTOMER_EMAIL', 'VENDOR_EMAIL', 'VENDOR_PORTAL', 'SYSTEM_NOTIFICATION')),
    sender_id VARCHAR(50) NOT NULL,
    recipient_id VARCHAR(50) NOT NULL,
    subject VARCHAR(200),
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT' CHECK (status IN ('QUEUED', 'SENT', 'DELIVERED', 'READ', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, communication_id)
);

CREATE TABLE task_items (
    task_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED')),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    assignee_id VARCHAR(50),
    due_date TIMESTAMPTZ,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, task_id)
);

-- ------------------------------------------------------------------------------------
-- MODULE 11: RETURN & SETTLEMENT MANAGEMENT
-- ------------------------------------------------------------------------------------

CREATE TABLE return_requests (
    return_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    return_no VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    original_challan_id VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED' CHECK (status IN ('REQUESTED', 'UNDER_INSPECTION', 'APPROVED', 'REJECTED', 'RETURN_RECEIVED', 'PROCESSED', 'SETTLED', 'CANCELLED')),
    reason VARCHAR(50) NOT NULL CHECK (reason IN ('PRINTING_DEFECT', 'COLOR_MISMATCH', 'CUTTING_DEFECT', 'BINDING_DEFECT', 'DAMAGED', 'WRONG_ITEM', 'CUSTOMER_REJECTED', 'OTHER')),
    description TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    requested_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, return_id),
    UNIQUE (project_id, return_no),
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE return_items (
    return_item_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    return_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    original_challan_item_id VARCHAR(50),
    requested_quantity INT NOT NULL CHECK (requested_quantity > 0),
    accepted_quantity INT NOT NULL DEFAULT 0 CHECK (accepted_quantity >= 0),
    rejected_quantity INT NOT NULL DEFAULT 0 CHECK (rejected_quantity >= 0),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, return_item_id),
    FOREIGN KEY (project_id, return_id) REFERENCES return_requests(project_id, return_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id, product_id) REFERENCES inventory_products(project_id, product_id) ON DELETE RESTRICT
);

CREATE TABLE return_inspections (
    inspection_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    return_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    inspected_by VARCHAR(50) NOT NULL,
    inspection_notes TEXT,
    inspected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, inspection_id),
    FOREIGN KEY (project_id, return_id) REFERENCES return_requests(project_id, return_id) ON DELETE RESTRICT
);

CREATE TABLE return_settlements (
    settlement_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    return_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    resolution_type VARCHAR(40) NOT NULL CHECK (resolution_type IN ('CREDIT_NOTE', 'REFUND', 'REPLACEMENT', 'REWORK', 'SCRAP_WRITE_OFF')),
    amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    settled_by VARCHAR(50) NOT NULL,
    settled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    credit_note_id VARCHAR(50),
    replacement_order_id VARCHAR(50),
    rework_job_id VARCHAR(50),
    notes TEXT,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, settlement_id),
    UNIQUE (project_id, return_id),
    FOREIGN KEY (project_id, return_id) REFERENCES return_requests(project_id, return_id) ON DELETE RESTRICT,
    FOREIGN KEY (project_id, customer_id) REFERENCES customers(project_id, customer_id) ON DELETE RESTRICT
);

CREATE TABLE return_exceptions (
    exception_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    return_id VARCHAR(50),
    exception_type VARCHAR(50) NOT NULL CHECK (exception_type IN ('AGING_UNINSPECTED', 'AGING_UNRECEIVED', 'UNSETTLED_PROCESSED', 'HIGH_VALUE_RETURN', 'HIGH_RETURN_RATE', 'SLA_BREACH', 'DATA_INTEGRITY_MISMATCH')),
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'DISMISSED')),
    description TEXT NOT NULL,
    acknowledged_by VARCHAR(50),
    acknowledged_at TIMESTAMPTZ,
    resolved_by VARCHAR(50),
    resolved_at TIMESTAMPTZ,
    resolution_notes TEXT,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, exception_id),
    UNIQUE (project_id, idempotency_key)
);

-- ------------------------------------------------------------------------------------
-- INDEXES & PERFORMANCE OPTIMIZATIONS
-- ------------------------------------------------------------------------------------

CREATE INDEX idx_customers_project_phone ON customers(project_id, primary_phone);
CREATE INDEX idx_inquiries_project_customer ON inquiries(project_id, customer_id, status);
CREATE INDEX idx_quotations_project_customer ON quotations(project_id, customer_id, status);
CREATE INDEX idx_orders_project_status ON orders(project_id, status, created_at DESC);
CREATE INDEX idx_orders_project_customer ON orders(project_id, customer_id);
CREATE INDEX idx_production_jobs_project_status ON production_jobs(project_id, status);
CREATE INDEX idx_qc_inspections_project_job ON qc_inspections(project_id, job_id);
CREATE INDEX idx_stock_movement_project_prod ON stock_movement_ledger(project_id, product_id, occurred_at DESC);
CREATE INDEX idx_stock_lots_project_prod_bin ON inventory_stock_lots(project_id, product_id, bin_id);
CREATE INDEX idx_challans_project_status ON delivery_challans(project_id, status, created_at DESC);
CREATE INDEX idx_financial_tx_project_period ON financial_transactions(project_id, period_id, transaction_type);
CREATE INDEX idx_customer_rec_project_cust ON customer_receivables(project_id, customer_id, status);
CREATE INDEX idx_return_requests_project_status ON return_requests(project_id, status, created_at DESC);
CREATE INDEX idx_return_exceptions_project_status ON return_exceptions(project_id, status, severity);
CREATE INDEX idx_domain_events_project_agg ON domain_activity_events(project_id, aggregate_type, aggregate_id, occurred_at DESC);
CREATE INDEX idx_idempotency_expiry ON idempotency_keys(expires_at);

-- ------------------------------------------------------------------------------------
-- ROW-LEVEL SECURITY (RLS) POLICIES — FAIL-CLOSED MULTI-TENANT ISOLATION
-- ------------------------------------------------------------------------------------

-- Function to safely extract session tenant with strict fallback
CREATE OR REPLACE FUNCTION get_current_tenant_id() RETURNS VARCHAR(36) AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_project_id', true), '');
END;
$$ LANGUAGE plpgsql STABLE;

-- Macro to enable RLS and create uniform tenant policy
DO $$
DECLARE
    t text;
    tenant_tables text[] := ARRAY[
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
        'return_exceptions'
    ];
BEGIN
    FOREACH t IN ARRAY tenant_tables
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation_policy ON %I;', t);
        EXECUTE format(
            'CREATE POLICY tenant_isolation_policy ON %I
             USING (project_id = get_current_tenant_id())
             WITH CHECK (project_id = get_current_tenant_id());',
            t
        );
    END LOOP;
END $$;
