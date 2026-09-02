package com.sucharu.sucharupro.domain.model.vendor

/**
 * Technical service or production capability possessed by a Vendor (Module 12 Step 02).
 */
data class VendorCapability(
    val capabilityId: String,
    val vendorId: String,
    val projectId: String,
    val capabilityType: CapabilityType,
    val displayName: String = capabilityType.name,
    val status: CapabilityStatus = CapabilityStatus.ACTIVE,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val version: Long = 1L
)
