package com.sucharu.sucharupro.data.repository.printingquote

import com.sucharu.sucharupro.data.datasource.printingquote.PrintingQuoteDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingquote.*
import com.sucharu.sucharupro.domain.repository.printingquote.PrintingQuoteRepository

/**
 * Repository implementation for the Smart Printing Calculator quotation layer.
 * Delegates all persistence to [PrintingQuoteDataSource].
 * Module 17 Step 02.
 */
class PrintingQuoteRepositoryImpl(
    private val dataSource: PrintingQuoteDataSource
) : PrintingQuoteRepository {

    // ─── Quote Header ─────────────────────────────────────────────────────────

    override suspend fun saveQuote(quote: PrintingQuote): DomainResult<PrintingQuote> =
        dataSource.insertQuote(quote)

    override suspend fun updateQuote(quote: PrintingQuote): DomainResult<PrintingQuote> =
        dataSource.updateQuote(quote)

    override suspend fun findQuoteById(tenantId: String, quoteId: String): DomainResult<PrintingQuote?> =
        dataSource.selectQuoteById(tenantId, quoteId)

    override suspend fun findQuoteByIdempotencyKey(tenantId: String, key: String): DomainResult<PrintingQuote?> =
        dataSource.selectQuoteByIdempotencyKey(tenantId, key)

    override suspend fun listQuotes(tenantId: String, limit: Int): DomainResult<List<PrintingQuote>> =
        dataSource.selectQuotes(tenantId, limit)

    override suspend fun listQuotesByCalculationId(
        tenantId: String, calculationId: String
    ): DomainResult<List<PrintingQuote>> =
        dataSource.selectQuotesByCalculationId(tenantId, calculationId)

    // ─── Quote Versions ───────────────────────────────────────────────────────

    override suspend fun saveQuoteVersion(version: PrintingQuoteVersion): DomainResult<PrintingQuoteVersion> =
        if (version.isApproved) dataSource.updateQuoteVersion(version)
        else dataSource.insertQuoteVersion(version)

    override suspend fun findVersionById(tenantId: String, versionId: String): DomainResult<PrintingQuoteVersion?> =
        dataSource.selectVersionById(tenantId, versionId)

    override suspend fun listVersionsByQuoteId(tenantId: String, quoteId: String): DomainResult<List<PrintingQuoteVersion>> =
        dataSource.selectVersionsByQuoteId(tenantId, quoteId)

    // ─── Cost Components ──────────────────────────────────────────────────────

    override suspend fun saveCostComponents(
        components: List<PrintingCostComponent>
    ): DomainResult<List<PrintingCostComponent>> =
        dataSource.insertCostComponents(components)

    override suspend fun listCostComponents(
        tenantId: String, versionId: String
    ): DomainResult<List<PrintingCostComponent>> =
        dataSource.selectCostComponents(tenantId, versionId)

    // ─── Quantity Tiers ───────────────────────────────────────────────────────

    override suspend fun saveQuantityTiers(
        tiers: List<PrintingQuantityTier>
    ): DomainResult<List<PrintingQuantityTier>> =
        dataSource.insertQuantityTiers(tiers)

    override suspend fun listQuantityTiers(
        tenantId: String, versionId: String
    ): DomainResult<List<PrintingQuantityTier>> =
        dataSource.selectQuantityTiers(tenantId, versionId)

    // ─── Audit Events ─────────────────────────────────────────────────────────

    override suspend fun saveAuditEvent(event: QuoteAuditEvent): DomainResult<QuoteAuditEvent> =
        dataSource.insertAuditEvent(event)

    override suspend fun listAuditEvents(tenantId: String, quoteId: String): DomainResult<List<QuoteAuditEvent>> =
        dataSource.selectAuditEvents(tenantId, quoteId)

    // ─── Provenance ───────────────────────────────────────────────────────────

    override suspend fun saveProvenance(provenance: QuoteProvenance): DomainResult<QuoteProvenance> =
        dataSource.insertProvenance(provenance)

    override suspend fun findProvenance(
        tenantId: String, quoteId: String, versionId: String
    ): DomainResult<QuoteProvenance?> =
        dataSource.selectProvenance(tenantId, quoteId, versionId)

    // ─── Reconciliation ───────────────────────────────────────────────────────

    override suspend fun saveReconciliationEvent(
        event: QuoteReconciliationEvent
    ): DomainResult<QuoteReconciliationEvent> =
        dataSource.insertReconciliationEvent(event)

    override suspend fun listReconciliationEvents(
        tenantId: String, quoteId: String
    ): DomainResult<List<QuoteReconciliationEvent>> =
        dataSource.selectReconciliationEvents(tenantId, quoteId)
}
