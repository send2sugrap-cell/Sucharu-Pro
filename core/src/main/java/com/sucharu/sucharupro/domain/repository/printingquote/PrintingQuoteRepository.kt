package com.sucharu.sucharupro.domain.repository.printingquote

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingquote.*

/**
 * Domain Repository interface for the Smart Printing Calculator quotation layer.
 * Module 17 Step 02.
 *
 * All operations are tenant-scoped. Implementations enforce tenant isolation via RLS.
 */
interface PrintingQuoteRepository {

    // ─── Quote Header ─────────────────────────────────────────────────────────

    suspend fun saveQuote(quote: PrintingQuote): DomainResult<PrintingQuote>
    suspend fun updateQuote(quote: PrintingQuote): DomainResult<PrintingQuote>
    suspend fun findQuoteById(tenantId: String, quoteId: String): DomainResult<PrintingQuote?>
    suspend fun findQuoteByIdempotencyKey(tenantId: String, key: String): DomainResult<PrintingQuote?>
    suspend fun listQuotes(tenantId: String, limit: Int = 50): DomainResult<List<PrintingQuote>>
    suspend fun listQuotesByCalculationId(
        tenantId: String,
        calculationId: String
    ): DomainResult<List<PrintingQuote>>

    // ─── Quote Versions ───────────────────────────────────────────────────────

    suspend fun saveQuoteVersion(version: PrintingQuoteVersion): DomainResult<PrintingQuoteVersion>
    suspend fun findVersionById(
        tenantId: String,
        versionId: String
    ): DomainResult<PrintingQuoteVersion?>
    suspend fun listVersionsByQuoteId(
        tenantId: String,
        quoteId: String
    ): DomainResult<List<PrintingQuoteVersion>>

    // ─── Cost Components ──────────────────────────────────────────────────────

    suspend fun saveCostComponents(
        components: List<PrintingCostComponent>
    ): DomainResult<List<PrintingCostComponent>>
    suspend fun listCostComponents(
        tenantId: String,
        versionId: String
    ): DomainResult<List<PrintingCostComponent>>

    // ─── Quantity Tiers ───────────────────────────────────────────────────────

    suspend fun saveQuantityTiers(
        tiers: List<PrintingQuantityTier>
    ): DomainResult<List<PrintingQuantityTier>>
    suspend fun listQuantityTiers(
        tenantId: String,
        versionId: String
    ): DomainResult<List<PrintingQuantityTier>>

    // ─── Audit Events ─────────────────────────────────────────────────────────

    suspend fun saveAuditEvent(event: QuoteAuditEvent): DomainResult<QuoteAuditEvent>
    suspend fun listAuditEvents(
        tenantId: String,
        quoteId: String
    ): DomainResult<List<QuoteAuditEvent>>

    // ─── Provenance ───────────────────────────────────────────────────────────

    suspend fun saveProvenance(provenance: QuoteProvenance): DomainResult<QuoteProvenance>
    suspend fun findProvenance(
        tenantId: String,
        quoteId: String,
        versionId: String
    ): DomainResult<QuoteProvenance?>

    // ─── Reconciliation ───────────────────────────────────────────────────────

    suspend fun saveReconciliationEvent(
        event: QuoteReconciliationEvent
    ): DomainResult<QuoteReconciliationEvent>
    suspend fun listReconciliationEvents(
        tenantId: String,
        quoteId: String
    ): DomainResult<List<QuoteReconciliationEvent>>
}
