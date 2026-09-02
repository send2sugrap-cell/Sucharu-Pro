package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

/**
 * Domain Service for Vendor Delivery, Receiving & Quality Collaboration (Module 13 Step 05).
 */
interface VendorPortalDeliveryService {

    // Delivery Notices (ASN)
    suspend fun createDeliveryNotice(
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
    ): DomainResult<VendorPortalDeliveryNotice>

    suspend fun updateDeliveryNotice(
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
    ): DomainResult<VendorPortalDeliveryNotice>

    suspend fun getDeliveryNotice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        noticeId: String
    ): DomainResult<VendorPortalDeliveryNotice>

    suspend fun listDeliveryNotices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String? = null,
        status: VendorPortalDeliveryNoticeStatus? = null
    ): DomainResult<List<VendorPortalDeliveryNotice>>

    suspend fun submitDeliveryNotice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        noticeId: String,
        actorId: String
    ): DomainResult<VendorPortalDeliveryNotice>

    suspend fun cancelDeliveryNotice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        noticeId: String,
        reason: String,
        actorId: String
    ): DomainResult<VendorPortalDeliveryNotice>

    // Receiving Collaboration Projections
    suspend fun getReceivingSummary(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String
    ): DomainResult<VendorPortalReceivingSummary>

    suspend fun getDeliveryReceiptDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        receiptId: String
    ): DomainResult<com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt>

    // Quality Collaboration Projections & Responses
    suspend fun listQualityInspections(
        tenantId: String,
        projectId: String,
        vendorId: String,
        deliveryReceiptId: String? = null
    ): DomainResult<List<VendorPortalQualityInspectionSummary>>

    suspend fun getQualityInspectionDetails(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String
    ): DomainResult<VendorPortalQualityInspectionSummary>

    suspend fun acknowledgeQualityInspection(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String,
        comment: String,
        actorId: String
    ): DomainResult<VendorPortalQualityResponse>

    suspend fun respondToQuality(
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
    ): DomainResult<VendorPortalQualityResponse>

    // Delivery & Quality Exceptions
    suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDeliveryExceptionStatus? = null,
        sourceType: String? = null
    ): DomainResult<List<VendorPortalDeliveryException>>

    suspend fun resolveException(
        tenantId: String,
        projectId: String,
        exceptionId: String,
        resolutionNotes: String,
        actorId: String
    ): DomainResult<VendorPortalDeliveryException>

    // Evidence
    suspend fun registerEvidence(
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
    ): DomainResult<VendorPortalDeliveryEvidence>

    suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalDeliveryEvidence>>

    // Audit Trail
    suspend fun listAuditEvents(
        tenantId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalDeliveryAuditEvent>>
}

/**
 * Input DTO for creating delivery notice items.
 */
data class VendorPortalDeliveryNoticeItemInput(
    val purchaseOrderItemId: String,
    val deliveryQuantity: BigDecimal,
    val lotNumber: String? = null,
    val packageCount: Int? = null,
    val remarks: String? = null
)
