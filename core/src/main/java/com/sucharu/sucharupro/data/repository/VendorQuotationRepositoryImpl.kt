package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorQuotationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorQuotationRepository

class VendorQuotationRepositoryImpl(
    private val dataSource: VendorQuotationDataSource
) : VendorQuotationRepository {

    override suspend fun createQuotation(quotation: VendorQuotation): DomainResult<VendorQuotation> =
        dataSource.insertQuotation(quotation)

    override suspend fun updateQuotation(quotation: VendorQuotation): DomainResult<VendorQuotation> =
        dataSource.updateQuotation(quotation)

    override suspend fun findQuotationById(quotationId: String, tenantId: String): DomainResult<VendorQuotation> =
        dataSource.fetchQuotationById(quotationId, tenantId)

    override suspend fun findQuotationByRfqAndVendor(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorQuotation?> =
        dataSource.fetchQuotationByRfqAndVendor(rfqId, vendorId, tenantId)

    override suspend fun listQuotationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorQuotation>> =
        dataSource.listQuotationsByRfq(rfqId, tenantId)

    override suspend fun listQuotationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorQuotation>> =
        dataSource.listQuotationsByVendor(vendorId, tenantId)

    override suspend fun recordRevision(revision: VendorQuotationRevision): DomainResult<VendorQuotationRevision> =
        dataSource.insertRevision(revision)

    override suspend fun listRevisionsByQuotation(quotationId: String, tenantId: String): DomainResult<List<VendorQuotationRevision>> =
        dataSource.listRevisionsByQuotation(quotationId, tenantId)

    override suspend fun saveEvaluation(evaluation: VendorRfqEvaluation): DomainResult<VendorRfqEvaluation> {
        val existing = dataSource.fetchEvaluationByQuotation(evaluation.quotationId, evaluation.tenantId)
        return if (existing is DomainResult.Success && existing.data != null) {
            dataSource.updateEvaluation(evaluation)
        } else {
            dataSource.insertEvaluation(evaluation)
        }
    }

    override suspend fun findEvaluationByQuotation(quotationId: String, tenantId: String): DomainResult<VendorRfqEvaluation?> =
        dataSource.fetchEvaluationByQuotation(quotationId, tenantId)

    override suspend fun listEvaluationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorRfqEvaluation>> =
        dataSource.listEvaluationsByRfq(rfqId, tenantId)
}
