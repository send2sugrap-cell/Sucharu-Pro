package com.sucharu.sucharupro.data.datasource.printingquote

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingquote.*

/**
 * DataSource contract for the Smart Printing Calculator quotation layer.
 * Implemented by [PostgresPrintingQuoteDataSource] (production) and [FakePrintingQuoteDataSource] (tests).
 * Module 17 Step 02.
 */
interface PrintingQuoteDataSource {

    // ─── Quote Header ─────────────────────────────────────────────────────────

    suspend fun insertQuote(quote: PrintingQuote): DomainResult<PrintingQuote>
    suspend fun updateQuote(quote: PrintingQuote): DomainResult<PrintingQuote>
    suspend fun selectQuoteById(tenantId: String, quoteId: String): DomainResult<PrintingQuote?>
    suspend fun selectQuoteByIdempotencyKey(tenantId: String, key: String): DomainResult<PrintingQuote?>
    suspend fun selectQuotes(tenantId: String, limit: Int): DomainResult<List<PrintingQuote>>
    suspend fun selectQuotesByCalculationId(tenantId: String, calculationId: String): DomainResult<List<PrintingQuote>>

    // ─── Quote Versions ───────────────────────────────────────────────────────

    suspend fun insertQuoteVersion(version: PrintingQuoteVersion): DomainResult<PrintingQuoteVersion>
    suspend fun updateQuoteVersion(version: PrintingQuoteVersion): DomainResult<PrintingQuoteVersion>
    suspend fun selectVersionById(tenantId: String, versionId: String): DomainResult<PrintingQuoteVersion?>
    suspend fun selectVersionsByQuoteId(tenantId: String, quoteId: String): DomainResult<List<PrintingQuoteVersion>>

    // ─── Cost Components ──────────────────────────────────────────────────────

    suspend fun insertCostComponents(components: List<PrintingCostComponent>): DomainResult<List<PrintingCostComponent>>
    suspend fun selectCostComponents(tenantId: String, versionId: String): DomainResult<List<PrintingCostComponent>>

    // ─── Quantity Tiers ───────────────────────────────────────────────────────

    suspend fun insertQuantityTiers(tiers: List<PrintingQuantityTier>): DomainResult<List<PrintingQuantityTier>>
    suspend fun selectQuantityTiers(tenantId: String, versionId: String): DomainResult<List<PrintingQuantityTier>>

    // ─── Audit Events ─────────────────────────────────────────────────────────

    suspend fun insertAuditEvent(event: QuoteAuditEvent): DomainResult<QuoteAuditEvent>
    suspend fun selectAuditEvents(tenantId: String, quoteId: String): DomainResult<List<QuoteAuditEvent>>

    // ─── Provenance ───────────────────────────────────────────────────────────

    suspend fun insertProvenance(provenance: QuoteProvenance): DomainResult<QuoteProvenance>
    suspend fun selectProvenance(tenantId: String, quoteId: String, versionId: String): DomainResult<QuoteProvenance?>

    // ─── Reconciliation ───────────────────────────────────────────────────────

    suspend fun insertReconciliationEvent(event: QuoteReconciliationEvent): DomainResult<QuoteReconciliationEvent>
    suspend fun selectReconciliationEvents(tenantId: String, quoteId: String): DomainResult<List<QuoteReconciliationEvent>>
}
