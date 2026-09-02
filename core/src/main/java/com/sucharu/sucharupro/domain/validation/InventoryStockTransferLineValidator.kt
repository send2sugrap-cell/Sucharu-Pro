package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferLine

/**
 * Domain validator for individual stock transfer lines (Module 07 Step 05).
 *
 * Validates product eligibility, structural integrity of the line, and quantity constraints.
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockTransferLineValidator {

    /**
     * Validates the structural invariants of an [InventoryStockTransferLine] entity.
     */
    fun validateLine(line: InventoryStockTransferLine): DomainResult<Unit> {
        if (line.transferLineId.isBlank()) {
            return DomainResult.Error(message = "Transfer line ID cannot be blank.")
        }
        if (line.transferId.isBlank()) {
            return DomainResult.Error(message = "Transfer ID cannot be blank.")
        }
        if (line.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (line.inventoryProductId.isBlank()) {
            return DomainResult.Error(message = "Inventory product ID cannot be blank.")
        }
        if (line.fromWarehouseId.isBlank()) {
            return DomainResult.Error(message = "Source warehouse ID cannot be blank.")
        }
        if (line.fromLocationId.isBlank()) {
            return DomainResult.Error(message = "Source location ID cannot be blank.")
        }
        if (line.toWarehouseId.isBlank()) {
            return DomainResult.Error(message = "Destination warehouse ID cannot be blank.")
        }
        if (line.toLocationId.isBlank()) {
            return DomainResult.Error(message = "Destination location ID cannot be blank.")
        }
        if (line.expectedQuantity <= 0) {
            return DomainResult.Error(message = "Expected quantity must be greater than zero.")
        }
        if (line.transferredQuantity < 0) {
            return DomainResult.Error(message = "Transferred quantity cannot be negative.")
        }
        if (line.updatedAt < line.createdAt) {
            return DomainResult.Error(
                message = "updatedAt (${line.updatedAt}) cannot precede createdAt (${line.createdAt})."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the inventory product exists, is active, and is stock-tracked.
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
            return DomainResult.Error(message = "Inventory product '${product.name}' (ID: '$productId') is inactive and cannot be transferred.")
        }
        if (!product.isStockTracked) {
            return DomainResult.Error(message = "Inventory product '${product.name}' (ID: '$productId') is not stock-tracked.")
        }
        return DomainResult.Success(product)
    }

    /**
     * Validates that the transferred quantity does not exceed the available stock in the source location.
     */
    fun validateQuantityAgainstAvailability(
        requestedQuantity: Int,
        availableQuantity: Int,
        productName: String
    ): DomainResult<Unit> {
        if (requestedQuantity <= 0) {
            return DomainResult.Error(message = "Requested quantity for '$productName' must be greater than zero.")
        }
        if (requestedQuantity > availableQuantity) {
            return DomainResult.Error(
                message = "Insufficient stock for '$productName' in source location. Requested: $requestedQuantity, Available: $availableQuantity."
            )
        }
        return DomainResult.Success(Unit)
    }
}
