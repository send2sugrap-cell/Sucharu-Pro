package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorContactDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorContact
import com.sucharu.sucharupro.domain.repository.VendorContactRepository
import kotlinx.coroutines.flow.Flow

class VendorContactRepositoryImpl(
    private val dataSource: VendorContactDataSource
) : VendorContactRepository {

    override fun observeContacts(projectId: String, vendorId: String): Flow<List<VendorContact>> {
        return dataSource.observeContacts(projectId, vendorId)
    }

    override suspend fun findById(projectId: String, contactId: String): DomainResult<VendorContact> {
        return dataSource.findById(projectId, contactId)
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, activeOnly: Boolean): DomainResult<List<VendorContact>> {
        return dataSource.listByVendor(projectId, vendorId, activeOnly)
    }

    override suspend fun createContact(contact: VendorContact): DomainResult<VendorContact> {
        return dataSource.createContact(contact)
    }

    override suspend fun updateContact(contact: VendorContact): DomainResult<VendorContact> {
        return dataSource.updateContact(contact)
    }

    override suspend fun updateStatus(projectId: String, contactId: String, active: Boolean, updatedBy: String): DomainResult<VendorContact> {
        return dataSource.updateStatus(projectId, contactId, active, updatedBy)
    }
}
