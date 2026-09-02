package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer

/**
 * Core domain validator for stock transfer operations (Module 07 Step 05).
 *
 * Validates structural integrity, reference uniqueness, warehouse and location
 * eligibility, and project isolation.
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockTransferValidator {

    /**
     * Validates the structural invariants of an [InventoryStockTransfer] entity.
     */
    fun validateTransfer(transfer: InventoryStockTransfer): DomainResult<Unit> {
        if (transfer.transferId.isBlank()) {
            return DomainResult.Error(message = "Transfer ID cannot be blank.")
        }
        if (transfer.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (transfer.transferReference.isBlank()) {
            return DomainResult.Error(message = "Transfer reference cannot be blank.")
        }
        if (transfer.fromWarehouseId.isBlank()) {
            return DomainResult.Error(message = "Source warehouse ID cannot be blank.")
        }
        if (transfer.toWarehouseId.isBlank()) {
            return DomainResult.Error(message = "Destination warehouse ID cannot be blank.")
        }
        if (transfer.fromWarehouseId == transfer.toWarehouseId) {
            return DomainResult.Error(message = "Source and destination warehouses must be different.")
        }
        if (transfer.transferDate.isBlank()) {
            return DomainResult.Error(message = "Transfer date cannot be blank.")
        }
        if (transfer.createdBy.isBlank()) {
            return DomainResult.Error(message = "createdBy actor cannot be blank.")
        }
        if (transfer.createdAt.isBlank()) {
            return DomainResult.Error(message = "createdAt timestamp cannot be blank.")
        }
        if (transfer.updatedAt.isBlank()) {
            return DomainResult.Error(message = "updatedAt timestamp cannot be blank.")
        }
        if (transfer.updatedAt < transfer.createdAt) {
            return DomainResult.Error(
                message = "updatedAt (${transfer.updatedAt}) cannot precede createdAt (${transfer.createdAt})."
            )
        }
        if (transfer.expectedTotalQuantity < 0) {
            return DomainResult.Error(message = "expectedTotalQuantity cannot be negative.")
        }
        if (transfer.transferredTotalQuantity < 0) {
            return DomainResult.Error(message = "transferredTotalQuantity cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates project-scoped reference uniqueness for a stock transfer.
     */
    fun validateReferenceUniqueness(
        reference: String,
        transferId: String,
        projectId: String,
        existingTransfers: List<InventoryStockTransfer>
    ): DomainResult<Unit> {
        val normalized = reference.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Transfer reference cannot be blank.")
        }
        val match = existingTransfers.find {
            it.projectId == projectId &&
                it.normalizedReference == normalized &&
                it.transferId != transferId
        }
        if (match != null) {
            return DomainResult.Error(
                message = "A stock transfer with reference '${reference.trim()}' already exists in project '$projectId' (ID: '${match.transferId}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the warehouse exists, is active, and belongs to the given project.
     */
    fun validateWarehouse(
        warehouseId: String,
        projectId: String,
        allWarehouses: List<InventoryWarehouse>,
        role: String // "source" or "destination"
    ): DomainResult<InventoryWarehouse> {
        if (warehouseId.isBlank()) {
            return DomainResult.Error(message = "$role warehouse ID cannot be blank.")
        }
        val warehouse = allWarehouses.find { it.id == warehouseId }
            ?: return DomainResult.Error(message = "$role warehouse with ID '$warehouseId' not found.")
        if (warehouse.projectId != projectId) {
            return DomainResult.Error(
                message = "$role warehouse '$warehouseId' belongs to project '${warehouse.projectId}', not '$projectId'."
            )
        }
        if (warehouse.status == InventoryWarehouseStatus.ARCHIVED) {
            return DomainResult.Error(message = "$role warehouse '${warehouse.name}' (ID: '$warehouseId') is archived.")
        }
        if (warehouse.status == InventoryWarehouseStatus.INACTIVE) {
            return DomainResult.Error(message = "$role warehouse '${warehouse.name}' (ID: '$warehouseId') is inactive.")
        }
        return DomainResult.Success(warehouse)
    }

    /**
     * Validates that the location exists, is active, and belongs to the correct warehouse and project.
     */
    fun validateLocation(
        locationId: String,
        warehouseId: String,
        projectId: String,
        allLocations: List<InventoryLocation>,
        role: String // "source" or "destination"
    ): DomainResult<InventoryLocation> {
        if (locationId.isBlank()) {
            return DomainResult.Error(message = "$role location ID cannot be blank.")
        }
        val location = allLocations.find { it.id == locationId }
            ?: return DomainResult.Error(message = "$role location with ID '$locationId' not found.")
        if (location.projectId != projectId) {
            return DomainResult.Error(
                message = "$role location '$locationId' belongs to project '${location.projectId}', not '$projectId'."
            )
        }
        if (location.warehouseId != warehouseId) {
            return DomainResult.Error(
                message = "$role location '$locationId' belongs to warehouse '${location.warehouseId}', not '$warehouseId'."
            )
        }
        if (location.status == InventoryLocationStatus.ARCHIVED) {
            return DomainResult.Error(message = "$role location '${location.name}' (ID: '$locationId') is archived.")
        }
        if (location.status == InventoryLocationStatus.INACTIVE) {
            return DomainResult.Error(message = "$role location '${location.name}' (ID: '$locationId') is inactive.")
        }
        return DomainResult.Success(location)
    }

    /**
     * Enforces strict project isolation for both source and destination.
     */
    fun validateProjectIsolation(
        transferProjectId: String,
        fromWarehouse: InventoryWarehouse,
        fromLocation: InventoryLocation,
        toWarehouse: InventoryWarehouse,
        toLocation: InventoryLocation
    ): DomainResult<Unit> {
        val checkFrom = validateLocationInWarehouse(transferProjectId, fromWarehouse, fromLocation, "source")
        if (checkFrom is DomainResult.Error) return checkFrom

        val checkTo = validateLocationInWarehouse(transferProjectId, toWarehouse, toLocation, "destination")
        if (checkTo is DomainResult.Error) return checkTo

        return DomainResult.Success(Unit)
    }

    private fun validateLocationInWarehouse(
        projectId: String,
        warehouse: InventoryWarehouse,
        location: InventoryLocation,
        role: String
    ): DomainResult<Unit> {
        if (warehouse.projectId != projectId) {
            return DomainResult.Error(
                message = "Project isolation violation: transfer projectId '$projectId' != $role warehouse projectId '${warehouse.projectId}'."
            )
        }
        if (location.projectId != projectId) {
            return DomainResult.Error(
                message = "Project isolation violation: transfer projectId '$projectId' != $role location projectId '${location.projectId}'."
            )
        }
        if (location.warehouseId != warehouse.id) {
            return DomainResult.Error(
                message = "Cross-warehouse violation: $role location '${location.id}' belongs to warehouse '${location.warehouseId}', not '${warehouse.id}'."
            )
        }
        return DomainResult.Success(Unit)
    }
}
