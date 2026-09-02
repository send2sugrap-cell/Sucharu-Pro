package com.sucharu.sucharupro.domain.model.payment

/**
 * Financial settlement states for orders and customer invoices.
 */
enum class PaymentStatusType(val defaultLabel: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    UNPAID("Unpaid"),
    OVERDUE("Overdue")
}
