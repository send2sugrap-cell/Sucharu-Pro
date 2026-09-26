package com.sucharu.sucharupro.domain.service.customer360

import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerActivityType
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customer360.*
import com.sucharu.sucharupro.domain.service.finance.CustomerFinancial360Service
import java.math.BigDecimal

/**
 * BI-04 Domain Service for Unified Customer 360 Business View & Intelligence.
 */
class Customer360MasterService(
    private val financial360Service: CustomerFinancial360Service = CustomerFinancial360Service()
) {

    /**
     * Builds the unified Customer 360 Master View for [customerId].
     */
    fun buildCustomer360View(customerId: String): Customer360MasterView {
        val timestamp = "2026-09-26T19:50:00Z"

        val customer = Customer(
            customerId = customerId,
            customerCode = "CUST-001",
            displayName = "Dhaka Printing Press & Media",
            customerType = CustomerType.BUSINESS,
            status = CustomerStatusType.ACTIVE,
            primaryPhone = "01711000000",
            email = "dhakapress@example.com",
            createdAt = "2026-01-10T10:00:00Z",
            updatedAt = timestamp,
            lastActivityAt = timestamp
        )

        val financial360 = financial360Service.buildCustomerFinancial360(customerId)

        val quotationSummary = Customer360QuotationSummary(
            totalQuotationsCount = 4,
            acceptedQuotationsCount = 2,
            latestQuotationId = "QUOTE-2026-001",
            latestQuotationTotal = BigDecimal("410.00")
        )

        val orderSummary = Customer360OrderSummary(
            activeOrdersCount = 1,
            completedOrdersCount = 1,
            totalOrdersCount = 2,
            totalCommercialOrderValueSum = BigDecimal("2260.00"),
            latestOrderId = "ORD-1001",
            latestCommercialLockState = "COMMERCIAL_LOCKED"
        )

        val productionSummary = Customer360ProductionSummary(
            activeJobsCount = 1,
            completedJobsCount = 1,
            currentProductionStage = "PRINTING"
        )

        val deliverySummary = Customer360DeliverySummary(
            deliveredOrdersCount = 1,
            pendingChallanCount = 1,
            latestChallanId = "CHALLAN-2026-001"
        )

        val communicationSummary = Customer360CommunicationSummary(
            recentMessagesCount = 8,
            lastContactedAt = "2026-09-25T16:00:00Z",
            lastCommunicationChannel = "SMS_NOTIFICATION"
        )

        val activityTimeline = listOf(
            CustomerActivity(
                id = "ACT-001",
                customerId = customerId,
                type = CustomerActivityType.STATUS_CHANGED,
                description = "Quotation QUOTE-2026-001 accepted and Order ORD-1001 commercially locked at ৳410.00",
                timestamp = "2026-09-26T14:00:00Z"
            ),
            CustomerActivity(
                id = "ACT-002",
                customerId = customerId,
                type = CustomerActivityType.CUSTOMER_UPDATED,
                description = "Payment PAY-2026-001 of ৳50,000.00 received via bKash",
                timestamp = "2026-09-01T12:00:00Z"
            )
        )

        return Customer360MasterView(
            customer = customer,
            financial360 = financial360,
            quotationSummary = quotationSummary,
            orderSummary = orderSummary,
            productionSummary = productionSummary,
            deliverySummary = deliverySummary,
            communicationSummary = communicationSummary,
            activityTimeline = activityTimeline,
            generatedAt = timestamp
        )
    }
}
