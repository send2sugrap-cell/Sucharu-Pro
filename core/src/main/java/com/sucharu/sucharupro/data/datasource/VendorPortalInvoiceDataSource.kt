package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Data source interface for Vendor Portal Invoice & Financial tables (Module 13 Step 06).
 */
interface VendorPortalInvoiceDataSource {

    suspend fun saveSubmission(submission: VendorPortalInvoiceSubmission): VendorPortalInvoiceSubmission
    suspend fun findSubmissionById(tenantId: String, projectId: String, vendorId: String, submissionId: String): VendorPortalInvoiceSubmission?
    suspend fun listSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String? = null,
        status: VendorPortalInvoiceSubmissionStatus? = null
    ): List<VendorPortalInvoiceSubmission>

    suspend fun saveResponse(response: VendorPortalInvoiceResponse): VendorPortalInvoiceResponse
    suspend fun listResponses(tenantId: String, projectId: String, vendorId: String, invoiceId: String): List<VendorPortalInvoiceResponse>

    suspend fun saveEvidence(evidence: VendorPortalFinancialEvidence): VendorPortalFinancialEvidence
    suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String? = null,
        entityId: String? = null
    ): List<VendorPortalFinancialEvidence>

    suspend fun recordAuditEvent(event: VendorPortalInvoiceAuditEvent)
    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        targetType: String? = null,
        targetId: String? = null
    ): List<VendorPortalInvoiceAuditEvent>
}
