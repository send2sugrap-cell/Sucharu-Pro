package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPortalWorkflowDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalWorkflowRepository

class VendorPortalWorkflowRepositoryImpl(
    private val dataSource: VendorPortalWorkflowDataSource
) : VendorPortalWorkflowRepository {

    override suspend fun saveWorkflow(workflow: VendorWorkflowItem): DomainResult<VendorWorkflowItem> =
        try {
            DomainResult.Success(dataSource.saveWorkflow(workflow))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to save workflow '${workflow.workflowId}'")
        }

    override suspend fun updateWorkflow(workflow: VendorWorkflowItem): DomainResult<VendorWorkflowItem> =
        try {
            DomainResult.Success(dataSource.updateWorkflow(workflow))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to update workflow '${workflow.workflowId}'")
        }

    override suspend fun findWorkflowById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<VendorWorkflowItem?> =
        try {
            DomainResult.Success(dataSource.findWorkflowById(tenantId, projectId, vendorId, workflowId))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to find workflow '$workflowId'")
        }

    override suspend fun findWorkflowByCorrelationId(
        tenantId: String,
        projectId: String,
        vendorId: String,
        correlationId: String
    ): DomainResult<VendorWorkflowItem?> =
        try {
            DomainResult.Success(dataSource.findWorkflowByCorrelationId(tenantId, projectId, vendorId, correlationId))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to find workflow with correlation '$correlationId'")
        }

    override suspend fun listWorkflows(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorWorkflowStatus?,
        stage: VendorWorkflowStage?,
        limit: Int,
        offset: Int
    ): DomainResult<List<VendorWorkflowItem>> =
        try {
            DomainResult.Success(dataSource.listWorkflows(tenantId, projectId, vendorId, status, stage, limit, offset))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to list workflows")
        }

    override suspend fun appendEvent(event: VendorWorkflowTimelineEvent): DomainResult<VendorWorkflowTimelineEvent> =
        try {
            DomainResult.Success(dataSource.appendEvent(event))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to append workflow event")
        }

    override suspend fun listEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<List<VendorWorkflowTimelineEvent>> =
        try {
            DomainResult.Success(dataSource.listEvents(tenantId, projectId, vendorId, workflowId))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to list workflow events")
        }

    override suspend fun saveException(exception: VendorWorkflowException): DomainResult<VendorWorkflowException> =
        try {
            DomainResult.Success(dataSource.saveException(exception))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to save workflow exception")
        }

    override suspend fun updateException(exception: VendorWorkflowException): DomainResult<VendorWorkflowException> =
        try {
            DomainResult.Success(dataSource.updateException(exception))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to update workflow exception")
        }

    override suspend fun findExceptionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        exceptionId: String
    ): DomainResult<VendorWorkflowException?> =
        try {
            DomainResult.Success(dataSource.findExceptionById(tenantId, projectId, vendorId, exceptionId))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to find exception '$exceptionId'")
        }

    override suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String?,
        status: VendorWorkflowExceptionStatus?
    ): DomainResult<List<VendorWorkflowException>> =
        try {
            DomainResult.Success(dataSource.listExceptions(tenantId, projectId, vendorId, workflowId, status))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to list workflow exceptions")
        }

    override suspend fun saveAction(action: VendorWorkflowNextAction): DomainResult<VendorWorkflowNextAction> =
        try {
            DomainResult.Success(dataSource.saveAction(action))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to save workflow action")
        }

    override suspend fun updateAction(action: VendorWorkflowNextAction): DomainResult<VendorWorkflowNextAction> =
        try {
            DomainResult.Success(dataSource.updateAction(action))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to update workflow action")
        }

    override suspend fun findActionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String
    ): DomainResult<VendorWorkflowNextAction?> =
        try {
            DomainResult.Success(dataSource.findActionById(tenantId, projectId, vendorId, actionId))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to find action '$actionId'")
        }

    override suspend fun listActions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String?,
        isCompleted: Boolean?
    ): DomainResult<List<VendorWorkflowNextAction>> =
        try {
            DomainResult.Success(dataSource.listActions(tenantId, projectId, vendorId, workflowId, isCompleted))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to list workflow actions")
        }

    override suspend fun appendAudit(audit: VendorWorkflowAuditEntry): DomainResult<VendorWorkflowAuditEntry> =
        try {
            DomainResult.Success(dataSource.appendAudit(audit))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to append audit entry")
        }

    override suspend fun listAudits(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<List<VendorWorkflowAuditEntry>> =
        try {
            DomainResult.Success(dataSource.listAudits(tenantId, projectId, vendorId, workflowId))
        } catch (e: Exception) {
            DomainResult.Error(e, "Failed to list audit entries")
        }
}
