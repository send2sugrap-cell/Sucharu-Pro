package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine

/**
 * Core domain validator for stock adjustment operations (Module 07 Step 06).
 *
 * Validates structural integrity, product/warehouse/location existence and eligibility,
 * project isolation, and non-empty line collections.
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockAdjustmentValidator {

    // ──────────────────────────────────────────────────────────────
    // Structural Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates the structural invariants of an [InventoryStockAdjustment] entity.
     */
    fun validateAdjustment(adjustment: InventoryStockAdjustment): DomainResult<Unit> {
        if (adjustment.adjustmentId.isBlank()) {
            return DomainResult.Error(message = "Adjustment ID cannot be blank.")
        }
        if (adjustment.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (adjustment.adjustmentReference.isBlank()) {
            return DomainResult.Error(message = "Adjustment reference cannot be blank.")
        }
        if (adjustment.warehouseId.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID cannot be blank.")
        }
        if (adjustment.adjustmentDate.isBlank()) {
            return DomainResult.Error(message = "Adjustment date cannot be blank.")
        }
        if (adjustment.createdBy.isBlank()) {
            return DomainResult.Error(message = "createdBy actor cannot be blank.")
        }
        if (adjustment.createdAt.isBlank()) {
            return DomainResult.Error(message = "createdAt timestamp cannot be blank.")
        }
        if (adjustment.updatedAt.isBlank()) {
            return DomainResult.Error(message = "updatedAt timestamp cannot be blank.")
        }
        if (adjustment.updatedAt < adjustment.createdAt) {
            return DomainResult.Error(
                message = "updatedAt (${adjustment.updatedAt}) cannot precede createdAt (${adjustment.createdAt})."
            )
        }
        if (adjustment.totalItemsAdjusted < 0) {
            return DomainResult.Error(message = "totalItemsAdjusted cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates project-scoped reference uniqueness for an adjustment.
     */
    fun validateAdjustmentReferenceUniqueness(
        reference: String,
        adjustmentId: String,
        projectId: String,
        existingAdjustments: List<InventoryStockAdjustment>
    ): DomainResult<Unit> {
        val normalized = reference.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Adjustment reference cannot be blank.")
        }
        val match = existingAdjustments.find {
            it.projectId == projectId &&
                it.normalizedReference == normalized &&
                it.adjustmentId != adjustmentId
        }
        if (match != null) {
            return DomainResult.Error(
                message = "A stock adjustment with reference '${reference.trim()}' already exists in project '$projectId' (ID: '${match.adjustmentId}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that an adjustment contains at least one line item.
     */
    fun validateNonEmptyLines(lines: List<InventoryStockAdjustmentLine>): DomainResult<Unit> {
        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Stock adjustment must contain at least one line item.")
        }
        return DomainResult.Success(Unit)
    }

    // ──────────────────────────────────────────────────────────────
    // Entity & Eligibility Validation
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates that the adjustment warehouse exists, belongs to the correct project, and is ACTIVE.
     */
    fun validateWarehouseEligibility(
        warehouseId: String,
        projectId: String,
        warehouse: InventoryWarehouse?
    ): DomainResult<Unit> {
        if (warehouse == null) {
            return DomainResult.Error(message = "Warehouse '$warehouseId' not found.")
        }
        if (warehouse.projectId != projectId) {
            return DomainResult.Error(
                message = "Warehouse '$warehouseId' belongs to project '${warehouse.projectId}', not '$projectId'."
            )
        }
        if (warehouse.status != InventoryWarehouseStatus.ACTIVE) {
            return DomainResult.Error(
                message = "Warehouse '$warehouseId' is '${warehouse.status.defaultLabel}' and cannot be used for adjustments."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that a product exists and is ACTIVE.
     */
    fun validateProductEligibility(
        productId: String,
        product: InventoryProduct?
    ): DomainResult<Unit> {
        if (product == null) {
            return DomainResult.Error(message = "Inventory product '$productId' not found.")
        }
        if (!product.isActive) {
            return DomainResult.Error(message = "Inventory product '$productId' is inactive.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that a location exists, is ACTIVE, and belongs to the specified warehouse.
     */
    fun validateLocationEligibility(
        locationId: String,
        warehouseId: String,
        location: InventoryLocation?
    ): DomainResult<Unit> {
        if (location == null) {
            return DomainResult.Error(message = "Inventory location '$locationId' not found.")
        }
        if (location.warehouseId != warehouseId) {
            return DomainResult.Error(
                message = "Location '$locationId' belongs to warehouse '${location.warehouseId}', not '$warehouseId'."
            )
        }
        if (location.status != InventoryLocationStatus.ACTIVE) {
            return DomainResult.Error(
                message = "Location '$locationId' is '${location.status.defaultLabel}' and cannot be used for adjustments."
            )
        }
        return DomainResult.Success(Unit)
    }

    // ──────────────────────────────────────────────────────────────
    // Isolation & Consistency
    // ──────────────────────────────────────────────────────────────

    /**
     * Validates project isolation for a line item.
     */
    fun validateProjectIsolation(
        adjustmentProjectId: String,
        lineProjectId: String
    ): DomainResult<Unit> {
        if (adjustmentProjectId != lineProjectId) {
            return DomainResult.Error(
                message = "Project isolation breach: Adjustment project '$adjustmentProjectId' " +
                    "does not match line project '$lineProjectId'."
            )
        }
        return DomainResult.Success(Unit)
    }
}
