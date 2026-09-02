package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorAddressDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorAddress
import com.sucharu.sucharupro.domain.repository.VendorAddressRepository
import kotlinx.coroutines.flow.Flow

class VendorAddressRepositoryImpl(
    private val dataSource: VendorAddressDataSource
) : VendorAddressRepository {

    override fun observeAddresses(projectId: String, vendorId: String): Flow<List<VendorAddress>> {
        return dataSource.observeAddresses(projectId, vendorId)
    }

    override suspend fun findById(projectId: String, addressId: String): DomainResult<VendorAddress> {
        return dataSource.findById(projectId, addressId)
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, activeOnly: Boolean): DomainResult<List<VendorAddress>> {
        return dataSource.listByVendor(projectId, vendorId, activeOnly)
    }

    override suspend fun createAddress(address: VendorAddress): DomainResult<VendorAddress> {
        return dataSource.createAddress(address)
    }

    override suspend fun updateAddress(address: VendorAddress): DomainResult<VendorAddress> {
        return dataSource.updateAddress(address)
    }

    override suspend fun updateStatus(projectId: String, addressId: String, active: Boolean, updatedBy: String): DomainResult<VendorAddress> {
        return dataSource.updateStatus(projectId, addressId, active, updatedBy)
    }
}
