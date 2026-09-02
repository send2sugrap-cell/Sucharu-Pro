package com.sucharu.sucharupro.domain.model.vendor

/**
 * Contact person associated with a Vendor (Module 12 Step 02).
 */
data class VendorContact(
    val contactId: String,
    val vendorId: String,
    val projectId: String,
    val contactType: ContactType = ContactType.PRIMARY,
    val name: String,
    val designation: String? = null,
    val phone: String? = null,
    val alternatePhone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val isPrimary: Boolean = false,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val version: Long = 1L
)
