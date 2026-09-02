package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customersettlement.CustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementAuditEvent
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * Production PostgreSQL DataSource for Customer Payment Allocations (Module 14 Step 06).
 */
class PostgresCustomerPaymentAllocationDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerPaymentAllocationDataSource {

    private fun mapAllocation(rs: ResultSet): CustomerPaymentAllocation {
        return CustomerPaymentAllocation(
            allocationId = rs.getString("allocation_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            customerFinancialAccountId = rs.getString("customer_financial_account_id"),
            paymentId = rs.getString("payment_id"),
            invoiceId = rs.getString("invoice_id"),
            allocatedAmount = rs.getBigDecimal("allocated_amount") ?: BigDecimal.ZERO,
            currency = rs.getString("currency") ?: "BDT",
            status = rs.getEnumByName("status", CustomerPaymentAllocationStatus.ALLOCATED),
            reversalReason = rs.getString("reversal_reason"),
            idempotencyKey = rs.getString("idempotency_key"),
            allocatedAt = rs.getLong("allocated_at"),
            allocatedBy = rs.getString("allocated_by") ?: "system",
            reversedAt = rs.getObject("reversed_at") as? Long,
            reversedBy = rs.getString("reversed_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): CustomerSettlementAuditEvent {
        return CustomerSettlementAuditEvent(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            allocationId = rs.getString("allocation_id"),
            paymentId = rs.getString("payment_id"),
            invoiceId = rs.getString("invoice_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            action = rs.getString("action"),
            previousStatus = rs.getString("previous_status"),
            newStatus = rs.getString("new_status"),
            amount = rs.getBigDecimal("amount"),
            reason = rs.getString("reason"),
            occurredAt = rs.getLong("occurred_at"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    override suspend fun createAllocation(allocation: CustomerPaymentAllocation): DomainResult<CustomerPaymentAllocation> {
        val tenantContext = TenantContext(projectId = allocation.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_payment_allocations (
                        allocation_id, tenant_id, project_id, customer_id, customer_financial_account_id,
                        payment_id, invoice_id, allocated_amount, currency, status,
                        reversal_reason, idempotency_key, allocated_at, allocated_by,
                        reversed_at, reversed_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, allocation.allocationId)
                    stmt.setString(2, allocation.tenantId)
                    stmt.setString(3, allocation.projectId)
                    stmt.setString(4, allocation.customerId)
                    stmt.setString(5, allocation.customerFinancialAccountId)
                    stmt.setString(6, allocation.paymentId)
                    stmt.setString(7, allocation.invoiceId)
                    stmt.setBigDecimal(8, allocation.allocatedAmount)
                    stmt.setString(9, allocation.currency)
                    stmt.setString(10, allocation.status.name)
                    stmt.setString(11, allocation.reversalReason)
                    stmt.setString(12, allocation.idempotencyKey)
                    stmt.setLong(13, allocation.allocatedAt)
                    stmt.setString(14, allocation.allocatedBy)
                    stmt.setObject(15, allocation.reversedAt)
                    stmt.setString(16, allocation.reversedBy)
                    stmt.setLong(17, allocation.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(allocation)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert customer payment allocation")
        }
    }

    override suspend fun getAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerPaymentAllocation> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_payment_allocations WHERE tenant_id = ? AND project_id = ? AND allocation_id = ?"
                val alloc = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, allocationId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAllocation(rs) else null
                }
                if (alloc != null) {
                    DomainResult.Success(alloc)
                } else {
                    DomainResult.Error(NoSuchElementException("CustomerPaymentAllocation '$allocationId' not found"))
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find customer payment allocation")
        }
    }

    override suspend fun findByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerPaymentAllocation?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_payment_allocations WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?"
                val alloc = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, idempotencyKey)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAllocation(rs) else null
                }
                DomainResult.Success(alloc)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed finding allocation by idempotency key")
        }
    }

    override suspend fun listAllocations(
        tenantId: String,
        projectId: String,
        paymentId: String?,
        invoiceId: String?,
        customerId: String?,
        status: CustomerPaymentAllocationStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPaymentAllocation>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)

                if (!paymentId.isNullOrBlank()) {
                    conditions.add("payment_id = ?")
                    params.add(paymentId)
                }
                if (!invoiceId.isNullOrBlank()) {
                    conditions.add("invoice_id = ?")
                    params.add(invoiceId)
                }
                if (!customerId.isNullOrBlank()) {
                    conditions.add("customer_id = ?")
                    params.add(customerId)
                }
                if (status != null) {
                    conditions.add("status = ?")
                    params.add(status.name)
                }

                val sql = """
                    SELECT * FROM customer_payment_allocations
                    WHERE ${conditions.joinToString(" AND ")}
                    ORDER BY allocated_at DESC
                    LIMIT ? OFFSET ?
                """.trimIndent()

                val list = tx.connection.prepareStatement(sql).use { stmt ->
                    var idx = 1
                    for (p in params) {
                        when (p) {
                            is String -> stmt.setString(idx++, p)
                            is Long -> stmt.setLong(idx++, p)
                            is Int -> stmt.setInt(idx++, p)
                        }
                    }
                    stmt.setInt(idx++, limit)
                    stmt.setInt(idx, offset)

                    val resultList = mutableListOf<CustomerPaymentAllocation>()
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        resultList.add(mapAllocation(rs))
                    }
                    resultList
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list customer payment allocations")
        }
    }

    override suspend fun updateAllocationStatus(
        tenantId: String,
        projectId: String,
        allocationId: String,
        newStatus: CustomerPaymentAllocationStatus,
        reversalReason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPaymentAllocation> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE customer_payment_allocations
                    SET status = ?, reversal_reason = COALESCE(?, reversal_reason),
                        reversed_at = CASE WHEN ? = 'REVERSED' THEN ? ELSE reversed_at END,
                        reversed_by = CASE WHEN ? = 'REVERSED' THEN ? ELSE reversed_by END,
                        version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND allocation_id = ? AND version = ?
                """.trimIndent()
                val updatedRows = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, newStatus.name)
                    stmt.setString(2, reversalReason)
                    stmt.setString(3, newStatus.name)
                    stmt.setLong(4, now)
                    stmt.setString(5, newStatus.name)
                    stmt.setString(6, actorId)
                    stmt.setString(7, tenantId)
                    stmt.setString(8, projectId)
                    stmt.setString(9, allocationId)
                    stmt.setLong(10, expectedVersion)
                    stmt.executeUpdate()
                }
                if (updatedRows == 0) {
                    throw IllegalStateException("Optimistic locking failure or allocation '$allocationId' not found.")
                }
                val selectSql = "SELECT * FROM customer_payment_allocations WHERE tenant_id = ? AND project_id = ? AND allocation_id = ?"
                val updated = tx.connection.prepareStatement(selectSql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, allocationId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAllocation(rs) else throw IllegalStateException("Allocation not found after update.")
                }
                DomainResult.Success(updated)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update allocation status")
        }
    }

    override suspend fun recordAuditEvent(event: CustomerSettlementAuditEvent): DomainResult<CustomerSettlementAuditEvent> {
        val tenantContext = TenantContext(projectId = event.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_settlement_audit_events (
                        audit_id, tenant_id, project_id, customer_id, allocation_id,
                        payment_id, invoice_id, actor_id, actor_role, action,
                        previous_status, new_status, amount, reason, occurred_at, metadata_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, event.auditId)
                    stmt.setString(2, event.tenantId)
                    stmt.setString(3, event.projectId)
                    stmt.setString(4, event.customerId)
                    stmt.setString(5, event.allocationId)
                    stmt.setString(6, event.paymentId)
                    stmt.setString(7, event.invoiceId)
                    stmt.setString(8, event.actorId)
                    stmt.setString(9, event.actorRole)
                    stmt.setString(10, event.action)
                    stmt.setString(11, event.previousStatus)
                    stmt.setString(12, event.newStatus)
                    stmt.setObject(13, event.amount)
                    stmt.setString(14, event.reason)
                    stmt.setLong(15, event.occurredAt)
                    stmt.setString(16, event.metadataJson)
                    stmt.executeUpdate()
                }
                DomainResult.Success(event)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to record settlement audit event")
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        allocationId: String?,
        paymentId: String?,
        invoiceId: String?
    ): DomainResult<List<CustomerSettlementAuditEvent>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)

                if (!allocationId.isNullOrBlank()) {
                    conditions.add("allocation_id = ?")
                    params.add(allocationId)
                }
                if (!paymentId.isNullOrBlank()) {
                    conditions.add("payment_id = ?")
                    params.add(paymentId)
                }
                if (!invoiceId.isNullOrBlank()) {
                    conditions.add("invoice_id = ?")
                    params.add(invoiceId)
                }

                val sql = """
                    SELECT * FROM customer_settlement_audit_events
                    WHERE ${conditions.joinToString(" AND ")}
                    ORDER BY occurred_at DESC
                """.trimIndent()

                val list = tx.connection.prepareStatement(sql).use { stmt ->
                    var idx = 1
                    for (p in params) {
                        stmt.setString(idx++, p.toString())
                    }
                    val resultList = mutableListOf<CustomerSettlementAuditEvent>()
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        resultList.add(mapAuditEvent(rs))
                    }
                    resultList
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to fetch settlement audit events")
        }
    }
}
