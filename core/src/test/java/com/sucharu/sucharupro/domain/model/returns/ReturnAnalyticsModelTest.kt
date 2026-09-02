package com.sucharu.sucharupro.domain.model.returns

import com.sucharu.sucharupro.domain.model.common.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain model validation unit tests for Return Analytics (Module 11 Step 06 Chunk 01).
 */
class ReturnAnalyticsModelTest {

    @Test
    fun `test valid ReturnAnalyticsSummary construction`() {
        val summary = ReturnAnalyticsSummary(
            projectId = "PRJ-01",
            period = ReturnAnalyticsPeriod.THIS_MONTH,
            totalReturns = 15,
            returnRate = 3.5,
            openReturns = 4,
            processedReturns = 6,
            settledReturns = 5,
            totalRequestedQuantity = 500,
            totalAcceptedQuantity = 400,
            totalRejectedQuantity = 100,
            totalSettledValue = Money(45000.0),
            averageTurnaroundDays = 2.4
        )
        assertEquals("PRJ-01", summary.projectId)
        assertEquals(ReturnAnalyticsPeriod.THIS_MONTH, summary.period)
        assertEquals(15, summary.totalReturns)
        assertEquals(3.5, summary.returnRate, 0.001)
        assertEquals(500, summary.totalRequestedQuantity)
        assertEquals(Money(45000.0), summary.totalSettledValue)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test ReturnAnalyticsSummary blank project ID throws`() {
        ReturnAnalyticsSummary(
            projectId = "   ",
            period = ReturnAnalyticsPeriod.TODAY,
            totalReturns = 0,
            returnRate = 0.0,
            openReturns = 0,
            processedReturns = 0,
            settledReturns = 0,
            totalRequestedQuantity = 0,
            totalAcceptedQuantity = 0,
            totalRejectedQuantity = 0,
            totalSettledValue = Money.ZERO,
            averageTurnaroundDays = 0.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test ReturnAnalyticsSummary negative counts throw`() {
        ReturnAnalyticsSummary(
            projectId = "PRJ-01",
            period = ReturnAnalyticsPeriod.TODAY,
            totalReturns = -1,
            returnRate = 0.0,
            openReturns = 0,
            processedReturns = 0,
            settledReturns = 0,
            totalRequestedQuantity = 0,
            totalAcceptedQuantity = 0,
            totalRejectedQuantity = 0,
            totalSettledValue = Money.ZERO,
            averageTurnaroundDays = 0.0
        )
    }

    @Test
    fun `test valid ReturnDefectBreakdown construction`() {
        val breakdown = ReturnDefectBreakdown(
            reason = ReturnReason.PRINTING_DEFECT,
            count = 10,
            quantity = 250,
            percentage = 62.5
        )
        assertEquals(ReturnReason.PRINTING_DEFECT, breakdown.reason)
        assertEquals(10, breakdown.count)
        assertEquals(250, breakdown.quantity)
        assertEquals(62.5, breakdown.percentage, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test ReturnDefectBreakdown invalid percentage throws`() {
        ReturnDefectBreakdown(
            reason = ReturnReason.PRINTING_DEFECT,
            count = 10,
            quantity = 250,
            percentage = 105.0
        )
    }

    @Test
    fun `test valid ReturnFinancialBreakdown construction`() {
        val breakdown = ReturnFinancialBreakdown(
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            count = 8,
            totalAmount = Money(25000.0),
            percentage = 55.5
        )
        assertEquals(ReturnResolutionType.CREDIT_NOTE, breakdown.resolutionType)
        assertEquals(8, breakdown.count)
        assertEquals(Money(25000.0), breakdown.totalAmount)
        assertEquals(55.5, breakdown.percentage, 0.001)
    }

    @Test
    fun `test ReturnAnalyticsPeriod calculateStartTimestamp`() {
        val now = 1_700_000_000_000L
        val oneDay = 86_400_000L

        assertEquals(now - oneDay, ReturnAnalyticsPeriod.TODAY.calculateStartTimestamp(now))
        assertEquals(now - (7 * oneDay), ReturnAnalyticsPeriod.THIS_WEEK.calculateStartTimestamp(now))
        assertEquals(now - (30 * oneDay), ReturnAnalyticsPeriod.THIS_MONTH.calculateStartTimestamp(now))
        assertEquals(now - (90 * oneDay), ReturnAnalyticsPeriod.THIS_QUARTER.calculateStartTimestamp(now))
        assertEquals(0L, ReturnAnalyticsPeriod.ALL_TIME.calculateStartTimestamp(now))
    }

    @Test
    fun `test valid ReturnAnalyticsTrendPoint construction`() {
        val point = ReturnAnalyticsTrendPoint(
            timestamp = 1000L,
            periodLabel = "Day 1",
            returnCount = 3,
            acceptedQuantity = 100,
            rejectedQuantity = 20,
            financialValue = Money(1500.0)
        )
        assertEquals(1000L, point.timestamp)
        assertEquals(3, point.returnCount)
        assertEquals(Money(1500.0), point.financialValue)
    }
}
