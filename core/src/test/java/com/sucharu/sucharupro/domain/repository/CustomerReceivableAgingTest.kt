package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.service.finance.CustomerReceivableAgingCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class CustomerReceivableAgingTest {

    @Test
    fun `aging buckets are computed deterministically based on overdue days`() {
        val now = 1000000000000L
        val oneDayMs = TimeUnit.DAYS.toMillis(1)

        // 1. Not due yet -> CURRENT
        val futureDue = now + (5 * oneDayMs)
        assertEquals(ReceivableAgingBucket.CURRENT, CustomerReceivableAgingCalculator.calculateAgingBucket(futureDue, now))

        // 2. Exactly on due date -> CURRENT
        assertEquals(ReceivableAgingBucket.CURRENT, CustomerReceivableAgingCalculator.calculateAgingBucket(now, now))

        // 3. 15 days overdue -> DAYS_1_TO_30
        val due15DaysAgo = now - (15 * oneDayMs)
        assertEquals(ReceivableAgingBucket.DAYS_1_TO_30, CustomerReceivableAgingCalculator.calculateAgingBucket(due15DaysAgo, now))

        // 4. 45 days overdue -> DAYS_31_TO_60
        val due45DaysAgo = now - (45 * oneDayMs)
        assertEquals(ReceivableAgingBucket.DAYS_31_TO_60, CustomerReceivableAgingCalculator.calculateAgingBucket(due45DaysAgo, now))

        // 5. 75 days overdue -> DAYS_61_TO_90
        val due75DaysAgo = now - (75 * oneDayMs)
        assertEquals(ReceivableAgingBucket.DAYS_61_TO_90, CustomerReceivableAgingCalculator.calculateAgingBucket(due75DaysAgo, now))

        // 6. 120 days overdue -> DAYS_OVER_90
        val due120DaysAgo = now - (120 * oneDayMs)
        assertEquals(ReceivableAgingBucket.DAYS_OVER_90, CustomerReceivableAgingCalculator.calculateAgingBucket(due120DaysAgo, now))
    }
}
