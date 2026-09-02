package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProfitabilityIntelligenceReconciliationTest {

    private val reconService = ProfitabilityIntelligenceReconciliationServiceImpl()

    @Test
    fun testReconciliationBalanced() = runBlocking {
        val snapshot = ProfitabilityIntelligenceSnapshot(
            snapshotId = "snap-001",
            tenantId = "TEN-001",
            projectId = "PROJ-001",
            analysisPeriodId = "2026-M09",
            revenue = BigDecimal("100000.0000"),
            totalCost = BigDecimal("70000.0000"),
            grossProfit = BigDecimal("30000.0000"),
            grossMargin = BigDecimal("30.0000"),
            dimensionInsights = listOf(
                DimensionInsight(
                    insightId = "dim-1",
                    snapshotId = "snap-001",
                    tenantId = "TEN-001",
                    periodId = "2026-M09",
                    dimensionType = ProfitabilityDimensionType.CUSTOMER,
                    dimensionId = "CUST-001",
                    dimensionLabel = "Customer 1",
                    revenue = BigDecimal("100000.0000"),
                    cost = BigDecimal("70000.0000"),
                    grossProfit = BigDecimal("30000.0000")
                )
            )
        )

        val result = reconService.reconcile("TEN-001", "PROJ-001", "2026-M09", snapshot)
        assertTrue(result is DomainResult.Success)
        val event = (result as DomainResult.Success).data
        assertTrue(event.isBalanced)
        assertEquals(0, event.errorDetails.size)
    }
}
