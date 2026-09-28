package com.sucharu.sucharupro.domain.model.finance

/**
 * Standard payment methods supported for customer receipts (Module 09 Step 03 & Static Bangla QR Integration).
 */
enum class CustomerPaymentMethod(val defaultLabel: String, val requiresReference: Boolean) {
    CASH("Cash", false),
    BANK_TRANSFER("Bank Transfer / EFT", true),
    CHEQUE("Cheque", true),
    MOBILE_BANKING("Mobile Banking (bKash/Nagad/Rocket)", true),
    CARD("Debit / Credit Card (POS)", true),
    BANGLA_QR("Bangla QR (BRAC Bank)", true),
    OTHER("Other Payment Method", false)
}
