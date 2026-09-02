-- ====================================================================================
-- SUCHARU PRO COMMERCIAL PRINTING ERP
-- CANONICAL POSTGRESQL PERSISTENCE MIGRATION (V20260824)
-- Module Performance Indexes & Deferred Journal Balance Invariant
-- ====================================================================================

-- 1. INDEXES
-- Index for tenant-scoped active customer lookups (Module 01)
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(project_id, status);

-- Index for tenant-scoped reorder level and low-stock quantity alerts (Module 07)
CREATE INDEX IF NOT EXISTS idx_inventory_stock_quantity ON inventory_stock_lots(project_id, quantity);

-- 2. JOURNAL BALANCE CHECK TRIGGER
-- Function to verify that total debit equals total credit for each financial transaction at commit boundary
CREATE OR REPLACE FUNCTION fn_check_journal_balance() RETURNS trigger AS $$
DECLARE
    v_tx_id VARCHAR(50);
    v_proj_id VARCHAR(36);
    debit_sum NUMERIC(15,2);
    credit_sum NUMERIC(15,2);
BEGIN
    v_tx_id := COALESCE(NEW.transaction_id, OLD.transaction_id);
    v_proj_id := COALESCE(NEW.project_id, OLD.project_id);

    SELECT COALESCE(SUM(amount), 0) INTO debit_sum FROM journal_lines
    WHERE transaction_id = v_tx_id AND entry_type = 'DEBIT' AND project_id = v_proj_id;

    SELECT COALESCE(SUM(amount), 0) INTO credit_sum FROM journal_lines
    WHERE transaction_id = v_tx_id AND entry_type = 'CREDIT' AND project_id = v_proj_id;

    IF debit_sum <> credit_sum THEN
        RAISE EXCEPTION 'Journal imbalance for transaction %: debits % vs credits %', v_tx_id, debit_sum, credit_sum;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Constraint Trigger on journal_lines evaluated at transaction commit boundary (initially deferred)
DROP TRIGGER IF EXISTS trg_check_journal_balance ON journal_lines;
CREATE CONSTRAINT TRIGGER trg_check_journal_balance
AFTER INSERT OR UPDATE OR DELETE ON journal_lines
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_check_journal_balance();
