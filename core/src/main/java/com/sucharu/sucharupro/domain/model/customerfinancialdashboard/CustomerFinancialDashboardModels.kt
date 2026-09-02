package com.sucharu.sucharupro.domain.model.customerfinancialdashboard

import com.sucharu.sucharupro.domain.model.customercollection.CollectionOutcomeType
import com.sucharu.sucharupro.domain.model.customercollection.CollectionPriority
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import com.sucharu.sucharupro.domain.model.customercreditcontrol.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import java.math.BigDecimal

/**
 * Type classifications for financial dashboard warnings.
 */
enum class FinancialWarningType {
    FINANCIAL_HOLD,
    CREDIT_LIMIT_EXCEEDED,
    ADVANCE_REQUIRED,
    OVERDUE_RECEIVABLE,
    DUE_TODAY,
    PAYMENT_PROMISE_DUE,
    RECONCILIATION_DISCREPANCY,
    UNALLOCATED_PAYMENT,
    REFUND_PENDING
}

/**
 * Recommended operational action types in the Action Center.
 */
enum class FinancialActionType {
    REVIEW_OVERDUE,
    RECORD_PAYMENT,
    ALLOCATE_PAYMENT,
    REVIEW_CREDIT,
    REVIEW_HOLD,
    REVIEW_COLLECTION,
    REVIEW_RECONCILIATION,
    REVIEW_REFUND,
    VIEW_STATEMENT
}

/**
 * Financial activity entry types.
 */
enum class FinancialActivityType {
    INVOICE,
    PAYMENT,
    ALLOCATION,
    ADVANCE,
    CREDIT_NOTE,
    ADJUSTMENT,
    REFUND,
    COLLECTION_ACTION,
    PAYMENT_PROMISE
}

/**
 * Actionable financial warning entity.
 */
data class CustomerFinancialWarning(
    val warningType: FinancialWarningType,
    val severity: CollectionPriority,
    val title: String,
    val message: String,
    val relatedEntityId: String? = null,
    val relatedEntityType: String? = null
)

/**
 * Actionable operational action entity for the Action Center.
 */
data class CustomerFinancialAction(
    val actionType: FinancialActionType,
    val priority: CollectionPriority,
    val title: String,
    val description: String,
    val targetRoute: String,
    val relatedEntityId: String? = null
)

/**
 * Unified recent activity entry.
 */
data class CustomerFinancialActivityItem(
    val activityId: String,
    val type: FinancialActivityType,
    val amount: BigDecimal?,
    val referenceNumber: String?,
    val status: String,
    val description: String,
    val occurredAt: Long
)

/**
 * Aging summary projection for dashboard.
 */
data class CustomerReceivableAgingSummary(
    val currentAmount: BigDecimal,
    val days1To7Amount: BigDecimal,
    val days8To30Amount: BigDecimal,
    val days31To60Amount: BigDecimal,
    val days61To90Amount: BigDecimal,
    val days90PlusAmount: BigDecimal,
    val totalAgingOutstanding: BigDecimal,
    val oldestOverdueDate: Long? = null,
    val maxDaysOverdue: Int = 0
)

/**
 * Payment due schedule summary for dashboard.
 */
data class CustomerDueScheduleSummary(
    val upcomingDueAmount: BigDecimal,
    val dueTodayAmount: BigDecimal,
    val overdueAmount: BigDecimal,
    val criticalOverdueAmount: BigDecimal,
    val overdueInvoiceCount: Int
)

/**
 * Collection status summary for dashboard.
 */
data class CustomerCollectionStatusSummary(
    val priority: CollectionPriority,
    val pendingActionCount: Int,
    val completedActionCount: Int,
    val activePromiseCount: Int,
    val activePromisedAmount: BigDecimal,
    val nextFollowUpAt: Long? = null,
    val latestOutcome: CollectionOutcomeType? = null
)

/**
 * Reconciliation status summary for dashboard.
 */
data class CustomerReconciliationStatusSummary(
    val isReconciled: Boolean,
    val discrepancyCount: Int,
    val lastReconciledAt: Long? = null,
    val varianceAmount: BigDecimal = BigDecimal.ZERO
)

/**
 * Unified Customer Financial Dashboard aggregate projection (Module 14 Step 09).
 */
data class CustomerFinancialDashboard(
    val customerId: String,
    val tenantId: String,
    val projectId: String,
    val customerCode: String,
    val customerDisplayName: String,
    val accountNumber: String,
    val accountStatus: CustomerFinancialAccountStatus,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val outstandingReceivable: BigDecimal,
    val creditLimit: BigDecimal,
    val currentCreditExposure: BigDecimal,
    val availableCreditCapacity: BigDecimal,
    val paymentTerms: CustomerPaymentTermsType,
    val creditDays: Int,
    val requiresAdvance: Boolean,
    val riskStatus: CustomerCreditRiskStatus,
    val financialHold: Boolean,
    val holdReason: String?,
    val agingSummary: CustomerReceivableAgingSummary,
    val dueSchedule: CustomerDueScheduleSummary,
    val collectionStatus: CustomerCollectionStatusSummary,
    val reconciliationSummary: CustomerReconciliationStatusSummary,
    val warnings: List<CustomerFinancialWarning>,
    val recommendedActions: List<CustomerFinancialAction>,
    val recentActivity: List<CustomerFinancialActivityItem>,
    val generatedAt: Long = System.currentTimeMillis()
)
