package com.sucharu.sucharupro.domain.model.printingquote

import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationBreakdownItem
import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationStatus
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult
import java.math.BigDecimal
import java.math.RoundingMode

// ============================================================
// PRECISION CONSTANTS (Module 17 Step 02)
// ============================================================

val QUOTE_SCALE = 4
val QUOTE_ROUNDING = RoundingMode.HALF_UP
val QUOTE_ZERO: BigDecimal = BigDecimal.ZERO.setScale(QUOTE_SCALE, QUOTE_ROUNDING)
val QUOTE_ONE_HUNDRED: BigDecimal = BigDecimal("100").setScale(QUOTE_SCALE, QUOTE_ROUNDING)

fun BigDecimal.q4(): BigDecimal = this.setScale(QUOTE_SCALE, QUOTE_ROUNDING)
fun BigDecimal?.q4OrZero(): BigDecimal = this?.setScale(QUOTE_SCALE, QUOTE_ROUNDING) ?: QUOTE_ZERO
fun BigDecimal.safeDiv(divisor: BigDecimal): BigDecimal =
    if (divisor.compareTo(BigDecimal.ZERO) == 0) QUOTE_ZERO
    else this.divide(divisor, QUOTE_SCALE, QUOTE_ROUNDING)

// ============================================================
// ENUMS
// ============================================================

/**
 * Quotation lifecycle status.
 * Module 17 Step 02.
 */
enum class QuoteStatus {
    DRAFT,
    CALCULATED,
    REVIEW,
    APPROVED,
    REJECTED,
    EXPIRED;

    fun canTransitionTo(next: QuoteStatus): Boolean = when (this) {
        DRAFT       -> next == CALCULATED
        CALCULATED  -> next == REVIEW
        REVIEW      -> next == APPROVED || next == REJECTED
        APPROVED    -> next == EXPIRED
        REJECTED    -> false
        EXPIRED     -> false
    }
}

/**
 * Canonical cost component types per the Step 02 Cost Component Scope Matrix.
 */
enum class CostComponentType(val displayName: String) {
    PAPER("Paper / Substrate"),
    INK("Ink / Consumables"),
    MACHINE("Printing Machine Cost"),
    SETUP("Setup Cost"),
    LABOR("Labor"),
    FINISHING("Finishing Operations"),
    BINDING("Binding"),
    PLATE("Plate / Prepress"),
    WASTAGE("Wastage"),
    PACKAGING("Packaging"),
    OVERHEAD("Overhead Allocation"),
    OTHER("Other Approved Cost")
}

/**
 * Discount type for quote pricing.
 */
enum class DiscountType {
    NONE,
    FIXED_AMOUNT,
    PERCENTAGE
}

/**
 * Audit event types for the quote lifecycle.
 */
enum class QuoteAuditEventType {
    QUOTE_CREATED,
    QUOTE_CALCULATED,
    QUOTE_RECALCULATED,
    QUOTE_SUBMITTED_FOR_REVIEW,
    QUOTE_APPROVED,
    QUOTE_REJECTED,
    QUOTE_EXPIRED,
    PRICING_CHANGED,
    VERSION_CREATED,
    RECONCILIATION_PERFORMED,
    HANDOFF_EXPORTED
}

// ============================================================
// VALUE OBJECTS
// ============================================================

/**
 * Explicit quantity breakdown for a printing quotation.
 * Must not silently reinterpret Step 01 quantities.
 */
data class QuoteQuantityBreakdown(
    val orderedQuantity: Long,
    val producedQuantity: Long,
    val sellableQuantity: Long,
    val wastageQuantity: Long,
    val wastagePercentage: BigDecimal,
    val impositionUps: Int,                            // inherited from Step 01
    val quantityBasis: String = "SELLABLE"             // explicit basis for pricing
) {
    init {
        require(orderedQuantity > 0) { "Ordered quantity must be positive" }
        require(sellableQuantity > 0) { "Sellable quantity must be positive" }
    }
}

/**
 * Costing assumptions snapshot — persisted for auditability.
 */
data class CostingAssumptions(
    val materialRateSource: String = "STEP01_CALCULATION",
    val machineRateSource: String = "STEP01_CALCULATION",
    val laborRateSource: String = "STEP01_CALCULATION",
    val overheadAllocationPct: BigDecimal = QUOTE_ZERO,
    val wastageCosted: Boolean = true,
    val engineVersion: String = "2.0.0"
)

/**
 * Pricing assumptions snapshot — persisted for auditability.
 */
data class PricingAssumptions(
    val pricingMethod: String = "COST_PLUS",           // COST_PLUS | TARGET_MARGIN | MANUAL
    val markupPercentage: BigDecimal = QUOTE_ZERO,
    val targetMarginPercentage: BigDecimal = QUOTE_ZERO,
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: BigDecimal = QUOTE_ZERO,
    val taxPercentage: BigDecimal = QUOTE_ZERO,
    val customerPricingNote: String? = null,
    val engineVersion: String = "2.0.0"
)

// ============================================================
// DOMAIN MODELS
// ============================================================

/**
 * Canonical cost component for a printing quote version.
 */
data class PrintingCostComponent(
    val componentId: String,
    val versionId: String,
    val quoteId: String,
    val tenantId: String = "",
    val componentType: CostComponentType,
    val componentCode: String,
    val description: String,
    val quantity: BigDecimal,
    val unit: String,
    val unitRate: BigDecimal?,
    val amount: BigDecimal,
    val formulaReference: String,
    val sourceRef: String? = null,
    val isApplicable: Boolean = true,
    val sortOrder: Int = 0
)

/**
 * Quantity-tier pricing record.
 */
data class PrintingQuantityTier(
    val tierId: String,
    val versionId: String,
    val quoteId: String,
    val tenantId: String = "",
    val tierQuantity: Long,
    val unitCost: BigDecimal,
    val totalCost: BigDecimal,
    val sellingPricePerUnit: BigDecimal,
    val finalTotal: BigDecimal,
    val grossMarginPercentage: BigDecimal,
    val isBaseTier: Boolean = false
)

/**
 * Immutable pricing snapshot embedded in a quote version.
 */
data class PrintingPricingSnapshot(
    val baseSellingPrice: BigDecimal,
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: BigDecimal = QUOTE_ZERO,
    val discountAmount: BigDecimal = QUOTE_ZERO,
    val taxPercentage: BigDecimal = QUOTE_ZERO,
    val taxAmount: BigDecimal = QUOTE_ZERO,
    val finalQuoteTotal: BigDecimal,
    val markupAmount: BigDecimal,
    val markupPercentage: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal,
    val contributionAmount: BigDecimal,
    val contributionMarginPercentage: BigDecimal,
    val breakEvenPrice: BigDecimal,
    val breakEvenQuantity: Long,
    val targetMarginPrice: BigDecimal?,
    val targetMarginPercentage: BigDecimal?
)

/**
 * Immutable quotation version snapshot.
 */
data class PrintingQuoteVersion(
    val versionId: String,
    val quoteId: String,
    val tenantId: String,
    val projectId: String,
    val versionNumber: Int,
    val status: QuoteStatus,
    val currency: String,

    // Step 01 provenance
    val calculationId: String,
    val specFingerprint: String,
    val calcFingerprint: String,

    // Quantity economics
    val quantityBreakdown: QuoteQuantityBreakdown,

    // Assumptions
    val costingAssumptions: CostingAssumptions,
    val pricingAssumptions: PricingAssumptions,

    // Calculated totals
    val totalCost: BigDecimal,
    val unitCost: BigDecimal,
    val pricing: PrintingPricingSnapshot,

    // Immutability
    val integrityHash: String,
    val createdBy: String,
    val createdAt: Long,
    val isApproved: Boolean = false,

    // Attached detail (loaded separately)
    val costComponents: List<PrintingCostComponent> = emptyList(),
    val quantityTiers: List<PrintingQuantityTier> = emptyList()
)

/**
 * Canonical printing quote header.
 */
data class PrintingQuote(
    val quoteId: String,
    val tenantId: String,
    val projectId: String,
    val quoteNumber: String,
    val jobTitle: String,
    val calculationId: String,
    val requestFingerprint: String,
    val status: QuoteStatus,
    val currentVersion: Int,
    val currency: String,
    val orderedQuantity: Long,
    val customerRef: String? = null,
    val customerNote: String? = null,
    val internalNote: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val approvedAt: Long? = null,
    val approvedBy: String? = null,
    val expiresAt: Long? = null,
    val integrityHash: String,
    val versions: List<PrintingQuoteVersion> = emptyList()
)

/**
 * Audit event record.
 */
data class QuoteAuditEvent(
    val auditId: String,
    val quoteId: String,
    val versionId: String?,
    val tenantId: String,
    val projectId: String,
    val eventType: QuoteAuditEventType,
    val actor: String,
    val description: String,
    val beforeStatus: QuoteStatus?,
    val afterStatus: QuoteStatus?,
    val metadataJson: String? = null,
    val occurredAt: Long
)

/**
 * Provenance record for a quote version.
 */
data class QuoteProvenance(
    val provenanceId: String,
    val quoteId: String,
    val versionId: String,
    val tenantId: String,
    val projectId: String,
    val calculationId: String,
    val calculationVersion: String,
    val calculationStatus: CalculationStatus,
    val specFingerprint: String,
    val calcFingerprint: String,
    val costingEngineVersion: String = "2.0.0",
    val pricingEngineVersion: String = "2.0.0",
    val assumptionsJson: String,
    val step01BreakdownJson: String?,
    val capturedAt: Long,
    val capturedBy: String
)

/**
 * Reconciliation result record.
 */
data class QuoteReconciliationEvent(
    val reconciliationId: String,
    val quoteId: String,
    val versionId: String,
    val tenantId: String,
    val projectId: String,
    val isReconciled: Boolean,
    val totalCostCheck: Boolean,
    val revenueIdentityCheck: Boolean,
    val grossProfitCheck: Boolean,
    val marginCheck: Boolean,
    val markupCheck: Boolean,
    val breakevenCheck: Boolean,
    val discrepanciesJson: String?,
    val reconciledAt: Long,
    val reconciledBy: String,
    val integrityHash: String
)

// ============================================================
// REQUEST MODELS
// ============================================================

/**
 * Request to create a new printing quotation.
 */
data class CreatePrintingQuoteRequest(
    val tenantId: String,
    val projectId: String,
    val calculationId: String,
    val jobTitle: String,
    val customerRef: String? = null,
    val customerNote: String? = null,
    val internalNote: String? = null,
    val currency: String = "BDT",
    val idempotencyKey: String? = null,
    val requestedBy: String
)

/**
 * Request to calculate/recalculate pricing for a quote.
 */
data class CalculatePrintingQuoteRequest(
    val quoteId: String,
    val tenantId: String,
    val projectId: String,
    val costingAssumptions: CostingAssumptions = CostingAssumptions(),
    val pricingAssumptions: PricingAssumptions = PricingAssumptions(),
    val quantityTierBreaks: List<Long> = emptyList(),
    val requestedBy: String
)

/**
 * Request to approve or reject a quote in REVIEW status.
 */
data class QuoteReviewRequest(
    val quoteId: String,
    val tenantId: String,
    val projectId: String,
    val approved: Boolean,
    val reason: String? = null,
    val requestedBy: String
)

// ============================================================
// AI HANDOFF CONTRACT (Module 17 Step 02)
// ============================================================

/**
 * Read-only, deterministic, cryptographically fingerprinted downstream handoff contract.
 * Intended for AI agents and external consumers.
 * Module 17 Step 02.
 */
data class Module17Step02PrintingQuotationHandoffContract(
    val handoffId: String,
    val quoteId: String,
    val versionId: String,
    val versionNumber: Int,
    val tenantId: String,
    val projectId: String,
    val generatedAt: Long,
    val contractVersion: String = "2.0.0",

    // Identity
    val quoteNumber: String,
    val jobTitle: String,
    val currency: String,
    val status: String,

    // Specification summary (from Step 01)
    val calculationId: String,
    val specFingerprint: String,
    val calcFingerprint: String,

    // Production quantities (all explicit)
    val orderedQuantity: Long,
    val producedQuantity: Long,
    val sellableQuantity: Long,
    val wastageQuantity: Long,
    val wastagePercentage: String,
    val impositionUps: Int,

    // Cost breakdown summary
    val costComponents: List<Map<String, String>>,
    val totalCost: String,
    val unitCost: String,

    // Pricing
    val pricingMethod: String,
    val baseSellingPrice: String,
    val discountType: String,
    val discountAmount: String,
    val taxPercentage: String,
    val taxAmount: String,
    val finalQuoteTotal: String,

    // Margin intelligence
    val markupAmount: String,
    val markupPercentage: String,
    val grossProfit: String,
    val grossMarginPercentage: String,

    // Break-even
    val breakEvenPrice: String,
    val breakEvenQuantity: Long,
    val targetMarginPrice: String?,
    val targetMarginPercentage: String?,

    // Quantity tiers
    val quantityTiers: List<Map<String, String>>,

    // Risk / pricing flags
    val riskFlags: List<String>,

    // Provenance
    val costingEngineVersion: String,
    val pricingEngineVersion: String,
    val reconciliationStatus: String,

    // Integrity
    val integrityHash: String,

    // Contractual constraints
    val isReadOnly: Boolean = true,
    val isMutable: Boolean = false,
    val containsSecrets: Boolean = false,
    val containsCredentials: Boolean = false
)
