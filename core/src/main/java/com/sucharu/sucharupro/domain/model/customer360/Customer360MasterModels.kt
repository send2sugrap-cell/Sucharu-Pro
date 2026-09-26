package com.sucharu.sucharupro.domain.model.customer360

import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.finance.CustomerFinancial360
import java.math.BigDecimal

/**
 * BI-04 Customer 360 Quotation Summary.
 */
data class Customer360QuotationSummary(
    val totalQuotationsCount: Int = 0,
    val acceptedQuotationsCount: Int = 0,
    val latestQuotationId: String? = null,
    val latestQuotationTotal: BigDecimal = BigDecimal.ZERO
)

/**
 * BI-04 Customer 360 Order & Commercial Baseline Summary.
 */
data class Customer360OrderSummary(
    val activeOrdersCount: Int = 0,
    val completedOrdersCount: Int = 0,
    val totalOrdersCount: Int = 0,
    val totalCommercialOrderValueSum: BigDecimal = BigDecimal.ZERO,
    val latestOrderId: String? = null,
    val latestCommercialLockState: String = "COMMERCIAL_LOCKED"
)

/**
 * BI-04 Customer 360 Production Jobs Summary.
 */
data class Customer360ProductionSummary(
    val activeJobsCount: Int = 0,
    val completedJobsCount: Int = 0,
    val currentProductionStage: String = "PRINTING"
)

/**
 * BI-04 Customer 360 Delivery & Fulfillment Summary.
 */
data class Customer360DeliverySummary(
    val deliveredOrdersCount: Int = 0,
    val pendingChallanCount: Int = 0,
    val latestChallanId: String? = null
)

/**
 * BI-04 Customer 360 Communication & Conversation Summary.
 */
data class Customer360CommunicationSummary(
    val recentMessagesCount: Int = 0,
    val lastContactedAt: String? = null,
    val lastCommunicationChannel: String = "SMS_NOTIFICATION"
)

/**
 * BI-04 Master Customer 360 Business View Read Model.
 */
data class Customer360MasterView(
    val customer: Customer,
    val financial360: CustomerFinancial360,
    val quotationSummary: Customer360QuotationSummary,
    val orderSummary: Customer360OrderSummary,
    val productionSummary: Customer360ProductionSummary,
    val deliverySummary: Customer360DeliverySummary,
    val communicationSummary: Customer360CommunicationSummary,
    val activityTimeline: List<CustomerActivity> = emptyList(),
    val generatedAt: String
)
