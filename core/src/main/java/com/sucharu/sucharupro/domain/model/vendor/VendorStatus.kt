package com.sucharu.sucharupro.domain.model.vendor

/**
 * Operational lifecycle status for a Vendor master entity (Module 12 Step 01).
 */
enum class VendorStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    INACTIVE,
    ARCHIVED;

    val isActive: Boolean get() = this == ACTIVE
    val isModifiable: Boolean get() = this != ARCHIVED
}
