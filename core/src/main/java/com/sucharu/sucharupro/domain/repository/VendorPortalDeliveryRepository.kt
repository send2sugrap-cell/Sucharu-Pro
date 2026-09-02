package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Domain Repository interface for Vendor Portal Delivery Notices, Quality Responses,
 * Exceptions, Evidence, and Audit persistence (Module 13 Step 05).
 */
interface VendorPortalDeliveryRepository {

    // Delivery Notices
    suspend fun saveDeliveryNotice(notice: VendorPortalDeliveryNotice): DomainResult<VendorPortalDeliveryNotice>
    suspend fun updateDeliveryNotice(notice: VendorPortalDeliveryNotice): DomainResult<VendorPortalDeliveryNotice>
    suspend fun findDeliveryNoticeById(noticeId: String, tenantId: String): DomainResult<VendorPortalDeliveryNotice?>
    suspend fun findDeliveryNoticeByNumber(noticeNumber: String, tenantId: String): DomainResult<VendorPortalDeliveryNotice?>
    suspend fun listDeliveryNotices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String? = null,
        status: VendorPortalDeliveryNoticeStatus? = null
    ): DomainResult<List<VendorPortalDeliveryNotice>>

    // Delivery Acknowledgement
    suspend fun saveDeliveryAcknowledgement(ack: VendorPortalDeliveryAcknowledgement): DomainResult<VendorPortalDeliveryAcknowledgement>
    suspend fun findDeliveryAcknowledgement(noticeId: String, tenantId: String): DomainResult<VendorPortalDeliveryAcknowledgement?>

    // Quality Responses
    suspend fun saveQualityResponse(response: VendorPortalQualityResponse): DomainResult<VendorPortalQualityResponse>
    suspend fun findQualityResponseById(responseId: String, tenantId: String): DomainResult<VendorPortalQualityResponse?>
    suspend fun listQualityResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String? = null,
        rejectionId: String? = null
    ): DomainResult<List<VendorPortalQualityResponse>>

    // Delivery & Quality Exceptions
    suspend fun saveException(exception: VendorPortalDeliveryException): DomainResult<VendorPortalDeliveryException>
    suspend fun updateException(exception: VendorPortalDeliveryException): DomainResult<VendorPortalDeliveryException>
    suspend fun findExceptionById(exceptionId: String, tenantId: String): DomainResult<VendorPortalDeliveryException?>
    suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDeliveryExceptionStatus? = null,
        sourceType: String? = null
    ): DomainResult<List<VendorPortalDeliveryException>>

    // Evidence Metadata
    suspend fun saveEvidence(evidence: VendorPortalDeliveryEvidence): DomainResult<VendorPortalDeliveryEvidence>
    suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalDeliveryEvidence>>

    // Audit Events
    suspend fun recordAuditEvent(event: VendorPortalDeliveryAuditEvent): DomainResult<VendorPortalDeliveryAuditEvent>
    suspend fun listAuditEvents(
        tenantId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalDeliveryAuditEvent>>
}
