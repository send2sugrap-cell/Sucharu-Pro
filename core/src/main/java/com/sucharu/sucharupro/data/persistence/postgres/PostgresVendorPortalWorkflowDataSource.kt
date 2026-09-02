package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPortalWorkflowDataSource
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of VendorPortalWorkflowDataSource with RLS enforcement (Module 13 Step 11).
 */
class PostgresVendorPortalWorkflowDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPortalWorkflowDataSource {

    override suspend fun saveWorkflow(workflow: VendorWorkflowItem): VendorWorkflowItem =
        transactionManager.inTransaction(TenantContext(workflow.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_workflows (
                    workflow_id, tenant_id, project_id, vendor_id, correlation_id,
                    workflow_title, current_stage, status, sla_status,
                    rfq_id, quotation_id, purchase_order_id, work_order_id,
                    delivery_notice_id, invoice_id, quality_case_id, settlement_id,
                    started_at, completed_at, target_delivery_at,
                    created_at, updated_at, version, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (workflow_id) DO UPDATE SET
                    current_stage = EXCLUDED.current_stage,
                    status = EXCLUDED.status,
                    sla_status = EXCLUDED.sla_status,
                    completed_at = EXCLUDED.completed_at,
                    updated_at = EXCLUDED.updated_at,
                    version = vendor_portal_workflows.version + 1,
                    metadata_json = EXCLUDED.metadata_json
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, workflow.workflowId)
                stmt.setString(2, workflow.tenantId)
                stmt.setString(3, workflow.projectId)
                stmt.setString(4, workflow.vendorId)
                stmt.setString(5, workflow.correlationId)
                stmt.setString(6, workflow.workflowTitle)
                stmt.setString(7, workflow.currentStage.name)
                stmt.setString(8, workflow.status.name)
                stmt.setString(9, workflow.slaStatus.name)
                stmt.setString(10, workflow.rfqId)
                stmt.setString(11, workflow.quotationId)
                stmt.setString(12, workflow.purchaseOrderId)
                stmt.setString(13, workflow.workOrderId)
                stmt.setString(14, workflow.deliveryNoticeId)
                stmt.setString(15, workflow.invoiceId)
                stmt.setString(16, workflow.qualityCaseId)
                stmt.setString(17, workflow.settlementId)
                stmt.setLong(18, workflow.startedAt)
                if (workflow.completedAt != null) stmt.setLong(19, workflow.completedAt) else stmt.setNull(19, java.sql.Types.BIGINT)
                if (workflow.targetDeliveryAt != null) stmt.setLong(20, workflow.targetDeliveryAt) else stmt.setNull(20, java.sql.Types.BIGINT)
                stmt.setLong(21, workflow.createdAt)
                stmt.setLong(22, workflow.updatedAt)
                stmt.setLong(23, workflow.version)
                stmt.setString(24, mapToJson(workflow.metadata))
                stmt.executeUpdate()
            }
            workflow
        }

    override suspend fun updateWorkflow(workflow: VendorWorkflowItem): VendorWorkflowItem =
        saveWorkflow(workflow.copy(updatedAt = System.currentTimeMillis(), version = workflow.version + 1))

    override suspend fun findWorkflowById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): VendorWorkflowItem? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_workflows WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND workflow_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, workflowId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToWorkflow(rs) else null
                }
            }
        }

    override suspend fun findWorkflowByCorrelationId(
        tenantId: String,
        projectId: String,
        vendorId: String,
        correlationId: String
    ): VendorWorkflowItem? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_workflows WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND correlation_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, correlationId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToWorkflow(rs) else null
                }
            }
        }

    override suspend fun listWorkflows(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorWorkflowStatus?,
        stage: VendorWorkflowStage?,
        limit: Int,
        offset: Int
    ): List<VendorWorkflowItem> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sb = StringBuilder("SELECT * FROM vendor_portal_workflows WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (status != null) {
                sb.append(" AND status = ?")
                params.add(status.name)
            }
            if (stage != null) {
                sb.append(" AND current_stage = ?")
                params.add(stage.name)
            }
            sb.append(" ORDER BY updated_at DESC LIMIT ? OFFSET ?")
            params.add(limit)
            params.add(offset)

            ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                params.forEachIndexed { idx, p ->
                    when (p) {
                        is String -> stmt.setString(idx + 1, p)
                        is Int -> stmt.setInt(idx + 1, p)
                    }
                }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorWorkflowItem>()
                    while (rs.next()) {
                        list.add(mapRowToWorkflow(rs))
                    }
                    list
                }
            }
        }

    override suspend fun appendEvent(event: VendorWorkflowTimelineEvent): VendorWorkflowTimelineEvent =
        transactionManager.inTransaction(TenantContext(event.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_workflow_events (
                    event_id, workflow_id, tenant_id, project_id, vendor_id,
                    correlation_id, causation_id, stage, event_type,
                    title, description, source_module, actor_id, actor_type,
                    occurred_at, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.workflowId)
                stmt.setString(3, event.tenantId)
                stmt.setString(4, event.projectId)
                stmt.setString(5, event.vendorId)
                stmt.setString(6, event.correlationId)
                stmt.setString(7, event.causationId)
                stmt.setString(8, event.stage.name)
                stmt.setString(9, event.eventType)
                stmt.setString(10, event.title)
                stmt.setString(11, event.description)
                stmt.setString(12, event.sourceModule)
                stmt.setString(13, event.actorId)
                stmt.setString(14, event.actorType)
                stmt.setLong(15, event.occurredAt)
                stmt.setString(16, mapToJson(event.metadata))
                stmt.executeUpdate()
            }
            event
        }

    override suspend fun listEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): List<VendorWorkflowTimelineEvent> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_workflow_events WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND workflow_id = ? ORDER BY occurred_at ASC"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, workflowId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorWorkflowTimelineEvent>()
                    while (rs.next()) {
                        list.add(mapRowToEvent(rs))
                    }
                    list
                }
            }
        }

    override suspend fun saveException(exception: VendorWorkflowException): VendorWorkflowException =
        transactionManager.inTransaction(TenantContext(exception.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_workflow_exceptions (
                    exception_id, workflow_id, tenant_id, project_id, vendor_id,
                    category, severity, status, title, description,
                    detected_at, resolved_at, resolved_by, resolution_notes,
                    created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (exception_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    resolved_at = EXCLUDED.resolved_at,
                    resolved_by = EXCLUDED.resolved_by,
                    resolution_notes = EXCLUDED.resolution_notes,
                    updated_at = EXCLUDED.updated_at,
                    version = vendor_portal_workflow_exceptions.version + 1
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, exception.exceptionId)
                stmt.setString(2, exception.workflowId)
                stmt.setString(3, exception.tenantId)
                stmt.setString(4, exception.projectId)
                stmt.setString(5, exception.vendorId)
                stmt.setString(6, exception.category)
                stmt.setString(7, exception.severity.name)
                stmt.setString(8, exception.status.name)
                stmt.setString(9, exception.title)
                stmt.setString(10, exception.description)
                stmt.setLong(11, exception.detectedAt)
                if (exception.resolvedAt != null) stmt.setLong(12, exception.resolvedAt) else stmt.setNull(12, java.sql.Types.BIGINT)
                stmt.setString(13, exception.resolvedBy)
                stmt.setString(14, exception.resolutionNotes)
                stmt.setLong(15, exception.createdAt)
                stmt.setLong(16, exception.updatedAt)
                stmt.setLong(17, exception.version)
                stmt.executeUpdate()
            }
            exception
        }

    override suspend fun updateException(exception: VendorWorkflowException): VendorWorkflowException =
        saveException(exception.copy(updatedAt = System.currentTimeMillis(), version = exception.version + 1))

    override suspend fun findExceptionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        exceptionId: String
    ): VendorWorkflowException? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_workflow_exceptions WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND exception_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, exceptionId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToException(rs) else null
                }
            }
        }

    override suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String?,
        status: VendorWorkflowExceptionStatus?
    ): List<VendorWorkflowException> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sb = StringBuilder("SELECT * FROM vendor_portal_workflow_exceptions WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (workflowId != null) {
                sb.append(" AND workflow_id = ?")
                params.add(workflowId)
            }
            if (status != null) {
                sb.append(" AND status = ?")
                params.add(status.name)
            }
            sb.append(" ORDER BY detected_at DESC")

            ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                params.forEachIndexed { idx, p ->
                    stmt.setString(idx + 1, p.toString())
                }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorWorkflowException>()
                    while (rs.next()) {
                        list.add(mapRowToException(rs))
                    }
                    list
                }
            }
        }

    override suspend fun saveAction(action: VendorWorkflowNextAction): VendorWorkflowNextAction =
        transactionManager.inTransaction(TenantContext(action.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_workflow_actions (
                    action_id, workflow_id, tenant_id, project_id, vendor_id,
                    action_type, title, description, required_role, priority,
                    due_at, deep_link_target, is_completed, completed_at, completed_by,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (action_id) DO UPDATE SET
                    is_completed = EXCLUDED.is_completed,
                    completed_at = EXCLUDED.completed_at,
                    completed_by = EXCLUDED.completed_by,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, action.actionId)
                stmt.setString(2, action.workflowId)
                stmt.setString(3, action.tenantId)
                stmt.setString(4, action.projectId)
                stmt.setString(5, action.vendorId)
                stmt.setString(6, action.actionType.name)
                stmt.setString(7, action.title)
                stmt.setString(8, action.description)
                stmt.setString(9, action.requiredRole)
                stmt.setString(10, action.priority.name)
                if (action.dueAt != null) stmt.setLong(11, action.dueAt) else stmt.setNull(11, java.sql.Types.BIGINT)
                stmt.setString(12, action.deepLinkTarget)
                stmt.setBoolean(13, action.isCompleted)
                if (action.completedAt != null) stmt.setLong(14, action.completedAt) else stmt.setNull(14, java.sql.Types.BIGINT)
                stmt.setString(15, action.completedBy)
                stmt.setLong(16, action.createdAt)
                stmt.setLong(17, action.updatedAt)
                stmt.executeUpdate()
            }
            action
        }

    override suspend fun updateAction(action: VendorWorkflowNextAction): VendorWorkflowNextAction =
        saveAction(action.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun findActionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String
    ): VendorWorkflowNextAction? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_workflow_actions WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND action_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, actionId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToAction(rs) else null
                }
            }
        }

    override suspend fun listActions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String?,
        isCompleted: Boolean?
    ): List<VendorWorkflowNextAction> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sb = StringBuilder("SELECT * FROM vendor_portal_workflow_actions WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId, vendorId)

            if (workflowId != null) {
                sb.append(" AND workflow_id = ?")
                params.add(workflowId)
            }
            if (isCompleted != null) {
                sb.append(" AND is_completed = ?")
                params.add(isCompleted)
            }
            sb.append(" ORDER BY due_at ASC NULLS LAST")

            ctx.connection.prepareStatement(sb.toString()).use { stmt ->
                params.forEachIndexed { idx, p ->
                    when (p) {
                        is String -> stmt.setString(idx + 1, p)
                        is Boolean -> stmt.setBoolean(idx + 1, p)
                    }
                }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorWorkflowNextAction>()
                    while (rs.next()) {
                        list.add(mapRowToAction(rs))
                    }
                    list
                }
            }
        }

    override suspend fun appendAudit(audit: VendorWorkflowAuditEntry): VendorWorkflowAuditEntry =
        transactionManager.inTransaction(TenantContext(audit.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_workflow_audit_events (
                    audit_id, workflow_id, tenant_id, project_id, vendor_id,
                    actor_id, actor_role, action, entity_type, entity_id,
                    correlation_id, reason, occurred_at, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, audit.auditId)
                stmt.setString(2, audit.workflowId)
                stmt.setString(3, audit.tenantId)
                stmt.setString(4, audit.projectId)
                stmt.setString(5, audit.vendorId)
                stmt.setString(6, audit.actorId)
                stmt.setString(7, audit.actorRole)
                stmt.setString(8, audit.action)
                stmt.setString(9, audit.entityType)
                stmt.setString(10, audit.entityId)
                stmt.setString(11, audit.correlationId)
                stmt.setString(12, audit.reason)
                stmt.setLong(13, audit.occurredAt)
                stmt.setString(14, mapToJson(audit.metadata))
                stmt.executeUpdate()
            }
            audit
        }

    override suspend fun listAudits(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): List<VendorWorkflowAuditEntry> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_workflow_audit_events WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND workflow_id = ? ORDER BY occurred_at DESC"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, workflowId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorWorkflowAuditEntry>()
                    while (rs.next()) {
                        list.add(mapRowToAudit(rs))
                    }
                    list
                }
            }
        }

    // --- Row Mappers ---
    private fun mapRowToWorkflow(rs: ResultSet): VendorWorkflowItem =
        VendorWorkflowItem(
            workflowId = rs.getString("workflow_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            correlationId = rs.getString("correlation_id"),
            workflowTitle = rs.getString("workflow_title"),
            currentStage = VendorWorkflowStage.valueOf(rs.getString("current_stage")),
            status = VendorWorkflowStatus.valueOf(rs.getString("status")),
            slaStatus = VendorWorkflowSlaStatus.valueOf(rs.getString("sla_status")),
            rfqId = rs.getString("rfq_id"),
            quotationId = rs.getString("quotation_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            workOrderId = rs.getString("work_order_id"),
            deliveryNoticeId = rs.getString("delivery_notice_id"),
            invoiceId = rs.getString("invoice_id"),
            qualityCaseId = rs.getString("quality_case_id"),
            settlementId = rs.getString("settlement_id"),
            startedAt = rs.getLong("started_at"),
            completedAt = rs.getLong("completed_at").takeIf { !rs.wasNull() },
            targetDeliveryAt = rs.getLong("target_delivery_at").takeIf { !rs.wasNull() },
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version"),
            metadata = jsonToMap(rs.getString("metadata_json"))
        )

    private fun mapRowToEvent(rs: ResultSet): VendorWorkflowTimelineEvent =
        VendorWorkflowTimelineEvent(
            eventId = rs.getString("event_id"),
            workflowId = rs.getString("workflow_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            correlationId = rs.getString("correlation_id"),
            causationId = rs.getString("causation_id"),
            stage = VendorWorkflowStage.valueOf(rs.getString("stage")),
            eventType = rs.getString("event_type"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            sourceModule = rs.getString("source_module"),
            actorId = rs.getString("actor_id"),
            actorType = rs.getString("actor_type"),
            occurredAt = rs.getLong("occurred_at"),
            metadata = jsonToMap(rs.getString("metadata_json"))
        )

    private fun mapRowToException(rs: ResultSet): VendorWorkflowException =
        VendorWorkflowException(
            exceptionId = rs.getString("exception_id"),
            workflowId = rs.getString("workflow_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            category = rs.getString("category"),
            severity = VendorWorkflowPriority.valueOf(rs.getString("severity")),
            status = VendorWorkflowExceptionStatus.valueOf(rs.getString("status")),
            title = rs.getString("title"),
            description = rs.getString("description"),
            detectedAt = rs.getLong("detected_at"),
            resolvedAt = rs.getLong("resolved_at").takeIf { !rs.wasNull() },
            resolvedBy = rs.getString("resolved_by"),
            resolutionNotes = rs.getString("resolution_notes"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )

    private fun mapRowToAction(rs: ResultSet): VendorWorkflowNextAction =
        VendorWorkflowNextAction(
            actionId = rs.getString("action_id"),
            workflowId = rs.getString("workflow_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            actionType = VendorWorkflowActionType.valueOf(rs.getString("action_type")),
            title = rs.getString("title"),
            description = rs.getString("description"),
            requiredRole = rs.getString("required_role"),
            priority = VendorWorkflowPriority.valueOf(rs.getString("priority")),
            dueAt = rs.getLong("due_at").takeIf { !rs.wasNull() },
            deepLinkTarget = rs.getString("deep_link_target"),
            isCompleted = rs.getBoolean("is_completed"),
            completedAt = rs.getLong("completed_at").takeIf { !rs.wasNull() },
            completedBy = rs.getString("completed_by"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )

    private fun mapRowToAudit(rs: ResultSet): VendorWorkflowAuditEntry =
        VendorWorkflowAuditEntry(
            auditId = rs.getString("audit_id"),
            workflowId = rs.getString("workflow_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            action = rs.getString("action"),
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            correlationId = rs.getString("correlation_id"),
            reason = rs.getString("reason"),
            occurredAt = rs.getLong("occurred_at"),
            metadata = jsonToMap(rs.getString("metadata_json"))
        )

    // --- JSON Helpers ---
    private fun mapToJson(map: Map<String, String>): String {
        if (map.isEmpty()) return "{}"
        val entries = map.entries.joinToString(",") { "\"${escape(it.key)}\":\"${escape(it.value)}\"" }
        return "{$entries}"
    }

    private fun jsonToMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        val cleaned = json.trim().removePrefix("{").removeSuffix("}").trim()
        if (cleaned.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val pairs = cleaned.split(",")
        for (pair in pairs) {
            val kv = pair.split(":")
            if (kv.size == 2) {
                val k = kv[0].trim().removeSurrounding("\"")
                val v = kv[1].trim().removeSurrounding("\"")
                result[k] = v
            }
        }
        return result
    }

    private fun escape(str: String): String = str.replace("\"", "\\\"")
}
