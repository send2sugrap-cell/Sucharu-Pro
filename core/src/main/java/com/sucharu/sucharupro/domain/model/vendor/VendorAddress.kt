package com.sucharu.sucharupro.domain.model.vendor

/**
 * Operating or registered address for a Vendor (Module 12 Step 02).
 */
data class VendorAddress(
    val addressId: String,
    val vendorId: String,
    val projectId: String,
    val addressType: AddressType = AddressType.OFFICE,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String = "Dhaka",
    val district: String? = null,
    val postalCode: String? = null,
    val country: String = "Bangladesh",
    val notes: String? = null,
    val isPrimary: Boolean = false,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val version: Long = 1L
)
