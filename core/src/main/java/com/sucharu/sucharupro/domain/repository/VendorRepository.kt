package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Vendor Master entity (Module 12 Step 01).
 *
 * Guarantees strict multi-tenant project isolation across all queries and mutations.
 */
interface VendorRepository {

    /**
     * Observe reactive stream of vendors within the specified [projectId].
     */
    fun observeVendors(projectId: String): Flow<List<Vendor>>

    /**
     * Retrieve a vendor by its canonical [vendorId] within [projectId].
     */
    suspend fun findById(projectId: String, vendorId: String): DomainResult<Vendor>

    /**
     * Retrieve a vendor by human-readable [vendorCode] within [projectId].
     */
    suspend fun findByCode(projectId: String, vendorCode: String): DomainResult<Vendor>

    /**
     * Checks if a [vendorCode] is already registered within [projectId], optionally excluding [excludeVendorId].
     */
    suspend fun existsByCode(projectId: String, vendorCode: String, excludeVendorId: String? = null): Boolean

    /**
     * Inserts a new canonical vendor record.
     */
    suspend fun createVendor(vendor: Vendor): DomainResult<Vendor>

    /**
     * Updates an existing vendor master record.
     */
    suspend fun updateVendor(vendor: Vendor): DomainResult<Vendor>

    /**
     * Transitions vendor lifecycle status.
     */
    suspend fun updateStatus(
        projectId: String,
        vendorId: String,
        status: VendorStatus,
        updatedBy: String
    ): DomainResult<Vendor>

    /**
     * Lists vendors within [projectId] with optional filtering by [type], [category], and [status].
     */
    suspend fun listVendors(
        projectId: String,
        type: VendorType? = null,
        category: VendorCategory? = null,
        status: VendorStatus? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<Vendor>>
}
