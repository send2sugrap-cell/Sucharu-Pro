package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customerpayment.CustomerPaymentDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * Production PostgreSQL DataSource for Customer Payments (Module 14 Step 03).
 */
class PostgresCustomerPaymentDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerPaymentDataSource {

    private fun mapPayment(rs: ResultSet): CustomerPayment {
        return CustomerPayment(
            paymentId = rs.getString("payment_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            paymentNumber = rs.getString("payment_number"),
            customerId = rs.getString("customer_id"),
            customerFinancialAccountId = rs.getString("customer_financial_account_id"),
            invoiceId = rs.getString("invoice_id"),
            amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO,
            currency = rs.getString("currency") ?: "BDT",
            paymentMethod = rs.getEnumByName("payment_method", CustomerPaymentMethod.CASH),
            paymentDate = rs.getLong("payment_date"),
            referenceNumber = rs.getString("reference_number"),
            externalReference = rs.getString("external_reference"),
            notes = rs.getString("notes"),
            status = rs.getEnumByName("status", CustomerPaymentStatus.RECORDED),
            idempotencyKey = rs.getString("idempotency_key"),
            cancellationReason = rs.getString("cancellation_reason"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): CustomerPaymentAuditEvent {
        return CustomerPaymentAuditEvent(
            auditId = rs.getString("audit_id"),
            paymentId = rs.getString("payment_id"),
            customerId = rs.getString("customer_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            action = rs.getString("action"),
            previousStatus = rs.getString("previous_status")?.let { CustomerPaymentStatus.valueOf(it) },
            newStatus = rs.getString("new_status")?.let { CustomerPaymentStatus.valueOf(it) },
            reason = rs.getString("reason"),
            occurredAt = rs.getLong("occurred_at"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    override suspend fun insertPayment(payment: CustomerPayment): DomainResult<CustomerPayment> {
        val tenantContext = TenantContext(projectId = payment.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_payments (
                        payment_id, tenant_id, project_id, payment_number, customer_id,
                        customer_financial_account_id, invoice_id, amount, currency,
                        payment_method, payment_date, reference_number, external_reference,
                        notes, status, idempotency_key, cancellation_reason,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, payment.paymentId)
                    stmt.setString(2, payment.tenantId)
                    stmt.setString(3, payment.projectId)
                    stmt.setString(4, payment.paymentNumber)
                    stmt.setString(5, payment.customerId)
                    stmt.setString(6, payment.customerFinancialAccountId)
                    stmt.setString(7, payment.invoiceId)
                    stmt.setBigDecimal(8, payment.amount)
                    stmt.setString(9, payment.currency)
                    stmt.setString(10, payment.paymentMethod.name)
                    stmt.setLong(11, payment.paymentDate)
                    stmt.setString(12, payment.referenceNumber)
                    stmt.setString(13, payment.externalReference)
                    stmt.setString(14, payment.notes)
                    stmt.setString(15, payment.status.name)
                    stmt.setString(16, payment.idempotencyKey)
                    stmt.setString(17, payment.cancellationReason)
                    stmt.setLong(18, payment.createdAt)
                    stmt.setString(19, payment.createdBy)
                    stmt.setLong(20, payment.updatedAt)
                    stmt.setString(21, payment.updatedBy)
                    stmt.setLong(22, payment.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(payment)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert customer payment")
        }
    }

    override suspend fun findPaymentById(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<CustomerPayment> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_payments WHERE tenant_id = ? AND project_id = ? AND payment_id = ?"
                val payment = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, paymentId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapPayment(rs) else null
                }
                if (payment != null) {
                    DomainResult.Success(payment)
                } else {
                    DomainResult.Error(NoSuchElementException("CustomerPayment '$paymentId' not found"))
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find customer payment by ID")
        }
    }

    override suspend fun findPaymentByNumber(
        tenantId: String,
        paymentNumber: String
    ): DomainResult<CustomerPayment> {
        val tenantContext = TenantContext(projectId = "DEFAULT")
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_payments WHERE tenant_id = ? AND payment_number = ?"
                val payment = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, paymentNumber)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapPayment(rs) else null
                }
                if (payment != null) {
                    DomainResult.Success(payment)
                } else {
                    DomainResult.Error(NoSuchElementException("CustomerPayment with number '$paymentNumber' not found"))
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find customer payment by number")
        }
    }

    override suspend fun findByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerPayment?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_payments WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?"
                val payment = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, idempotencyKey)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapPayment(rs) else null
                }
                DomainResult.Success(payment)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to lookup payment by idempotency key")
        }
    }

    override suspend fun listPayments(
        tenantId: String,
        projectId: String,
        customerId: String?,
        invoiceId: String?,
        status: CustomerPaymentStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPayment>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val baseSql = StringBuilder("SELECT * FROM customer_payments WHERE tenant_id = ? AND project_id = ?")
                if (customerId != null) baseSql.append(" AND customer_id = ?")
                if (invoiceId != null) baseSql.append(" AND invoice_id = ?")
                if (status != null) baseSql.append(" AND status = ?")
                baseSql.append(" ORDER BY payment_date DESC LIMIT ? OFFSET ?")

                tx.connection.prepareStatement(baseSql.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenantId)
                    stmt.setString(idx++, projectId)
                    if (customerId != null) stmt.setString(idx++, customerId)
                    if (invoiceId != null) stmt.setString(idx++, invoiceId)
                    if (status != null) stmt.setString(idx++, status.name)
                    stmt.setInt(idx++, limit)
                    stmt.setInt(idx++, offset)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<CustomerPayment>()
                    while (rs.next()) {
                        list.add(mapPayment(rs))
                    }
                    DomainResult.Success(list)
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list customer payments")
        }
    }

    override suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        paymentId: String,
        newStatus: CustomerPaymentStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE customer_payments
                    SET status = ?,
                        cancellation_reason = CASE WHEN ? = 'CANCELLED' THEN ? ELSE cancellation_reason END,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND payment_id = ? AND version = ?
                """.trimIndent()
                val rows = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, newStatus.name)
                    stmt.setString(2, newStatus.name)
                    stmt.setString(3, reason)
                    stmt.setLong(4, now)
                    stmt.setString(5, actorId)
                    stmt.setString(6, tenantId)
                    stmt.setString(7, projectId)
                    stmt.setString(8, paymentId)
                    stmt.setLong(9, expectedVersion)
                    stmt.executeUpdate()
                }
                if (rows == 0) {
                    return@inTransaction DomainResult.Error(
                        IllegalStateException("Status update failed for payment '$paymentId'. Version conflict or payment not found.")
                    )
                }
                findPaymentById(tenantId, projectId, paymentId)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update payment status")
        }
    }

    override suspend fun insertAuditEvent(event: CustomerPaymentAuditEvent): DomainResult<CustomerPaymentAuditEvent> {
        val tenantContext = TenantContext(projectId = event.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_payment_audit_events (
                        audit_id, payment_id, customer_id, tenant_id, project_id,
                        actor_id, actor_role, action, previous_status, new_status,
                        reason, occurred_at, metadata_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, event.auditId)
                    stmt.setString(2, event.paymentId)
                    stmt.setString(3, event.customerId)
                    stmt.setString(4, event.tenantId)
                    stmt.setString(5, event.projectId)
                    stmt.setString(6, event.actorId)
                    stmt.setString(7, event.actorRole)
                    stmt.setString(8, event.action)
                    stmt.setString(9, event.previousStatus?.name)
                    stmt.setString(10, event.newStatus?.name)
                    stmt.setString(11, event.reason)
                    stmt.setLong(12, event.occurredAt)
                    stmt.setString(13, event.metadataJson)
                    stmt.executeUpdate()
                }
                DomainResult.Success(event)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert payment audit event")
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<List<CustomerPaymentAuditEvent>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = """
                    SELECT * FROM customer_payment_audit_events
                    WHERE tenant_id = ? AND project_id = ? AND payment_id = ?
                    ORDER BY occurred_at DESC
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, paymentId)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<CustomerPaymentAuditEvent>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    DomainResult.Success(list)
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get payment audit events")
        }
    }
}
