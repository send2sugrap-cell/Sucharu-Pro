package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorContactDataSource : VendorContactDataSource {

    private val contacts = ConcurrentHashMap<String, VendorContact>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<List<VendorContact>>>()

    private fun key(projectId: String, contactId: String): String = "$projectId:$contactId"
    private fun vendorKey(projectId: String, vendorId: String): String = "$projectId:$vendorId"

    private fun updateFlow(projectId: String, vendorId: String) {
        val list = contacts.values.filter { it.projectId == projectId && it.vendorId == vendorId }
        flows[vendorKey(projectId, vendorId)]?.value = list
    }

    override fun observeContacts(projectId: String, vendorId: String): Flow<List<VendorContact>> {
        val vk = vendorKey(projectId, vendorId)
        val initial = contacts.values.filter { it.projectId == projectId && it.vendorId == vendorId }
        return flows.getOrPut(vk) { MutableStateFlow(initial) }.asStateFlow()
    }

    override suspend fun findById(projectId: String, contactId: String): DomainResult<VendorContact> {
        val c = contacts[key(projectId, contactId)]
        return if (c != null && c.projectId == projectId) {
            DomainResult.Success(c)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor contact '$contactId' not found in project '$projectId'."))
        }
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, activeOnly: Boolean): DomainResult<List<VendorContact>> {
        val list = contacts.values
            .filter { it.projectId == projectId && it.vendorId == vendorId && (!activeOnly || it.active) }
            .sortedWith(compareByDescending<VendorContact> { it.isPrimary }.thenBy { it.name })
        return DomainResult.Success(list)
    }

    override suspend fun createContact(contact: VendorContact): DomainResult<VendorContact> {
        val k = key(contact.projectId, contact.contactId)
        if (contacts.containsKey(k)) {
            return DomainResult.Error(IllegalStateException("Contact '${contact.contactId}' already exists."))
        }
        val saved = contact.copy(version = 1L)
        contacts[k] = saved
        updateFlow(saved.projectId, saved.vendorId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateContact(contact: VendorContact): DomainResult<VendorContact> {
        val k = key(contact.projectId, contact.contactId)
        val existing = contacts[k] ?: return DomainResult.Error(NoSuchElementException("Contact not found."))
        if (existing.version != contact.version) {
            return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on contact '${contact.contactId}'."))
        }
        val saved = contact.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        contacts[k] = saved
        updateFlow(saved.projectId, saved.vendorId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateStatus(projectId: String, contactId: String, active: Boolean, updatedBy: String): DomainResult<VendorContact> {
        val k = key(projectId, contactId)
        val existing = contacts[k] ?: return DomainResult.Error(NoSuchElementException("Contact not found."))
        val updated = existing.copy(
            active = active,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1L
        )
        contacts[k] = updated
        updateFlow(updated.projectId, updated.vendorId)
        return DomainResult.Success(updated)
    }
}
