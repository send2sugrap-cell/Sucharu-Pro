package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPortalPerformanceComplianceDataSource
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of VendorPortalPerformanceComplianceDataSource with tenant RLS enforcement.
 */
class PostgresVendorPortalPerformanceComplianceDataSource(
    private val transactionManager: TransactionManager
) : VendorPortalPerformanceComplianceDataSource {

    override suspend fun saveEvaluationResponse(response: VendorPortalEvaluationResponse): VendorPortalEvaluationResponse =
        transactionManager.inTransaction(TenantContext(response.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_evaluation_responses (
                    response_id, evaluation_id, tenant_id, project_id, vendor_id,
                    response_type, subject, remarks, proposed_remediation, evidence_references,
                    status, submitted_by, submitted_at, reviewer_feedback, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (response_id) DO UPDATE SET
                    subject = EXCLUDED.subject,
                    remarks = EXCLUDED.remarks,
                    proposed_remediation = EXCLUDED.proposed_remediation,
                    evidence_references = EXCLUDED.evidence_references,
                    status = EXCLUDED.status,
                    reviewer_feedback = EXCLUDED.reviewer_feedback,
                    version = vendor_portal_evaluation_responses.version + 1
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, response.responseId)
                stmt.setString(2, response.evaluationId)
                stmt.setString(3, response.tenantId)
                stmt.setString(4, response.projectId)
                stmt.setString(5, response.vendorId)
                stmt.setString(6, response.responseType.name)
                stmt.setString(7, response.subject)
                stmt.setString(8, response.remarks)
                stmt.setString(9, response.proposedRemediation)
                stmt.setArray(10, ctx.connection.createArrayOf("text", response.evidenceReferences.toTypedArray()))
                stmt.setString(11, response.status.name)
                stmt.setString(12, response.submittedBy)
                stmt.setLong(13, response.submittedAt)
                stmt.setString(14, response.reviewerFeedback)
                stmt.setLong(15, response.version)
                stmt.executeUpdate()
            }
            response
        }

    override suspend fun findEvaluationResponseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        responseId: String
    ): VendorPortalEvaluationResponse? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_evaluation_responses
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND response_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, responseId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapEvaluationResponse(rs) else null
                }
            }
        }

    override suspend fun listEvaluationResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evaluationId: String
    ): List<VendorPortalEvaluationResponse> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_evaluation_responses
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND evaluation_id = ?
                ORDER BY submitted_at DESC
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, evaluationId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalEvaluationResponse>()
                    while (rs.next()) {
                        list.add(mapEvaluationResponse(rs))
                    }
                    list
                }
            }
        }

    override suspend fun saveComplianceEvidence(evidence: VendorPortalComplianceEvidence): VendorPortalComplianceEvidence =
        transactionManager.inTransaction(TenantContext(evidence.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_compliance_evidence (
                    evidence_id, record_id, requirement_id, action_id, tenant_id, project_id,
                    vendor_id, evidence_type, file_name, file_url, checksum, file_size_bytes,
                    mime_type, description, uploaded_by, uploaded_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (evidence_id) DO UPDATE SET
                    description = EXCLUDED.description,
                    version = vendor_portal_compliance_evidence.version + 1
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, evidence.evidenceId)
                stmt.setString(2, evidence.recordId)
                stmt.setString(3, evidence.requirementId)
                stmt.setString(4, evidence.actionId)
                stmt.setString(5, evidence.tenantId)
                stmt.setString(6, evidence.projectId)
                stmt.setString(7, evidence.vendorId)
                stmt.setString(8, evidence.evidenceType.name)
                stmt.setString(9, evidence.fileName)
                stmt.setString(10, evidence.fileUrl)
                stmt.setString(11, evidence.checksum)
                stmt.setLong(12, evidence.fileSizeBytes)
                stmt.setString(13, evidence.mimeType)
                stmt.setString(14, evidence.description)
                stmt.setString(15, evidence.uploadedBy)
                stmt.setLong(16, evidence.uploadedAt)
                stmt.setLong(17, evidence.version)
                stmt.executeUpdate()
            }
            evidence
        }

    override suspend fun findComplianceEvidenceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evidenceId: String
    ): VendorPortalComplianceEvidence? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_compliance_evidence
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND evidence_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, evidenceId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapComplianceEvidence(rs) else null
                }
            }
        }

    override suspend fun listComplianceEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        recordId: String?,
        actionId: String?
    ): List<VendorPortalComplianceEvidence> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_compliance_evidence WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            if (recordId != null) sql.append(" AND record_id = ?")
            if (actionId != null) sql.append(" AND action_id = ?")
            sql.append(" ORDER BY uploaded_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                var idx = 1
                stmt.setString(idx++, tenantId)
                stmt.setString(idx++, projectId)
                stmt.setString(idx++, vendorId)
                if (recordId != null) stmt.setString(idx++, recordId)
                if (actionId != null) stmt.setString(idx++, actionId)

                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalComplianceEvidence>()
                    while (rs.next()) {
                        list.add(mapComplianceEvidence(rs))
                    }
                    list
                }
            }
        }

    override suspend fun saveCorrectiveActionResponse(response: VendorPortalCorrectiveActionResponse): VendorPortalCorrectiveActionResponse =
        transactionManager.inTransaction(TenantContext(response.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_corrective_action_responses (
                    response_id, action_id, tenant_id, project_id, vendor_id,
                    remediation_notes, root_cause_explanation, progress_percentage,
                    is_completion_request, evidence_references, status, submitted_by,
                    submitted_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (response_id) DO UPDATE SET
                    remediation_notes = EXCLUDED.remediation_notes,
                    root_cause_explanation = EXCLUDED.root_cause_explanation,
                    progress_percentage = EXCLUDED.progress_percentage,
                    is_completion_request = EXCLUDED.is_completion_request,
                    evidence_references = EXCLUDED.evidence_references,
                    status = EXCLUDED.status,
                    version = vendor_portal_corrective_action_responses.version + 1
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, response.responseId)
                stmt.setString(2, response.actionId)
                stmt.setString(3, response.tenantId)
                stmt.setString(4, response.projectId)
                stmt.setString(5, response.vendorId)
                stmt.setString(6, response.remediationNotes)
                stmt.setString(7, response.rootCauseExplanation)
                stmt.setBigDecimal(8, java.math.BigDecimal.valueOf(response.progressPercentage))
                stmt.setBoolean(9, response.isCompletionRequest)
                stmt.setArray(10, ctx.connection.createArrayOf("text", response.evidenceReferences.toTypedArray()))
                stmt.setString(11, response.status.name)
                stmt.setString(12, response.submittedBy)
                stmt.setLong(13, response.submittedAt)
                stmt.setLong(14, response.version)
                stmt.executeUpdate()
            }
            response
        }

    override suspend fun findCorrectiveActionResponseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        responseId: String
    ): VendorPortalCorrectiveActionResponse? =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_corrective_action_responses
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND response_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, responseId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapCorrectiveActionResponse(rs) else null
                }
            }
        }

    override suspend fun listCorrectiveActionResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String
    ): List<VendorPortalCorrectiveActionResponse> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = """
                SELECT * FROM vendor_portal_corrective_action_responses
                WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND action_id = ?
                ORDER BY submitted_at DESC
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, actionId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalCorrectiveActionResponse>()
                    while (rs.next()) {
                        list.add(mapCorrectiveActionResponse(rs))
                    }
                    list
                }
            }
        }

    override suspend fun recordAudit(activity: VendorPortalPerformanceComplianceActivity) {
        transactionManager.inTransaction(TenantContext(activity.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_performance_compliance_audit_events (
                    activity_id, tenant_id, project_id, vendor_id, event_type,
                    entity_type, entity_id, actor_id, actor_role, description,
                    occurred_at, metadata
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
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
                stmt.setString(12, "{}")
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): List<VendorPortalPerformanceComplianceActivity> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_performance_compliance_audit_events WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            if (entityType != null) sql.append(" AND entity_type = ?")
            if (entityId != null) sql.append(" AND entity_id = ?")
            sql.append(" ORDER BY occurred_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                var idx = 1
                stmt.setString(idx++, tenantId)
                stmt.setString(idx++, projectId)
                stmt.setString(idx++, vendorId)
                if (entityType != null) stmt.setString(idx++, entityType)
                if (entityId != null) stmt.setString(idx++, entityId)

                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalPerformanceComplianceActivity>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    list
                }
            }
        }

    private fun mapEvaluationResponse(rs: ResultSet): VendorPortalEvaluationResponse {
        val evidenceArr = rs.getArray("evidence_references")
        val evidenceList = if (evidenceArr != null) (evidenceArr.array as Array<*>).filterIsInstance<String>() else emptyList()

        return VendorPortalEvaluationResponse(
            responseId = rs.getString("response_id"),
            evaluationId = rs.getString("evaluation_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            responseType = VendorPortalEvaluationResponseType.valueOf(rs.getString("response_type")),
            subject = rs.getString("subject"),
            remarks = rs.getString("remarks"),
            proposedRemediation = rs.getString("proposed_remediation"),
            evidenceReferences = evidenceList,
            status = VendorPortalEvaluationResponseStatus.valueOf(rs.getString("status")),
            submittedBy = rs.getString("submitted_by"),
            submittedAt = rs.getLong("submitted_at"),
            reviewerFeedback = rs.getString("reviewer_feedback"),
            version = rs.getLong("version")
        )
    }

    private fun mapComplianceEvidence(rs: ResultSet): VendorPortalComplianceEvidence =
        VendorPortalComplianceEvidence(
            evidenceId = rs.getString("evidence_id"),
            recordId = rs.getString("record_id"),
            requirementId = rs.getString("requirement_id"),
            actionId = rs.getString("action_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            evidenceType = VendorPortalComplianceEvidenceType.valueOf(rs.getString("evidence_type")),
            fileName = rs.getString("file_name"),
            fileUrl = rs.getString("file_url"),
            checksum = rs.getString("checksum"),
            fileSizeBytes = rs.getLong("file_size_bytes"),
            mimeType = rs.getString("mime_type"),
            description = rs.getString("description"),
            uploadedBy = rs.getString("uploaded_by"),
            uploadedAt = rs.getLong("uploaded_at"),
            version = rs.getLong("version")
        )

    private fun mapCorrectiveActionResponse(rs: ResultSet): VendorPortalCorrectiveActionResponse {
        val evidenceArr = rs.getArray("evidence_references")
        val evidenceList = if (evidenceArr != null) (evidenceArr.array as Array<*>).filterIsInstance<String>() else emptyList()

        return VendorPortalCorrectiveActionResponse(
            responseId = rs.getString("response_id"),
            actionId = rs.getString("action_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            remediationNotes = rs.getString("remediation_notes"),
            rootCauseExplanation = rs.getString("root_cause_explanation"),
            progressPercentage = rs.getBigDecimal("progress_percentage")?.toDouble() ?: 0.0,
            isCompletionRequest = rs.getBoolean("is_completion_request"),
            evidenceReferences = evidenceList,
            status = VendorPortalRemediationStatus.valueOf(rs.getString("status")),
            submittedBy = rs.getString("submitted_by"),
            submittedAt = rs.getLong("submitted_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): VendorPortalPerformanceComplianceActivity =
        VendorPortalPerformanceComplianceActivity(
            activityId = rs.getString("activity_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            eventType = VendorPortalPerformanceComplianceAuditEventType.valueOf(rs.getString("event_type")),
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            description = rs.getString("description"),
            occurredAt = rs.getLong("occurred_at"),
            metadata = emptyMap()
        )
}
