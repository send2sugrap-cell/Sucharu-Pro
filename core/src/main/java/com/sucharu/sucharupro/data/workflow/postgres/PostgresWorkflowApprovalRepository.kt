package com.sucharu.sucharupro.data.workflow.postgres

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecision
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecisionType
import com.sucharu.sucharupro.domain.workflow.model.ApprovalEscalation
import com.sucharu.sucharupro.domain.workflow.model.ApprovalPolicy
import com.sucharu.sucharupro.domain.workflow.model.ApprovalRequest
import com.sucharu.sucharupro.domain.workflow.model.ApprovalStatus
import com.sucharu.sucharupro.domain.workflow.model.HumanConfirmationMetadata
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for workflow approval persistence (INFRA-04 Step 05).
 */
interface WorkflowApprovalRepository {
    suspend fun savePolicy(policy: ApprovalPolicy, tenantContext: TenantContext)
    suspend fun getPolicyById(policyId: String, tenantContext: TenantContext): ApprovalPolicy?
    suspend fun createApprovalRequest(request: ApprovalRequest, tenantContext: TenantContext): Boolean
    suspend fun updateApprovalRequest(request: ApprovalRequest, tenantContext: TenantContext)
    suspend fun getApprovalRequestById(approvalId: String, tenantContext: TenantContext): ApprovalRequest?
    suspend fun listPendingApprovals(limit: Int, tenantContext: TenantContext): List<ApprovalRequest>
    suspend fun recordDecision(decision: ApprovalDecision, tenantContext: TenantContext)
    suspend fun getDecisionsForApproval(approvalId: String, tenantContext: TenantContext): List<ApprovalDecision>
    suspend fun recordEscalation(escalation: ApprovalEscalation, tenantContext: TenantContext)
}

/**
 * PostgreSQL implementation of WorkflowApprovalRepository with strict RLS.
 */
class PostgresWorkflowApprovalRepository(
    private val transactionManager: TransactionManager
) : WorkflowApprovalRepository {

    override suspend fun savePolicy(policy: ApprovalPolicy, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_approval_policies (
                    policy_id, project_id, policy_name, required_role,
                    required_capability, minimum_approvals, allow_self_approval,
                    timeout_ms, escalation_role, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (project_id, policy_id) DO UPDATE SET
                    policy_name = EXCLUDED.policy_name,
                    required_role = EXCLUDED.required_role,
                    required_capability = EXCLUDED.required_capability,
                    minimum_approvals = EXCLUDED.minimum_approvals,
                    allow_self_approval = EXCLUDED.allow_self_approval,
                    timeout_ms = EXCLUDED.timeout_ms,
                    escalation_role = EXCLUDED.escalation_role;
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    policy.policyId,
                    tenantContext.projectId,
                    policy.policyName,
                    policy.requiredRole.name,
                    policy.requiredCapability,
                    policy.minimumApprovals,
                    policy.allowSelfApproval,
                    policy.timeoutMs,
                    policy.escalationRole?.name,
                    Timestamp(System.currentTimeMillis())
                )
            )
        }
    }

    override suspend fun getPolicyById(policyId: String, tenantContext: TenantContext): ApprovalPolicy? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT policy_id, project_id, policy_name, required_role,
                       required_capability, minimum_approvals, allow_self_approval,
                       timeout_ms, escalation_role
                FROM workflow_approval_policies
                WHERE project_id = ? AND policy_id = ?
            """.trimIndent()

            txContext.sqlExecutor.querySingleOrNull(
                sql = sql,
                params = listOf(tenantContext.projectId, policyId)
            ) { rs ->
                ApprovalPolicy(
                    policyId = rs.getString("policy_id"),
                    projectId = rs.getString("project_id"),
                    policyName = rs.getString("policy_name"),
                    requiredRole = UserRole.valueOf(rs.getString("required_role")),
                    requiredCapability = rs.getString("required_capability"),
                    minimumApprovals = rs.getInt("minimum_approvals"),
                    allowSelfApproval = rs.getBoolean("allow_self_approval"),
                    timeoutMs = rs.getLong("timeout_ms"),
                    escalationRole = rs.getString("escalation_role")?.let { UserRole.valueOf(it) }
                )
            }
        }
    }

    override suspend fun createApprovalRequest(request: ApprovalRequest, tenantContext: TenantContext): Boolean {
        return transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_approval_requests (
                    approval_id, project_id, workflow_id, step_id, policy_id,
                    requester_id, requester_role, requester_principal_type,
                    status, title, summary, payload_json, expires_at,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (project_id, approval_id) DO NOTHING;
            """.trimIndent()

            val rows = txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    request.approvalId,
                    tenantContext.projectId,
                    request.workflowId,
                    request.stepId,
                    request.policyId,
                    request.requesterId,
                    request.requesterRole.name,
                    request.requesterPrincipalType.name,
                    request.status.name,
                    request.title,
                    request.summary,
                    request.payloadJson,
                    Timestamp(request.expiresAt),
                    Timestamp(request.createdAt),
                    Timestamp(request.updatedAt)
                )
            )
            rows > 0
        }
    }

    override suspend fun updateApprovalRequest(request: ApprovalRequest, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE workflow_approval_requests SET
                    status = ?,
                    updated_at = ?
                WHERE project_id = ? AND approval_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    request.status.name,
                    Timestamp(request.updatedAt),
                    tenantContext.projectId,
                    request.approvalId
                )
            )
        }
    }

    override suspend fun getApprovalRequestById(approvalId: String, tenantContext: TenantContext): ApprovalRequest? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT approval_id, project_id, workflow_id, step_id, policy_id,
                       requester_id, requester_role, requester_principal_type,
                       status, title, summary, payload_json, expires_at,
                       created_at, updated_at
                FROM workflow_approval_requests
                WHERE project_id = ? AND approval_id = ?
            """.trimIndent()

            txContext.sqlExecutor.querySingleOrNull(
                sql = sql,
                params = listOf(tenantContext.projectId, approvalId)
            ) { rs ->
                mapRowToApprovalRequest(rs)
            }
        }
    }

    override suspend fun listPendingApprovals(limit: Int, tenantContext: TenantContext): List<ApprovalRequest> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT approval_id, project_id, workflow_id, step_id, policy_id,
                       requester_id, requester_role, requester_principal_type,
                       status, title, summary, payload_json, expires_at,
                       created_at, updated_at
                FROM workflow_approval_requests
                WHERE project_id = ? AND status IN ('PENDING', 'ESCALATED')
                ORDER BY created_at ASC
                LIMIT ?
            """.trimIndent()

            txContext.sqlExecutor.queryList(
                sql = sql,
                params = listOf(tenantContext.projectId, limit)
            ) { rs ->
                mapRowToApprovalRequest(rs)
            }
        }
    }

    override suspend fun recordDecision(decision: ApprovalDecision, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_approval_decisions (
                    decision_id, project_id, approval_id, approver_id,
                    approver_role, approver_principal_type, decision_type,
                    notes, human_confirmation_json, decided_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    decision.decisionId,
                    tenantContext.projectId,
                    decision.approvalId,
                    decision.approverId,
                    decision.approverRole.name,
                    decision.approverPrincipalType.name,
                    decision.decisionType.name,
                    decision.notes,
                    null,
                    Timestamp(decision.decidedAt)
                )
            )
        }
    }

    override suspend fun getDecisionsForApproval(
        approvalId: String,
        tenantContext: TenantContext
    ): List<ApprovalDecision> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT decision_id, project_id, approval_id, approver_id,
                       approver_role, approver_principal_type, decision_type,
                       notes, human_confirmation_json, decided_at
                FROM workflow_approval_decisions
                WHERE project_id = ? AND approval_id = ?
                ORDER BY decided_at ASC
            """.trimIndent()

            txContext.sqlExecutor.queryList(
                sql = sql,
                params = listOf(tenantContext.projectId, approvalId)
            ) { rs ->
                ApprovalDecision(
                    decisionId = rs.getString("decision_id"),
                    projectId = rs.getString("project_id"),
                    approvalId = rs.getString("approval_id"),
                    approverId = rs.getString("approver_id"),
                    approverRole = UserRole.valueOf(rs.getString("approver_role")),
                    approverPrincipalType = PrincipalType.valueOf(rs.getString("approver_principal_type")),
                    decisionType = ApprovalDecisionType.valueOf(rs.getString("decision_type")),
                    notes = rs.getString("notes"),
                    humanConfirmation = null,
                    decidedAt = rs.getTimestamp("decided_at")?.time ?: 0L
                )
            }
        }
    }

    override suspend fun recordEscalation(escalation: ApprovalEscalation, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO workflow_escalations (
                    escalation_id, project_id, approval_id, workflow_id,
                    from_role, to_role, reason, escalated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    escalation.escalationId,
                    tenantContext.projectId,
                    escalation.approvalId,
                    escalation.workflowId,
                    escalation.fromRole.name,
                    escalation.toRole.name,
                    escalation.reason,
                    Timestamp(escalation.escalatedAt)
                )
            )
        }
    }

    private fun mapRowToApprovalRequest(rs: ResultSet): ApprovalRequest {
        return ApprovalRequest(
            approvalId = rs.getString("approval_id"),
            projectId = rs.getString("project_id"),
            workflowId = rs.getString("workflow_id"),
            stepId = rs.getString("step_id"),
            policyId = rs.getString("policy_id"),
            requesterId = rs.getString("requester_id"),
            requesterRole = UserRole.valueOf(rs.getString("requester_role")),
            requesterPrincipalType = PrincipalType.valueOf(rs.getString("requester_principal_type")),
            status = ApprovalStatus.valueOf(rs.getString("status")),
            title = rs.getString("title"),
            summary = rs.getString("summary"),
            payloadJson = rs.getString("payload_json") ?: "{}",
            expiresAt = rs.getTimestamp("expires_at")?.time ?: 0L,
            createdAt = rs.getTimestamp("created_at")?.time ?: 0L,
            updatedAt = rs.getTimestamp("updated_at")?.time ?: 0L
        )
    }
}
