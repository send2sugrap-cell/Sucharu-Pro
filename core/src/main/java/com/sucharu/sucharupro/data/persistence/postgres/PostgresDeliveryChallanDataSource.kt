package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.DeliveryChallanDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getNullableTimestampMillis
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getTimestampMillis
import com.sucharu.sucharupro.domain.model.delivery.challan.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Production-grade PostgreSQL DataSource for Delivery Challans (Module 08).
 */
class PostgresDeliveryChallanDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : DeliveryChallanDataSource {

    private fun mapDeliveryChallan(rs: ResultSet): DeliveryChallan {
        val createdAt = rs.getTimestampMillis("created_at")
        val updatedAt = rs.getTimestampMillis("updated_at")
        val dispatchedAt = rs.getNullableTimestampMillis("dispatched_at") ?: createdAt

        return DeliveryChallan(
            challanId = rs.getString("challan_id"),
            projectId = rs.getString("project_id"),
            challanNo = rs.getString("challan_number"),
            deliveryOrderId = rs.getString("delivery_order_id"),
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = rs.getEnumByName("status", DeliveryChallanStatus.DRAFT),
            issueDate = dispatchedAt,
            notes = null,
            createdBy = rs.getString("dispatched_by") ?: "SYSTEM",
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    override fun observeChallans(projectId: String): Flow<List<DeliveryChallan>> = flow {
        val tenant = TenantContext(projectId)
        val list = transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT challan_id, project_id, challan_number, delivery_order_id, status,
                       dispatched_at, dispatched_by, created_at, updated_at
                FROM delivery_challans
                WHERE project_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(projectId)) { rs ->
                mapDeliveryChallan(rs)
            }
        }
        emit(list)
    }

    override fun observeChallansForDeliveryOrder(deliveryOrderId: String): Flow<List<DeliveryChallan>> = flow {
        val list = getChallansForDeliveryOrder(deliveryOrderId)
        emit(list)
    }

    override fun observeChallan(challanId: String): Flow<DeliveryChallan?> = flow {
        val item = getChallan(challanId)
        emit(item)
    }

    override suspend fun getChallan(challanId: String): DeliveryChallan? {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT challan_id, project_id, challan_number, delivery_order_id, status,
                       dispatched_at, dispatched_by, created_at, updated_at
                FROM delivery_challans
                WHERE project_id = ? AND challan_id = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, challanId)) { rs ->
                mapDeliveryChallan(rs)
            }
        }
    }

    override suspend fun getChallanByNo(projectId: String, challanNo: String): DeliveryChallan? {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT challan_id, project_id, challan_number, delivery_order_id, status,
                       dispatched_at, dispatched_by, created_at, updated_at
                FROM delivery_challans
                WHERE project_id = ? AND challan_number = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(projectId, challanNo)) { rs ->
                mapDeliveryChallan(rs)
            }
        }
    }

    override suspend fun getChallansForDeliveryOrder(deliveryOrderId: String): List<DeliveryChallan> {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT challan_id, project_id, challan_number, delivery_order_id, status,
                       dispatched_at, dispatched_by, created_at, updated_at
                FROM delivery_challans
                WHERE project_id = ? AND delivery_order_id = ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, deliveryOrderId)) { rs ->
                mapDeliveryChallan(rs)
            }
        }
    }

    private fun mapChallanLine(rs: ResultSet): DeliveryChallanLine {
        return DeliveryChallanLine(
            lineId = rs.getString("challan_item_id"),
            challanId = rs.getString("challan_id"),
            projectId = rs.getString("project_id"),
            deliveryOrderLineId = rs.getString("challan_id"),
            productId = rs.getString("product_id"),
            quantity = rs.getDouble("quantity"),
            notes = null,
            batchId = null,
            lotId = null
        )
    }

    override suspend fun insertChallan(challan: DeliveryChallan, lines: List<DeliveryChallanLine>) {
        val tenant = TenantContext(challan.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO delivery_challans (
                    project_id, challan_id, challan_number, delivery_order_id, status,
                    dispatched_at, dispatched_by, created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 1)
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    challan.projectId,
                    challan.challanId,
                    challan.challanNo,
                    challan.deliveryOrderId,
                    challan.status.name,
                    Timestamp(challan.issueDate),
                    challan.createdBy
                )
            )

            lines.forEach { line ->
                val lineSql = """
                    INSERT INTO delivery_challan_items (
                        project_id, challan_item_id, challan_id, product_id, quantity, created_at
                    ) VALUES (?, ?, ?, ?, ?, NOW())
                """.trimIndent()
                ctx.sqlExecutor.executeUpdate(
                    lineSql,
                    listOf(
                        challan.projectId,
                        line.lineId,
                        challan.challanId,
                        line.productId,
                        line.quantity.toInt()
                    )
                )
            }
        }
    }

    override suspend fun updateChallan(challan: DeliveryChallan) {
        val tenant = TenantContext(challan.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE delivery_challans
                SET status = ?, dispatched_at = ?, updated_at = NOW(), version = version + 1
                WHERE project_id = ? AND challan_id = ?
            """.trimIndent()

            val affected = ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    challan.status.name,
                    Timestamp(challan.issueDate),
                    challan.projectId,
                    challan.challanId
                )
            )
            if (affected == 0) {
                throw OptimisticLockException("DeliveryChallan", challan.challanId, 1L)
            }
        }
    }

    override suspend fun updateChallanWithLines(challan: DeliveryChallan, lines: List<DeliveryChallanLine>) {
        val tenant = TenantContext(challan.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            updateChallan(challan)

            // Sync lines: delete existing and re-insert
            val deleteSql = "DELETE FROM delivery_challan_items WHERE project_id = ? AND challan_id = ?"
            ctx.sqlExecutor.executeUpdate(deleteSql, listOf(challan.projectId, challan.challanId))

            lines.forEach { line ->
                val lineSql = """
                    INSERT INTO delivery_challan_items (
                        project_id, challan_item_id, challan_id, product_id, quantity, created_at
                    ) VALUES (?, ?, ?, ?, ?, NOW())
                """.trimIndent()
                ctx.sqlExecutor.executeUpdate(
                    lineSql,
                    listOf(
                        challan.projectId,
                        line.lineId,
                        challan.challanId,
                        line.productId,
                        line.quantity.toInt()
                    )
                )
            }
        }
    }

    override fun observeChallanLines(challanId: String): Flow<List<DeliveryChallanLine>> = flow {
        emit(getChallanLines(challanId))
    }

    override suspend fun getChallanLines(challanId: String): List<DeliveryChallanLine> {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT challan_item_id, project_id, challan_id, product_id, quantity, created_at
                FROM delivery_challan_items
                WHERE project_id = ? AND challan_id = ?
                ORDER BY created_at ASC
            """.trimIndent()
            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, challanId)) { rs ->
                mapChallanLine(rs)
            }
        }
    }

    override suspend fun getChallanLine(lineId: String): DeliveryChallanLine? {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT challan_item_id, project_id, challan_id, product_id, quantity, created_at
                FROM delivery_challan_items
                WHERE project_id = ? AND challan_item_id = ?
            """.trimIndent()
            ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, lineId)) { rs ->
                mapChallanLine(rs)
            }
        }
    }

    override suspend fun getLinesForChallans(challanIds: List<String>): List<DeliveryChallanLine> {
        if (challanIds.isEmpty()) return emptyList()
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val placeholders = challanIds.joinToString(",") { "?" }
            val sql = """
                SELECT challan_item_id, project_id, challan_id, product_id, quantity, created_at
                FROM delivery_challan_items
                WHERE project_id = ? AND challan_id IN ($placeholders)
                ORDER BY created_at ASC
            """.trimIndent()
            val params = listOf(tenant.projectId) + challanIds
            ctx.sqlExecutor.queryList(sql, params) { rs ->
                mapChallanLine(rs)
            }
        }
    }

    override fun observeActivityEvents(challanId: String): Flow<List<DeliveryChallanActivityEvent>> = flow { emit(emptyList()) }
    override suspend fun getActivityEvents(challanId: String): List<DeliveryChallanActivityEvent> = emptyList()
    override suspend fun insertActivityEvent(event: DeliveryChallanActivityEvent) {}
}
