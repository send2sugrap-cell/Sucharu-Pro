package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.ContactType
import com.sucharu.sucharupro.domain.model.vendor.VendorContact
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.repository.VendorContactRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorContactValidator
import java.util.UUID

interface VendorContactService {
    suspend fun listContacts(projectId: String, vendorId: String, activeOnly: Boolean = false): DomainResult<List<VendorContact>>
    suspend fun getContactById(projectId: String, contactId: String): DomainResult<VendorContact>
    suspend fun createContact(
        projectId: String,
        vendorId: String,
        name: String,
        contactType: ContactType = ContactType.PRIMARY,
        designation: String? = null,
        phone: String? = null,
        alternatePhone: String? = null,
        email: String? = null,
        notes: String? = null,
        isPrimary: Boolean = false,
        createdBy: String = "system"
    ): DomainResult<VendorContact>
    suspend fun updateContact(
        projectId: String,
        contactId: String,
        name: String,
        contactType: ContactType = ContactType.PRIMARY,
        designation: String? = null,
        phone: String? = null,
        alternatePhone: String? = null,
        email: String? = null,
        notes: String? = null,
        isPrimary: Boolean = false,
        updatedBy: String = "system"
    ): DomainResult<VendorContact>
    suspend fun updateStatus(projectId: String, contactId: String, active: Boolean, updatedBy: String = "system"): DomainResult<VendorContact>
}

class VendorContactServiceImpl(
    private val vendorRepository: VendorRepository,
    private val contactRepository: VendorContactRepository
) : VendorContactService {

    override suspend fun listContacts(projectId: String, vendorId: String, activeOnly: Boolean): DomainResult<List<VendorContact>> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }
        return contactRepository.listByVendor(pId, vId, activeOnly)
    }

    override suspend fun getContactById(projectId: String, contactId: String): DomainResult<VendorContact> {
        val pId = projectId.trim()
        val cId = contactId.trim()
        if (pId.isBlank() || cId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and contactId cannot be blank."))
        }
        return contactRepository.findById(pId, cId)
    }

    override suspend fun createContact(
        projectId: String,
        vendorId: String,
        name: String,
        contactType: ContactType,
        designation: String?,
        phone: String?,
        alternatePhone: String?,
        email: String?,
        notes: String?,
        isPrimary: Boolean,
        createdBy: String
    ): DomainResult<VendorContact> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }

        val vendor = when (val res = vendorRepository.findById(pId, vId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (vendor.status == VendorStatus.ARCHIVED) {
            return DomainResult.Error(IllegalStateException("Cannot add contact to archived vendor '$vId'."))
        }

        val contact = VendorContact(
            contactId = "cnt_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            vendorId = vId,
            projectId = pId,
            contactType = contactType,
            name = name.trim(),
            designation = designation?.trim()?.takeIf { it.isNotBlank() },
            phone = phone?.trim()?.takeIf { it.isNotBlank() },
            alternatePhone = alternatePhone?.trim()?.takeIf { it.isNotBlank() },
            email = email?.trim()?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            isPrimary = isPrimary,
            active = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = createdBy.trim().ifBlank { "system" },
            updatedBy = createdBy.trim().ifBlank { "system" },
            version = 1L
        )

        val validation = VendorContactValidator.validate(contact)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return contactRepository.createContact(contact)
    }

    override suspend fun updateContact(
        projectId: String,
        contactId: String,
        name: String,
        contactType: ContactType,
        designation: String?,
        phone: String?,
        alternatePhone: String?,
        email: String?,
        notes: String?,
        isPrimary: Boolean,
        updatedBy: String
    ): DomainResult<VendorContact> {
        val pId = projectId.trim()
        val cId = contactId.trim()
        if (pId.isBlank() || cId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and contactId cannot be blank."))
        }

        val existing = when (val res = contactRepository.findById(pId, cId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val updated = existing.copy(
            name = name.trim(),
            contactType = contactType,
            designation = designation?.trim()?.takeIf { it.isNotBlank() } ?: existing.designation,
            phone = phone?.trim()?.takeIf { it.isNotBlank() } ?: existing.phone,
            alternatePhone = alternatePhone?.trim()?.takeIf { it.isNotBlank() } ?: existing.alternatePhone,
            email = email?.trim()?.takeIf { it.isNotBlank() } ?: existing.email,
            notes = notes?.trim()?.takeIf { it.isNotBlank() } ?: existing.notes,
            isPrimary = isPrimary,
            updatedAt = System.currentTimeMillis(),
            updatedBy = updatedBy.trim().ifBlank { "system" },
            version = existing.version
        )

        val validation = VendorContactValidator.validate(updated)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return contactRepository.updateContact(updated)
    }

    override suspend fun updateStatus(projectId: String, contactId: String, active: Boolean, updatedBy: String): DomainResult<VendorContact> {
        val pId = projectId.trim()
        val cId = contactId.trim()
        if (pId.isBlank() || cId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and contactId cannot be blank."))
        }
        return contactRepository.updateStatus(pId, cId, active, updatedBy)
    }
}
