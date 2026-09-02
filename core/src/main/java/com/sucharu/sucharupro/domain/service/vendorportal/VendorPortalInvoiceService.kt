package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceMatchStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Service interface for Vendor Portal Invoice, Billing & Payment Collaboration (Module 13 Step 06).
 */
interface VendorPortalInvoiceService {

    // Invoice Projections
    suspend fun listInvoices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorInvoiceStatus? = null,
        matchStatus: VendorInvoiceMatchStatus? = null
    ): DomainResult<List<VendorPortalInvoiceSummary>>

    suspend fun getInvoiceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): DomainResult<VendorPortalInvoiceSummary>

    suspend fun getThreeWayMatch(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): DomainResult<VendorPortalInvoiceMatchSummary>

    // Vendor Invoice Submissions
    suspend fun createInvoiceSubmission(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String,
        vendorInvoiceNumber: String,
        invoiceDate: Long,
        currency: String = "BDT",
        shippingAmount: java.math.BigDecimal? = null,
        otherCharges: java.math.BigDecimal? = null,
        notes: String? = null,
        items: List<VendorPortalInvoiceSubmissionItemInput>,
        actorId: String
    ): DomainResult<VendorPortalInvoiceSubmission>

    suspend fun getInvoiceSubmission(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String
    ): DomainResult<VendorPortalInvoiceSubmission>

    suspend fun listInvoiceSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String? = null,
        status: VendorPortalInvoiceSubmissionStatus? = null
    ): DomainResult<List<VendorPortalInvoiceSubmission>>

    suspend fun submitInvoiceSubmission(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String,
        actorId: String
    ): DomainResult<VendorPortalInvoiceSubmission>

    suspend fun cancelInvoiceSubmission(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String,
        reason: String,
        actorId: String
    ): DomainResult<VendorPortalInvoiceSubmission>

    // Responses & Clarifications
    suspend fun respondToInvoice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String,
        exceptionId: String? = null,
        responseType: VendorPortalInvoiceResponseType,
        comment: String,
        proposedCorrection: String? = null,
        evidenceReferences: List<String> = emptyList(),
        actorId: String
    ): DomainResult<VendorPortalInvoiceResponse>

    suspend fun listInvoiceResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): DomainResult<List<VendorPortalInvoiceResponse>>

    // Financial Evidence
    suspend fun uploadFinancialEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String,
        evidenceType: VendorPortalFinancialEvidenceType,
        filename: String,
        fileReference: String,
        mimeType: String = "application/pdf",
        sizeBytes: Long,
        actorId: String
    ): DomainResult<VendorPortalFinancialEvidence>

    suspend fun listFinancialEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String? = null,
        entityId: String? = null
    ): DomainResult<List<VendorPortalFinancialEvidence>>

    // Payments & Settlement Projections
    suspend fun listPayments(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalPaymentStatus? = null
    ): DomainResult<List<VendorPortalPaymentSummary>>

    suspend fun getPaymentById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String
    ): DomainResult<VendorPortalPaymentSummary>

    // Financial KPI Summary & Activity
    suspend fun getFinancialSummary(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalFinancialKpiSummary>

    suspend fun getFinancialActivityTimeline(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String? = null
    ): DomainResult<List<VendorPortalFinancialActivity>>
}
