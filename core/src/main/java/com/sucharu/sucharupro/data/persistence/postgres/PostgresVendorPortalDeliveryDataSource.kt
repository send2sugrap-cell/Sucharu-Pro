package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPortalDeliveryDataSource
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * PostgreSQL JDBC implementation of [VendorPortalDeliveryDataSource] with RLS (Module 13 Step 05).
 */
class PostgresVendorPortalDeliveryDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPortalDeliveryDataSource {

    private fun mapNoticeRow(rs: ResultSet, items: List<VendorPortalDeliveryNoticeItem> = emptyList()): VendorPortalDeliveryNotice {
        val subAt = rs.getLong("submitted_at")
        val canAt = rs.getLong("cancelled_at")
        return VendorPortalDeliveryNotice(
            noticeId = rs.getString("notice_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            orderNumber = rs.getString("order_number"),
            noticeNumber = rs.getString("notice_number"),
            status = VendorPortalDeliveryNoticeStatus.valueOf(rs.getString("status")),
            plannedDeliveryDate = rs.getLong("planned_delivery_date"),
            carrierName = rs.getString("carrier_name"),
            trackingNumber = rs.getString("tracking_number"),
            vehicleNumber = rs.getString("vehicle_number"),
            driverName = rs.getString("driver_name"),
            driverPhone = rs.getString("driver_phone"),
            vendorNotes = rs.getString("vendor_notes"),
            items = items,
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            submittedAt = if (rs.wasNull() || subAt == 0L) null else subAt,
            submittedBy = rs.getString("submitted_by"),
            cancelledAt = if (rs.wasNull() || canAt == 0L) null else canAt,
            cancelledBy = rs.getString("cancelled_by"),
            cancellationReason = rs.getString("cancellation_reason"),
            version = rs.getLong("version")
        )
    }

    private fun mapNoticeItemRow(rs: ResultSet): VendorPortalDeliveryNoticeItem {
        return VendorPortalDeliveryNoticeItem(
            itemId = rs.getString("item_id"),
            noticeId = rs.getString("notice_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            purchaseOrderItemId = rs.getString("purchase_order_item_id"),
            itemName = rs.getString("item_name"),
            itemCode = rs.getString("item_code"),
            orderedQuantity = rs.getBigDecimal("ordered_quantity") ?: BigDecimal.ZERO,
            previouslyDeliveredQuantity = rs.getBigDecimal("previously_delivered_quantity") ?: BigDecimal.ZERO,
            deliveryQuantity = rs.getBigDecimal("delivery_quantity") ?: BigDecimal.ZERO,
            unitOfMeasure = rs.getString("unit_of_measure") ?: "PIECE",
            lotNumber = rs.getString("lot_number"),
            packageCount = rs.getInt("package_count").let { if (rs.wasNull()) null else it },
            remarks = rs.getString("remarks")
        )
    }

    private fun mapAckRow(rs: ResultSet): VendorPortalDeliveryAcknowledgement {
        return VendorPortalDeliveryAcknowledgement(
            acknowledgementId = rs.getString("acknowledgement_id"),
            noticeId = rs.getString("notice_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            deliveryReceiptId = rs.getString("delivery_receipt_id"),
            acknowledgedBy = rs.getString("acknowledged_by"),
            acknowledgedAt = rs.getLong("acknowledged_at"),
            receivingGate = rs.getString("receiving_gate"),
            notes = rs.getString("notes")
        )
    }

    private fun mapQualityResponseRow(rs: ResultSet): VendorPortalQualityResponse {
        val repDate = rs.getLong("promised_replacement_date")
        val evRefs = rs.getString("evidence_references")?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
        return VendorPortalQualityResponse(
            responseId = rs.getString("response_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            inspectionId = rs.getString("inspection_id"),
            rejectionId = rs.getString("rejection_id"),
            responseType = VendorPortalQualityResponseType.valueOf(rs.getString("response_type")),
            comment = rs.getString("comment") ?: "",
            correctiveActionPlan = rs.getString("corrective_action_plan"),
            promisedReplacementDate = if (rs.wasNull() || repDate == 0L) null else repDate,
            evidenceReferences = evRefs,
            respondedBy = rs.getString("responded_by"),
            respondedAt = rs.getLong("responded_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapExceptionRow(rs: ResultSet): VendorPortalDeliveryException {
        val dueAt = rs.getLong("due_at")
        val resAt = rs.getLong("resolved_at")
        return VendorPortalDeliveryException(
            exceptionId = rs.getString("exception_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            sourceType = rs.getString("source_type"),
            sourceId = rs.getString("source_id"),
            exceptionType = VendorPortalDeliveryExceptionType.valueOf(rs.getString("exception_type")),
            severity = VendorPortalDeliveryExceptionSeverity.valueOf(rs.getString("severity")),
            status = VendorPortalDeliveryExceptionStatus.valueOf(rs.getString("status")),
            title = rs.getString("title"),
            description = rs.getString("description"),
            requiredVendorAction = rs.getString("required_vendor_action"),
            dueAt = if (rs.wasNull() || dueAt == 0L) null else dueAt,
            resolvedAt = if (rs.wasNull() || resAt == 0L) null else resAt,
            resolvedBy = rs.getString("resolved_by"),
            resolutionNotes = rs.getString("resolution_notes"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapEvidenceRow(rs: ResultSet): VendorPortalDeliveryEvidence {
        return VendorPortalDeliveryEvidence(
            evidenceId = rs.getString("evidence_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            filename = rs.getString("filename"),
            fileReference = rs.getString("file_reference"),
            mimeType = rs.getString("mime_type"),
            sizeBytes = rs.getLong("size_bytes"),
            description = rs.getString("description"),
            uploadedBy = rs.getString("uploaded_by"),
            uploadedAt = rs.getLong("uploaded_at")
        )
    }

    private fun mapAuditRow(rs: ResultSet): VendorPortalDeliveryAuditEvent {
        return VendorPortalDeliveryAuditEvent(
            eventId = rs.getString("event_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            eventType = VendorPortalDeliveryAuditEventType.valueOf(rs.getString("event_type")),
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            actorId = rs.getString("actor_id"),
            description = rs.getString("description"),
            previousState = rs.getString("previous_state"),
            newState = rs.getString("new_state"),
            correlationId = rs.getString("correlation_id"),
            createdAt = rs.getLong("created_at")
        )
    }

    override suspend fun saveDeliveryNotice(notice: VendorPortalDeliveryNotice): VendorPortalDeliveryNotice {
        return transactionManager.inTransaction(TenantContext(notice.tenantId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_delivery_notices (
                    notice_id, tenant_id, project_id, vendor_id, purchase_order_id, order_number, notice_number,
                    status, planned_delivery_date, carrier_name, tracking_number, vehicle_number, driver_name,
                    driver_phone, vendor_notes, created_at, created_by, updated_at, updated_by, submitted_at,
                    submitted_by, cancelled_at, cancelled_by, cancellation_reason, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    notice.noticeId, notice.tenantId, notice.projectId, notice.vendorId, notice.purchaseOrderId,
                    notice.orderNumber, notice.noticeNumber, notice.status.name, notice.plannedDeliveryDate,
                    notice.carrierName, notice.trackingNumber, notice.vehicleNumber, notice.driverName,
                    notice.driverPhone, notice.vendorNotes, notice.createdAt, notice.createdBy, notice.updatedAt,
                    notice.updatedBy, notice.submittedAt, notice.submittedBy, notice.cancelledAt, notice.cancelledBy,
                    notice.cancellationReason, notice.version
                )
            )

            val itemSql = """
                INSERT INTO vendor_portal_delivery_notice_items (
                    item_id, notice_id, tenant_id, purchase_order_item_id, item_name, item_code,
                    ordered_quantity, previously_delivered_quantity, delivery_quantity, unit_of_measure,
                    lot_number, package_count, remarks
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            for (item in notice.items) {
                ctx.sqlExecutor.executeUpdate(
                    itemSql,
                    listOf(
                        item.itemId, item.noticeId, item.tenantId, item.purchaseOrderItemId, item.itemName,
                        item.itemCode, item.orderedQuantity, item.previouslyDeliveredQuantity, item.deliveryQuantity,
                        item.unitOfMeasure, item.lotNumber, item.packageCount, item.remarks
                    )
                )
            }
            notice
        }
    }

    override suspend fun updateDeliveryNotice(notice: VendorPortalDeliveryNotice): VendorPortalDeliveryNotice {
        return transactionManager.inTransaction(TenantContext(notice.tenantId)) { ctx ->
            val sql = """
                UPDATE vendor_portal_delivery_notices SET
                    status = ?, planned_delivery_date = ?, carrier_name = ?, tracking_number = ?,
                    vehicle_number = ?, driver_name = ?, driver_phone = ?, vendor_notes = ?,
                    updated_at = ?, updated_by = ?, submitted_at = ?, submitted_by = ?,
                    cancelled_at = ?, cancelled_by = ?, cancellation_reason = ?, version = version + 1
                WHERE notice_id = ? AND tenant_id = ?
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    notice.status.name, notice.plannedDeliveryDate, notice.carrierName, notice.trackingNumber,
                    notice.vehicleNumber, notice.driverName, notice.driverPhone, notice.vendorNotes,
                    System.currentTimeMillis(), notice.updatedBy, notice.submittedAt, notice.submittedBy,
                    notice.cancelledAt, notice.cancelledBy, notice.cancellationReason,
                    notice.noticeId, notice.tenantId
                )
            )
            notice.copy(version = notice.version + 1, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun findDeliveryNoticeById(noticeId: String, tenantId: String): VendorPortalDeliveryNotice? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val noticeSql = "SELECT * FROM vendor_portal_delivery_notices WHERE notice_id = ? AND tenant_id = ?"
            val notice = ctx.sqlExecutor.querySingleOrNull(noticeSql, listOf(noticeId, tenantId)) { rs ->
                mapNoticeRow(rs)
            } ?: return@inReadOnly null

            val itemSql = "SELECT * FROM vendor_portal_delivery_notice_items WHERE notice_id = ? AND tenant_id = ?"
            val items = ctx.sqlExecutor.queryList(itemSql, listOf(noticeId, tenantId)) { rs ->
                mapNoticeItemRow(rs)
            }
            notice.copy(items = items)
        }
    }

    override suspend fun findDeliveryNoticeByNumber(noticeNumber: String, tenantId: String): VendorPortalDeliveryNotice? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val noticeSql = "SELECT * FROM vendor_portal_delivery_notices WHERE notice_number = ? AND tenant_id = ?"
            val notice = ctx.sqlExecutor.querySingleOrNull(noticeSql, listOf(noticeNumber, tenantId)) { rs ->
                mapNoticeRow(rs)
            } ?: return@inReadOnly null

            val itemSql = "SELECT * FROM vendor_portal_delivery_notice_items WHERE notice_id = ? AND tenant_id = ?"
            val items = ctx.sqlExecutor.queryList(itemSql, listOf(notice.noticeId, tenantId)) { rs ->
                mapNoticeItemRow(rs)
            }
            notice.copy(items = items)
        }
    }

    override suspend fun listDeliveryNotices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String?,
        status: VendorPortalDeliveryNoticeStatus?
    ): List<VendorPortalDeliveryNotice> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_delivery_notices WHERE tenant_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any?>(tenantId, vendorId)
            if (purchaseOrderId != null) {
                sql.append(" AND purchase_order_id = ?")
                params.add(purchaseOrderId)
            }
            if (status != null) {
                sql.append(" AND status = ?")
                params.add(status.name)
            }
            sql.append(" ORDER BY created_at DESC")

            ctx.sqlExecutor.queryList(sql.toString(), params) { rs ->
                mapNoticeRow(rs)
            }
        }
    }

    override suspend fun saveDeliveryAcknowledgement(ack: VendorPortalDeliveryAcknowledgement): VendorPortalDeliveryAcknowledgement {
        return transactionManager.inTransaction(TenantContext(ack.tenantId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_delivery_acknowledgements (
                    acknowledgement_id, notice_id, tenant_id, project_id, vendor_id,
                    delivery_receipt_id, acknowledged_by, acknowledged_at, receiving_gate, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    ack.acknowledgementId, ack.noticeId, ack.tenantId, ack.projectId, ack.vendorId,
                    ack.deliveryReceiptId, ack.acknowledgedBy, ack.acknowledgedAt, ack.receivingGate, ack.notes
                )
            )
            ack
        }
    }

    override suspend fun findDeliveryAcknowledgement(noticeId: String, tenantId: String): VendorPortalDeliveryAcknowledgement? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_delivery_acknowledgements WHERE notice_id = ? AND tenant_id = ?"
            ctx.sqlExecutor.querySingleOrNull(sql, listOf(noticeId, tenantId)) { rs ->
                mapAckRow(rs)
            }
        }
    }

    override suspend fun saveQualityResponse(response: VendorPortalQualityResponse): VendorPortalQualityResponse {
        return transactionManager.inTransaction(TenantContext(response.tenantId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_quality_responses (
                    response_id, tenant_id, project_id, vendor_id, inspection_id, rejection_id,
                    response_type, comment, corrective_action_plan, promised_replacement_date,
                    evidence_references, responded_by, responded_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    response.responseId, response.tenantId, response.projectId, response.vendorId,
                    response.inspectionId, response.rejectionId, response.responseType.name,
                    response.comment, response.correctiveActionPlan, response.promisedReplacementDate,
                    response.evidenceReferences.joinToString(";"), response.respondedBy, response.respondedAt, response.version
                )
            )
            response
        }
    }

    override suspend fun findQualityResponseById(responseId: String, tenantId: String): VendorPortalQualityResponse? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_quality_responses WHERE response_id = ? AND tenant_id = ?"
            ctx.sqlExecutor.querySingleOrNull(sql, listOf(responseId, tenantId)) { rs ->
                mapQualityResponseRow(rs)
            }
        }
    }

    override suspend fun listQualityResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String?,
        rejectionId: String?
    ): List<VendorPortalQualityResponse> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_quality_responses WHERE tenant_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any?>(tenantId, vendorId)
            if (inspectionId != null) {
                sql.append(" AND inspection_id = ?")
                params.add(inspectionId)
            }
            if (rejectionId != null) {
                sql.append(" AND rejection_id = ?")
                params.add(rejectionId)
            }
            sql.append(" ORDER BY responded_at DESC")

            ctx.sqlExecutor.queryList(sql.toString(), params) { rs ->
                mapQualityResponseRow(rs)
            }
        }
    }

    override suspend fun saveException(exception: VendorPortalDeliveryException): VendorPortalDeliveryException {
        return transactionManager.inTransaction(TenantContext(exception.tenantId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_delivery_exceptions (
                    exception_id, tenant_id, project_id, vendor_id, source_type, source_id,
                    exception_type, severity, status, title, description, required_vendor_action,
                    due_at, resolved_at, resolved_by, resolution_notes, created_at, created_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    exception.exceptionId, exception.tenantId, exception.projectId, exception.vendorId,
                    exception.sourceType, exception.sourceId, exception.exceptionType.name,
                    exception.severity.name, exception.status.name, exception.title, exception.description,
                    exception.requiredVendorAction, exception.dueAt, exception.resolvedAt,
                    exception.resolvedBy, exception.resolutionNotes, exception.createdAt, exception.createdBy,
                    exception.version
                )
            )
            exception
        }
    }

    override suspend fun updateException(exception: VendorPortalDeliveryException): VendorPortalDeliveryException {
        return transactionManager.inTransaction(TenantContext(exception.tenantId)) { ctx ->
            val sql = """
                UPDATE vendor_portal_delivery_exceptions SET
                    status = ?, resolved_at = ?, resolved_by = ?, resolution_notes = ?, version = version + 1
                WHERE exception_id = ? AND tenant_id = ?
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    exception.status.name, exception.resolvedAt, exception.resolvedBy,
                    exception.resolutionNotes, exception.exceptionId, exception.tenantId
                )
            )
            exception.copy(version = exception.version + 1)
        }
    }

    override suspend fun findExceptionById(exceptionId: String, tenantId: String): VendorPortalDeliveryException? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_delivery_exceptions WHERE exception_id = ? AND tenant_id = ?"
            ctx.sqlExecutor.querySingleOrNull(sql, listOf(exceptionId, tenantId)) { rs ->
                mapExceptionRow(rs)
            }
        }
    }

    override suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDeliveryExceptionStatus?,
        sourceType: String?
    ): List<VendorPortalDeliveryException> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_delivery_exceptions WHERE tenant_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any?>(tenantId, vendorId)
            if (status != null) {
                sql.append(" AND status = ?")
                params.add(status.name)
            }
            if (sourceType != null) {
                sql.append(" AND source_type = ?")
                params.add(sourceType)
            }
            sql.append(" ORDER BY created_at DESC")

            ctx.sqlExecutor.queryList(sql.toString(), params) { rs ->
                mapExceptionRow(rs)
            }
        }
    }

    override suspend fun saveEvidence(evidence: VendorPortalDeliveryEvidence): VendorPortalDeliveryEvidence {
        return transactionManager.inTransaction(TenantContext(evidence.tenantId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_delivery_evidence (
                    evidence_id, tenant_id, project_id, vendor_id, entity_type, entity_id,
                    filename, file_reference, mime_type, size_bytes, description, uploaded_by, uploaded_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    evidence.evidenceId, evidence.tenantId, evidence.projectId, evidence.vendorId,
                    evidence.entityType, evidence.entityId, evidence.filename, evidence.fileReference,
                    evidence.mimeType, evidence.sizeBytes, evidence.description, evidence.uploadedBy, evidence.uploadedAt
                )
            )
            evidence
        }
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): List<VendorPortalDeliveryEvidence> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_delivery_evidence
                WHERE tenant_id = ? AND vendor_id = ? AND entity_type = ? AND entity_id = ?
                ORDER BY uploaded_at DESC
            """.trimIndent()
            ctx.sqlExecutor.queryList(sql, listOf(tenantId, vendorId, entityType, entityId)) { rs ->
                mapEvidenceRow(rs)
            }
        }
    }

    override suspend fun recordAuditEvent(event: VendorPortalDeliveryAuditEvent): VendorPortalDeliveryAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_delivery_audit_events (
                    event_id, tenant_id, project_id, vendor_id, event_type, entity_type,
                    entity_id, actor_id, description, previous_state, new_state, correlation_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    event.eventId, event.tenantId, event.projectId, event.vendorId, event.eventType.name,
                    event.entityType, event.entityId, event.actorId, event.description, event.previousState,
                    event.newState, event.correlationId, event.createdAt
                )
            )
            event
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        entityType: String,
        entityId: String
    ): List<VendorPortalDeliveryAuditEvent> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_delivery_audit_events
                WHERE tenant_id = ? AND entity_type = ? AND entity_id = ?
                ORDER BY created_at ASC
            """.trimIndent()
            ctx.sqlExecutor.queryList(sql, listOf(tenantId, entityType, entityId)) { rs ->
                mapAuditRow(rs)
            }
        }
    }
}
