package com.sucharu.sucharupro.domain.model.customer

/**
 * Categorization of customer-level operational lifecycle activities.
 *
 * Scoped strictly to Customer Management events.
 */
enum class CustomerActivityType(val defaultLabel: String) {
    /** Customer account / profile initially registered. */
    CUSTOMER_CREATED("Customer Created"),

    /** Profile details, name, or metadata edited. */
    CUSTOMER_UPDATED("Customer Updated"),

    /** Phone, email, contact person, or address updated. */
    CONTACT_UPDATED("Contact Information Updated"),

    /** Internal customer note recorded. */
    NOTE_ADDED("Note Added"),

    /** Existing customer note edited. */
    NOTE_UPDATED("Note Updated"),

    /** Customer note deleted. */
    NOTE_DELETED("Note Deleted"),

    /** Customer trade status changed (Active, Inactive, Blocked, Archived). */
    STATUS_CHANGED("Status Changed"),

    /** Business entity categorization changed. */
    TYPE_CHANGED("Customer Type Changed"),

    /** Follow-up date scheduled or modified. */
    FOLLOW_UP_SCHEDULED("Follow-up Scheduled"),

    /** Follow-up date cleared. */
    FOLLOW_UP_CLEARED("Follow-up Cleared")
}
