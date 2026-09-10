package com.sucharu.sucharupro.data.api.model.machine

import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineOwnershipType
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType

/**
 * REST API DTOs for Machine & Equipment Registry.
 */
data class CreateMachineRequestDto(
    val assetCode: String,
    val name: String,
    val type: MachineType,
    val category: String = "PRODUCTION",
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val description: String? = null,
    val status: MachineStatus = MachineStatus.AVAILABLE,
    val ownershipType: MachineOwnershipType = MachineOwnershipType.COMPANY_OWNED,
    val locationReference: String? = null,
    val department: String? = null,
    val configurationMetadata: String? = null
)

data class UpdateMachineRequestDto(
    val assetCode: String? = null,
    val name: String? = null,
    val type: MachineType? = null,
    val category: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val description: String? = null,
    val status: MachineStatus? = null,
    val ownershipType: MachineOwnershipType? = null,
    val locationReference: String? = null,
    val department: String? = null,
    val configurationMetadata: String? = null
)

data class UpdateMachineStatusRequestDto(
    val status: MachineStatus
)

data class MachineResponseDto(
    val machineId: String,
    val tenantId: String,
    val assetCode: String,
    val name: String,
    val type: MachineType,
    val category: String,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val description: String?,
    val status: MachineStatus,
    val isActive: Boolean,
    val ownershipType: MachineOwnershipType,
    val locationReference: String?,
    val department: String?,
    val configurationMetadata: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String?,
    val updatedBy: String?
) {
    companion object {
        fun fromDomain(domain: MachineEquipment): MachineResponseDto = MachineResponseDto(
            machineId = domain.machineId,
            tenantId = domain.tenantId,
            assetCode = domain.assetCode,
            name = domain.name,
            type = domain.type,
            category = domain.category,
            manufacturer = domain.manufacturer,
            model = domain.model,
            serialNumber = domain.serialNumber,
            description = domain.description,
            status = domain.status,
            isActive = domain.isActive,
            ownershipType = domain.ownershipType,
            locationReference = domain.locationReference,
            department = domain.department,
            configurationMetadata = domain.configurationMetadata,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            createdBy = domain.createdBy,
            updatedBy = domain.updatedBy
        )
    }
}

data class MachineListResponseDto(
    val machines: List<MachineResponseDto>,
    val totalCount: Int = machines.size
)
