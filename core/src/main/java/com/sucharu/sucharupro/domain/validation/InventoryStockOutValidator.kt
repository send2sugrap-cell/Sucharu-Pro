package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut

/**
 * Core domain validator for stock out / issue operations (Module 07 Step 04).
 *
 * Validates structural integrity, reference uniqueness, warehouse and location
 * eligibility, and project isolation.
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockOutValidator {

    // ──────────────────────────────────────────────────────────────
    // Structural Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates the structural invariants of an [InventoryStockOut] entity.
     */
    fun validateStockOut(stockOut: InventoryStockOut): DomainResult<Unit> {
        if (stockOut.stockOutId.isBlank()) {
            return DomainResult.Error(message = "Stock-out ID cannot be blank.")
        }
        if (stockOut.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (stockOut.stockOutReference.isBlank()) {
            return DomainResult.Error(message = "Stock-out reference cannot be blank.")
        }
        if (stockOut.warehouseId.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID cannot be blank.")
        }
        if (stockOut.stockOutDate.isBlank()) {
            return DomainResult.Error(message = "Stock-out date cannot be blank.")
        }
        if (stockOut.createdBy.isBlank()) {
            return DomainResult.Error(message = "createdBy actor cannot be blank.")
        }
        if (stockOut.createdAt.isBlank()) {
            return DomainResult.Error(message = "createdAt timestamp cannot be blank.")
        }
        if (stockOut.updatedAt.isBlank()) {
            return DomainResult.Error(message = "updatedAt timestamp cannot be blank.")
        }
        if (stockOut.updatedAt < stockOut.createdAt) {
            return DomainResult.Error(
                message = "updatedAt (${stockOut.updatedAt}) cannot precede createdAt (${stockOut.createdAt})."
            )
        }
        if (stockOut.expectedTotalQuantity < 0) {
            return DomainResult.Error(message = "expectedTotalQuantity cannot be negative.")
        }
        if (stockOut.issuedTotalQuantity < 0) {
            return DomainResult.Error(message = "issuedTotalQuantity cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates project-scoped reference uniqueness for a stock out.
     */
    fun validateReferenceUniqueness(
        reference: String,
        stockOutId: String,
        projectId: String,
        existingStockOuts: List<InventoryStockOut>
    ): DomainResult<Unit> {
        val normalized = reference.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Stock-out reference cannot be blank.")
        }
        val match = existingStockOuts.find {
            it.projectId == projectId &&
                it.normalizedReference == normalized &&
                it.stockOutId != stockOutId
        }
        if (match != null) {
            return DomainResult.Error(
                message = "A stock-out with reference '${reference.trim()}' already exists in project '$projectId' (ID: '${match.stockOutId}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    // ──────────────────────────────────────────────────────────────
    // Warehouse Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates that the warehouse exists, is active, and belongs to the given project.
     */
    fun validateWarehouse(
        warehouseId: String,
        projectId: String,
        allWarehouses: List<InventoryWarehouse>
    ): DomainResult<InventoryWarehouse> {
        if (warehouseId.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID cannot be blank.")
        }
        val warehouse = allWarehouses.find { it.id == warehouseId }
            ?: return DomainResult.Error(message = "Warehouse with ID '$warehouseId' not found.")
        if (warehouse.projectId != projectId) {
            return DomainResult.Error(
                message = "Warehouse '$warehouseId' belongs to project '${warehouse.projectId}', not '$projectId'."
            )
        }
        if (warehouse.status == InventoryWarehouseStatus.ARCHIVED) {
            return DomainResult.Error(message = "Warehouse '${warehouse.name}' (ID: '$warehouseId') is archived and cannot issue stock.")
        }
        if (warehouse.status == InventoryWarehouseStatus.INACTIVE) {
            return DomainResult.Error(message = "Warehouse '${warehouse.name}' (ID: '$warehouseId') is inactive and cannot issue stock.")
        }
        return DomainResult.Success(warehouse)
    }

    // ──────────────────────────────────────────────────────────────
    // Location Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates that the location exists, is active, and belongs to the correct warehouse and project.
     */
    fun validateLocation(
        locationId: String,
        warehouseId: String,
        projectId: String,
        allLocations: List<InventoryLocation>
    ): DomainResult<InventoryLocation> {
        if (locationId.isBlank()) {
            return DomainResult.Error(message = "Location ID cannot be blank.")
        }
        val location = allLocations.find { it.id == locationId }
            ?: return DomainResult.Error(message = "Location with ID '$locationId' not found.")
        if (location.projectId != projectId) {
            return DomainResult.Error(
                message = "Location '$locationId' belongs to project '${location.projectId}', not '$projectId'."
            )
        }
        if (location.warehouseId != warehouseId) {
            return DomainResult.Error(
                message = "Location '$locationId' belongs to warehouse '${location.warehouseId}', not '$warehouseId'."
            )
        }
        if (location.status == InventoryLocationStatus.ARCHIVED) {
            return DomainResult.Error(message = "Location '${location.name}' (ID: '$locationId') is archived and cannot issue stock.")
        }
        if (location.status == InventoryLocationStatus.INACTIVE) {
            return DomainResult.Error(message = "Location '${location.name}' (ID: '$locationId') is inactive and cannot issue stock.")
        }
        return DomainResult.Success(location)
    }

    // ──────────────────────────────────────────────────────────────
    // Project Isolation
    // ──────────────────────────────────────────────────────────────

    /**
     * Enforces strict project isolation.
     */
    fun validateProjectIsolation(
        stockOutProjectId: String,
        warehouse: InventoryWarehouse,
        location: InventoryLocation
    ): DomainResult<Unit> {
        if (warehouse.projectId != stockOutProjectId) {
            return DomainResult.Error(
                message = "Project isolation violation: stock-out projectId '$stockOutProjectId' != warehouse projectId '${warehouse.projectId}'."
            )
        }
        if (location.projectId != stockOutProjectId) {
            return DomainResult.Error(
                message = "Project isolation violation: stock-out projectId '$stockOutProjectId' != location projectId '${location.projectId}'."
            )
        }
        if (location.warehouseId != warehouse.id) {
            return DomainResult.Error(
                message = "Cross-warehouse violation: location '${location.id}' belongs to warehouse '${location.warehouseId}', not '${warehouse.id}'."
            )
        }
        return DomainResult.Success(Unit)
    }
}
