package com.sucharu.sucharupro.domain.model.inventory

/**
 * Lightweight foundational identity model describing that a product is inventory-tracked (Module 07 Step 01).
 *
 * NOTE: This is NOT a stock balance or quantity. It establishes tracking eligibility.
 */
data class InventoryStockIdentity(
    val productId: String,
    val sku: String,
    val unit: InventoryUnit,
    val stockTracked: Boolean = true,
    val createdAt: String
) {
    init {
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(sku.isNotBlank()) { "SKU cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt cannot be blank." }
    }

    companion object {
        fun fromProduct(product: InventoryProduct): InventoryStockIdentity {
            return InventoryStockIdentity(
                productId = product.id,
                sku = product.sku,
                unit = product.unitOfMeasure,
                stockTracked = product.isStockTracked,
                createdAt = product.createdAt
            )
        }
    }
}
