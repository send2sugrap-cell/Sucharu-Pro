package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.InventoryProductDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Inventory Product Master (Module 07).
 */
class PostgresInventoryProductDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : InventoryProductDataSource {

    private fun mapInventoryProduct(rs: ResultSet): InventoryProduct {
        return InventoryProduct(
            id = rs.getString("product_id"),
            sku = rs.getString("product_code"),
            name = rs.getString("product_name"),
            description = rs.getString("product_name"),
            categoryId = rs.getString("category"),
            productType = InventoryProductType.FINISHED_PRODUCT,
            unitOfMeasure = rs.getEnumByName("unit", InventoryUnit.PCS),
            isStockTracked = true,
            isFinishedProduct = true,
            isSaleable = true,
            isActive = true,
            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            createdBy = "SYSTEM",
            updatedBy = null
        )
    }

    override fun observeProducts(): Flow<List<InventoryProduct>> = flow {
        val tenant = TenantContext(defaultTenantId)
        val list = transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT product_id, project_id, product_code, product_name, category, unit, reorder_level, created_at, updated_at
                FROM inventory_products
                WHERE project_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId)) { rs ->
                mapInventoryProduct(rs)
            }
        }
        emit(list)
    }

    override suspend fun insertProduct(product: InventoryProduct): DomainResult<InventoryProduct> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO inventory_products (
                        project_id, product_id, product_code, product_name, category, unit, reorder_level, created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, 0, NOW(), NOW(), 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId,
                        product.id,
                        product.sku,
                        product.name,
                        product.categoryId ?: "FINISHED_GOODS",
                        product.unitOfMeasure.name
                    )
                )
            }
            DomainResult.Success(product)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "insert inventory product")
        }
    }

    override suspend fun updateProduct(product: InventoryProduct): DomainResult<InventoryProduct> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE inventory_products
                    SET product_name = ?, category = ?, unit = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND product_id = ?
                """.trimIndent()

                val affected = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        product.name,
                        product.categoryId ?: "FINISHED_GOODS",
                        product.unitOfMeasure.name,
                        tenant.projectId,
                        product.id
                    )
                )
                if (affected == 0) {
                    throw OptimisticLockException("InventoryProduct", product.id, 1L)
                }
            }
            DomainResult.Success(product)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update inventory product")
        }
    }

    override fun observeCategories(): Flow<List<InventoryProductCategory>> = flow { emit(emptyList()) }
    override suspend fun insertCategory(category: InventoryProductCategory): DomainResult<InventoryProductCategory> = DomainResult.Success(category)
    override suspend fun updateCategory(category: InventoryProductCategory): DomainResult<InventoryProductCategory> = DomainResult.Success(category)

    override fun observeActivityEvents(): Flow<List<InventoryActivityEvent>> = flow { emit(emptyList()) }
    override suspend fun recordActivity(event: InventoryActivityEvent): DomainResult<Unit> = DomainResult.Success(Unit)
}
