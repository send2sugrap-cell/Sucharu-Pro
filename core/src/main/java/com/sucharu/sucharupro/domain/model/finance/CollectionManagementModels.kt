package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.customercollection.CollectionPriority
import java.math.BigDecimal

/**
 * BI-01-D Receivable Aging Categories.
 */
enum class ReceivableAgingCategory {
    CURRENT,
    DUE_TODAY,
    OVERDUE_1_30,
    OVERDUE_31_60,
    OVERDUE_61_90,
    OVERDUE_90_PLUS
}

/**
 * BI-01-D Operational Collection Attention Categories.
 */
enum class CollectionManagementCategory {
    DUE_TODAY,
    DUE_SOON,
    OVERDUE,
    LONG_OVERDUE,
    HIGH_OUTSTANDING,
    HIGH_CUSTOMER_EXPOSURE,
    PAYMENT_EXPECTED,
    FOLLOW_UP_REQUIRED
}

/**
 * Actionable Collection Intelligence Item.
 */
data class CollectionIntelligenceItem(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val totalOutstanding: BigDecimal,
    val overdueAmount: BigDecimal,
    val maxDaysOverdue: Int,
    val agingCategory: ReceivableAgingCategory,
    val attentionCategory: CollectionManagementCategory,
    val priority: CollectionPriority = CollectionPriority.NORMAL,
    val promisedPaymentAmount: BigDecimal = BigDecimal.ZERO,
    val promisedPaymentDate: String? = null,
    val lastFollowUpDate: String? = null,
    val nextFollowUpDate: String? = null,
    val recommendedAction: String = "Follow up payment"
)

/**
 * BI-01-D Collection & Receivable Management Master Read Model.
 */
data class CollectionManagementSummary(
    val totalOutstandingReceivables: BigDecimal,
    val totalCurrentDue: BigDecimal,
    val totalOverdueReceivables: BigDecimal,
    val dueTodayTotal: BigDecimal,
    val dueSoonTotal: BigDecimal,
    val activePromisedPaymentTotal: BigDecimal,

    val current0To30DaysAmount: BigDecimal,
    val overdue31To60DaysAmount: BigDecimal,
    val overdue61To90DaysAmount: BigDecimal,
    val overdue90PlusDaysAmount: BigDecimal,

    val highExposureCustomerCount: Int = 0,
    val pendingFollowUpCount: Int = 0,

    val intelligenceItems: List<CollectionIntelligenceItem> = emptyList(),
    val highExposureItems: List<CollectionIntelligenceItem> = emptyList(),

    val generatedAt: String
)
