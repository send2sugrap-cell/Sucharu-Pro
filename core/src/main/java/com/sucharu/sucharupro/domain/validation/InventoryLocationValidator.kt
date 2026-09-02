package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for storage locations and hierarchy trees (Module 07 Step 02).
 */
object InventoryLocationValidator {

    const val MAX_HIERARCHY_DEPTH = 10

    /**
     * Validates structural invariants of an [InventoryLocation].
     */
    fun validateLocation(location: InventoryLocation): DomainResult<Unit> {
        if (location.id.isBlank()) {
            return DomainResult.Error(message = "Location ID cannot be blank.")
        }
        if (location.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (location.warehouseId.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID cannot be blank.")
        }
        if (location.code.isBlank()) {
            return DomainResult.Error(message = "Location code cannot be blank.")
        }
        if (location.name.isBlank()) {
            return DomainResult.Error(message = "Location name cannot be blank.")
        }
        if (location.createdBy.isBlank()) {
            return DomainResult.Error(message = "createdBy actor cannot be blank.")
        }
        if (location.createdAt.isBlank()) {
            return DomainResult.Error(message = "createdAt timestamp cannot be blank.")
        }
        if (location.updatedAt.isBlank()) {
            return DomainResult.Error(message = "updatedAt timestamp cannot be blank.")
        }
        if (location.updatedAt < location.createdAt) {
            return DomainResult.Error(
                message = "Location updatedAt (${location.updatedAt}) cannot precede createdAt (${location.createdAt})."
            )
        }
        if (location.capacity != null && location.capacity < 0.0) {
            return DomainResult.Error(message = "Location capacity cannot be negative (was ${location.capacity}).")
        }
        if (location.status == InventoryLocationStatus.ARCHIVED && location.archivedAt.isNullOrBlank()) {
            return DomainResult.Error(message = "archivedAt timestamp is required for ARCHIVED locations.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates warehouse-scoped location code uniqueness.
     */
    fun validateLocationCodeUniqueness(
        code: String,
        locationId: String,
        warehouseId: String,
        existingLocations: List<InventoryLocation>
    ): DomainResult<Unit> {
        val normalized = code.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Location code cannot be blank.")
        }
        val match = existingLocations.find {
            it.warehouseId == warehouseId && it.normalizedCode == normalized && it.id != locationId
        }
        if (match != null) {
            return DomainResult.Error(
                message = "A location with code '${code.trim()}' already exists in warehouse '$warehouseId' (ID: '${match.id}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates the warehouse reference for a location.
     */
    fun validateWarehouseAssociation(
        location: InventoryLocation,
        warehouse: InventoryWarehouse?
    ): DomainResult<Unit> {
        if (warehouse == null) {
            return DomainResult.Error(message = "Warehouse with ID '${location.warehouseId}' does not exist.")
        }
        if (warehouse.projectId != location.projectId) {
            return DomainResult.Error(
                message = "Warehouse project mismatch: warehouse belongs to project '${warehouse.projectId}', location has project '${location.projectId}'."
            )
        }
        if (warehouse.status == InventoryWarehouseStatus.ARCHIVED) {
            return DomainResult.Error(
                message = "Cannot create or assign a location to an ARCHIVED warehouse (ID: '${warehouse.id}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates parent location relationships, hierarchy depth, and circular references.
     */
    fun validateParentHierarchy(
        locationId: String,
        parentLocationId: String?,
        warehouseId: String,
        projectId: String,
        allLocations: List<InventoryLocation>
    ): DomainResult<Unit> {
        if (parentLocationId == null) return DomainResult.Success(Unit)

        if (parentLocationId == locationId) {
            return DomainResult.Error(message = "A location cannot be its own parent (ID: '$locationId').")
        }

        val parent = allLocations.find { it.id == parentLocationId }
            ?: return DomainResult.Error(message = "Parent location with ID '$parentLocationId' not found.")

        if (parent.warehouseId != warehouseId) {
            return DomainResult.Error(
                message = "Parent location '$parentLocationId' belongs to warehouse '${parent.warehouseId}', expected '$warehouseId'."
            )
        }
        if (parent.projectId != projectId) {
            return DomainResult.Error(
                message = "Parent location '$parentLocationId' belongs to project '${parent.projectId}', expected '$projectId'."
            )
        }
        if (parent.status == InventoryLocationStatus.ARCHIVED) {
            return DomainResult.Error(
                message = "Cannot assign a child location to an ARCHIVED parent (ID: '$parentLocationId')."
            )
        }

        // Circular reference & depth check
        var current: InventoryLocation? = parent
        var depth = 1
        val visited = mutableSetOf(locationId, parentLocationId)

        while (current?.parentLocationId != null) {
            depth++
            if (depth > MAX_HIERARCHY_DEPTH) {
                return DomainResult.Error(
                    message = "Location hierarchy depth exceeds maximum allowed depth of $MAX_HIERARCHY_DEPTH levels."
                )
            }
            val nextParentId = current.parentLocationId!!
            if (visited.contains(nextParentId)) {
                return DomainResult.Error(
                    message = "Circular hierarchy reference detected in location chain at ID '$nextParentId'."
                )
            }
            visited.add(nextParentId)
            current = allLocations.find { it.id == nextParentId }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates location lifecycle state transitions.
     */
    fun validateLocationTransition(
        currentStatus: InventoryLocationStatus,
        targetStatus: InventoryLocationStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        if (currentStatus == InventoryLocationStatus.ARCHIVED) {
            return DomainResult.Error(
                message = "Archived location is terminal and cannot transition to '${targetStatus.defaultLabel}'."
            )
        }

        return when (currentStatus) {
            InventoryLocationStatus.ACTIVE -> {
                if (targetStatus == InventoryLocationStatus.INACTIVE || targetStatus == InventoryLocationStatus.ARCHIVED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Invalid location transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'.")
                }
            }
            InventoryLocationStatus.INACTIVE -> {
                if (targetStatus == InventoryLocationStatus.ACTIVE || targetStatus == InventoryLocationStatus.ARCHIVED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Invalid location transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'.")
                }
            }
            InventoryLocationStatus.ARCHIVED -> DomainResult.Error(message = "Archived location cannot transition.")
        }
    }

    /**
     * Validates location admin permission (ADMIN, MANAGER).
     */
    fun validateLocationAdminPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '$callerRole' is not authorized to manage storage locations."
            )
        }
    }

    /**
     * Validates location read permission.
     */
    fun validateLocationViewPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE,
            UserRole.STAFF,
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE -> DomainResult.Error(
                message = "Role '$callerRole' is not authorized to view storage locations."
            )
        }
    }
}
