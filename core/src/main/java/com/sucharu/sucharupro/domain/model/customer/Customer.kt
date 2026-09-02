package com.sucharu.sucharupro.domain.model.customer

/**
 * Master entity representing a Customer in Sucharu Pro Printing ERP.
 *
 * Represents both individual clients and institutional/corporate accounts.
 *
 * Architectural Boundary:
 *  - Customer stores stable profile, identity, contact, address, and credit rules.
 *  - Customer does NOT own transactional lists (orders, jobs, invoices, challans).
 *  - Future transactional entities reference this customer via [customerId].
 */
data class Customer(
    /** Unique internal identifier (UUID or database ID). */
    val customerId: String,

    /** Human-friendly customer code (e.g., "CUS-000101"). */
    val customerCode: String,

    /** Display name of the customer or organization (e.g., "Bengal Software Ltd." or "Md. Abdullah Rahman"). */
    val displayName: String,

    /** Business entity classification. */
    val customerType: CustomerType = CustomerType.INDIVIDUAL,

    /** Current operational status. */
    val status: CustomerStatusType = CustomerStatusType.ACTIVE,

    /** Primary contact telephone / mobile number (e.g., "+880 1711-234567"). */
    val primaryPhone: String,

    /** Optional secondary / backup phone number. */
    val alternatePhone: String? = null,

    /** Optional email address. */
    val email: String? = null,

    /** Contact person name for corporate/institutional clients. */
    val contactPersonName: String? = null,

    /** Physical and correspondence addresses. */
    val addresses: List<CustomerAddress> = emptyList(),

    /** Credit terms and credit limit configuration. */
    val creditProfile: CustomerCreditProfile = CustomerCreditProfile.DEFAULT_CASH_ONLY,

    /** Optional affiliate ID if referred through an affiliate partner. */
    val affiliateId: String? = null,

    /** Optional referral / promo code used during customer onboarding. */
    val referralCode: String? = null,

    /** Internal business notes, remarks, or special handling instructions. */
    val notes: String? = null,

    /** ISO 8601 creation timestamp. */
    val createdAt: String,

    /** ISO 8601 last update timestamp. */
    val updatedAt: String,

    /** ISO 8601 timestamp of last operational customer activity. */
    val lastActivityAt: String? = null,

    /** Optional ISO 8601 target timestamp for next follow-up. */
    val nextFollowUpAt: String? = null
) {
    init {
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(customerCode.isNotBlank()) { "Customer code cannot be blank." }
        require(displayName.isNotBlank()) { "Display name cannot be blank." }
        require(primaryPhone.isNotBlank()) { "Primary phone cannot be blank." }
    }

    /** Helper to get the default or primary address if available. */
    val primaryAddress: CustomerAddress?
        get() = addresses.firstOrNull { it.isDefault || it.addressType == CustomerAddressType.PRIMARY }
            ?: addresses.firstOrNull()
}
