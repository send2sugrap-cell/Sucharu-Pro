package com.sucharu.sucharupro.domain.machine

/**
 * Extensible operational classification for printing and packaging equipment.
 */
enum class MachineType {
    PRINTING_PRESS,
    DIGITAL_PRINTER,
    CTP,
    LAMINATION,
    FOLDING,
    BINDING,
    CUTTING,
    FINISHING,
    PACKAGING,
    OTHER
}

/**
 * Master operational availability status for machines.
 */
enum class MachineStatus {
    AVAILABLE,
    IN_USE,
    MAINTENANCE,
    OFFLINE,
    DECOMMISSIONED
}

/**
 * Ownership classification of production assets.
 */
enum class MachineOwnershipType {
    COMPANY_OWNED,
    LEASED,
    RENTED,
    VENDOR_OWNED
}

/**
 * Core Machine / Equipment Master Identity entity.
 */
data class MachineEquipment(
    val machineId: String,
    val tenantId: String,
    val assetCode: String,
    val name: String,
    val type: MachineType,
    val category: String = "PRODUCTION",
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val description: String? = null,
    val status: MachineStatus = MachineStatus.AVAILABLE,
    val isActive: Boolean = true,
    val ownershipType: MachineOwnershipType = MachineOwnershipType.COMPANY_OWNED,
    val locationReference: String? = null,
    val department: String? = null,
    val configurationMetadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
    val updatedBy: String? = null
)
