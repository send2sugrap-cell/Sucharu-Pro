package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPortalQualityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorDefectSeverity
import com.sucharu.sucharupro.domain.model.vendor.VendorDisputeType
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of VendorPortalQualityDataSource with tenant RLS enforcement.
 */
class PostgresVendorPortalQualityDataSource(
    private val transactionManager: TransactionManager
) : VendorPortalQualityDataSource {

    override suspend fun saveQualityCase(case: VendorPortalQualityCase): DomainResult<VendorPortalQualityCase> =
        transactionManager.inTransaction(TenantContext(case.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_quality_cases (
                    case_id, tenant_id, project_id, vendor_id, inspection_id, delivery_receipt_id,
                    purchase_order_id, rejection_id, case_number, status, title, description,
                    severity, acknowledged_at, acknowledged_by, closed_at, closed_by,
                    created_at, created_by, updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (case_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    acknowledged_at = EXCLUDED.acknowledged_at,
                    acknowledged_by = EXCLUDED.acknowledged_by,
                    closed_at = EXCLUDED.closed_at,
                    closed_by = EXCLUDED.closed_by,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    version = vendor_portal_quality_cases.version + 1
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, case.caseId)
                stmt.setString(2, case.tenantId)
                stmt.setString(3, case.projectId)
                stmt.setString(4, case.vendorId)
                stmt.setString(5, case.inspectionId)
                stmt.setString(6, case.deliveryReceiptId)
                stmt.setString(7, case.purchaseOrderId)
                stmt.setString(8, case.rejectionId)
                stmt.setString(9, case.caseNumber)
                stmt.setString(10, case.status.name)
                stmt.setString(11, case.title)
                stmt.setString(12, case.description)
                stmt.setString(13, case.severity.name)
                stmt.setObject(14, case.acknowledgedAt)
                stmt.setString(15, case.acknowledgedBy)
                stmt.setObject(16, case.closedAt)
                stmt.setString(17, case.closedBy)
                stmt.setLong(18, case.createdAt)
                stmt.setString(19, case.createdBy)
                stmt.setLong(20, case.updatedAt)
                stmt.setString(21, case.updatedBy)
                stmt.setLong(22, case.version)
                stmt.executeUpdate()
            }
            DomainResult.Success(case)
        }

    override suspend fun findQualityCaseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String
    ): DomainResult<VendorPortalQualityCase?> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_quality_cases WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND case_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, caseId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) DomainResult.Success(mapQualityCase(rs)) else DomainResult.Success(null)
                }
            }
        }

    override suspend fun listQualityCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalQualityCaseStatus?
    ): DomainResult<List<VendorPortalQualityCase>> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_quality_cases WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            if (status != null) sql.append(" AND status = ?")
            sql.append(" ORDER BY created_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                if (status != null) stmt.setString(4, status.name)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalQualityCase>()
                    while (rs.next()) list.add(mapQualityCase(rs))
                    DomainResult.Success(list)
                }
            }
        }

    override suspend fun saveCapaPlan(capa: VendorPortalCapaPlan): DomainResult<VendorPortalCapaPlan> =
        transactionManager.inTransaction(TenantContext(capa.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_capa_plans (
                    capa_id, tenant_id, project_id, vendor_id, case_id, inspection_id,
                    rejection_id, capa_number, status, priority, title, root_cause,
                    corrective_action, preventive_action, responsible_person, target_completion_date,
                    actual_completion_date, affected_quantity, affected_unit, verification_status,
                    verified_by, verified_at, reviewer_comments, created_at, created_by,
                    updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (capa_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    root_cause = EXCLUDED.root_cause,
                    corrective_action = EXCLUDED.corrective_action,
                    preventive_action = EXCLUDED.preventive_action,
                    actual_completion_date = EXCLUDED.actual_completion_date,
                    verification_status = EXCLUDED.verification_status,
                    verified_by = EXCLUDED.verified_by,
                    verified_at = EXCLUDED.verified_at,
                    reviewer_comments = EXCLUDED.reviewer_comments,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    version = vendor_portal_capa_plans.version + 1
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, capa.capaId)
                stmt.setString(2, capa.tenantId)
                stmt.setString(3, capa.projectId)
                stmt.setString(4, capa.vendorId)
                stmt.setString(5, capa.caseId)
                stmt.setString(6, capa.inspectionId)
                stmt.setString(7, capa.rejectionId)
                stmt.setString(8, capa.capaNumber)
                stmt.setString(9, capa.status.name)
                stmt.setString(10, capa.priority.name)
                stmt.setString(11, capa.title)
                stmt.setString(12, capa.rootCause)
                stmt.setString(13, capa.correctiveAction)
                stmt.setString(14, capa.preventiveAction)
                stmt.setString(15, capa.responsiblePerson)
                stmt.setLong(16, capa.targetCompletionDate)
                stmt.setObject(17, capa.actualCompletionDate)
                stmt.setBigDecimal(18, capa.affectedQuantity)
                stmt.setString(19, capa.affectedUnit)
                stmt.setString(20, capa.verificationStatus)
                stmt.setString(21, capa.verifiedBy)
                stmt.setObject(22, capa.verifiedAt)
                stmt.setString(23, capa.reviewerComments)
                stmt.setLong(24, capa.createdAt)
                stmt.setString(25, capa.createdBy)
                stmt.setLong(26, capa.updatedAt)
                stmt.setString(27, capa.updatedBy)
                stmt.setLong(28, capa.version)
                stmt.executeUpdate()
            }
            DomainResult.Success(capa)
        }

    override suspend fun findCapaPlanById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        capaId: String
    ): DomainResult<VendorPortalCapaPlan?> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_capa_plans WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND capa_id = ?"
            val plan = ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, capaId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapCapaPlan(rs) else null
                }
            } ?: return@inReadOnly DomainResult.Success(null)

            val actionsRes = listCapaActions(tenantId, projectId, capaId)
            val actions = (actionsRes as? DomainResult.Success)?.data ?: emptyList()
            DomainResult.Success(plan.copy(actions = actions))
        }

    override suspend fun listCapaPlans(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalCapaStatus?,
        caseId: String?
    ): DomainResult<List<VendorPortalCapaPlan>> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_capa_plans WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            if (status != null) sql.append(" AND status = ?")
            if (caseId != null) sql.append(" AND case_id = ?")
            sql.append(" ORDER BY created_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                var idx = 1
                stmt.setString(idx++, tenantId)
                stmt.setString(idx++, projectId)
                stmt.setString(idx++, vendorId)
                if (status != null) stmt.setString(idx++, status.name)
                if (caseId != null) stmt.setString(idx++, caseId)

                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalCapaPlan>()
                    while (rs.next()) list.add(mapCapaPlan(rs))
                    DomainResult.Success(list)
                }
            }
        }

    override suspend fun saveCapaAction(action: VendorPortalCapaAction): DomainResult<VendorPortalCapaAction> =
        transactionManager.inTransaction(TenantContext(action.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_capa_actions (
                    action_id, capa_id, tenant_id, project_id, action_number, action_type,
                    description, owner, target_date, status, completed_at, evidence_references, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (action_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    completed_at = EXCLUDED.completed_at,
                    evidence_references = EXCLUDED.evidence_references,
                    notes = EXCLUDED.notes
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, action.actionId)
                stmt.setString(2, action.capaId)
                stmt.setString(3, action.tenantId)
                stmt.setString(4, action.projectId)
                stmt.setInt(5, action.actionNumber)
                stmt.setString(6, action.actionType.name)
                stmt.setString(7, action.description)
                stmt.setString(8, action.owner)
                stmt.setLong(9, action.targetDate)
                stmt.setString(10, action.status.name)
                stmt.setObject(11, action.completedAt)
                stmt.setString(12, action.evidenceReferences.joinToString(","))
                stmt.setString(13, action.notes)
                stmt.executeUpdate()
            }
            DomainResult.Success(action)
        }

    override suspend fun listCapaActions(
        tenantId: String,
        projectId: String,
        capaId: String
    ): DomainResult<List<VendorPortalCapaAction>> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_capa_actions WHERE tenant_id = ? AND project_id = ? AND capa_id = ? ORDER BY action_number ASC"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, capaId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalCapaAction>()
                    while (rs.next()) list.add(mapCapaAction(rs))
                    DomainResult.Success(list)
                }
            }
        }

    override suspend fun saveDisputeSubmission(dispute: VendorPortalDisputeSummary): DomainResult<VendorPortalDisputeSummary> =
        transactionManager.inTransaction(TenantContext(dispute.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_dispute_submissions (
                    dispute_submission_id, tenant_id, project_id, vendor_id, dispute_id,
                    source_type, source_id, dispute_type, priority, status, subject,
                    description, requested_resolution, disputed_quantity, disputed_amount,
                    vendor_response, vendor_response_at, resolution_proposal, resolution_status,
                    resolution_notes, created_at, created_by, updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (dispute_submission_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    vendor_response = EXCLUDED.vendor_response,
                    vendor_response_at = EXCLUDED.vendor_response_at,
                    resolution_proposal = EXCLUDED.resolution_proposal,
                    resolution_status = EXCLUDED.resolution_status,
                    resolution_notes = EXCLUDED.resolution_notes,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    version = vendor_portal_dispute_submissions.version + 1
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, dispute.disputeId)
                stmt.setString(2, dispute.tenantId)
                stmt.setString(3, dispute.projectId)
                stmt.setString(4, dispute.vendorId)
                stmt.setString(5, dispute.disputeId)
                stmt.setString(6, dispute.sourceType)
                stmt.setString(7, dispute.sourceId)
                stmt.setString(8, dispute.disputeType.name)
                stmt.setString(9, dispute.priority.name)
                stmt.setString(10, dispute.status.name)
                stmt.setString(11, dispute.subject)
                stmt.setString(12, dispute.description)
                stmt.setString(13, dispute.requestedResolution.name)
                stmt.setBigDecimal(14, dispute.disputedQuantity)
                stmt.setBigDecimal(15, dispute.disputedAmount.amount)
                stmt.setString(16, dispute.vendorResponse)
                stmt.setObject(17, dispute.vendorResponseAt)
                stmt.setString(18, dispute.resolutionProposal)
                stmt.setString(19, if (dispute.resolution != null) "RESOLVED" else "NONE")
                stmt.setString(20, dispute.resolution)
                stmt.setLong(21, dispute.createdAt)
                stmt.setString(22, dispute.raisedBy)
                stmt.setLong(23, dispute.updatedAt)
                stmt.setString(24, dispute.raisedBy)
                stmt.setLong(25, dispute.version)
                stmt.executeUpdate()
            }
            DomainResult.Success(dispute)
        }

    override suspend fun findDisputeSubmissionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<VendorPortalDisputeSummary?> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_dispute_submissions WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND dispute_submission_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, disputeId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) DomainResult.Success(mapDispute(rs)) else DomainResult.Success(null)
                }
            }
        }

    override suspend fun listDisputeSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDisputeStatus?
    ): DomainResult<List<VendorPortalDisputeSummary>> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = StringBuilder("SELECT * FROM vendor_portal_dispute_submissions WHERE tenant_id = ? AND project_id = ? AND vendor_id = ?")
            if (status != null) sql.append(" AND status = ?")
            sql.append(" ORDER BY created_at DESC")

            ctx.connection.prepareStatement(sql.toString()).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                if (status != null) stmt.setString(4, status.name)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalDisputeSummary>()
                    while (rs.next()) list.add(mapDispute(rs))
                    DomainResult.Success(list)
                }
            }
        }

    override suspend fun saveResolutionResponse(response: VendorPortalResolutionResponse): DomainResult<VendorPortalResolutionResponse> =
        transactionManager.inTransaction(TenantContext(response.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_resolution_responses (
                    response_id, tenant_id, project_id, vendor_id, dispute_id, proposal_action, rationale, responded_by, responded_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, response.responseId)
                stmt.setString(2, response.tenantId)
                stmt.setString(3, response.projectId)
                stmt.setString(4, response.vendorId)
                stmt.setString(5, response.disputeId)
                stmt.setString(6, response.proposalAction.name)
                stmt.setString(7, response.rationale)
                stmt.setString(8, response.respondedBy)
                stmt.setLong(9, response.respondedAt)
                stmt.executeUpdate()
            }
            DomainResult.Success(response)
        }

    override suspend fun listResolutionResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<List<VendorPortalResolutionResponse>> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_resolution_responses WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND dispute_id = ? ORDER BY responded_at DESC"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, disputeId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalResolutionResponse>()
                    while (rs.next()) {
                        list.add(
                            VendorPortalResolutionResponse(
                                responseId = rs.getString("response_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                vendorId = rs.getString("vendor_id"),
                                disputeId = rs.getString("dispute_id"),
                                proposalAction = VendorPortalProposalAction.valueOf(rs.getString("proposal_action")),
                                rationale = rs.getString("rationale"),
                                respondedBy = rs.getString("responded_by"),
                                respondedAt = rs.getLong("responded_at")
                            )
                        )
                    }
                    DomainResult.Success(list)
                }
            }
        }

    override suspend fun saveEvidence(evidence: VendorPortalQualityEvidence): DomainResult<VendorPortalQualityEvidence> =
        transactionManager.inTransaction(TenantContext(evidence.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_quality_evidence (
                    evidence_id, tenant_id, project_id, vendor_id, entity_type, entity_id,
                    evidence_type, filename, file_reference, size_bytes, checksum, description,
                    uploaded_by, uploaded_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, evidence.evidenceId)
                stmt.setString(2, evidence.tenantId)
                stmt.setString(3, evidence.projectId)
                stmt.setString(4, evidence.vendorId)
                stmt.setString(5, evidence.entityType)
                stmt.setString(6, evidence.entityId)
                stmt.setString(7, evidence.evidenceType.name)
                stmt.setString(8, evidence.filename)
                stmt.setString(9, evidence.fileReference)
                stmt.setLong(10, evidence.sizeBytes)
                stmt.setString(11, evidence.checksum)
                stmt.setString(12, evidence.description)
                stmt.setString(13, evidence.uploadedBy)
                stmt.setLong(14, evidence.uploadedAt)
                stmt.executeUpdate()
            }
            DomainResult.Success(evidence)
        }

    override suspend fun findEvidenceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evidenceId: String
    ): DomainResult<VendorPortalQualityEvidence?> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_quality_evidence WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND evidence_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, evidenceId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) DomainResult.Success(mapEvidence(rs)) else DomainResult.Success(null)
                }
            }
        }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalQualityEvidence>> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_quality_evidence WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND entity_type = ? AND entity_id = ? ORDER BY uploaded_at DESC"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, entityType)
                stmt.setString(5, entityId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalQualityEvidence>()
                    while (rs.next()) list.add(mapEvidence(rs))
                    DomainResult.Success(list)
                }
            }
        }

    override suspend fun recordAudit(activity: VendorPortalQualityActivity): DomainResult<Unit> =
        transactionManager.inTransaction(TenantContext(activity.projectId)) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_quality_audit_events (
                    audit_id, tenant_id, project_id, vendor_id, entity_type, entity_id, action, actor_id, details, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, activity.activityId)
                stmt.setString(2, activity.tenantId)
                stmt.setString(3, activity.projectId)
                stmt.setString(4, activity.vendorId)
                stmt.setString(5, activity.entityType)
                stmt.setString(6, activity.entityId)
                stmt.setString(7, activity.action)
                stmt.setString(8, activity.actorId)
                stmt.setString(9, activity.details)
                stmt.setLong(10, activity.timestamp)
                stmt.executeUpdate()
            }
            DomainResult.Success(Unit)
        }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalQualityActivity>> =
        transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
            val sql = "SELECT * FROM vendor_portal_quality_audit_events WHERE tenant_id = ? AND project_id = ? AND vendor_id = ? AND entity_type = ? AND entity_id = ? ORDER BY occurred_at DESC"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, vendorId)
                stmt.setString(4, entityType)
                stmt.setString(5, entityId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<VendorPortalQualityActivity>()
                    while (rs.next()) {
                        list.add(
                            VendorPortalQualityActivity(
                                activityId = rs.getString("audit_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                vendorId = rs.getString("vendor_id"),
                                entityType = rs.getString("entity_type"),
                                entityId = rs.getString("entity_id"),
                                action = rs.getString("action"),
                                actorId = rs.getString("actor_id"),
                                details = rs.getString("details"),
                                timestamp = rs.getLong("occurred_at")
                            )
                        )
                    }
                    DomainResult.Success(list)
                }
            }
        }

    private fun mapQualityCase(rs: ResultSet) = VendorPortalQualityCase(
        caseId = rs.getString("case_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        vendorId = rs.getString("vendor_id"),
        inspectionId = rs.getString("inspection_id"),
        deliveryReceiptId = rs.getString("delivery_receipt_id"),
        purchaseOrderId = rs.getString("purchase_order_id"),
        rejectionId = rs.getString("rejection_id"),
        caseNumber = rs.getString("case_number"),
        status = VendorPortalQualityCaseStatus.valueOf(rs.getString("status")),
        title = rs.getString("title"),
        description = rs.getString("description"),
        severity = VendorDefectSeverity.valueOf(rs.getString("severity")),
        acknowledgedAt = rs.getObject("acknowledged_at") as? Long,
        acknowledgedBy = rs.getString("acknowledged_by"),
        closedAt = rs.getObject("closed_at") as? Long,
        closedBy = rs.getString("closed_by"),
        createdAt = rs.getLong("created_at"),
        createdBy = rs.getString("created_by"),
        updatedAt = rs.getLong("updated_at"),
        updatedBy = rs.getString("updated_by"),
        version = rs.getLong("version")
    )

    private fun mapCapaPlan(rs: ResultSet) = VendorPortalCapaPlan(
        capaId = rs.getString("capa_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        vendorId = rs.getString("vendor_id"),
        caseId = rs.getString("case_id"),
        inspectionId = rs.getString("inspection_id"),
        rejectionId = rs.getString("rejection_id"),
        capaNumber = rs.getString("capa_number"),
        status = VendorPortalCapaStatus.valueOf(rs.getString("status")),
        priority = VendorPortalQualityPriority.valueOf(rs.getString("priority")),
        title = rs.getString("title"),
        rootCause = rs.getString("root_cause"),
        correctiveAction = rs.getString("corrective_action"),
        preventiveAction = rs.getString("preventive_action"),
        responsiblePerson = rs.getString("responsible_person"),
        targetCompletionDate = rs.getLong("target_completion_date"),
        actualCompletionDate = rs.getObject("actual_completion_date") as? Long,
        affectedQuantity = rs.getBigDecimal("affected_quantity"),
        affectedUnit = rs.getString("affected_unit"),
        verificationStatus = rs.getString("verification_status"),
        verifiedBy = rs.getString("verified_by"),
        verifiedAt = rs.getObject("verified_at") as? Long,
        reviewerComments = rs.getString("reviewer_comments"),
        createdAt = rs.getLong("created_at"),
        createdBy = rs.getString("created_by"),
        updatedAt = rs.getLong("updated_at"),
        updatedBy = rs.getString("updated_by"),
        version = rs.getLong("version")
    )

    private fun mapCapaAction(rs: ResultSet) = VendorPortalCapaAction(
        actionId = rs.getString("action_id"),
        capaId = rs.getString("capa_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        actionNumber = rs.getInt("action_number"),
        actionType = VendorPortalCapaActionType.valueOf(rs.getString("action_type")),
        description = rs.getString("description"),
        owner = rs.getString("owner"),
        targetDate = rs.getLong("target_date"),
        status = VendorPortalCapaActionStatus.valueOf(rs.getString("status")),
        completedAt = rs.getObject("completed_at") as? Long,
        evidenceReferences = rs.getString("evidence_references")?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        notes = rs.getString("notes")
    )

    private fun mapDispute(rs: ResultSet) = VendorPortalDisputeSummary(
        disputeId = rs.getString("dispute_submission_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        vendorId = rs.getString("vendor_id"),
        disputeReference = rs.getString("dispute_id") ?: rs.getString("dispute_submission_id"),
        sourceType = rs.getString("source_type"),
        sourceId = rs.getString("source_id"),
        disputeType = VendorDisputeType.valueOf(rs.getString("dispute_type")),
        priority = VendorPortalQualityPriority.valueOf(rs.getString("priority")),
        status = VendorPortalDisputeStatus.valueOf(rs.getString("status")),
        subject = rs.getString("subject"),
        description = rs.getString("description"),
        requestedResolution = VendorPortalResolutionType.valueOf(rs.getString("requested_resolution")),
        disputedQuantity = rs.getBigDecimal("disputed_quantity"),
        disputedAmount = Money(rs.getBigDecimal("disputed_amount")),
        raisedBy = rs.getString("created_by"),
        vendorResponse = rs.getString("vendor_response"),
        vendorResponseAt = rs.getObject("vendor_response_at") as? Long,
        resolutionProposal = rs.getString("resolution_proposal"),
        resolution = rs.getString("resolution_notes"),
        resolvedAt = null,
        resolvedBy = null,
        createdAt = rs.getLong("created_at"),
        updatedAt = rs.getLong("updated_at"),
        version = rs.getLong("version")
    )

    private fun mapEvidence(rs: ResultSet) = VendorPortalQualityEvidence(
        evidenceId = rs.getString("evidence_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        vendorId = rs.getString("vendor_id"),
        entityType = rs.getString("entity_type"),
        entityId = rs.getString("entity_id"),
        evidenceType = VendorPortalQualityEvidenceType.valueOf(rs.getString("evidence_type")),
        filename = rs.getString("filename"),
        fileReference = rs.getString("file_reference"),
        sizeBytes = rs.getLong("size_bytes"),
        checksum = rs.getString("checksum"),
        description = rs.getString("description"),
        uploadedBy = rs.getString("uploaded_by"),
        uploadedAt = rs.getLong("uploaded_at")
    )
}
