package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Data source contract for persisting vendor portal delivery, quality, and exception data.
 */
interface VendorPortalDeliveryDataSource {

    // Delivery Notices
    suspend fun saveDeliveryNotice(notice: VendorPortalDeliveryNotice): VendorPortalDeliveryNotice
    suspend fun updateDeliveryNotice(notice: VendorPortalDeliveryNotice): VendorPortalDeliveryNotice
    suspend fun findDeliveryNoticeById(noticeId: String, tenantId: String): VendorPortalDeliveryNotice?
    suspend fun findDeliveryNoticeByNumber(noticeNumber: String, tenantId: String): VendorPortalDeliveryNotice?
    suspend fun listDeliveryNotices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String? = null,
        status: VendorPortalDeliveryNoticeStatus? = null
    ): List<VendorPortalDeliveryNotice>

    // Acknowledgements
    suspend fun saveDeliveryAcknowledgement(ack: VendorPortalDeliveryAcknowledgement): VendorPortalDeliveryAcknowledgement
    suspend fun findDeliveryAcknowledgement(noticeId: String, tenantId: String): VendorPortalDeliveryAcknowledgement?

    // Quality Responses
    suspend fun saveQualityResponse(response: VendorPortalQualityResponse): VendorPortalQualityResponse
    suspend fun findQualityResponseById(responseId: String, tenantId: String): VendorPortalQualityResponse?
    suspend fun listQualityResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String? = null,
        rejectionId: String? = null
    ): List<VendorPortalQualityResponse>

    // Exceptions
    suspend fun saveException(exception: VendorPortalDeliveryException): VendorPortalDeliveryException
    suspend fun updateException(exception: VendorPortalDeliveryException): VendorPortalDeliveryException
    suspend fun findExceptionById(exceptionId: String, tenantId: String): VendorPortalDeliveryException?
    suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDeliveryExceptionStatus? = null,
        sourceType: String? = null
    ): List<VendorPortalDeliveryException>

    // Evidence
    suspend fun saveEvidence(evidence: VendorPortalDeliveryEvidence): VendorPortalDeliveryEvidence
    suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): List<VendorPortalDeliveryEvidence>

    // Audit Events
    suspend fun recordAuditEvent(event: VendorPortalDeliveryAuditEvent): VendorPortalDeliveryAuditEvent
    suspend fun listAuditEvents(
        tenantId: String,
        entityType: String,
        entityId: String
    ): List<VendorPortalDeliveryAuditEvent>
}
