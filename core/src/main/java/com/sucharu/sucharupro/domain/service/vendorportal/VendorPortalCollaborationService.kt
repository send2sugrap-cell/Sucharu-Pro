package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

interface VendorPortalCollaborationService {

    // Purchase Orders
    suspend fun listPurchaseOrders(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPurchaseOrderStatus? = null
    ): DomainResult<List<VendorPortalPurchaseOrderSummary>>

    suspend fun getPurchaseOrderDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String
    ): DomainResult<VendorPortalPurchaseOrderDetails>

    suspend fun acknowledgePurchaseOrder(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String,
        ackType: VendorPoAcknowledgementType,
        exceptionDetails: String? = null,
        declineReason: String? = null,
        promisedDeliveryDate: Long? = null,
        comment: String? = null,
        actorId: String
    ): DomainResult<VendorPoAcknowledgement>

    // Work Orders
    suspend fun listWorkOrders(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorWorkOrderStatus? = null
    ): DomainResult<List<VendorPortalWorkOrderSummary>>

    suspend fun getWorkOrderDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String
    ): DomainResult<VendorPortalWorkOrderDetails>

    suspend fun acknowledgeWorkOrder(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String,
        ackType: VendorWoAcknowledgementType,
        exceptionDetails: String? = null,
        declineReason: String? = null,
        promisedStartDate: Long? = null,
        promisedCompletionDate: Long? = null,
        comment: String? = null,
        actorId: String
    ): DomainResult<VendorWoAcknowledgement>

    // Progress
    suspend fun submitProgress(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String,
        completedQuantity: BigDecimal,
        remainingQuantity: BigDecimal,
        progressPercentage: Double? = null,
        statusSummary: String,
        notes: String? = null,
        expectedCompletionDate: Long? = null,
        blockerReferenceId: String? = null,
        actorId: String
    ): DomainResult<VendorProgressUpdate>

    suspend fun listProgressUpdates(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String
    ): DomainResult<List<VendorProgressUpdate>>

    // Blockers
    suspend fun reportBlocker(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String,
        purchaseOrderId: String? = null,
        category: VendorBlockerCategory,
        severity: VendorBlockerSeverity,
        title: String,
        description: String,
        actorId: String
    ): DomainResult<VendorBlocker>

    suspend fun listBlockers(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String? = null,
        status: VendorBlockerStatus? = null
    ): DomainResult<List<VendorBlocker>>

    suspend fun acknowledgeBlocker(
        tenantId: String,
        projectId: String,
        blockerId: String,
        actorId: String
    ): DomainResult<VendorBlocker>

    suspend fun resolveBlocker(
        tenantId: String,
        projectId: String,
        blockerId: String,
        resolutionNotes: String,
        actorId: String
    ): DomainResult<VendorBlocker>

    // Collaboration Threads & Messages
    suspend fun getOrCreateThread(
        tenantId: String,
        projectId: String,
        vendorId: String,
        resourceType: VendorThreadResourceType,
        resourceId: String,
        title: String,
        actorId: String
    ): DomainResult<VendorCollaborationThread>

    suspend fun postMessage(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String,
        messageText: String,
        isInternal: Boolean,
        visibility: VendorMessageVisibility = VendorMessageVisibility.VENDOR_VISIBLE,
        authorName: String? = null,
        actorId: String
    ): DomainResult<VendorCollaborationMessage>

    suspend fun listMessages(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String,
        isVendorViewer: Boolean = true
    ): DomainResult<List<VendorCollaborationMessage>>

    // Evidence
    suspend fun registerEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        resourceType: VendorThreadResourceType,
        resourceId: String,
        fileReference: String,
        filename: String,
        mimeType: String,
        sizeBytes: Long,
        description: String? = null,
        visibility: VendorMessageVisibility = VendorMessageVisibility.VENDOR_VISIBLE,
        actorId: String
    ): DomainResult<VendorCollaborationEvidence>

    suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        resourceType: VendorThreadResourceType,
        resourceId: String,
        isVendorViewer: Boolean = true
    ): DomainResult<List<VendorCollaborationEvidence>>

    // Completion Requests
    suspend fun submitCompletionRequest(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String,
        completionNotes: String,
        finalCompletedQuantity: BigDecimal,
        evidenceReferences: List<String> = emptyList(),
        actorId: String
    ): DomainResult<VendorCompletionRequest>

    suspend fun reviewCompletionRequest(
        tenantId: String,
        projectId: String,
        workOrderId: String,
        approved: Boolean,
        reviewNotes: String? = null,
        actorId: String
    ): DomainResult<VendorCompletionRequest>

    suspend fun getCompletionRequest(
        tenantId: String,
        projectId: String,
        vendorId: String,
        workOrderId: String
    ): DomainResult<VendorCompletionRequest?>
}
