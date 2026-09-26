package com.sucharu.sucharupro.domain.model.finance

import java.math.BigDecimal

/**
 * BI-01 Collection Attention Classifications.
 */
enum class CollectionAttentionCategory {
    DUE_TODAY,
    DUE_SOON,
    OVERDUE,
    LONG_OVERDUE,
    HIGH_OUTSTANDING,
    PAYMENT_PENDING_ALLOCATION,
    RECONCILIATION_EXCEPTION
}

/**
 * Actionable Collection Attention Item for Management & Collection Officers.
 */
data class CollectionAttentionItem(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val totalOutstanding: BigDecimal,
    val overdueAmount: BigDecimal,
    val overdueDays: Int,
    val attentionCategory: CollectionAttentionCategory,
    val lastPaymentDate: String? = null,
    val outstandingInvoiceCount: Int = 0
)

/**
 * Aging Bucket Distribution for Receivables.
 */
data class ReceivableAgingSummary(
    val currentDue: BigDecimal = BigDecimal.ZERO,
    val overdue1To30Days: BigDecimal = BigDecimal.ZERO,
    val overdue31To60Days: BigDecimal = BigDecimal.ZERO,
    val overdue61To90Days: BigDecimal = BigDecimal.ZERO,
    val overdue90PlusDays: BigDecimal = BigDecimal.ZERO,
    val totalOutstanding: BigDecimal = BigDecimal.ZERO
)

/**
 * BI-01 Financial Control Center Master Summary Model.
 */
data class FinancialControlCenterSummary(
    val totalOutstanding: BigDecimal,
    val currentDue: BigDecimal,
    val overdueAmount: BigDecimal,
    val dueTodayAmount: BigDecimal,
    val dueSoonAmount: BigDecimal,
    val totalCollectedYtd: BigDecimal,
    val unallocatedPaymentsTotal: BigDecimal,
    val reconciliationDiscrepancyCount: Int,
    val agingSummary: ReceivableAgingSummary,
    val attentionItems: List<CollectionAttentionItem> = emptyList(),
    val calculatedAt: String
)
