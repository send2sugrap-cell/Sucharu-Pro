package com.sucharu.sucharupro.data.workflow.operations

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.workflow.observability.WorkflowAuditLogger
import com.sucharu.sucharupro.data.workflow.observability.WorkflowMetrics
import com.sucharu.sucharupro.data.workflow.postgres.WorkflowApprovalRepository
import com.sucharu.sucharupro.data.workflow.postgres.WorkflowDefinitionRepository
import com.sucharu.sucharupro.data.workflow.postgres.WorkflowInstanceRepository
import com.sucharu.sucharupro.domain.workflow.approval.ApprovalEngine
import com.sucharu.sucharupro.domain.workflow.approval.ApprovalEvaluationResult
import com.sucharu.sucharupro.domain.workflow.engine.WorkflowOrchestrator
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecisionType
import com.sucharu.sucharupro.domain.workflow.model.ApprovalRequest
import com.sucharu.sucharupro.domain.workflow.model.HumanConfirmationMetadata
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus
import com.sucharu.sucharupro.domain.workflow.operations.WorkflowOperationsService
import java.util.UUID

/**
 * Concrete operations service for Workflow and Approval orchestration (INFRA-04 Step 05).
 */
class DefaultWorkflowOperationsService(
    private val definitionRepository: WorkflowDefinitionRepository,
    private val instanceRepository: WorkflowInstanceRepository,
    private val approvalRepository: WorkflowApprovalRepository,
    private val orchestrator: WorkflowOrchestrator = WorkflowOrchestrator(),
    private val approvalEngine: ApprovalEngine = ApprovalEngine(),
    private val auditLogger: WorkflowAuditLogger = WorkflowAuditLogger(),
    private val metrics: WorkflowMetrics = WorkflowMetrics()
) : WorkflowOperationsService {

    override suspend fun startWorkflow(
        definitionId: String,
        versionId: String,
        context: Map<String, String>,
        idempotencyKey: String?,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        val tenant = TenantContext(principal.projectId)
        val version = definitionRepository.getVersion(definitionId, versionId, tenant)
            ?: throw IllegalArgumentException("Workflow definition '$definitionId' version '$versionId' not found.")

        val workflowId = UUID.randomUUID().toString()
        val instance = WorkflowInstance(
            workflowId = workflowId,
            projectId = principal.projectId,
            definitionId = definitionId,
            versionId = versionId,
            executionId = UUID.randomUUID().toString(),
            context = context,
            actorType = principal.principalType,
            actorId = principal.userId,
            principalType = principal.principalType,
            idempotencyKey = idempotencyKey
        )

        val created = instanceRepository.createInstance(instance, tenant)
        if (!created && idempotencyKey != null) {
            // Already created
            return instanceRepository.getInstanceById(workflowId, tenant) ?: instance
        }

        val (startedInstance, _) = orchestrator.startWorkflow(instance, version)
        instanceRepository.updateInstance(startedInstance, tenant)
        metrics.recordWorkflowStarted(principal.projectId)

        return startedInstance
    }

    override suspend fun getWorkflowById(
        workflowId: String,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance? {
        val tenant = TenantContext(principal.projectId)
        return instanceRepository.getInstanceById(workflowId, tenant)
    }

    override suspend fun pauseWorkflow(
        workflowId: String,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        val tenant = TenantContext(principal.projectId)
        val instance = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw IllegalArgumentException("Workflow '$workflowId' not found.")

        val updated = instance.copy(status = WorkflowStatus.PAUSED, updatedAt = System.currentTimeMillis())
        instanceRepository.updateInstance(updated, tenant)
        return updated
    }

    override suspend fun resumeWorkflow(
        workflowId: String,
        contextUpdates: Map<String, String>,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        val tenant = TenantContext(principal.projectId)
        val instance = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw IllegalArgumentException("Workflow '$workflowId' not found.")

        val version = definitionRepository.getVersion(instance.definitionId, instance.versionId, tenant)
            ?: throw IllegalStateException("Workflow version not found")

        val resumed = orchestrator.resumeWorkflow(instance, version, contextUpdates)
        instanceRepository.updateInstance(resumed, tenant)
        return resumed
    }

    override suspend fun cancelWorkflow(
        workflowId: String,
        reason: String,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        val tenant = TenantContext(principal.projectId)
        val instance = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw IllegalArgumentException("Workflow '$workflowId' not found.")

        val cancelled = orchestrator.cancelWorkflow(instance, principal.userId, reason)
        instanceRepository.updateInstance(cancelled, tenant)
        return cancelled
    }

    override suspend fun submitApprovalDecision(
        approvalId: String,
        decisionType: ApprovalDecisionType,
        notes: String?,
        humanConfirmation: HumanConfirmationMetadata?,
        principal: AuthenticatedPrincipal
    ): ApprovalRequest {
        val tenant = TenantContext(principal.projectId)
        val request = approvalRepository.getApprovalRequestById(approvalId, tenant)
            ?: throw IllegalArgumentException("Approval request '$approvalId' not found.")

        val policy = approvalRepository.getPolicyById(request.policyId, tenant)
            ?: throw IllegalStateException("Approval policy '${request.policyId}' not found.")

        val existingDecisions = approvalRepository.getDecisionsForApproval(approvalId, tenant)

        val evalResult = approvalEngine.processDecision(
            request = request,
            policy = policy,
            principal = principal,
            decisionType = decisionType,
            existingDecisions = existingDecisions,
            notes = notes,
            humanConfirmation = humanConfirmation
        )

        return when (evalResult) {
            is ApprovalEvaluationResult.Decided -> {
                approvalRepository.recordDecision(evalResult.decision, tenant)
                approvalRepository.updateApprovalRequest(evalResult.updatedRequest, tenant)
                auditLogger.logApproval(approvalId, principal.projectId, principal.userId, decisionType.name, notes)
                metrics.recordApprovalDecided()

                // Resume workflow
                val instance = instanceRepository.getInstanceById(request.workflowId, tenant)
                val version = instance?.let { definitionRepository.getVersion(it.definitionId, it.versionId, tenant) }
                if (instance != null && version != null) {
                    val resumed = orchestrator.resumeWorkflow(instance, version, mapOf("approvalDecision" to "APPROVED"))
                    instanceRepository.updateInstance(resumed, tenant)
                }

                evalResult.updatedRequest
            }
            is ApprovalEvaluationResult.Rejected -> {
                approvalRepository.recordDecision(evalResult.decision, tenant)
                approvalRepository.updateApprovalRequest(evalResult.updatedRequest, tenant)
                auditLogger.logApproval(approvalId, principal.projectId, principal.userId, "REJECTED", notes)
                metrics.recordApprovalDecided()

                // Fail workflow
                val instance = instanceRepository.getInstanceById(request.workflowId, tenant)
                val version = instance?.let { definitionRepository.getVersion(it.definitionId, it.versionId, tenant) }
                if (instance != null && version != null) {
                    val failedResult = orchestrator.failWorkflowWithCompensation(
                        instance = instance,
                        stepDefinitions = version.steps,
                        stepExecutions = emptyList(),
                        errorMessage = "Approval request '$approvalId' was rejected by ${principal.username}"
                    )
                    instanceRepository.updateInstance(failedResult.instance, tenant)
                }

                evalResult.updatedRequest
            }
            is ApprovalEvaluationResult.Escalated -> {
                approvalRepository.recordEscalation(evalResult.escalation, tenant)
                val escalatedRequest = request.copy(
                    status = com.sucharu.sucharupro.domain.workflow.model.ApprovalStatus.ESCALATED,
                    updatedAt = System.currentTimeMillis()
                )
                approvalRepository.updateApprovalRequest(escalatedRequest, tenant)
                escalatedRequest
            }
            is ApprovalEvaluationResult.RequiresMoreApprovals -> {
                request
            }
            is ApprovalEvaluationResult.Denied -> {
                throw SecurityException(evalResult.reason)
            }
        }
    }
}
