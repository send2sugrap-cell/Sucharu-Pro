package com.sucharu.sucharupro.domain.model.finance

/**
 * Payment disbursement channels for supplier liabilities (Module 09 Step 05).
 */
enum class SupplierPaymentMethod(val defaultLabel: String, val requiresReference: Boolean) {
    CASH("Cash in Hand", false),
    BANK_TRANSFER("Bank Transfer / EFT", true),
    CHEQUE("Bank Cheque", true),
    MOBILE_BANKING("Mobile Banking (bKash/Nagad/Rocket)", true),
    CARD("Corporate Card", true),
    OTHER("Other Payment Method", true)
}
