package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for Inventory Product Master operations (Module 07 Step 01).
 */
object InventoryProductValidator {

    /**
     * Validates structural invariants of an [InventoryProduct].
     */
    fun validateProduct(product: InventoryProduct): DomainResult<Unit> {
        if (product.id.isBlank()) {
            return DomainResult.Error(message = "Product ID cannot be blank.")
        }
        if (product.sku.isBlank()) {
            return DomainResult.Error(message = "Product SKU cannot be blank.")
        }
        if (product.name.isBlank()) {
            return DomainResult.Error(message = "Product name cannot be blank.")
        }
        if (product.createdAt.isBlank()) {
            return DomainResult.Error(message = "Product createdAt timestamp cannot be blank.")
        }
        if (product.updatedAt.isBlank()) {
            return DomainResult.Error(message = "Product updatedAt timestamp cannot be blank.")
        }
        if (product.updatedAt < product.createdAt) {
            return DomainResult.Error(
                message = "Product updatedAt (${product.updatedAt}) cannot precede createdAt (${product.createdAt})."
            )
        }
        if (product.createdBy.isBlank()) {
            return DomainResult.Error(message = "Product createdBy actor cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates structural invariants of an [InventoryProductCategory].
     */
    fun validateCategory(category: InventoryProductCategory): DomainResult<Unit> {
        if (category.id.isBlank()) {
            return DomainResult.Error(message = "Category ID cannot be blank.")
        }
        if (category.name.isBlank()) {
            return DomainResult.Error(message = "Category name cannot be blank.")
        }
        if (category.createdAt.isBlank()) {
            return DomainResult.Error(message = "Category createdAt timestamp cannot be blank.")
        }
        if (category.updatedAt.isBlank()) {
            return DomainResult.Error(message = "Category updatedAt timestamp cannot be blank.")
        }
        if (category.updatedAt < category.createdAt) {
            return DomainResult.Error(
                message = "Category updatedAt (${category.updatedAt}) cannot precede createdAt (${category.createdAt})."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether a SKU is unique among existing products.
     */
    fun validateSkuUniqueness(
        sku: String,
        productId: String,
        existingProducts: List<InventoryProduct>
    ): DomainResult<Unit> {
        val normalized = sku.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Product SKU cannot be blank.")
        }
        val match = existingProducts.find { it.normalizedSku == normalized && it.id != productId }
        if (match != null) {
            return DomainResult.Error(message = "A product with SKU '${sku.trim()}' already exists (ID: '${match.id}').")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether a category name is unique among existing categories.
     */
    fun validateCategoryNameUniqueness(
        name: String,
        categoryId: String,
        existingCategories: List<InventoryProductCategory>
    ): DomainResult<Unit> {
        val normalized = name.trim().lowercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Category name cannot be blank.")
        }
        val match = existingCategories.find { it.name.trim().lowercase() == normalized && it.id != categoryId }
        if (match != null) {
            return DomainResult.Error(message = "A category with name '${name.trim()}' already exists (ID: '${match.id}').")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates mutation permission for inventory master catalog management.
     * Allowed: ADMIN, MANAGER.
     */
    fun validateMasterAdminPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for inventory master authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '$callerRole' is not authorized to manage inventory product master records."
            )
        }
    }

    /**
     * Validates read permission for inventory master catalog.
     * Allowed: ADMIN, MANAGER, WAREHOUSE, STAFF, QC_INSPECTOR, DESIGNER, ACCOUNTS.
     * Denied: CUSTOMER, VENDOR, AFFILIATE.
     */
    fun validateMasterViewPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE,
            UserRole.STAFF,
            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE -> DomainResult.Error(
                message = "Role '$callerRole' is not authorized to view inventory master records."
            )
        }
    }
}
