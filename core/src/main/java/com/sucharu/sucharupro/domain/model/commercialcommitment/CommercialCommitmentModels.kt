package com.sucharu.sucharupro.domain.model.commercialcommitment

import java.math.BigDecimal
import java.math.RoundingMode

// ============================================================
// PRECISION CONSTANTS & HELPERS (Module 17 Step 03)
// ============================================================

val COMMITMENT_SCALE = 4
val COMMITMENT_ROUNDING = RoundingMode.HALF_UP
val COMMITMENT_ZERO: BigDecimal = BigDecimal.ZERO.setScale(COMMITMENT_SCALE, COMMITMENT_ROUNDING)
val COMMITMENT_ONE_HUNDRED: BigDecimal = BigDecimal("100").setScale(COMMITMENT_SCALE, COMMITMENT_ROUNDING)

fun BigDecimal.c4(): BigDecimal = this.setScale(COMMITMENT_SCALE, COMMITMENT_ROUNDING)
fun BigDecimal?.c4OrZero(): BigDecimal = this?.setScale(COMMITMENT_SCALE, COMMITMENT_ROUNDING) ?: COMMITMENT_ZERO

// ============================================================
// ENUMS
// ============================================================

/**
 * Lifecycle status of a Commercial Commitment.
 * Module 17 Step 03.
 */
enum class CommitmentStatus {
    PENDING,
    READY_FOR_CONVERSION,
    CONVERTED,
    CANCELLED,
    EXPIRED,
    BLOCKED;

    fun canTransitionTo(next: CommitmentStatus): Boolean = when (this) {
        PENDING              -> next == READY_FOR_CONVERSION || next == CANCELLED || next == BLOCKED
        READY_FOR_CONVERSION -> next == CONVERTED || next == CANCELLED || next == EXPIRED || next == BLOCKED
        CONVERTED            -> false // Terminal immutable state
        CANCELLED            -> false
        EXPIRED              -> false
        BLOCKED              -> next == PENDING || next == READY_FOR_CONVERSION || next == CANCELLED
    }
}

/**
 * Event types for commercial commitment audit and lifecycle timeline.
 */
enum class CommercialCommitmentEventType {
    COMMITMENT_PREPARED,
    ELIGIBILITY_CHECKED,
    CONVERSION_INITIATED,
    CONVERSION_COMPLETED,
    CONVERSION_FAILED,
    COMMITMENT_CANCELLED,
    COMMITMENT_RECONCILED,
    HANDOFF_EXPORTED
}

// ============================================================
// DOMAIN ENTITIES & VALUE OBJECTS
// ============================================================

/**
 * Canonical Commercial Commitment Entity.
 * Represents the legally/commercially binding transition record between an approved
 * printing quotation version and a confirmed downstream Order.
 */
data class CommercialCommitment(
    val commitmentId: String,
    val tenantId: String,
    val projectId: String,
    val quotationId: String,
    val quotationVersion: Int,
    val customerId: String,
    val orderId: String? = null,
    val orderNumber: String? = null,
    val status: CommitmentStatus = CommitmentStatus.PENDING,
    val committedQuantity: Long,
    val approvedUnitPrice: BigDecimal,
    val approvedSubtotal: BigDecimal,
    val approvedDiscount: BigDecimal = COMMITMENT_ZERO,
    val approvedTax: BigDecimal = COMMITMENT_ZERO,
    val approvedGrandTotal: BigDecimal,
    val currency: String = "BDT",
    val paymentTerms: String = "DEFAULT",
    val deliveryTerms: String? = null,
    val conversionNotes: String? = null,
    val idempotencyKey: String? = null,
    val integrityHash: String,
    val createdAt: Long,
    val createdBy: String,
    val convertedAt: Long? = null,
    val convertedBy: String? = null
) {
    val isConverted: Boolean get() = status == CommitmentStatus.CONVERTED && orderId != null
}

/**
 * Audit event for tracking all lifecycle transitions of a Commercial Commitment.
 */
data class CommercialCommitmentEvent(
    val eventId: String,
    val commitmentId: String,
    val tenantId: String,
    val projectId: String,
    val eventType: CommercialCommitmentEventType,
    val actor: String,
    val actorRole: String = "SYSTEM",
    val detailsJson: String? = null,
    val occurredAt: Long
)

/**
 * Comprehensive Evaluation of Quotation -> Order Conversion Eligibility.
 */
data class ConversionEligibility(
    val isEligible: Boolean,
    val reasons: List<String> = emptyList(),
    val quotationId: String,
    val quotationVersion: Int,
    val quotationStatus: String,
    val customerId: String,
    val orderedQuantity: Long,
    val approvedGrandTotal: BigDecimal,
    val currency: String,
    val existingCommitmentId: String? = null,
    val existingOrderId: String? = null,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Result of executing the atomic quotation to order conversion.
 */
data class ConversionResult(
    val isSuccess: Boolean,
    val commitment: CommercialCommitment,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val committedAmount: BigDecimal,
    val currency: String,
    val message: String
)

/**
 * Conversion Request Value Object.
 */
data class ConvertQuotationToOrderRequest(
    val quotationId: String,
    val tenantId: String,
    val projectId: String,
    val targetVersionNumber: Int? = null,
    val requestedQuantity: Long? = null,
    val customOrderNumber: String? = null,
    val priority: String = "NORMAL",
    val paymentTerms: String? = null,
    val deliveryTerms: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val requestedBy: String
)

/**
 * Mathematical and Business Reconciliation Result between Quotation, Commitment, and Order.
 */
data class CommercialCommitmentReconciliationResult(
    val commitmentId: String,
    val quotationId: String,
    val orderId: String?,
    val isFullyReconciled: Boolean,
    val customerMatch: Boolean,
    val currencyMatch: Boolean,
    val quantityMatch: Boolean,
    val amountMatch: Boolean,
    val statusIntegrityMatch: Boolean,
    val discrepancies: List<String> = emptyList(),
    val reconciledAt: Long = System.currentTimeMillis()
)

/**
 * Read-Only, Cryptographically Fingerprinted AI Agent Handoff Contract.
 * Module 17 Step 03.
 */
data class Module17Step03CommercialCommitmentHandoffContract(
    val handoffId: String,
    val contractVersion: String = "1.0.0",
    val commitmentId: String,
    val tenantId: String,
    val projectId: String,
    val quotationId: String,
    val quotationVersion: Int,
    val quotationNumber: String,
    val customerId: String,
    val orderId: String?,
    val orderNumber: String?,
    val commitmentStatus: String,
    val committedQuantity: Long,
    val approvedUnitPrice: String,
    val approvedSubtotal: String,
    val approvedDiscount: String,
    val approvedTax: String,
    val approvedGrandTotal: String,
    val currency: String,
    val paymentTerms: String,
    val deliveryTerms: String?,
    val conversionTimestamp: Long?,
    val reconciliationStatus: String,
    val isReadOnly: Boolean = true,
    val isMutable: Boolean = false,
    val integrityHash: String,
    val generatedAt: Long
)
