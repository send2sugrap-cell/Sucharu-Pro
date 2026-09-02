package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.AddressType
import com.sucharu.sucharupro.domain.model.vendor.VendorAddress
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.repository.VendorAddressRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorAddressValidator
import java.util.UUID

interface VendorAddressService {
    suspend fun listAddresses(projectId: String, vendorId: String, activeOnly: Boolean = false): DomainResult<List<VendorAddress>>
    suspend fun getAddressById(projectId: String, addressId: String): DomainResult<VendorAddress>
    suspend fun createAddress(
        projectId: String,
        vendorId: String,
        addressLine1: String,
        addressType: AddressType = AddressType.OFFICE,
        addressLine2: String? = null,
        city: String = "Dhaka",
        district: String? = null,
        postalCode: String? = null,
        country: String = "Bangladesh",
        notes: String? = null,
        isPrimary: Boolean = false,
        createdBy: String = "system"
    ): DomainResult<VendorAddress>
    suspend fun updateAddress(
        projectId: String,
        addressId: String,
        addressLine1: String,
        addressType: AddressType = AddressType.OFFICE,
        addressLine2: String? = null,
        city: String = "Dhaka",
        district: String? = null,
        postalCode: String? = null,
        country: String = "Bangladesh",
        notes: String? = null,
        isPrimary: Boolean = false,
        updatedBy: String = "system"
    ): DomainResult<VendorAddress>
    suspend fun updateStatus(projectId: String, addressId: String, active: Boolean, updatedBy: String = "system"): DomainResult<VendorAddress>
}

class VendorAddressServiceImpl(
    private val vendorRepository: VendorRepository,
    private val addressRepository: VendorAddressRepository
) : VendorAddressService {

    override suspend fun listAddresses(projectId: String, vendorId: String, activeOnly: Boolean): DomainResult<List<VendorAddress>> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }
        return addressRepository.listByVendor(pId, vId, activeOnly)
    }

    override suspend fun getAddressById(projectId: String, addressId: String): DomainResult<VendorAddress> {
        val pId = projectId.trim()
        val aId = addressId.trim()
        if (pId.isBlank() || aId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and addressId cannot be blank."))
        }
        return addressRepository.findById(pId, aId)
    }

    override suspend fun createAddress(
        projectId: String,
        vendorId: String,
        addressLine1: String,
        addressType: AddressType,
        addressLine2: String?,
        city: String,
        district: String?,
        postalCode: String?,
        country: String,
        notes: String?,
        isPrimary: Boolean,
        createdBy: String
    ): DomainResult<VendorAddress> {
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
            return DomainResult.Error(IllegalStateException("Cannot add address to archived vendor '$vId'."))
        }

        val address = VendorAddress(
            addressId = "addr_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            vendorId = vId,
            projectId = pId,
            addressType = addressType,
            addressLine1 = addressLine1.trim(),
            addressLine2 = addressLine2?.trim()?.takeIf { it.isNotBlank() },
            city = city.trim().ifBlank { "Dhaka" },
            district = district?.trim()?.takeIf { it.isNotBlank() },
            postalCode = postalCode?.trim()?.takeIf { it.isNotBlank() },
            country = country.trim().ifBlank { "Bangladesh" },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            isPrimary = isPrimary,
            active = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = createdBy.trim().ifBlank { "system" },
            updatedBy = createdBy.trim().ifBlank { "system" },
            version = 1L
        )

        val validation = VendorAddressValidator.validate(address)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return addressRepository.createAddress(address)
    }

    override suspend fun updateAddress(
        projectId: String,
        addressId: String,
        addressLine1: String,
        addressType: AddressType,
        addressLine2: String?,
        city: String,
        district: String?,
        postalCode: String?,
        country: String,
        notes: String?,
        isPrimary: Boolean,
        updatedBy: String
    ): DomainResult<VendorAddress> {
        val pId = projectId.trim()
        val aId = addressId.trim()
        if (pId.isBlank() || aId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and addressId cannot be blank."))
        }

        val existing = when (val res = addressRepository.findById(pId, aId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val updated = existing.copy(
            addressLine1 = addressLine1.trim(),
            addressType = addressType,
            addressLine2 = addressLine2?.trim()?.takeIf { it.isNotBlank() } ?: existing.addressLine2,
            city = city.trim().ifBlank { existing.city },
            district = district?.trim()?.takeIf { it.isNotBlank() } ?: existing.district,
            postalCode = postalCode?.trim()?.takeIf { it.isNotBlank() } ?: existing.postalCode,
            country = country.trim().ifBlank { existing.country },
            notes = notes?.trim()?.takeIf { it.isNotBlank() } ?: existing.notes,
            isPrimary = isPrimary,
            updatedAt = System.currentTimeMillis(),
            updatedBy = updatedBy.trim().ifBlank { "system" },
            version = existing.version
        )

        val validation = VendorAddressValidator.validate(updated)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return addressRepository.updateAddress(updated)
    }

    override suspend fun updateStatus(projectId: String, addressId: String, active: Boolean, updatedBy: String): DomainResult<VendorAddress> {
        val pId = projectId.trim()
        val aId = addressId.trim()
        if (pId.isBlank() || aId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and addressId cannot be blank."))
        }
        return addressRepository.updateStatus(pId, aId, active, updatedBy)
    }
}
