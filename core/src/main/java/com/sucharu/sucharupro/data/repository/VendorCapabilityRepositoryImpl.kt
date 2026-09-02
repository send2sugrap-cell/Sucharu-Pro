package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorCapabilityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityStatus
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.VendorCapability
import com.sucharu.sucharupro.domain.repository.VendorCapabilityRepository
import kotlinx.coroutines.flow.Flow

class VendorCapabilityRepositoryImpl(
    private val dataSource: VendorCapabilityDataSource
) : VendorCapabilityRepository {

    override fun observeCapabilities(projectId: String, vendorId: String): Flow<List<VendorCapability>> {
        return dataSource.observeCapabilities(projectId, vendorId)
    }

    override suspend fun findById(projectId: String, capabilityId: String): DomainResult<VendorCapability> {
        return dataSource.findById(projectId, capabilityId)
    }

    override suspend fun findByVendorAndType(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<VendorCapability> {
        return dataSource.findByVendorAndType(projectId, vendorId, capabilityType)
    }

    override suspend fun existsByVendorAndType(projectId: String, vendorId: String, capabilityType: CapabilityType): Boolean {
        return dataSource.existsByVendorAndType(projectId, vendorId, capabilityType)
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, status: CapabilityStatus?): DomainResult<List<VendorCapability>> {
        return dataSource.listByVendor(projectId, vendorId, status)
    }

    override suspend fun listVendorsByCapability(projectId: String, capabilityType: CapabilityType, status: CapabilityStatus?): DomainResult<List<String>> {
        return dataSource.listVendorsByCapability(projectId, capabilityType, status)
    }

    override suspend fun createCapability(capability: VendorCapability): DomainResult<VendorCapability> {
        return dataSource.createCapability(capability)
    }

    override suspend fun updateCapability(capability: VendorCapability): DomainResult<VendorCapability> {
        return dataSource.updateCapability(capability)
    }

    override suspend fun updateStatus(projectId: String, capabilityId: String, status: CapabilityStatus, updatedBy: String): DomainResult<VendorCapability> {
        return dataSource.updateStatus(projectId, capabilityId, status, updatedBy)
    }
}
