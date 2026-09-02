package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPortalSettlementDataSource
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of VendorPortalSettlementDataSource with RLS enforcement (Module 13 Step 09).
 */
class PostgresVendorPortalSettlementDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPortalSettlementDataSource {

    // --- Settlement Acknowledgement ---

    override suspend fun saveAcknowledgement(acknowledgement: VendorPortalSettlementAcknowledgement): VendorPortalSettlementAcknowledgement =
        transactionManager.inTransaction(TenantContext(acknowledgement.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_settlement_acknowledgements (
                    acknowledgement_id, settlement_id, tenant_id, project_id, vendor_id,
                    acknowledged_by, acknowledged_at, status, idempotency_key, discrepancy_flag,
                    discrepancy_notes, evidence_references
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (acknowledgement_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    discrepancy_flag = EXCLUDED.discrepancy_flag,
                    discrepancy_notes = EXCLUDED.discrepancy_notes,
                    evidence_references = EXCLUDED.evidence_references
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, acknowledgement.acknowledgementId)
                stmt.setString(2, acknowledgement.settlementId)
                stmt.setString(3, acknowledgement.tenantId)
                stmt.setString(4, acknowledgement.projectId)
                stmt.setString(5, acknowledgement.vendorId)
                stmt.setString(6, acknowledgement.acknowledgedBy)
                stmt.setLong(7, acknowledgement.acknowledgedAt)
                stmt.setString(8, acknowledgement.status.name)
                stmt.setString(9, acknowledgement.idempotencyKey)
                stmt.setBoolean(10, acknowledgement.discrepancyFlag)
                stmt.setString(11, acknowledgement.discrepancyNotes)
                stmt.setArray(12, ctx.connection.createArrayOf("text", acknowledgement.evidenceReferences.toTypedArray()))
                stmt.executeUpdate()
            }
            acknowledgement
        }

    override suspend fun findAcknowledgementById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        acknowledgementId: String
    ): VendorPortalSettlementAcknowledgement? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_settlement_acknowledgements
                WHERE acknowledgement_id = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, acknowledgementId)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAcknowledgement(rs) else null
                }
            }
        }

    override suspend fun findAcknowledgementBySettlementId(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String
    ): VendorPortalSettlementAcknowledgement? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_settlement_acknowledgements
                WHERE settlement_id = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, settlementId)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAcknowledgement(rs) else null
                }
            }
        }

    override suspend fun findAcknowledgementByIdempotencyKey(
        tenantId: String,
        projectId: String,
        vendorId: String,
        idempotencyKey: String
    ): VendorPortalSettlementAcknowledgement? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_settlement_acknowledgements
                WHERE idempotency_key = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, idempotencyKey)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAcknowledgement(rs) else null
                }
            }
        }

    // --- Reconciliation Cases ---

    override suspend fun saveReconciliationCase(reconciliationCase: VendorPortalReconciliationCase): VendorPortalReconciliationCase =
        transactionManager.inTransaction(TenantContext(reconciliationCase.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_reconciliation_cases (
                    case_id, tenant_id, project_id, vendor_id, settlement_id, invoice_id,
                    case_number, subject, status, claimed_amount, system_amount, variance_amount,
                    currency, notes, created_by, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (case_id) DO UPDATE SET
                    subject = EXCLUDED.subject,
                    status = EXCLUDED.status,
                    claimed_amount = EXCLUDED.claimed_amount,
                    system_amount = EXCLUDED.system_amount,
                    variance_amount = EXCLUDED.variance_amount,
                    notes = EXCLUDED.notes,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, reconciliationCase.caseId)
                stmt.setString(2, reconciliationCase.tenantId)
                stmt.setString(3, reconciliationCase.projectId)
                stmt.setString(4, reconciliationCase.vendorId)
                stmt.setString(5, reconciliationCase.settlementId)
                stmt.setString(6, reconciliationCase.invoiceId)
                stmt.setString(7, reconciliationCase.caseNumber)
                stmt.setString(8, reconciliationCase.subject)
                stmt.setString(9, reconciliationCase.status.name)
                stmt.setBigDecimal(10, reconciliationCase.claimedAmount.amount)
                stmt.setBigDecimal(11, reconciliationCase.systemAmount.amount)
                stmt.setBigDecimal(12, reconciliationCase.varianceAmount.amount)
                stmt.setString(13, reconciliationCase.currency)
                stmt.setString(14, reconciliationCase.notes)
                stmt.setString(15, reconciliationCase.createdBy)
                stmt.setLong(16, reconciliationCase.createdAt)
                stmt.setLong(17, reconciliationCase.updatedAt)
                stmt.executeUpdate()
            }
            reconciliationCase
        }

    override suspend fun findReconciliationCaseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String
    ): VendorPortalReconciliationCase? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_reconciliation_cases
                WHERE case_id = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?
            """.trimIndent()
            val case = ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, caseId)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapReconciliationCase(rs) else null
                }
            } ?: return@inReadOnly null

            val eventsSql = "SELECT * FROM vendor_portal_reconciliation_events WHERE case_id = ? ORDER BY timestamp ASC"
            val events = ctx.connection.prepareStatement(eventsSql).use { stmt ->
                stmt.setString(1, caseId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalReconciliationEvent>()
                    while (rs.next()) {
                        list.add(mapReconciliationEvent(rs))
                    }
                    list
                }
            }
            case.copy(events = events)
        }

    override suspend fun listReconciliationCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        status: VendorPortalReconciliationCaseStatus?
    ): List<VendorPortalReconciliationCase> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_reconciliation_cases WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (settlementId != null) {
                sql.append(" AND settlement_id = ?")
                params.add(settlementId)
            }
            if (invoiceId != null) {
                sql.append(" AND invoice_id = ?")
                params.add(invoiceId)
            }
            if (status != null) {
                sql.append(" AND status = ?")
                params.add(status.name)
            }
            sql.append(" ORDER BY created_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalReconciliationCase>()
                    while (rs.next()) {
                        list.add(mapReconciliationCase(rs))
                    }
                    list
                }
            }
        }

    override suspend fun appendReconciliationEvent(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        event: VendorPortalReconciliationEvent
    ) {
        transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_reconciliation_events (
                    event_id, case_id, actor_id, actor_role, action, remarks, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.caseId)
                stmt.setString(3, event.actorId)
                stmt.setString(4, event.actorRole)
                stmt.setString(5, event.action)
                stmt.setString(6, event.remarks)
                stmt.setLong(7, event.timestamp)
                stmt.executeUpdate()
            }
        }
    }

    // --- Financial Disputes ---

    override suspend fun saveFinancialDispute(dispute: VendorPortalFinancialDispute): VendorPortalFinancialDispute =
        transactionManager.inTransaction(TenantContext(dispute.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_financial_disputes (
                    dispute_id, tenant_id, project_id, vendor_id, settlement_id, invoice_id,
                    dispute_number, category, priority, status, disputed_amount,
                    proposed_resolution_amount, currency, reason, resolution_notes,
                    created_by, created_at, updated_at, resolved_by, resolved_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (dispute_id) DO UPDATE SET
                    priority = EXCLUDED.priority,
                    status = EXCLUDED.status,
                    proposed_resolution_amount = EXCLUDED.proposed_resolution_amount,
                    resolution_notes = EXCLUDED.resolution_notes,
                    updated_at = EXCLUDED.updated_at,
                    resolved_by = EXCLUDED.resolved_by,
                    resolved_at = EXCLUDED.resolved_at
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, dispute.disputeId)
                stmt.setString(2, dispute.tenantId)
                stmt.setString(3, dispute.projectId)
                stmt.setString(4, dispute.vendorId)
                stmt.setString(5, dispute.settlementId)
                stmt.setString(6, dispute.invoiceId)
                stmt.setString(7, dispute.disputeNumber)
                stmt.setString(8, dispute.category)
                stmt.setString(9, dispute.priority)
                stmt.setString(10, dispute.status.name)
                stmt.setBigDecimal(11, dispute.disputedAmount.amount)
                stmt.setObject(12, dispute.proposedResolutionAmount?.amount)
                stmt.setString(13, dispute.currency)
                stmt.setString(14, dispute.reason)
                stmt.setString(15, dispute.resolutionNotes)
                stmt.setString(16, dispute.createdBy)
                stmt.setLong(17, dispute.createdAt)
                stmt.setLong(18, dispute.updatedAt)
                stmt.setString(19, dispute.resolvedBy)
                stmt.setObject(20, dispute.resolvedAt)
                stmt.executeUpdate()
            }
            dispute
        }

    override suspend fun findFinancialDisputeById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): VendorPortalFinancialDispute? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_financial_disputes
                WHERE dispute_id = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?
            """.trimIndent()
            val dispute = ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, disputeId)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapFinancialDispute(rs) else null
                }
            } ?: return@inReadOnly null

            val eventsSql = "SELECT * FROM vendor_portal_financial_dispute_events WHERE dispute_id = ? ORDER BY timestamp ASC"
            val events = ctx.connection.prepareStatement(eventsSql).use { stmt ->
                stmt.setString(1, disputeId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalFinancialDisputeEvent>()
                    while (rs.next()) {
                        list.add(mapFinancialDisputeEvent(rs))
                    }
                    list
                }
            }
            dispute.copy(events = events)
        }

    override suspend fun listFinancialDisputes(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        status: VendorPortalFinancialDisputeStatus?
    ): List<VendorPortalFinancialDispute> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_financial_disputes WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (settlementId != null) {
                sql.append(" AND settlement_id = ?")
                params.add(settlementId)
            }
            if (invoiceId != null) {
                sql.append(" AND invoice_id = ?")
                params.add(invoiceId)
            }
            if (status != null) {
                sql.append(" AND status = ?")
                params.add(status.name)
            }
            sql.append(" ORDER BY created_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalFinancialDispute>()
                    while (rs.next()) {
                        list.add(mapFinancialDispute(rs))
                    }
                    list
                }
            }
        }

    override suspend fun appendFinancialDisputeEvent(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        event: VendorPortalFinancialDisputeEvent
    ) {
        transactionManager.inTransaction(TenantContext(projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_financial_dispute_events (
                    event_id, dispute_id, actor_id, actor_role, action, remarks, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.disputeId)
                stmt.setString(3, event.actorId)
                stmt.setString(4, event.actorRole)
                stmt.setString(5, event.action)
                stmt.setString(6, event.remarks)
                stmt.setLong(7, event.timestamp)
                stmt.executeUpdate()
            }
        }
    }

    // --- Financial Evidence ---

    override suspend fun saveEvidence(evidence: VendorPortalFinancialSettlementEvidence): VendorPortalFinancialSettlementEvidence =
        transactionManager.inTransaction(TenantContext(evidence.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_financial_evidence (
                    evidence_id, tenant_id, project_id, vendor_id, entity_type, entity_id,
                    evidence_type, file_name, file_url, checksum, file_size_bytes,
                    mime_type, description, uploaded_by, uploaded_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (evidence_id) DO NOTHING
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, evidence.evidenceId)
                stmt.setString(2, evidence.tenantId)
                stmt.setString(3, evidence.projectId)
                stmt.setString(4, evidence.vendorId)
                stmt.setString(5, evidence.entityType)
                stmt.setString(6, evidence.entityId)
                stmt.setString(7, evidence.evidenceType.name)
                stmt.setString(8, evidence.fileName)
                stmt.setString(9, evidence.fileUrl)
                stmt.setString(10, evidence.checksum)
                stmt.setLong(11, evidence.fileSizeBytes)
                stmt.setString(12, evidence.mimeType)
                stmt.setString(13, evidence.description)
                stmt.setString(14, evidence.uploadedBy)
                stmt.setLong(15, evidence.uploadedAt)
                stmt.executeUpdate()
            }
            evidence
        }

    override suspend fun findEvidenceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evidenceId: String
    ): VendorPortalFinancialSettlementEvidence? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_financial_evidence
                WHERE evidence_id = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, evidenceId)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapEvidence(rs) else null
                }
            }
        }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): List<VendorPortalFinancialSettlementEvidence> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_financial_evidence WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (entityType != null) {
                sql.append(" AND entity_type = ?")
                params.add(entityType)
            }
            if (entityId != null) {
                sql.append(" AND entity_id = ?")
                params.add(entityId)
            }
            sql.append(" ORDER BY uploaded_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalFinancialSettlementEvidence>()
                    while (rs.next()) {
                        list.add(mapEvidence(rs))
                    }
                    list
                }
            }
        }

    // --- Financial Threads & Messages ---

    override suspend fun saveThread(thread: VendorPortalFinancialThread): VendorPortalFinancialThread =
        transactionManager.inTransaction(TenantContext(thread.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_financial_threads (
                    thread_id, tenant_id, project_id, vendor_id, context_type, context_id,
                    subject, status, created_by, created_at, updated_at, message_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (thread_id) DO UPDATE SET
                    subject = EXCLUDED.subject,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    message_count = EXCLUDED.message_count
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, thread.threadId)
                stmt.setString(2, thread.tenantId)
                stmt.setString(3, thread.projectId)
                stmt.setString(4, thread.vendorId)
                stmt.setString(5, thread.contextType)
                stmt.setString(6, thread.contextId)
                stmt.setString(7, thread.subject)
                stmt.setString(8, thread.status)
                stmt.setString(9, thread.createdBy)
                stmt.setLong(10, thread.createdAt)
                stmt.setLong(11, thread.updatedAt)
                stmt.setInt(12, thread.messageCount)
                stmt.executeUpdate()
            }
            thread
        }

    override suspend fun findThreadById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): VendorPortalFinancialThread? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_financial_threads
                WHERE thread_id = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, threadId)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapThread(rs) else null
                }
            }
        }

    override suspend fun listThreads(
        tenantId: String,
        projectId: String,
        vendorId: String,
        contextType: String?,
        contextId: String?
    ): List<VendorPortalFinancialThread> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_financial_threads WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (contextType != null) {
                sql.append(" AND context_type = ?")
                params.add(contextType)
            }
            if (contextId != null) {
                sql.append(" AND context_id = ?")
                params.add(contextId)
            }
            sql.append(" ORDER BY updated_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalFinancialThread>()
                    while (rs.next()) {
                        list.add(mapThread(rs))
                    }
                    list
                }
            }
        }

    override suspend fun saveMessage(message: VendorPortalFinancialMessage): VendorPortalFinancialMessage =
        transactionManager.inTransaction(TenantContext(message.projectId)) { ctx ->
            val msgSql = """
                INSERT INTO vendor_portal_financial_messages (
                    message_id, thread_id, tenant_id, project_id, vendor_id,
                    sender_id, sender_role, content, evidence_references, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            ctx.connection.prepareStatement(msgSql).use { stmt ->
                stmt.setString(1, message.messageId)
                stmt.setString(2, message.threadId)
                stmt.setString(3, message.tenantId)
                stmt.setString(4, message.projectId)
                stmt.setString(5, message.vendorId)
                stmt.setString(6, message.senderId)
                stmt.setString(7, message.senderRole)
                stmt.setString(8, message.content)
                stmt.setArray(9, ctx.connection.createArrayOf("text", message.evidenceReferences.toTypedArray()))
                stmt.setLong(10, message.timestamp)
                stmt.executeUpdate()
            }

            val updateThreadSql = """
                UPDATE vendor_portal_financial_threads
                SET message_count = message_count + 1, updated_at = ?
                WHERE thread_id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(updateThreadSql).use { stmt ->
                stmt.setLong(1, message.timestamp)
                stmt.setString(2, message.threadId)
                stmt.setString(3, message.tenantId)
                stmt.setString(4, message.projectId)
                stmt.executeUpdate()
            }
            message
        }

    override suspend fun listMessages(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): List<VendorPortalFinancialMessage> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_financial_messages
                WHERE thread_id = ? AND tenant_id = ? AND project_id = ? AND vendor_id = ?
                ORDER BY timestamp ASC
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, threadId)
                stmt.setString(2, tenantId)
                stmt.setString(3, projectId)
                stmt.setString(4, vendorId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalFinancialMessage>()
                    while (rs.next()) {
                        list.add(mapMessage(rs))
                    }
                    list
                }
            }
        }

    // --- Activity Events ---

    override suspend fun recordActivity(activity: VendorPortalFinancialActivityEvent) {
        transactionManager.inTransaction(TenantContext(activity.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_financial_audit_events (
                    activity_id, tenant_id, project_id, vendor_id, event_type,
                    entity_type, entity_id, actor_id, actor_role, description, occurred_at, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, activity.activityId)
                stmt.setString(2, activity.tenantId)
                stmt.setString(3, activity.projectId)
                stmt.setString(4, activity.vendorId)
                stmt.setString(5, activity.eventType.name)
                stmt.setString(6, activity.entityType)
                stmt.setString(7, activity.entityId)
                stmt.setString(8, activity.actorId)
                stmt.setString(9, activity.actorRole)
                stmt.setString(10, activity.description)
                stmt.setLong(11, activity.occurredAt)
                stmt.setString(12, activity.metadata.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }.let { "{$it}" })
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun listActivities(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): List<VendorPortalFinancialActivityEvent> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_financial_audit_events WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (entityType != null) {
                sql.append(" AND entity_type = ?")
                params.add(entityType)
            }
            if (entityId != null) {
                sql.append(" AND entity_id = ?")
                params.add(entityId)
            }
            sql.append(" ORDER BY occurred_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalFinancialActivityEvent>()
                    while (rs.next()) {
                        list.add(mapActivity(rs))
                    }
                    list
                }
            }
        }

    // --- Helpers / Mappers ---

    private fun mapAcknowledgement(rs: ResultSet): VendorPortalSettlementAcknowledgement {
        val rawRefs = rs.getArray("evidence_references")?.array as? Array<*>
        val refs = rawRefs?.mapNotNull { it?.toString() } ?: emptyList()
        return VendorPortalSettlementAcknowledgement(
            acknowledgementId = rs.getString("acknowledgement_id"),
            settlementId = rs.getString("settlement_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            acknowledgedBy = rs.getString("acknowledged_by"),
            acknowledgedAt = rs.getLong("acknowledged_at"),
            status = VendorPortalSettlementViewStatus.valueOf(rs.getString("status")),
            idempotencyKey = rs.getString("idempotency_key"),
            discrepancyFlag = rs.getBoolean("discrepancy_flag"),
            discrepancyNotes = rs.getString("discrepancy_notes"),
            evidenceReferences = refs
        )
    }

    private fun mapReconciliationCase(rs: ResultSet): VendorPortalReconciliationCase {
        val currency = rs.getString("currency") ?: "BDT"
        return VendorPortalReconciliationCase(
            caseId = rs.getString("case_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            settlementId = rs.getString("settlement_id"),
            invoiceId = rs.getString("invoice_id"),
            caseNumber = rs.getString("case_number"),
            subject = rs.getString("subject"),
            status = VendorPortalReconciliationCaseStatus.valueOf(rs.getString("status")),
            claimedAmount = Money(rs.getBigDecimal("claimed_amount")),
            systemAmount = Money(rs.getBigDecimal("system_amount")),
            varianceAmount = Money(rs.getBigDecimal("variance_amount")),
            currency = currency,
            notes = rs.getString("notes"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapReconciliationEvent(rs: ResultSet): VendorPortalReconciliationEvent {
        return VendorPortalReconciliationEvent(
            eventId = rs.getString("event_id"),
            caseId = rs.getString("case_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            action = rs.getString("action"),
            remarks = rs.getString("remarks"),
            timestamp = rs.getLong("timestamp")
        )
    }

    private fun mapFinancialDispute(rs: ResultSet): VendorPortalFinancialDispute {
        val currency = rs.getString("currency") ?: "BDT"
        val propAmt = rs.getBigDecimal("proposed_resolution_amount")
        return VendorPortalFinancialDispute(
            disputeId = rs.getString("dispute_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            settlementId = rs.getString("settlement_id"),
            invoiceId = rs.getString("invoice_id"),
            disputeNumber = rs.getString("dispute_number"),
            category = rs.getString("category"),
            priority = rs.getString("priority"),
            status = VendorPortalFinancialDisputeStatus.valueOf(rs.getString("status")),
            disputedAmount = Money(rs.getBigDecimal("disputed_amount")),
            proposedResolutionAmount = if (propAmt != null) Money(propAmt) else null,
            currency = currency,
            reason = rs.getString("reason"),
            resolutionNotes = rs.getString("resolution_notes"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            resolvedBy = rs.getString("resolved_by"),
            resolvedAt = rs.getObject("resolved_at") as? Long
        )
    }

    private fun mapFinancialDisputeEvent(rs: ResultSet): VendorPortalFinancialDisputeEvent {
        return VendorPortalFinancialDisputeEvent(
            eventId = rs.getString("event_id"),
            disputeId = rs.getString("dispute_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            action = rs.getString("action"),
            remarks = rs.getString("remarks"),
            timestamp = rs.getLong("timestamp")
        )
    }

    private fun mapEvidence(rs: ResultSet): VendorPortalFinancialSettlementEvidence {
        return VendorPortalFinancialSettlementEvidence(
            evidenceId = rs.getString("evidence_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            evidenceType = VendorPortalSettlementEvidenceType.valueOf(rs.getString("evidence_type")),
            fileName = rs.getString("file_name"),
            fileUrl = rs.getString("file_url"),
            checksum = rs.getString("checksum"),
            fileSizeBytes = rs.getLong("file_size_bytes"),
            mimeType = rs.getString("mime_type") ?: "application/pdf",
            description = rs.getString("description"),
            uploadedBy = rs.getString("uploaded_by"),
            uploadedAt = rs.getLong("uploaded_at")
        )
    }

    private fun mapThread(rs: ResultSet): VendorPortalFinancialThread {
        return VendorPortalFinancialThread(
            threadId = rs.getString("thread_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            contextType = rs.getString("context_type"),
            contextId = rs.getString("context_id"),
            subject = rs.getString("subject"),
            status = rs.getString("status"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            messageCount = rs.getInt("message_count")
        )
    }

    private fun mapMessage(rs: ResultSet): VendorPortalFinancialMessage {
        val rawRefs = rs.getArray("evidence_references")?.array as? Array<*>
        val refs = rawRefs?.mapNotNull { it?.toString() } ?: emptyList()
        return VendorPortalFinancialMessage(
            messageId = rs.getString("message_id"),
            threadId = rs.getString("thread_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            senderId = rs.getString("sender_id"),
            senderRole = rs.getString("sender_role"),
            content = rs.getString("content"),
            evidenceReferences = refs,
            timestamp = rs.getLong("timestamp")
        )
    }

    private fun mapActivity(rs: ResultSet): VendorPortalFinancialActivityEvent {
        return VendorPortalFinancialActivityEvent(
            activityId = rs.getString("activity_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            eventType = VendorPortalFinancialActivityEventType.valueOf(rs.getString("event_type")),
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            description = rs.getString("description"),
            occurredAt = rs.getLong("occurred_at"),
            metadata = emptyMap()
        )
    }
}
