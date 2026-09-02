package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorQualityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorQualityDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorQualityDataSource {

    private val inspectionFlows = mutableMapOf<String, MutableStateFlow<List<VendorQualityInspection>>>()
    private val rejectionFlows = mutableMapOf<String, MutableStateFlow<List<VendorRejection>>>()
    private val disputeFlows = mutableMapOf<String, MutableStateFlow<List<VendorDispute>>>()

    private fun mapInspectionItemRow(rs: ResultSet): VendorQualityInspectionItem {
        return VendorQualityInspectionItem(
            inspectionItemId = rs.getString("item_id"),
            inspectionId = rs.getString("inspection_id"),
            purchaseOrderItemId = rs.getString("purchase_order_item_id"),
            deliveryReceiptItemId = rs.getString("delivery_receipt_item_id"),
            itemDescription = rs.getString("item_description"),
            receivedQuantity = rs.getBigDecimal("received_quantity"),
            acceptedQuantity = rs.getBigDecimal("accepted_quantity"),
            rejectedQuantity = rs.getBigDecimal("rejected_quantity"),
            conditionalQuantity = rs.getBigDecimal("conditional_quantity"),
            defectCount = rs.getInt("defect_count"),
            defectRate = rs.getBigDecimal("defect_rate"),
            inspectionResult = InspectionResult.valueOf(rs.getString("inspection_result")),
            notes = rs.getString("notes"),
            version = rs.getLong("version")
        )
    }

    private fun mapInspectionRow(rs: ResultSet, items: List<VendorQualityInspectionItem> = emptyList()): VendorQualityInspection {
        return VendorQualityInspection(
            inspectionId = rs.getString("inspection_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            deliveryReceiptId = rs.getString("delivery_receipt_id"),
            inspectionReference = rs.getString("inspection_reference"),
            inspectionType = VendorInspectionType.valueOf(rs.getString("inspection_type")),
            inspectionStatus = VendorInspectionStatus.valueOf(rs.getString("inspection_status")),
            inspectedBy = rs.getString("inspected_by"),
            inspectionStartedAt = rs.getTimestamp("inspection_started_at")?.time,
            inspectionCompletedAt = rs.getTimestamp("inspection_completed_at")?.time,
            receivedQuantity = rs.getBigDecimal("received_quantity"),
            acceptedQuantity = rs.getBigDecimal("accepted_quantity"),
            rejectedQuantity = rs.getBigDecimal("rejected_quantity"),
            conditionalQuantity = rs.getBigDecimal("conditional_quantity"),
            overallResult = rs.getString("overall_result")?.let { InspectionResult.valueOf(it) },
            notes = rs.getString("notes"),
            items = items,
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapDefectRow(rs: ResultSet): VendorDefect {
        return VendorDefect(
            defectId = rs.getString("defect_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            inspectionId = rs.getString("inspection_id"),
            inspectionItemId = rs.getString("inspection_item_id"),
            vendorId = rs.getString("vendor_id"),
            defectType = VendorDefectType.valueOf(rs.getString("defect_type")),
            severity = VendorDefectSeverity.valueOf(rs.getString("severity")),
            description = rs.getString("description"),
            quantityAffected = rs.getBigDecimal("quantity_affected"),
            detectedAt = rs.getTimestamp("detected_at")?.time ?: System.currentTimeMillis(),
            detectedBy = rs.getString("detected_by"),
            evidenceReference = rs.getString("evidence_reference"),
            status = rs.getString("status"),
            resolutionReference = rs.getString("resolution_reference"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            version = rs.getLong("version")
        )
    }

    private fun mapRejectionRow(rs: ResultSet): VendorRejection {
        return VendorRejection(
            rejectionId = rs.getString("rejection_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            deliveryReceiptId = rs.getString("delivery_receipt_id"),
            deliveryReceiptItemId = rs.getString("delivery_receipt_item_id"),
            inspectionId = rs.getString("inspection_id"),
            rejectionReference = rs.getString("rejection_reference"),
            rejectionType = rs.getString("rejection_type"),
            rejectionReason = rs.getString("rejection_reason"),
            rejectedQuantity = rs.getBigDecimal("rejected_quantity"),
            rejectedValue = Money(rs.getBigDecimal("rejected_value")),
            status = VendorRejectionStatus.valueOf(rs.getString("status")),
            disposition = VendorRejectionDisposition.valueOf(rs.getString("disposition")),
            replacementRequired = rs.getBoolean("replacement_required"),
            returnRequired = rs.getBoolean("return_required"),
            creditRequired = rs.getBoolean("credit_required"),
            notes = rs.getString("notes"),
            vendorResponse = rs.getString("vendor_response"),
            vendorResponseAt = rs.getTimestamp("vendor_response_at")?.time,
            resolutionNotes = rs.getString("resolution_notes"),
            resolvedAt = rs.getTimestamp("resolved_at")?.time,
            resolvedBy = rs.getString("resolved_by"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapDisputeRow(rs: ResultSet): VendorDispute {
        return VendorDispute(
            disputeId = rs.getString("dispute_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            deliveryReceiptId = rs.getString("delivery_receipt_id"),
            invoiceId = rs.getString("invoice_id"),
            inspectionId = rs.getString("inspection_id"),
            rejectionId = rs.getString("rejection_id"),
            disputeReference = rs.getString("dispute_reference"),
            disputeType = VendorDisputeType.valueOf(rs.getString("dispute_type")),
            priority = VendorDisputePriority.valueOf(rs.getString("priority")),
            status = VendorDisputeStatus.valueOf(rs.getString("status")),
            subject = rs.getString("subject"),
            description = rs.getString("description"),
            disputedQuantity = rs.getBigDecimal("disputed_quantity"),
            disputedAmount = Money(rs.getBigDecimal("disputed_amount")),
            raisedBy = rs.getString("raised_by"),
            assignedTo = rs.getString("assigned_to"),
            vendorResponseDueAt = rs.getTimestamp("vendor_response_due_at")?.time,
            vendorResponse = rs.getString("vendor_response"),
            vendorResponseAt = rs.getTimestamp("vendor_response_at")?.time,
            resolutionProposal = rs.getString("resolution_proposal"),
            resolution = rs.getString("resolution"),
            resolvedAt = rs.getTimestamp("resolved_at")?.time,
            resolvedBy = rs.getString("resolved_by"),
            closedAt = rs.getTimestamp("closed_at")?.time,
            closedBy = rs.getString("closed_by"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapDisputeEventRow(rs: ResultSet): VendorDisputeEvent {
        return VendorDisputeEvent(
            eventId = rs.getString("event_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            disputeId = rs.getString("dispute_id"),
            eventType = VendorDisputeEventType.valueOf(rs.getString("event_type")),
            actorId = rs.getString("actor_id"),
            notes = rs.getString("notes"),
            payloadJson = rs.getString("payload_json"),
            occurredAt = rs.getTimestamp("occurred_at")?.time ?: System.currentTimeMillis()
        )
    }

    private fun mapEvidenceRow(rs: ResultSet): VendorQualityEvidence {
        return VendorQualityEvidence(
            evidenceId = rs.getString("evidence_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            sourceType = rs.getString("source_type"),
            sourceId = rs.getString("source_id"),
            fileReference = rs.getString("file_reference"),
            fileName = rs.getString("file_name"),
            fileType = rs.getString("file_type"),
            description = rs.getString("description"),
            uploadedBy = rs.getString("uploaded_by"),
            uploadedAt = rs.getTimestamp("uploaded_at")?.time ?: System.currentTimeMillis(),
            checksum = rs.getString("checksum")
        )
    }

    private fun mapQualityAuditRow(rs: ResultSet): VendorQualityAuditEvent {
        return VendorQualityAuditEvent(
            auditId = rs.getString("audit_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            correlationId = rs.getString("correlation_id"),
            occurredAt = rs.getTimestamp("occurred_at")?.time ?: System.currentTimeMillis(),
            details = rs.getString("details")
        )
    }

    override fun observeInspections(projectId: String, vendorId: String?, deliveryReceiptId: String?): Flow<List<VendorQualityInspection>> {
        val key = "$projectId:$vendorId:$deliveryReceiptId"
        return synchronized(inspectionFlows) {
            inspectionFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findInspectionById(projectId: String, inspectionId: String): DomainResult<VendorQualityInspection> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val insp = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_quality_inspections WHERE project_id = ? AND inspection_id = ?"
                val raw = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, inspectionId)) { rs -> mapInspectionRow(rs) }
                if (raw != null) {
                    val itemSql = "SELECT * FROM vendor_quality_inspection_items WHERE project_id = ? AND inspection_id = ?"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, inspectionId)) { rs -> mapInspectionItemRow(rs) }
                    raw.copy(items = items)
                } else null
            }
            if (insp != null) DomainResult.Success(insp)
            else DomainResult.Error(NoSuchElementException("Quality inspection '$inspectionId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find inspection by id")
        }
    }

    override suspend fun findInspectionByReference(projectId: String, reference: String): DomainResult<VendorQualityInspection> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val insp = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_quality_inspections WHERE project_id = ? AND inspection_reference = ?"
                val raw = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, reference)) { rs -> mapInspectionRow(rs) }
                if (raw != null) {
                    val itemSql = "SELECT * FROM vendor_quality_inspection_items WHERE project_id = ? AND inspection_id = ?"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, raw.inspectionId)) { rs -> mapInspectionItemRow(rs) }
                    raw.copy(items = items)
                } else null
            }
            if (insp != null) DomainResult.Success(insp)
            else DomainResult.Error(NoSuchElementException("Quality inspection '$reference' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find inspection by reference")
        }
    }

    override suspend fun listInspections(
        projectId: String,
        vendorId: String?,
        deliveryReceiptId: String?,
        status: VendorInspectionStatus?
    ): DomainResult<List<VendorQualityInspection>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = StringBuilder("SELECT * FROM vendor_quality_inspections WHERE project_id = ?")
                val params = mutableListOf<Any>(tenant.projectId)
                if (vendorId != null) {
                    sql.append(" AND vendor_id = ?")
                    params.add(vendorId)
                }
                if (deliveryReceiptId != null) {
                    sql.append(" AND delivery_receipt_id = ?")
                    params.add(deliveryReceiptId)
                }
                if (status != null) {
                    sql.append(" AND inspection_status = ?")
                    params.add(status.name)
                }
                sql.append(" ORDER BY created_at DESC")

                val rawList = ctx.sqlExecutor.queryList(sql.toString(), params) { rs -> mapInspectionRow(rs) }
                rawList.map { raw ->
                    val itemSql = "SELECT * FROM vendor_quality_inspection_items WHERE project_id = ? AND inspection_id = ?"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, raw.inspectionId)) { rs -> mapInspectionItemRow(rs) }
                    raw.copy(items = items)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list quality inspections")
        }
    }

    override suspend fun createInspection(inspection: VendorQualityInspection): DomainResult<VendorQualityInspection> {
        val tenant = TenantContext(inspection.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    INSERT INTO vendor_quality_inspections (
                        project_id, inspection_id, tenant_id, vendor_id, purchase_order_id,
                        delivery_receipt_id, inspection_reference, inspection_type, inspection_status,
                        inspected_by, inspection_started_at, inspection_completed_at, received_quantity,
                        accepted_quantity, rejected_quantity, conditional_quantity, overall_result,
                        notes, created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, inspection.inspectionId, inspection.tenantId, inspection.vendorId,
                        inspection.purchaseOrderId, inspection.deliveryReceiptId, inspection.inspectionReference,
                        inspection.inspectionType.name, inspection.inspectionStatus.name, inspection.inspectedBy,
                        inspection.inspectionStartedAt?.let { Timestamp(it) }, inspection.inspectionCompletedAt?.let { Timestamp(it) },
                        inspection.receivedQuantity, inspection.acceptedQuantity, inspection.rejectedQuantity,
                        inspection.conditionalQuantity, inspection.overallResult?.name, inspection.notes,
                        now, inspection.createdBy, now, inspection.updatedBy
                    )
                )

                val itemSql = """
                    INSERT INTO vendor_quality_inspection_items (
                        project_id, item_id, inspection_id, purchase_order_item_id, delivery_receipt_item_id,
                        item_description, received_quantity, accepted_quantity, rejected_quantity,
                        conditional_quantity, defect_count, defect_rate, inspection_result, notes, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                inspection.items.forEach { item ->
                    ctx.sqlExecutor.executeUpdate(
                        itemSql,
                        listOf(
                            tenant.projectId, item.inspectionItemId, inspection.inspectionId, item.purchaseOrderItemId,
                            item.deliveryReceiptItemId, item.itemDescription, item.receivedQuantity, item.acceptedQuantity,
                            item.rejectedQuantity, item.conditionalQuantity, item.defectCount, item.defectRate,
                            item.inspectionResult.name, item.notes
                        )
                    )
                }
                inspection.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create quality inspection")
        }
    }

    override suspend fun updateInspection(inspection: VendorQualityInspection): DomainResult<VendorQualityInspection> {
        val tenant = TenantContext(inspection.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    UPDATE vendor_quality_inspections SET
                        inspection_status = ?, inspected_by = ?, inspection_started_at = ?,
                        inspection_completed_at = ?, received_quantity = ?, accepted_quantity = ?,
                        rejected_quantity = ?, conditional_quantity = ?, overall_result = ?,
                        notes = ?, updated_at = ?, updated_by = ?, version = version + 1
                    WHERE project_id = ? AND inspection_id = ? AND version = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        inspection.inspectionStatus.name, inspection.inspectedBy,
                        inspection.inspectionStartedAt?.let { Timestamp(it) }, inspection.inspectionCompletedAt?.let { Timestamp(it) },
                        inspection.receivedQuantity, inspection.acceptedQuantity, inspection.rejectedQuantity,
                        inspection.conditionalQuantity, inspection.overallResult?.name, inspection.notes,
                        now, inspection.updatedBy, tenant.projectId, inspection.inspectionId, inspection.version
                    )
                )
                if (rows == 0) {
                    throw IllegalStateException("Optimistic concurrency conflict on quality inspection '${inspection.inspectionId}'")
                }
                inspection.copy(updatedAt = now.time, version = inspection.version + 1)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update quality inspection")
        }
    }

    override suspend fun updateInspectionStatus(
        projectId: String,
        inspectionId: String,
        status: VendorInspectionStatus,
        overallResult: InspectionResult?,
        updatedBy: String
    ): DomainResult<VendorQualityInspection> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = if (overallResult != null) {
                    "UPDATE vendor_quality_inspections SET inspection_status = ?, overall_result = ?, updated_at = ?, updated_by = ?, version = version + 1 WHERE project_id = ? AND inspection_id = ?"
                } else {
                    "UPDATE vendor_quality_inspections SET inspection_status = ?, updated_at = ?, updated_by = ?, version = version + 1 WHERE project_id = ? AND inspection_id = ?"
                }
                val params = if (overallResult != null) {
                    listOf(status.name, overallResult.name, now, updatedBy, tenant.projectId, inspectionId)
                } else {
                    listOf(status.name, now, updatedBy, tenant.projectId, inspectionId)
                }
                val rows = ctx.sqlExecutor.executeUpdate(sql, params)
                if (rows == 0) throw NoSuchElementException("Quality inspection '$inspectionId' not found")

                val rawSql = "SELECT * FROM vendor_quality_inspections WHERE project_id = ? AND inspection_id = ?"
                val raw = ctx.sqlExecutor.querySingleOrNull(rawSql, listOf(tenant.projectId, inspectionId)) { rs -> mapInspectionRow(rs) }!!
                val itemSql = "SELECT * FROM vendor_quality_inspection_items WHERE project_id = ? AND inspection_id = ?"
                val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, inspectionId)) { rs -> mapInspectionItemRow(rs) }
                raw.copy(items = items)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update inspection status")
        }
    }

    override suspend fun createDefect(defect: VendorDefect): DomainResult<VendorDefect> {
        val tenant = TenantContext(defect.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    INSERT INTO vendor_defects (
                        project_id, defect_id, tenant_id, inspection_id, inspection_item_id,
                        vendor_id, defect_type, severity, description, quantity_affected,
                        detected_at, detected_by, evidence_reference, status, resolution_reference,
                        created_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, defect.defectId, defect.tenantId, defect.inspectionId, defect.inspectionItemId,
                        defect.vendorId, defect.defectType.name, defect.severity.name, defect.description, defect.quantityAffected,
                        Timestamp(defect.detectedAt), defect.detectedBy, defect.evidenceReference, defect.status, defect.resolutionReference,
                        now
                    )
                )
                defect.copy(createdAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor defect")
        }
    }

    override suspend fun listDefectsByInspection(projectId: String, inspectionId: String): DomainResult<List<VendorDefect>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_defects WHERE project_id = ? AND inspection_id = ? ORDER BY created_at ASC"
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, inspectionId)) { rs -> mapDefectRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list defects by inspection")
        }
    }

    override fun observeRejections(projectId: String, vendorId: String?, deliveryReceiptId: String?): Flow<List<VendorRejection>> {
        val key = "$projectId:$vendorId:$deliveryReceiptId"
        return synchronized(rejectionFlows) {
            rejectionFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findRejectionById(projectId: String, rejectionId: String): DomainResult<VendorRejection> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val rej = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_rejections WHERE project_id = ? AND rejection_id = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, rejectionId)) { rs -> mapRejectionRow(rs) }
            }
            if (rej != null) DomainResult.Success(rej)
            else DomainResult.Error(NoSuchElementException("Vendor rejection '$rejectionId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find rejection by id")
        }
    }

    override suspend fun findRejectionByReference(projectId: String, reference: String): DomainResult<VendorRejection> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val rej = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_rejections WHERE project_id = ? AND rejection_reference = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, reference)) { rs -> mapRejectionRow(rs) }
            }
            if (rej != null) DomainResult.Success(rej)
            else DomainResult.Error(NoSuchElementException("Vendor rejection '$reference' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find rejection by reference")
        }
    }

    override suspend fun listRejections(
        projectId: String,
        vendorId: String?,
        deliveryReceiptId: String?,
        status: VendorRejectionStatus?
    ): DomainResult<List<VendorRejection>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = StringBuilder("SELECT * FROM vendor_rejections WHERE project_id = ?")
                val params = mutableListOf<Any>(tenant.projectId)
                if (vendorId != null) {
                    sql.append(" AND vendor_id = ?")
                    params.add(vendorId)
                }
                if (deliveryReceiptId != null) {
                    sql.append(" AND delivery_receipt_id = ?")
                    params.add(deliveryReceiptId)
                }
                if (status != null) {
                    sql.append(" AND status = ?")
                    params.add(status.name)
                }
                sql.append(" ORDER BY created_at DESC")
                ctx.sqlExecutor.queryList(sql.toString(), params) { rs -> mapRejectionRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor rejections")
        }
    }

    override suspend fun createRejection(rejection: VendorRejection): DomainResult<VendorRejection> {
        val tenant = TenantContext(rejection.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    INSERT INTO vendor_rejections (
                        project_id, rejection_id, tenant_id, vendor_id, purchase_order_id,
                        delivery_receipt_id, delivery_receipt_item_id, inspection_id,
                        rejection_reference, rejection_type, rejection_reason, rejected_quantity,
                        rejected_value, status, disposition, replacement_required, return_required,
                        credit_required, notes, created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, rejection.rejectionId, rejection.tenantId, rejection.vendorId,
                        rejection.purchaseOrderId, rejection.deliveryReceiptId, rejection.deliveryReceiptItemId,
                        rejection.inspectionId, rejection.rejectionReference, rejection.rejectionType,
                        rejection.rejectionReason, rejection.rejectedQuantity, rejection.rejectedValue.amount,
                        rejection.status.name, rejection.disposition.name, rejection.replacementRequired,
                        rejection.returnRequired, rejection.creditRequired, rejection.notes,
                        now, rejection.createdBy, now, rejection.updatedBy
                    )
                )
                rejection.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor rejection")
        }
    }

    override suspend fun updateRejection(rejection: VendorRejection): DomainResult<VendorRejection> {
        val tenant = TenantContext(rejection.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    UPDATE vendor_rejections SET
                        rejection_reason = ?, rejected_quantity = ?, rejected_value = ?,
                        status = ?, disposition = ?, replacement_required = ?, return_required = ?,
                        credit_required = ?, notes = ?, vendor_response = ?, vendor_response_at = ?,
                        resolution_notes = ?, resolved_at = ?, resolved_by = ?,
                        updated_at = ?, updated_by = ?, version = version + 1
                    WHERE project_id = ? AND rejection_id = ? AND version = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        rejection.rejectionReason, rejection.rejectedQuantity, rejection.rejectedValue.amount,
                        rejection.status.name, rejection.disposition.name, rejection.replacementRequired,
                        rejection.returnRequired, rejection.creditRequired, rejection.notes, rejection.vendorResponse,
                        rejection.vendorResponseAt?.let { Timestamp(it) }, rejection.resolutionNotes,
                        rejection.resolvedAt?.let { Timestamp(it) }, rejection.resolvedBy,
                        now, rejection.updatedBy, tenant.projectId, rejection.rejectionId, rejection.version
                    )
                )
                if (rows == 0) throw IllegalStateException("Optimistic concurrency conflict on rejection '${rejection.rejectionId}'")
                rejection.copy(updatedAt = now.time, version = rejection.version + 1)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor rejection")
        }
    }

    override suspend fun updateRejectionStatus(
        projectId: String,
        rejectionId: String,
        status: VendorRejectionStatus,
        updatedBy: String,
        vendorResponse: String?,
        resolutionNotes: String?
    ): DomainResult<VendorRejection> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    UPDATE vendor_rejections SET
                        status = ?, vendor_response = COALESCE(?, vendor_response),
                        vendor_response_at = CASE WHEN ? IS NOT NULL THEN ? ELSE vendor_response_at END,
                        resolution_notes = COALESCE(?, resolution_notes),
                        resolved_at = CASE WHEN ? = 'RESOLVED' THEN ? ELSE resolved_at END,
                        resolved_by = CASE WHEN ? = 'RESOLVED' THEN ? ELSE resolved_by END,
                        updated_at = ?, updated_by = ?, version = version + 1
                    WHERE project_id = ? AND rejection_id = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        status.name, vendorResponse, vendorResponse, now,
                        resolutionNotes, status.name, now, status.name, updatedBy,
                        now, updatedBy, tenant.projectId, rejectionId
                    )
                )
                if (rows == 0) throw NoSuchElementException("Vendor rejection '$rejectionId' not found")

                val rawSql = "SELECT * FROM vendor_rejections WHERE project_id = ? AND rejection_id = ?"
                ctx.sqlExecutor.querySingleOrNull(rawSql, listOf(tenant.projectId, rejectionId)) { rs -> mapRejectionRow(rs) }!!
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update rejection status")
        }
    }

    override fun observeDisputes(projectId: String, vendorId: String?, status: VendorDisputeStatus?): Flow<List<VendorDispute>> {
        val key = "$projectId:$vendorId:$status"
        return synchronized(disputeFlows) {
            disputeFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findDisputeById(projectId: String, disputeId: String): DomainResult<VendorDispute> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val disp = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_disputes WHERE project_id = ? AND dispute_id = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, disputeId)) { rs -> mapDisputeRow(rs) }
            }
            if (disp != null) DomainResult.Success(disp)
            else DomainResult.Error(NoSuchElementException("Vendor dispute '$disputeId' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find dispute by id")
        }
    }

    override suspend fun findDisputeByReference(projectId: String, reference: String): DomainResult<VendorDispute> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val disp = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_disputes WHERE project_id = ? AND dispute_reference = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, reference)) { rs -> mapDisputeRow(rs) }
            }
            if (disp != null) DomainResult.Success(disp)
            else DomainResult.Error(NoSuchElementException("Vendor dispute '$reference' not found"))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find dispute by reference")
        }
    }

    override suspend fun listDisputes(
        projectId: String,
        vendorId: String?,
        status: VendorDisputeStatus?,
        disputeType: VendorDisputeType?
    ): DomainResult<List<VendorDispute>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = StringBuilder("SELECT * FROM vendor_disputes WHERE project_id = ?")
                val params = mutableListOf<Any>(tenant.projectId)
                if (vendorId != null) {
                    sql.append(" AND vendor_id = ?")
                    params.add(vendorId)
                }
                if (status != null) {
                    sql.append(" AND status = ?")
                    params.add(status.name)
                }
                if (disputeType != null) {
                    sql.append(" AND dispute_type = ?")
                    params.add(disputeType.name)
                }
                sql.append(" ORDER BY created_at DESC")
                ctx.sqlExecutor.queryList(sql.toString(), params) { rs -> mapDisputeRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor disputes")
        }
    }

    override suspend fun createDispute(dispute: VendorDispute): DomainResult<VendorDispute> {
        val tenant = TenantContext(dispute.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    INSERT INTO vendor_disputes (
                        project_id, dispute_id, tenant_id, vendor_id, purchase_order_id,
                        delivery_receipt_id, invoice_id, inspection_id, rejection_id,
                        dispute_reference, dispute_type, priority, status, subject, description,
                        disputed_quantity, disputed_amount, raised_by, assigned_to,
                        vendor_response_due_at, created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, dispute.disputeId, dispute.tenantId, dispute.vendorId,
                        dispute.purchaseOrderId, dispute.deliveryReceiptId, dispute.invoiceId,
                        dispute.inspectionId, dispute.rejectionId, dispute.disputeReference,
                        dispute.disputeType.name, dispute.priority.name, dispute.status.name,
                        dispute.subject, dispute.description, dispute.disputedQuantity, dispute.disputedAmount.amount,
                        dispute.raisedBy, dispute.assignedTo, dispute.vendorResponseDueAt?.let { Timestamp(it) },
                        now, dispute.createdBy, now, dispute.updatedBy
                    )
                )
                dispute.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor dispute")
        }
    }

    override suspend fun updateDispute(dispute: VendorDispute): DomainResult<VendorDispute> {
        val tenant = TenantContext(dispute.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    UPDATE vendor_disputes SET
                        priority = ?, status = ?, subject = ?, description = ?,
                        disputed_quantity = ?, disputed_amount = ?, assigned_to = ?,
                        vendor_response_due_at = ?, vendor_response = ?, vendor_response_at = ?,
                        resolution_proposal = ?, resolution = ?, resolved_at = ?, resolved_by = ?,
                        closed_at = ?, closed_by = ?, updated_at = ?, updated_by = ?, version = version + 1
                    WHERE project_id = ? AND dispute_id = ? AND version = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        dispute.priority.name, dispute.status.name, dispute.subject, dispute.description,
                        dispute.disputedQuantity, dispute.disputedAmount.amount, dispute.assignedTo,
                        dispute.vendorResponseDueAt?.let { Timestamp(it) }, dispute.vendorResponse,
                        dispute.vendorResponseAt?.let { Timestamp(it) }, dispute.resolutionProposal,
                        dispute.resolution, dispute.resolvedAt?.let { Timestamp(it) }, dispute.resolvedBy,
                        dispute.closedAt?.let { Timestamp(it) }, dispute.closedBy,
                        now, dispute.updatedBy, tenant.projectId, dispute.disputeId, dispute.version
                    )
                )
                if (rows == 0) throw IllegalStateException("Optimistic concurrency conflict on dispute '${dispute.disputeId}'")
                dispute.copy(updatedAt = now.time, version = dispute.version + 1)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor dispute")
        }
    }

    override suspend fun updateDisputeStatus(
        projectId: String,
        disputeId: String,
        status: VendorDisputeStatus,
        updatedBy: String,
        vendorResponse: String?,
        resolutionProposal: String?,
        resolution: String?
    ): DomainResult<VendorDispute> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val sql = """
                    UPDATE vendor_disputes SET
                        status = ?, vendor_response = COALESCE(?, vendor_response),
                        vendor_response_at = CASE WHEN ? IS NOT NULL THEN ? ELSE vendor_response_at END,
                        resolution_proposal = COALESCE(?, resolution_proposal),
                        resolution = COALESCE(?, resolution),
                        resolved_at = CASE WHEN ? = 'RESOLVED' THEN ? ELSE resolved_at END,
                        resolved_by = CASE WHEN ? = 'RESOLVED' THEN ? ELSE resolved_by END,
                        closed_at = CASE WHEN ? = 'CLOSED' THEN ? ELSE closed_at END,
                        closed_by = CASE WHEN ? = 'CLOSED' THEN ? ELSE closed_by END,
                        updated_at = ?, updated_by = ?, version = version + 1
                    WHERE project_id = ? AND dispute_id = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        status.name, vendorResponse, vendorResponse, now,
                        resolutionProposal, resolution,
                        status.name, now, status.name, updatedBy,
                        status.name, now, status.name, updatedBy,
                        now, updatedBy, tenant.projectId, disputeId
                    )
                )
                if (rows == 0) throw NoSuchElementException("Vendor dispute '$disputeId' not found")

                val rawSql = "SELECT * FROM vendor_disputes WHERE project_id = ? AND dispute_id = ?"
                ctx.sqlExecutor.querySingleOrNull(rawSql, listOf(tenant.projectId, disputeId)) { rs -> mapDisputeRow(rs) }!!
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update dispute status")
        }
    }

    override suspend fun appendDisputeEvent(event: VendorDisputeEvent): DomainResult<VendorDisputeEvent> {
        val tenant = TenantContext(event.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_dispute_events (
                        project_id, event_id, tenant_id, dispute_id, event_type,
                        actor_id, notes, payload_json, occurred_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, event.eventId, event.tenantId, event.disputeId,
                        event.eventType.name, event.actorId, event.notes, event.payloadJson,
                        Timestamp(event.occurredAt)
                    )
                )
                event
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "append dispute event")
        }
    }

    override suspend fun listDisputeEvents(projectId: String, disputeId: String): DomainResult<List<VendorDisputeEvent>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_dispute_events WHERE project_id = ? AND dispute_id = ? ORDER BY occurred_at ASC"
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, disputeId)) { rs -> mapDisputeEventRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list dispute events")
        }
    }

    override suspend fun appendQualityAudit(auditEvent: VendorQualityAuditEvent): DomainResult<VendorQualityAuditEvent> {
        val tenant = TenantContext(auditEvent.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_quality_audits (
                        project_id, audit_id, tenant_id, entity_type, entity_id,
                        event_type, actor_id, correlation_id, occurred_at, details
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, auditEvent.auditId, auditEvent.tenantId, auditEvent.entityType,
                        auditEvent.entityId, auditEvent.eventType, auditEvent.actorId, auditEvent.correlationId,
                        Timestamp(auditEvent.occurredAt), auditEvent.details
                    )
                )
                auditEvent
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "append quality audit")
        }
    }

    override suspend fun listQualityAudits(projectId: String, entityId: String): DomainResult<List<VendorQualityAuditEvent>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_quality_audits WHERE project_id = ? AND entity_id = ? ORDER BY occurred_at ASC"
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, entityId)) { rs -> mapQualityAuditRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list quality audits")
        }
    }

    override suspend fun addEvidence(evidence: VendorQualityEvidence): DomainResult<VendorQualityEvidence> {
        val tenant = TenantContext(evidence.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendor_quality_evidence (
                        project_id, evidence_id, tenant_id, source_type, source_id,
                        file_reference, file_name, file_type, description, uploaded_by,
                        uploaded_at, checksum
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId, evidence.evidenceId, evidence.tenantId, evidence.sourceType,
                        evidence.sourceId, evidence.fileReference, evidence.fileName, evidence.fileType,
                        evidence.description, evidence.uploadedBy, Timestamp(evidence.uploadedAt), evidence.checksum
                    )
                )
                evidence
            }
            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "add quality evidence")
        }
    }

    override suspend fun listEvidence(projectId: String, sourceType: String, sourceId: String): DomainResult<List<VendorQualityEvidence>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_quality_evidence WHERE project_id = ? AND source_type = ? AND source_id = ? ORDER BY uploaded_at ASC"
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, sourceType, sourceId)) { rs -> mapEvidenceRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list quality evidence")
        }
    }
}
