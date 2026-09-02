package com.sucharu.sucharupro.data.datasource.printingquote

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingquote.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory DataSource for Smart Printing Calculator Quotation layer.
 * Used in unit tests and local integration tests.
 * Module 17 Step 02.
 */
class FakePrintingQuoteDataSource : PrintingQuoteDataSource {

    private val quotes = ConcurrentHashMap<String, PrintingQuote>()
    private val versions = ConcurrentHashMap<String, PrintingQuoteVersion>()
    private val costComponents = ConcurrentHashMap<String, PrintingCostComponent>()
    private val quantityTiers = ConcurrentHashMap<String, PrintingQuantityTier>()
    private val auditEvents = ConcurrentHashMap<String, QuoteAuditEvent>()
    private val provenances = ConcurrentHashMap<String, QuoteProvenance>()
    private val reconciliationEvents = ConcurrentHashMap<String, QuoteReconciliationEvent>()

    // ─── Quote Header ─────────────────────────────────────────────────────────

    override suspend fun insertQuote(quote: PrintingQuote): DomainResult<PrintingQuote> {
        quotes[quote.quoteId] = quote
        return DomainResult.Success(quote)
    }

    override suspend fun updateQuote(quote: PrintingQuote): DomainResult<PrintingQuote> {
        quotes[quote.quoteId] = quote
        return DomainResult.Success(quote)
    }

    override suspend fun selectQuoteById(tenantId: String, quoteId: String): DomainResult<PrintingQuote?> =
        DomainResult.Success(quotes[quoteId]?.takeIf { it.tenantId == tenantId })

    override suspend fun selectQuoteByIdempotencyKey(tenantId: String, key: String): DomainResult<PrintingQuote?> =
        DomainResult.Success(quotes.values.find { it.tenantId == tenantId && it.idempotencyKey == key })

    override suspend fun selectQuotes(tenantId: String, limit: Int): DomainResult<List<PrintingQuote>> =
        DomainResult.Success(
            quotes.values.filter { it.tenantId == tenantId }.sortedByDescending { it.createdAt }.take(limit)
        )

    override suspend fun selectQuotesByCalculationId(tenantId: String, calculationId: String): DomainResult<List<PrintingQuote>> =
        DomainResult.Success(
            quotes.values.filter { it.tenantId == tenantId && it.calculationId == calculationId }
        )

    // ─── Quote Versions ───────────────────────────────────────────────────────

    override suspend fun insertQuoteVersion(version: PrintingQuoteVersion): DomainResult<PrintingQuoteVersion> {
        versions[version.versionId] = version
        return DomainResult.Success(version)
    }

    override suspend fun updateQuoteVersion(version: PrintingQuoteVersion): DomainResult<PrintingQuoteVersion> {
        versions[version.versionId] = version
        return DomainResult.Success(version)
    }

    override suspend fun selectVersionById(tenantId: String, versionId: String): DomainResult<PrintingQuoteVersion?> =
        DomainResult.Success(versions[versionId]?.takeIf { it.tenantId == tenantId })

    override suspend fun selectVersionsByQuoteId(tenantId: String, quoteId: String): DomainResult<List<PrintingQuoteVersion>> =
        DomainResult.Success(
            versions.values.filter { it.tenantId == tenantId && it.quoteId == quoteId }
                .sortedBy { it.versionNumber }
        )

    // ─── Cost Components ──────────────────────────────────────────────────────

    override suspend fun insertCostComponents(components: List<PrintingCostComponent>): DomainResult<List<PrintingCostComponent>> {
        components.forEach { costComponents[it.componentId] = it }
        return DomainResult.Success(components)
    }

    override suspend fun selectCostComponents(tenantId: String, versionId: String): DomainResult<List<PrintingCostComponent>> =
        DomainResult.Success(
            costComponents.values.filter { it.versionId == versionId && it.tenantId == tenantId }
                .sortedBy { it.sortOrder }
        )

    // ─── Quantity Tiers ───────────────────────────────────────────────────────

    override suspend fun insertQuantityTiers(tiers: List<PrintingQuantityTier>): DomainResult<List<PrintingQuantityTier>> {
        tiers.forEach { quantityTiers[it.tierId] = it }
        return DomainResult.Success(tiers)
    }

    override suspend fun selectQuantityTiers(tenantId: String, versionId: String): DomainResult<List<PrintingQuantityTier>> =
        DomainResult.Success(
            quantityTiers.values.filter { it.versionId == versionId && it.tenantId == tenantId }
                .sortedBy { it.tierQuantity }
        )

    // ─── Audit Events ─────────────────────────────────────────────────────────

    override suspend fun insertAuditEvent(event: QuoteAuditEvent): DomainResult<QuoteAuditEvent> {
        auditEvents[event.auditId] = event
        return DomainResult.Success(event)
    }

    override suspend fun selectAuditEvents(tenantId: String, quoteId: String): DomainResult<List<QuoteAuditEvent>> =
        DomainResult.Success(
            auditEvents.values.filter { it.tenantId == tenantId && it.quoteId == quoteId }
                .sortedBy { it.occurredAt }
        )

    // ─── Provenance ───────────────────────────────────────────────────────────

    override suspend fun insertProvenance(provenance: QuoteProvenance): DomainResult<QuoteProvenance> {
        provenances[provenance.provenanceId] = provenance
        return DomainResult.Success(provenance)
    }

    override suspend fun selectProvenance(tenantId: String, quoteId: String, versionId: String): DomainResult<QuoteProvenance?> =
        DomainResult.Success(
            provenances.values.find { it.tenantId == tenantId && it.quoteId == quoteId && it.versionId == versionId }
        )

    // ─── Reconciliation ───────────────────────────────────────────────────────

    override suspend fun insertReconciliationEvent(event: QuoteReconciliationEvent): DomainResult<QuoteReconciliationEvent> {
        reconciliationEvents[event.reconciliationId] = event
        return DomainResult.Success(event)
    }

    override suspend fun selectReconciliationEvents(tenantId: String, quoteId: String): DomainResult<List<QuoteReconciliationEvent>> =
        DomainResult.Success(
            reconciliationEvents.values.filter { it.tenantId == tenantId && it.quoteId == quoteId }
                .sortedBy { it.reconciledAt }
        )

    fun clear() {
        quotes.clear(); versions.clear(); costComponents.clear(); quantityTiers.clear()
        auditEvents.clear(); provenances.clear(); reconciliationEvents.clear()
    }
}
