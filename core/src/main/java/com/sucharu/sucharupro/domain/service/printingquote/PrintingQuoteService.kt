package com.sucharu.sucharupro.domain.service.printingquote

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult
import com.sucharu.sucharupro.domain.model.printingquote.*

/**
 * Domain Service contract for the Smart Printing Calculator quotation layer.
 * Module 17 Step 02.
 */
interface PrintingQuoteService {

    /** Create a new DRAFT quote linked to a completed Step 01 calculation. */
    suspend fun createQuote(
        request: CreatePrintingQuoteRequest
    ): DomainResult<PrintingQuote>

    /** Calculate pricing for a quote, creating a new immutable version. */
    suspend fun calculateQuote(
        request: CalculatePrintingQuoteRequest,
        step01: PrintingCalculationResult
    ): DomainResult<PrintingQuoteVersion>

    /** Recalculate an existing CALCULATED or REVIEW quote with new assumptions. */
    suspend fun recalculateQuote(
        request: CalculatePrintingQuoteRequest,
        step01: PrintingCalculationResult
    ): DomainResult<PrintingQuoteVersion>

    /** Retrieve a quote by ID (header only). */
    suspend fun getQuote(tenantId: String, quoteId: String): DomainResult<PrintingQuote?>

    /** Retrieve a specific version with full cost components and tiers. */
    suspend fun getVersion(
        tenantId: String,
        versionId: String
    ): DomainResult<PrintingQuoteVersion?>

    /** Retrieve cost components for a version. */
    suspend fun getCostBreakdown(
        tenantId: String,
        versionId: String
    ): DomainResult<List<PrintingCostComponent>>

    /** Retrieve quantity tiers for a version. */
    suspend fun getQuantityTiers(
        tenantId: String,
        versionId: String
    ): DomainResult<List<PrintingQuantityTier>>

    /** List all versions for a quote. */
    suspend fun listVersions(
        tenantId: String,
        quoteId: String
    ): DomainResult<List<PrintingQuoteVersion>>

    /** Submit a CALCULATED quote for review. */
    suspend fun submitForReview(
        quoteId: String, tenantId: String, projectId: String, actor: String
    ): DomainResult<PrintingQuote>

    /** Approve or reject a quote in REVIEW status. */
    suspend fun reviewQuote(request: QuoteReviewRequest): DomainResult<PrintingQuote>

    /** Retrieve full audit trail for a quote. */
    suspend fun getAuditTrail(
        tenantId: String,
        quoteId: String
    ): DomainResult<List<QuoteAuditEvent>>

    /** Retrieve provenance for a specific version. */
    suspend fun getProvenance(
        tenantId: String,
        quoteId: String,
        versionId: String
    ): DomainResult<QuoteProvenance?>

    /** Run 6-identity reconciliation and persist the result. */
    suspend fun reconcileQuote(
        quoteId: String,
        versionId: String,
        tenantId: String,
        projectId: String,
        actor: String
    ): DomainResult<QuoteReconciliationEvent>

    /** Export a deterministic, read-only handoff contract for downstream consumption. */
    suspend fun exportHandoffContract(
        tenantId: String,
        quoteId: String
    ): DomainResult<Module17Step02PrintingQuotationHandoffContract>
}
