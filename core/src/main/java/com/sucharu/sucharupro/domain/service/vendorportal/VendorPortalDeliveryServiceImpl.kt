package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptItem
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalDeliveryRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.service.vendor.VendorDeliveryReceiptService
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderService
import com.sucharu.sucharupro.domain.service.vendor.VendorQualityService
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalDeliveryValidator
import java.math.BigDecimal
import java.util.UUID

/**
 * Implementation of [VendorPortalDeliveryService] orchestrating delivery notices,
 * canonical receiving and quality projections, vendor responses, exceptions, and audit events.
 */
class VendorPortalDeliveryServiceImpl(
    private val deliveryRepository: VendorPortalDeliveryRepository,
    private val vendorPurchaseOrderService: VendorPurchaseOrderService,
    private val vendorDeliveryReceiptService: VendorDeliveryReceiptService,
    private val vendorQualityService: VendorQualityService,
    private val vendorRepository: VendorRepository
) : VendorPortalDeliveryService {

    override suspend fun createDeliveryNotice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String,
        plannedDeliveryDate: Long,
        carrierName: String?,
        trackingNumber: String?,
        vehicleNumber: String?,
        driverName: String?,
        driverPhone: String?,
        vendorNotes: String?,
        items: List<VendorPortalDeliveryNoticeItemInput>,
        actorId: String
    ): DomainResult<VendorPortalDeliveryNotice> {
        val vendorRes = vendorRepository.findById(projectId, vendorId)
        if (vendorRes is DomainResult.Error) return vendorRes

        val poRes = vendorPurchaseOrderService.getOrderById(projectId, purchaseOrderId)
        if (poRes is DomainResult.Error) return poRes
        val po = (poRes as DomainResult.Success).data
        if (po.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to purchase order for different vendor."))
        }

        // Fetch receiving summary to check previously delivered quantities
        val receivingSummaryRes = getReceivingSummary(tenantId, projectId, vendorId, purchaseOrderId)
        val receivingSummary = (receivingSummaryRes as? DomainResult.Success)?.data

        val noticeId = UUID.randomUUID().toString()
        val noticeNumber = "ASN-${System.currentTimeMillis() % 1000000}"

        val poItemsMap = po.items.associateBy { it.itemId }
        val receivingItemsMap = receivingSummary?.items?.associateBy { it.purchaseOrderItemId } ?: emptyMap()

        val noticeItems = items.map { input ->
            val poItem = poItemsMap[input.purchaseOrderItemId]
                ?: return DomainResult.Error(IllegalArgumentException("Item '${input.purchaseOrderItemId}' does not belong to PO '$purchaseOrderId'."))
            val recItem = receivingItemsMap[input.purchaseOrderItemId]
            val prevDelivered = recItem?.receivedQuantity ?: BigDecimal.ZERO

            VendorPortalDeliveryNoticeItem(
                itemId = UUID.randomUUID().toString(),
                noticeId = noticeId,
                tenantId = tenantId,
                purchaseOrderItemId = input.purchaseOrderItemId,
                itemName = poItem.itemDescription,
                itemCode = poItem.itemCode,
                orderedQuantity = poItem.quantity,
                previouslyDeliveredQuantity = prevDelivered,
                deliveryQuantity = input.deliveryQuantity,
                unitOfMeasure = poItem.unitOfMeasure.name,
                lotNumber = input.lotNumber,
                packageCount = input.packageCount,
                remarks = input.remarks
            )
        }

        val notice = VendorPortalDeliveryNotice(
            noticeId = noticeId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            purchaseOrderId = purchaseOrderId,
            orderNumber = po.orderNumber,
            noticeNumber = noticeNumber,
            status = VendorPortalDeliveryNoticeStatus.DRAFT,
            plannedDeliveryDate = plannedDeliveryDate,
            carrierName = carrierName,
            trackingNumber = trackingNumber,
            vehicleNumber = vehicleNumber,
            driverName = driverName,
            driverPhone = driverPhone,
            vendorNotes = vendorNotes,
            items = noticeItems,
            createdAt = System.currentTimeMillis(),
            createdBy = actorId,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        try {
            VendorPortalDeliveryValidator.validateDeliveryNotice(notice)
        } catch (e: Exception) {
            return DomainResult.Error(e)
        }

        val saveRes = deliveryRepository.saveDeliveryNotice(notice)
        if (saveRes is DomainResult.Error) return saveRes

        deliveryRepository.recordAuditEvent(
            VendorPortalDeliveryAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalDeliveryAuditEventType.DELIVERY_NOTICE_CREATED,
                entityType = "DELIVERY_NOTICE",
                entityId = noticeId,
                actorId = actorId,
                description = "Created delivery notice $noticeNumber for PO ${po.orderNumber}",
                newState = VendorPortalDeliveryNoticeStatus.DRAFT.name
            )
        )

        return DomainResult.Success(notice)
    }

    override suspend fun updateDeliveryNotice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        noticeId: String,
        plannedDeliveryDate: Long,
        carrierName: String?,
        trackingNumber: String?,
        vehicleNumber: String?,
        driverName: String?,
        driverPhone: String?,
        vendorNotes: String?,
        actorId: String
    ): DomainResult<VendorPortalDeliveryNotice> {
        val existingRes = deliveryRepository.findDeliveryNoticeById(noticeId, tenantId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Delivery notice $noticeId not found."))

        if (existing.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to delivery notice for different vendor."))
        }
        if (existing.status != VendorPortalDeliveryNoticeStatus.DRAFT) {
            return DomainResult.Error(IllegalStateException("Cannot modify delivery notice in ${existing.status} state."))
        }

        val updated = existing.copy(
            plannedDeliveryDate = plannedDeliveryDate,
            carrierName = carrierName,
            trackingNumber = trackingNumber,
            vehicleNumber = vehicleNumber,
            driverName = driverName,
            driverPhone = driverPhone,
            vendorNotes = vendorNotes,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        val updateRes = deliveryRepository.updateDeliveryNotice(updated)
        if (updateRes is DomainResult.Error) return updateRes

        deliveryRepository.recordAuditEvent(
            VendorPortalDeliveryAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalDeliveryAuditEventType.DELIVERY_NOTICE_UPDATED,
                entityType = "DELIVERY_NOTICE",
                entityId = noticeId,
                actorId = actorId,
                description = "Updated delivery notice ${existing.noticeNumber}"
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getDeliveryNotice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        noticeId: String
    ): DomainResult<VendorPortalDeliveryNotice> {
        val res = deliveryRepository.findDeliveryNoticeById(noticeId, tenantId)
        if (res is DomainResult.Error) return res
        val notice = (res as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Delivery notice $noticeId not found."))
        if (notice.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to delivery notice for different vendor."))
        }
        return DomainResult.Success(notice)
    }

    override suspend fun listDeliveryNotices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String?,
        status: VendorPortalDeliveryNoticeStatus?
    ): DomainResult<List<VendorPortalDeliveryNotice>> {
        return deliveryRepository.listDeliveryNotices(tenantId, projectId, vendorId, purchaseOrderId, status)
    }

    override suspend fun submitDeliveryNotice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        noticeId: String,
        actorId: String
    ): DomainResult<VendorPortalDeliveryNotice> {
        val existingRes = deliveryRepository.findDeliveryNoticeById(noticeId, tenantId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Delivery notice $noticeId not found."))

        if (existing.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to submit delivery notice for different vendor."))
        }

        VendorPortalDeliveryValidator.validateNoticeStatusTransition(existing.status, VendorPortalDeliveryNoticeStatus.SUBMITTED)

        val updated = existing.copy(
            status = VendorPortalDeliveryNoticeStatus.SUBMITTED,
            submittedAt = System.currentTimeMillis(),
            submittedBy = actorId,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val updateRes = deliveryRepository.updateDeliveryNotice(updated)
        if (updateRes is DomainResult.Error) return updateRes

        deliveryRepository.recordAuditEvent(
            VendorPortalDeliveryAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalDeliveryAuditEventType.DELIVERY_NOTICE_SUBMITTED,
                entityType = "DELIVERY_NOTICE",
                entityId = noticeId,
                actorId = actorId,
                description = "Submitted delivery notice ${existing.noticeNumber}",
                previousState = existing.status.name,
                newState = VendorPortalDeliveryNoticeStatus.SUBMITTED.name
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun cancelDeliveryNotice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        noticeId: String,
        reason: String,
        actorId: String
    ): DomainResult<VendorPortalDeliveryNotice> {
        val existingRes = deliveryRepository.findDeliveryNoticeById(noticeId, tenantId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Delivery notice $noticeId not found."))

        if (existing.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to cancel delivery notice for different vendor."))
        }

        VendorPortalDeliveryValidator.validateNoticeStatusTransition(existing.status, VendorPortalDeliveryNoticeStatus.CANCELLED)

        val updated = existing.copy(
            status = VendorPortalDeliveryNoticeStatus.CANCELLED,
            cancelledAt = System.currentTimeMillis(),
            cancelledBy = actorId,
            cancellationReason = reason,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val updateRes = deliveryRepository.updateDeliveryNotice(updated)
        if (updateRes is DomainResult.Error) return updateRes

        deliveryRepository.recordAuditEvent(
            VendorPortalDeliveryAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalDeliveryAuditEventType.DELIVERY_NOTICE_CANCELLED,
                entityType = "DELIVERY_NOTICE",
                entityId = noticeId,
                actorId = actorId,
                description = "Cancelled delivery notice ${existing.noticeNumber}: $reason",
                previousState = existing.status.name,
                newState = VendorPortalDeliveryNoticeStatus.CANCELLED.name
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getReceivingSummary(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String
    ): DomainResult<VendorPortalReceivingSummary> {
        val poRes = vendorPurchaseOrderService.getOrderById(projectId, purchaseOrderId)
        if (poRes is DomainResult.Error) return poRes
        val po = (poRes as DomainResult.Success).data
        if (po.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to PO receiving summary for different vendor."))
        }

        // Query canonical receipts for this PO
        val receiptsRes = vendorDeliveryReceiptService.listReceipts(projectId, vendorId = vendorId, purchaseOrderId = purchaseOrderId)
        val receipts = (receiptsRes as? DomainResult.Success)?.data ?: emptyList()
        val activeReceipts = receipts.filter { it.status != VendorDeliveryReceiptStatus.CANCELLED }

        // Aggregate quantities per PO item
        val poItemSummaries: List<VendorPortalReceivingItemSummary> = po.items.map { poItem ->
            val matchingReceiptItems = activeReceipts.flatMap { it.items }.filter { it.purchaseOrderItemId == poItem.itemId }
            val receivedQty = matchingReceiptItems.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorDeliveryReceiptItem -> acc.add(item.receivedQuantity) }
            val acceptedQty = matchingReceiptItems.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorDeliveryReceiptItem -> acc.add(item.acceptedQuantity) }
            val rejectedQty = matchingReceiptItems.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorDeliveryReceiptItem -> acc.add(item.rejectedQuantity) }
            val conditionalQty = BigDecimal.ZERO
            val remainingQty = poItem.quantity.subtract(acceptedQty).max(BigDecimal.ZERO)

            VendorPortalReceivingItemSummary(
                purchaseOrderItemId = poItem.itemId,
                itemName = poItem.itemDescription,
                orderedQuantity = poItem.quantity,
                notifiedQuantity = poItem.quantity, // fallback/snapshot
                receivedQuantity = receivedQty,
                acceptedQuantity = acceptedQty,
                rejectedQuantity = rejectedQty,
                conditionalQuantity = conditionalQty,
                remainingQuantity = remainingQty,
                unitOfMeasure = poItem.unitOfMeasure.name
            )
        }

        val totalOrdered = poItemSummaries.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorPortalReceivingItemSummary -> acc.add(item.orderedQuantity) }
        val totalReceived = poItemSummaries.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorPortalReceivingItemSummary -> acc.add(item.receivedQuantity) }
        val totalAccepted = poItemSummaries.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorPortalReceivingItemSummary -> acc.add(item.acceptedQuantity) }
        val totalRejected = poItemSummaries.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorPortalReceivingItemSummary -> acc.add(item.rejectedQuantity) }
        val totalConditional = poItemSummaries.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorPortalReceivingItemSummary -> acc.add(item.conditionalQuantity) }
        val totalRemaining = poItemSummaries.fold(BigDecimal.ZERO) { acc: BigDecimal, item: VendorPortalReceivingItemSummary -> acc.add(item.remainingQuantity) }

        val summary = VendorPortalReceivingSummary(
            purchaseOrderId = purchaseOrderId,
            orderNumber = po.orderNumber,
            vendorId = vendorId,
            status = po.status.name,
            totalOrderedQuantity = totalOrdered,
            totalNotifiedQuantity = totalOrdered,
            totalReceivedQuantity = totalReceived,
            totalAcceptedQuantity = totalAccepted,
            totalRejectedQuantity = totalRejected,
            totalConditionalQuantity = totalConditional,
            totalRemainingQuantity = totalRemaining,
            receiptCount = activeReceipts.size,
            latestReceiptDate = activeReceipts.maxOfOrNull { it.receiptDate },
            items = poItemSummaries
        )

        return DomainResult.Success(summary)
    }

    override suspend fun getDeliveryReceiptDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        receiptId: String
    ): DomainResult<com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt> {
        val res = vendorDeliveryReceiptService.getReceiptById(projectId, receiptId)
        if (res is DomainResult.Error) return res
        val receipt = (res as DomainResult.Success).data
        if (receipt.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to delivery receipt for different vendor."))
        }
        return DomainResult.Success(receipt)
    }

    override suspend fun listQualityInspections(
        tenantId: String,
        projectId: String,
        vendorId: String,
        deliveryReceiptId: String?
    ): DomainResult<List<VendorPortalQualityInspectionSummary>> {
        val inspectionsRes = vendorQualityService.listInspections(projectId, vendorId = vendorId, deliveryReceiptId = deliveryReceiptId)
        if (inspectionsRes is DomainResult.Error) return inspectionsRes
        val inspections = (inspectionsRes as DomainResult.Success).data

        val summaries = inspections.map { insp ->
            val defectsRes = vendorQualityService.listDefects(projectId, insp.inspectionId)
            val defects = (defectsRes as? DomainResult.Success)?.data ?: emptyList()
            val rejectionsRes = vendorQualityService.listRejections(projectId, vendorId = vendorId, deliveryReceiptId = insp.deliveryReceiptId)
            val rejection = (rejectionsRes as? DomainResult.Success)?.data?.firstOrNull { it.deliveryReceiptId == insp.deliveryReceiptId }
            val disputesRes = vendorQualityService.listDisputes(projectId, vendorId = vendorId)
            val dispute = (disputesRes as? DomainResult.Success)?.data?.firstOrNull { it.inspectionId == insp.inspectionId }

            VendorPortalQualityInspectionSummary(
                inspectionId = insp.inspectionId,
                inspectionNumber = insp.inspectionReference,
                deliveryReceiptId = insp.deliveryReceiptId ?: "",
                purchaseOrderId = insp.purchaseOrderId ?: "",
                vendorId = insp.vendorId,
                inspectionDate = insp.inspectionStartedAt ?: insp.createdAt,
                status = insp.inspectionStatus.name,
                overallResult = insp.overallResult?.name ?: "PENDING",
                inspectedQuantity = insp.receivedQuantity,
                acceptedQuantity = insp.acceptedQuantity,
                rejectedQuantity = insp.rejectedQuantity,
                conditionalQuantity = insp.conditionalQuantity,
                rejectionId = rejection?.rejectionId,
                rejectionReason = rejection?.rejectionReason,
                disposition = rejection?.disposition?.name,
                replacementRequired = rejection?.replacementRequired ?: false,
                creditRequired = rejection?.creditRequired ?: false,
                correctiveActionRequired = rejection?.resolutionNotes != null,
                disputeId = dispute?.disputeId,
                disputeStatus = dispute?.status?.name,
                items = insp.items.map { item ->
                    VendorPortalQualityItemSummary(
                        inspectionItemId = item.inspectionItemId,
                        purchaseOrderItemId = item.purchaseOrderItemId,
                        itemName = item.itemDescription,
                        inspectedQuantity = item.receivedQuantity,
                        acceptedQuantity = item.acceptedQuantity,
                        rejectedQuantity = item.rejectedQuantity,
                        conditionalQuantity = item.conditionalQuantity,
                        defectCount = item.defectCount,
                        remarks = item.notes
                    )
                },
                defects = defects.map { d ->
                    VendorPortalDefectSummary(
                        defectId = d.defectId,
                        defectCode = d.defectType.name,
                        defectCategory = d.defectType.name,
                        severity = d.severity.name,
                        affectedQuantity = d.quantityAffected,
                        description = d.description
                    )
                }
            )
        }

        return DomainResult.Success(summaries)
    }

    override suspend fun getQualityInspectionDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String
    ): DomainResult<VendorPortalQualityInspectionSummary> {
        val inspRes = vendorQualityService.getInspection(projectId, inspectionId)
        if (inspRes is DomainResult.Error) return inspRes
        val insp = (inspRes as DomainResult.Success).data
        if (insp.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to quality inspection for different vendor."))
        }

        val defectsRes = vendorQualityService.listDefects(projectId, insp.inspectionId)
        val defects = (defectsRes as? DomainResult.Success)?.data ?: emptyList()
        val rejectionsRes = vendorQualityService.listRejections(projectId, vendorId = vendorId, deliveryReceiptId = insp.deliveryReceiptId)
        val rejection = (rejectionsRes as? DomainResult.Success)?.data?.firstOrNull { it.deliveryReceiptId == insp.deliveryReceiptId }
        val disputesRes = vendorQualityService.listDisputes(projectId, vendorId = vendorId)
        val dispute = (disputesRes as? DomainResult.Success)?.data?.firstOrNull { it.inspectionId == insp.inspectionId }

        val summary = VendorPortalQualityInspectionSummary(
            inspectionId = insp.inspectionId,
            inspectionNumber = insp.inspectionReference,
            deliveryReceiptId = insp.deliveryReceiptId ?: "",
            purchaseOrderId = insp.purchaseOrderId ?: "",
            vendorId = insp.vendorId,
            inspectionDate = insp.inspectionStartedAt ?: insp.createdAt,
            status = insp.inspectionStatus.name,
            overallResult = insp.overallResult?.name ?: "PENDING",
            inspectedQuantity = insp.receivedQuantity,
            acceptedQuantity = insp.acceptedQuantity,
            rejectedQuantity = insp.rejectedQuantity,
            conditionalQuantity = insp.conditionalQuantity,
            rejectionId = rejection?.rejectionId,
            rejectionReason = rejection?.rejectionReason,
            disposition = rejection?.disposition?.name,
            replacementRequired = rejection?.replacementRequired ?: false,
            creditRequired = rejection?.creditRequired ?: false,
            correctiveActionRequired = rejection?.resolutionNotes != null,
            disputeId = dispute?.disputeId,
            disputeStatus = dispute?.status?.name,
            items = insp.items.map { item ->
                VendorPortalQualityItemSummary(
                    inspectionItemId = item.inspectionItemId,
                    purchaseOrderItemId = item.purchaseOrderItemId,
                    itemName = item.itemDescription,
                    inspectedQuantity = item.receivedQuantity,
                    acceptedQuantity = item.acceptedQuantity,
                    rejectedQuantity = item.rejectedQuantity,
                    conditionalQuantity = item.conditionalQuantity,
                    defectCount = item.defectCount,
                    remarks = item.notes
                )
            },
            defects = defects.map { d ->
                VendorPortalDefectSummary(
                    defectId = d.defectId,
                    defectCode = d.defectType.name,
                    defectCategory = d.defectType.name,
                    severity = d.severity.name,
                    affectedQuantity = d.quantityAffected,
                    description = d.description
                )
            }
        )

        return DomainResult.Success(summary)
    }

    override suspend fun acknowledgeQualityInspection(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String,
        comment: String,
        actorId: String
    ): DomainResult<VendorPortalQualityResponse> {
        return respondToQuality(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            inspectionId = inspectionId,
            rejectionId = null,
            responseType = VendorPortalQualityResponseType.ACKNOWLEDGE,
            comment = comment,
            correctiveActionPlan = null,
            promisedReplacementDate = null,
            evidenceReferences = emptyList(),
            actorId = actorId
        )
    }

    override suspend fun respondToQuality(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String,
        rejectionId: String?,
        responseType: VendorPortalQualityResponseType,
        comment: String,
        correctiveActionPlan: String?,
        promisedReplacementDate: Long?,
        evidenceReferences: List<String>,
        actorId: String
    ): DomainResult<VendorPortalQualityResponse> {
        val inspRes = vendorQualityService.getInspection(projectId, inspectionId)
        if (inspRes is DomainResult.Error) return inspRes
        val insp = (inspRes as DomainResult.Success).data
        if (insp.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized to respond to quality inspection for different vendor."))
        }

        val response = VendorPortalQualityResponse(
            responseId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            inspectionId = inspectionId,
            rejectionId = rejectionId,
            responseType = responseType,
            comment = comment,
            correctiveActionPlan = correctiveActionPlan,
            promisedReplacementDate = promisedReplacementDate,
            evidenceReferences = evidenceReferences,
            respondedBy = actorId,
            respondedAt = System.currentTimeMillis()
        )

        try {
            VendorPortalDeliveryValidator.validateQualityResponse(response)
        } catch (e: Exception) {
            return DomainResult.Error(e)
        }

        val saveRes = deliveryRepository.saveQualityResponse(response)
        if (saveRes is DomainResult.Error) return saveRes

        // If response is dispute request and rejectionId exists, advance Module 12 rejection dispute flow if supported
        if (responseType == VendorPortalQualityResponseType.REQUEST_DISPUTE && rejectionId != null) {
            vendorQualityService.disputeRejection(projectId, rejectionId, comment, actorId)
        }

        val auditEventType = when (responseType) {
            VendorPortalQualityResponseType.ACKNOWLEDGE -> VendorPortalDeliveryAuditEventType.QUALITY_ACKNOWLEDGED
            VendorPortalQualityResponseType.PROPOSE_CORRECTIVE_ACTION -> VendorPortalDeliveryAuditEventType.CORRECTIVE_ACTION_PROPOSED
            VendorPortalQualityResponseType.COMMIT_REPLACEMENT -> VendorPortalDeliveryAuditEventType.REPLACEMENT_COMMITTED
            VendorPortalQualityResponseType.REQUEST_DISPUTE -> VendorPortalDeliveryAuditEventType.QUALITY_RESPONSE_SUBMITTED
        }

        deliveryRepository.recordAuditEvent(
            VendorPortalDeliveryAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = auditEventType,
                entityType = "QUALITY_INSPECTION",
                entityId = inspectionId,
                actorId = actorId,
                description = "Vendor responded with $responseType to inspection ${insp.inspectionReference}"
            )
        )

        return DomainResult.Success(response)
    }

    override suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDeliveryExceptionStatus?,
        sourceType: String?
    ): DomainResult<List<VendorPortalDeliveryException>> {
        return deliveryRepository.listExceptions(tenantId, projectId, vendorId, status, sourceType)
    }

    override suspend fun resolveException(
        tenantId: String,
        projectId: String,
        exceptionId: String,
        resolutionNotes: String,
        actorId: String
    ): DomainResult<VendorPortalDeliveryException> {
        val existingRes = deliveryRepository.findExceptionById(exceptionId, tenantId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Exception $exceptionId not found."))

        val resolved = existing.copy(
            status = VendorPortalDeliveryExceptionStatus.RESOLVED,
            resolvedAt = System.currentTimeMillis(),
            resolvedBy = actorId,
            resolutionNotes = resolutionNotes
        )

        val updateRes = deliveryRepository.updateException(resolved)
        if (updateRes is DomainResult.Error) return updateRes

        deliveryRepository.recordAuditEvent(
            VendorPortalDeliveryAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = existing.vendorId,
                eventType = VendorPortalDeliveryAuditEventType.EXCEPTION_RESOLVED,
                entityType = "DELIVERY_EXCEPTION",
                entityId = exceptionId,
                actorId = actorId,
                description = "Resolved exception '${existing.title}': $resolutionNotes",
                previousState = existing.status.name,
                newState = VendorPortalDeliveryExceptionStatus.RESOLVED.name
            )
        )

        return DomainResult.Success(resolved)
    }

    override suspend fun registerEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String,
        filename: String,
        fileReference: String,
        mimeType: String,
        sizeBytes: Long,
        description: String?,
        actorId: String
    ): DomainResult<VendorPortalDeliveryEvidence> {
        val evidence = VendorPortalDeliveryEvidence(
            evidenceId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            entityType = entityType,
            entityId = entityId,
            filename = filename,
            fileReference = fileReference,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            description = description,
            uploadedBy = actorId,
            uploadedAt = System.currentTimeMillis()
        )

        try {
            VendorPortalDeliveryValidator.validateEvidence(evidence)
        } catch (e: Exception) {
            return DomainResult.Error(e)
        }

        val saveRes = deliveryRepository.saveEvidence(evidence)
        if (saveRes is DomainResult.Error) return saveRes

        deliveryRepository.recordAuditEvent(
            VendorPortalDeliveryAuditEvent(
                eventId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                eventType = VendorPortalDeliveryAuditEventType.EVIDENCE_ATTACHED,
                entityType = entityType,
                entityId = entityId,
                actorId = actorId,
                description = "Attached evidence '$filename' ($sizeBytes bytes) to $entityType $entityId"
            )
        )

        return DomainResult.Success(evidence)
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalDeliveryEvidence>> {
        return deliveryRepository.listEvidence(tenantId, projectId, vendorId, entityType, entityId)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalDeliveryAuditEvent>> {
        return deliveryRepository.listAuditEvents(tenantId, entityType, entityId)
    }
}
