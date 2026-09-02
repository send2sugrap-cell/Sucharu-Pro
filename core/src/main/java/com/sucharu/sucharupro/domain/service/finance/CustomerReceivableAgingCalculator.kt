package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket
import java.util.concurrent.TimeUnit

/**
 * Deterministic aging evaluator for customer receivables (Module 09 Step 02).
 */
object CustomerReceivableAgingCalculator {

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    /**
     * Computes the aging bucket for a receivable given an evaluation timestamp.
     */
    fun calculateAgingBucket(
        dueDate: Long,
        asOfTimestamp: Long = System.currentTimeMillis()
    ): ReceivableAgingBucket {
        if (asOfTimestamp <= dueDate) {
            return ReceivableAgingBucket.CURRENT
        }

        val overdueDays = TimeUnit.MILLISECONDS.toDays(asOfTimestamp - dueDate)
        return when {
            overdueDays <= 30 -> ReceivableAgingBucket.DAYS_1_TO_30
            overdueDays <= 60 -> ReceivableAgingBucket.DAYS_31_TO_60
            overdueDays <= 90 -> ReceivableAgingBucket.DAYS_61_TO_90
            else -> ReceivableAgingBucket.DAYS_OVER_90
        }
    }

    /**
     * Determines whether an open/partially-settled receivable has become overdue.
     */
    fun evaluateEffectiveStatus(
        receivable: CustomerReceivable,
        asOfTimestamp: Long = System.currentTimeMillis()
    ): CustomerReceivableStatus {
        if (receivable.status == CustomerReceivableStatus.SETTLED ||
            receivable.status == CustomerReceivableStatus.CANCELLED
        ) {
            return receivable.status
        }

        if (receivable.outstandingAmount.isZero()) {
            return CustomerReceivableStatus.SETTLED
        }

        return if (asOfTimestamp > receivable.dueDate) {
            CustomerReceivableStatus.OVERDUE
        } else if (receivable.settledAmount.isPositive()) {
            CustomerReceivableStatus.PARTIALLY_SETTLED
        } else {
            CustomerReceivableStatus.OPEN
        }
    }
}
