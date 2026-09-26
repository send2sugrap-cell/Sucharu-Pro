package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.finance.*
import java.math.BigDecimal

/**
 * BI-01-C Domain Service for Payment Allocation, Settlement Operations & Reconciliation Resolution.
 */
class SettlementOperationsService {

    /**
     * Builds the operational Settlement Operations & Reconciliation Summary.
     */
    fun buildSettlementOperationsSummary(): SettlementOperationsSummary {
        val timestamp = "2026-09-26T18:15:00Z"

        val paymentExceptions = listOf(
            PaymentSettlementExceptionItem(
                paymentId = "PAY-2026-002",
                paymentNumber = "PAY-002",
                customerId = "CUST-1001",
                customerCode = "CUST-001",
                customerName = "Dhaka Printing Press & Media",
                paymentDate = "2026-09-01",
                paymentAmount = BigDecimal("25000.00"),
                allocatedAmount = BigDecimal("0.00"),
                remainingUnallocatedAmount = BigDecimal("25000.00"),
                paymentMethod = "BANK_TRANSFER",
                operationCategory = SettlementOperationCategory.UNALLOCATED,
                recommendedAction = "Review Allocation"
            ),
            PaymentSettlementExceptionItem(
                paymentId = "PAY-2026-003",
                paymentNumber = "PAY-003",
                customerId = "CUST-1002",
                customerCode = "CUST-002",
                customerName = "Ideal Publications Ltd",
                paymentDate = "2026-09-05",
                paymentAmount = BigDecimal("100000.00"),
                allocatedAmount = BigDecimal("60000.00"),
                remainingUnallocatedAmount = BigDecimal("40000.00"),
                paymentMethod = "BKASH",
                operationCategory = SettlementOperationCategory.PARTIALLY_ALLOCATED,
                recommendedAction = "Review Remaining Allocation"
            )
        )

        val reconciliationExceptions = listOf(
            ReconciliationExceptionItem(
                accountId = "ACC-CUST-002",
                customerId = "CUST-1002",
                customerCode = "CUST-002",
                customerName = "Ideal Publications Ltd",
                invoiceReceivableTotal = BigDecimal("320000.00"),
                ledgerCalculatedBalance = BigDecimal("325000.00"),
                availableCreditBalance = BigDecimal("0.00"),
                discrepancyAmount = BigDecimal("5000.00"),
                healthStatus = ReconciliationHealthStatus.RECONCILIATION_EXCEPTION,
                recommendedAction = "Open Reconciliation"
            )
        )

        val unallocatedTotal = paymentExceptions.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.remainingUnallocatedAmount) }

        return SettlementOperationsSummary(
            totalUnallocatedPaymentsCount = 1,
            totalUnallocatedAmount = unallocatedTotal,
            totalPartiallyAllocatedCount = 1,
            totalAdvanceCreditAmount = BigDecimal("25000.00"),
            reconciliationExceptionCount = reconciliationExceptions.size,
            paymentExceptions = paymentExceptions,
            reconciliationExceptions = reconciliationExceptions,
            generatedAt = timestamp
        )
    }

    /**
     * Filters payment exception items by operational category.
     */
    fun filterExceptionsByCategory(
        summary: SettlementOperationsSummary,
        category: SettlementOperationCategory
    ): List<PaymentSettlementExceptionItem> {
        return summary.paymentExceptions.filter { it.operationCategory == category }
    }
}
