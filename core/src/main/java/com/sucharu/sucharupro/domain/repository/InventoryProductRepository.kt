package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Inventory Product Master management (Module 07 Step 01).
 */
interface InventoryProductRepository {

    // Product Queries
    fun observeProducts(): Flow<List<InventoryProduct>>
    fun observeActiveProducts(): Flow<List<InventoryProduct>>
    suspend fun getProductById(productId: String, callerRole: UserRole? = null): DomainResult<InventoryProduct>
    suspend fun getProductBySku(sku: String, callerRole: UserRole? = null): DomainResult<InventoryProduct>

    // Product Mutations
    suspend fun createProduct(
        product: InventoryProduct,
        callerRole: UserRole? = null
    ): DomainResult<InventoryProduct>

    suspend fun updateProductMetadata(
        productId: String,
        name: String,
        description: String?,
        categoryId: String?,
        productType: InventoryProductType,
        unitOfMeasure: InventoryUnit,
        isStockTracked: Boolean,
        isFinishedProduct: Boolean,
        isSaleable: Boolean,
        updatedBy: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryProduct>

    suspend fun activateProduct(
        productId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryProduct>

    suspend fun deactivateProduct(
        productId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryProduct>

    // Category Queries & Mutations
    fun observeCategories(): Flow<List<InventoryProductCategory>>
    fun observeActiveCategories(): Flow<List<InventoryProductCategory>>
    suspend fun getCategoryById(categoryId: String, callerRole: UserRole? = null): DomainResult<InventoryProductCategory>

    suspend fun createCategory(
        category: InventoryProductCategory,
        callerRole: UserRole? = null
    ): DomainResult<InventoryProductCategory>

    suspend fun updateCategory(
        categoryId: String,
        name: String,
        description: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryProductCategory>

    suspend fun activateCategory(
        categoryId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryProductCategory>

    suspend fun deactivateCategory(
        categoryId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryProductCategory>

    // Audit Trail
    fun observeActivityEvents(): Flow<List<InventoryActivityEvent>>
}
