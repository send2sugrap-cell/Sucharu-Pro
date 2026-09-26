package com.sucharu.sucharupro.data.api.model.finance

import kotlinx.serialization.Serializable

@Serializable
data class CollectionAttentionItemDto(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val totalOutstanding: String,
    val overdueAmount: String,
    val overdueDays: Int,
    val attentionCategory: String,
    val lastPaymentDate: String? = null,
    val outstandingInvoiceCount: Int = 0
)

@Serializable
data class ReceivableAgingSummaryDto(
    val currentDue: String,
    val overdue1To30Days: String,
    val overdue31To60Days: String,
    val overdue61To90Days: String,
    val overdue90PlusDays: String,
    val totalOutstanding: String
)

@Serializable
data class FinancialControlCenterSummaryDto(
    val totalOutstanding: String,
    val currentDue: String,
    val overdueAmount: String,
    val dueTodayAmount: String,
    val dueSoonAmount: String,
    val totalCollectedYtd: String,
    val unallocatedPaymentsTotal: String,
    val reconciliationDiscrepancyCount: Int,
    val agingSummary: ReceivableAgingSummaryDto,
    val attentionItems: List<CollectionAttentionItemDto> = emptyList(),
    val calculatedAt: String
)
