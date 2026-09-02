package com.sucharu.sucharupro.domain.model.qc.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class QcAnalyticsEfficiencyScoreTest {

    @Test
    fun `perfect job with zero failures scores 100 percent`() {
        val score = QcJobAnalytics.calculateEfficiencyScore(
            plannedCost = 100.0,
            actualCost = 100.0,
            plannedMinutes = 30L,
            actualMinutes = 30L,
            defectCount = 0,
            reworkCount = 0,
            reQcCycleCount = 0
        )
        assertEquals(100.0, score, 0.001)
    }

    @Test
    fun `overrun and defects deterministically deduct from efficiency score`() {
        val score = QcJobAnalytics.calculateEfficiencyScore(
            plannedCost = 100.0,
            actualCost = 200.0, // 100% cost overrun -> 25.0 deduction
            plannedMinutes = 30L,
            actualMinutes = 60L,  // 100% time overrun -> 25.0 deduction
            defectCount = 1,      // 1 defect -> 10.0 deduction
            reworkCount = 1,      // 1 rework -> 7.5 deduction
            reQcCycleCount = 1     // 1 reQc -> 7.5 deduction
        )
        // 100 - 25 - 25 - 10 - 7.5 - 7.5 = 25.0
        assertEquals(25.0, score, 0.001)
    }

    @Test
    fun `severe failures clamp to zero and do not go negative`() {
        val score = QcJobAnalytics.calculateEfficiencyScore(
            plannedCost = 100.0,
            actualCost = 1000.0,
            plannedMinutes = 30L,
            actualMinutes = 300L,
            defectCount = 10,
            reworkCount = 10,
            reQcCycleCount = 10
        )
        assertEquals(0.0, score, 0.001)
    }
}
