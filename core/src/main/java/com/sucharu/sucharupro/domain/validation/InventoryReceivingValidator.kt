package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving

/**
 * Core domain validator for stock receiving operations (Module 07 Step 03).
 *
 * Validates structural integrity, product/warehouse/location existence and eligibility,
 * cross-project isolation, and cross-warehouse location usage.
 *
 * All methods are pure and side-effect-free.
 */
object InventoryReceivingValidator {

    // ──────────────────────────────────────────────────────────────
    // Structural Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates the structural invariants of an [InventoryReceiving] entity.
     */
    fun validateReceiving(receiving: InventoryReceiving): DomainResult<Unit> {
        if (receiving.receivingId.isBlank()) {
            return DomainResult.Error(message = "Receiving ID cannot be blank.")
        }
        if (receiving.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (receiving.receivingReference.isBlank()) {
            return DomainResult.Error(message = "Receiving reference cannot be blank.")
        }
        if (receiving.warehouseId.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID cannot be blank.")
        }
        if (receiving.receivingDate.isBlank()) {
            return DomainResult.Error(message = "Receiving date cannot be blank.")
        }
        if (receiving.createdBy.isBlank()) {
            return DomainResult.Error(message = "createdBy actor cannot be blank.")
        }
        if (receiving.createdAt.isBlank()) {
            return DomainResult.Error(message = "createdAt timestamp cannot be blank.")
        }
        if (receiving.updatedAt.isBlank()) {
            return DomainResult.Error(message = "updatedAt timestamp cannot be blank.")
        }
        if (receiving.updatedAt < receiving.createdAt) {
            return DomainResult.Error(
                message = "updatedAt (${receiving.updatedAt}) cannot precede createdAt (${receiving.createdAt})."
            )
        }
        if (receiving.expectedTotalQuantity < 0) {
            return DomainResult.Error(message = "expectedTotalQuantity cannot be negative.")
        }
        if (receiving.acceptedTotalQuantity < 0) {
            return DomainResult.Error(message = "acceptedTotalQuantity cannot be negative.")
        }
        if (receiving.rejectedTotalQuantity < 0) {
            return DomainResult.Error(message = "rejectedTotalQuantity cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates project-scoped reference uniqueness for a receiving.
     */
    fun validateReceivingReferenceUniqueness(
        reference: String,
        receivingId: String,
        projectId: String,
        existingReceivings: List<InventoryReceiving>
    ): DomainResult<Unit> {
        val normalized = reference.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Receiving reference cannot be blank.")
        }
        val match = existingReceivings.find {
            it.projectId == projectId &&
                it.normalizedReference == normalized &&
                it.receivingId != receivingId
        }
        if (match != null) {
            return DomainResult.Error(
                message = "A receiving with reference '${reference.trim()}' already exists in project '$projectId' (ID: '${match.receivingId}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    // ──────────────────────────────────────────────────────────────
    // Product Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates that the inventory product exists and is active.
     *
     * Note: [InventoryProduct] has no projectId (Step 01 design).
     * Cross-project isolation is enforced via warehouse and location projectId checks.
     */
    fun validateProduct(
        productId: String,
        allProducts: List<InventoryProduct>
    ): DomainResult<InventoryProduct> {
        if (productId.isBlank()) {
            return DomainResult.Error(message = "Product ID cannot be blank.")
        }
        val product = allProducts.find { it.id == productId }
            ?: return DomainResult.Error(message = "Inventory product with ID '$productId' not found.")
        if (!product.isActive) {
            return DomainResult.Error(message = "Inventory product '${product.name}' (ID: '$productId') is inactive and cannot receive stock.")
        }
        if (!product.isStockTracked) {
            return DomainResult.Error(message = "Inventory product '${product.name}' (ID: '$productId') is not stock-tracked.")
        }
        return DomainResult.Success(product)
    }

    // ──────────────────────────────────────────────────────────────
    // Warehouse Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates that the warehouse exists, is ACTIVE, and belongs to the given project.
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
                message = "Warehouse '$warehouseId' belongs to project '${warehouse.projectId}', not '$projectId'. Cross-project access is forbidden."
            )
        }
        if (warehouse.status == InventoryWarehouseStatus.ARCHIVED) {
            return DomainResult.Error(message = "Warehouse '${warehouse.name}' (ID: '$warehouseId') is archived and cannot receive stock.")
        }
        if (warehouse.status == InventoryWarehouseStatus.INACTIVE) {
            return DomainResult.Error(message = "Warehouse '${warehouse.name}' (ID: '$warehouseId') is inactive and cannot receive stock.")
        }
        return DomainResult.Success(warehouse)
    }

    // ──────────────────────────────────────────────────────────────
    // Location Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates that the location exists, is ACTIVE, and belongs to the correct warehouse and project.
     *
     * Enforces:
     * - location.projectId == projectId
     * - location.warehouseId == warehouseId
     * - location.status == ACTIVE
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
                message = "Location '$locationId' belongs to project '${location.projectId}', not '$projectId'. Cross-project access is forbidden."
            )
        }
        if (location.warehouseId != warehouseId) {
            return DomainResult.Error(
                message = "Location '$locationId' belongs to warehouse '${location.warehouseId}', not '$warehouseId'. Cross-warehouse location usage is forbidden."
            )
        }
        if (location.status == InventoryLocationStatus.ARCHIVED) {
            return DomainResult.Error(message = "Location '${location.name}' (ID: '$locationId') is archived and cannot receive stock.")
        }
        if (location.status == InventoryLocationStatus.INACTIVE) {
            return DomainResult.Error(message = "Location '${location.name}' (ID: '$locationId') is inactive and cannot receive stock.")
        }
        return DomainResult.Success(location)
    }

    // ──────────────────────────────────────────────────────────────
    // Project Isolation
    // ──────────────────────────────────────────────────────────────

    /**
     * Enforces strict project isolation:
     *   receiving.projectId == warehouse.projectId == location.projectId
     */
    fun validateProjectIsolation(
        receivingProjectId: String,
        warehouse: InventoryWarehouse,
        location: InventoryLocation
    ): DomainResult<Unit> {
        if (warehouse.projectId != receivingProjectId) {
            return DomainResult.Error(
                message = "Project isolation violation: receiving projectId '$receivingProjectId' != warehouse projectId '${warehouse.projectId}'."
            )
        }
        if (location.projectId != receivingProjectId) {
            return DomainResult.Error(
                message = "Project isolation violation: receiving projectId '$receivingProjectId' != location projectId '${location.projectId}'."
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
