package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import kotlinx.coroutines.flow.Flow

/**
 * Reactive data source interface for Inventory Product Master management (Module 07 Step 01).
 */
interface InventoryProductDataSource {

    // Products
    fun observeProducts(): Flow<List<InventoryProduct>>
    suspend fun insertProduct(product: InventoryProduct): DomainResult<InventoryProduct>
    suspend fun updateProduct(product: InventoryProduct): DomainResult<InventoryProduct>

    // Categories
    fun observeCategories(): Flow<List<InventoryProductCategory>>
    suspend fun insertCategory(category: InventoryProductCategory): DomainResult<InventoryProductCategory>
    suspend fun updateCategory(category: InventoryProductCategory): DomainResult<InventoryProductCategory>

    // Audit Events
    fun observeActivityEvents(): Flow<List<InventoryActivityEvent>>
    suspend fun recordActivity(event: InventoryActivityEvent): DomainResult<Unit>
}
