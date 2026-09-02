package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap

class FakeVendorQuotationDataSource : VendorQuotationDataSource {

    private val quotations = ConcurrentHashMap<String, VendorQuotation>()
    private val revisions = ConcurrentHashMap<String, MutableList<VendorQuotationRevision>>()
    private val evaluations = ConcurrentHashMap<String, VendorRfqEvaluation>()

    override suspend fun insertQuotation(quotation: VendorQuotation): DomainResult<VendorQuotation> {
        val existing = quotations.values.firstOrNull {
            it.tenantId == quotation.tenantId && it.rfqId == quotation.rfqId && it.vendorId == quotation.vendorId
        }
        if (existing != null) {
            return DomainResult.Error(IllegalStateException("Vendor '${quotation.vendorId}' has already created quotation '${existing.quotationId}' for RFQ '${quotation.rfqId}'."))
        }
        quotations[quotation.quotationId] = quotation
        return DomainResult.Success(quotation)
    }

    override suspend fun updateQuotation(quotation: VendorQuotation): DomainResult<VendorQuotation> {
        val current = quotations[quotation.quotationId]
            ?: return DomainResult.Error(NoSuchElementException("Quotation '${quotation.quotationId}' not found."))
        if (current.version != quotation.version - 1 && current.version != quotation.version) {
            return DomainResult.Error(IllegalStateException("Optimistic lock conflict on Quotation '${quotation.quotationId}'."))
        }
        quotations[quotation.quotationId] = quotation
        return DomainResult.Success(quotation)
    }

    override suspend fun fetchQuotationById(quotationId: String, tenantId: String): DomainResult<VendorQuotation> {
        val q = quotations[quotationId]
        return if (q != null && q.tenantId == tenantId) {
            DomainResult.Success(q)
        } else {
            DomainResult.Error(NoSuchElementException("Quotation '$quotationId' not found."))
        }
    }

    override suspend fun fetchQuotationByRfqAndVendor(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorQuotation?> {
        val q = quotations.values.firstOrNull { it.tenantId == tenantId && it.rfqId == rfqId && it.vendorId == vendorId }
        return DomainResult.Success(q)
    }

    override suspend fun listQuotationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorQuotation>> {
        val list = quotations.values.filter { it.tenantId == tenantId && it.rfqId == rfqId }.sortedBy { it.grandTotal.amount }
        return DomainResult.Success(list)
    }

    override suspend fun listQuotationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorQuotation>> {
        val list = quotations.values.filter { it.tenantId == tenantId && it.vendorId == vendorId }.sortedByDescending { it.createdAt }
        return DomainResult.Success(list)
    }

    override suspend fun insertRevision(revision: VendorQuotationRevision): DomainResult<VendorQuotationRevision> {
        val list = revisions.getOrPut(revision.quotationId) { mutableListOf() }
        list.add(revision)
        return DomainResult.Success(revision)
    }

    override suspend fun listRevisionsByQuotation(quotationId: String, tenantId: String): DomainResult<List<VendorQuotationRevision>> {
        val list = revisions[quotationId]?.filter { it.tenantId == tenantId }?.sortedBy { it.revisionNumber } ?: emptyList()
        return DomainResult.Success(list)
    }

    override suspend fun insertEvaluation(evaluation: VendorRfqEvaluation): DomainResult<VendorRfqEvaluation> {
        val key = "${evaluation.rfqId}_${evaluation.quotationId}"
        if (evaluations.containsKey(key)) {
            return DomainResult.Error(IllegalStateException("Evaluation for quotation '${evaluation.quotationId}' already exists."))
        }
        evaluations[key] = evaluation
        return DomainResult.Success(evaluation)
    }

    override suspend fun updateEvaluation(evaluation: VendorRfqEvaluation): DomainResult<VendorRfqEvaluation> {
        val key = "${evaluation.rfqId}_${evaluation.quotationId}"
        if (!evaluations.containsKey(key)) {
            return DomainResult.Error(NoSuchElementException("Evaluation '${evaluation.evaluationId}' not found."))
        }
        evaluations[key] = evaluation
        return DomainResult.Success(evaluation)
    }

    override suspend fun fetchEvaluationByQuotation(quotationId: String, tenantId: String): DomainResult<VendorRfqEvaluation?> {
        val eval = evaluations.values.firstOrNull { it.tenantId == tenantId && it.quotationId == quotationId }
        return DomainResult.Success(eval)
    }

    override suspend fun listEvaluationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorRfqEvaluation>> {
        val list = evaluations.values.filter { it.tenantId == tenantId && it.rfqId == rfqId }.sortedByDescending { it.totalScore }
        return DomainResult.Success(list)
    }
}
