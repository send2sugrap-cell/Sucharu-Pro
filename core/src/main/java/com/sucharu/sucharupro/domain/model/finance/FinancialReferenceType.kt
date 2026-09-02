package com.sucharu.sucharupro.domain.model.finance

/**
 * Originating business aggregate entity reference for financial events (Module 09 Step 01 - Step 07).
 */
enum class FinancialReferenceType(val defaultLabel: String) {
    ORDER("Commercial Order"),
    QUOTATION("Commercial Quotation"),
    INVOICE("Customer Invoice"),
    DELIVERY("Delivery / Dispatch"),
    PAYMENT("Payment Transaction"),
    RECEIVABLE("Customer Receivable"),
    PURCHASE("Purchase Obligation"),
    PURCHASE_ORDER("Purchase Order"),
    SUPPLIER_INVOICE("Supplier Invoice"),
    VENDOR_BILL("Vendor Bill"),
    PAYABLE("Vendor Payable"),
    STOCK_RECEIPT("Stock Receiving / GRN"),
    EXPENSE("Expense Voucher"),
    SUPPLIER_PAYMENT("Supplier Payment"),
    REFUND("Refund Note"),
    ADJUSTMENT("Audit / Balance Adjustment"),
    MANUAL("Direct Journal Entry")
}
