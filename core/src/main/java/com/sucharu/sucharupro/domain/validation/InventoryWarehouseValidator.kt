package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for Warehouse management (Module 07 Step 02).
 */
object InventoryWarehouseValidator {

    /**
     * Validates structural invariants of an [InventoryWarehouse].
     */
    fun validateWarehouse(warehouse: InventoryWarehouse): DomainResult<Unit> {
        if (warehouse.id.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID cannot be blank.")
        }
        if (warehouse.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (warehouse.code.isBlank()) {
            return DomainResult.Error(message = "Warehouse code cannot be blank.")
        }
        if (warehouse.name.isBlank()) {
            return DomainResult.Error(message = "Warehouse name cannot be blank.")
        }
        if (warehouse.createdBy.isBlank()) {
            return DomainResult.Error(message = "createdBy actor cannot be blank.")
        }
        if (warehouse.createdAt.isBlank()) {
            return DomainResult.Error(message = "createdAt timestamp cannot be blank.")
        }
        if (warehouse.updatedAt.isBlank()) {
            return DomainResult.Error(message = "updatedAt timestamp cannot be blank.")
        }
        if (warehouse.updatedAt < warehouse.createdAt) {
            return DomainResult.Error(
                message = "Warehouse updatedAt (${warehouse.updatedAt}) cannot precede createdAt (${warehouse.createdAt})."
            )
        }
        if (warehouse.status == InventoryWarehouseStatus.ARCHIVED && warehouse.archivedAt.isNullOrBlank()) {
            return DomainResult.Error(message = "archivedAt timestamp is required for ARCHIVED warehouses.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates project-scoped warehouse code uniqueness.
     */
    fun validateWarehouseCodeUniqueness(
        code: String,
        warehouseId: String,
        projectId: String,
        existingWarehouses: List<InventoryWarehouse>
    ): DomainResult<Unit> {
        val normalized = code.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Warehouse code cannot be blank.")
        }
        val match = existingWarehouses.find {
            it.projectId == projectId && it.normalizedCode == normalized && it.id != warehouseId
        }
        if (match != null) {
            return DomainResult.Error(
                message = "A warehouse with code '${code.trim()}' already exists in project '$projectId' (ID: '${match.id}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates warehouse lifecycle state transitions.
     * ACTIVE <-> INACTIVE -> ARCHIVED (terminal).
     */
    fun validateWarehouseTransition(
        currentStatus: InventoryWarehouseStatus,
        targetStatus: InventoryWarehouseStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        if (currentStatus == InventoryWarehouseStatus.ARCHIVED) {
            return DomainResult.Error(message = "Archived warehouse is terminal and cannot transition to '${targetStatus.defaultLabel}'.")
        }

        return when (currentStatus) {
            InventoryWarehouseStatus.ACTIVE -> {
                if (targetStatus == InventoryWarehouseStatus.INACTIVE || targetStatus == InventoryWarehouseStatus.ARCHIVED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Invalid warehouse transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'.")
                }
            }
            InventoryWarehouseStatus.INACTIVE -> {
                if (targetStatus == InventoryWarehouseStatus.ACTIVE || targetStatus == InventoryWarehouseStatus.ARCHIVED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Invalid warehouse transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'.")
                }
            }
            InventoryWarehouseStatus.ARCHIVED -> DomainResult.Error(message = "Archived warehouse cannot transition.")
        }
    }

    /**
     * Validates mutation permission for warehouse management.
     * Allowed: ADMIN, MANAGER.
     */
    fun validateWarehouseAdminPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '$callerRole' is not authorized to manage warehouses."
            )
        }
    }

    /**
     * Validates read permission for warehouses.
     */
    fun validateWarehouseViewPermission(callerRole: UserRole?): DomainResult<Unit> {
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
                message = "Role '$callerRole' is not authorized to view warehouse records."
            )
        }
    }
}
