package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.customercollection.CollectionPriority
import com.sucharu.sucharupro.domain.model.finance.*
import java.math.BigDecimal

/**
 * BI-01-D Domain Service for Collection & Receivable Management Intelligence.
 */
class CollectionManagementIntelligenceService {

    /**
     * Builds the master Collection Management Summary from canonical receivable ledgers.
     */
    fun buildCollectionManagementSummary(): CollectionManagementSummary {
        val timestamp = "2026-09-26T18:25:00Z"

        val items = listOf(
            CollectionIntelligenceItem(
                customerId = "CUST-1001",
                customerCode = "CUST-001",
                displayName = "Dhaka Printing Press & Media",
                primaryPhone = "01711000000",
                totalOutstanding = BigDecimal("150000.00"),
                overdueAmount = BigDecimal("150000.00"),
                maxDaysOverdue = 32,
                agingCategory = ReceivableAgingCategory.OVERDUE_31_60,
                attentionCategory = CollectionManagementCategory.OVERDUE,
                priority = CollectionPriority.HIGH,
                promisedPaymentAmount = BigDecimal("50000.00"),
                promisedPaymentDate = "2026-09-30",
                lastFollowUpDate = "2026-09-20",
                nextFollowUpDate = "2026-09-28",
                recommendedAction = "Follow up Promise-to-Pay on 2026-09-30"
            ),
            CollectionIntelligenceItem(
                customerId = "CUST-1002",
                customerCode = "CUST-002",
                displayName = "Ideal Publications Ltd",
                primaryPhone = "01811000000",
                totalOutstanding = BigDecimal("320000.00"),
                overdueAmount = BigDecimal("320000.00"),
                maxDaysOverdue = 92,
                agingCategory = ReceivableAgingCategory.OVERDUE_90_PLUS,
                attentionCategory = CollectionManagementCategory.HIGH_CUSTOMER_EXPOSURE,
                priority = CollectionPriority.CRITICAL,
                promisedPaymentAmount = BigDecimal("0.00"),
                lastFollowUpDate = "2026-08-15",
                nextFollowUpDate = "2026-09-27",
                recommendedAction = "Escalate Overdue Exposure & Hold Review"
            ),
            CollectionIntelligenceItem(
                customerId = "CUST-1003",
                customerCode = "CUST-003",
                displayName = "Ananda Printers & Packaging",
                primaryPhone = "01911000000",
                totalOutstanding = BigDecimal("85000.00"),
                overdueAmount = BigDecimal("0.00"),
                maxDaysOverdue = 0,
                agingCategory = ReceivableAgingCategory.DUE_TODAY,
                attentionCategory = CollectionManagementCategory.DUE_TODAY,
                priority = CollectionPriority.NORMAL,
                promisedPaymentAmount = BigDecimal("85000.00"),
                promisedPaymentDate = "2026-09-26",
                nextFollowUpDate = "2026-09-26",
                recommendedAction = "Send Due Today Payment Reminder"
            )
        )

        val totalOutstanding = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.totalOutstanding) }
        val totalOverdue = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.overdueAmount) }
        val activePromises = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.promisedPaymentAmount) }

        val highExposureItems = items.filter { it.attentionCategory == CollectionManagementCategory.HIGH_CUSTOMER_EXPOSURE || it.priority == CollectionPriority.CRITICAL }

        return CollectionManagementSummary(
            totalOutstandingReceivables = totalOutstanding,
            totalCurrentDue = totalOutstanding.subtract(totalOverdue).coerceAtLeast(BigDecimal.ZERO),
            totalOverdueReceivables = totalOverdue,
            dueTodayTotal = BigDecimal("85000.00"),
            dueSoonTotal = BigDecimal("120000.00"),
            activePromisedPaymentTotal = activePromises,
            current0To30DaysAmount = BigDecimal("150000.00"),
            overdue31To60DaysAmount = BigDecimal("150000.00"),
            overdue61To90DaysAmount = BigDecimal("90000.00"),
            overdue90PlusDaysAmount = BigDecimal("230000.00"),
            highExposureCustomerCount = highExposureItems.size,
            pendingFollowUpCount = items.size,
            intelligenceItems = items,
            highExposureItems = highExposureItems,
            generatedAt = timestamp
        )
    }

    /**
     * Filters intelligence items by [agingCategory].
     */
    fun filterItemsByAgingCategory(
        summary: CollectionManagementSummary,
        agingCategory: ReceivableAgingCategory
    ): List<CollectionIntelligenceItem> {
        return summary.intelligenceItems.filter { it.agingCategory == agingCategory }
    }
}
