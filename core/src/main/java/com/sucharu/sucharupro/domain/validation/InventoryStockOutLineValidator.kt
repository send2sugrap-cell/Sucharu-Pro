package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine

/**
 * Domain validator for individual stock out lines (Module 07 Step 04).
 *
 * Validates product eligibility, structural integrity of the line, and quantity constraints.
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockOutLineValidator {

    /**
     * Validates the structural invariants of an [InventoryStockOutLine] entity.
     */
    fun validateLine(line: InventoryStockOutLine): DomainResult<Unit> {
        if (line.stockOutLineId.isBlank()) {
            return DomainResult.Error(message = "Stock-out line ID cannot be blank.")
        }
        if (line.stockOutId.isBlank()) {
            return DomainResult.Error(message = "Stock-out ID cannot be blank.")
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
        if (line.expectedQuantity <= 0) {
            return DomainResult.Error(message = "Expected quantity must be greater than zero.")
        }
        if (line.issuedQuantity < 0) {
            return DomainResult.Error(message = "Issued quantity cannot be negative.")
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
            return DomainResult.Error(message = "Inventory product '${product.name}' (ID: '$productId') is inactive and cannot be issued.")
        }
        if (!product.isStockTracked) {
            return DomainResult.Error(message = "Inventory product '${product.name}' (ID: '$productId') is not stock-tracked.")
        }
        return DomainResult.Success(product)
    }

    /**
     * Validates that the issued quantity does not exceed the expected quantity (optional business rule)
     * and does not exceed the available stock.
     */
    fun validateQuantityAgainstAvailability(
        issuedQuantity: Int,
        availableQuantity: Int,
        productName: String
    ): DomainResult<Unit> {
        if (issuedQuantity <= 0) {
            return DomainResult.Error(message = "Issued quantity for '$productName' must be greater than zero.")
        }
        if (issuedQuantity > availableQuantity) {
            return DomainResult.Error(
                message = "Insufficient stock for '$productName'. Requested: $issuedQuantity, Available: $availableQuantity."
            )
        }
        return DomainResult.Success(Unit)
    }
}
