package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine

/**
 * Domain validator for receiving line quantity and structural rules (Module 07 Step 03).
 *
 * All methods are pure and side-effect-free.
 */
object InventoryReceivingLineValidator {

    /**
     * Validates the structural invariants of an [InventoryReceivingLine].
     */
    fun validateLine(line: InventoryReceivingLine): DomainResult<Unit> {
        if (line.receivingLineId.isBlank()) {
            return DomainResult.Error(message = "Receiving line ID cannot be blank.")
        }
        if (line.receivingId.isBlank()) {
            return DomainResult.Error(message = "Receiving ID cannot be blank.")
        }
        if (line.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (line.inventoryProductId.isBlank()) {
            return DomainResult.Error(message = "Inventory product ID cannot be blank.")
        }
        if (line.warehouseId.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID cannot be blank.")
        }
        if (line.locationId.isBlank()) {
            return DomainResult.Error(message = "Location ID cannot be blank.")
        }
        if (line.createdAt.isBlank()) {
            return DomainResult.Error(message = "createdAt timestamp cannot be blank.")
        }
        if (line.updatedAt.isBlank()) {
            return DomainResult.Error(message = "updatedAt timestamp cannot be blank.")
        }
        if (line.updatedAt < line.createdAt) {
            return DomainResult.Error(
                message = "Line updatedAt (${line.updatedAt}) cannot precede createdAt (${line.createdAt})."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates quantity constraints for adding received quantity.
     *
     * receivedQuantity must be > 0.
     * expectedQuantity must be >= 0.
     */
    fun validateReceivedQuantity(
        receivedQuantity: Int,
        expectedQuantity: Int
    ): DomainResult<Unit> {
        if (expectedQuantity < 0) {
            return DomainResult.Error(message = "expectedQuantity cannot be negative (was $expectedQuantity).")
        }
        if (receivedQuantity <= 0) {
            return DomainResult.Error(message = "receivedQuantity must be greater than zero (was $receivedQuantity).")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates the acceptance/rejection split against the received quantity.
     *
     * Rules:
     *   acceptedQuantity >= 0
     *   rejectedQuantity >= 0
     *   acceptedQuantity + rejectedQuantity <= receivedQuantity
     *   For finalized: acceptedQuantity + rejectedQuantity == receivedQuantity
     */
    fun validateQuantitySplit(
        receivedQuantity: Int,
        acceptedQuantity: Int,
        rejectedQuantity: Int,
        requireFullReconciliation: Boolean = false
    ): DomainResult<Unit> {
        if (acceptedQuantity < 0) {
            return DomainResult.Error(message = "acceptedQuantity cannot be negative (was $acceptedQuantity).")
        }
        if (rejectedQuantity < 0) {
            return DomainResult.Error(message = "rejectedQuantity cannot be negative (was $rejectedQuantity).")
        }
        val total = acceptedQuantity + rejectedQuantity
        if (total > receivedQuantity) {
            return DomainResult.Error(
                message = "acceptedQuantity ($acceptedQuantity) + rejectedQuantity ($rejectedQuantity) = $total " +
                    "exceeds receivedQuantity ($receivedQuantity)."
            )
        }
        if (requireFullReconciliation && total != receivedQuantity) {
            return DomainResult.Error(
                message = "For a finalized line: acceptedQuantity ($acceptedQuantity) + rejectedQuantity ($rejectedQuantity) " +
                    "must equal receivedQuantity ($receivedQuantity)."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that a duplicate receiving line does not already exist for the same product
     * within the same receiving document.
     */
    fun validateNoDuplicateLine(
        receivingId: String,
        inventoryProductId: String,
        locationId: String,
        existingLines: List<InventoryReceivingLine>,
        excludeLineId: String? = null
    ): DomainResult<Unit> {
        val duplicate = existingLines.find {
            it.receivingId == receivingId &&
                it.inventoryProductId == inventoryProductId &&
                it.locationId == locationId &&
                it.receivingLineId != excludeLineId
        }
        if (duplicate != null) {
            return DomainResult.Error(
                message = "A receiving line for product '$inventoryProductId' at location '$locationId' " +
                    "already exists in receiving '$receivingId' (line ID: '${duplicate.receivingLineId}')."
            )
        }
        return DomainResult.Success(Unit)
    }
}
