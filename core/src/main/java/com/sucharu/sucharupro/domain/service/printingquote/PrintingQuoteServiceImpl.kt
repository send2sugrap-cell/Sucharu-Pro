package com.sucharu.sucharupro.domain.service.printingquote

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationStatus
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult
import com.sucharu.sucharupro.domain.model.printingquote.*
import com.sucharu.sucharupro.domain.repository.printingquote.PrintingQuoteRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID

/**
 * Implementation of [PrintingQuoteService].
 *
 * Orchestration responsibilities:
 *  - Idempotency via quote idempotencyKey (mutex-guarded)
 *  - Step 01 provenance capture
 *  - Delegation to [PrintingCostingEngine] and [PrintingPricingEngine]
 *  - Immutable version creation
 *  - Audit trail emission
 *  - 6-identity reconciliation
 *  - Read-only handoff contract export
 *
 * Module 17 Step 02.
 */
class PrintingQuoteServiceImpl(
    private val repository: PrintingQuoteRepository
) : PrintingQuoteService {

    private val mutex = Mutex()

    // ─────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────

    override suspend fun createQuote(request: CreatePrintingQuoteRequest): DomainResult<PrintingQuote> {
        // Idempotency check
        if (!request.idempotencyKey.isNullOrBlank()) {
            mutex.withLock {
                val existing = repository.findQuoteByIdempotencyKey(request.tenantId, request.idempotencyKey)
                if (existing is DomainResult.Success && existing.data != null) {
                    return DomainResult.Success(existing.data!!)
                }
            }
        }

        val now = System.currentTimeMillis()
        val quoteId = UUID.randomUUID().toString()
        val quoteNumber = generateQuoteNumber(request.tenantId, now)
        val hash = sha256("QUOTE-V2:${quoteId}:${request.tenantId}:${request.calculationId}:$now")

        val quote = PrintingQuote(
            quoteId = quoteId,
            tenantId = request.tenantId,
            projectId = request.projectId,
            quoteNumber = quoteNumber,
            jobTitle = request.jobTitle,
            calculationId = request.calculationId,
            requestFingerprint = "",          // set after first calculation
            status = QuoteStatus.DRAFT,
            currentVersion = 0,
            currency = request.currency,
            orderedQuantity = 0L,            // set after calculation
            customerRef = request.customerRef,
            customerNote = request.customerNote,
            internalNote = request.internalNote,
            idempotencyKey = request.idempotencyKey,
            createdBy = request.requestedBy,
            createdAt = now,
            updatedAt = now,
            integrityHash = hash
        )

        val saveResult = repository.saveQuote(quote)
        if (saveResult is DomainResult.Success) {
            repository.saveAuditEvent(buildAudit(
                quoteId, null, request.tenantId, request.projectId,
                QuoteAuditEventType.QUOTE_CREATED, request.requestedBy,
                "Quote created from calculation ${request.calculationId}", null, QuoteStatus.DRAFT, now
            ))
        }
        return saveResult
    }

    // ─────────────────────────────────────────────────────────────
    // Calculate (first version)
    // ─────────────────────────────────────────────────────────────

    override suspend fun calculateQuote(
        request: CalculatePrintingQuoteRequest,
        step01: PrintingCalculationResult
    ): DomainResult<PrintingQuoteVersion> {
        val quoteResult = repository.findQuoteById(request.tenantId, request.quoteId)
        if (quoteResult !is DomainResult.Success || quoteResult.data == null) {
            return DomainResult.Error(message = "Quote not found: ${request.quoteId}")
        }
        val quote = quoteResult.data!!

        if (quote.status !in listOf(QuoteStatus.DRAFT, QuoteStatus.CALCULATED)) {
            return DomainResult.Error(message = "Cannot calculate quote in status: ${quote.status}")
        }

        return createVersion(quote, step01, request, quote.currentVersion + 1)
    }

    // ─────────────────────────────────────────────────────────────
    // Recalculate (subsequent versions)
    // ─────────────────────────────────────────────────────────────

    override suspend fun recalculateQuote(
        request: CalculatePrintingQuoteRequest,
        step01: PrintingCalculationResult
    ): DomainResult<PrintingQuoteVersion> {
        val quoteResult = repository.findQuoteById(request.tenantId, request.quoteId)
        if (quoteResult !is DomainResult.Success || quoteResult.data == null) {
            return DomainResult.Error(message = "Quote not found: ${request.quoteId}")
        }
        val quote = quoteResult.data!!

        if (quote.status == QuoteStatus.APPROVED || quote.status == QuoteStatus.REJECTED) {
            return DomainResult.Error(message = "Cannot recalculate a ${quote.status} quote.")
        }

        return createVersion(quote, step01, request, quote.currentVersion + 1)
    }

    // ─────────────────────────────────────────────────────────────
    // Read operations
    // ─────────────────────────────────────────────────────────────

    override suspend fun getQuote(tenantId: String, quoteId: String): DomainResult<PrintingQuote?> =
        repository.findQuoteById(tenantId, quoteId)

    override suspend fun getVersion(tenantId: String, versionId: String): DomainResult<PrintingQuoteVersion?> =
        repository.findVersionById(tenantId, versionId)

    override suspend fun getCostBreakdown(
        tenantId: String, versionId: String
    ): DomainResult<List<PrintingCostComponent>> =
        repository.listCostComponents(tenantId, versionId)

    override suspend fun getQuantityTiers(
        tenantId: String, versionId: String
    ): DomainResult<List<PrintingQuantityTier>> =
        repository.listQuantityTiers(tenantId, versionId)

    override suspend fun listVersions(
        tenantId: String, quoteId: String
    ): DomainResult<List<PrintingQuoteVersion>> =
        repository.listVersionsByQuoteId(tenantId, quoteId)

    // ─────────────────────────────────────────────────────────────
    // Workflow transitions
    // ─────────────────────────────────────────────────────────────

    override suspend fun submitForReview(
        quoteId: String, tenantId: String, projectId: String, actor: String
    ): DomainResult<PrintingQuote> {
        val quoteResult = repository.findQuoteById(tenantId, quoteId)
        if (quoteResult !is DomainResult.Success || quoteResult.data == null) {
            return DomainResult.Error(message = "Quote not found: $quoteId")
        }
        val quote = quoteResult.data!!
        if (!quote.status.canTransitionTo(QuoteStatus.REVIEW)) {
            return DomainResult.Error(message = "Cannot submit quote in status ${quote.status} for review.")
        }
        val now = System.currentTimeMillis()
        val updated = quote.copy(
            status = QuoteStatus.REVIEW,
            updatedAt = now,
            integrityHash = sha256("QUOTE-V2:${quote.quoteId}:REVIEW:${now}:${quote.integrityHash}")
        )
        val res = repository.updateQuote(updated)
        if (res is DomainResult.Success) {
            repository.saveAuditEvent(buildAudit(
                quoteId, null, tenantId, projectId,
                QuoteAuditEventType.QUOTE_SUBMITTED_FOR_REVIEW, actor,
                "Quote submitted for review", QuoteStatus.CALCULATED, QuoteStatus.REVIEW, now
            ))
        }
        return res
    }

    override suspend fun reviewQuote(request: QuoteReviewRequest): DomainResult<PrintingQuote> {
        val quoteResult = repository.findQuoteById(request.tenantId, request.quoteId)
        if (quoteResult !is DomainResult.Success || quoteResult.data == null) {
            return DomainResult.Error(message = "Quote not found: ${request.quoteId}")
        }
        val quote = quoteResult.data!!
        if (quote.status != QuoteStatus.REVIEW) {
            return DomainResult.Error(message = "Quote must be in REVIEW status to approve/reject.")
        }

        val targetStatus = if (request.approved) QuoteStatus.APPROVED else QuoteStatus.REJECTED
        if (!quote.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(message = "Cannot transition from ${quote.status} to $targetStatus.")
        }

        val now = System.currentTimeMillis()
        val updated = quote.copy(
            status = targetStatus,
            approvedAt = if (request.approved) now else null,
            approvedBy = if (request.approved) request.requestedBy else null,
            updatedAt = now,
            integrityHash = sha256("QUOTE-V2:${quote.quoteId}:${targetStatus}:${now}:${quote.integrityHash}")
        )

        // Mark current version as approved if applicable
        if (request.approved && quote.currentVersion > 0) {
            val versions = repository.listVersionsByQuoteId(request.tenantId, request.quoteId)
            if (versions is DomainResult.Success) {
                val latest = versions.data.maxByOrNull { it.versionNumber }
                if (latest != null && !latest.isApproved) {
                    repository.saveQuoteVersion(latest.copy(isApproved = true))
                }
            }
        }

        val res = repository.updateQuote(updated)
        if (res is DomainResult.Success) {
            val evtType = if (request.approved) QuoteAuditEventType.QUOTE_APPROVED else QuoteAuditEventType.QUOTE_REJECTED
            val desc = if (request.approved) "Quote approved" else "Quote rejected: ${request.reason ?: "No reason given"}"
            repository.saveAuditEvent(buildAudit(
                request.quoteId, null, request.tenantId, request.projectId,
                evtType, request.requestedBy, desc, QuoteStatus.REVIEW, targetStatus, now
            ))
        }
        return res
    }

    // ─────────────────────────────────────────────────────────────
    // Audit & Provenance
    // ─────────────────────────────────────────────────────────────

    override suspend fun getAuditTrail(
        tenantId: String, quoteId: String
    ): DomainResult<List<QuoteAuditEvent>> =
        repository.listAuditEvents(tenantId, quoteId)

    override suspend fun getProvenance(
        tenantId: String, quoteId: String, versionId: String
    ): DomainResult<QuoteProvenance?> =
        repository.findProvenance(tenantId, quoteId, versionId)

    // ─────────────────────────────────────────────────────────────
    // Reconciliation
    // ─────────────────────────────────────────────────────────────

    override suspend fun reconcileQuote(
        quoteId: String, versionId: String, tenantId: String, projectId: String, actor: String
    ): DomainResult<QuoteReconciliationEvent> {
        val versionResult = repository.findVersionById(tenantId, versionId)
        if (versionResult !is DomainResult.Success || versionResult.data == null) {
            return DomainResult.Error(message = "Version not found: $versionId")
        }
        val version = versionResult.data!!
        val recon = PrintingPricingEngine.reconcile(
            snapshot = version.pricing,
            totalCost = version.totalCost,
            unitCost = version.unitCost,
            sellableQty = version.quantityBreakdown.sellableQuantity
        )

        val now = System.currentTimeMillis()
        val reconId = UUID.randomUUID().toString()
        val reconHash = sha256("RECON-V2:$reconId:$quoteId:$versionId:${recon.isFullyReconciled}:$now")

        val event = QuoteReconciliationEvent(
            reconciliationId = reconId,
            quoteId = quoteId,
            versionId = versionId,
            tenantId = tenantId,
            projectId = projectId,
            isReconciled = recon.isFullyReconciled,
            totalCostCheck = recon.totalCostCheck,
            revenueIdentityCheck = recon.revenueIdentityCheck,
            grossProfitCheck = recon.grossProfitCheck,
            marginCheck = recon.marginCheck,
            markupCheck = recon.markupCheck,
            breakevenCheck = recon.breakevenCheck,
            discrepanciesJson = if (recon.discrepancies.isEmpty()) null
            else """{"discrepancies":${recon.discrepancies.map { "\"$it\"" }}}""",
            reconciledAt = now,
            reconciledBy = actor,
            integrityHash = reconHash
        )

        val saveResult = repository.saveReconciliationEvent(event)
        repository.saveAuditEvent(buildAudit(
            quoteId, versionId, tenantId, projectId,
            QuoteAuditEventType.RECONCILIATION_PERFORMED, actor,
            "Reconciliation: ${if (recon.isFullyReconciled) "PASS" else "FAIL (${recon.discrepancies.size} issues)"}",
            null, null, now
        ))
        return saveResult
    }

    // ─────────────────────────────────────────────────────────────
    // Handoff Contract
    // ─────────────────────────────────────────────────────────────

    override suspend fun exportHandoffContract(
        tenantId: String, quoteId: String
    ): DomainResult<Module17Step02PrintingQuotationHandoffContract> {
        val quoteResult = repository.findQuoteById(tenantId, quoteId)
        if (quoteResult !is DomainResult.Success || quoteResult.data == null) {
            return DomainResult.Error(message = "Quote not found: $quoteId")
        }
        val quote = quoteResult.data!!

        val versionsResult = repository.listVersionsByQuoteId(tenantId, quoteId)
        val latestVersion = if (versionsResult is DomainResult.Success) {
            versionsResult.data.maxByOrNull { it.versionNumber }
        } else null

        if (latestVersion == null) {
            return DomainResult.Error(message = "No calculated version found for quote: $quoteId")
        }

        val costComponents = (repository.listCostComponents(tenantId, latestVersion.versionId) as? DomainResult.Success)?.data ?: emptyList()
        val tiers = (repository.listQuantityTiers(tenantId, latestVersion.versionId) as? DomainResult.Success)?.data ?: emptyList()

        val now = System.currentTimeMillis()
        val handoffId = "handoff-quote-${quoteId.take(16)}-v${latestVersion.versionNumber}"
        val riskFlags = buildRiskFlags(latestVersion)
        val integrityHash = sha256(
            "HANDOFF-V2:${handoffId}:${quoteId}:${latestVersion.versionId}:${quote.status}:${latestVersion.pricing.finalQuoteTotal}:$now"
        )

        val contract = Module17Step02PrintingQuotationHandoffContract(
            handoffId = handoffId,
            quoteId = quoteId,
            versionId = latestVersion.versionId,
            versionNumber = latestVersion.versionNumber,
            tenantId = tenantId,
            projectId = quote.projectId,
            generatedAt = now,
            quoteNumber = quote.quoteNumber,
            jobTitle = quote.jobTitle,
            currency = quote.currency,
            status = quote.status.name,
            calculationId = latestVersion.calculationId,
            specFingerprint = latestVersion.specFingerprint,
            calcFingerprint = latestVersion.calcFingerprint,
            orderedQuantity = latestVersion.quantityBreakdown.orderedQuantity,
            producedQuantity = latestVersion.quantityBreakdown.producedQuantity,
            sellableQuantity = latestVersion.quantityBreakdown.sellableQuantity,
            wastageQuantity = latestVersion.quantityBreakdown.wastageQuantity,
            wastagePercentage = latestVersion.quantityBreakdown.wastagePercentage.toPlainString(),
            impositionUps = latestVersion.quantityBreakdown.impositionUps,
            costComponents = costComponents.map { c ->
                mapOf(
                    "type" to c.componentType.name,
                    "code" to c.componentCode,
                    "description" to c.description,
                    "amount" to c.amount.toPlainString(),
                    "formula" to c.formulaReference
                )
            },
            totalCost = latestVersion.totalCost.toPlainString(),
            unitCost = latestVersion.unitCost.toPlainString(),
            pricingMethod = latestVersion.pricingAssumptions.pricingMethod,
            baseSellingPrice = latestVersion.pricing.baseSellingPrice.toPlainString(),
            discountType = latestVersion.pricing.discountType.name,
            discountAmount = latestVersion.pricing.discountAmount.toPlainString(),
            taxPercentage = latestVersion.pricing.taxPercentage.toPlainString(),
            taxAmount = latestVersion.pricing.taxAmount.toPlainString(),
            finalQuoteTotal = latestVersion.pricing.finalQuoteTotal.toPlainString(),
            markupAmount = latestVersion.pricing.markupAmount.toPlainString(),
            markupPercentage = latestVersion.pricing.markupPercentage.toPlainString(),
            grossProfit = latestVersion.pricing.grossProfit.toPlainString(),
            grossMarginPercentage = latestVersion.pricing.grossMarginPercentage.toPlainString(),
            breakEvenPrice = latestVersion.pricing.breakEvenPrice.toPlainString(),
            breakEvenQuantity = latestVersion.pricing.breakEvenQuantity,
            targetMarginPrice = latestVersion.pricing.targetMarginPrice?.toPlainString(),
            targetMarginPercentage = latestVersion.pricing.targetMarginPercentage?.toPlainString(),
            quantityTiers = tiers.map { t ->
                mapOf(
                    "quantity" to t.tierQuantity.toString(),
                    "unitCost" to t.unitCost.toPlainString(),
                    "sellingPricePerUnit" to t.sellingPricePerUnit.toPlainString(),
                    "finalTotal" to t.finalTotal.toPlainString(),
                    "grossMarginPct" to t.grossMarginPercentage.toPlainString(),
                    "isBaseTier" to t.isBaseTier.toString()
                )
            },
            riskFlags = riskFlags,
            costingEngineVersion = latestVersion.costingAssumptions.engineVersion,
            pricingEngineVersion = latestVersion.pricingAssumptions.engineVersion,
            reconciliationStatus = if (quote.status == QuoteStatus.APPROVED) "APPROVED" else "PENDING",
            integrityHash = integrityHash
        )

        repository.saveAuditEvent(buildAudit(
            quoteId, latestVersion.versionId, tenantId, quote.projectId,
            QuoteAuditEventType.HANDOFF_EXPORTED, "SYSTEM",
            "Handoff contract exported: $handoffId", null, null, now
        ))

        return DomainResult.Success(contract)
    }

    // ─────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────

    private suspend fun createVersion(
        quote: PrintingQuote,
        step01: PrintingCalculationResult,
        request: CalculatePrintingQuoteRequest,
        versionNumber: Int
    ): DomainResult<PrintingQuoteVersion> {
        val now = System.currentTimeMillis()
        val versionId = UUID.randomUUID().toString()

        // Quantity breakdown
        val totalSheets = step01.materialRequirement.totalSheetsRequired
        val wasteSheets = step01.materialRequirement.wasteSheetsRequired
        val productiveSheets = step01.materialRequirement.productiveSheetsRequired
        val wastePct = BigDecimal(wasteSheets).safeDiv(BigDecimal(totalSheets.coerceAtLeast(1)))
            .multiply(QUOTE_ONE_HUNDRED).q4()

        val qtyBreakdown = QuoteQuantityBreakdown(
            orderedQuantity = step01.materialRequirement.totalSheetsRequired.let { _ -> step01.normalizedSpecification.quantity.orderedQuantity },
            producedQuantity = productiveSheets.toLong(),
            sellableQuantity = step01.normalizedSpecification.quantity.orderedQuantity,
            wastageQuantity = wasteSheets,
            wastagePercentage = wastePct,
            impositionUps = step01.materialRequirement.finishedItemsPerSheet
        )

        // Costing
        val costingResult = PrintingCostingEngine.computeFromStep01(
            quoteId = quote.quoteId,
            versionId = versionId,
            step01 = step01,
            quantityBreakdown = qtyBreakdown,
            assumptions = request.costingAssumptions
        )

        // Pricing
        val pricingSnapshot = PrintingPricingEngine.compute(
            totalCost = costingResult.totalCost,
            unitCost = costingResult.unitCost,
            sellableQty = qtyBreakdown.sellableQuantity,
            assumptions = request.pricingAssumptions
        )

        // Integrity hash
        val integrityHash = sha256(
            "VER-V2:$versionId:${quote.quoteId}:${step01.calculationId}:" +
                    "${costingResult.totalCost}:${pricingSnapshot.finalQuoteTotal}:$now"
        )

        val version = PrintingQuoteVersion(
            versionId = versionId,
            quoteId = quote.quoteId,
            tenantId = quote.tenantId,
            projectId = quote.projectId,
            versionNumber = versionNumber,
            status = QuoteStatus.CALCULATED,
            currency = quote.currency,
            calculationId = step01.calculationId,
            specFingerprint = step01.requestFingerprint,
            calcFingerprint = step01.integrityHash,
            quantityBreakdown = qtyBreakdown,
            costingAssumptions = request.costingAssumptions,
            pricingAssumptions = request.pricingAssumptions,
            totalCost = costingResult.totalCost,
            unitCost = costingResult.unitCost,
            pricing = pricingSnapshot,
            integrityHash = integrityHash,
            createdBy = request.requestedBy,
            createdAt = now,
            costComponents = costingResult.components
        )

        // Persist version
        val versionSave = repository.saveQuoteVersion(version)
        if (versionSave !is DomainResult.Success) return versionSave

        // Persist cost components
        if (costingResult.components.isNotEmpty()) {
            repository.saveCostComponents(costingResult.components)
        }

        // Quantity tiers
        val computedTiers = if (request.quantityTierBreaks.isNotEmpty()) {
            val tiers = PrintingCostingEngine.computeQuantityTiers(
                quoteId = quote.quoteId,
                versionId = versionId,
                baseTotalCost = costingResult.totalCost,
                baseQuantity = qtyBreakdown.sellableQuantity,
                step01 = step01,
                tierQuantities = request.quantityTierBreaks,
                pricingAssumptions = request.pricingAssumptions
            )
            repository.saveQuantityTiers(tiers)
            tiers
        } else emptyList()

        // Provenance
        val provenance = QuoteProvenance(
            provenanceId = UUID.randomUUID().toString(),
            quoteId = quote.quoteId,
            versionId = versionId,
            tenantId = quote.tenantId,
            projectId = quote.projectId,
            calculationId = step01.calculationId,
            calculationVersion = step01.calculationVersion,
            calculationStatus = step01.status,
            specFingerprint = step01.requestFingerprint,
            calcFingerprint = step01.integrityHash,
            assumptionsJson = """{"costing":"${request.costingAssumptions.engineVersion}","pricing":"${request.pricingAssumptions.engineVersion}"}""",
            step01BreakdownJson = null,  // optional; kept null for performance
            capturedAt = now,
            capturedBy = request.requestedBy
        )
        repository.saveProvenance(provenance)

        // Update quote header
        val updatedQuote = quote.copy(
            status = QuoteStatus.CALCULATED,
            currentVersion = versionNumber,
            orderedQuantity = qtyBreakdown.orderedQuantity,
            requestFingerprint = step01.requestFingerprint,
            updatedAt = now,
            integrityHash = sha256("QUOTE-V2:${quote.quoteId}:CALCULATED:$versionNumber:$now:${quote.integrityHash}")
        )
        repository.updateQuote(updatedQuote)

        // Audit
        val evtType = if (versionNumber == 1) QuoteAuditEventType.QUOTE_CALCULATED else QuoteAuditEventType.QUOTE_RECALCULATED
        repository.saveAuditEvent(buildAudit(
            quote.quoteId, versionId, quote.tenantId, quote.projectId,
            evtType, request.requestedBy,
            "Version $versionNumber created: totalCost=${costingResult.totalCost.toPlainString()} ${quote.currency}",
            quote.status, QuoteStatus.CALCULATED, now
        ))

        return DomainResult.Success(version.copy(quantityTiers = computedTiers))
    }

    private fun buildAudit(
        quoteId: String, versionId: String?, tenantId: String, projectId: String,
        eventType: QuoteAuditEventType, actor: String, description: String,
        before: QuoteStatus?, after: QuoteStatus?, now: Long
    ): QuoteAuditEvent = QuoteAuditEvent(
        auditId = UUID.randomUUID().toString(),
        quoteId = quoteId,
        versionId = versionId,
        tenantId = tenantId,
        projectId = projectId,
        eventType = eventType,
        actor = actor,
        description = description,
        beforeStatus = before,
        afterStatus = after,
        occurredAt = now
    )

    private fun buildRiskFlags(version: PrintingQuoteVersion): List<String> {
        val flags = mutableListOf<String>()
        if (version.pricing.grossMarginPercentage < BigDecimal("10.0000"))
            flags += "LOW_MARGIN: Gross margin below 10%"
        if (version.pricing.discountAmount > version.pricing.markupAmount)
            flags += "DISCOUNT_EXCEEDS_MARKUP"
        if (version.pricing.breakEvenQuantity > version.quantityBreakdown.orderedQuantity)
            flags += "BREAK_EVEN_EXCEEDS_ORDER_QUANTITY"
        if (version.quantityBreakdown.wastagePercentage > BigDecimal("15.0000"))
            flags += "HIGH_WASTAGE: Wastage above 15%"
        return flags
    }

    private fun generateQuoteNumber(tenantId: String, timestamp: Long): String {
        val prefix = "QT"
        val suffix = timestamp.toString().takeLast(6)
        return "${prefix}-${tenantId.take(4).uppercase()}-${suffix}"
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
