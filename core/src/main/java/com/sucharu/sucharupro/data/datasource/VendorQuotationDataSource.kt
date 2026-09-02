package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorQuotationDataSource {
    suspend fun insertQuotation(quotation: VendorQuotation): DomainResult<VendorQuotation>
    suspend fun updateQuotation(quotation: VendorQuotation): DomainResult<VendorQuotation>
    suspend fun fetchQuotationById(quotationId: String, tenantId: String): DomainResult<VendorQuotation>
    suspend fun fetchQuotationByRfqAndVendor(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorQuotation?>
    suspend fun listQuotationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorQuotation>>
    suspend fun listQuotationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorQuotation>>

    // Revisions
    suspend fun insertRevision(revision: VendorQuotationRevision): DomainResult<VendorQuotationRevision>
    suspend fun listRevisionsByQuotation(quotationId: String, tenantId: String): DomainResult<List<VendorQuotationRevision>>

    // Evaluation
    suspend fun insertEvaluation(evaluation: VendorRfqEvaluation): DomainResult<VendorRfqEvaluation>
    suspend fun updateEvaluation(evaluation: VendorRfqEvaluation): DomainResult<VendorRfqEvaluation>
    suspend fun fetchEvaluationByQuotation(quotationId: String, tenantId: String): DomainResult<VendorRfqEvaluation?>
    suspend fun listEvaluationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorRfqEvaluation>>
}
