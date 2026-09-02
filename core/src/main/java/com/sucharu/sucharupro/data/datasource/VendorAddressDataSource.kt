package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorAddress
import kotlinx.coroutines.flow.Flow

interface VendorAddressDataSource {
    fun observeAddresses(projectId: String, vendorId: String): Flow<List<VendorAddress>>
    suspend fun findById(projectId: String, addressId: String): DomainResult<VendorAddress>
    suspend fun listByVendor(projectId: String, vendorId: String, activeOnly: Boolean = false): DomainResult<List<VendorAddress>>
    suspend fun createAddress(address: VendorAddress): DomainResult<VendorAddress>
    suspend fun updateAddress(address: VendorAddress): DomainResult<VendorAddress>
    suspend fun updateStatus(projectId: String, addressId: String, active: Boolean, updatedBy: String): DomainResult<VendorAddress>
}
