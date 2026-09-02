package com.sucharu.sucharupro.domain.model.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * State machine for Request for Quotation (Module 13 Step 03).
 */
enum class VendorRfqStatus {
    DRAFT,
    PUBLISHED,
    OPEN,
    CLOSING,
    CLOSED,
    EVALUATION,
    AWARDED,
    CANCELLED,
    EXPIRED;

    val isEditable: Boolean get() = this == DRAFT
    val isBiddable: Boolean get() = this in setOf(PUBLISHED, OPEN, CLOSING)
    val isClosedForBidding: Boolean get() = this in setOf(CLOSED, EVALUATION, AWARDED, CANCELLED, EXPIRED)
    val isTerminal: Boolean get() = this in setOf(AWARDED, CANCELLED, EXPIRED)

    fun canTransitionTo(target: VendorRfqStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(PUBLISHED, CANCELLED)
            PUBLISHED -> target in setOf(OPEN, CANCELLED, EXPIRED)
            OPEN -> target in setOf(CLOSING, CLOSED, CANCELLED, EXPIRED)
            CLOSING -> target in setOf(CLOSED, CANCELLED, EXPIRED)
            CLOSED -> target in setOf(EVALUATION, CANCELLED)
            EVALUATION -> target in setOf(AWARDED, CANCELLED)
            AWARDED -> false
            CANCELLED -> false
            EXPIRED -> false
        }
    }
}

/**
 * Lifecycle status of a vendor invitation to an RFQ.
 */
enum class VendorRfqInvitationStatus {
    INVITED,
    VIEWED,
    ACKNOWLEDGED,
    DECLINED,
    RESPONDED,
    WITHDRAWN,
    EXPIRED;

    fun canTransitionTo(target: VendorRfqInvitationStatus): Boolean {
        if (this == target) return true
        return when (this) {
            INVITED -> target in setOf(VIEWED, ACKNOWLEDGED, DECLINED, RESPONDED, EXPIRED)
            VIEWED -> target in setOf(ACKNOWLEDGED, DECLINED, RESPONDED, EXPIRED)
            ACKNOWLEDGED -> target in setOf(DECLINED, RESPONDED, EXPIRED)
            DECLINED -> false
            RESPONDED -> target in setOf(WITHDRAWN, EXPIRED)
            WITHDRAWN -> target in setOf(RESPONDED, EXPIRED)
            EXPIRED -> false
        }
    }
}

/**
 * Lifecycle status of a vendor quotation / bid.
 */
enum class VendorQuotationStatus {
    DRAFT,
    IN_PROGRESS,
    SUBMITTED,
    UNDER_REVIEW,
    REVISION_REQUESTED,
    REVISED,
    WITHDRAWN,
    ACCEPTED,
    REJECTED,
    EXPIRED;

    val isEditable: Boolean get() = this in setOf(DRAFT, IN_PROGRESS)
    val isSubmittedOrActive: Boolean get() = this in setOf(SUBMITTED, UNDER_REVIEW, REVISED, ACCEPTED)
    val isTerminal: Boolean get() = this in setOf(WITHDRAWN, ACCEPTED, REJECTED, EXPIRED)

    fun canTransitionTo(target: VendorQuotationStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(IN_PROGRESS, SUBMITTED, WITHDRAWN)
            IN_PROGRESS -> target in setOf(SUBMITTED, WITHDRAWN)
            SUBMITTED -> target in setOf(UNDER_REVIEW, REVISION_REQUESTED, WITHDRAWN, ACCEPTED, REJECTED, EXPIRED)
            UNDER_REVIEW -> target in setOf(REVISION_REQUESTED, WITHDRAWN, ACCEPTED, REJECTED, EXPIRED)
            REVISION_REQUESTED -> target in setOf(REVISED, WITHDRAWN, EXPIRED)
            REVISED -> target in setOf(UNDER_REVIEW, REVISION_REQUESTED, WITHDRAWN, ACCEPTED, REJECTED, EXPIRED)
            WITHDRAWN -> false
            ACCEPTED -> false
            REJECTED -> false
            EXPIRED -> false
        }
    }
}

/**
 * Status of an RFQ clarification question.
 */
enum class VendorClarificationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ANSWERED,
    REJECTED
}

/**
 * Visibility scope for clarification answers.
 */
enum class VendorClarificationVisibility {
    PUBLIC_TO_ALL_INVITED,
    PRIVATE_TO_VENDOR
}

/**
 * Evaluation recommendation decision for a vendor quotation.
 */
enum class VendorRfqEvaluationDecision {
    RECOMMENDED_FOR_AWARD,
    RESERVE_CANDIDATE,
    REJECTED,
    UNDER_CONSIDERATION
}

/**
 * Type of RFQ audit event.
 */
enum class VendorRfqAuditEventType {
    RFQ_CREATED,
    RFQ_UPDATED,
    RFQ_PUBLISHED,
    RFQ_OPENED,
    RFQ_CLOSED,
    RFQ_CANCELLED,
    DEADLINE_EXTENDED,
    VENDOR_INVITED,
    INVITATION_VIEWED,
    INVITATION_ACKNOWLEDGED,
    INVITATION_DECLINED,
    QUOTATION_CREATED,
    QUOTATION_UPDATED,
    QUOTATION_SUBMITTED,
    QUOTATION_WITHDRAWN,
    QUOTATION_REVISION_REQUESTED,
    QUOTATION_REVISED,
    CLARIFICATION_CREATED,
    CLARIFICATION_ANSWERED,
    EVALUATION_CREATED,
    EVALUATION_APPROVED,
    RFQ_AWARDED
}

/**
 * Line item in an RFQ specifying a service/material requirement.
 */
data class VendorRfqItem(
    val rfqItemId: String,
    val rfqId: String,
    val sequenceNumber: Int,
    val itemCode: String? = null,
    val description: String,
    val requiredCapabilityType: String? = null,
    val quantity: BigDecimal,
    val unitOfMeasure: String = "UNIT",
    val targetUnitPrice: Money? = null,
    val targetDeliveryDate: Long? = null,
    val specifications: String? = null,
    val notes: String? = null,
    val version: Long = 1L
)

/**
 * Formal Request for Quotation aggregate root (Module 13 Step 03).
 */
data class VendorRfq(
    val rfqId: String,
    val tenantId: String,
    val projectId: String,
    val rfqNumber: String,
    val title: String,
    val description: String? = null,
    val requestedBy: String,
    val issueDate: Long = System.currentTimeMillis(),
    val responseDeadline: Long,
    val currency: String = "BDT",
    val deliveryRequirements: String? = null,
    val paymentTerms: String? = null,
    val shippingTerms: String? = null,
    val requiredCapabilities: List<String> = emptyList(),
    val status: VendorRfqStatus = VendorRfqStatus.DRAFT,
    val items: List<VendorRfqItem> = emptyList(),
    val awardDecision: VendorRfqAwardDecision? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val version: Long = 1L
)

/**
 * Formal invitation of a vendor to participate in an RFQ.
 */
data class VendorRfqInvitation(
    val invitationId: String,
    val rfqId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String,
    val status: VendorRfqInvitationStatus = VendorRfqInvitationStatus.INVITED,
    val invitedAt: Long = System.currentTimeMillis(),
    val viewedAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val respondedAt: Long? = null,
    val declineReason: String? = null,
    val version: Long = 1L
)

/**
 * Line item in a vendor quotation / bid.
 */
data class VendorQuotationItem(
    val quotationItemId: String,
    val quotationId: String,
    val rfqItemId: String,
    val quantity: BigDecimal,
    val unitPrice: Money,
    val discountAmount: Money = Money.ZERO,
    val taxAmount: Money = Money.ZERO,
    val lineTotal: Money,
    val deliveryLeadTimeDays: Int = 0,
    val notes: String? = null,
    val version: Long = 1L
)

/**
 * Formal quotation / bid submitted by an invited vendor.
 */
data class VendorQuotation(
    val quotationId: String,
    val rfqId: String,
    val invitationId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String,
    val quotationNumber: String,
    val vendorReferenceNumber: String? = null,
    val revisionNumber: Int = 1,
    val currency: String = "BDT",
    val validityPeriodDays: Int = 30,
    val paymentTerms: String? = null,
    val deliveryLeadTimeDays: Int = 0,
    val shippingTerms: String? = null,
    val notes: String? = null,
    val subtotal: Money = Money.ZERO,
    val totalDiscount: Money = Money.ZERO,
    val totalTax: Money = Money.ZERO,
    val grandTotal: Money = Money.ZERO,
    val status: VendorQuotationStatus = VendorQuotationStatus.DRAFT,
    val items: List<VendorQuotationItem> = emptyList(),
    val submittedAt: Long? = null,
    val submittedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val version: Long = 1L
)

/**
 * Immutable historical snapshot of a previous quotation revision.
 */
data class VendorQuotationRevision(
    val revisionId: String,
    val quotationId: String,
    val rfqId: String,
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val revisionNumber: Int,
    val reasonForRevision: String,
    val snapshotSubtotal: Money,
    val snapshotGrandTotal: Money,
    val itemsSnapshotJson: String,
    val revisedBy: String,
    val revisedAt: Long = System.currentTimeMillis()
)

/**
 * Clarification question and official answer for an RFQ.
 */
data class VendorRfqClarification(
    val clarificationId: String,
    val rfqId: String,
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val question: String,
    val askedBy: String,
    val askedAt: Long = System.currentTimeMillis(),
    val answer: String? = null,
    val answeredBy: String? = null,
    val answeredAt: Long? = null,
    val status: VendorClarificationStatus = VendorClarificationStatus.SUBMITTED,
    val visibility: VendorClarificationVisibility = VendorClarificationVisibility.PUBLIC_TO_ALL_INVITED,
    val version: Long = 1L
)

/**
 * Evaluated criterion score in an RFQ bid evaluation scorecard.
 */
data class VendorRfqEvaluationScore(
    val criterion: String,
    val weightPercent: Double,
    val rawScore: Double,
    val weightedScore: Double,
    val evaluatorNotes: String? = null
)

/**
 * Formal evaluation scorecard for a vendor quotation.
 */
data class VendorRfqEvaluation(
    val evaluationId: String,
    val rfqId: String,
    val quotationId: String,
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val evaluatorUserId: String,
    val scores: List<VendorRfqEvaluationScore> = emptyList(),
    val totalScore: Double = 0.0,
    val decision: VendorRfqEvaluationDecision = VendorRfqEvaluationDecision.UNDER_CONSIDERATION,
    val remarks: String? = null,
    val evaluatedAt: Long = System.currentTimeMillis(),
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val version: Long = 1L
)

/**
 * Award decision captured upon completing RFQ evaluation.
 */
data class VendorRfqAwardDecision(
    val awardId: String,
    val rfqId: String,
    val winningVendorId: String,
    val winningQuotationId: String,
    val awardReason: String,
    val awardedAmount: Money,
    val awardedBy: String,
    val awardedAt: Long = System.currentTimeMillis()
)

/**
 * Comparison projection snapshot across multiple submitted vendor bids.
 */
data class VendorRfqComparisonItem(
    val quotationId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val grandTotal: Money,
    val deliveryLeadTimeDays: Int,
    val evaluationScore: Double?,
    val decision: VendorRfqEvaluationDecision?,
    val submittedAt: Long?
)

data class VendorRfqComparisonSnapshot(
    val rfqId: String,
    val rfqNumber: String,
    val title: String,
    val totalInvited: Int,
    val totalBidsReceived: Int,
    val lowestBidAmount: Money?,
    val highestBidAmount: Money?,
    val averageBidAmount: Money?,
    val comparisonItems: List<VendorRfqComparisonItem> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable audit record for all RFQ and Quotation operations.
 */
data class VendorRfqAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val rfqId: String,
    val vendorId: String? = null,
    val quotationId: String? = null,
    val actorUserId: String,
    val eventType: VendorRfqAuditEventType,
    val action: String,
    val details: String? = null,
    val ipAddress: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
