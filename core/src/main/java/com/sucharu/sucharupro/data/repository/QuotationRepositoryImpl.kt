package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.QuotationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.domain.repository.QuotationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Production-ready implementation of [QuotationRepository] delegating to [QuotationDataSource].
 */
class QuotationRepositoryImpl(
    private val dataSource: QuotationDataSource
) : QuotationRepository {

    override fun getQuotations(): Flow<List<Quotation>> = dataSource.observeQuotations()

    override fun getQuotationById(quotationId: String): Flow<Quotation?> {
        return dataSource.observeQuotations().map { list ->
            list.find { it.quotationId == quotationId }
        }
    }

    override suspend fun findQuotationById(quotationId: String): DomainResult<Quotation> {
        return dataSource.fetchQuotationById(quotationId)
    }

    override fun getQuotationsForCustomer(customerId: String): Flow<List<Quotation>> {
        return dataSource.observeQuotations().map { list ->
            list.filter { it.customerId == customerId }
        }
    }

    override fun getQuotationsForInquiry(inquiryId: String): Flow<List<Quotation>> {
        return dataSource.observeQuotations().map { list ->
            list.filter { it.inquiryId == inquiryId }
        }
    }

    override suspend fun createQuotation(quotation: Quotation): DomainResult<Quotation> {
        if (quotation.quotationId.isBlank()) {
            return DomainResult.Error(message = "Quotation ID cannot be blank.")
        }
        if (quotation.quotationNumber.isBlank()) {
            return DomainResult.Error(message = "Quotation Number cannot be blank.")
        }
        if (quotation.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        if (quotation.revisions.isEmpty()) {
            return DomainResult.Error(message = "Quotation must contain at least one revision.")
        }
        return dataSource.insertQuotation(quotation)
    }

    override suspend fun updateQuotation(quotation: Quotation): DomainResult<Quotation> {
        if (quotation.quotationId.isBlank()) {
            return DomainResult.Error(message = "Quotation ID cannot be blank.")
        }
        return dataSource.updateQuotation(quotation)
    }

    override suspend fun updateQuotationStatus(
        quotationId: String,
        status: QuotationStatusType
    ): DomainResult<Quotation> {
        if (quotationId.isBlank()) {
            return DomainResult.Error(message = "Quotation ID cannot be blank.")
        }
        return dataSource.updateQuotationStatus(quotationId, status)
    }

    override suspend fun deleteQuotation(quotationId: String): DomainResult<Unit> {
        if (quotationId.isBlank()) {
            return DomainResult.Error(message = "Quotation ID cannot be blank.")
        }
        return dataSource.deleteQuotation(quotationId)
    }

    override fun getQuotationRevisions(quotationId: String): Flow<List<QuotationRevision>> {
        return dataSource.observeQuotations().map { list ->
            val quotation = list.find { it.quotationId == quotationId }
            quotation?.revisions ?: emptyList()
        }
    }

    override suspend fun findQuotationRevision(
        quotationId: String,
        revisionId: String
    ): DomainResult<QuotationRevision> {
        return when (val qResult = dataSource.fetchQuotationById(quotationId)) {
            is DomainResult.Success -> {
                val rev = qResult.data.revisions.find { it.revisionId == revisionId }
                if (rev != null) {
                    DomainResult.Success(rev)
                } else {
                    DomainResult.Error(message = "Revision '$revisionId' not found for quotation '$quotationId'.")
                }
            }
            is DomainResult.Error -> DomainResult.Error(message = qResult.message, exception = qResult.exception)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun createQuotationRevision(
        quotationId: String,
        revision: QuotationRevision
    ): DomainResult<QuotationRevision> {
        if (quotationId.isBlank()) {
            return DomainResult.Error(message = "Quotation ID cannot be blank.")
        }
        if (revision.revisionId.isBlank()) {
            return DomainResult.Error(message = "Revision ID cannot be blank.")
        }
        if (revision.quotationId != quotationId) {
            return DomainResult.Error(message = "Revision quotationId '${revision.quotationId}' does not match target quotationId '$quotationId'.")
        }
        return dataSource.insertQuotationRevision(quotationId, revision)
    }

    override suspend fun getLatestQuotationRevision(quotationId: String): QuotationRevision? {
        return when (val result = dataSource.fetchQuotationById(quotationId)) {
            is DomainResult.Success -> result.data.currentRevision
            else -> null
        }
    }

    override suspend fun getApprovedQuotationRevision(quotationId: String): QuotationRevision? {
        return when (val result = dataSource.fetchQuotationById(quotationId)) {
            is DomainResult.Success -> {
                val q = result.data
                if (q.isApproved && q.approvedRevisionId != null) {
                    q.revisions.find { it.revisionId == q.approvedRevisionId }
                } else null
            }
            else -> null
        }
    }

    override suspend fun approveQuotationRevision(
        quotationId: String,
        revisionId: String,
        approvedBy: String,
        timestamp: String
    ): DomainResult<Quotation> {
        if (quotationId.isBlank()) {
            return DomainResult.Error(message = "Quotation ID cannot be blank.")
        }
        if (revisionId.isBlank()) {
            return DomainResult.Error(message = "Revision ID cannot be blank.")
        }
        if (approvedBy.isBlank()) {
            return DomainResult.Error(message = "ApprovedBy cannot be blank.")
        }
        return dataSource.approveQuotationRevision(quotationId, revisionId, approvedBy, timestamp)
    }
}
