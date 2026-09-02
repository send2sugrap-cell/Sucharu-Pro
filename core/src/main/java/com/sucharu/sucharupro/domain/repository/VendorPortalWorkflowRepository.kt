package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Domain Repository interface for End-to-End Vendor Workflow Orchestration (Module 13 Step 11).
 */
interface VendorPortalWorkflowRepository {
    // Workflows
    suspend fun saveWorkflow(workflow: VendorWorkflowItem): DomainResult<VendorWorkflowItem>
    suspend fun updateWorkflow(workflow: VendorWorkflowItem): DomainResult<VendorWorkflowItem>
    suspend fun findWorkflowById(tenantId: String, projectId: String, vendorId: String, workflowId: String): DomainResult<VendorWorkflowItem?>
    suspend fun findWorkflowByCorrelationId(tenantId: String, projectId: String, vendorId: String, correlationId: String): DomainResult<VendorWorkflowItem?>
    suspend fun listWorkflows(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorWorkflowStatus? = null,
        stage: VendorWorkflowStage? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<VendorWorkflowItem>>

    // Events / Timeline
    suspend fun appendEvent(event: VendorWorkflowTimelineEvent): DomainResult<VendorWorkflowTimelineEvent>
    suspend fun listEvents(tenantId: String, projectId: String, vendorId: String, workflowId: String): DomainResult<List<VendorWorkflowTimelineEvent>>

    // Exceptions
    suspend fun saveException(exception: VendorWorkflowException): DomainResult<VendorWorkflowException>
    suspend fun updateException(exception: VendorWorkflowException): DomainResult<VendorWorkflowException>
    suspend fun findExceptionById(tenantId: String, projectId: String, vendorId: String, exceptionId: String): DomainResult<VendorWorkflowException?>
    suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String? = null,
        status: VendorWorkflowExceptionStatus? = null
    ): DomainResult<List<VendorWorkflowException>>

    // Next Actions
    suspend fun saveAction(action: VendorWorkflowNextAction): DomainResult<VendorWorkflowNextAction>
    suspend fun updateAction(action: VendorWorkflowNextAction): DomainResult<VendorWorkflowNextAction>
    suspend fun findActionById(tenantId: String, projectId: String, vendorId: String, actionId: String): DomainResult<VendorWorkflowNextAction?>
    suspend fun listActions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String? = null,
        isCompleted: Boolean? = null
    ): DomainResult<List<VendorWorkflowNextAction>>

    // Audits
    suspend fun appendAudit(audit: VendorWorkflowAuditEntry): DomainResult<VendorWorkflowAuditEntry>
    suspend fun listAudits(tenantId: String, projectId: String, vendorId: String, workflowId: String): DomainResult<List<VendorWorkflowAuditEntry>>
}
