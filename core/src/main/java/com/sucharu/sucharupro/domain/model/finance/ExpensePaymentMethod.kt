package com.sucharu.sucharupro.domain.model.finance

/**
 * Payment disbursement channels for business operational expenses (Module 09 Step 06).
 */
enum class ExpensePaymentMethod(val defaultLabel: String, val requiresReference: Boolean) {
    CASH("Cash in Hand", false),
    BANK_TRANSFER("Bank Transfer / EFT", true),
    CHEQUE("Bank Cheque", true),
    MOBILE_BANKING("Mobile Banking (bKash/Nagad/Rocket)", true),
    CARD("Corporate Debit/Credit Card", true),
    OTHER("Other Payment Method", true)
}
