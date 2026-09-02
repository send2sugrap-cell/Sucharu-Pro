package com.sucharu.sucharupro.data.api.model.printingquote

import com.sucharu.sucharupro.domain.model.printingquote.*
import java.math.BigDecimal

// ============================================================
// REQUEST DTOs
// ============================================================

data class CreatePrintingQuoteRequestDto(
    val calculationId: String,
    val jobTitle: String = "Printing Quote",
    val customerRef: String? = null,
    val customerNote: String? = null,
    val internalNote: String? = null,
    val currency: String = "BDT",
    val idempotencyKey: String? = null
)

data class CalculatePrintingQuoteRequestDto(
    val quoteId: String,
    // Costing assumptions
    val overheadAllocationPct: String = "0.0000",
    val wastageCosted: Boolean = true,
    // Pricing
    val pricingMethod: String = "COST_PLUS",          // COST_PLUS | TARGET_MARGIN | MANUAL
    val markupPercentage: String = "0.0000",
    val targetMarginPercentage: String = "0.0000",
    val discountType: String = "NONE",                 // NONE | FIXED_AMOUNT | PERCENTAGE
    val discountValue: String = "0.0000",
    val taxPercentage: String = "0.0000",
    // Quantity tiers (optional)
    val quantityTierBreaks: List<Long> = emptyList()
)

data class QuoteReviewRequestDto(
    val quoteId: String,
    val approved: Boolean,
    val reason: String? = null
)

// ============================================================
// RESPONSE DTOs — QUOTE HEADER
// ============================================================

data class PrintingQuoteDto(
    val quoteId: String,
    val quoteNumber: String,
    val jobTitle: String,
    val calculationId: String,
    val status: String,
    val currentVersion: Int,
    val currency: String,
    val orderedQuantity: Long,
    val customerRef: String?,
    val customerNote: String?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val approvedAt: Long?,
    val approvedBy: String?
)

// ============================================================
// RESPONSE DTOs — QUOTE VERSION
// ============================================================

data class QuoteQuantityBreakdownDto(
    val orderedQuantity: Long,
    val producedQuantity: Long,
    val sellableQuantity: Long,
    val wastageQuantity: Long,
    val wastagePercentage: String,
    val impositionUps: Int,
    val quantityBasis: String
)

data class PrintingPricingSnapshotDto(
    val baseSellingPrice: String,
    val discountType: String,
    val discountValue: String,
    val discountAmount: String,
    val taxPercentage: String,
    val taxAmount: String,
    val finalQuoteTotal: String,
    val markupAmount: String,
    val markupPercentage: String,
    val grossProfit: String,
    val grossMarginPercentage: String,
    val contributionAmount: String,
    val contributionMarginPercentage: String,
    val breakEvenPrice: String,
    val breakEvenQuantity: Long,
    val targetMarginPrice: String?,
    val targetMarginPercentage: String?
)

data class PrintingQuoteVersionDto(
    val versionId: String,
    val quoteId: String,
    val versionNumber: Int,
    val status: String,
    val currency: String,
    val calculationId: String,
    val specFingerprint: String,
    val calcFingerprint: String,
    val quantityBreakdown: QuoteQuantityBreakdownDto,
    val totalCost: String,
    val unitCost: String,
    val pricing: PrintingPricingSnapshotDto,
    val pricingMethod: String,
    val isApproved: Boolean,
    val createdBy: String,
    val createdAt: Long,
    val costComponents: List<PrintingCostComponentDto> = emptyList(),
    val quantityTiers: List<PrintingQuantityTierDto> = emptyList()
)

// ============================================================
// RESPONSE DTOs — COST COMPONENTS & TIERS
// ============================================================

data class PrintingCostComponentDto(
    val componentId: String,
    val componentType: String,
    val componentCode: String,
    val description: String,
    val quantity: String,
    val unit: String,
    val unitRate: String?,
    val amount: String,
    val formulaReference: String,
    val sourceRef: String?,
    val isApplicable: Boolean,
    val sortOrder: Int
)

data class PrintingQuantityTierDto(
    val tierId: String,
    val tierQuantity: Long,
    val unitCost: String,
    val totalCost: String,
    val sellingPricePerUnit: String,
    val finalTotal: String,
    val grossMarginPercentage: String,
    val isBaseTier: Boolean
)

// ============================================================
// RESPONSE DTOs — AUDIT & PROVENANCE
// ============================================================

data class QuoteAuditEventDto(
    val auditId: String,
    val quoteId: String,
    val versionId: String?,
    val eventType: String,
    val actor: String,
    val description: String,
    val beforeStatus: String?,
    val afterStatus: String?,
    val occurredAt: Long
)

data class QuoteProvenanceDto(
    val provenanceId: String,
    val quoteId: String,
    val versionId: String,
    val calculationId: String,
    val calculationVersion: String,
    val calculationStatus: String,
    val specFingerprint: String,
    val calcFingerprint: String,
    val costingEngineVersion: String,
    val pricingEngineVersion: String,
    val capturedAt: Long,
    val capturedBy: String
)

data class QuoteReconciliationEventDto(
    val reconciliationId: String,
    val quoteId: String,
    val versionId: String,
    val isReconciled: Boolean,
    val totalCostCheck: Boolean,
    val revenueIdentityCheck: Boolean,
    val grossProfitCheck: Boolean,
    val marginCheck: Boolean,
    val markupCheck: Boolean,
    val breakevenCheck: Boolean,
    val discrepanciesJson: String?,
    val reconciledAt: Long,
    val reconciledBy: String
)

// ============================================================
// HANDOFF CONTRACT DTO
// ============================================================

data class Module17Step02PrintingQuotationHandoffContractDto(
    val handoffId: String,
    val quoteId: String,
    val versionId: String,
    val versionNumber: Int,
    val tenantId: String,
    val projectId: String,
    val generatedAt: Long,
    val contractVersion: String,
    val quoteNumber: String,
    val jobTitle: String,
    val currency: String,
    val status: String,
    val calculationId: String,
    val specFingerprint: String,
    val calcFingerprint: String,
    val orderedQuantity: Long,
    val producedQuantity: Long,
    val sellableQuantity: Long,
    val wastageQuantity: Long,
    val wastagePercentage: String,
    val impositionUps: Int,
    val costComponents: List<Map<String, String>>,
    val totalCost: String,
    val unitCost: String,
    val pricingMethod: String,
    val baseSellingPrice: String,
    val discountType: String,
    val discountAmount: String,
    val taxPercentage: String,
    val taxAmount: String,
    val finalQuoteTotal: String,
    val markupAmount: String,
    val markupPercentage: String,
    val grossProfit: String,
    val grossMarginPercentage: String,
    val breakEvenPrice: String,
    val breakEvenQuantity: Long,
    val targetMarginPrice: String?,
    val targetMarginPercentage: String?,
    val quantityTiers: List<Map<String, String>>,
    val riskFlags: List<String>,
    val costingEngineVersion: String,
    val pricingEngineVersion: String,
    val reconciliationStatus: String,
    val integrityHash: String,
    val isReadOnly: Boolean = true,
    val isMutable: Boolean = false
)

// ============================================================
// DOMAIN → DTO MAPPING EXTENSIONS
// ============================================================

fun PrintingQuote.toDto() = PrintingQuoteDto(
    quoteId = quoteId,
    quoteNumber = quoteNumber,
    jobTitle = jobTitle,
    calculationId = calculationId,
    status = status.name,
    currentVersion = currentVersion,
    currency = currency,
    orderedQuantity = orderedQuantity,
    customerRef = customerRef,
    customerNote = customerNote,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    approvedAt = approvedAt,
    approvedBy = approvedBy
)

fun PrintingQuoteVersion.toDto() = PrintingQuoteVersionDto(
    versionId = versionId,
    quoteId = quoteId,
    versionNumber = versionNumber,
    status = status.name,
    currency = currency,
    calculationId = calculationId,
    specFingerprint = specFingerprint,
    calcFingerprint = calcFingerprint,
    quantityBreakdown = quantityBreakdown.toDto(),
    totalCost = totalCost.toPlainString(),
    unitCost = unitCost.toPlainString(),
    pricing = pricing.toDto(),
    pricingMethod = pricingAssumptions.pricingMethod,
    isApproved = isApproved,
    createdBy = createdBy,
    createdAt = createdAt,
    costComponents = costComponents.map { it.toDto() },
    quantityTiers = quantityTiers.map { it.toDto() }
)

fun QuoteQuantityBreakdown.toDto() = QuoteQuantityBreakdownDto(
    orderedQuantity = orderedQuantity,
    producedQuantity = producedQuantity,
    sellableQuantity = sellableQuantity,
    wastageQuantity = wastageQuantity,
    wastagePercentage = wastagePercentage.toPlainString(),
    impositionUps = impositionUps,
    quantityBasis = quantityBasis
)

fun PrintingPricingSnapshot.toDto() = PrintingPricingSnapshotDto(
    baseSellingPrice = baseSellingPrice.toPlainString(),
    discountType = discountType.name,
    discountValue = discountValue.toPlainString(),
    discountAmount = discountAmount.toPlainString(),
    taxPercentage = taxPercentage.toPlainString(),
    taxAmount = taxAmount.toPlainString(),
    finalQuoteTotal = finalQuoteTotal.toPlainString(),
    markupAmount = markupAmount.toPlainString(),
    markupPercentage = markupPercentage.toPlainString(),
    grossProfit = grossProfit.toPlainString(),
    grossMarginPercentage = grossMarginPercentage.toPlainString(),
    contributionAmount = contributionAmount.toPlainString(),
    contributionMarginPercentage = contributionMarginPercentage.toPlainString(),
    breakEvenPrice = breakEvenPrice.toPlainString(),
    breakEvenQuantity = breakEvenQuantity,
    targetMarginPrice = targetMarginPrice?.toPlainString(),
    targetMarginPercentage = targetMarginPercentage?.toPlainString()
)

fun PrintingCostComponent.toDto() = PrintingCostComponentDto(
    componentId = componentId,
    componentType = componentType.name,
    componentCode = componentCode,
    description = description,
    quantity = quantity.toPlainString(),
    unit = unit,
    unitRate = unitRate?.toPlainString(),
    amount = amount.toPlainString(),
    formulaReference = formulaReference,
    sourceRef = sourceRef,
    isApplicable = isApplicable,
    sortOrder = sortOrder
)

fun PrintingQuantityTier.toDto() = PrintingQuantityTierDto(
    tierId = tierId,
    tierQuantity = tierQuantity,
    unitCost = unitCost.toPlainString(),
    totalCost = totalCost.toPlainString(),
    sellingPricePerUnit = sellingPricePerUnit.toPlainString(),
    finalTotal = finalTotal.toPlainString(),
    grossMarginPercentage = grossMarginPercentage.toPlainString(),
    isBaseTier = isBaseTier
)

fun QuoteAuditEvent.toDto() = QuoteAuditEventDto(
    auditId = auditId,
    quoteId = quoteId,
    versionId = versionId,
    eventType = eventType.name,
    actor = actor,
    description = description,
    beforeStatus = beforeStatus?.name,
    afterStatus = afterStatus?.name,
    occurredAt = occurredAt
)

fun QuoteProvenance.toDto() = QuoteProvenanceDto(
    provenanceId = provenanceId,
    quoteId = quoteId,
    versionId = versionId,
    calculationId = calculationId,
    calculationVersion = calculationVersion,
    calculationStatus = calculationStatus.name,
    specFingerprint = specFingerprint,
    calcFingerprint = calcFingerprint,
    costingEngineVersion = costingEngineVersion,
    pricingEngineVersion = pricingEngineVersion,
    capturedAt = capturedAt,
    capturedBy = capturedBy
)

fun QuoteReconciliationEvent.toDto() = QuoteReconciliationEventDto(
    reconciliationId = reconciliationId,
    quoteId = quoteId,
    versionId = versionId,
    isReconciled = isReconciled,
    totalCostCheck = totalCostCheck,
    revenueIdentityCheck = revenueIdentityCheck,
    grossProfitCheck = grossProfitCheck,
    marginCheck = marginCheck,
    markupCheck = markupCheck,
    breakevenCheck = breakevenCheck,
    discrepanciesJson = discrepanciesJson,
    reconciledAt = reconciledAt,
    reconciledBy = reconciledBy
)

fun Module17Step02PrintingQuotationHandoffContract.toDto() = Module17Step02PrintingQuotationHandoffContractDto(
    handoffId = handoffId,
    quoteId = quoteId,
    versionId = versionId,
    versionNumber = versionNumber,
    tenantId = tenantId,
    projectId = projectId,
    generatedAt = generatedAt,
    contractVersion = contractVersion,
    quoteNumber = quoteNumber,
    jobTitle = jobTitle,
    currency = currency,
    status = status,
    calculationId = calculationId,
    specFingerprint = specFingerprint,
    calcFingerprint = calcFingerprint,
    orderedQuantity = orderedQuantity,
    producedQuantity = producedQuantity,
    sellableQuantity = sellableQuantity,
    wastageQuantity = wastageQuantity,
    wastagePercentage = wastagePercentage,
    impositionUps = impositionUps,
    costComponents = costComponents,
    totalCost = totalCost,
    unitCost = unitCost,
    pricingMethod = pricingMethod,
    baseSellingPrice = baseSellingPrice,
    discountType = discountType,
    discountAmount = discountAmount,
    taxPercentage = taxPercentage,
    taxAmount = taxAmount,
    finalQuoteTotal = finalQuoteTotal,
    markupAmount = markupAmount,
    markupPercentage = markupPercentage,
    grossProfit = grossProfit,
    grossMarginPercentage = grossMarginPercentage,
    breakEvenPrice = breakEvenPrice,
    breakEvenQuantity = breakEvenQuantity,
    targetMarginPrice = targetMarginPrice,
    targetMarginPercentage = targetMarginPercentage,
    quantityTiers = quantityTiers,
    riskFlags = riskFlags,
    costingEngineVersion = costingEngineVersion,
    pricingEngineVersion = pricingEngineVersion,
    reconciliationStatus = reconciliationStatus,
    integrityHash = integrityHash,
    isReadOnly = isReadOnly,
    isMutable = isMutable
)
