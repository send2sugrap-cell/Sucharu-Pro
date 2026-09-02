package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory data source for test suites and mock environments (Module 13 Step 06).
 */
class FakeVendorPortalInvoiceDataSource : VendorPortalInvoiceDataSource {

    private val submissions = ConcurrentHashMap<String, VendorPortalInvoiceSubmission>()
    private val responses = ConcurrentHashMap<String, VendorPortalInvoiceResponse>()
    private val evidenceRecords = ConcurrentHashMap<String, VendorPortalFinancialEvidence>()
    private val auditEvents = mutableListOf<VendorPortalInvoiceAuditEvent>()
    private val lock = Any()

    override suspend fun saveSubmission(submission: VendorPortalInvoiceSubmission): VendorPortalInvoiceSubmission {
        val key = "${submission.tenantId}:${submission.submissionId}"
        submissions[key] = submission
        return submission
    }

    override suspend fun findSubmissionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String
    ): VendorPortalInvoiceSubmission? {
        val sub = submissions["$tenantId:$submissionId"] ?: return null
        return if (sub.projectId == projectId && sub.vendorId == vendorId) sub else null
    }

    override suspend fun listSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String?,
        status: VendorPortalInvoiceSubmissionStatus?
    ): List<VendorPortalInvoiceSubmission> {
        return submissions.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { purchaseOrderId == null || it.purchaseOrderId == purchaseOrderId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun saveResponse(response: VendorPortalInvoiceResponse): VendorPortalInvoiceResponse {
        val key = "${response.tenantId}:${response.responseId}"
        responses[key] = response
        return response
    }

    override suspend fun listResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): List<VendorPortalInvoiceResponse> {
        return responses.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId && it.invoiceId == invoiceId }
            .sortedByDescending { it.respondedAt }
    }

    override suspend fun saveEvidence(evidence: VendorPortalFinancialEvidence): VendorPortalFinancialEvidence {
        val key = "${evidence.tenantId}:${evidence.evidenceId}"
        evidenceRecords[key] = evidence
        return evidence
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): List<VendorPortalFinancialEvidence> {
        return evidenceRecords.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
            .filter { entityType == null || it.entityType == entityType }
            .filter { entityId == null || it.entityId == entityId }
            .sortedByDescending { it.uploadedAt }
    }

    override suspend fun recordAuditEvent(event: VendorPortalInvoiceAuditEvent) {
        synchronized(lock) {
            auditEvents.add(event)
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        targetType: String?,
        targetId: String?
    ): List<VendorPortalInvoiceAuditEvent> {
        return synchronized(lock) {
            auditEvents
                .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
                .filter { targetType == null || it.targetType == targetType }
                .filter { targetId == null || it.targetId == targetId }
                .sortedByDescending { it.createdAt }
        }
    }
}
