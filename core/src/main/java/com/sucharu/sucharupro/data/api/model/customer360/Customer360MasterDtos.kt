package com.sucharu.sucharupro.data.api.model.customer360

import kotlinx.serialization.Serializable

@Serializable
data class Customer360QuotationSummaryDto(
    val totalQuotationsCount: Int = 0,
    val acceptedQuotationsCount: Int = 0,
    val latestQuotationId: String? = null,
    val latestQuotationTotal: String = "0.00"
)

@Serializable
data class Customer360OrderSummaryDto(
    val activeOrdersCount: Int = 0,
    val completedOrdersCount: Int = 0,
    val totalOrdersCount: Int = 0,
    val totalCommercialOrderValueSum: String = "0.00",
    val latestOrderId: String? = null,
    val latestCommercialLockState: String = "COMMERCIAL_LOCKED"
)

@Serializable
data class Customer360ProductionSummaryDto(
    val activeJobsCount: Int = 0,
    val completedJobsCount: Int = 0,
    val currentProductionStage: String = "PRINTING"
)

@Serializable
data class Customer360DeliverySummaryDto(
    val deliveredOrdersCount: Int = 0,
    val pendingChallanCount: Int = 0,
    val latestChallanId: String? = null
)

@Serializable
data class Customer360CommunicationSummaryDto(
    val recentMessagesCount: Int = 0,
    val lastContactedAt: String? = null,
    val lastCommunicationChannel: String = "SMS_NOTIFICATION"
)

@Serializable
data class Customer360MasterViewDto(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val quotationSummary: Customer360QuotationSummaryDto,
    val orderSummary: Customer360OrderSummaryDto,
    val productionSummary: Customer360ProductionSummaryDto,
    val deliverySummary: Customer360DeliverySummaryDto,
    val communicationSummary: Customer360CommunicationSummaryDto,
    val generatedAt: String
)
