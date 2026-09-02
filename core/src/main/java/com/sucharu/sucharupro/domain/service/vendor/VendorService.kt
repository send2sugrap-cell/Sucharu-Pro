package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType

/**
 * Domain service contract for Vendor Master management (Module 12 Step 01).
 *
 * Coordinates business rules, validation, uniqueness invariants, and aggregate persistence.
 */
interface VendorService {

    suspend fun createVendor(
        projectId: String,
        vendorName: String,
        vendorCode: String? = null,
        legalName: String? = null,
        vendorType: VendorType = VendorType.SERVICE_PROVIDER,
        vendorCategory: VendorCategory = VendorCategory.OTHER,
        status: VendorStatus = VendorStatus.ACTIVE,
        primaryContactName: String? = null,
        primaryPhone: String? = null,
        primaryEmail: String? = null,
        notes: String? = null,
        createdBy: String = "system"
    ): DomainResult<Vendor>

    suspend fun updateVendor(
        projectId: String,
        vendorId: String,
        vendorName: String,
        legalName: String? = null,
        vendorType: VendorType? = null,
        vendorCategory: VendorCategory? = null,
        primaryContactName: String? = null,
        primaryPhone: String? = null,
        primaryEmail: String? = null,
        notes: String? = null,
        updatedBy: String = "system"
    ): DomainResult<Vendor>

    suspend fun updateStatus(
        projectId: String,
        vendorId: String,
        newStatus: VendorStatus,
        updatedBy: String = "system"
    ): DomainResult<Vendor>

    suspend fun getVendorById(projectId: String, vendorId: String): DomainResult<Vendor>

    suspend fun getVendorByCode(projectId: String, vendorCode: String): DomainResult<Vendor>

    suspend fun listVendors(
        projectId: String,
        type: VendorType? = null,
        category: VendorCategory? = null,
        status: VendorStatus? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<Vendor>>
}
