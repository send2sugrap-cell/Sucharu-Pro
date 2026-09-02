package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Repository interface for Vendor Portal Invoice, Responses, Evidence, and Audit operations (Module 13 Step 06).
 */
interface VendorPortalInvoiceRepository {

    // Submissions
    suspend fun saveSubmission(submission: VendorPortalInvoiceSubmission): DomainResult<VendorPortalInvoiceSubmission>
    suspend fun findSubmissionById(tenantId: String, projectId: String, vendorId: String, submissionId: String): DomainResult<VendorPortalInvoiceSubmission?>
    suspend fun listSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String? = null,
        status: VendorPortalInvoiceSubmissionStatus? = null
    ): DomainResult<List<VendorPortalInvoiceSubmission>>

    // Responses
    suspend fun saveResponse(response: VendorPortalInvoiceResponse): DomainResult<VendorPortalInvoiceResponse>
    suspend fun listResponses(tenantId: String, projectId: String, vendorId: String, invoiceId: String): DomainResult<List<VendorPortalInvoiceResponse>>

    // Financial Evidence
    suspend fun saveEvidence(evidence: VendorPortalFinancialEvidence): DomainResult<VendorPortalFinancialEvidence>
    suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String? = null,
        entityId: String? = null
    ): DomainResult<List<VendorPortalFinancialEvidence>>

    // Audit Events
    suspend fun recordAuditEvent(event: VendorPortalInvoiceAuditEvent): DomainResult<Unit>
    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        targetType: String? = null,
        targetId: String? = null
    ): DomainResult<List<VendorPortalInvoiceAuditEvent>>
}
