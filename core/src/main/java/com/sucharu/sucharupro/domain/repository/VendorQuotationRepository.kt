package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorQuotationRepository {
    suspend fun createQuotation(quotation: VendorQuotation): DomainResult<VendorQuotation>
    suspend fun updateQuotation(quotation: VendorQuotation): DomainResult<VendorQuotation>
    suspend fun findQuotationById(quotationId: String, tenantId: String): DomainResult<VendorQuotation>
    suspend fun findQuotationByRfqAndVendor(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorQuotation?>
    suspend fun listQuotationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorQuotation>>
    suspend fun listQuotationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorQuotation>>

    // Revisions
    suspend fun recordRevision(revision: VendorQuotationRevision): DomainResult<VendorQuotationRevision>
    suspend fun listRevisionsByQuotation(quotationId: String, tenantId: String): DomainResult<List<VendorQuotationRevision>>

    // Evaluation
    suspend fun saveEvaluation(evaluation: VendorRfqEvaluation): DomainResult<VendorRfqEvaluation>
    suspend fun findEvaluationByQuotation(quotationId: String, tenantId: String): DomainResult<VendorRfqEvaluation?>
    suspend fun listEvaluationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorRfqEvaluation>>
}
