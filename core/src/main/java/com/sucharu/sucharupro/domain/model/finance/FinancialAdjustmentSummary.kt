package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

data class FinancialAdjustmentSummary(
    val projectId: String,
    val totalAdjustmentsCount: Int,
    val totalCreditNotesCount: Int,
    val totalDebitNotesCount: Int,
    val totalRefundsCount: Int,
    val totalPostedAdjustmentAmount: Money,
    val totalPendingAdjustmentAmount: Money,
    val totalCancelledAdjustmentAmount: Money,
    val totalCreditNotesAmount: Money,
    val totalDebitNotesAmount: Money,
    val totalRefundsAmount: Money,
    val customerTotalCredit: Money = Money.ZERO,
    val customerTotalRefund: Money = Money.ZERO,
    val vendorTotalDebit: Money = Money.ZERO
)
