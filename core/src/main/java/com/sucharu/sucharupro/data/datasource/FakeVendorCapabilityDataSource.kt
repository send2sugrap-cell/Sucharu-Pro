package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityStatus
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.VendorCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorCapabilityDataSource : VendorCapabilityDataSource {

    private val capabilities = ConcurrentHashMap<String, VendorCapability>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<List<VendorCapability>>>()

    private fun key(projectId: String, capabilityId: String): String = "$projectId:$capabilityId"
    private fun vendorKey(projectId: String, vendorId: String): String = "$projectId:$vendorId"

    private fun updateFlow(projectId: String, vendorId: String) {
        val list = capabilities.values.filter { it.projectId == projectId && it.vendorId == vendorId }
        flows[vendorKey(projectId, vendorId)]?.value = list
    }

    override fun observeCapabilities(projectId: String, vendorId: String): Flow<List<VendorCapability>> {
        val vk = vendorKey(projectId, vendorId)
        val initial = capabilities.values.filter { it.projectId == projectId && it.vendorId == vendorId }
        return flows.getOrPut(vk) { MutableStateFlow(initial) }.asStateFlow()
    }

    override suspend fun findById(projectId: String, capabilityId: String): DomainResult<VendorCapability> {
        val cap = capabilities[key(projectId, capabilityId)]
        return if (cap != null && cap.projectId == projectId) {
            DomainResult.Success(cap)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor capability '$capabilityId' not found in project '$projectId'."))
        }
    }

    override suspend fun findByVendorAndType(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<VendorCapability> {
        val cap = capabilities.values.find { it.projectId == projectId && it.vendorId == vendorId && it.capabilityType == capabilityType }
        return if (cap != null) {
            DomainResult.Success(cap)
        } else {
            DomainResult.Error(NoSuchElementException("Capability '${capabilityType.name}' not found for vendor '$vendorId'."))
        }
    }

    override suspend fun existsByVendorAndType(projectId: String, vendorId: String, capabilityType: CapabilityType): Boolean {
        return capabilities.values.any { it.projectId == projectId && it.vendorId == vendorId && it.capabilityType == capabilityType }
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, status: CapabilityStatus?): DomainResult<List<VendorCapability>> {
        val list = capabilities.values
            .filter { it.projectId == projectId && it.vendorId == vendorId && (status == null || it.status == status) }
            .sortedBy { it.displayName }
        return DomainResult.Success(list)
    }

    override suspend fun listVendorsByCapability(projectId: String, capabilityType: CapabilityType, status: CapabilityStatus?): DomainResult<List<String>> {
        val vendorIds = capabilities.values
            .filter { it.projectId == projectId && it.capabilityType == capabilityType && (status == null || it.status == status) }
            .map { it.vendorId }
            .distinct()
        return DomainResult.Success(vendorIds)
    }

    override suspend fun createCapability(capability: VendorCapability): DomainResult<VendorCapability> {
        val k = key(capability.projectId, capability.capabilityId)
        if (capabilities.containsKey(k)) {
            return DomainResult.Error(IllegalStateException("Capability '${capability.capabilityId}' already exists."))
        }
        if (existsByVendorAndType(capability.projectId, capability.vendorId, capability.capabilityType)) {
            return DomainResult.Error(IllegalStateException("Vendor '${capability.vendorId}' already has capability '${capability.capabilityType.name}'."))
        }
        val saved = capability.copy(version = 1L)
        capabilities[k] = saved
        updateFlow(saved.projectId, saved.vendorId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateCapability(capability: VendorCapability): DomainResult<VendorCapability> {
        val k = key(capability.projectId, capability.capabilityId)
        val existing = capabilities[k] ?: return DomainResult.Error(NoSuchElementException("Capability not found."))
        if (existing.version != capability.version) {
            return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on capability '${capability.capabilityId}'."))
        }
        val saved = capability.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        capabilities[k] = saved
        updateFlow(saved.projectId, saved.vendorId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateStatus(projectId: String, capabilityId: String, status: CapabilityStatus, updatedBy: String): DomainResult<VendorCapability> {
        val k = key(projectId, capabilityId)
        val existing = capabilities[k] ?: return DomainResult.Error(NoSuchElementException("Capability not found."))
        val updated = existing.copy(
            status = status,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1L
        )
        capabilities[k] = updated
        updateFlow(updated.projectId, updated.vendorId)
        return DomainResult.Success(updated)
    }
}
