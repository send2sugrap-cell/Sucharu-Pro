package com.sucharu.sucharupro.domain.model.finance

/**
 * Business classification of financial adjustments, credit notes, and debit notes (Module 09 Step 07).
 */
enum class FinancialAdjustmentType(val defaultLabel: String, val defaultDirection: FinancialAdjustmentDirection) {
    CUSTOMER_CREDIT_NOTE("Customer Credit Note", FinancialAdjustmentDirection.CREDIT),
    CUSTOMER_REFUND("Customer Refund", FinancialAdjustmentDirection.DEBIT),
    CUSTOMER_BALANCE_ADJUSTMENT("Customer Balance Adjustment", FinancialAdjustmentDirection.CREDIT),
    CUSTOMER_DUE_ADJUSTMENT("Customer Due Adjustment", FinancialAdjustmentDirection.CREDIT),

    VENDOR_DEBIT_NOTE("Vendor Debit Note", FinancialAdjustmentDirection.DEBIT),
    VENDOR_BALANCE_ADJUSTMENT("Vendor Balance Adjustment", FinancialAdjustmentDirection.DEBIT),
    VENDOR_PAYABLE_ADJUSTMENT("Vendor Payable Adjustment", FinancialAdjustmentDirection.DEBIT),

    GENERAL_ADJUSTMENT("General Financial Adjustment", FinancialAdjustmentDirection.CREDIT);

    val isCustomerFacing: Boolean
        get() = this == CUSTOMER_CREDIT_NOTE ||
                this == CUSTOMER_REFUND ||
                this == CUSTOMER_BALANCE_ADJUSTMENT ||
                this == CUSTOMER_DUE_ADJUSTMENT

    val isVendorFacing: Boolean
        get() = this == VENDOR_DEBIT_NOTE ||
                this == VENDOR_BALANCE_ADJUSTMENT ||
                this == VENDOR_PAYABLE_ADJUSTMENT
}
