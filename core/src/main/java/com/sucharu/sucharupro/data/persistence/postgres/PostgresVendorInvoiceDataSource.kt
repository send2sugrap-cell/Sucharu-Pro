package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorInvoiceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorInvoiceDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorInvoiceDataSource {

    private val invoiceFlows = mutableMapOf<String, MutableStateFlow<List<VendorInvoice>>>()

    private fun mapItemRow(rs: ResultSet): VendorInvoiceItem {
        return VendorInvoiceItem(
            itemId = rs.getString("item_id"),
            invoiceId = rs.getString("invoice_id"),
            purchaseOrderItemId = rs.getString("purchase_order_item_id"),
            deliveryReceiptItemId = rs.getString("delivery_receipt_item_id"),
            description = rs.getString("description"),
            quantity = rs.getBigDecimal("quantity"),
            unitOfMeasure = UnitOfMeasure.valueOf(rs.getString("unit_of_measure")),
            unitPrice = Money(rs.getBigDecimal("unit_price")),
            taxRate = rs.getBigDecimal("tax_rate"),
            taxAmount = Money(rs.getBigDecimal("tax_amount")),
            discountAmount = Money(rs.getBigDecimal("discount_amount")),
            lineTotal = Money(rs.getBigDecimal("line_total")),
            sequence = rs.getInt("sequence"),
            version = rs.getLong("version")
        )
    }

    private fun mapInvoiceRow(rs: ResultSet, items: List<VendorInvoiceItem> = emptyList()): VendorInvoice {
        return VendorInvoice(
            invoiceId = rs.getString("invoice_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            invoiceNumber = rs.getString("invoice_number"),
            vendorInvoiceNumber = rs.getString("vendor_invoice_number"),
            invoiceDate = rs.getTimestamp("invoice_date")?.time ?: System.currentTimeMillis(),
            receivedDate = rs.getTimestamp("received_date")?.time ?: System.currentTimeMillis(),
            currency = rs.getString("currency"),
            subtotal = Money(rs.getBigDecimal("subtotal")),
            taxAmount = Money(rs.getBigDecimal("tax_amount")),
            discountAmount = Money(rs.getBigDecimal("discount_amount")),
            shippingAmount = Money(rs.getBigDecimal("shipping_amount")),
            otherCharges = Money(rs.getBigDecimal("other_charges")),
            totalAmount = Money(rs.getBigDecimal("total_amount")),
            notes = rs.getString("notes"),
            status = VendorInvoiceStatus.valueOf(rs.getString("status")),
            matchStatus = VendorInvoiceMatchStatus.valueOf(rs.getString("match_status")),
            items = items,
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapMatchRow(rs: ResultSet, lines: List<VendorInvoiceMatchLine> = emptyList()): VendorInvoiceMatch {
        return VendorInvoiceMatch(
            matchId = rs.getString("match_id"),
            projectId = rs.getString("project_id"),
            invoiceId = rs.getString("invoice_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            matchStatus = VendorInvoiceMatchStatus.valueOf(rs.getString("match_status")),
            matchedAt = rs.getTimestamp("matched_at")?.time ?: System.currentTimeMillis(),
            matchedBy = rs.getString("matched_by"),
            subtotalVariance = Money(rs.getBigDecimal("subtotal_variance")),
            quantityVariance = rs.getBigDecimal("quantity_variance"),
            priceVariance = Money(rs.getBigDecimal("price_variance")),
            taxVariance = Money(rs.getBigDecimal("tax_variance")),
            totalVariance = Money(rs.getBigDecimal("total_variance")),
            currencyMismatch = rs.getBoolean("currency_mismatch"),
            vendorMismatch = rs.getBoolean("vendor_mismatch"),
            unmatchedLineCount = rs.getInt("unmatched_line_count"),
            exceptionCount = rs.getInt("exception_count"),
            lines = lines,
            version = rs.getLong("version")
        )
    }

    private fun mapMatchLineRow(rs: ResultSet): VendorInvoiceMatchLine {
        return VendorInvoiceMatchLine(
            matchLineId = rs.getString("match_line_id"),
            matchId = rs.getString("match_id"),
            invoiceItemId = rs.getString("invoice_item_id"),
            purchaseOrderItemId = rs.getString("purchase_order_item_id"),
            deliveryReceiptItemId = rs.getString("delivery_receipt_item_id"),
            description = rs.getString("description"),
            orderedQuantity = rs.getBigDecimal("ordered_quantity"),
            receivedQuantity = rs.getBigDecimal("received_quantity"),
            invoicedQuantity = rs.getBigDecimal("invoiced_quantity"),
            orderedUnitPrice = Money(rs.getBigDecimal("ordered_unit_price")),
            invoicedUnitPrice = Money(rs.getBigDecimal("invoiced_unit_price")),
            quantityVariance = rs.getBigDecimal("quantity_variance"),
            priceVariance = Money(rs.getBigDecimal("price_variance")),
            amountVariance = Money(rs.getBigDecimal("amount_variance")),
            matchStatus = VendorInvoiceMatchStatus.valueOf(rs.getString("match_status")),
            exceptionReason = rs.getString("exception_reason")
        )
    }

    private fun mapExceptionRow(rs: ResultSet): VendorInvoiceException {
        return VendorInvoiceException(
            exceptionId = rs.getString("exception_id"),
            projectId = rs.getString("project_id"),
            invoiceId = rs.getString("invoice_id"),
            matchId = rs.getString("match_id"),
            exceptionType = VendorInvoiceExceptionType.valueOf(rs.getString("exception_type")),
            description = rs.getString("description"),
            resolved = rs.getBoolean("resolved"),
            resolvedBy = rs.getString("resolved_by"),
            resolvedAt = rs.getTimestamp("resolved_at")?.time,
            resolutionNotes = rs.getString("resolution_notes"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis()
        )
    }

    private fun mapAuditRow(rs: ResultSet): VendorInvoiceAuditEvent {
        return VendorInvoiceAuditEvent(
            auditId = rs.getString("audit_id"),
            projectId = rs.getString("project_id"),
            invoiceId = rs.getString("invoice_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            correlationId = rs.getString("correlation_id"),
            occurredAt = rs.getTimestamp("occurred_at")?.time ?: System.currentTimeMillis(),
            details = rs.getString("details")
        )
    }

    override fun observeInvoices(projectId: String, vendorId: String?, purchaseOrderId: String?): Flow<List<VendorInvoice>> {
        val key = "$projectId:$vendorId:$purchaseOrderId"
        return synchronized(invoiceFlows) {
            invoiceFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findById(projectId: String, invoiceId: String): DomainResult<VendorInvoice> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val invoice = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_invoices WHERE project_id = ? AND invoice_id = ?"
                val raw = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, invoiceId)) { rs -> mapInvoiceRow(rs) }
                if (raw != null) {
                    val itemSql = "SELECT * FROM vendor_invoice_items WHERE project_id = ? AND invoice_id = ? ORDER BY sequence ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, invoiceId)) { rs -> mapItemRow(rs) }
                    raw.copy(items = items)
                } else null
            }
            if (invoice != null) DomainResult.Success(invoice) else DomainResult.Error(NoSuchElementException("Vendor invoice '$invoiceId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor invoice by id")
        }
    }

    override suspend fun findByInvoiceNumber(projectId: String, invoiceNumber: String): DomainResult<VendorInvoice> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val invoice = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_invoices WHERE project_id = ? AND invoice_number = ?"
                val raw = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, invoiceNumber)) { rs -> mapInvoiceRow(rs) }
                if (raw != null) {
                    val itemSql = "SELECT * FROM vendor_invoice_items WHERE project_id = ? AND invoice_id = ? ORDER BY sequence ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, raw.invoiceId)) { rs -> mapItemRow(rs) }
                    raw.copy(items = items)
                } else null
            }
            if (invoice != null) DomainResult.Success(invoice) else DomainResult.Error(NoSuchElementException("Vendor invoice '$invoiceNumber' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor invoice by number")
        }
    }

    override suspend fun findByVendorInvoiceNumber(projectId: String, vendorId: String, vendorInvoiceNumber: String): DomainResult<VendorInvoice> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val invoice = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_invoices WHERE project_id = ? AND vendor_id = ? AND vendor_invoice_number = ?"
                val raw = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, vendorId, vendorInvoiceNumber)) { rs -> mapInvoiceRow(rs) }
                if (raw != null) {
                    val itemSql = "SELECT * FROM vendor_invoice_items WHERE project_id = ? AND invoice_id = ? ORDER BY sequence ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, raw.invoiceId)) { rs -> mapItemRow(rs) }
                    raw.copy(items = items)
                } else null
            }
            if (invoice != null) DomainResult.Success(invoice) else DomainResult.Error(NoSuchElementException("Vendor invoice '$vendorInvoiceNumber' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor invoice by vendor invoice number")
        }
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        purchaseOrderId: String?,
        status: VendorInvoiceStatus?,
        matchStatus: VendorInvoiceMatchStatus?
    ): DomainResult<List<VendorInvoice>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = StringBuilder("SELECT * FROM vendor_invoices WHERE project_id = ?")
                val params = mutableListOf<Any>(tenant.projectId)

                if (vendorId != null) {
                    sql.append(" AND vendor_id = ?")
                    params.add(vendorId)
                }
                if (purchaseOrderId != null) {
                    sql.append(" AND purchase_order_id = ?")
                    params.add(purchaseOrderId)
                }
                if (status != null) {
                    sql.append(" AND status = ?")
                    params.add(status.name)
                }
                if (matchStatus != null) {
                    sql.append(" AND match_status = ?")
                    params.add(matchStatus.name)
                }
                sql.append(" ORDER BY created_at DESC")

                val rawInvoices = ctx.sqlExecutor.queryList(sql.toString(), params) { rs -> mapInvoiceRow(rs) }
                rawInvoices.map { raw ->
                    val itemSql = "SELECT * FROM vendor_invoice_items WHERE project_id = ? AND invoice_id = ? ORDER BY sequence ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, raw.invoiceId)) { rs -> mapItemRow(rs) }
                    raw.copy(items = items)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor invoices")
        }
    }

    override suspend fun createInvoice(invoice: VendorInvoice): DomainResult<VendorInvoice> {
        val tenant = TenantContext(invoice.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    INSERT INTO vendor_invoices (
                        project_id, invoice_id, tenant_id, vendor_id, purchase_order_id,
                        invoice_number, vendor_invoice_number, invoice_date, received_date,
                        currency, subtotal, tax_amount, discount_amount, shipping_amount,
                        other_charges, total_amount, notes, status, match_status,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, invoice.invoiceId, invoice.tenantId, invoice.vendorId, invoice.purchaseOrderId,
                        invoice.invoiceNumber, invoice.vendorInvoiceNumber, Timestamp(invoice.invoiceDate), Timestamp(invoice.receivedDate),
                        invoice.currency, invoice.subtotal.amount, invoice.taxAmount.amount, invoice.discountAmount.amount,
                        invoice.shippingAmount.amount, invoice.otherCharges.amount, invoice.totalAmount.amount,
                        invoice.notes, invoice.status.name, invoice.matchStatus.name,
                        now, invoice.createdBy, now, invoice.updatedBy
                    )
                )

                val itemSql = """
                    INSERT INTO vendor_invoice_items (
                        project_id, item_id, invoice_id, purchase_order_item_id, delivery_receipt_item_id,
                        description, quantity, unit_of_measure, unit_price, tax_rate,
                        tax_amount, discount_amount, line_total, sequence, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                invoice.items.forEach { item ->
                    ctx.sqlExecutor.executeUpdate(
                        itemSql,
                        listOf(
                            tenant.projectId, item.itemId, invoice.invoiceId, item.purchaseOrderItemId, item.deliveryReceiptItemId,
                            item.description, item.quantity, item.unitOfMeasure.name, item.unitPrice.amount, item.taxRate,
                            item.taxAmount.amount, item.discountAmount.amount, item.lineTotal.amount, item.sequence
                        )
                    )
                }
                invoice.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor invoice")
        }
    }

    override suspend fun updateInvoice(invoice: VendorInvoice): DomainResult<VendorInvoice> {
        val tenant = TenantContext(invoice.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    UPDATE vendor_invoices SET
                        vendor_invoice_number = ?, invoice_date = ?, received_date = ?,
                        subtotal = ?, tax_amount = ?, discount_amount = ?, shipping_amount = ?,
                        other_charges = ?, total_amount = ?, notes = ?, status = ?, match_status = ?,
                        updated_at = ?, updated_by = ?, version = version + 1
                    WHERE project_id = ? AND invoice_id = ? AND version = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        invoice.vendorInvoiceNumber, Timestamp(invoice.invoiceDate), Timestamp(invoice.receivedDate),
                        invoice.subtotal.amount, invoice.taxAmount.amount, invoice.discountAmount.amount, invoice.shippingAmount.amount,
                        invoice.otherCharges.amount, invoice.totalAmount.amount, invoice.notes, invoice.status.name, invoice.matchStatus.name,
                        now, invoice.updatedBy,
                        tenant.projectId, invoice.invoiceId, invoice.version
                    )
                )

                if (rows == 0) {
                    throw IllegalStateException("Optimistic concurrency conflict on vendor invoice '${invoice.invoiceId}'")
                }

                ctx.sqlExecutor.executeUpdate("DELETE FROM vendor_invoice_items WHERE project_id = ? AND invoice_id = ?", listOf(tenant.projectId, invoice.invoiceId))

                val itemSql = """
                    INSERT INTO vendor_invoice_items (
                        project_id, item_id, invoice_id, purchase_order_item_id, delivery_receipt_item_id,
                        description, quantity, unit_of_measure, unit_price, tax_rate,
                        tax_amount, discount_amount, line_total, sequence, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                invoice.items.forEach { item ->
                    ctx.sqlExecutor.executeUpdate(
                        itemSql,
                        listOf(
                            tenant.projectId, item.itemId, invoice.invoiceId, item.purchaseOrderItemId, item.deliveryReceiptItemId,
                            item.description, item.quantity, item.unitOfMeasure.name, item.unitPrice.amount, item.taxRate,
                            item.taxAmount.amount, item.discountAmount.amount, item.lineTotal.amount, item.sequence
                        )
                    )
                }
                invoice.copy(updatedAt = now.time, version = invoice.version + 1)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor invoice")
        }
    }

    override suspend fun updateStatus(
        projectId: String,
        invoiceId: String,
        status: VendorInvoiceStatus,
        matchStatus: VendorInvoiceMatchStatus?,
        updatedBy: String
    ): DomainResult<VendorInvoice> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = if (matchStatus != null) {
                    "UPDATE vendor_invoices SET status = ?, match_status = ?, updated_at = ?, updated_by = ?, version = version + 1 WHERE project_id = ? AND invoice_id = ?"
                } else {
                    "UPDATE vendor_invoices SET status = ?, updated_at = ?, updated_by = ?, version = version + 1 WHERE project_id = ? AND invoice_id = ?"
                }
                val params = if (matchStatus != null) {
                    listOf(status.name, matchStatus.name, now, updatedBy, tenant.projectId, invoiceId)
                } else {
                    listOf(status.name, now, updatedBy, tenant.projectId, invoiceId)
                }
                val rows = ctx.sqlExecutor.executeUpdate(sql, params)
                if (rows == 0) {
                    throw NoSuchElementException("Vendor invoice '$invoiceId' not found")
                }
                val rawSql = "SELECT * FROM vendor_invoices WHERE project_id = ? AND invoice_id = ?"
                val raw = ctx.sqlExecutor.querySingleOrNull(rawSql, listOf(tenant.projectId, invoiceId)) { rs -> mapInvoiceRow(rs) }!!
                val itemSql = "SELECT * FROM vendor_invoice_items WHERE project_id = ? AND invoice_id = ? ORDER BY sequence ASC"
                val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, invoiceId)) { rs -> mapItemRow(rs) }
                raw.copy(items = items)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor invoice status")
        }
    }

    override suspend fun saveMatch(match: VendorInvoiceMatch): DomainResult<VendorInvoiceMatch> {
        val tenant = TenantContext(match.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                ctx.sqlExecutor.executeUpdate("DELETE FROM vendor_invoice_matches WHERE project_id = ? AND invoice_id = ?", listOf(tenant.projectId, match.invoiceId))

                val sql = """
                    INSERT INTO vendor_invoice_matches (
                        project_id, match_id, invoice_id, purchase_order_id, match_status,
                        matched_at, matched_by, subtotal_variance, quantity_variance,
                        price_variance, tax_variance, total_variance, currency_mismatch,
                        vendor_mismatch, unmatched_line_count, exception_count, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, match.matchId, match.invoiceId, match.purchaseOrderId, match.matchStatus.name,
                        Timestamp(match.matchedAt), match.matchedBy, match.subtotalVariance.amount, match.quantityVariance,
                        match.priceVariance.amount, match.taxVariance.amount, match.totalVariance.amount, match.currencyMismatch,
                        match.vendorMismatch, match.unmatchedLineCount, match.exceptionCount
                    )
                )

                val lineSql = """
                    INSERT INTO vendor_invoice_match_lines (
                        project_id, match_line_id, match_id, invoice_item_id, purchase_order_item_id,
                        delivery_receipt_item_id, description, ordered_quantity, received_quantity,
                        invoiced_quantity, ordered_unit_price, invoiced_unit_price, quantity_variance,
                        price_variance, amount_variance, match_status, exception_reason
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                match.lines.forEach { line ->
                    ctx.sqlExecutor.executeUpdate(
                        lineSql,
                        listOf(
                            tenant.projectId, line.matchLineId, match.matchId, line.invoiceItemId, line.purchaseOrderItemId,
                            line.deliveryReceiptItemId, line.description, line.orderedQuantity, line.receivedQuantity,
                            line.invoicedQuantity, line.orderedUnitPrice.amount, line.invoicedUnitPrice.amount, line.quantityVariance,
                            line.priceVariance.amount, line.amountVariance.amount, line.matchStatus.name, line.exceptionReason
                        )
                    )
                }
                match
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "save vendor invoice match")
        }
    }

    override suspend fun findMatchByInvoiceId(projectId: String, invoiceId: String): DomainResult<VendorInvoiceMatch> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val match = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_invoice_matches WHERE project_id = ? AND invoice_id = ?"
                val raw = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, invoiceId)) { rs -> mapMatchRow(rs) }
                if (raw != null) {
                    val lineSql = "SELECT * FROM vendor_invoice_match_lines WHERE project_id = ? AND match_id = ?"
                    val lines = ctx.sqlExecutor.queryList(lineSql, listOf(tenant.projectId, raw.matchId)) { rs -> mapMatchLineRow(rs) }
                    raw.copy(lines = lines)
                } else null
            }
            if (match != null) DomainResult.Success(match) else DomainResult.Error(NoSuchElementException("No match record found for invoice '$invoiceId'"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor invoice match")
        }
    }

    override suspend fun saveException(exception: VendorInvoiceException): DomainResult<VendorInvoiceException> {
        val tenant = TenantContext(exception.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_invoice_exceptions (
                        project_id, exception_id, invoice_id, match_id, exception_type,
                        description, resolved, resolved_by, resolved_at, resolution_notes, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, exception.exceptionId, exception.invoiceId, exception.matchId,
                        exception.exceptionType.name, exception.description, exception.resolved,
                        exception.resolvedBy, exception.resolvedAt?.let { Timestamp(it) }, exception.resolutionNotes,
                        Timestamp(exception.createdAt)
                    )
                )
                exception
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "save vendor invoice exception")
        }
    }

    override suspend fun listExceptions(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceException>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_invoice_exceptions WHERE project_id = ? AND invoice_id = ? ORDER BY created_at ASC"
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, invoiceId)) { rs -> mapExceptionRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor invoice exceptions")
        }
    }

    override suspend fun resolveException(
        projectId: String,
        exceptionId: String,
        resolvedBy: String,
        resolutionNotes: String
    ): DomainResult<VendorInvoiceException> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE vendor_invoice_exceptions SET
                        resolved = TRUE, resolved_by = ?, resolved_at = ?, resolution_notes = ?
                    WHERE project_id = ? AND exception_id = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(sql, listOf(resolvedBy, Timestamp(now), resolutionNotes, tenant.projectId, exceptionId))
                if (rows == 0) {
                    throw NoSuchElementException("Exception '$exceptionId' not found")
                }
                val rawSql = "SELECT * FROM vendor_invoice_exceptions WHERE project_id = ? AND exception_id = ?"
                ctx.sqlExecutor.querySingleOrNull(rawSql, listOf(tenant.projectId, exceptionId)) { rs -> mapExceptionRow(rs) }!!
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "resolve vendor invoice exception")
        }
    }

    override suspend fun appendAudit(auditEvent: VendorInvoiceAuditEvent): DomainResult<VendorInvoiceAuditEvent> {
        val tenant = TenantContext(auditEvent.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_invoice_audits (
                        project_id, audit_id, invoice_id, event_type, actor_id,
                        correlation_id, occurred_at, details
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, auditEvent.auditId, auditEvent.invoiceId, auditEvent.eventType,
                        auditEvent.actorId, auditEvent.correlationId, Timestamp(auditEvent.occurredAt), auditEvent.details
                    )
                )
                auditEvent
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "append vendor invoice audit")
        }
    }

    override suspend fun listAudits(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceAuditEvent>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_invoice_audits WHERE project_id = ? AND invoice_id = ? ORDER BY occurred_at ASC"
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, invoiceId)) { rs -> mapAuditRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor invoice audits")
        }
    }
}
