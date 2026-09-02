package com.sucharu.sucharupro.domain.service.returns

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ReturnAnalyticsEvaluator] calculation algorithms (Module 11 Step 06 Chunk 02).
 */
class ReturnAnalyticsEvaluatorTest {

    private val projectId = "PRJ-EVAL-01"
    private val nowMillis = 1_700_000_000_000L

    @Test
    fun `test empty dataset evaluation returns zeroed summary`() {
        val summary = ReturnAnalyticsEvaluator.evaluateAnalytics(
            projectId = projectId,
            period = ReturnAnalyticsPeriod.THIS_MONTH,
            returns = emptyList(),
            settlements = emptyList(),
            items = emptyMap(),
            nowMillis = nowMillis
        )
        assertEquals(0, summary.totalReturns)
        assertEquals(0.0, summary.returnRate, 0.001)
        assertEquals(0, summary.openReturns)
        assertEquals(0, summary.processedReturns)
        assertEquals(0, summary.settledReturns)
        assertEquals(0, summary.totalRequestedQuantity)
        assertEquals(Money.ZERO, summary.totalSettledValue)
    }

    @Test
    fun `test return rate calculation with valid denominator`() {
        val returns = listOf(
            createTestReturn("R-01", ReturnStatus.PROCESSED, nowMillis - 1000),
            createTestReturn("R-02", ReturnStatus.REQUESTED, nowMillis - 2000)
        )
        val summary = ReturnAnalyticsEvaluator.evaluateAnalytics(
            projectId = projectId,
            period = ReturnAnalyticsPeriod.ALL_TIME,
            returns = returns,
            settlements = emptyList(),
            items = emptyMap(),
            totalDispatchedCount = 40,
            nowMillis = nowMillis
        )
        assertEquals(2, summary.totalReturns)
        // 2 / 40 * 100 = 5.0%
        assertEquals(5.0, summary.returnRate, 0.001)
    }

    @Test
    fun `test defect root cause breakdown aggregation`() {
        val returns = listOf(
            createTestReturn("R-01", ReturnStatus.PROCESSED, nowMillis, reason = ReturnReason.PRINTING_DEFECT),
            createTestReturn("R-02", ReturnStatus.PROCESSED, nowMillis, reason = ReturnReason.PRINTING_DEFECT),
            createTestReturn("R-03", ReturnStatus.PROCESSED, nowMillis, reason = ReturnReason.DAMAGED)
        )
        val itemsMap = mapOf(
            "R-01" to listOf(ReturnItem("RI-01", "R-01", "P-1", "CI-1", 100, 100, 0, "PCS")),
            "R-02" to listOf(ReturnItem("RI-02", "R-02", "P-1", "CI-2", 50, 50, 0, "PCS")),
            "R-03" to listOf(ReturnItem("RI-03", "R-03", "P-2", "CI-3", 20, 20, 0, "PCS"))
        )

        val breakdown = ReturnAnalyticsEvaluator.calculateDefectBreakdown(returns, itemsMap)
        val printing = breakdown.find { it.reason == ReturnReason.PRINTING_DEFECT }
        val damaged = breakdown.find { it.reason == ReturnReason.DAMAGED }

        assertNotNull(printing)
        assertEquals(2, printing!!.count)
        assertEquals(150, printing.quantity)
        assertEquals(66.67, printing.percentage, 0.01)

        assertNotNull(damaged)
        assertEquals(1, damaged!!.count)
        assertEquals(20, damaged.quantity)
        assertEquals(33.33, damaged.percentage, 0.01)
    }

    @Test
    fun `test financial breakdown aggregation across resolution types`() {
        val settlements = listOf(
            ReturnSettlement(
                settlementId = "S-01",
                returnId = "R-01",
                projectId = projectId,
                customerId = "C-01",
                resolutionType = ReturnResolutionType.CREDIT_NOTE,
                amount = Money(30000.0),
                status = ReturnSettlementStatus.COMPLETED,
                settledBy = "ADMIN",
                idempotencyKey = "K-01"
            ),
            ReturnSettlement(
                settlementId = "S-02",
                returnId = "R-02",
                projectId = projectId,
                customerId = "C-01",
                resolutionType = ReturnResolutionType.REFUND,
                amount = Money(20000.0),
                status = ReturnSettlementStatus.COMPLETED,
                settledBy = "ADMIN",
                idempotencyKey = "K-02"
            )
        )

        val breakdown = ReturnAnalyticsEvaluator.calculateFinancialBreakdown(settlements)
        val credit = breakdown.find { it.resolutionType == ReturnResolutionType.CREDIT_NOTE }
        val refund = breakdown.find { it.resolutionType == ReturnResolutionType.REFUND }

        assertNotNull(credit)
        assertEquals(1, credit!!.count)
        assertEquals(Money(30000.0), credit.totalAmount)
        assertEquals(60.0, credit.percentage, 0.01)

        assertNotNull(refund)
        assertEquals(1, refund!!.count)
        assertEquals(Money(20000.0), refund.totalAmount)
        assertEquals(40.0, refund.percentage, 0.01)
    }

    private fun createTestReturn(
        id: String,
        status: ReturnStatus,
        createdAt: Long,
        reason: ReturnReason = ReturnReason.PRINTING_DEFECT
    ) = ReturnRequest(
        returnId = id,
        projectId = projectId,
        returnNo = "RN-$id",
        customerId = "CUST-01",
        originalChallanId = "CHAL-01",
        status = status,
        reason = reason,
        requestedBy = "USER-01",
        createdAt = createdAt,
        updatedAt = createdAt
    )
}
