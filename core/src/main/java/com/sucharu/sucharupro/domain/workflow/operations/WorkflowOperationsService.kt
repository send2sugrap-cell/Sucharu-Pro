package com.sucharu.sucharupro.domain.workflow.operations

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecisionType
import com.sucharu.sucharupro.domain.workflow.model.ApprovalRequest
import com.sucharu.sucharupro.domain.workflow.model.HumanConfirmationMetadata
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance

/**
 * Administrative and operations service interface for Workflow Orchestration (INFRA-04 Step 05).
 */
interface WorkflowOperationsService {
    suspend fun startWorkflow(
        definitionId: String,
        versionId: String,
        context: Map<String, String>,
        idempotencyKey: String?,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance

    suspend fun getWorkflowById(
        workflowId: String,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance?

    suspend fun pauseWorkflow(
        workflowId: String,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance

    suspend fun resumeWorkflow(
        workflowId: String,
        contextUpdates: Map<String, String>,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance

    suspend fun cancelWorkflow(
        workflowId: String,
        reason: String,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance

    suspend fun submitApprovalDecision(
        approvalId: String,
        decisionType: ApprovalDecisionType,
        notes: String?,
        humanConfirmation: HumanConfirmationMetadata?,
        principal: AuthenticatedPrincipal
    ): ApprovalRequest
}
