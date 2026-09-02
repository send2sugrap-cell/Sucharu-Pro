package com.sucharu.sucharupro.domain.model.inventory

/**
 * Reusable product category entity for organizing products (Module 07 Step 01).
 */
data class InventoryProductCategory(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(id.isNotBlank()) { "Category ID cannot be blank." }
        require(name.isNotBlank()) { "Category name cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
    }
}
