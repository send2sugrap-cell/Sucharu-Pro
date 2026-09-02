package com.sucharu.sucharupro.domain.model.inventory

/**
 * Primary Product Master entity in Sucharu Pro (Module 07 Step 01).
 *
 * Defines the immutable identity and metadata for finished products, books, gifts, and goods
 * tracked within the ERP system.
 */
data class InventoryProduct(
    val id: String,
    val sku: String,
    val name: String,
    val description: String? = null,
    val categoryId: String? = null,
    val productType: InventoryProductType = InventoryProductType.FINISHED_PRODUCT,
    val unitOfMeasure: InventoryUnit = InventoryUnit.PCS,
    val isStockTracked: Boolean = true,
    val isFinishedProduct: Boolean = true,
    val isSaleable: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(id.isNotBlank()) { "Product ID cannot be blank." }
        require(sku.isNotBlank()) { "SKU cannot be blank." }
        require(name.isNotBlank()) { "Product name cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
    }

    /**
     * Normalized SKU representation for case-insensitive and whitespace-invariant matching.
     */
    val normalizedSku: String
        get() = sku.trim().uppercase()
}
