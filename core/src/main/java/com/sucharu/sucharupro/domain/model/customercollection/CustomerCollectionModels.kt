package com.sucharu.sucharupro.domain.model.customercollection

import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.ReceivableAgingBucket
import java.math.BigDecimal

/**
 * Types of operational collection actions.
 */
enum class CollectionActionType {
    REMINDER,
    PHONE_FOLLOW_UP,
    MESSAGE,
    EMAIL,
    PAYMENT_PROMISE,
    ESCALATION,
    HOLD_REVIEW,
    OTHER
}

/**
 * Priority levels for collection tasks.
 */
enum class CollectionPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * Lifecycle status for collection actions.
 */
enum class CollectionActionStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

/**
 * Controlled outcome options when completing a collection action.
 */
enum class CollectionOutcomeType {
    CONTACTED,
    PAYMENT_PROMISED,
    PAYMENT_RECEIVED,
    NO_RESPONSE,
    CUSTOMER_DISPUTE,
    FOLLOW_UP_REQUIRED,
    ESCALATED
}

/**
 * Lifecycle status for payment promises.
 */
enum class PaymentPromiseStatus {
    PENDING,
    FULFILLED,
    BROKEN,
    CANCELLED
}

/**
 * Operational collection action aggregate entity.
 */
data class CustomerCollectionAction(
    val actionId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val invoiceId: String? = null,
    val actionType: CollectionActionType = CollectionActionType.REMINDER,
    val priority: CollectionPriority = CollectionPriority.NORMAL,
    val status: CollectionActionStatus = CollectionActionStatus.SCHEDULED,
    val scheduledAt: Long,
    val performedAt: Long? = null,
    val nextFollowUpAt: Long? = null,
    val assignedUserId: String? = null,
    val outcome: CollectionOutcomeType? = null,
    val outcomeNotes: String? = null,
    val cancellationReason: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long = 1
)

/**
 * Operational promise-to-pay record.
 * NOTE: This is NOT a financial transaction. It does not alter ledger or invoice balances.
 */
data class CustomerPaymentPromise(
    val promiseId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val invoiceId: String? = null,
    val actionId: String? = null,
    val promisedAmount: BigDecimal,
    val promisedDate: Long,
    val status: PaymentPromiseStatus = PaymentPromiseStatus.PENDING,
    val notes: String? = null,
    val fulfilledAt: Long? = null,
    val fulfilledPaymentId: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long = 1
)

/**
 * Due schedule item representation for upcoming, due today, or overdue invoices.
 */
data class ReceivableDueScheduleItem(
    val invoiceId: String,
    val invoiceNumber: String,
    val customerId: String,
    val dueDate: Long,
    val dueAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val daysOverdue: Int,
    val agingBucket: ReceivableAgingBucket,
    val priority: CollectionPriority
)

/**
 * Queue item projected for staff collection dashboard.
 */
data class CollectionQueueItem(
    val customerId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val invoiceId: String? = null,
    val invoiceNumber: String? = null,
    val totalOutstanding: BigDecimal,
    val overdueAmount: BigDecimal,
    val oldestDueInvoiceDate: Long? = null,
    val maxDaysOverdue: Int = 0,
    val agingBucket: ReceivableAgingBucket,
    val creditRiskStatus: CustomerCreditRiskStatus,
    val financialHold: Boolean,
    val priority: CollectionPriority,
    val latestActionId: String? = null,
    val latestActionType: CollectionActionType? = null,
    val latestActionStatus: CollectionActionStatus? = null,
    val nextFollowUpAt: Long? = null,
    val assignedUserId: String? = null,
    val activePromiseCount: Int = 0,
    val activePromisedAmount: BigDecimal = BigDecimal.ZERO
)

/**
 * Comprehensive customer collection summary projection.
 */
data class CustomerReceivableCollectionSummary(
    val customerId: String,
    val totalOutstanding: BigDecimal,
    val dueTodayAmount: BigDecimal,
    val upcomingDueAmount: BigDecimal,
    val overdueAmount: BigDecimal,
    val criticalOverdueAmount: BigDecimal,
    val overdueInvoiceCount: Int,
    val pendingActionCount: Int,
    val completedActionCount: Int,
    val activePromiseCount: Int,
    val activePromisedAmount: BigDecimal,
    val creditRiskStatus: CustomerCreditRiskStatus,
    val financialHold: Boolean,
    val priority: CollectionPriority,
    val nextFollowUpAt: Long? = null,
    val latestOutcome: CollectionOutcomeType? = null
)

/**
 * Audit event entity for collection actions and promises.
 */
data class CustomerCollectionAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val actionId: String? = null,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousValueJson: String? = null,
    val newValueJson: String? = null,
    val reason: String? = null,
    val occurredAt: Long,
    val metadataJson: String? = null
)
