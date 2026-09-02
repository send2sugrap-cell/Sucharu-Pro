package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityStatus
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.VendorCapability
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for VendorCapability (Module 12 Step 02).
 */
interface VendorCapabilityRepository {
    fun observeCapabilities(projectId: String, vendorId: String): Flow<List<VendorCapability>>
    suspend fun findById(projectId: String, capabilityId: String): DomainResult<VendorCapability>
    suspend fun findByVendorAndType(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<VendorCapability>
    suspend fun existsByVendorAndType(projectId: String, vendorId: String, capabilityType: CapabilityType): Boolean
    suspend fun listByVendor(projectId: String, vendorId: String, status: CapabilityStatus? = null): DomainResult<List<VendorCapability>>
    suspend fun listVendorsByCapability(projectId: String, capabilityType: CapabilityType, status: CapabilityStatus? = CapabilityStatus.ACTIVE): DomainResult<List<String>>
    suspend fun createCapability(capability: VendorCapability): DomainResult<VendorCapability>
    suspend fun updateCapability(capability: VendorCapability): DomainResult<VendorCapability>
    suspend fun updateStatus(projectId: String, capabilityId: String, status: CapabilityStatus, updatedBy: String): DomainResult<VendorCapability>
}
