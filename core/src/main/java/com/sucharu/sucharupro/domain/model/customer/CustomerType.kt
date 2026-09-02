package com.sucharu.sucharupro.domain.model.customer

/**
 * Categorization of customers in Sucharu Pro.
 *
 * Defines the business type/entity classification for a customer profile.
 */
enum class CustomerType(val defaultLabel: String) {
    /** Individual consumer / retail customer. */
    INDIVIDUAL("Individual"),

    /** Commercial business, enterprise, or corporate client. */
    BUSINESS("Business"),

    /** Wholesale partner, reseller, or printing dealer. */
    DEALER("Dealer"),

    /** VIP / High-Value priority client. */
    VIP("VIP"),

    /** Government or public sector entity. */
    GOVERNMENT("Government"),

    /** Educational, religious, or public institution (madrasa, school, hospital, etc.). */
    INSTITUTION("Institution"),

    /** Non-profit, NGO, foundation, or club organization. */
    ORGANIZATION("Organization"),

    /** Other / miscellaneous customer category. */
    OTHER("Other");

    companion object {
        /** Alias for individual consumer / retail customer. */
        val RETAIL: CustomerType get() = INDIVIDUAL
    }
}
