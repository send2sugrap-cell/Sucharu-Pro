package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InventoryProductDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.InventoryActivityType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryProductRepository
import com.sucharu.sucharupro.domain.validation.InventoryProductValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe production-grade repository implementation for Inventory Product Master (Module 07 Step 01).
 */
class InventoryProductRepositoryImpl(
    private val dataSource: InventoryProductDataSource
) : InventoryProductRepository {

    private val repositoryMutex = Mutex()

    // ==========================================
    // 1. Product Queries
    // ==========================================

    override fun observeProducts(): Flow<List<InventoryProduct>> {
        return dataSource.observeProducts()
    }

    override fun observeActiveProducts(): Flow<List<InventoryProduct>> {
        return dataSource.observeProducts().map { list ->
            list.filter { it.isActive }
        }
    }

    override suspend fun getProductById(
        productId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryProduct> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbacResult = InventoryProductValidator.validateMasterViewPermission(callerRole)
            if (rbacResult is DomainResult.Error) return rbacResult
        }
        val products = dataSource.observeProducts().first()
        val product = products.find { it.id == productId }
            ?: return DomainResult.Error(message = "Product with ID '$productId' not found.")
        DomainResult.Success(product)
    }

    override suspend fun getProductBySku(
        sku: String,
        callerRole: UserRole?
    ): DomainResult<InventoryProduct> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbacResult = InventoryProductValidator.validateMasterViewPermission(callerRole)
            if (rbacResult is DomainResult.Error) return rbacResult
        }
        val normalized = sku.trim().uppercase()
        val products = dataSource.observeProducts().first()
        val product = products.find { it.normalizedSku == normalized }
            ?: return DomainResult.Error(message = "Product with SKU '$sku' not found.")
        DomainResult.Success(product)
    }

    // ==========================================
    // 2. Product Mutations
    // ==========================================

    override suspend fun createProduct(
        product: InventoryProduct,
        callerRole: UserRole?
    ): DomainResult<InventoryProduct> = repositoryMutex.withLock {
        val rbacResult = InventoryProductValidator.validateMasterAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val valResult = InventoryProductValidator.validateProduct(product)
        if (valResult is DomainResult.Error) return valResult

        val existing = dataSource.observeProducts().first()
        val skuUniqueness = InventoryProductValidator.validateSkuUniqueness(product.sku, product.id, existing)
        if (skuUniqueness is DomainResult.Error) return skuUniqueness

        val insertResult = dataSource.insertProduct(product)
        if (insertResult is DomainResult.Success) {
            recordActivityInternal(
                eventType = InventoryActivityType.PRODUCT_CREATED,
                targetId = product.id,
                targetType = "InventoryProduct",
                actorId = product.createdBy,
                description = "Created product '${product.name}' (SKU: ${product.sku})",
                timestamp = product.createdAt
            )
        }
        return insertResult
    }

    override suspend fun updateProductMetadata(
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
        callerRole: UserRole?
    ): DomainResult<InventoryProduct> = repositoryMutex.withLock {
        val rbacResult = InventoryProductValidator.validateMasterAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val products = dataSource.observeProducts().first()
        val current = products.find { it.id == productId }
            ?: return DomainResult.Error(message = "Product with ID '$productId' not found.")

        val updated = current.copy(
            name = name,
            description = description,
            categoryId = categoryId,
            productType = productType,
            unitOfMeasure = unitOfMeasure,
            isStockTracked = isStockTracked,
            isFinishedProduct = isFinishedProduct,
            isSaleable = isSaleable,
            updatedBy = updatedBy,
            updatedAt = timestamp
        )

        val valResult = InventoryProductValidator.validateProduct(updated)
        if (valResult is DomainResult.Error) return valResult

        val updateResult = dataSource.updateProduct(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                eventType = InventoryActivityType.PRODUCT_UPDATED,
                targetId = updated.id,
                targetType = "InventoryProduct",
                actorId = updatedBy,
                description = "Updated metadata for product '${updated.name}' (SKU: ${updated.sku})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun activateProduct(
        productId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryProduct> = repositoryMutex.withLock {
        val rbacResult = InventoryProductValidator.validateMasterAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val products = dataSource.observeProducts().first()
        val current = products.find { it.id == productId }
            ?: return DomainResult.Error(message = "Product with ID '$productId' not found.")

        if (current.isActive) {
            return DomainResult.Success(current)
        }

        val updated = current.copy(
            isActive = true,
            updatedBy = actorId,
            updatedAt = timestamp
        )

        val updateResult = dataSource.updateProduct(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                eventType = InventoryActivityType.PRODUCT_ACTIVATED,
                targetId = updated.id,
                targetType = "InventoryProduct",
                actorId = actorId,
                description = "Activated product '${updated.name}' (SKU: ${updated.sku})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun deactivateProduct(
        productId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryProduct> = repositoryMutex.withLock {
        val rbacResult = InventoryProductValidator.validateMasterAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val products = dataSource.observeProducts().first()
        val current = products.find { it.id == productId }
            ?: return DomainResult.Error(message = "Product with ID '$productId' not found.")

        if (!current.isActive) {
            return DomainResult.Success(current)
        }

        val updated = current.copy(
            isActive = false,
            updatedBy = actorId,
            updatedAt = timestamp
        )

        val updateResult = dataSource.updateProduct(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                eventType = InventoryActivityType.PRODUCT_DEACTIVATED,
                targetId = updated.id,
                targetType = "InventoryProduct",
                actorId = actorId,
                description = "Deactivated product '${updated.name}' (SKU: ${updated.sku})",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    // ==========================================
    // 3. Category Queries & Mutations
    // ==========================================

    override fun observeCategories(): Flow<List<InventoryProductCategory>> {
        return dataSource.observeCategories()
    }

    override fun observeActiveCategories(): Flow<List<InventoryProductCategory>> {
        return dataSource.observeCategories().map { list ->
            list.filter { it.isActive }
        }
    }

    override suspend fun getCategoryById(
        categoryId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryProductCategory> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbacResult = InventoryProductValidator.validateMasterViewPermission(callerRole)
            if (rbacResult is DomainResult.Error) return rbacResult
        }
        val categories = dataSource.observeCategories().first()
        val category = categories.find { it.id == categoryId }
            ?: return DomainResult.Error(message = "Category with ID '$categoryId' not found.")
        DomainResult.Success(category)
    }

    override suspend fun createCategory(
        category: InventoryProductCategory,
        callerRole: UserRole?
    ): DomainResult<InventoryProductCategory> = repositoryMutex.withLock {
        val rbacResult = InventoryProductValidator.validateMasterAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val valResult = InventoryProductValidator.validateCategory(category)
        if (valResult is DomainResult.Error) return valResult

        val existing = dataSource.observeCategories().first()
        val uniqueness = InventoryProductValidator.validateCategoryNameUniqueness(category.name, category.id, existing)
        if (uniqueness is DomainResult.Error) return uniqueness

        val insertResult = dataSource.insertCategory(category)
        if (insertResult is DomainResult.Success) {
            recordActivityInternal(
                eventType = InventoryActivityType.CATEGORY_CREATED,
                targetId = category.id,
                targetType = "InventoryProductCategory",
                actorId = "SYSTEM",
                description = "Created category '${category.name}'",
                timestamp = category.createdAt
            )
        }
        return insertResult
    }

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
        description: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryProductCategory> = repositoryMutex.withLock {
        val rbacResult = InventoryProductValidator.validateMasterAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val categories = dataSource.observeCategories().first()
        val current = categories.find { it.id == categoryId }
            ?: return DomainResult.Error(message = "Category with ID '$categoryId' not found.")

        val uniqueness = InventoryProductValidator.validateCategoryNameUniqueness(name, categoryId, categories)
        if (uniqueness is DomainResult.Error) return uniqueness

        val updated = current.copy(
            name = name,
            description = description,
            updatedAt = timestamp
        )

        val valResult = InventoryProductValidator.validateCategory(updated)
        if (valResult is DomainResult.Error) return valResult

        val updateResult = dataSource.updateCategory(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                eventType = InventoryActivityType.CATEGORY_UPDATED,
                targetId = updated.id,
                targetType = "InventoryProductCategory",
                actorId = "SYSTEM",
                description = "Updated category '${updated.name}'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun activateCategory(
        categoryId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryProductCategory> = repositoryMutex.withLock {
        val rbacResult = InventoryProductValidator.validateMasterAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val categories = dataSource.observeCategories().first()
        val current = categories.find { it.id == categoryId }
            ?: return DomainResult.Error(message = "Category with ID '$categoryId' not found.")

        if (current.isActive) return DomainResult.Success(current)

        val updated = current.copy(isActive = true, updatedAt = timestamp)
        val updateResult = dataSource.updateCategory(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                eventType = InventoryActivityType.CATEGORY_ACTIVATED,
                targetId = updated.id,
                targetType = "InventoryProductCategory",
                actorId = "SYSTEM",
                description = "Activated category '${updated.name}'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun deactivateCategory(
        categoryId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryProductCategory> = repositoryMutex.withLock {
        val rbacResult = InventoryProductValidator.validateMasterAdminPermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        val categories = dataSource.observeCategories().first()
        val current = categories.find { it.id == categoryId }
            ?: return DomainResult.Error(message = "Category with ID '$categoryId' not found.")

        if (!current.isActive) return DomainResult.Success(current)

        val updated = current.copy(isActive = false, updatedAt = timestamp)
        val updateResult = dataSource.updateCategory(updated)
        if (updateResult is DomainResult.Success) {
            recordActivityInternal(
                eventType = InventoryActivityType.CATEGORY_DEACTIVATED,
                targetId = updated.id,
                targetType = "InventoryProductCategory",
                actorId = "SYSTEM",
                description = "Deactivated category '${updated.name}'",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    // ==========================================
    // 4. Audit Trail
    // ==========================================

    override fun observeActivityEvents(): Flow<List<InventoryActivityEvent>> {
        return dataSource.observeActivityEvents()
    }

    private suspend fun recordActivityInternal(
        eventType: InventoryActivityType,
        targetId: String,
        targetType: String,
        actorId: String,
        actorName: String? = null,
        description: String,
        timestamp: String
    ) {
        val event = InventoryActivityEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = eventType,
            targetId = targetId,
            targetType = targetType,
            actorId = actorId,
            actorName = actorName,
            description = description,
            timestamp = timestamp
        )
        dataSource.recordActivity(event)
    }
}
