package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [InventoryProductDataSource] (Module 07 Step 01).
 */
class FakeInventoryProductDataSource : InventoryProductDataSource {

    private val mutex = Mutex()

    private val productsFlow = MutableStateFlow<List<InventoryProduct>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<InventoryProductCategory>>(emptyList())
    private val eventsFlow = MutableStateFlow<List<InventoryActivityEvent>>(emptyList())

    override fun observeProducts(): Flow<List<InventoryProduct>> = productsFlow.asStateFlow()

    override suspend fun insertProduct(product: InventoryProduct): DomainResult<InventoryProduct> = mutex.withLock {
        val current = productsFlow.value.toMutableList()
        if (current.any { it.id == product.id }) {
            return DomainResult.Error(message = "Product with ID '${product.id}' already exists.")
        }
        if (current.any { it.normalizedSku == product.normalizedSku }) {
            return DomainResult.Error(message = "Product with SKU '${product.sku}' already exists.")
        }
        current.add(product)
        productsFlow.value = current
        DomainResult.Success(product)
    }

    override suspend fun updateProduct(product: InventoryProduct): DomainResult<InventoryProduct> = mutex.withLock {
        val current = productsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == product.id }
        if (index == -1) {
            return DomainResult.Error(message = "Product with ID '${product.id}' not found.")
        }
        if (current.any { it.normalizedSku == product.normalizedSku && it.id != product.id }) {
            return DomainResult.Error(message = "Product with SKU '${product.sku}' already exists on another item.")
        }
        current[index] = product
        productsFlow.value = current
        DomainResult.Success(product)
    }

    override fun observeCategories(): Flow<List<InventoryProductCategory>> = categoriesFlow.asStateFlow()

    override suspend fun insertCategory(category: InventoryProductCategory): DomainResult<InventoryProductCategory> = mutex.withLock {
        val current = categoriesFlow.value.toMutableList()
        if (current.any { it.id == category.id }) {
            return DomainResult.Error(message = "Category with ID '${category.id}' already exists.")
        }
        if (current.any { it.name.trim().equals(category.name.trim(), ignoreCase = true) }) {
            return DomainResult.Error(message = "Category with name '${category.name}' already exists.")
        }
        current.add(category)
        categoriesFlow.value = current
        DomainResult.Success(category)
    }

    override suspend fun updateCategory(category: InventoryProductCategory): DomainResult<InventoryProductCategory> = mutex.withLock {
        val current = categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index == -1) {
            return DomainResult.Error(message = "Category with ID '${category.id}' not found.")
        }
        if (current.any { it.name.trim().equals(category.name.trim(), ignoreCase = true) && it.id != category.id }) {
            return DomainResult.Error(message = "Category with name '${category.name}' already exists on another category.")
        }
        current[index] = category
        categoriesFlow.value = current
        DomainResult.Success(category)
    }

    override fun observeActivityEvents(): Flow<List<InventoryActivityEvent>> = eventsFlow.asStateFlow()

    override suspend fun recordActivity(event: InventoryActivityEvent): DomainResult<Unit> = mutex.withLock {
        val current = eventsFlow.value.toMutableList()
        current.add(0, event) // newest first
        eventsFlow.value = current
        DomainResult.Success(Unit)
    }
}
