package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customercredit.CustomerCreditDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAllocationStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAllocation
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAuditEvent
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditEntityType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditSummary
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet

/**
 * Production PostgreSQL DataSource for Customer Advances, Credits, Allocations, Adjustments, and Refunds (Module 14 Step 04).
 */
class PostgresCustomerCreditDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerCreditDataSource {

    private fun mapAdvance(rs: ResultSet): CustomerAdvance {
        return CustomerAdvance(
            advanceId = rs.getString("advance_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            customerFinancialAccountId = rs.getString("customer_financial_account_id"),
            advanceNumber = rs.getString("advance_number"),
            amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO,
            allocatedAmount = rs.getBigDecimal("allocated_amount") ?: BigDecimal.ZERO,
            availableAmount = rs.getBigDecimal("available_amount") ?: BigDecimal.ZERO,
            currency = rs.getString("currency") ?: "BDT",
            paymentMethod = rs.getEnumByName("payment_method", CustomerPaymentMethod.CASH),
            receiptDate = rs.getLong("receipt_date"),
            referenceNumber = rs.getString("reference_number"),
            externalReference = rs.getString("external_reference"),
            notes = rs.getString("notes"),
            status = rs.getEnumByName("status", CustomerAdvanceStatus.RECORDED),
            idempotencyKey = rs.getString("idempotency_key"),
            cancellationReason = rs.getString("cancellation_reason"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapAllocation(rs: ResultSet): CustomerCreditAllocation {
        return CustomerCreditAllocation(
            allocationId = rs.getString("allocation_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            customerFinancialAccountId = rs.getString("customer_financial_account_id"),
            advanceId = rs.getString("advance_id"),
            invoiceId = rs.getString("invoice_id"),
            allocatedAmount = rs.getBigDecimal("allocated_amount") ?: BigDecimal.ZERO,
            currency = rs.getString("currency") ?: "BDT",
            status = rs.getEnumByName("status", CustomerAllocationStatus.ALLOCATED),
            reversalReason = rs.getString("reversal_reason"),
            idempotencyKey = rs.getString("idempotency_key"),
            allocatedAt = rs.getLong("allocated_at"),
            allocatedBy = rs.getString("allocated_by") ?: "system",
            reversedAt = rs.getLong("reversed_at").takeIf { !rs.wasNull() },
            reversedBy = rs.getString("reversed_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapAdjustment(rs: ResultSet): CustomerAdjustment {
        return CustomerAdjustment(
            adjustmentId = rs.getString("adjustment_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            customerFinancialAccountId = rs.getString("customer_financial_account_id"),
            adjustmentNumber = rs.getString("adjustment_number"),
            adjustmentType = rs.getEnumByName("adjustment_type", CustomerAdjustmentType.CREDIT),
            amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO,
            currency = rs.getString("currency") ?: "BDT",
            reason = rs.getString("reason") ?: "",
            referenceNumber = rs.getString("reference_number"),
            notes = rs.getString("notes"),
            status = rs.getEnumByName("status", CustomerAdjustmentStatus.APPLIED),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapRefund(rs: ResultSet): CustomerRefund {
        return CustomerRefund(
            refundId = rs.getString("refund_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            customerFinancialAccountId = rs.getString("customer_financial_account_id"),
            paymentId = rs.getString("payment_id"),
            advanceId = rs.getString("advance_id"),
            refundNumber = rs.getString("refund_number"),
            amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO,
            currency = rs.getString("currency") ?: "BDT",
            refundMethod = rs.getEnumByName("refund_method", CustomerPaymentMethod.CASH),
            reason = rs.getString("reason") ?: "",
            status = rs.getEnumByName("status", CustomerRefundStatus.REQUESTED),
            rejectionReason = rs.getString("rejection_reason"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            approvedAt = rs.getLong("approved_at").takeIf { !rs.wasNull() },
            approvedBy = rs.getString("approved_by"),
            processedAt = rs.getLong("processed_at").takeIf { !rs.wasNull() },
            processedBy = rs.getString("processed_by"),
            completedAt = rs.getLong("completed_at").takeIf { !rs.wasNull() },
            completedBy = rs.getString("completed_by"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): CustomerCreditAuditEvent {
        return CustomerCreditAuditEvent(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            entityType = rs.getEnumByName("entity_type", CustomerCreditEntityType.ADVANCE),
            entityId = rs.getString("entity_id"),
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

    override suspend fun insertAdvance(advance: CustomerAdvance): DomainResult<CustomerAdvance> {
        val tenantContext = TenantContext(projectId = advance.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_advances (
                        advance_id, tenant_id, project_id, customer_id, customer_financial_account_id,
                        advance_number, amount, allocated_amount, available_amount, currency,
                        payment_method, receipt_date, reference_number, external_reference,
                        notes, status, idempotency_key, cancellation_reason,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, advance.advanceId)
                    stmt.setString(2, advance.tenantId)
                    stmt.setString(3, advance.projectId)
                    stmt.setString(4, advance.customerId)
                    stmt.setString(5, advance.customerFinancialAccountId)
                    stmt.setString(6, advance.advanceNumber)
                    stmt.setBigDecimal(7, advance.amount)
                    stmt.setBigDecimal(8, advance.allocatedAmount)
                    stmt.setBigDecimal(9, advance.availableAmount)
                    stmt.setString(10, advance.currency)
                    stmt.setString(11, advance.paymentMethod.name)
                    stmt.setLong(12, advance.receiptDate)
                    stmt.setString(13, advance.referenceNumber)
                    stmt.setString(14, advance.externalReference)
                    stmt.setString(15, advance.notes)
                    stmt.setString(16, advance.status.name)
                    stmt.setString(17, advance.idempotencyKey)
                    stmt.setString(18, advance.cancellationReason)
                    stmt.setLong(19, advance.createdAt)
                    stmt.setString(20, advance.createdBy)
                    stmt.setLong(21, advance.updatedAt)
                    stmt.setString(22, advance.updatedBy)
                    stmt.setLong(23, advance.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(advance)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert customer advance")
        }
    }

    override suspend fun findAdvanceById(
        tenantId: String,
        projectId: String,
        advanceId: String
    ): DomainResult<CustomerAdvance> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_advances WHERE tenant_id = ? AND project_id = ? AND advance_id = ?"
                val adv = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, advanceId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAdvance(rs) else null
                }
                if (adv != null) DomainResult.Success(adv) else DomainResult.Error(IllegalArgumentException("Advance '$advanceId' not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find advance by ID")
        }
    }

    override suspend fun findAdvanceByNumber(
        tenantId: String,
        advanceNumber: String
    ): DomainResult<CustomerAdvance> {
        val tenantContext = TenantContext(projectId = defaultTenantId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_advances WHERE tenant_id = ? AND advance_number = ?"
                val adv = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, advanceNumber)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAdvance(rs) else null
                }
                if (adv != null) DomainResult.Success(adv) else DomainResult.Error(IllegalArgumentException("Advance '$advanceNumber' not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find advance by number")
        }
    }

    override suspend fun findAdvanceByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerAdvance?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_advances WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?"
                val adv = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, idempotencyKey)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAdvance(rs) else null
                }
                DomainResult.Success(adv)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find advance by idempotency key")
        }
    }

    override suspend fun listAdvances(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerAdvanceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerAdvance>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sb = StringBuilder("SELECT * FROM customer_advances WHERE tenant_id = ? AND project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)
                if (customerId != null) {
                    sb.append(" AND customer_id = ?")
                    params.add(customerId)
                }
                if (status != null) {
                    sb.append(" AND status = ?")
                    params.add(status.name)
                }
                sb.append(" ORDER BY receipt_date DESC LIMIT ? OFFSET ?")
                params.add(limit)
                params.add(offset)

                val list = tx.connection.prepareStatement(sb.toString()).use { stmt ->
                    params.forEachIndexed { i, p ->
                        when (p) {
                            is String -> stmt.setString(i + 1, p)
                            is Int -> stmt.setInt(i + 1, p)
                        }
                    }
                    val rs = stmt.executeQuery()
                    val res = mutableListOf<CustomerAdvance>()
                    while (rs.next()) res.add(mapAdvance(rs))
                    res
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list advances")
        }
    }

    override suspend fun updateAdvanceAllocation(
        tenantId: String,
        projectId: String,
        advanceId: String,
        newAllocatedAmount: BigDecimal,
        newAvailableAmount: BigDecimal,
        newStatus: CustomerAdvanceStatus,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    UPDATE customer_advances
                    SET allocated_amount = ?, available_amount = ?, status = ?,
                        updated_at = ?, updated_by = ?, version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND advance_id = ? AND version = ?
                    RETURNING *
                """.trimIndent()
                val updated = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setBigDecimal(1, newAllocatedAmount)
                    stmt.setBigDecimal(2, newAvailableAmount)
                    stmt.setString(3, newStatus.name)
                    stmt.setLong(4, System.currentTimeMillis())
                    stmt.setString(5, actorId)
                    stmt.setString(6, tenantId)
                    stmt.setString(7, projectId)
                    stmt.setString(8, advanceId)
                    stmt.setLong(9, expectedVersion)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAdvance(rs) else null
                }
                if (updated != null) DomainResult.Success(updated) else DomainResult.Error(IllegalStateException("Optimistic locking failure or advance not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update advance allocation")
        }
    }

    override suspend fun cancelAdvance(
        tenantId: String,
        projectId: String,
        advanceId: String,
        reason: String,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    UPDATE customer_advances
                    SET status = 'CANCELLED', cancellation_reason = ?, available_amount = 0,
                        updated_at = ?, updated_by = ?, version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND advance_id = ? AND version = ?
                    RETURNING *
                """.trimIndent()
                val updated = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, reason)
                    stmt.setLong(2, System.currentTimeMillis())
                    stmt.setString(3, actorId)
                    stmt.setString(4, tenantId)
                    stmt.setString(5, projectId)
                    stmt.setString(6, advanceId)
                    stmt.setLong(7, expectedVersion)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAdvance(rs) else null
                }
                if (updated != null) DomainResult.Success(updated) else DomainResult.Error(IllegalStateException("Optimistic locking failure or advance not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to cancel advance")
        }
    }

    override suspend fun insertAllocation(allocation: CustomerCreditAllocation): DomainResult<CustomerCreditAllocation> {
        val tenantContext = TenantContext(projectId = allocation.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_credit_allocations (
                        allocation_id, tenant_id, project_id, customer_id, customer_financial_account_id,
                        advance_id, invoice_id, allocated_amount, currency, status,
                        reversal_reason, idempotency_key, allocated_at, allocated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, allocation.allocationId)
                    stmt.setString(2, allocation.tenantId)
                    stmt.setString(3, allocation.projectId)
                    stmt.setString(4, allocation.customerId)
                    stmt.setString(5, allocation.customerFinancialAccountId)
                    stmt.setString(6, allocation.advanceId)
                    stmt.setString(7, allocation.invoiceId)
                    stmt.setBigDecimal(8, allocation.allocatedAmount)
                    stmt.setString(9, allocation.currency)
                    stmt.setString(10, allocation.status.name)
                    stmt.setString(11, allocation.reversalReason)
                    stmt.setString(12, allocation.idempotencyKey)
                    stmt.setLong(13, allocation.allocatedAt)
                    stmt.setString(14, allocation.allocatedBy)
                    stmt.setLong(15, allocation.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(allocation)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert allocation")
        }
    }

    override suspend fun findAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerCreditAllocation> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_credit_allocations WHERE tenant_id = ? AND project_id = ? AND allocation_id = ?"
                val alloc = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, allocationId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAllocation(rs) else null
                }
                if (alloc != null) DomainResult.Success(alloc) else DomainResult.Error(IllegalArgumentException("Allocation '$allocationId' not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find allocation by ID")
        }
    }

    override suspend fun findAllocationByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerCreditAllocation?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_credit_allocations WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?"
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
            DomainResult.Error(e, e.message ?: "Failed to find allocation by idempotency key")
        }
    }

    override suspend fun listAllocations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        invoiceId: String?,
        advanceId: String?,
        status: CustomerAllocationStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerCreditAllocation>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sb = StringBuilder("SELECT * FROM customer_credit_allocations WHERE tenant_id = ? AND project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)
                if (customerId != null) {
                    sb.append(" AND customer_id = ?")
                    params.add(customerId)
                }
                if (invoiceId != null) {
                    sb.append(" AND invoice_id = ?")
                    params.add(invoiceId)
                }
                if (advanceId != null) {
                    sb.append(" AND advance_id = ?")
                    params.add(advanceId)
                }
                if (status != null) {
                    sb.append(" AND status = ?")
                    params.add(status.name)
                }
                sb.append(" ORDER BY allocated_at DESC LIMIT ? OFFSET ?")
                params.add(limit)
                params.add(offset)

                val list = tx.connection.prepareStatement(sb.toString()).use { stmt ->
                    params.forEachIndexed { i, p ->
                        when (p) {
                            is String -> stmt.setString(i + 1, p)
                            is Int -> stmt.setInt(i + 1, p)
                        }
                    }
                    val rs = stmt.executeQuery()
                    val res = mutableListOf<CustomerCreditAllocation>()
                    while (rs.next()) res.add(mapAllocation(rs))
                    res
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list allocations")
        }
    }

    override suspend fun updateAllocationStatus(
        tenantId: String,
        projectId: String,
        allocationId: String,
        newStatus: CustomerAllocationStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerCreditAllocation> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    UPDATE customer_credit_allocations
                    SET status = ?, reversal_reason = ?,
                        reversed_at = ?, reversed_by = ?, version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND allocation_id = ? AND version = ?
                    RETURNING *
                """.trimIndent()
                val updated = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, newStatus.name)
                    stmt.setString(2, reason)
                    stmt.setLong(3, System.currentTimeMillis())
                    stmt.setString(4, actorId)
                    stmt.setString(5, tenantId)
                    stmt.setString(6, projectId)
                    stmt.setString(7, allocationId)
                    stmt.setLong(8, expectedVersion)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAllocation(rs) else null
                }
                if (updated != null) DomainResult.Success(updated) else DomainResult.Error(IllegalStateException("Optimistic locking failure or allocation not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update allocation status")
        }
    }

    override suspend fun insertAdjustment(adjustment: CustomerAdjustment): DomainResult<CustomerAdjustment> {
        val tenantContext = TenantContext(projectId = adjustment.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_adjustments (
                        adjustment_id, tenant_id, project_id, customer_id, customer_financial_account_id,
                        adjustment_number, adjustment_type, amount, currency, reason,
                        reference_number, notes, status, idempotency_key,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, adjustment.adjustmentId)
                    stmt.setString(2, adjustment.tenantId)
                    stmt.setString(3, adjustment.projectId)
                    stmt.setString(4, adjustment.customerId)
                    stmt.setString(5, adjustment.customerFinancialAccountId)
                    stmt.setString(6, adjustment.adjustmentNumber)
                    stmt.setString(7, adjustment.adjustmentType.name)
                    stmt.setBigDecimal(8, adjustment.amount)
                    stmt.setString(9, adjustment.currency)
                    stmt.setString(10, adjustment.reason)
                    stmt.setString(11, adjustment.referenceNumber)
                    stmt.setString(12, adjustment.notes)
                    stmt.setString(13, adjustment.status.name)
                    stmt.setString(14, adjustment.idempotencyKey)
                    stmt.setLong(15, adjustment.createdAt)
                    stmt.setString(16, adjustment.createdBy)
                    stmt.setLong(17, adjustment.updatedAt)
                    stmt.setString(18, adjustment.updatedBy)
                    stmt.setLong(19, adjustment.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(adjustment)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert adjustment")
        }
    }

    override suspend fun findAdjustmentById(
        tenantId: String,
        projectId: String,
        adjustmentId: String
    ): DomainResult<CustomerAdjustment> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_adjustments WHERE tenant_id = ? AND project_id = ? AND adjustment_id = ?"
                val adj = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, adjustmentId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAdjustment(rs) else null
                }
                if (adj != null) DomainResult.Success(adj) else DomainResult.Error(IllegalArgumentException("Adjustment '$adjustmentId' not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find adjustment by ID")
        }
    }

    override suspend fun findAdjustmentByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerAdjustment?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_adjustments WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?"
                val adj = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, idempotencyKey)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAdjustment(rs) else null
                }
                DomainResult.Success(adj)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find adjustment by idempotency key")
        }
    }

    override suspend fun listAdjustments(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerAdjustment>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sb = StringBuilder("SELECT * FROM customer_adjustments WHERE tenant_id = ? AND project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)
                if (customerId != null) {
                    sb.append(" AND customer_id = ?")
                    params.add(customerId)
                }
                sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?")
                params.add(limit)
                params.add(offset)

                val list = tx.connection.prepareStatement(sb.toString()).use { stmt ->
                    params.forEachIndexed { i, p ->
                        when (p) {
                            is String -> stmt.setString(i + 1, p)
                            is Int -> stmt.setInt(i + 1, p)
                        }
                    }
                    val rs = stmt.executeQuery()
                    val res = mutableListOf<CustomerAdjustment>()
                    while (rs.next()) res.add(mapAdjustment(rs))
                    res
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list adjustments")
        }
    }

    override suspend fun insertRefund(refund: CustomerRefund): DomainResult<CustomerRefund> {
        val tenantContext = TenantContext(projectId = refund.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_refunds (
                        refund_id, tenant_id, project_id, customer_id, customer_financial_account_id,
                        payment_id, advance_id, refund_number, amount, currency,
                        refund_method, reason, status, rejection_reason, idempotency_key,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, refund.refundId)
                    stmt.setString(2, refund.tenantId)
                    stmt.setString(3, refund.projectId)
                    stmt.setString(4, refund.customerId)
                    stmt.setString(5, refund.customerFinancialAccountId)
                    stmt.setString(6, refund.paymentId)
                    stmt.setString(7, refund.advanceId)
                    stmt.setString(8, refund.refundNumber)
                    stmt.setBigDecimal(9, refund.amount)
                    stmt.setString(10, refund.currency)
                    stmt.setString(11, refund.refundMethod.name)
                    stmt.setString(12, refund.reason)
                    stmt.setString(13, refund.status.name)
                    stmt.setString(14, refund.rejectionReason)
                    stmt.setString(15, refund.idempotencyKey)
                    stmt.setLong(16, refund.createdAt)
                    stmt.setString(17, refund.createdBy)
                    stmt.setLong(18, refund.updatedAt)
                    stmt.setString(19, refund.updatedBy)
                    stmt.setLong(20, refund.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(refund)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert refund")
        }
    }

    override suspend fun findRefundById(
        tenantId: String,
        projectId: String,
        refundId: String
    ): DomainResult<CustomerRefund> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_refunds WHERE tenant_id = ? AND project_id = ? AND refund_id = ?"
                val ref = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, refundId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapRefund(rs) else null
                }
                if (ref != null) DomainResult.Success(ref) else DomainResult.Error(IllegalArgumentException("Refund '$refundId' not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find refund by ID")
        }
    }

    override suspend fun findRefundByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerRefund?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_refunds WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?"
                val ref = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, idempotencyKey)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapRefund(rs) else null
                }
                DomainResult.Success(ref)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find refund by idempotency key")
        }
    }

    override suspend fun listRefunds(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerRefundStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerRefund>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sb = StringBuilder("SELECT * FROM customer_refunds WHERE tenant_id = ? AND project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)
                if (customerId != null) {
                    sb.append(" AND customer_id = ?")
                    params.add(customerId)
                }
                if (status != null) {
                    sb.append(" AND status = ?")
                    params.add(status.name)
                }
                sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?")
                params.add(limit)
                params.add(offset)

                val list = tx.connection.prepareStatement(sb.toString()).use { stmt ->
                    params.forEachIndexed { i, p ->
                        when (p) {
                            is String -> stmt.setString(i + 1, p)
                            is Int -> stmt.setInt(i + 1, p)
                        }
                    }
                    val rs = stmt.executeQuery()
                    val res = mutableListOf<CustomerRefund>()
                    while (rs.next()) res.add(mapRefund(rs))
                    res
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list refunds")
        }
    }

    override suspend fun updateRefundStatus(
        tenantId: String,
        projectId: String,
        refundId: String,
        newStatus: CustomerRefundStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE customer_refunds
                    SET status = ?, rejection_reason = COALESCE(?, rejection_reason),
                        approved_at = CASE WHEN ? = 'APPROVED' THEN ? ELSE approved_at END,
                        approved_by = CASE WHEN ? = 'APPROVED' THEN ? ELSE approved_by END,
                        processed_at = CASE WHEN ? = 'PROCESSED' THEN ? ELSE processed_at END,
                        processed_by = CASE WHEN ? = 'PROCESSED' THEN ? ELSE processed_by END,
                        completed_at = CASE WHEN ? = 'COMPLETED' THEN ? ELSE completed_at END,
                        completed_by = CASE WHEN ? = 'COMPLETED' THEN ? ELSE completed_by END,
                        updated_at = ?, updated_by = ?, version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND refund_id = ? AND version = ?
                    RETURNING *
                """.trimIndent()
                val updated = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, newStatus.name)
                    stmt.setString(2, reason)
                    stmt.setString(3, newStatus.name)
                    stmt.setLong(4, now)
                    stmt.setString(5, newStatus.name)
                    stmt.setString(6, actorId)
                    stmt.setString(7, newStatus.name)
                    stmt.setLong(8, now)
                    stmt.setString(9, newStatus.name)
                    stmt.setString(10, actorId)
                    stmt.setString(11, newStatus.name)
                    stmt.setLong(12, now)
                    stmt.setString(13, newStatus.name)
                    stmt.setString(14, actorId)
                    stmt.setLong(15, now)
                    stmt.setString(16, actorId)
                    stmt.setString(17, tenantId)
                    stmt.setString(18, projectId)
                    stmt.setString(19, refundId)
                    stmt.setLong(20, expectedVersion)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapRefund(rs) else null
                }
                if (updated != null) DomainResult.Success(updated) else DomainResult.Error(IllegalStateException("Optimistic locking failure or refund not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update refund status")
        }
    }

    override suspend fun getCustomerCreditSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditSummary> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                // Advances
                val advSql = """
                    SELECT COALESCE(SUM(amount), 0) as total_advances,
                           COALESCE(SUM(allocated_amount), 0) as total_allocated,
                           COALESCE(SUM(available_amount), 0) as total_available_advances,
                           MAX(customer_financial_account_id) as financial_account_id
                    FROM customer_advances
                    WHERE tenant_id = ? AND project_id = ? AND customer_id = ? AND status != 'CANCELLED'
                """.trimIndent()
                var totalAdv = BigDecimal.ZERO
                var totalAlloc = BigDecimal.ZERO
                var totalAvailAdv = BigDecimal.ZERO
                var accountId = ""

                tx.connection.prepareStatement(advSql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, customerId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        totalAdv = rs.getBigDecimal("total_advances") ?: BigDecimal.ZERO
                        totalAlloc = rs.getBigDecimal("total_allocated") ?: BigDecimal.ZERO
                        totalAvailAdv = rs.getBigDecimal("total_available_advances") ?: BigDecimal.ZERO
                        accountId = rs.getString("financial_account_id") ?: ""
                    }
                }

                // Adjustments
                val adjSql = """
                    SELECT
                        COALESCE(SUM(CASE WHEN adjustment_type = 'CREDIT' THEN amount ELSE 0 END), 0) as total_credit_adj,
                        COALESCE(SUM(CASE WHEN adjustment_type = 'DEBIT' THEN amount ELSE 0 END), 0) as total_debit_adj,
                        MAX(customer_financial_account_id) as adj_account_id
                    FROM customer_adjustments
                    WHERE tenant_id = ? AND project_id = ? AND customer_id = ? AND status = 'APPLIED'
                """.trimIndent()
                var totalCreditAdj = BigDecimal.ZERO
                var totalDebitAdj = BigDecimal.ZERO

                tx.connection.prepareStatement(adjSql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, customerId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        totalCreditAdj = rs.getBigDecimal("total_credit_adj") ?: BigDecimal.ZERO
                        totalDebitAdj = rs.getBigDecimal("total_debit_adj") ?: BigDecimal.ZERO
                        if (accountId.isBlank()) {
                            accountId = rs.getString("adj_account_id") ?: ""
                        }
                    }
                }

                // Refunds
                val refSql = """
                    SELECT COALESCE(SUM(amount), 0) as total_refunds
                    FROM customer_refunds
                    WHERE tenant_id = ? AND project_id = ? AND customer_id = ?
                      AND status IN ('APPROVED', 'PROCESSED', 'COMPLETED')
                """.trimIndent()
                var totalRef = BigDecimal.ZERO
                tx.connection.prepareStatement(refSql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, customerId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        totalRef = rs.getBigDecimal("total_refunds") ?: BigDecimal.ZERO
                    }
                }

                val netAvail = totalAvailAdv.add(totalCreditAdj).subtract(totalDebitAdj).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)

                val summary = CustomerCreditSummary(
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    totalAdvances = totalAdv.setScale(4, RoundingMode.HALF_UP),
                    totalAllocated = totalAlloc.setScale(4, RoundingMode.HALF_UP),
                    totalAvailableCredit = netAvail,
                    totalAdjustmentsCredit = totalCreditAdj.setScale(4, RoundingMode.HALF_UP),
                    totalAdjustmentsDebit = totalDebitAdj.setScale(4, RoundingMode.HALF_UP),
                    totalRefunds = totalRef.setScale(4, RoundingMode.HALF_UP),
                    currency = "BDT"
                )
                DomainResult.Success(summary)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get customer credit summary")
        }
    }

    override suspend fun insertAuditEvent(event: CustomerCreditAuditEvent): DomainResult<CustomerCreditAuditEvent> {
        val tenantContext = TenantContext(projectId = event.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_credit_audit_events (
                        audit_id, tenant_id, project_id, customer_id, entity_type,
                        entity_id, actor_id, actor_role, action, previous_status,
                        new_status, amount, reason, occurred_at, metadata_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, event.auditId)
                    stmt.setString(2, event.tenantId)
                    stmt.setString(3, event.projectId)
                    stmt.setString(4, event.customerId)
                    stmt.setString(5, event.entityType.name)
                    stmt.setString(6, event.entityId)
                    stmt.setString(7, event.actorId)
                    stmt.setString(8, event.actorRole)
                    stmt.setString(9, event.action)
                    stmt.setString(10, event.previousStatus)
                    stmt.setString(11, event.newStatus)
                    if (event.amount != null) stmt.setBigDecimal(12, event.amount) else stmt.setNull(12, java.sql.Types.NUMERIC)
                    stmt.setString(13, event.reason)
                    stmt.setLong(14, event.occurredAt)
                    stmt.setString(15, event.metadataJson)
                    stmt.executeUpdate()
                }
                DomainResult.Success(event)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert audit event")
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        entityId: String
    ): DomainResult<List<CustomerCreditAuditEvent>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_credit_audit_events WHERE tenant_id = ? AND project_id = ? AND entity_id = ? ORDER BY occurred_at DESC"
                val list = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, entityId)
                    val rs = stmt.executeQuery()
                    val res = mutableListOf<CustomerCreditAuditEvent>()
                    while (rs.next()) res.add(mapAuditEvent(rs))
                    res
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get audit events")
        }
    }
}
