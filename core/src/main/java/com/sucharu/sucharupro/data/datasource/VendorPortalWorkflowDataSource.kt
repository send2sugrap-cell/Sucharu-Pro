package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorPortalWorkflowDataSource {
    // Workflows
    suspend fun saveWorkflow(workflow: VendorWorkflowItem): VendorWorkflowItem
    suspend fun updateWorkflow(workflow: VendorWorkflowItem): VendorWorkflowItem
    suspend fun findWorkflowById(tenantId: String, projectId: String, vendorId: String, workflowId: String): VendorWorkflowItem?
    suspend fun findWorkflowByCorrelationId(tenantId: String, projectId: String, vendorId: String, correlationId: String): VendorWorkflowItem?
    suspend fun listWorkflows(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorWorkflowStatus? = null,
        stage: VendorWorkflowStage? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<VendorWorkflowItem>

    // Events / Timeline
    suspend fun appendEvent(event: VendorWorkflowTimelineEvent): VendorWorkflowTimelineEvent
    suspend fun listEvents(tenantId: String, projectId: String, vendorId: String, workflowId: String): List<VendorWorkflowTimelineEvent>

    // Exceptions
    suspend fun saveException(exception: VendorWorkflowException): VendorWorkflowException
    suspend fun updateException(exception: VendorWorkflowException): VendorWorkflowException
    suspend fun findExceptionById(tenantId: String, projectId: String, vendorId: String, exceptionId: String): VendorWorkflowException?
    suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String? = null,
        status: VendorWorkflowExceptionStatus? = null
    ): List<VendorWorkflowException>

    // Next Actions
    suspend fun saveAction(action: VendorWorkflowNextAction): VendorWorkflowNextAction
    suspend fun updateAction(action: VendorWorkflowNextAction): VendorWorkflowNextAction
    suspend fun findActionById(tenantId: String, projectId: String, vendorId: String, actionId: String): VendorWorkflowNextAction?
    suspend fun listActions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String? = null,
        isCompleted: Boolean? = null
    ): List<VendorWorkflowNextAction>

    // Audits
    suspend fun appendAudit(audit: VendorWorkflowAuditEntry): VendorWorkflowAuditEntry
    suspend fun listAudits(tenantId: String, projectId: String, vendorId: String, workflowId: String): List<VendorWorkflowAuditEntry>
}
