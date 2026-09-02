package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableAgingBucket
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import java.util.concurrent.TimeUnit

/**
 * Deterministic aging and overdue evaluator for supplier/vendor payables (Module 09 Step 04).
 */
object VendorPayableAgingCalculator {

    /**
     * Calculates the aging bucket for a payable relative to a given evaluation timestamp.
     */
    fun calculateAgingBucket(
        dueDate: Long,
        asOfTimestamp: Long = System.currentTimeMillis()
    ): VendorPayableAgingBucket {
        if (asOfTimestamp <= dueDate) {
            return VendorPayableAgingBucket.CURRENT
        }

        val overdueDays = TimeUnit.MILLISECONDS.toDays(asOfTimestamp - dueDate)
        return when {
            overdueDays <= 30 -> VendorPayableAgingBucket.DAYS_1_TO_30
            overdueDays <= 60 -> VendorPayableAgingBucket.DAYS_31_TO_60
            overdueDays <= 90 -> VendorPayableAgingBucket.DAYS_61_TO_90
            else -> VendorPayableAgingBucket.DAYS_OVER_90
        }
    }

    /**
     * Determines whether an approved/partially-settled payable is overdue.
     */
    fun evaluateEffectiveStatus(
        payable: VendorPayable,
        asOfTimestamp: Long = System.currentTimeMillis()
    ): VendorPayableStatus {
        if (payable.status == VendorPayableStatus.SETTLED ||
            payable.status == VendorPayableStatus.CANCELLED ||
            payable.status == VendorPayableStatus.DRAFT ||
            payable.status == VendorPayableStatus.PENDING
        ) {
            return payable.status
        }

        if (payable.outstandingAmount.isZero()) {
            return VendorPayableStatus.SETTLED
        }

        return if (asOfTimestamp > payable.dueDate) {
            VendorPayableStatus.OVERDUE
        } else if (payable.settledAmount.isPositive()) {
            VendorPayableStatus.PARTIALLY_SETTLED
        } else {
            VendorPayableStatus.APPROVED
        }
    }
}
