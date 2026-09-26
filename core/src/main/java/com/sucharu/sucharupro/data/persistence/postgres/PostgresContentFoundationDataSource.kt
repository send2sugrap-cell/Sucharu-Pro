package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.content.ContentFoundation
import com.sucharu.sucharupro.domain.model.content.ContentMediaReference
import com.sucharu.sucharupro.domain.model.content.MediaType
import com.sucharu.sucharupro.domain.model.content.PublicationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.product.ProductType
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Form 01 Content & Product Foundation tables.
 */
class PostgresContentFoundationDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) {

    private fun mapContentFoundation(rs: ResultSet): ContentFoundation {
        return ContentFoundation(
            contentId = rs.getString("content_id"),
            contentType = rs.getEnumByName("content_type", ProductType.FINISHED_PRODUCT),
            productId = rs.getString("product_id"),
            productName = rs.getString("product_name"),
            productCode = rs.getString("product_code"),
            categoryId = rs.getString("category_id"),
            categoryName = rs.getString("category_name"),
            subCategoryName = rs.getString("sub_category_name"),
            templateCode = rs.getString("template_code"),
            internalReference = rs.getString("internal_reference"),
            title = rs.getString("title"),
            subtitle = rs.getString("subtitle"),
            description = rs.getString("description"),
            shortDescription = rs.getString("short_description"),
            badgeText = rs.getString("badge_text"),
            unit = rs.getEnumByName("unit", InventoryUnit.PCS),
            minimumQuantity = rs.getInt("minimum_quantity"),
            availableQuantity = rs.getInt("available_quantity"),
            isActive = rs.getBoolean("is_active"),
            publicationStatus = rs.getEnumByName("publication_status", PublicationStatus.DRAFT),
            scheduledStartAt = rs.getTimestamp("scheduled_start_at")?.toInstant()?.toString(),
            scheduledEndAt = rs.getTimestamp("scheduled_end_at")?.toInstant()?.toString(),
            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            createdBy = rs.getString("created_by") ?: "SYSTEM",
            updatedBy = rs.getString("updated_by")
        )
    }

    suspend fun insertContentFoundation(content: ContentFoundation, tenantId: String = defaultTenantId): ContentFoundation {
        val tenant = TenantContext(tenantId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO content_foundations (
                    project_id, content_id, content_type, product_id, product_name, product_code,
                    category_id, category_name, sub_category_name, template_code, internal_reference,
                    title, subtitle, description, short_description, badge_text,
                    unit, minimum_quantity, available_quantity, is_active, publication_status,
                    scheduled_start_at, scheduled_end_at, created_at, updated_at, created_by, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?, 1)
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    tenant.projectId,
                    content.contentId,
                    content.contentType.name,
                    content.productId,
                    content.productName,
                    content.productCode,
                    content.categoryId,
                    content.categoryName,
                    content.subCategoryName,
                    content.templateCode,
                    content.internalReference,
                    content.title,
                    content.subtitle,
                    content.description,
                    content.shortDescription,
                    content.badgeText,
                    content.unit.name,
                    content.minimumQuantity,
                    content.availableQuantity,
                    content.isActive,
                    content.publicationStatus.name,
                    content.scheduledStartAt,
                    content.scheduledEndAt,
                    content.createdBy,
                    content.updatedBy
                )
            )
        }
        return content
    }

    suspend fun listAllContentFoundations(tenantId: String = defaultTenantId): List<ContentFoundation> {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM content_foundations
                WHERE project_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId)) { rs ->
                mapContentFoundation(rs)
            }
        }
    }

    suspend fun getContentFoundationById(contentId: String, tenantId: String = defaultTenantId): ContentFoundation? {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM content_foundations
                WHERE project_id = ? AND content_id = ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, contentId)) { rs ->
                mapContentFoundation(rs)
            }.firstOrNull()
        }
    }
}
