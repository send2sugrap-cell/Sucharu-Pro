package com.sucharu.sucharupro.data.api.model.finance

import kotlinx.serialization.Serializable

@Serializable
data class CollectionIntelligenceItemDto(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val totalOutstanding: String,
    val overdueAmount: String,
    val maxDaysOverdue: Int,
    val agingCategory: String,
    val attentionCategory: String,
    val priority: String = "NORMAL",
    val promisedPaymentAmount: String = "0.00",
    val promisedPaymentDate: String? = null,
    val lastFollowUpDate: String? = null,
    val nextFollowUpDate: String? = null,
    val recommendedAction: String
)

@Serializable
data class CollectionManagementSummaryDto(
    val totalOutstandingReceivables: String,
    val totalCurrentDue: String,
    val totalOverdueReceivables: String,
    val dueTodayTotal: String,
    val dueSoonTotal: String,
    val activePromisedPaymentTotal: String,

    val current0To30DaysAmount: String,
    val overdue31To60DaysAmount: String,
    val overdue61To90DaysAmount: String,
    val overdue90PlusDaysAmount: String,

    val highExposureCustomerCount: Int = 0,
    val pendingFollowUpCount: Int = 0,

    val intelligenceItems: List<CollectionIntelligenceItemDto> = emptyList(),
    val highExposureItems: List<CollectionIntelligenceItemDto> = emptyList(),

    val generatedAt: String
)
