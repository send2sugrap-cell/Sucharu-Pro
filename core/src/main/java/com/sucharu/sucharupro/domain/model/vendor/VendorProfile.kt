package com.sucharu.sucharupro.domain.model.vendor

/**
 * Extended business profile for a Vendor entity (Module 12 Step 02).
 */
data class VendorProfile(
    val vendorId: String,
    val projectId: String,
    val legalName: String? = null,
    val displayName: String,
    val contactPerson: String? = null,
    val primaryPhone: String? = null,
    val alternatePhone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val taxId: String? = null,
    val businessRegistrationNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val version: Long = 1L
)
