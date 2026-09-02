package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import com.sucharu.sucharupro.domain.repository.VendorRepository
import kotlinx.coroutines.flow.Flow

/**
 * Production implementation of [VendorRepository] (Module 12 Step 01).
 * Delegates persistence operations to the underlying [VendorDataSource].
 */
class VendorRepositoryImpl(
    private val dataSource: VendorDataSource
) : VendorRepository {

    override fun observeVendors(projectId: String): Flow<List<Vendor>> {
        return dataSource.observeVendors(projectId)
    }

    override suspend fun findById(projectId: String, vendorId: String): DomainResult<Vendor> {
        return dataSource.fetchVendorById(projectId, vendorId)
    }

    override suspend fun findByCode(projectId: String, vendorCode: String): DomainResult<Vendor> {
        return dataSource.fetchVendorByCode(projectId, vendorCode)
    }

    override suspend fun existsByCode(projectId: String, vendorCode: String, excludeVendorId: String?): Boolean {
        return dataSource.existsByCode(projectId, vendorCode, excludeVendorId)
    }

    override suspend fun createVendor(vendor: Vendor): DomainResult<Vendor> {
        return dataSource.insertVendor(vendor)
    }

    override suspend fun updateVendor(vendor: Vendor): DomainResult<Vendor> {
        return dataSource.updateVendor(vendor)
    }

    override suspend fun updateStatus(
        projectId: String,
        vendorId: String,
        status: VendorStatus,
        updatedBy: String
    ): DomainResult<Vendor> {
        return dataSource.updateVendorStatus(projectId, vendorId, status, updatedBy)
    }

    override suspend fun listVendors(
        projectId: String,
        type: VendorType?,
        category: VendorCategory?,
        status: VendorStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<Vendor>> {
        return dataSource.fetchVendors(projectId, type, category, status, limit, offset)
    }
}
