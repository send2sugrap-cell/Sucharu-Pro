package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Commercial Quotations and Revisions in Sucharu Pro.
 */
interface QuotationRepository {

    /** Reactive stream of all commercial quotations. */
    fun getQuotations(): Flow<List<Quotation>>

    /** Reactive stream observing a single quotation by [quotationId]. */
    fun getQuotationById(quotationId: String): Flow<Quotation?>

    /** Direct lookup of a quotation by [quotationId]. */
    suspend fun findQuotationById(quotationId: String): DomainResult<Quotation>

    /** Reactive stream of quotations for a specific customer. */
    fun getQuotationsForCustomer(customerId: String): Flow<List<Quotation>>

    /** Reactive stream of quotations originating from a specific inquiry. */
    fun getQuotationsForInquiry(inquiryId: String): Flow<List<Quotation>>

    /** Creates a new quotation with its initial revision. */
    suspend fun createQuotation(quotation: Quotation): DomainResult<Quotation>

    /** Updates an existing quotation. */
    suspend fun updateQuotation(quotation: Quotation): DomainResult<Quotation>

    /** Updates the lifecycle status of a quotation following domain transition rules. */
    suspend fun updateQuotationStatus(quotationId: String, status: QuotationStatusType): DomainResult<Quotation>

    /** Deletes a quotation only if unapproved and not linked to confirmed orders. */
    suspend fun deleteQuotation(quotationId: String): DomainResult<Unit>

    // ------------------------------------------------------------------------
    // Revision Management
    // ------------------------------------------------------------------------

    /** Reactive stream of historical revisions for a given quotation. */
    fun getQuotationRevisions(quotationId: String): Flow<List<QuotationRevision>>

    /** Direct lookup of a specific revision of a quotation. */
    suspend fun findQuotationRevision(quotationId: String, revisionId: String): DomainResult<QuotationRevision>

    /** Adds a new revision to an existing quotation, updating the quotation's current revision. */
    suspend fun createQuotationRevision(quotationId: String, revision: QuotationRevision): DomainResult<QuotationRevision>

    /** Returns the latest revision for a quotation. */
    suspend fun getLatestQuotationRevision(quotationId: String): QuotationRevision?

    /** Returns the approved revision for a quotation, if any. */
    suspend fun getApprovedQuotationRevision(quotationId: String): QuotationRevision?

    /**
     * Formally approves a specific revision of a quotation, marking the quotation as APPROVED.
     */
    suspend fun approveQuotationRevision(
        quotationId: String,
        revisionId: String,
        approvedBy: String,
        timestamp: String
    ): DomainResult<Quotation>
}
