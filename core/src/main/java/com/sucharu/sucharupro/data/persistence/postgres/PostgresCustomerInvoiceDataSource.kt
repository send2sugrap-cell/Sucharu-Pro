package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.CustomerInvoiceDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * Production-grade PostgreSQL DataSource for Customer Invoices (Module 14 Step 02).
 */
class PostgresCustomerInvoiceDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerInvoiceDataSource {

    private fun mapLine(rs: ResultSet): CustomerInvoiceLine {
        return CustomerInvoiceLine(
            lineId = rs.getString("line_id"),
            invoiceId = rs.getString("invoice_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            description = rs.getString("description"),
            productId = rs.getString("product_id"),
            jobId = rs.getString("job_id"),
            quantity = rs.getBigDecimal("quantity") ?: BigDecimal.ONE,
            unit = rs.getString("unit") ?: "PCS",
            unitPrice = rs.getBigDecimal("unit_price") ?: BigDecimal.ZERO,
            discount = rs.getBigDecimal("discount") ?: BigDecimal.ZERO,
            tax = rs.getBigDecimal("tax") ?: BigDecimal.ZERO,
            lineTotal = rs.getBigDecimal("line_total") ?: BigDecimal.ZERO,
            notes = rs.getString("notes"),
            lineOrder = rs.getInt("line_order")
        )
    }

    private fun mapInvoice(rs: ResultSet, lines: List<CustomerInvoiceLine> = emptyList()): CustomerInvoice {
        return CustomerInvoice(
            invoiceId = rs.getString("invoice_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            customerFinancialAccountId = rs.getString("customer_financial_account_id"),
            invoiceNumber = rs.getString("invoice_number"),
            sourceOrderId = rs.getString("source_order_id"),
            sourceJobId = rs.getString("source_job_id"),
            issueDate = rs.getLong("issue_date").takeIf { !rs.wasNull() },
            dueDate = rs.getLong("due_date").takeIf { !rs.wasNull() },
            currency = rs.getString("currency") ?: "BDT",
            subtotal = rs.getBigDecimal("subtotal") ?: BigDecimal.ZERO,
            discount = rs.getBigDecimal("discount") ?: BigDecimal.ZERO,
            tax = rs.getBigDecimal("tax") ?: BigDecimal.ZERO,
            adjustment = rs.getBigDecimal("adjustment") ?: BigDecimal.ZERO,
            grandTotal = rs.getBigDecimal("grand_total") ?: BigDecimal.ZERO,
            paidAmount = rs.getBigDecimal("paid_amount") ?: BigDecimal.ZERO,
            dueAmount = rs.getBigDecimal("due_amount") ?: BigDecimal.ZERO,
            status = rs.getEnumByName("status", CustomerInvoiceStatus.DRAFT),
            lines = lines,
            notes = rs.getString("notes"),
            cancellationReason = rs.getString("cancellation_reason"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): CustomerInvoiceAuditEvent {
        return CustomerInvoiceAuditEvent(
            auditId = rs.getString("audit_id"),
            invoiceId = rs.getString("invoice_id"),
            customerId = rs.getString("customer_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            action = rs.getString("action"),
            previousStatus = rs.getString("previous_status")?.let { CustomerInvoiceStatus.valueOf(it) },
            newStatus = rs.getString("new_status")?.let { CustomerInvoiceStatus.valueOf(it) },
            reason = rs.getString("reason"),
            occurredAt = rs.getLong("occurred_at"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    override suspend fun insertInvoice(invoice: CustomerInvoice): DomainResult<CustomerInvoice> {
        val tenantContext = TenantContext(projectId = invoice.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_invoices (
                        invoice_id, tenant_id, project_id, customer_id, customer_financial_account_id,
                        invoice_number, source_order_id, source_job_id, issue_date, due_date,
                        currency, subtotal, discount, tax, adjustment,
                        grand_total, paid_amount, due_amount, status, notes,
                        cancellation_reason, created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, invoice.invoiceId)
                    stmt.setString(2, invoice.tenantId)
                    stmt.setString(3, invoice.projectId)
                    stmt.setString(4, invoice.customerId)
                    stmt.setString(5, invoice.customerFinancialAccountId)
                    stmt.setString(6, invoice.invoiceNumber)
                    stmt.setString(7, invoice.sourceOrderId)
                    stmt.setString(8, invoice.sourceJobId)
                    if (invoice.issueDate != null) stmt.setLong(9, invoice.issueDate) else stmt.setNull(9, java.sql.Types.BIGINT)
                    if (invoice.dueDate != null) stmt.setLong(10, invoice.dueDate) else stmt.setNull(10, java.sql.Types.BIGINT)
                    stmt.setString(11, invoice.currency)
                    stmt.setBigDecimal(12, invoice.subtotal)
                    stmt.setBigDecimal(13, invoice.discount)
                    stmt.setBigDecimal(14, invoice.tax)
                    stmt.setBigDecimal(15, invoice.adjustment)
                    stmt.setBigDecimal(16, invoice.grandTotal)
                    stmt.setBigDecimal(17, invoice.paidAmount)
                    stmt.setBigDecimal(18, invoice.dueAmount)
                    stmt.setString(19, invoice.status.name)
                    stmt.setString(20, invoice.notes)
                    stmt.setString(21, invoice.cancellationReason)
                    stmt.setLong(22, invoice.createdAt)
                    stmt.setString(23, invoice.createdBy)
                    stmt.setLong(24, invoice.updatedAt)
                    stmt.setString(25, invoice.updatedBy)
                    stmt.setLong(26, invoice.version)
                    stmt.executeUpdate()
                }

                // Insert Lines
                val lineSql = """
                    INSERT INTO customer_invoice_lines (
                        line_id, invoice_id, tenant_id, project_id, description,
                        product_id, job_id, quantity, unit, unit_price,
                        discount, tax, line_total, notes, line_order
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(lineSql).use { stmt ->
                    for (line in invoice.lines) {
                        stmt.setString(1, line.lineId)
                        stmt.setString(2, line.invoiceId)
                        stmt.setString(3, line.tenantId)
                        stmt.setString(4, line.projectId)
                        stmt.setString(5, line.description)
                        stmt.setString(6, line.productId)
                        stmt.setString(7, line.jobId)
                        stmt.setBigDecimal(8, line.quantity)
                        stmt.setString(9, line.unit)
                        stmt.setBigDecimal(10, line.unitPrice)
                        stmt.setBigDecimal(11, line.discount)
                        stmt.setBigDecimal(12, line.tax)
                        stmt.setBigDecimal(13, line.lineTotal)
                        stmt.setString(14, line.notes)
                        stmt.setInt(15, line.lineOrder)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }

                DomainResult.Success(invoice)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert customer invoice")
        }
    }

    override suspend fun findInvoiceById(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<CustomerInvoice> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_invoices WHERE tenant_id = ? AND project_id = ? AND invoice_id = ?"
                val invoice = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, invoiceId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapInvoice(rs) else null
                } ?: return@inReadOnly DomainResult.Error(NoSuchElementException("CustomerInvoice '$invoiceId' not found"))

                // Fetch lines
                val linesSql = "SELECT * FROM customer_invoice_lines WHERE tenant_id = ? AND project_id = ? AND invoice_id = ? ORDER BY line_order ASC"
                val lines = tx.connection.prepareStatement(linesSql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, invoiceId)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<CustomerInvoiceLine>()
                    while (rs.next()) {
                        list.add(mapLine(rs))
                    }
                    list
                }
                DomainResult.Success(invoice.copy(lines = lines))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find customer invoice by ID")
        }
    }

    override suspend fun findInvoiceByNumber(
        tenantId: String,
        invoiceNumber: String
    ): DomainResult<CustomerInvoice> {
        val tenantContext = TenantContext(projectId = "DEFAULT")
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_invoices WHERE tenant_id = ? AND invoice_number = ?"
                val invoice = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, invoiceNumber)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapInvoice(rs) else null
                } ?: return@inReadOnly DomainResult.Error(NoSuchElementException("CustomerInvoice '$invoiceNumber' not found"))

                val linesSql = "SELECT * FROM customer_invoice_lines WHERE tenant_id = ? AND invoice_id = ? ORDER BY line_order ASC"
                val lines = tx.connection.prepareStatement(linesSql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, invoice.invoiceId)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<CustomerInvoiceLine>()
                    while (rs.next()) {
                        list.add(mapLine(rs))
                    }
                    list
                }
                DomainResult.Success(invoice.copy(lines = lines))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find customer invoice by number")
        }
    }

    override suspend fun listInvoices(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerInvoiceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerInvoice>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val baseSql = StringBuilder("SELECT * FROM customer_invoices WHERE tenant_id = ? AND project_id = ?")
                if (customerId != null) baseSql.append(" AND customer_id = ?")
                if (status != null) baseSql.append(" AND status = ?")
                baseSql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?")

                tx.connection.prepareStatement(baseSql.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenantId)
                    stmt.setString(idx++, projectId)
                    if (customerId != null) stmt.setString(idx++, customerId)
                    if (status != null) stmt.setString(idx++, status.name)
                    stmt.setInt(idx++, limit)
                    stmt.setInt(idx++, offset)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<CustomerInvoice>()
                    while (rs.next()) {
                        list.add(mapInvoice(rs))
                    }
                    DomainResult.Success(list)
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list customer invoices")
        }
    }

    override suspend fun updateDraft(
        tenantId: String,
        projectId: String,
        invoice: CustomerInvoice,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE customer_invoices
                    SET subtotal = ?,
                        discount = ?,
                        tax = ?,
                        adjustment = ?,
                        grand_total = ?,
                        due_amount = ?,
                        notes = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND invoice_id = ? AND version = ? AND status = 'DRAFT'
                """.trimIndent()
                val rows = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setBigDecimal(1, invoice.subtotal)
                    stmt.setBigDecimal(2, invoice.discount)
                    stmt.setBigDecimal(3, invoice.tax)
                    stmt.setBigDecimal(4, invoice.adjustment)
                    stmt.setBigDecimal(5, invoice.grandTotal)
                    stmt.setBigDecimal(6, invoice.dueAmount)
                    stmt.setString(7, invoice.notes)
                    stmt.setLong(8, now)
                    stmt.setString(9, invoice.updatedBy)
                    stmt.setString(10, tenantId)
                    stmt.setString(11, projectId)
                    stmt.setString(12, invoice.invoiceId)
                    stmt.setLong(13, expectedVersion)
                    stmt.executeUpdate()
                }
                if (rows == 0) {
                    return@inTransaction DomainResult.Error(
                        IllegalStateException("Draft update failed for invoice '${invoice.invoiceId}'. It may not be in DRAFT state or version conflict occurred.")
                    )
                }

                // Replace lines
                val delLines = "DELETE FROM customer_invoice_lines WHERE tenant_id = ? AND project_id = ? AND invoice_id = ?"
                tx.connection.prepareStatement(delLines).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, invoice.invoiceId)
                    stmt.executeUpdate()
                }

                val lineSql = """
                    INSERT INTO customer_invoice_lines (
                        line_id, invoice_id, tenant_id, project_id, description,
                        product_id, job_id, quantity, unit, unit_price,
                        discount, tax, line_total, notes, line_order
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(lineSql).use { stmt ->
                    for (line in invoice.lines) {
                        stmt.setString(1, line.lineId)
                        stmt.setString(2, line.invoiceId)
                        stmt.setString(3, line.tenantId)
                        stmt.setString(4, line.projectId)
                        stmt.setString(5, line.description)
                        stmt.setString(6, line.productId)
                        stmt.setString(7, line.jobId)
                        stmt.setBigDecimal(8, line.quantity)
                        stmt.setString(9, line.unit)
                        stmt.setBigDecimal(10, line.unitPrice)
                        stmt.setBigDecimal(11, line.discount)
                        stmt.setBigDecimal(12, line.tax)
                        stmt.setBigDecimal(13, line.lineTotal)
                        stmt.setString(14, line.notes)
                        stmt.setInt(15, line.lineOrder)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }

                findInvoiceById(tenantId, projectId, invoice.invoiceId)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update draft invoice")
        }
    }

    override suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newStatus: CustomerInvoiceStatus,
        reason: String?,
        actorId: String,
        issueDate: Long?,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE customer_invoices
                    SET status = ?,
                        issue_date = COALESCE(?, issue_date),
                        cancellation_reason = CASE WHEN ? IN ('CANCELLED', 'VOID') THEN ? ELSE cancellation_reason END,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND invoice_id = ? AND version = ?
                """.trimIndent()
                val rows = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, newStatus.name)
                    if (issueDate != null) stmt.setLong(2, issueDate) else stmt.setNull(2, java.sql.Types.BIGINT)
                    stmt.setString(3, newStatus.name)
                    stmt.setString(4, reason)
                    stmt.setLong(5, now)
                    stmt.setString(6, actorId)
                    stmt.setString(7, tenantId)
                    stmt.setString(8, projectId)
                    stmt.setString(9, invoiceId)
                    stmt.setLong(10, expectedVersion)
                    stmt.executeUpdate()
                }
                if (rows == 0) {
                    return@inTransaction DomainResult.Error(
                        IllegalStateException("Status update failed for invoice '$invoiceId'. Version conflict or invoice not found.")
                    )
                }
                findInvoiceById(tenantId, projectId, invoiceId)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update invoice status")
        }
    }

    override suspend fun updatePaymentBalance(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newPaidAmount: BigDecimal,
        newDueAmount: BigDecimal,
        newStatus: CustomerInvoiceStatus,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE customer_invoices
                    SET paid_amount = ?,
                        due_amount = ?,
                        status = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND invoice_id = ? AND version = ?
                """.trimIndent()
                val rows = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setBigDecimal(1, newPaidAmount)
                    stmt.setBigDecimal(2, newDueAmount)
                    stmt.setString(3, newStatus.name)
                    stmt.setLong(4, now)
                    stmt.setString(5, actorId)
                    stmt.setString(6, tenantId)
                    stmt.setString(7, projectId)
                    stmt.setString(8, invoiceId)
                    stmt.setLong(9, expectedVersion)
                    stmt.executeUpdate()
                }
                if (rows == 0) {
                    return@inTransaction DomainResult.Error(
                        IllegalStateException("Payment balance update failed for invoice '$invoiceId'. Version conflict or invoice not found.")
                    )
                }
                findInvoiceById(tenantId, projectId, invoiceId)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update payment balance on invoice")
        }
    }

    override suspend fun insertAuditEvent(event: CustomerInvoiceAuditEvent): DomainResult<CustomerInvoiceAuditEvent> {
        val tenantContext = TenantContext(projectId = event.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_invoice_audit_events (
                        audit_id, invoice_id, customer_id, tenant_id, project_id,
                        actor_id, actor_role, action, previous_status, new_status,
                        reason, occurred_at, metadata_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, event.auditId)
                    stmt.setString(2, event.invoiceId)
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
            DomainResult.Error(e, e.message ?: "Failed to insert customer invoice audit event")
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<List<CustomerInvoiceAuditEvent>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = """
                    SELECT * FROM customer_invoice_audit_events
                    WHERE tenant_id = ? AND project_id = ? AND invoice_id = ?
                    ORDER BY occurred_at DESC
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, invoiceId)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<CustomerInvoiceAuditEvent>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    DomainResult.Success(list)
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get invoice audit events")
        }
    }
}
