package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPortalAnalyticsNotificationSearchDataSource
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of VendorPortalAnalyticsNotificationSearchDataSource with RLS enforcement (Module 13 Step 10).
 */
class PostgresVendorPortalAnalyticsNotificationSearchDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPortalAnalyticsNotificationSearchDataSource {

    override suspend fun saveNotification(notification: VendorPortalNotification): VendorPortalNotification =
        transactionManager.inTransaction(TenantContext(notification.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_notifications (
                    notification_id, tenant_id, project_id, vendor_id, category, severity,
                    status, title, message, related_entity_type, related_entity_id,
                    deep_link_target, created_at, read_at, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (notification_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    read_at = EXCLUDED.read_at,
                    metadata_json = EXCLUDED.metadata_json
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, notification.notificationId)
                stmt.setString(2, notification.tenantId)
                stmt.setString(3, notification.projectId)
                stmt.setString(4, notification.vendorId)
                stmt.setString(5, notification.category.name)
                stmt.setString(6, notification.severity.name)
                stmt.setString(7, notification.status.name)
                stmt.setString(8, notification.title)
                stmt.setString(9, notification.message)
                stmt.setString(10, notification.relatedEntityType)
                stmt.setString(11, notification.relatedEntityId)
                stmt.setString(12, notification.deepLinkTarget)
                stmt.setLong(13, notification.createdAt)
                if (notification.readAt != null) stmt.setLong(14, notification.readAt) else stmt.setNull(14, java.sql.Types.BIGINT)
                stmt.setString(15, mapToJson(notification.metadata))
                stmt.executeUpdate()
            }
            notification
        }

    override suspend fun findNotificationById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String
    ): VendorPortalNotification? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_notifications
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND notification_id = ?
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, notificationId)
                val rs = stmt.executeQuery()
                if (rs.next()) mapNotification(rs) else null
            }
        }

    override suspend fun listNotifications(
        tenantId: String,
        projectId: String,
        vendorId: String,
        category: VendorPortalNotificationCategory?,
        status: VendorPortalNotificationStatus?,
        limit: Int,
        offset: Int
    ): List<VendorPortalNotification> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?", "vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (category != null) {
                conditions.add("category = ?")
                params.add(category.name)
            }
            if (status != null) {
                conditions.add("status = ?")
                params.add(status.name)
            }

            val sql = """
                SELECT * FROM vendor_portal_notifications
                WHERE ${conditions.joinToString(" AND ")}
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                var idx = 1
                for (p in params) {
                    stmt.setObject(idx++, p)
                }
                stmt.setInt(idx++, limit)
                stmt.setInt(idx, offset)
                val rs = stmt.executeQuery()
                val list = mutableListOf<VendorPortalNotification>()
                while (rs.next()) {
                    list.add(mapNotification(rs))
                }
                list
            }
        }

    override suspend fun countUnreadNotifications(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): Int =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT COUNT(*) FROM vendor_portal_notifications
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND status = 'UNREAD'
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getInt(1) else 0
            }
        }

    override suspend fun markNotificationAsRead(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String,
        readAt: Long
    ): Boolean =
        transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val sql = """
                UPDATE vendor_portal_notifications
                SET status = 'READ', read_at = ?
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND notification_id = ?
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, readAt)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.setString(5, notificationId)
                stmt.executeUpdate() > 0
            }
        }

    override suspend fun markAllNotificationsAsRead(
        tenantId: String,
        projectId: String,
        vendorId: String,
        readAt: Long
    ): Int =
        transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val sql = """
                UPDATE vendor_portal_notifications
                SET status = 'READ', read_at = ?
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND status = 'UNREAD'
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, readAt)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeUpdate()
            }
        }

    override suspend fun archiveNotification(
        tenantId: String,
        projectId: String,
        vendorId: String,
        notificationId: String
    ): Boolean =
        transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val sql = """
                UPDATE vendor_portal_notifications
                SET status = 'ARCHIVED'
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND notification_id = ?
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, notificationId)
                stmt.executeUpdate() > 0
            }
        }

    override suspend fun savePreferences(
        preferences: VendorPortalNotificationPreference
    ): VendorPortalNotificationPreference =
        transactionManager.inTransaction(TenantContext(preferences.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_notification_preferences (
                    preference_id, tenant_id, project_id, vendor_id, email_enabled,
                    in_app_enabled, push_enabled, important_only_mode,
                    disabled_categories_json, min_severity, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, project_id, vendor_id) DO UPDATE SET
                    email_enabled = EXCLUDED.email_enabled,
                    in_app_enabled = EXCLUDED.in_app_enabled,
                    push_enabled = EXCLUDED.push_enabled,
                    important_only_mode = EXCLUDED.important_only_mode,
                    disabled_categories_json = EXCLUDED.disabled_categories_json,
                    min_severity = EXCLUDED.min_severity,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, preferences.preferenceId)
                stmt.setString(2, preferences.tenantId)
                stmt.setString(3, preferences.projectId)
                stmt.setString(4, preferences.vendorId)
                stmt.setBoolean(5, preferences.emailEnabled)
                stmt.setBoolean(6, preferences.inAppEnabled)
                stmt.setBoolean(7, preferences.pushEnabled)
                stmt.setBoolean(8, preferences.importantOnlyMode)
                stmt.setString(9, preferences.disabledCategories.joinToString(",") { it.name })
                stmt.setString(10, preferences.minSeverity.name)
                stmt.setLong(11, preferences.updatedAt)
                stmt.executeUpdate()
            }
            preferences
        }

    override suspend fun getPreferences(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): VendorPortalNotificationPreference? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_notification_preferences
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                val rs = stmt.executeQuery()
                if (rs.next()) mapPreferences(rs) else null
            }
        }

    override suspend fun saveAnalyticsSnapshot(
        snapshot: VendorPortalAnalyticsSnapshot
    ): VendorPortalAnalyticsSnapshot =
        transactionManager.inTransaction(TenantContext(snapshot.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_analytics_snapshots (
                    snapshot_id, tenant_id, project_id, vendor_id, period,
                    calculation_version, metrics_json, calculated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_id) DO UPDATE SET
                    calculation_version = EXCLUDED.calculation_version,
                    metrics_json = EXCLUDED.metrics_json,
                    calculated_at = EXCLUDED.calculated_at
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, snapshot.snapshotId)
                stmt.setString(2, snapshot.tenantId)
                stmt.setString(3, snapshot.projectId)
                stmt.setString(4, snapshot.vendorId)
                stmt.setString(5, snapshot.period.name)
                stmt.setInt(6, snapshot.calculationVersion)
                stmt.setString(7, snapshot.metricsJson)
                stmt.setLong(8, snapshot.calculatedAt)
                stmt.executeUpdate()
            }
            snapshot
        }

    override suspend fun getLatestAnalyticsSnapshot(
        tenantId: String,
        projectId: String,
        vendorId: String,
        period: VendorPortalPeriod
    ): VendorPortalAnalyticsSnapshot? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_analytics_snapshots
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND period = ?
                ORDER BY calculated_at DESC
                LIMIT 1
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, period.name)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    VendorPortalAnalyticsSnapshot(
                        snapshotId = rs.getString("snapshot_id"),
                        tenantId = rs.getString("tenant_id"),
                        projectId = rs.getString("project_id"),
                        vendorId = rs.getString("vendor_id"),
                        period = VendorPortalPeriod.valueOf(rs.getString("period")),
                        calculationVersion = rs.getInt("calculation_version"),
                        metricsJson = rs.getString("metrics_json"),
                        calculatedAt = rs.getLong("calculated_at")
                    )
                } else null
            }
        }

    private fun mapNotification(rs: ResultSet): VendorPortalNotification =
        VendorPortalNotification(
            notificationId = rs.getString("notification_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            category = try { VendorPortalNotificationCategory.valueOf(rs.getString("category")) } catch (_: Exception) { VendorPortalNotificationCategory.SYSTEM },
            severity = try { VendorPortalNotificationSeverity.valueOf(rs.getString("severity")) } catch (_: Exception) { VendorPortalNotificationSeverity.NORMAL },
            status = try { VendorPortalNotificationStatus.valueOf(rs.getString("status")) } catch (_: Exception) { VendorPortalNotificationStatus.UNREAD },
            title = rs.getString("title"),
            message = rs.getString("message"),
            relatedEntityType = rs.getString("related_entity_type"),
            relatedEntityId = rs.getString("related_entity_id"),
            deepLinkTarget = rs.getString("deep_link_target"),
            createdAt = rs.getLong("created_at"),
            readAt = rs.getLong("read_at").takeIf { !rs.wasNull() },
            metadata = jsonToMap(rs.getString("metadata_json"))
        )

    private fun mapPreferences(rs: ResultSet): VendorPortalNotificationPreference {
        val catStr = rs.getString("disabled_categories_json") ?: ""
        val disabledCats = if (catStr.isNotBlank()) {
            catStr.split(",").mapNotNull {
                try { VendorPortalNotificationCategory.valueOf(it.trim()) } catch (_: Exception) { null }
            }.toSet()
        } else emptySet()

        return VendorPortalNotificationPreference(
            preferenceId = rs.getString("preference_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            emailEnabled = rs.getBoolean("email_enabled"),
            inAppEnabled = rs.getBoolean("in_app_enabled"),
            pushEnabled = rs.getBoolean("push_enabled"),
            importantOnlyMode = rs.getBoolean("important_only_mode"),
            disabledCategories = disabledCats,
            minSeverity = try { VendorPortalNotificationSeverity.valueOf(rs.getString("min_severity")) } catch (_: Exception) { VendorPortalNotificationSeverity.LOW },
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapToJson(map: Map<String, String>): String =
        map.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\":\"${it.value}\"" }

    private fun jsonToMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank() || json == "{}" || json == "[]") return emptyMap()
        val result = mutableMapOf<String, String>()
        val trimmed = json.removeSurrounding("{", "}").trim()
        if (trimmed.isNotBlank()) {
            trimmed.split(",").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size >= 2) {
                    val k = parts[0].replace("\"", "").trim()
                    val v = parts[1].replace("\"", "").trim()
                    result[k] = v
                }
            }
        }
        return result
    }
}
