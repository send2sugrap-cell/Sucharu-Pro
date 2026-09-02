package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Fake Data Source for testing Vendor Portal Delivery Collaboration (Module 13 Step 05).
 */
class FakeVendorPortalDeliveryDataSource : VendorPortalDeliveryDataSource {

    private val notices = ConcurrentHashMap<String, VendorPortalDeliveryNotice>()
    private val acknowledgements = ConcurrentHashMap<String, VendorPortalDeliveryAcknowledgement>()
    private val qualityResponses = ConcurrentHashMap<String, VendorPortalQualityResponse>()
    private val exceptions = ConcurrentHashMap<String, VendorPortalDeliveryException>()
    private val evidenceList = ConcurrentHashMap<String, VendorPortalDeliveryEvidence>()
    private val auditEvents = mutableListOf<VendorPortalDeliveryAuditEvent>()

    override suspend fun saveDeliveryNotice(notice: VendorPortalDeliveryNotice): VendorPortalDeliveryNotice {
        notices[notice.noticeId] = notice
        return notice
    }

    override suspend fun updateDeliveryNotice(notice: VendorPortalDeliveryNotice): VendorPortalDeliveryNotice {
        notices[notice.noticeId] = notice.copy(version = notice.version + 1, updatedAt = System.currentTimeMillis())
        return notices[notice.noticeId]!!
    }

    override suspend fun findDeliveryNoticeById(noticeId: String, tenantId: String): VendorPortalDeliveryNotice? {
        val n = notices[noticeId]
        return if (n != null && n.tenantId == tenantId) n else null
    }

    override suspend fun findDeliveryNoticeByNumber(noticeNumber: String, tenantId: String): VendorPortalDeliveryNotice? {
        return notices.values.find { it.noticeNumber == noticeNumber && it.tenantId == tenantId }
    }

    override suspend fun listDeliveryNotices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String?,
        status: VendorPortalDeliveryNoticeStatus?
    ): List<VendorPortalDeliveryNotice> {
        return notices.values.filter { n ->
            n.tenantId == tenantId &&
            n.vendorId == vendorId &&
            (purchaseOrderId == null || n.purchaseOrderId == purchaseOrderId) &&
            (status == null || n.status == status)
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun saveDeliveryAcknowledgement(ack: VendorPortalDeliveryAcknowledgement): VendorPortalDeliveryAcknowledgement {
        acknowledgements[ack.noticeId] = ack
        return ack
    }

    override suspend fun findDeliveryAcknowledgement(noticeId: String, tenantId: String): VendorPortalDeliveryAcknowledgement? {
        val a = acknowledgements[noticeId]
        return if (a != null && a.tenantId == tenantId) a else null
    }

    override suspend fun saveQualityResponse(response: VendorPortalQualityResponse): VendorPortalQualityResponse {
        qualityResponses[response.responseId] = response
        return response
    }

    override suspend fun findQualityResponseById(responseId: String, tenantId: String): VendorPortalQualityResponse? {
        val qr = qualityResponses[responseId]
        return if (qr != null && qr.tenantId == tenantId) qr else null
    }

    override suspend fun listQualityResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String?,
        rejectionId: String?
    ): List<VendorPortalQualityResponse> {
        return qualityResponses.values.filter { qr ->
            qr.tenantId == tenantId &&
            qr.vendorId == vendorId &&
            (inspectionId == null || qr.inspectionId == inspectionId) &&
            (rejectionId == null || qr.rejectionId == rejectionId)
        }.sortedByDescending { it.respondedAt }
    }

    override suspend fun saveException(exception: VendorPortalDeliveryException): VendorPortalDeliveryException {
        exceptions[exception.exceptionId] = exception
        return exception
    }

    override suspend fun updateException(exception: VendorPortalDeliveryException): VendorPortalDeliveryException {
        exceptions[exception.exceptionId] = exception.copy(version = exception.version + 1)
        return exceptions[exception.exceptionId]!!
    }

    override suspend fun findExceptionById(exceptionId: String, tenantId: String): VendorPortalDeliveryException? {
        val ex = exceptions[exceptionId]
        return if (ex != null && ex.tenantId == tenantId) ex else null
    }

    override suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDeliveryExceptionStatus?,
        sourceType: String?
    ): List<VendorPortalDeliveryException> {
        return exceptions.values.filter { ex ->
            ex.tenantId == tenantId &&
            ex.vendorId == vendorId &&
            (status == null || ex.status == status) &&
            (sourceType == null || ex.sourceType == sourceType)
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun saveEvidence(evidence: VendorPortalDeliveryEvidence): VendorPortalDeliveryEvidence {
        evidenceList[evidence.evidenceId] = evidence
        return evidence
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): List<VendorPortalDeliveryEvidence> {
        return evidenceList.values.filter { ev ->
            ev.tenantId == tenantId &&
            ev.vendorId == vendorId &&
            ev.entityType == entityType &&
            ev.entityId == entityId
        }.sortedByDescending { it.uploadedAt }
    }

    override suspend fun recordAuditEvent(event: VendorPortalDeliveryAuditEvent): VendorPortalDeliveryAuditEvent {
        synchronized(auditEvents) {
            auditEvents.add(event)
        }
        return event
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        entityType: String,
        entityId: String
    ): List<VendorPortalDeliveryAuditEvent> {
        synchronized(auditEvents) {
            return auditEvents.filter {
                it.tenantId == tenantId &&
                it.entityType == entityType &&
                it.entityId == entityId
            }.sortedBy { it.createdAt }
        }
    }
}
