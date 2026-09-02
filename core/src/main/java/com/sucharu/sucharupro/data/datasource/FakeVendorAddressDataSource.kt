package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorAddressDataSource : VendorAddressDataSource {

    private val addresses = ConcurrentHashMap<String, VendorAddress>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<List<VendorAddress>>>()

    private fun key(projectId: String, addressId: String): String = "$projectId:$addressId"
    private fun vendorKey(projectId: String, vendorId: String): String = "$projectId:$vendorId"

    private fun updateFlow(projectId: String, vendorId: String) {
        val list = addresses.values.filter { it.projectId == projectId && it.vendorId == vendorId }
        flows[vendorKey(projectId, vendorId)]?.value = list
    }

    override fun observeAddresses(projectId: String, vendorId: String): Flow<List<VendorAddress>> {
        val vk = vendorKey(projectId, vendorId)
        val initial = addresses.values.filter { it.projectId == projectId && it.vendorId == vendorId }
        return flows.getOrPut(vk) { MutableStateFlow(initial) }.asStateFlow()
    }

    override suspend fun findById(projectId: String, addressId: String): DomainResult<VendorAddress> {
        val a = addresses[key(projectId, addressId)]
        return if (a != null && a.projectId == projectId) {
            DomainResult.Success(a)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor address '$addressId' not found in project '$projectId'."))
        }
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, activeOnly: Boolean): DomainResult<List<VendorAddress>> {
        val list = addresses.values
            .filter { it.projectId == projectId && it.vendorId == vendorId && (!activeOnly || it.active) }
            .sortedWith(compareByDescending<VendorAddress> { it.isPrimary }.thenBy { it.addressType })
        return DomainResult.Success(list)
    }

    override suspend fun createAddress(address: VendorAddress): DomainResult<VendorAddress> {
        val k = key(address.projectId, address.addressId)
        if (addresses.containsKey(k)) {
            return DomainResult.Error(IllegalStateException("Address '${address.addressId}' already exists."))
        }
        val saved = address.copy(version = 1L)
        addresses[k] = saved
        updateFlow(saved.projectId, saved.vendorId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateAddress(address: VendorAddress): DomainResult<VendorAddress> {
        val k = key(address.projectId, address.addressId)
        val existing = addresses[k] ?: return DomainResult.Error(NoSuchElementException("Address not found."))
        if (existing.version != address.version) {
            return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on address '${address.addressId}'."))
        }
        val saved = address.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        addresses[k] = saved
        updateFlow(saved.projectId, saved.vendorId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateStatus(projectId: String, addressId: String, active: Boolean, updatedBy: String): DomainResult<VendorAddress> {
        val k = key(projectId, addressId)
        val existing = addresses[k] ?: return DomainResult.Error(NoSuchElementException("Address not found."))
        val updated = existing.copy(
            active = active,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1L
        )
        addresses[k] = updated
        updateFlow(updated.projectId, updated.vendorId)
        return DomainResult.Success(updated)
    }
}
