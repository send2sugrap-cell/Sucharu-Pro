package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorCollaborationRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderService
import com.sucharu.sucharupro.domain.service.vendor.VendorWorkOrderService
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorCollaborationValidator
import java.math.BigDecimal
import java.util.UUID

class VendorPortalCollaborationServiceImpl(
    private val collaborationRepository: VendorCollaborationRepository,
    private val vendorPurchaseOrderService: VendorPurchaseOrderService,
    private val vendorWorkOrderService: VendorWorkOrderService,
    private val vendorRepository: VendorRepository
) : VendorPortalCollaborationService {

    override suspend fun listPurchaseOrders(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPurchaseOrderStatus?
    ): DomainResult<List<VendorPortalPurchaseOrderSummary>> {
        val ordersResult = vendorPurchaseOrderService.listOrders(
            projectId = projectId,
            vendorId = vendorId,
            status = status
        )
        if (ordersResult is DomainResult.Error) return ordersResult
        val orders = (ordersResult as DomainResult.Success).data

        val summaries = orders.map { po ->
            val ack = (collaborationRepository.findPoAcknowledgement(po.purchaseOrderId, tenantId) as? DomainResult.Success)?.data
            val blockers = (collaborationRepository.listBlockers(tenantId, projectId, vendorId, null, VendorBlockerStatus.OPEN) as? DomainResult.Success)?.data ?: emptyList()
            val poBlockersCount = blockers.count { it.purchaseOrderId == po.purchaseOrderId }

            VendorPortalPurchaseOrderSummary(
                purchaseOrderId = po.purchaseOrderId,
                orderNumber = po.orderNumber,
                vendorId = po.vendorId,
                status = po.status,
                orderDate = po.orderDate,
                expectedDeliveryDate = po.expectedDeliveryDate,
                deliveryLocation = po.deliveryLocation,
                currency = po.currency,
                totalAmount = po.totalAmount,
                acknowledgementStatus = ack?.acknowledgementType,
                acknowledgedAt = ack?.acknowledgedAt,
                activeWorkOrdersCount = 0,
                openBlockersCount = poBlockersCount
            )
        }
        return DomainResult.Success(summaries)
    }

    override suspend fun getPurchaseOrderDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String
    ): DomainResult<VendorPortalPurchaseOrderDetails> {
        val poResult = vendorPurchaseOrderService.getOrderById(projectId, purchaseOrderId)
        if (poResult is DomainResult.Error) return poResult
        val po = (poResult as DomainResult.Success).data

        if (po.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to purchase order $purchaseOrderId for vendor $vendorId"))
        }

        val ack = (collaborationRepository.findPoAcknowledgement(purchaseOrderId, tenantId) as? DomainResult.Success)?.data
        val blockers = (collaborationRepository.listBlockers(tenantId, projectId, vendorId, null, VendorBlockerStatus.OPEN) as? DomainResult.Success)?.data ?: emptyList()
        val poBlockers = blockers.filter { it.purchaseOrderId == purchaseOrderId }
        val evidence = (collaborationRepository.listEvidence(VendorThreadResourceType.PURCHASE_ORDER, purchaseOrderId, tenantId) as? DomainResult.Success)?.data ?: emptyList()

        return DomainResult.Success(
            VendorPortalPurchaseOrderDetails(
                purchaseOrderId = po.purchaseOrderId,
                orderNumber = po.orderNumber,
                vendorId = po.vendorId,
                status = po.status,
                orderDate = po.orderDate,
                expectedDeliveryDate = po.expectedDeliveryDate,
                deliveryLocation = po.deliveryLocation,
                currency = po.currency,
                subtotal = po.subtotal,
                taxAmount = po.taxAmount,
                discountAmount = po.discountAmount,
                totalAmount = po.totalAmount,
                notes = po.notes,
                items = po.items,
                acknowledgement = ack,
                openBlockers = poBlockers,
                evidenceList = evidence
            )
        )
    }

    override suspend fun acknowledgePurchaseOrder(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String,
        ackType: VendorPoAcknowledgementType,
        exceptionDetails: String?,
        declineReason: String?,
        promisedDeliveryDate: Long?,
        comment: String?,
        actorId: String
    ): DomainResult<VendorPoAcknowledgement> {
        val poResult = vendorPurchaseOrderService.getOrderById(projectId, purchaseOrderId)
        if (poResult is DomainResult.Error) return poResult
        val po = (poResult as DomainResult.Success).data

        if (po.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to acknowledge purchase order for different vendor."))
        }

        val ack = VendorPoAcknowledgement(
            acknowledgementId = UUID.randomUUID().toString(),
            purchaseOrderId = purchaseOrderId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            actorId = actorId,
            acknowledgementType = ackType,
            exceptionDetails = exceptionDetails,
            declineReason = declineReason,
            promisedDeliveryDate = promisedDeliveryDate,
            comment = comment,
            acknowledgedAt = System.currentTimeMillis()
        )
        VendorCollaborationValidator.validatePoAcknowledgement(ack)

        val saveRes = collaborationRepository.savePoAcknowledgement(ack)
        if (saveRes is DomainResult.Error) return saveRes

        // If acknowledged fully and PO is in ISSUED status, update Module 12 canonical status
        if (ackType == VendorPoAcknowledgementType.ACKNOWLEDGED && po.status == VendorPurchaseOrderStatus.ISSUED) {
            vendorPurchaseOrderService.acknowledgeOrder(projectId, purchaseOrderId, actorId)
        }

        val eventType = when (ackType) {
            VendorPoAcknowledgementType.ACKNOWLEDGED -> VendorCollaborationAuditEventType.PO_ACKNOWLEDGED
            VendorPoAcknowledgementType.ACKNOWLEDGED_WITH_EXCEPTION -> VendorCollaborationAuditEventType.PO_ACKNOWLEDGED_WITH_EXCEPTION
            VendorPoAcknowledgementType.DECLINED -> VendorCollaborationAuditEventType.PO_DECLINED
        }
        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = eventType,
                resourceType = "PURCHASE_ORDER",
                resourceId = purchaseOrderId,
                actorId = actorId,
                description = "Vendor acknowledged PO with outcome $ackType",
                previousState = po.status.name,
                newState = ackType.name
            )
        )

        return DomainResult.Success(ack)
    }

    override suspend fun listWorkOrders(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorWorkOrderStatus?
    ): DomainResult<List<VendorPortalWorkOrderSummary>> {
        val woResult = vendorWorkOrderService.listWorkOrders(
            projectId = projectId,
            vendorId = vendorId,
            status = status
        )
        if (woResult is DomainResult.Error) return woResult
        val workOrders = (woResult as DomainResult.Success).data

        val summaries = workOrders.map { wo ->
            val ack = (collaborationRepository.findWoAcknowledgement(wo.workOrderId, tenantId) as? DomainResult.Success)?.data
            val latestProg = (collaborationRepository.getLatestProgressUpdate(wo.workOrderId, tenantId) as? DomainResult.Success)?.data
            val complReq = (collaborationRepository.findCompletionRequest(wo.workOrderId, tenantId) as? DomainResult.Success)?.data
            val blockers = (collaborationRepository.listBlockers(tenantId, projectId, vendorId, wo.workOrderId, VendorBlockerStatus.OPEN) as? DomainResult.Success)?.data ?: emptyList()

            VendorPortalWorkOrderSummary(
                workOrderId = wo.workOrderId,
                workOrderNumber = wo.workOrderNumber,
                purchaseOrderId = wo.sourceReferenceId?.takeIf { wo.sourceReferenceType == "PURCHASE_ORDER" },
                title = wo.title,
                capabilityType = wo.capabilityType,
                quantity = wo.quantity,
                unitOfMeasure = wo.unitOfMeasure,
                status = wo.status,
                priority = wo.priority,
                scheduledStartAt = wo.scheduledStartAt,
                scheduledDueAt = wo.scheduledDueAt,
                estimatedAmount = wo.estimatedAmount,
                currency = wo.currency,
                acknowledgementStatus = ack?.acknowledgementType,
                latestProgressPercentage = latestProg?.progressPercentage,
                completionStatus = complReq?.status,
                openBlockersCount = blockers.size
            )
        }
        return DomainResult.Success(summaries)
    }

    override suspend fun getWorkOrderDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String
    ): DomainResult<VendorPortalWorkOrderDetails> {
        val woResult = vendorWorkOrderService.getWorkOrderById(projectId, workOrderId)
        if (woResult is DomainResult.Error) return woResult
        val wo = (woResult as DomainResult.Success).data

        if (wo.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to work order $workOrderId for vendor $vendorId"))
        }

        val ack = (collaborationRepository.findWoAcknowledgement(workOrderId, tenantId) as? DomainResult.Success)?.data
        val progressUpdates = (collaborationRepository.listProgressUpdates(workOrderId, tenantId) as? DomainResult.Success)?.data ?: emptyList()
        val blockers = (collaborationRepository.listBlockers(tenantId, projectId, vendorId, workOrderId) as? DomainResult.Success)?.data ?: emptyList()
        val evidence = (collaborationRepository.listEvidence(VendorThreadResourceType.WORK_ORDER, workOrderId, tenantId) as? DomainResult.Success)?.data ?: emptyList()
        val complReq = (collaborationRepository.findCompletionRequest(workOrderId, tenantId) as? DomainResult.Success)?.data

        return DomainResult.Success(
            VendorPortalWorkOrderDetails(
                workOrderId = wo.workOrderId,
                workOrderNumber = wo.workOrderNumber,
                purchaseOrderId = wo.sourceReferenceId?.takeIf { wo.sourceReferenceType == "PURCHASE_ORDER" },
                title = wo.title,
                description = wo.description,
                capabilityType = wo.capabilityType,
                quantity = wo.quantity,
                unitOfMeasure = wo.unitOfMeasure,
                pricingMethod = wo.pricingMethod,
                estimatedAmount = wo.estimatedAmount,
                currency = wo.currency,
                status = wo.status,
                priority = wo.priority,
                scheduledStartAt = wo.scheduledStartAt,
                scheduledDueAt = wo.scheduledDueAt,
                notes = wo.notes,
                acknowledgement = ack,
                progressUpdates = progressUpdates,
                blockers = blockers,
                evidenceList = evidence,
                completionRequest = complReq
            )
        )
    }

    override suspend fun acknowledgeWorkOrder(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String,
        ackType: VendorWoAcknowledgementType,
        exceptionDetails: String?,
        declineReason: String?,
        promisedStartDate: Long?,
        promisedCompletionDate: Long?,
        comment: String?,
        actorId: String
    ): DomainResult<VendorWoAcknowledgement> {
        val woResult = vendorWorkOrderService.getWorkOrderById(projectId, workOrderId)
        if (woResult is DomainResult.Error) return woResult
        val wo = (woResult as DomainResult.Success).data

        if (wo.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to acknowledge work order for different vendor."))
        }

        val ack = VendorWoAcknowledgement(
            acknowledgementId = UUID.randomUUID().toString(),
            workOrderId = workOrderId,
            purchaseOrderId = wo.sourceReferenceId?.takeIf { wo.sourceReferenceType == "PURCHASE_ORDER" },
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            actorId = actorId,
            acknowledgementType = ackType,
            exceptionDetails = exceptionDetails,
            declineReason = declineReason,
            promisedStartDate = promisedStartDate,
            promisedCompletionDate = promisedCompletionDate,
            comment = comment,
            acknowledgedAt = System.currentTimeMillis()
        )
        VendorCollaborationValidator.validateWoAcknowledgement(ack)

        val saveRes = collaborationRepository.saveWoAcknowledgement(ack)
        if (saveRes is DomainResult.Error) return saveRes

        // If acknowledged and in ASSIGNED status, release/advance if eligible
        if (ackType == VendorWoAcknowledgementType.ACKNOWLEDGED && wo.status == VendorWorkOrderStatus.ASSIGNED) {
            vendorWorkOrderService.changeStatus(projectId, workOrderId, VendorWorkOrderStatus.READY, actorId, reason = "Vendor acknowledged work order")
        }

        val eventType = when (ackType) {
            VendorWoAcknowledgementType.ACKNOWLEDGED -> VendorCollaborationAuditEventType.WO_ACKNOWLEDGED
            VendorWoAcknowledgementType.ACKNOWLEDGED_WITH_EXCEPTION -> VendorCollaborationAuditEventType.WO_ACKNOWLEDGED_WITH_EXCEPTION
            VendorWoAcknowledgementType.DECLINED -> VendorCollaborationAuditEventType.WO_DECLINED
        }
        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = eventType,
                resourceType = "WORK_ORDER",
                resourceId = workOrderId,
                actorId = actorId,
                description = "Vendor acknowledged Work Order with outcome $ackType",
                previousState = wo.status.name,
                newState = ackType.name
            )
        )

        return DomainResult.Success(ack)
    }

    override suspend fun submitProgress(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String,
        completedQuantity: BigDecimal,
        remainingQuantity: BigDecimal,
        progressPercentage: Double?,
        statusSummary: String,
        notes: String?,
        expectedCompletionDate: Long?,
        blockerReferenceId: String?,
        actorId: String
    ): DomainResult<VendorProgressUpdate> {
        val woResult = vendorWorkOrderService.getWorkOrderById(projectId, workOrderId)
        if (woResult is DomainResult.Error) return woResult
        val wo = (woResult as DomainResult.Success).data

        if (wo.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to submit progress for different vendor."))
        }

        val update = VendorProgressUpdate(
            progressUpdateId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            workOrderId = workOrderId,
            progressPercentage = progressPercentage,
            completedQuantity = completedQuantity,
            remainingQuantity = remainingQuantity,
            authorizedQuantity = wo.quantity,
            statusSummary = statusSummary,
            notes = notes,
            expectedCompletionDate = expectedCompletionDate,
            blockerReferenceId = blockerReferenceId,
            submittedBy = actorId,
            submittedAt = System.currentTimeMillis()
        )
        VendorCollaborationValidator.validateProgressUpdate(update)

        val saveRes = collaborationRepository.recordProgressUpdate(update)
        if (saveRes is DomainResult.Error) return saveRes

        // If WO is in READY or RELEASED and progress > 0, transition to IN_PROGRESS
        if (completedQuantity > BigDecimal.ZERO && (wo.status == VendorWorkOrderStatus.RELEASED || wo.status == VendorWorkOrderStatus.READY)) {
            vendorWorkOrderService.changeStatus(projectId, workOrderId, VendorWorkOrderStatus.IN_PROGRESS, actorId, reason = "Vendor reported progress")
        }

        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorCollaborationAuditEventType.PROGRESS_SUBMITTED,
                resourceType = "WORK_ORDER",
                resourceId = workOrderId,
                actorId = actorId,
                description = "Vendor submitted progress: $completedQuantity completed of ${wo.quantity}"
            )
        )

        return DomainResult.Success(update)
    }

    override suspend fun listProgressUpdates(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String
    ): DomainResult<List<VendorProgressUpdate>> {
        val woResult = vendorWorkOrderService.getWorkOrderById(projectId, workOrderId)
        if (woResult is DomainResult.Error) return woResult
        val wo = (woResult as DomainResult.Success).data
        if (wo.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to progress updates."))
        }
        return collaborationRepository.listProgressUpdates(workOrderId, tenantId)
    }

    override suspend fun reportBlocker(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String,
        purchaseOrderId: String?,
        category: VendorBlockerCategory,
        severity: VendorBlockerSeverity,
        title: String,
        description: String,
        actorId: String
    ): DomainResult<VendorBlocker> {
        val woResult = vendorWorkOrderService.getWorkOrderById(projectId, workOrderId)
        if (woResult is DomainResult.Error) return woResult
        val wo = (woResult as DomainResult.Success).data
        if (wo.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to report blocker for different vendor."))
        }

        val blocker = VendorBlocker(
            blockerId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            workOrderId = workOrderId,
            purchaseOrderId = purchaseOrderId ?: wo.sourceReferenceId?.takeIf { wo.sourceReferenceType == "PURCHASE_ORDER" },
            category = category,
            severity = severity,
            status = VendorBlockerStatus.OPEN,
            title = title,
            description = description,
            reportedBy = actorId,
            reportedAt = System.currentTimeMillis()
        )
        VendorCollaborationValidator.validateBlocker(blocker)

        val saveRes = collaborationRepository.saveBlocker(blocker)
        if (saveRes is DomainResult.Error) return saveRes

        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorCollaborationAuditEventType.BLOCKER_CREATED,
                resourceType = "WORK_ORDER",
                resourceId = workOrderId,
                actorId = actorId,
                description = "Vendor reported blocker '${blocker.title}' ($severity)"
            )
        )

        return DomainResult.Success(blocker)
    }

    override suspend fun listBlockers(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String?,
        status: VendorBlockerStatus?
    ): DomainResult<List<VendorBlocker>> {
        return collaborationRepository.listBlockers(tenantId, projectId, vendorId, workOrderId, status)
    }

    override suspend fun acknowledgeBlocker(
        tenantId: String,
        projectId: String,
        blockerId: String,
        actorId: String
    ): DomainResult<VendorBlocker> {
        val blockerResult = collaborationRepository.findBlockerById(blockerId, tenantId)
        if (blockerResult is DomainResult.Error) return blockerResult
        val blocker = (blockerResult as DomainResult.Success).data

        VendorCollaborationValidator.validateBlockerTransition(blocker.status, VendorBlockerStatus.ACKNOWLEDGED)

        val updated = blocker.copy(
            status = VendorBlockerStatus.ACKNOWLEDGED,
            acknowledgedBy = actorId,
            acknowledgedAt = System.currentTimeMillis(),
            version = blocker.version + 1
        )
        val saveRes = collaborationRepository.saveBlocker(updated)
        if (saveRes is DomainResult.Error) return saveRes

        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = blocker.vendorId,
                eventType = VendorCollaborationAuditEventType.BLOCKER_ACKNOWLEDGED,
                resourceType = "BLOCKER",
                resourceId = blockerId,
                actorId = actorId,
                description = "Blocker acknowledged by internal staff"
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun resolveBlocker(
        tenantId: String,
        projectId: String,
        blockerId: String,
        resolutionNotes: String,
        actorId: String
    ): DomainResult<VendorBlocker> {
        val blockerResult = collaborationRepository.findBlockerById(blockerId, tenantId)
        if (blockerResult is DomainResult.Error) return blockerResult
        val blocker = (blockerResult as DomainResult.Success).data

        VendorCollaborationValidator.validateBlockerTransition(blocker.status, VendorBlockerStatus.RESOLVED)

        val updated = blocker.copy(
            status = VendorBlockerStatus.RESOLVED,
            resolutionNotes = resolutionNotes,
            resolvedBy = actorId,
            resolvedAt = System.currentTimeMillis(),
            version = blocker.version + 1
        )
        val saveRes = collaborationRepository.saveBlocker(updated)
        if (saveRes is DomainResult.Error) return saveRes

        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = blocker.vendorId,
                eventType = VendorCollaborationAuditEventType.BLOCKER_RESOLVED,
                resourceType = "BLOCKER",
                resourceId = blockerId,
                actorId = actorId,
                description = "Blocker resolved: $resolutionNotes"
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getOrCreateThread(
        tenantId: String,
        projectId: String,
        vendorId: String,
        resourceType: VendorThreadResourceType,
        resourceId: String,
        title: String,
        actorId: String
    ): DomainResult<VendorCollaborationThread> {
        val existing = collaborationRepository.findThreadByResource(resourceType, resourceId, tenantId)
        if (existing is DomainResult.Success && existing.data != null) {
            return DomainResult.Success(existing.data!!)
        }

        val thread = VendorCollaborationThread(
            threadId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            resourceType = resourceType,
            resourceId = resourceId,
            title = title,
            createdBy = actorId,
            createdAt = System.currentTimeMillis()
        )
        val saveRes = collaborationRepository.saveThread(thread)
        if (saveRes is DomainResult.Error) return saveRes

        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorCollaborationAuditEventType.COLLABORATION_THREAD_CREATED,
                resourceType = resourceType.name,
                resourceId = resourceId,
                actorId = actorId,
                description = "Collaboration thread created: '$title'"
            )
        )

        return DomainResult.Success(thread)
    }

    override suspend fun postMessage(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String,
        messageText: String,
        isInternal: Boolean,
        visibility: VendorMessageVisibility,
        authorName: String?,
        actorId: String
    ): DomainResult<VendorCollaborationMessage> {
        val threadResult = collaborationRepository.findThreadById(threadId, tenantId)
        if (threadResult is DomainResult.Error) return threadResult
        val thread = (threadResult as DomainResult.Success).data

        if (!isInternal && thread.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to post messages in this thread."))
        }

        val message = VendorCollaborationMessage(
            messageId = UUID.randomUUID().toString(),
            threadId = threadId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = thread.vendorId,
            authorId = actorId,
            authorName = authorName,
            isInternalAuthor = isInternal,
            message = messageText,
            visibility = visibility,
            createdAt = System.currentTimeMillis()
        )
        VendorCollaborationValidator.validateCollaborationMessage(message)

        val saveRes = collaborationRepository.recordMessage(message)
        if (saveRes is DomainResult.Error) return saveRes

        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = thread.vendorId,
                eventType = VendorCollaborationAuditEventType.COLLABORATION_MESSAGE_CREATED,
                resourceType = "COLLABORATION_THREAD",
                resourceId = threadId,
                actorId = actorId,
                description = "Collaboration message posted in thread '${thread.title}'"
            )
        )

        return DomainResult.Success(message)
    }

    override suspend fun listMessages(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String,
        isVendorViewer: Boolean
    ): DomainResult<List<VendorCollaborationMessage>> {
        val threadResult = collaborationRepository.findThreadById(threadId, tenantId)
        if (threadResult is DomainResult.Error) return threadResult
        val thread = (threadResult as DomainResult.Success).data

        if (isVendorViewer && thread.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to view thread messages."))
        }

        val messagesResult = collaborationRepository.listMessages(threadId, tenantId)
        if (messagesResult is DomainResult.Error) return messagesResult
        val messages = (messagesResult as DomainResult.Success).data

        val filtered = if (isVendorViewer) {
            messages.filter { it.visibility != VendorMessageVisibility.INTERNAL_ONLY }
        } else {
            messages
        }
        return DomainResult.Success(filtered)
    }

    override suspend fun registerEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        resourceType: VendorThreadResourceType,
        resourceId: String,
        fileReference: String,
        filename: String,
        mimeType: String,
        sizeBytes: Long,
        description: String?,
        visibility: VendorMessageVisibility,
        actorId: String
    ): DomainResult<VendorCollaborationEvidence> {
        val evidence = VendorCollaborationEvidence(
            evidenceId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            resourceType = resourceType,
            resourceId = resourceId,
            fileReference = fileReference,
            filename = filename,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            description = description,
            visibility = visibility,
            uploadedBy = actorId,
            uploadedAt = System.currentTimeMillis()
        )
        val saveRes = collaborationRepository.saveEvidence(evidence)
        if (saveRes is DomainResult.Error) return saveRes

        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorCollaborationAuditEventType.EVIDENCE_REGISTERED,
                resourceType = resourceType.name,
                resourceId = resourceId,
                actorId = actorId,
                description = "Evidence registered: $filename ($sizeBytes bytes)"
            )
        )

        return DomainResult.Success(evidence)
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        resourceType: VendorThreadResourceType,
        resourceId: String,
        isVendorViewer: Boolean
    ): DomainResult<List<VendorCollaborationEvidence>> {
        val evidenceResult = collaborationRepository.listEvidence(resourceType, resourceId, tenantId)
        if (evidenceResult is DomainResult.Error) return evidenceResult
        val list = (evidenceResult as DomainResult.Success).data

        val filtered = list.filter {
            it.vendorId == vendorId && (!isVendorViewer || it.visibility != VendorMessageVisibility.INTERNAL_ONLY)
        }
        return DomainResult.Success(filtered)
    }

    override suspend fun submitCompletionRequest(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String,
        completionNotes: String,
        finalCompletedQuantity: BigDecimal,
        evidenceReferences: List<String>,
        actorId: String
    ): DomainResult<VendorCompletionRequest> {
        val woResult = vendorWorkOrderService.getWorkOrderById(projectId, workOrderId)
        if (woResult is DomainResult.Error) return woResult
        val wo = (woResult as DomainResult.Success).data

        if (wo.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to submit completion request for different vendor."))
        }

        val req = VendorCompletionRequest(
            completionRequestId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            workOrderId = workOrderId,
            status = VendorCompletionStatus.REQUESTED,
            completionNotes = completionNotes,
            finalCompletedQuantity = finalCompletedQuantity,
            evidenceReferences = evidenceReferences,
            submittedBy = actorId,
            submittedAt = System.currentTimeMillis()
        )
        VendorCollaborationValidator.validateCompletionRequest(req, wo.quantity)

        val saveRes = collaborationRepository.saveCompletionRequest(req)
        if (saveRes is DomainResult.Error) return saveRes

        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorCollaborationAuditEventType.COMPLETION_REQUESTED,
                resourceType = "WORK_ORDER",
                resourceId = workOrderId,
                actorId = actorId,
                description = "Vendor requested completion with final quantity $finalCompletedQuantity"
            )
        )

        return DomainResult.Success(req)
    }

    override suspend fun reviewCompletionRequest(
        tenantId: String,
        projectId: String,
        workOrderId: String,
        approved: Boolean,
        reviewNotes: String?,
        actorId: String
    ): DomainResult<VendorCompletionRequest> {
        val existing = collaborationRepository.findCompletionRequest(workOrderId, tenantId)
        if (existing is DomainResult.Error) return existing
        val req = (existing as DomainResult.Success).data
            ?: return DomainResult.Error(NoSuchElementException("No completion request found for work order $workOrderId"))

        val targetStatus = if (approved) VendorCompletionStatus.APPROVED else VendorCompletionStatus.RETURNED_FOR_CORRECTION
        VendorCollaborationValidator.validateCompletionTransition(req.status, targetStatus)

        val updated = req.copy(
            status = targetStatus,
            reviewedBy = actorId,
            reviewedAt = System.currentTimeMillis(),
            reviewNotes = reviewNotes,
            version = req.version + 1
        )
        val saveRes = collaborationRepository.saveCompletionRequest(updated)
        if (saveRes is DomainResult.Error) return saveRes

        if (approved) {
            // Advance Module 12 canonical work order to COMPLETED
            vendorWorkOrderService.changeStatus(projectId, workOrderId, VendorWorkOrderStatus.COMPLETED, actorId, reason = reviewNotes ?: "Vendor completion approved")
        }

        val eventType = if (approved) VendorCollaborationAuditEventType.COMPLETION_APPROVED else VendorCollaborationAuditEventType.COMPLETION_RETURNED
        collaborationRepository.recordAuditEvent(
            VendorCollaborationAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = req.vendorId,
                eventType = eventType,
                resourceType = "WORK_ORDER",
                resourceId = workOrderId,
                actorId = actorId,
                description = "Internal review of completion request: $targetStatus (${reviewNotes ?: "No notes"})"
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getCompletionRequest(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String
    ): DomainResult<VendorCompletionRequest?> {
        val existing = collaborationRepository.findCompletionRequest(workOrderId, tenantId)
        if (existing is DomainResult.Error) return existing
        val req = (existing as DomainResult.Success).data
        if (req != null && req.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to completion request."))
        }
        return DomainResult.Success(req)
    }
}
