package com.sucharu.sucharupro.domain.model.finance

import java.math.BigDecimal

/**
 * BI-01-C Settlement Operation Exception Categories.
 */
enum class SettlementOperationCategory {
    UNALLOCATED,
    PARTIALLY_ALLOCATED,
    ADVANCE,
    FULLY_ALLOCATED
}

/**
 * Actionable Payment Settlement Exception Item for Operations.
 */
data class PaymentSettlementExceptionItem(
    val paymentId: String,
    val paymentNumber: String,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val paymentDate: String,
    val paymentAmount: BigDecimal,
    val allocatedAmount: BigDecimal,
    val remainingUnallocatedAmount: BigDecimal,
    val paymentMethod: String = "BKASH",
    val operationCategory: SettlementOperationCategory,
    val recommendedAction: String
)

/**
 * Actionable Reconciliation Exception Item for Management.
 */
data class ReconciliationExceptionItem(
    val accountId: String,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val invoiceReceivableTotal: BigDecimal,
    val ledgerCalculatedBalance: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val discrepancyAmount: BigDecimal,
    val healthStatus: ReconciliationHealthStatus = ReconciliationHealthStatus.RECONCILIATION_EXCEPTION,
    val recommendedAction: String = "Open Reconciliation"
)

/**
 * BI-01-C Master Settlement Operations & Reconciliation Summary.
 */
data class SettlementOperationsSummary(
    val totalUnallocatedPaymentsCount: Int,
    val totalUnallocatedAmount: BigDecimal,
    val totalPartiallyAllocatedCount: Int,
    val totalAdvanceCreditAmount: BigDecimal,
    val reconciliationExceptionCount: Int,
    val paymentExceptions: List<PaymentSettlementExceptionItem> = emptyList(),
    val reconciliationExceptions: List<ReconciliationExceptionItem> = emptyList(),
    val generatedAt: String
)
