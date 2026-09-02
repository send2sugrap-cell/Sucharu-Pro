package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.finance.VendorPayableAgingBucket
import com.sucharu.sucharupro.domain.service.finance.VendorPayableAgingCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class VendorPayableAgingTest {

    @Test
    fun `deterministic aging bucket classification relative to due date`() {
        val now = 1700000000000L

        // Due in future or today -> CURRENT
        assertEquals(
            VendorPayableAgingBucket.CURRENT,
            VendorPayableAgingCalculator.calculateAgingBucket(dueDate = now + TimeUnit.DAYS.toMillis(5), asOfTimestamp = now)
        )
        assertEquals(
            VendorPayableAgingBucket.CURRENT,
            VendorPayableAgingCalculator.calculateAgingBucket(dueDate = now, asOfTimestamp = now)
        )

        // 10 days overdue -> DAYS_1_TO_30
        assertEquals(
            VendorPayableAgingBucket.DAYS_1_TO_30,
            VendorPayableAgingCalculator.calculateAgingBucket(dueDate = now - TimeUnit.DAYS.toMillis(10), asOfTimestamp = now)
        )

        // 45 days overdue -> DAYS_31_TO_60
        assertEquals(
            VendorPayableAgingBucket.DAYS_31_TO_60,
            VendorPayableAgingCalculator.calculateAgingBucket(dueDate = now - TimeUnit.DAYS.toMillis(45), asOfTimestamp = now)
        )

        // 75 days overdue -> DAYS_61_TO_90
        assertEquals(
            VendorPayableAgingBucket.DAYS_61_TO_90,
            VendorPayableAgingCalculator.calculateAgingBucket(dueDate = now - TimeUnit.DAYS.toMillis(75), asOfTimestamp = now)
        )

        // 120 days overdue -> DAYS_OVER_90
        assertEquals(
            VendorPayableAgingBucket.DAYS_OVER_90,
            VendorPayableAgingCalculator.calculateAgingBucket(dueDate = now - TimeUnit.DAYS.toMillis(120), asOfTimestamp = now)
        )
    }
}
