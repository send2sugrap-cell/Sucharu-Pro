package com.sucharu.sucharupro.domain.model.customer

/**
 * Operational status of a customer record in Sucharu Pro.
 *
 * Tracks customer lifecycle and trade permission:
 *  - ACTIVE: Normal active customer eligible for new orders and transactions.
 *  - INACTIVE: Temporarily inactive customer.
 *  - BLOCKED: Restricted customer (e.g. overdue default); cannot place new orders.
 *  - ARCHIVED: Soft-deleted / historical record preserved for audit and ledger integrity.
 */
enum class CustomerStatusType(val defaultLabel: String) {
    /** Normal customer eligible for all transactions. */
    ACTIVE("Active"),

    /** Temporarily inactive or dormant customer. */
    INACTIVE("Inactive"),

    /** Blocked from placing new orders or credit transactions (e.g., severe overdue). */
    BLOCKED("Blocked"),

    /** Archived / soft-deleted record preserved for historical and accounting records. */
    ARCHIVED("Archived")
}
