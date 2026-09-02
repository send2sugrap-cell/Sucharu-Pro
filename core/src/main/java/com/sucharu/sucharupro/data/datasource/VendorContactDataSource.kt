package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorContact
import kotlinx.coroutines.flow.Flow

interface VendorContactDataSource {
    fun observeContacts(projectId: String, vendorId: String): Flow<List<VendorContact>>
    suspend fun findById(projectId: String, contactId: String): DomainResult<VendorContact>
    suspend fun listByVendor(projectId: String, vendorId: String, activeOnly: Boolean = false): DomainResult<List<VendorContact>>
    suspend fun createContact(contact: VendorContact): DomainResult<VendorContact>
    suspend fun updateContact(contact: VendorContact): DomainResult<VendorContact>
    suspend fun updateStatus(projectId: String, contactId: String, active: Boolean, updatedBy: String): DomainResult<VendorContact>
}
