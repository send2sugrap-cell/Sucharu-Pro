package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Vendor Master persistence (Module 12 Step 01).
 */
interface VendorDataSource {
    fun observeVendors(projectId: String): Flow<List<Vendor>>
    suspend fun fetchVendors(projectId: String, type: VendorType? = null, category: VendorCategory? = null, status: VendorStatus? = null, limit: Int = 100, offset: Int = 0): DomainResult<List<Vendor>>
    suspend fun fetchVendorById(projectId: String, vendorId: String): DomainResult<Vendor>
    suspend fun fetchVendorByCode(projectId: String, vendorCode: String): DomainResult<Vendor>
    suspend fun existsByCode(projectId: String, vendorCode: String, excludeVendorId: String? = null): Boolean
    suspend fun insertVendor(vendor: Vendor): DomainResult<Vendor>
    suspend fun updateVendor(vendor: Vendor): DomainResult<Vendor>
    suspend fun updateVendorStatus(projectId: String, vendorId: String, status: VendorStatus, updatedBy: String): DomainResult<Vendor>
}
