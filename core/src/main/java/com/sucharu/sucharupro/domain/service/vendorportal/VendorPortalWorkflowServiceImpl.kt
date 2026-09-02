package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.*
import java.util.UUID

/**
 * Production implementation of VendorPortalWorkflowService (Module 13 Step 11).
 */
class VendorPortalWorkflowServiceImpl(
    private val repository: VendorPortalWorkflowRepository,
    private val vendorRepository: VendorRepository,
    private val purchaseOrderRepository: VendorPurchaseOrderRepository? = null,
    private val workOrderRepository: VendorWorkOrderRepository? = null,
    private val deliveryRepository: VendorPortalDeliveryRepository? = null,
    private val invoiceRepository: VendorInvoiceRepository? = null,
    private val qualityRepository: VendorQualityRepository? = null,
    private val portalQualityRepository: VendorPortalQualityRepository? = null,
    private val settlementRepository: VendorSettlementRepository? = null,
    private val portalSettlementRepository: VendorPortalSettlementRepository? = null,
    private val notificationService: VendorPortalAnalyticsNotificationSearchService? = null
) : VendorPortalWorkflowService {

    // --- Validation Helper ---
    private suspend fun validateVendor(tenantId: String, projectId: String, vendorId: String): DomainResult<Unit> {
        val vendorRes = vendorRepository.findById(projectId, vendorId)
        val vendor = when (vendorRes) {
            is DomainResult.Success -> vendorRes.data ?: return DomainResult.Error(IllegalArgumentException("Vendor '$vendorId' not found"))
            is DomainResult.Error -> return DomainResult.Error(vendorRes.exception, vendorRes.message)
            DomainResult.Loading -> return DomainResult.Loading
        }
        if (vendor.projectId != projectId) {
            return DomainResult.Error(IllegalArgumentException("Vendor '$vendorId' does not belong to project '$projectId'"))
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun getWorkflowHubSummary(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorWorkflowHubSummary> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check

        val listRes = repository.listWorkflows(tenantId, projectId, vendorId, null, null, 500, 0)
        val allWorkflows = (listRes as? DomainResult.Success)?.data ?: emptyList()

        val active = allWorkflows.count { it.status == VendorWorkflowStatus.ACTIVE || it.status == VendorWorkflowStatus.PENDING_ACTION }
        val completed = allWorkflows.count { it.status == VendorWorkflowStatus.COMPLETED }
        val blocked = allWorkflows.count { it.status == VendorWorkflowStatus.BLOCKED || it.status == VendorWorkflowStatus.EXCEPTION }
        val overdue = allWorkflows.count { it.slaStatus == VendorWorkflowSlaStatus.OVERDUE }

        val stageBreakdown = allWorkflows.groupBy { it.currentStage.name }.mapValues { it.value.size }

        val completedItems = allWorkflows.filter { it.completedAt != null && it.completedAt > it.startedAt }
        val avgCycleDays = if (completedItems.isNotEmpty()) {
            val totalDays = completedItems.sumOf { (it.completedAt!! - it.startedAt).toDouble() / 86400000.0 }
            Math.round((totalDays / completedItems.size) * 10.0) / 10.0
        } else 0.0

        val actionsRes = repository.listActions(tenantId, projectId, vendorId, null, false)
        val urgentActions = (actionsRes as? DomainResult.Success)?.data?.take(5) ?: emptyList()

        return DomainResult.Success(
            VendorWorkflowHubSummary(
                vendorId = vendorId,
                tenantId = tenantId,
                projectId = projectId,
                totalActiveWorkflows = active,
                completedWorkflows = completed,
                blockedWorkflows = blocked,
                overdueWorkflows = overdue,
                averageCycleTimeDays = avgCycleDays,
                stageBreakdown = stageBreakdown,
                recentWorkflows = allWorkflows.take(10),
                urgentActions = urgentActions
            )
        )
    }

    override suspend fun listWorkflows(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorWorkflowStatus?,
        stage: VendorWorkflowStage?,
        limit: Int,
        offset: Int
    ): DomainResult<List<VendorWorkflowItem>> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check
        return repository.listWorkflows(tenantId, projectId, vendorId, status, stage, limit, offset)
    }

    override suspend fun getWorkflowDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<VendorWorkflowItem> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check

        val res = repository.findWorkflowById(tenantId, projectId, vendorId, workflowId)
        val item = when (res) {
            is DomainResult.Success -> res.data ?: return DomainResult.Error(IllegalArgumentException("Workflow '$workflowId' not found"))
            is DomainResult.Error -> return DomainResult.Error(res.exception, res.message)
            DomainResult.Loading -> return DomainResult.Loading
        }
        return DomainResult.Success(item)
    }

    override suspend fun getWorkflowTimeline(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<List<VendorWorkflowTimelineEvent>> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check
        return repository.listEvents(tenantId, projectId, vendorId, workflowId)
    }

    override suspend fun getWorkflowExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String?,
        status: VendorWorkflowExceptionStatus?
    ): DomainResult<List<VendorWorkflowException>> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check
        return repository.listExceptions(tenantId, projectId, vendorId, workflowId, status)
    }

    override suspend fun getWorkflowNextActions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String?,
        userRole: String?
    ): DomainResult<List<VendorWorkflowNextAction>> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check

        val res = repository.listActions(tenantId, projectId, vendorId, workflowId, false)
        val actions = (res as? DomainResult.Success)?.data ?: emptyList()

        val filtered = if (userRole != null && userRole != "VENDOR_ADMIN") {
            actions.filter { it.requiredRole == userRole || it.requiredRole == "ANY" || it.requiredRole == "VENDOR" }
        } else actions

        return DomainResult.Success(filtered)
    }

    override suspend fun getWorkflowSlaProjection(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String
    ): DomainResult<VendorWorkflowSlaProjection> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check

        val workflowRes = repository.findWorkflowById(tenantId, projectId, vendorId, workflowId)
        val workflow = (workflowRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(IllegalArgumentException("Workflow '$workflowId' not found"))

        val deadline = workflow.targetDeliveryAt ?: (workflow.startedAt + 86400000L * 7) // 7 days default
        val projection = VendorWorkflowSlaProjection.calculate(
            workflowId = workflowId,
            milestoneTitle = "Delivery / Fulfillment for ${workflow.workflowTitle}",
            deadline = deadline
        )
        return DomainResult.Success(projection)
    }

    override suspend fun acknowledgeWorkflowAction(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String,
        actionId: String,
        actorId: String
    ): DomainResult<VendorWorkflowNextAction> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check

        val actionRes = repository.findActionById(tenantId, projectId, vendorId, actionId)
        val action = (actionRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(IllegalArgumentException("Action '$actionId' not found"))

        if (action.isCompleted) {
            return DomainResult.Success(action)
        }

        val completed = action.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            completedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        val updateRes = repository.updateAction(completed)

        // Record Timeline Event
        repository.appendEvent(
            VendorWorkflowTimelineEvent(
                eventId = "EVT-${UUID.randomUUID()}",
                workflowId = workflowId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                correlationId = action.workflowId,
                stage = VendorWorkflowStage.PRODUCTION_IN_PROGRESS,
                eventType = "ACTION_COMPLETED",
                title = "Action Completed: ${action.title}",
                description = "Completed by $actorId",
                sourceModule = "WORKFLOW",
                actorId = actorId,
                actorType = "VENDOR",
                occurredAt = System.currentTimeMillis()
            )
        )

        // Record Audit
        repository.appendAudit(
            VendorWorkflowAuditEntry(
                auditId = "AUD-${UUID.randomUUID()}",
                workflowId = workflowId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                actorId = actorId,
                actorRole = "VENDOR",
                action = "ACKNOWLEDGE_ACTION",
                entityType = "WORKFLOW_ACTION",
                entityId = actionId,
                correlationId = action.workflowId,
                occurredAt = System.currentTimeMillis()
            )
        )

        return updateRes
    }

    override suspend fun recordWorkflowException(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workflowId: String,
        category: String,
        severity: VendorWorkflowPriority,
        title: String,
        description: String,
        actorId: String
    ): DomainResult<VendorWorkflowException> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check

        val exception = VendorWorkflowException(
            exceptionId = "EXC-${UUID.randomUUID()}",
            workflowId = workflowId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            category = category,
            severity = severity,
            status = VendorWorkflowExceptionStatus.OPEN,
            title = title,
            description = description,
            detectedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val saveRes = repository.saveException(exception)

        // Emit Notification
        notificationService?.emitNotification(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            category = VendorPortalNotificationCategory.SYSTEM,
            severity = when (severity) {
                VendorWorkflowPriority.CRITICAL -> VendorPortalNotificationSeverity.CRITICAL
                VendorWorkflowPriority.URGENT -> VendorPortalNotificationSeverity.URGENT
                VendorWorkflowPriority.HIGH -> VendorPortalNotificationSeverity.HIGH
                else -> VendorPortalNotificationSeverity.NORMAL
            },
            title = "Workflow Exception: $title",
            message = description,
            relatedEntityType = "WORKFLOW_EXCEPTION",
            relatedEntityId = exception.exceptionId,
            deepLinkTarget = "/vendor-portal/workflows/$workflowId/exceptions",
            idempotencyKey = "NOTIF-EXC-${exception.exceptionId}"
        )

        // Record Audit
        repository.appendAudit(
            VendorWorkflowAuditEntry(
                auditId = "AUD-${UUID.randomUUID()}",
                workflowId = workflowId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                actorId = actorId,
                actorRole = "VENDOR",
                action = "RECORD_EXCEPTION",
                entityType = "WORKFLOW_EXCEPTION",
                entityId = exception.exceptionId,
                occurredAt = System.currentTimeMillis()
            )
        )

        return saveRes
    }

    override suspend fun resolveWorkflowException(
        tenantId: String,
        projectId: String,
        vendorId: String,
        exceptionId: String,
        resolutionNotes: String,
        actorId: String
    ): DomainResult<VendorWorkflowException> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check

        val exceptionRes = repository.findExceptionById(tenantId, projectId, vendorId, exceptionId)
        val exception = (exceptionRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(IllegalArgumentException("Exception '$exceptionId' not found"))

        val resolved = exception.copy(
            status = VendorWorkflowExceptionStatus.RESOLVED,
            resolvedAt = System.currentTimeMillis(),
            resolvedBy = actorId,
            resolutionNotes = resolutionNotes,
            updatedAt = System.currentTimeMillis()
        )

        return repository.updateException(resolved)
    }

    override suspend fun synchronizeWorkflowFromModule12(
        tenantId: String,
        projectId: String,
        vendorId: String,
        correlationId: String
    ): DomainResult<VendorWorkflowItem> {
        val check = validateVendor(tenantId, projectId, vendorId)
        if (check is DomainResult.Error) return check

        val existingRes = repository.findWorkflowByCorrelationId(tenantId, projectId, vendorId, correlationId)
        val existing = (existingRes as? DomainResult.Success)?.data

        // Probe Module 12 entities
        val posRes = purchaseOrderRepository?.list(projectId = projectId, vendorId = vendorId)
        val pos = (posRes as? DomainResult.Success)?.data ?: emptyList()
        val matchedPo = pos.firstOrNull { it.purchaseOrderId == correlationId || it.orderNumber == correlationId }

        val wosRes = workOrderRepository?.list(projectId = projectId, vendorId = vendorId)
        val wos = (wosRes as? DomainResult.Success)?.data ?: emptyList()
        val matchedWo = wos.firstOrNull { it.workOrderId == correlationId || it.workOrderNumber == correlationId }

        val currentStage = when {
            matchedPo != null && matchedPo.status.name in listOf("COMPLETED", "CLOSED", "FULFILLED") -> VendorWorkflowStage.COMPLETED
            matchedPo != null && matchedPo.status.name == "IN_PROGRESS" -> VendorWorkflowStage.PRODUCTION_IN_PROGRESS
            matchedPo != null && matchedPo.status.name == "CONFIRMED" -> VendorWorkflowStage.PO_ACKNOWLEDGED
            matchedPo != null -> VendorWorkflowStage.AWARDED
            matchedWo != null && matchedWo.status.name in listOf("COMPLETED", "CLOSED") -> VendorWorkflowStage.COMPLETED
            matchedWo != null && matchedWo.status.name == "IN_PROGRESS" -> VendorWorkflowStage.PRODUCTION_IN_PROGRESS
            matchedWo != null -> VendorWorkflowStage.WORK_ORDER_ACKNOWLEDGED
            else -> VendorWorkflowStage.RFQ_RECEIVED
        }

        val workflowId = existing?.workflowId ?: "WF-${UUID.randomUUID()}"
        val item = VendorWorkflowItem(
            workflowId = workflowId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            correlationId = correlationId,
            workflowTitle = matchedPo?.let { "Commercial Order #${it.orderNumber}" }
                ?: matchedWo?.let { "Service Work Order #${it.workOrderNumber}" }
                ?: "Procurement Cycle for $correlationId",
            currentStage = currentStage,
            status = if (currentStage == VendorWorkflowStage.COMPLETED) VendorWorkflowStatus.COMPLETED else VendorWorkflowStatus.ACTIVE,
            slaStatus = VendorWorkflowSlaStatus.ON_TRACK,
            purchaseOrderId = matchedPo?.purchaseOrderId,
            workOrderId = matchedWo?.workOrderId,
            startedAt = existing?.startedAt ?: System.currentTimeMillis(),
            completedAt = if (currentStage == VendorWorkflowStage.COMPLETED) System.currentTimeMillis() else null,
            targetDeliveryAt = matchedPo?.expectedDeliveryDate ?: (System.currentTimeMillis() + 86400000L * 7),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return repository.saveWorkflow(item)
    }
}
