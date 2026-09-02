package com.sucharu.sucharupro.domain.model.vendor

/**
 * Operational state of a Vendor's capability (Module 12 Step 02).
 */
enum class CapabilityStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED;

    val isActive: Boolean
        get() = this == ACTIVE

    val isSelectable: Boolean
        get() = this == ACTIVE
}
