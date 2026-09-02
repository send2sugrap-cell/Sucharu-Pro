package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorValidator
import java.util.UUID

/**
 * Production implementation of [VendorService] (Module 12 Step 01).
 */
class VendorServiceImpl(
    private val vendorRepository: VendorRepository
) : VendorService {

    override suspend fun createVendor(
        projectId: String,
        vendorName: String,
        vendorCode: String?,
        legalName: String?,
        vendorType: VendorType,
        vendorCategory: VendorCategory,
        status: VendorStatus,
        primaryContactName: String?,
        primaryPhone: String?,
        primaryEmail: String?,
        notes: String?,
        createdBy: String
    ): DomainResult<Vendor> {
        val effectiveProjectId = projectId.trim()
        if (effectiveProjectId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId cannot be blank."))
        }

        val generatedCode = vendorCode?.trim()?.takeIf { it.isNotBlank() }
            ?: "VND-${UUID.randomUUID().toString().take(8).uppercase()}"

        val vendor = Vendor(
            vendorId = UUID.randomUUID().toString(),
            projectId = effectiveProjectId,
            vendorCode = generatedCode,
            vendorName = vendorName.trim(),
            legalName = legalName?.trim()?.takeIf { it.isNotBlank() },
            vendorType = vendorType,
            vendorCategory = vendorCategory,
            status = status,
            primaryContactName = primaryContactName?.trim()?.takeIf { it.isNotBlank() },
            primaryPhone = primaryPhone?.trim()?.takeIf { it.isNotBlank() },
            primaryEmail = primaryEmail?.trim()?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            createdBy = createdBy.trim().ifBlank { "system" },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = 1L
        )

        val validation = VendorValidator.validate(vendor)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        // Check for duplicate code
        if (vendorRepository.existsByCode(effectiveProjectId, vendor.vendorCode)) {
            return DomainResult.Error(
                IllegalStateException("Vendor with code '${vendor.vendorCode}' already exists in project '$effectiveProjectId'.")
            )
        }

        return vendorRepository.createVendor(vendor)
    }

    override suspend fun updateVendor(
        projectId: String,
        vendorId: String,
        vendorName: String,
        legalName: String?,
        vendorType: VendorType?,
        vendorCategory: VendorCategory?,
        primaryContactName: String?,
        primaryPhone: String?,
        primaryEmail: String?,
        notes: String?,
        updatedBy: String
    ): DomainResult<Vendor> {
        val effectiveProjectId = projectId.trim()
        val effectiveVendorId = vendorId.trim()

        if (effectiveProjectId.isBlank() || effectiveVendorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }

        val existing = when (val res = vendorRepository.findById(effectiveProjectId, effectiveVendorId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Repository returned loading state."))
        }

        if (existing.status == VendorStatus.ARCHIVED) {
            return DomainResult.Error(IllegalStateException("Cannot modify archived vendor '$effectiveVendorId'."))
        }

        val updated = existing.copy(
            vendorName = vendorName.trim(),
            legalName = legalName?.trim() ?: existing.legalName,
            vendorType = vendorType ?: existing.vendorType,
            vendorCategory = vendorCategory ?: existing.vendorCategory,
            primaryContactName = primaryContactName?.trim() ?: existing.primaryContactName,
            primaryPhone = primaryPhone?.trim() ?: existing.primaryPhone,
            primaryEmail = primaryEmail?.trim() ?: existing.primaryEmail,
            notes = notes?.trim() ?: existing.notes,
            updatedBy = updatedBy.trim().ifBlank { "system" },
            updatedAt = System.currentTimeMillis(),
            version = existing.version
        )

        val validation = VendorValidator.validate(updated)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return vendorRepository.updateVendor(updated)
    }

    override suspend fun updateStatus(
        projectId: String,
        vendorId: String,
        newStatus: VendorStatus,
        updatedBy: String
    ): DomainResult<Vendor> {
        val effectiveProjectId = projectId.trim()
        val effectiveVendorId = vendorId.trim()

        if (effectiveProjectId.isBlank() || effectiveVendorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }

        val existing = when (val res = vendorRepository.findById(effectiveProjectId, effectiveVendorId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Repository returned loading state."))
        }

        val transition = VendorValidator.validateStatusTransition(existing.status, newStatus)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalStateException(transition.errorMessage))
        }

        return vendorRepository.updateStatus(effectiveProjectId, effectiveVendorId, newStatus, updatedBy)
    }

    override suspend fun getVendorById(projectId: String, vendorId: String): DomainResult<Vendor> {
        return vendorRepository.findById(projectId.trim(), vendorId.trim())
    }

    override suspend fun getVendorByCode(projectId: String, vendorCode: String): DomainResult<Vendor> {
        return vendorRepository.findByCode(projectId.trim(), vendorCode.trim())
    }

    override suspend fun listVendors(
        projectId: String,
        type: VendorType?,
        category: VendorCategory?,
        status: VendorStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<Vendor>> {
        return vendorRepository.listVendors(projectId.trim(), type, category, status, limit, offset)
    }
}
