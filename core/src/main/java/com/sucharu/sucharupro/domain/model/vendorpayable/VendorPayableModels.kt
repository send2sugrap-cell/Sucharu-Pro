package com.sucharu.sucharupro.domain.model.vendorpayable

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Status lifecycle of a Vendor Payable liability (Module 15 Step 02).
 */
enum class VendorPayableStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    REJECTED,
    CANCELLED,
    VOIDED;

    val isTerminal: Boolean
        get() = this in setOf(PAID, REJECTED, CANCELLED, VOIDED)

    val canBeEdited: Boolean
        get() = this in setOf(DRAFT, REJECTED)

    val canBeSubmitted: Boolean
        get() = this in setOf(DRAFT, REJECTED)

    val canBeApproved: Boolean
        get() = this == SUBMITTED

    val canReceivePayment: Boolean
        get() = this in setOf(APPROVED, PARTIALLY_PAID)

    fun canTransitionTo(target: VendorPayableStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(SUBMITTED, CANCELLED)
            SUBMITTED -> target in setOf(APPROVED, REJECTED, CANCELLED)
            APPROVED -> target in setOf(PARTIALLY_PAID, PAID, VOIDED)
            PARTIALLY_PAID -> target in setOf(PAID, VOIDED)
            PAID, REJECTED, CANCELLED, VOIDED -> false
        }
    }
}

/**
 * Standard payment terms for calculating due dates deterministically.
 */
enum class VendorPayablePaymentTerms(val defaultDays: Int) {
    IMMEDIATE(0),
    NET_7(7),
    NET_15(15),
    NET_30(30),
    NET_45(45),
    NET_60(60),
    CUSTOM(0);

    fun calculateDueDate(issueDate: Long, customDays: Int? = null): Long {
        val daysToAdd = if (this == CUSTOM) (customDays ?: 0) else defaultDays
        return issueDate + (daysToAdd.toLong() * 24L * 60L * 60L * 1000L)
    }
}

/**
 * Supported payment methods for vendor payable settlement.
 */
enum class VendorPayablePaymentMethod(val requiresReference: Boolean) {
    CASH(false),
    BANK(true),
    CHEQUE(true),
    MOBILE_BANKING(true),
    CARD(true),
    PETTY_CASH(false),
    OTHER(false)
}

/**
 * Deterministic aging buckets for vendor payable liabilities.
 */
enum class VendorPayableAgingBucket(val label: String) {
    CURRENT("Current / Not Due"),
    DAYS_1_7("1 - 7 Days Overdue"),
    DAYS_8_30("8 - 30 Days Overdue"),
    DAYS_31_60("31 - 60 Days Overdue"),
    DAYS_61_90("61 - 90 Days Overdue"),
    DAYS_90_PLUS("90+ Days Overdue");

    companion object {
        fun calculateBucket(dueDate: Long, asOfTimestamp: Long = System.currentTimeMillis()): VendorPayableAgingBucket {
            if (asOfTimestamp <= dueDate) return CURRENT
            val overdueMillis = asOfTimestamp - dueDate
            val overdueDays = (overdueMillis / (24L * 60L * 60L * 1000L)).toInt()
            return when {
                overdueDays <= 7 -> DAYS_1_7
                overdueDays <= 30 -> DAYS_8_30
                overdueDays <= 60 -> DAYS_31_60
                overdueDays <= 90 -> DAYS_61_90
                else -> DAYS_90_PLUS
            }
        }
    }
}

/**
 * Canonical Vendor Payable Liability aggregate root entity.
 */
data class VendorPayable(
    val payableId: String,
    val tenantId: String,
    val projectId: String,
    val payableNumber: String,
    val vendorId: String,
    val jobId: String? = null,
    val vendorJobId: String? = null,
    val billReference: String? = null,
    val description: String,
    val notes: String? = null,
    val originalAmount: BigDecimal,
    val paidAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val issueDate: Long,
    val paymentTerms: VendorPayablePaymentTerms = VendorPayablePaymentTerms.NET_30,
    val customTermDays: Int? = null,
    val dueDate: Long,
    val status: VendorPayableStatus = VendorPayableStatus.DRAFT,
    val attachmentUrl: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val submittedBy: String? = null,
    val submittedAt: Long? = null,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val rejectedBy: String? = null,
    val rejectedAt: Long? = null,
    val recheckRequestedBy: String? = null,
    val recheckRequestedAt: Long? = null,
    val cancelledBy: String? = null,
    val cancelledAt: Long? = null,
    val voidedBy: String? = null,
    val voidedAt: Long? = null,
    val rejectionReason: String? = null,
    val cancellationReason: String? = null,
    val voidReason: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val version: Long = 1L
) {
    /**
     * Outstanding liability balance owed to the vendor.
     * Guaranteed invariant: originalAmount - paidAmount >= 0
     */
    val outstandingAmount: BigDecimal
        get() {
            val diff = originalAmount.subtract(paidAmount).setScale(4, RoundingMode.HALF_UP)
            return if (diff < BigDecimal.ZERO) BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) else diff
        }

    fun getAgingBucket(asOfTimestamp: Long = System.currentTimeMillis()): VendorPayableAgingBucket {
        return VendorPayableAgingBucket.calculateBucket(dueDate, asOfTimestamp)
    }

    val isOverdue: Boolean
        get() = status in setOf(VendorPayableStatus.APPROVED, VendorPayableStatus.PARTIALLY_PAID) &&
                System.currentTimeMillis() > dueDate
}

/**
 * Record of payment allocation against a vendor payable.
 */
data class VendorPayablePaymentAllocation(
    val allocationId: String,
    val tenantId: String,
    val projectId: String,
    val payableId: String,
    val vendorId: String,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val paymentMethod: VendorPayablePaymentMethod,
    val paymentReference: String? = null,
    val paymentDate: Long,
    val notes: String? = null,
    val allocatedBy: String,
    val allocatedAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null,
    val version: Long = 1L
)

/**
 * Immutable append-only audit event for vendor payable lifecycle transitions.
 */
data class VendorPayableAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val payableId: String,
    val vendorId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val previousStatus: VendorPayableStatus? = null,
    val newStatus: VendorPayableStatus? = null,
    val amount: BigDecimal? = null,
    val reason: String? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null,
    val metadataJson: String? = null
)

/**
 * Vendor-level liability summary.
 */
data class VendorPayableSummary(
    val vendorId: String,
    val totalApprovedLiability: BigDecimal,
    val totalPaid: BigDecimal,
    val totalOutstanding: BigDecimal,
    val totalOverdue: BigDecimal,
    val currentDue: BigDecimal,
    val dueToday: BigDecimal,
    val upcomingDue: BigDecimal,
    val draftCount: Int,
    val submittedCount: Int,
    val approvedCount: Int,
    val partiallyPaidCount: Int,
    val paidCount: Int,
    val rejectedCount: Int,
    val cancelledCount: Int,
    val voidedCount: Int,
    val currency: String = "BDT"
)

/**
 * Aging breakdown item.
 */
data class VendorPayableAgingItem(
    val bucket: VendorPayableAgingBucket,
    val count: Int,
    val totalAmount: BigDecimal,
    val outstandingAmount: BigDecimal
)

/**
 * Aging summary report.
 */
data class VendorPayableAgingReport(
    val vendorId: String?,
    val asOfDate: Long,
    val buckets: List<VendorPayableAgingItem>,
    val totalOutstanding: BigDecimal,
    val currency: String = "BDT"
)
