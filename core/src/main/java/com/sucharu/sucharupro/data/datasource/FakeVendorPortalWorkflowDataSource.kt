package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class FakeVendorPortalWorkflowDataSource : VendorPortalWorkflowDataSource {

    private val workflows = ConcurrentHashMap<String, VendorWorkflowItem>()
    private val events = ConcurrentHashMap<String, CopyOnWriteArrayList<VendorWorkflowTimelineEvent>>()
    private val exceptions = ConcurrentHashMap<String, VendorWorkflowException>()
    private val actions = ConcurrentHashMap<String, VendorWorkflowNextAction>()
    private val audits = ConcurrentHashMap<String, CopyOnWriteArrayList<VendorWorkflowAuditEntry>>()

    override suspend fun saveWorkflow(workflow: VendorWorkflowItem): VendorWorkflowItem {
        workflows[workflow.workflowId] = workflow
        return workflow
    }

    override suspend fun updateWorkflow(workflow: VendorWorkflowItem): VendorWorkflowItem {
        val updated = workflow.copy(version = workflow.version + 1, updatedAt = System.currentTimeMillis())
        workflows[workflow.workflowId] = updated
        return updated
    }

    override suspend fun findWorkflowById(tenantId: String, projectId: String, vendorId: String, workflowId: String): VendorWorkflowItem? {
        val w = workflows[workflowId] ?: return null
        return if (w.tenantId == tenantId && w.projectId == projectId && w.vendorId == vendorId) w else null
    }

    override suspend fun findWorkflowByCorrelationId(tenantId: String, projectId: String, vendorId: String, correlationId: String): VendorWorkflowItem? {
        return workflows.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId && it.correlationId == correlationId
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
    ): List<VendorWorkflowItem> {
        return workflows.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { status == null || it.status == status }
            .filter { stage == null || it.currentStage == stage }
            .sortedByDescending { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun appendEvent(event: VendorWorkflowTimelineEvent): VendorWorkflowTimelineEvent {
        events.computeIfAbsent(event.workflowId) { CopyOnWriteArrayList() }.add(event)
        return event
    }

    override suspend fun listEvents(tenantId: String, projectId: String, vendorId: String, workflowId: String): List<VendorWorkflowTimelineEvent> {
        return (events[workflowId] ?: emptyList())
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .sortedBy { it.occurredAt }
    }

    override suspend fun saveException(exception: VendorWorkflowException): VendorWorkflowException {
        exceptions[exception.exceptionId] = exception
        return exception
    }

    override suspend fun updateException(exception: VendorWorkflowException): VendorWorkflowException {
        val updated = exception.copy(version = exception.version + 1, updatedAt = System.currentTimeMillis())
        exceptions[exception.exceptionId] = updated
        return updated
    }

    override suspend fun findExceptionById(tenantId: String, projectId: String, vendorId: String, exceptionId: String): VendorWorkflowException? {
        val e = exceptions[exceptionId] ?: return null
        return if (e.tenantId == tenantId && e.projectId == projectId && e.vendorId == vendorId) e else null
    }

    override suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String?,
        status: VendorWorkflowExceptionStatus?
    ): List<VendorWorkflowException> {
        return exceptions.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { workflowId == null || it.workflowId == workflowId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.detectedAt }
    }

    override suspend fun saveAction(action: VendorWorkflowNextAction): VendorWorkflowNextAction {
        actions[action.actionId] = action
        return action
    }

    override suspend fun updateAction(action: VendorWorkflowNextAction): VendorWorkflowNextAction {
        val updated = action.copy(updatedAt = System.currentTimeMillis())
        actions[action.actionId] = updated
        return updated
    }

    override suspend fun findActionById(tenantId: String, projectId: String, vendorId: String, actionId: String): VendorWorkflowNextAction? {
        val a = actions[actionId] ?: return null
        return if (a.tenantId == tenantId && a.projectId == projectId && a.vendorId == vendorId) a else null
    }

    override suspend fun listActions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String?,
        isCompleted: Boolean?
    ): List<VendorWorkflowNextAction> {
        return actions.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { workflowId == null || it.workflowId == workflowId }
            .filter { isCompleted == null || it.isCompleted == isCompleted }
            .sortedBy { it.dueAt ?: Long.MAX_VALUE }
    }

    override suspend fun appendAudit(audit: VendorWorkflowAuditEntry): VendorWorkflowAuditEntry {
        audits.computeIfAbsent(audit.workflowId) { CopyOnWriteArrayList() }.add(audit)
        return audit
    }

    override suspend fun listAudits(tenantId: String, projectId: String, vendorId: String, workflowId: String): List<VendorWorkflowAuditEntry> {
        return (audits[workflowId] ?: emptyList())
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .sortedByDescending { it.occurredAt }
    }
}
