package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPortalInvoiceDataSource
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

/**
 * Production Postgres implementation for Vendor Portal Invoice & Financial data (Module 13 Step 06).
 */
class PostgresVendorPortalInvoiceDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String
) : VendorPortalInvoiceDataSource {

    override suspend fun saveSubmission(submission: VendorPortalInvoiceSubmission): VendorPortalInvoiceSubmission {
        val tenantContext = TenantContext(submission.projectId)
        return transactionManager.inTransaction(tenantContext) { tx ->
            val sql = """
                INSERT INTO vendor_portal_invoice_submissions (
                    submission_id, tenant_id, project_id, vendor_id, purchase_order_id,
                    order_number, vendor_invoice_number, invoice_date, currency,
                    subtotal_amount, tax_amount, discount_amount, shipping_amount,
                    other_charges, total_amount, notes, status, canonical_invoice_id,
                    rejection_reason, created_at, created_by, updated_at, updated_by,
                    submitted_at, submitted_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (submission_id) DO UPDATE SET
                    order_number = EXCLUDED.order_number,
                    vendor_invoice_number = EXCLUDED.vendor_invoice_number,
                    invoice_date = EXCLUDED.invoice_date,
                    currency = EXCLUDED.currency,
                    subtotal_amount = EXCLUDED.subtotal_amount,
                    tax_amount = EXCLUDED.tax_amount,
                    discount_amount = EXCLUDED.discount_amount,
                    shipping_amount = EXCLUDED.shipping_amount,
                    other_charges = EXCLUDED.other_charges,
                    total_amount = EXCLUDED.total_amount,
                    notes = EXCLUDED.notes,
                    status = EXCLUDED.status,
                    canonical_invoice_id = EXCLUDED.canonical_invoice_id,
                    rejection_reason = EXCLUDED.rejection_reason,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    submitted_at = EXCLUDED.submitted_at,
                    submitted_by = EXCLUDED.submitted_by,
                    version = vendor_portal_invoice_submissions.version + 1
            """.trimIndent()

            tx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    submission.submissionId,
                    submission.tenantId,
                    submission.projectId,
                    submission.vendorId,
                    submission.purchaseOrderId,
                    submission.orderNumber,
                    submission.vendorInvoiceNumber,
                    submission.invoiceDate,
                    submission.currency,
                    submission.subtotalAmount.amount,
                    submission.taxAmount.amount,
                    submission.discountAmount.amount,
                    submission.shippingAmount.amount,
                    submission.otherCharges.amount,
                    submission.totalAmount.amount,
                    submission.notes,
                    submission.status.name,
                    submission.canonicalInvoiceId,
                    submission.rejectionReason,
                    submission.createdAt,
                    submission.createdBy,
                    submission.updatedAt,
                    submission.updatedBy,
                    submission.submittedAt,
                    submission.submittedBy,
                    submission.version
                )
            )

            // Replace items
            tx.sqlExecutor.executeUpdate(
                "DELETE FROM vendor_portal_invoice_submission_items WHERE submission_id = ? AND tenant_id = ?",
                listOf(submission.submissionId, submission.tenantId)
            )

            for (item in submission.items) {
                val itemSql = """
                    INSERT INTO vendor_portal_invoice_submission_items (
                        item_id, submission_id, tenant_id, purchase_order_item_id,
                        delivery_receipt_item_id, item_name, item_code,
                        invoiced_quantity, unit_of_measure, unit_price,
                        tax_amount, line_total, remarks
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                tx.sqlExecutor.executeUpdate(
                    itemSql,
                    listOf(
                        item.itemId,
                        item.submissionId,
                        item.tenantId,
                        item.purchaseOrderItemId,
                        item.deliveryReceiptItemId,
                        item.itemName,
                        item.itemCode,
                        item.invoicedQuantity,
                        item.unitOfMeasure,
                        item.unitPrice.amount,
                        item.taxAmount.amount,
                        item.lineTotal.amount,
                        item.remarks
                    )
                )
            }
            submission
        }
    }

    override suspend fun findSubmissionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String
    ): VendorPortalInvoiceSubmission? {
        val tenantContext = TenantContext(projectId)
        return transactionManager.inReadOnly(tenantContext) { tx ->
            val sql = "SELECT * FROM vendor_portal_invoice_submissions WHERE submission_id = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?"
            tx.sqlExecutor.querySingleOrNull(sql, listOf(submissionId, tenantId, projectId, vendorId)) { rs ->
                val currency = rs.getString("currency") ?: "BDT"
                val items = loadSubmissionItems(tx, submissionId, tenantId, currency)
                VendorPortalInvoiceSubmission(
                    submissionId = rs.getString("submission_id"),
                    tenantId = rs.getString("tenant_id"),
                    projectId = rs.getString("project_id"),
                    vendorId = rs.getString("vendor_id"),
                    purchaseOrderId = rs.getString("purchase_order_id"),
                    orderNumber = rs.getString("order_number"),
                    vendorInvoiceNumber = rs.getString("vendor_invoice_number"),
                    invoiceDate = rs.getLong("invoice_date"),
                    currency = currency,
                    subtotalAmount = Money(rs.getBigDecimal("subtotal_amount") ?: BigDecimal.ZERO),
                    taxAmount = Money(rs.getBigDecimal("tax_amount") ?: BigDecimal.ZERO),
                    discountAmount = Money(rs.getBigDecimal("discount_amount") ?: BigDecimal.ZERO),
                    shippingAmount = Money(rs.getBigDecimal("shipping_amount") ?: BigDecimal.ZERO),
                    otherCharges = Money(rs.getBigDecimal("other_charges") ?: BigDecimal.ZERO),
                    totalAmount = Money(rs.getBigDecimal("total_amount") ?: BigDecimal.ZERO),
                    notes = rs.getString("notes"),
                    status = VendorPortalInvoiceSubmissionStatus.valueOf(rs.getString("status")),
                    canonicalInvoiceId = rs.getString("canonical_invoice_id"),
                    rejectionReason = rs.getString("rejection_reason"),
                    items = items,
                    createdAt = rs.getLong("created_at"),
                    createdBy = rs.getString("created_by"),
                    updatedAt = rs.getLong("updated_at"),
                    updatedBy = rs.getString("updated_by"),
                    submittedAt = rs.getLong("submitted_at").takeIf { !rs.wasNull() },
                    submittedBy = rs.getString("submitted_by"),
                    version = rs.getLong("version")
                )
            }
        }
    }

    override suspend fun listSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String?,
        status: VendorPortalInvoiceSubmissionStatus?
    ): List<VendorPortalInvoiceSubmission> {
        val tenantContext = TenantContext(projectId)
        return transactionManager.inReadOnly(tenantContext) { tx ->
            val sb = StringBuilder("SELECT * FROM vendor_portal_invoice_submissions WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (purchaseOrderId != null) {
                sb.append(" AND purchase_order_id = ?")
                params.add(purchaseOrderId)
            }
            if (status != null) {
                sb.append(" AND status = ?")
                params.add(status.name)
            }
            sb.append(" ORDER BY created_at DESC")

            tx.sqlExecutor.queryList(sb.toString(), params) { rs ->
                val subId = rs.getString("submission_id")
                val currency = rs.getString("currency") ?: "BDT"
                val items = loadSubmissionItems(tx, subId, tenantId, currency)
                VendorPortalInvoiceSubmission(
                    submissionId = subId,
                    tenantId = rs.getString("tenant_id"),
                    projectId = rs.getString("project_id"),
                    vendorId = rs.getString("vendor_id"),
                    purchaseOrderId = rs.getString("purchase_order_id"),
                    orderNumber = rs.getString("order_number"),
                    vendorInvoiceNumber = rs.getString("vendor_invoice_number"),
                    invoiceDate = rs.getLong("invoice_date"),
                    currency = currency,
                    subtotalAmount = Money(rs.getBigDecimal("subtotal_amount") ?: BigDecimal.ZERO),
                    taxAmount = Money(rs.getBigDecimal("tax_amount") ?: BigDecimal.ZERO),
                    discountAmount = Money(rs.getBigDecimal("discount_amount") ?: BigDecimal.ZERO),
                    shippingAmount = Money(rs.getBigDecimal("shipping_amount") ?: BigDecimal.ZERO),
                    otherCharges = Money(rs.getBigDecimal("other_charges") ?: BigDecimal.ZERO),
                    totalAmount = Money(rs.getBigDecimal("total_amount") ?: BigDecimal.ZERO),
                    notes = rs.getString("notes"),
                    status = VendorPortalInvoiceSubmissionStatus.valueOf(rs.getString("status")),
                    canonicalInvoiceId = rs.getString("canonical_invoice_id"),
                    rejectionReason = rs.getString("rejection_reason"),
                    items = items,
                    createdAt = rs.getLong("created_at"),
                    createdBy = rs.getString("created_by"),
                    updatedAt = rs.getLong("updated_at"),
                    updatedBy = rs.getString("updated_by"),
                    submittedAt = rs.getLong("submitted_at").takeIf { !rs.wasNull() },
                    submittedBy = rs.getString("submitted_by"),
                    version = rs.getLong("version")
                )
            }
        }
    }

    private fun loadSubmissionItems(
        tx: TransactionContext,
        submissionId: String,
        tenantId: String,
        currency: String
    ): List<VendorPortalInvoiceSubmissionItem> {
        val sql = "SELECT * FROM vendor_portal_invoice_submission_items WHERE submission_id = ? AND tenant_id = ?"
        val items = mutableListOf<VendorPortalInvoiceSubmissionItem>()
        val conn = tx.connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, submissionId)
            stmt.setString(2, tenantId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    items.add(
                        VendorPortalInvoiceSubmissionItem(
                            itemId = rs.getString("item_id"),
                            submissionId = rs.getString("submission_id"),
                            tenantId = rs.getString("tenant_id"),
                            purchaseOrderItemId = rs.getString("purchase_order_item_id"),
                            deliveryReceiptItemId = rs.getString("delivery_receipt_item_id"),
                            itemName = rs.getString("item_name"),
                            itemCode = rs.getString("item_code"),
                            invoicedQuantity = rs.getBigDecimal("invoiced_quantity"),
                            unitOfMeasure = rs.getString("unit_of_measure") ?: "PIECE",
                            unitPrice = Money(rs.getBigDecimal("unit_price") ?: BigDecimal.ZERO),
                            taxAmount = Money(rs.getBigDecimal("tax_amount") ?: BigDecimal.ZERO),
                            lineTotal = Money(rs.getBigDecimal("line_total") ?: BigDecimal.ZERO),
                            remarks = rs.getString("remarks")
                        )
                    )
                }
            }
        }
        return items
    }

    override suspend fun saveResponse(response: VendorPortalInvoiceResponse): VendorPortalInvoiceResponse {
        val tenantContext = TenantContext(response.projectId)
        return transactionManager.inTransaction(tenantContext) { tx ->
            val sql = """
                INSERT INTO vendor_portal_invoice_responses (
                    response_id, tenant_id, project_id, vendor_id, invoice_id,
                    exception_id, response_type, comment, proposed_correction,
                    evidence_references, responded_by, responded_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (response_id) DO UPDATE SET
                    comment = EXCLUDED.comment,
                    proposed_correction = EXCLUDED.proposed_correction,
                    evidence_references = EXCLUDED.evidence_references,
                    version = vendor_portal_invoice_responses.version + 1
            """.trimIndent()

            tx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    response.responseId,
                    response.tenantId,
                    response.projectId,
                    response.vendorId,
                    response.invoiceId,
                    response.exceptionId,
                    response.responseType.name,
                    response.comment,
                    response.proposedCorrection,
                    response.evidenceReferences.joinToString(","),
                    response.respondedBy,
                    response.respondedAt,
                    response.version
                )
            )
            response
        }
    }

    override suspend fun listResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): List<VendorPortalInvoiceResponse> {
        val tenantContext = TenantContext(projectId)
        return transactionManager.inReadOnly(tenantContext) { tx ->
            val sql = "SELECT * FROM vendor_portal_invoice_responses WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND invoice_id = ? ORDER BY responded_at DESC"
            tx.sqlExecutor.queryList(sql, listOf(tenantId, projectId, vendorId, invoiceId)) { rs ->
                val evStr = rs.getString("evidence_references") ?: ""
                val evList = if (evStr.isNotBlank()) evStr.split(",") else emptyList()
                VendorPortalInvoiceResponse(
                    responseId = rs.getString("response_id"),
                    tenantId = rs.getString("tenant_id"),
                    projectId = rs.getString("project_id"),
                    vendorId = rs.getString("vendor_id"),
                    invoiceId = rs.getString("invoice_id"),
                    exceptionId = rs.getString("exception_id"),
                    responseType = VendorPortalInvoiceResponseType.valueOf(rs.getString("response_type")),
                    comment = rs.getString("comment"),
                    proposedCorrection = rs.getString("proposed_correction"),
                    evidenceReferences = evList,
                    respondedBy = rs.getString("responded_by"),
                    respondedAt = rs.getLong("responded_at"),
                    version = rs.getLong("version")
                )
            }
        }
    }

    override suspend fun saveEvidence(evidence: VendorPortalFinancialEvidence): VendorPortalFinancialEvidence {
        val tenantContext = TenantContext(evidence.projectId)
        return transactionManager.inTransaction(tenantContext) { tx ->
            val sql = """
                INSERT INTO vendor_portal_financial_evidence (
                    evidence_id, tenant_id, project_id, vendor_id, entity_type,
                    entity_id, evidence_type, filename, file_reference, file_hash,
                    mime_type, size_bytes, uploaded_by, uploaded_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (evidence_id) DO UPDATE SET
                    filename = EXCLUDED.filename,
                    file_reference = EXCLUDED.file_reference
            """.trimIndent()

            tx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    evidence.evidenceId,
                    evidence.tenantId,
                    evidence.projectId,
                    evidence.vendorId,
                    evidence.entityType,
                    evidence.entityId,
                    evidence.evidenceType.name,
                    evidence.filename,
                    evidence.fileReference,
                    evidence.fileHash,
                    evidence.mimeType,
                    evidence.sizeBytes,
                    evidence.uploadedBy,
                    evidence.uploadedAt
                )
            )
            evidence
        }
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): List<VendorPortalFinancialEvidence> {
        val tenantContext = TenantContext(projectId)
        return transactionManager.inReadOnly(tenantContext) { tx ->
            val sb = StringBuilder("SELECT * FROM vendor_portal_financial_evidence WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (entityType != null) {
                sb.append(" AND entity_type = ?")
                params.add(entityType)
            }
            if (entityId != null) {
                sb.append(" AND entity_id = ?")
                params.add(entityId)
            }
            sb.append(" ORDER BY uploaded_at DESC")

            tx.sqlExecutor.queryList(sb.toString(), params) { rs ->
                VendorPortalFinancialEvidence(
                    evidenceId = rs.getString("evidence_id"),
                    tenantId = rs.getString("tenant_id"),
                    projectId = rs.getString("project_id"),
                    vendorId = rs.getString("vendor_id"),
                    entityType = rs.getString("entity_type"),
                    entityId = rs.getString("entity_id"),
                    evidenceType = VendorPortalFinancialEvidenceType.valueOf(rs.getString("evidence_type")),
                    filename = rs.getString("filename"),
                    fileReference = rs.getString("file_reference"),
                    fileHash = rs.getString("file_hash"),
                    mimeType = rs.getString("mime_type") ?: "application/pdf",
                    sizeBytes = rs.getLong("size_bytes"),
                    uploadedBy = rs.getString("uploaded_by"),
                    uploadedAt = rs.getLong("uploaded_at")
                )
            }
        }
    }

    override suspend fun recordAuditEvent(event: VendorPortalInvoiceAuditEvent) {
        val tenantContext = TenantContext(event.projectId)
        transactionManager.inTransaction(tenantContext) { tx ->
            val sql = """
                INSERT INTO vendor_portal_invoice_audit_events (
                    audit_id, tenant_id, project_id, vendor_id, target_type,
                    target_id, action, actor_id, actor_role, correlation_id,
                    payload, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            tx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    event.auditId,
                    event.tenantId,
                    event.projectId,
                    event.vendorId,
                    event.targetType,
                    event.targetId,
                    event.action,
                    event.actorId,
                    event.actorRole,
                    event.correlationId,
                    event.payload,
                    event.createdAt
                )
            )
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        targetType: String?,
        targetId: String?
    ): List<VendorPortalInvoiceAuditEvent> {
        val tenantContext = TenantContext(projectId)
        return transactionManager.inReadOnly(tenantContext) { tx ->
            val sb = StringBuilder("SELECT * FROM vendor_portal_invoice_audit_events WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (targetType != null) {
                sb.append(" AND target_type = ?")
                params.add(targetType)
            }
            if (targetId != null) {
                sb.append(" AND target_id = ?")
                params.add(targetId)
            }
            sb.append(" ORDER BY created_at DESC")

            tx.sqlExecutor.queryList(sb.toString(), params) { rs ->
                VendorPortalInvoiceAuditEvent(
                    auditId = rs.getString("audit_id"),
                    tenantId = rs.getString("tenant_id"),
                    projectId = rs.getString("project_id"),
                    vendorId = rs.getString("vendor_id"),
                    targetType = rs.getString("target_type"),
                    targetId = rs.getString("target_id"),
                    action = rs.getString("action"),
                    actorId = rs.getString("actor_id"),
                    actorRole = rs.getString("actor_role"),
                    correlationId = rs.getString("correlation_id"),
                    payload = rs.getString("payload"),
                    createdAt = rs.getLong("created_at")
                )
            }
        }
    }
}
