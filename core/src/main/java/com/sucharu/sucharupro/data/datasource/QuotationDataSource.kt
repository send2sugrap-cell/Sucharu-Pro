package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Commercial Quotations and Revisions in Sucharu Pro.
 */
interface QuotationDataSource {

    /** Continuous reactive stream of all quotations. */
    fun observeQuotations(): Flow<List<Quotation>>

    /** One-shot fetch of a quotation by ID. */
    suspend fun fetchQuotationById(quotationId: String): DomainResult<Quotation>

    /** Inserts a new quotation. */
    suspend fun insertQuotation(quotation: Quotation): DomainResult<Quotation>

    /** Updates an existing quotation. */
    suspend fun updateQuotation(quotation: Quotation): DomainResult<Quotation>

    /** Updates the status of a quotation. */
    suspend fun updateQuotationStatus(quotationId: String, status: QuotationStatusType): DomainResult<Quotation>

    /** Deletes a quotation by ID. */
    suspend fun deleteQuotation(quotationId: String): DomainResult<Unit>

    /** Inserts a new revision into an existing quotation. */
    suspend fun insertQuotationRevision(quotationId: String, revision: QuotationRevision): DomainResult<QuotationRevision>

    /** Formally approves a specific revision of a quotation. */
    suspend fun approveQuotationRevision(
        quotationId: String,
        revisionId: String,
        approvedBy: String,
        timestamp: String
    ): DomainResult<Quotation>
}
