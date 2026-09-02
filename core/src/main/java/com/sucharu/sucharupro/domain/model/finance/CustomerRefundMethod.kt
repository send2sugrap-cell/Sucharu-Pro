package com.sucharu.sucharupro.domain.model.finance

/**
 * Disbursement channel for Customer Refunds (Module 09 Step 07).
 */
enum class CustomerRefundMethod(val defaultLabel: String, val requiresReference: Boolean) {
    CASH("Cash in Hand", false),
    BANK_TRANSFER("Bank Transfer / EFT", true),
    CHEQUE("Bank Cheque", true),
    MOBILE_BANKING("Mobile Banking (bKash/Nagad/Rocket)", true),
    CARD("Credit/Debit Card Refund", true),
    OTHER("Other Disbursement Method", true)
}
