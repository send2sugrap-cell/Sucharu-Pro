package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.OrderDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getMoney
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Order domain (INFRA-01 Step 03).
 */
class PostgresOrderDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : OrderDataSource {

    private fun mapOrder(rs: ResultSet): Order {
        return Order(
            orderId = rs.getString("order_id"),
            orderNumber = rs.getString("order_number"),
            customerId = rs.getString("customer_id"),
            quotationId = rs.getString("quotation_id"),
            status = rs.getEnumByName("status", OrderStatusType.CONFIRMED),
            priority = rs.getEnumByName("priority", OrderPriority.NORMAL),
            discount = rs.getMoney("discount_amount"),
            jobHandoffStatus = rs.getEnumByName("job_handoff_status", JobHandoffStatus.NOT_READY),
            notes = rs.getString("notes"),
            confirmedAt = rs.getTimestamp("confirmed_at")?.toInstant()?.toString(),
            confirmedBy = rs.getString("confirmed_by"),
            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString()
        )
    }

    override fun observeOrders(): Flow<List<Order>> = flow {
        val tenant = TenantContext(defaultTenantId)
        val list = try {
            transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT order_id, project_id, order_number, customer_id, quotation_id,
                           status, priority, discount_amount, total_amount, job_handoff_status,
                           notes, confirmed_by, confirmed_at, created_at, updated_at
                    FROM orders
                    WHERE project_id = ?
                    ORDER BY created_at DESC
                """.trimIndent()

                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId)) { rs ->
                    mapOrder(rs)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
        emit(list)
    }

    override suspend fun fetchOrderById(orderId: String): DomainResult<Order> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            val order = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT order_id, project_id, order_number, customer_id, quotation_id,
                           status, priority, discount_amount, total_amount, job_handoff_status,
                           notes, confirmed_by, confirmed_at, created_at, updated_at
                    FROM orders
                    WHERE project_id = ? AND order_id = ?
                """.trimIndent()

                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, orderId)) { rs ->
                    mapOrder(rs)
                }
            }
            if (order != null) {
                DomainResult.Success(order)
            } else {
                DomainResult.Error(message = "Order with ID '$orderId' not found.")
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "fetch order by ID")
        }
    }

    override suspend fun insertOrder(order: Order): DomainResult<Order> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO orders (
                        project_id, order_id, order_number, customer_id, quotation_id,
                        status, priority, subtotal_amount, discount_amount, total_amount,
                        currency, job_handoff_status, notes, confirmed_by, confirmed_at,
                        created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'BDT', ?, ?, ?, NOW(), NOW(), NOW(), 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId,
                        order.orderId,
                        order.orderNumber,
                        order.customerId,
                        order.quotationId,
                        order.status.name,
                        order.priority.name,
                        order.subtotal.amount,
                        order.discount.amount,
                        order.totalAmount.amount,
                        order.jobHandoffStatus.name,
                        order.notes,
                        order.confirmedBy
                    )
                )
            }
            DomainResult.Success(order)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "insert order")
        }
    }

    override suspend fun updateOrder(order: Order): DomainResult<Order> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE orders
                    SET status = ?, priority = ?, subtotal_amount = ?, discount_amount = ?,
                        total_amount = ?, job_handoff_status = ?, notes = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND order_id = ?
                """.trimIndent()

                val affected = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        order.status.name,
                        order.priority.name,
                        order.subtotal.amount,
                        order.discount.amount,
                        order.totalAmount.amount,
                        order.jobHandoffStatus.name,
                        order.notes,
                        tenant.projectId,
                        order.orderId
                    )
                )
                if (affected == 0) {
                    throw OptimisticLockException("Order", order.orderId, 1)
                }
            }
            DomainResult.Success(order)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update order")
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        status: OrderStatusType
    ): DomainResult<Order> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE orders
                    SET status = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND order_id = ?
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(sql, listOf(status.name, tenant.projectId, orderId))
            }
            fetchOrderById(orderId)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update order status")
        }
    }

    override suspend fun updateOrderPriority(
        orderId: String,
        priority: OrderPriority
    ): DomainResult<Order> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE orders
                    SET priority = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND order_id = ?
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(sql, listOf(priority.name, tenant.projectId, orderId))
            }
            fetchOrderById(orderId)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update order priority")
        }
    }

    override suspend fun updateJobHandoffStatus(
        orderId: String,
        status: JobHandoffStatus
    ): DomainResult<Order> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE orders
                    SET job_handoff_status = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND order_id = ?
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(sql, listOf(status.name, tenant.projectId, orderId))
            }
            fetchOrderById(orderId)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update job handoff status")
        }
    }

    override suspend fun updateOrderNotes(
        orderId: String,
        notes: String?
    ): DomainResult<Order> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE orders
                    SET notes = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND order_id = ?
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(sql, listOf(notes, tenant.projectId, orderId))
            }
            fetchOrderById(orderId)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update order notes")
        }
    }

    override suspend fun cancelOrder(
        orderId: String,
        reason: String?
    ): DomainResult<Order> {
        return updateOrderStatus(orderId, OrderStatusType.CANCELLED)
    }
}
