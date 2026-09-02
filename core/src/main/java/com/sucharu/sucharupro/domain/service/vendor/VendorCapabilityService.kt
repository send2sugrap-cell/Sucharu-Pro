package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityStatus
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.VendorCapability
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.repository.VendorCapabilityRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorCapabilityValidator
import java.util.UUID

interface VendorCapabilityService {
    suspend fun listCapabilities(projectId: String, vendorId: String, status: CapabilityStatus? = null): DomainResult<List<VendorCapability>>
    suspend fun getCapabilityById(projectId: String, capabilityId: String): DomainResult<VendorCapability>
    suspend fun hasCapability(projectId: String, vendorId: String, capabilityType: CapabilityType): Boolean
    suspend fun listVendorsByCapability(projectId: String, capabilityType: CapabilityType, status: CapabilityStatus? = CapabilityStatus.ACTIVE): DomainResult<List<String>>
    suspend fun createCapability(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        displayName: String? = null,
        status: CapabilityStatus = CapabilityStatus.ACTIVE,
        notes: String? = null,
        createdBy: String = "system"
    ): DomainResult<VendorCapability>
    suspend fun updateCapability(
        projectId: String,
        capabilityId: String,
        displayName: String? = null,
        status: CapabilityStatus? = null,
        notes: String? = null,
        updatedBy: String = "system"
    ): DomainResult<VendorCapability>
    suspend fun updateStatus(
        projectId: String,
        capabilityId: String,
        status: CapabilityStatus,
        updatedBy: String = "system"
    ): DomainResult<VendorCapability>
}

class VendorCapabilityServiceImpl(
    private val vendorRepository: VendorRepository,
    private val capabilityRepository: VendorCapabilityRepository
) : VendorCapabilityService {

    override suspend fun listCapabilities(projectId: String, vendorId: String, status: CapabilityStatus?): DomainResult<List<VendorCapability>> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }
        return capabilityRepository.listByVendor(pId, vId, status)
    }

    override suspend fun getCapabilityById(projectId: String, capabilityId: String): DomainResult<VendorCapability> {
        val pId = projectId.trim()
        val cId = capabilityId.trim()
        if (pId.isBlank() || cId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and capabilityId cannot be blank."))
        }
        return capabilityRepository.findById(pId, cId)
    }

    override suspend fun hasCapability(projectId: String, vendorId: String, capabilityType: CapabilityType): Boolean {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) return false
        val cap = when (val res = capabilityRepository.findByVendorAndType(pId, vId, capabilityType)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        return cap != null && cap.status.isActive
    }

    override suspend fun listVendorsByCapability(projectId: String, capabilityType: CapabilityType, status: CapabilityStatus?): DomainResult<List<String>> {
        val pId = projectId.trim()
        if (pId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId cannot be blank."))
        }
        return capabilityRepository.listVendorsByCapability(pId, capabilityType, status)
    }

    override suspend fun createCapability(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        displayName: String?,
        status: CapabilityStatus,
        notes: String?,
        createdBy: String
    ): DomainResult<VendorCapability> {
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
            return DomainResult.Error(IllegalStateException("Cannot register capability for archived vendor '$vId'."))
        }

        if (capabilityRepository.existsByVendorAndType(pId, vId, capabilityType)) {
            return DomainResult.Error(
                IllegalStateException("Vendor '$vId' already possesses capability '${capabilityType.name}'.")
            )
        }

        val capability = VendorCapability(
            capabilityId = "cap_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            vendorId = vId,
            projectId = pId,
            capabilityType = capabilityType,
            displayName = displayName?.trim()?.takeIf { it.isNotBlank() } ?: capabilityType.name.replace("_", " "),
            status = status,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = createdBy.trim().ifBlank { "system" },
            updatedBy = createdBy.trim().ifBlank { "system" },
            version = 1L
        )

        val validation = VendorCapabilityValidator.validate(capability)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return capabilityRepository.createCapability(capability)
    }

    override suspend fun updateCapability(
        projectId: String,
        capabilityId: String,
        displayName: String?,
        status: CapabilityStatus?,
        notes: String?,
        updatedBy: String
    ): DomainResult<VendorCapability> {
        val pId = projectId.trim()
        val cId = capabilityId.trim()
        if (pId.isBlank() || cId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and capabilityId cannot be blank."))
        }

        val existing = when (val res = capabilityRepository.findById(pId, cId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val targetStatus = status ?: existing.status
        val transitionVal = VendorCapabilityValidator.validateStatusTransition(existing.status, targetStatus)
        if (!transitionVal.isValid) {
            return DomainResult.Error(IllegalArgumentException(transitionVal.errorMessage))
        }

        val updated = existing.copy(
            displayName = displayName?.trim()?.takeIf { it.isNotBlank() } ?: existing.displayName,
            status = targetStatus,
            notes = notes?.trim()?.takeIf { it.isNotBlank() } ?: existing.notes,
            updatedAt = System.currentTimeMillis(),
            updatedBy = updatedBy.trim().ifBlank { "system" },
            version = existing.version
        )

        val validation = VendorCapabilityValidator.validate(updated)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return capabilityRepository.updateCapability(updated)
    }

    override suspend fun updateStatus(
        projectId: String,
        capabilityId: String,
        status: CapabilityStatus,
        updatedBy: String
    ): DomainResult<VendorCapability> {
        return updateCapability(
            projectId = projectId,
            capabilityId = capabilityId,
            status = status,
            updatedBy = updatedBy
        )
    }
}
