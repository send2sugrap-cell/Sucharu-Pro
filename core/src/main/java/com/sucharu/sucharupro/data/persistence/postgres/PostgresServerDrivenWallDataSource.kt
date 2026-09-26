package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.wall.*
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Form 06 Server-Driven Wall & Section Publishing tables.
 */
class PostgresServerDrivenWallDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) {

    private fun mapWallConfig(rs: ResultSet): ServerDrivenWallConfig {
        return ServerDrivenWallConfig(
            wallId = rs.getString("wall_id"),
            wallType = rs.getEnumByName("wall_type", WallCategoryType.PUBLIC),
            wallTitle = rs.getString("wall_title"),
            versionNumber = rs.getInt("version_number"),
            status = rs.getEnumByName("status", WallPublishStatus.DRAFT),
            isActivePublished = rs.getBoolean("is_active_published"),

            publicVisibility = rs.getBoolean("public_visibility"),
            guestVisibility = rs.getBoolean("guest_visibility"),
            customerVisibility = rs.getBoolean("customer_visibility"),
            affiliateVisibility = rs.getBoolean("affiliate_visibility"),

            scheduledStartAt = rs.getTimestamp("scheduled_start_at")?.toInstant()?.toString(),
            scheduledEndAt = rs.getTimestamp("scheduled_end_at")?.toInstant()?.toString(),

            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            createdBy = rs.getString("created_by") ?: "SYSTEM",
            updatedBy = rs.getString("updated_by")
        )
    }

    suspend fun insertWallConfig(config: ServerDrivenWallConfig, tenantId: String = defaultTenantId): ServerDrivenWallConfig {
        val tenant = TenantContext(tenantId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO wall_configurations (
                    project_id, wall_id, wall_type, wall_title, version_number, status, is_active_published,
                    public_visibility, guest_visibility, customer_visibility, affiliate_visibility,
                    scheduled_start_at, scheduled_end_at, created_at, updated_at, created_by, updated_by, version
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, NOW(), NOW(), ?, ?, 1
                )
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    tenant.projectId, config.wallId, config.wallType.name, config.wallTitle, config.versionNumber, config.status.name, config.isActivePublished,
                    config.publicVisibility, config.guestVisibility, config.customerVisibility, config.affiliateVisibility,
                    config.scheduledStartAt, config.scheduledEndAt, config.createdBy, config.updatedBy
                )
            )
        }
        return config
    }

    suspend fun listAllWallConfigs(tenantId: String = defaultTenantId): List<ServerDrivenWallConfig> {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM wall_configurations
                WHERE project_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId)) { rs ->
                mapWallConfig(rs)
            }
        }
    }

    suspend fun getPublishedWallConfigByType(wallType: WallCategoryType, tenantId: String = defaultTenantId): ServerDrivenWallConfig? {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM wall_configurations
                WHERE project_id = ? AND wall_type = ? AND is_active_published = TRUE
                ORDER BY version_number DESC
                LIMIT 1
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, wallType.name)) { rs ->
                mapWallConfig(rs)
            }.firstOrNull()
        }
    }
}
