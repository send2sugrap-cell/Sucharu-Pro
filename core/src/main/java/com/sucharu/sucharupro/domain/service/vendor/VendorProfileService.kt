package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorProfile
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.repository.VendorProfileRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorProfileValidator

interface VendorProfileService {
    suspend fun getProfile(projectId: String, vendorId: String): DomainResult<VendorProfile>
    suspend fun updateProfile(
        projectId: String,
        vendorId: String,
        displayName: String,
        legalName: String? = null,
        contactPerson: String? = null,
        primaryPhone: String? = null,
        alternatePhone: String? = null,
        email: String? = null,
        website: String? = null,
        taxId: String? = null,
        businessRegistrationNumber: String? = null,
        notes: String? = null,
        updatedBy: String = "system"
    ): DomainResult<VendorProfile>
}

class VendorProfileServiceImpl(
    private val vendorRepository: VendorRepository,
    private val profileRepository: VendorProfileRepository
) : VendorProfileService {

    override suspend fun getProfile(projectId: String, vendorId: String): DomainResult<VendorProfile> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }
        return profileRepository.findByVendorId(pId, vId)
    }

    override suspend fun updateProfile(
        projectId: String,
        vendorId: String,
        displayName: String,
        legalName: String?,
        contactPerson: String?,
        primaryPhone: String?,
        alternatePhone: String?,
        email: String?,
        website: String?,
        taxId: String?,
        businessRegistrationNumber: String?,
        notes: String?,
        updatedBy: String
    ): DomainResult<VendorProfile> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }

        // Verify parent vendor exists and is not archived
        val vendor = when (val res = vendorRepository.findById(pId, vId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (vendor.status == VendorStatus.ARCHIVED) {
            return DomainResult.Error(IllegalStateException("Cannot modify profile for archived vendor '$vId'."))
        }

        val existingProfile = when (val res = profileRepository.findByVendorId(pId, vId)) {
            is DomainResult.Success -> res.data
            else -> null
        }

        val profile = VendorProfile(
            vendorId = vId,
            projectId = pId,
            legalName = legalName?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.legalName,
            displayName = displayName.trim().ifBlank { vendor.vendorName },
            contactPerson = contactPerson?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.contactPerson,
            primaryPhone = primaryPhone?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.primaryPhone,
            alternatePhone = alternatePhone?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.alternatePhone,
            email = email?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.email,
            website = website?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.website,
            taxId = taxId?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.taxId,
            businessRegistrationNumber = businessRegistrationNumber?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.businessRegistrationNumber,
            notes = notes?.trim()?.takeIf { it.isNotBlank() } ?: existingProfile?.notes,
            createdAt = existingProfile?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = existingProfile?.createdBy ?: updatedBy.trim().ifBlank { "system" },
            updatedBy = updatedBy.trim().ifBlank { "system" },
            version = existingProfile?.version ?: 1L
        )

        val validation = VendorProfileValidator.validate(profile)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return profileRepository.saveProfile(profile)
    }
}
