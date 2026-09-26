package com.sucharu.sucharupro.data.api.model.finance

import kotlinx.serialization.Serializable

@Serializable
data class PaymentSettlementExceptionItemDto(
    val paymentId: String,
    val paymentNumber: String,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val paymentDate: String,
    val paymentAmount: String,
    val allocatedAmount: String,
    val remainingUnallocatedAmount: String,
    val paymentMethod: String = "BKASH",
    val operationCategory: String = "UNALLOCATED",
    val recommendedAction: String
)

@Serializable
data class ReconciliationExceptionItemDto(
    val accountId: String,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val invoiceReceivableTotal: String,
    val ledgerCalculatedBalance: String,
    val availableCreditBalance: String,
    val discrepancyAmount: String,
    val healthStatus: String = "RECONCILIATION_EXCEPTION",
    val recommendedAction: String = "Open Reconciliation"
)

@Serializable
data class SettlementOperationsSummaryDto(
    val totalUnallocatedPaymentsCount: Int,
    val totalUnallocatedAmount: String,
    val totalPartiallyAllocatedCount: Int,
    val totalAdvanceCreditAmount: String,
    val reconciliationExceptionCount: Int,
    val paymentExceptions: List<PaymentSettlementExceptionItemDto> = emptyList(),
    val reconciliationExceptions: List<ReconciliationExceptionItemDto> = emptyList(),
    val generatedAt: String
)
