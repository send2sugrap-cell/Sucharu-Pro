package com.sucharu.sucharupro.domain.model.order

/**
 * Commercial payment term categories for Quotations and Orders in Sucharu Pro.
 *
 * Defines commercial payment conditions (NOT the payment transaction or ledger itself).
 */
enum class PaymentTermType(val defaultLabel: String) {
    /** 100% full payment in advance before production/dispatch. */
    FULL_ADVANCE("100% Full Advance"),

    /** Agreed percentage deposit in advance, balance on delivery. */
    PARTIAL_ADVANCE("Partial Advance (Deposit)"),

    /** Full payment upon delivery of goods / challan. */
    ON_DELIVERY("Cash On Delivery (COD)"),

    /** Credit terms with agreed due window in days. */
    CREDIT("Credit Terms"),

    /** Custom negotiated commercial payment schedule. */
    CUSTOM("Custom Payment Terms")
}
