package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProfitabilityDriverEngineTest {

    private val driverEngine = ProfitabilityDriverEngineImpl()

    @Test
    fun testDriverDetection() {
        val dims = listOf(
            DimensionInsight(
                insightId = "dim-1",
                snapshotId = "snap-1",
                tenantId = "TEN-001",
                periodId = "2026-M09",
                dimensionType = ProfitabilityDimensionType.CUSTOMER,
                dimensionId = "CUST-001",
                dimensionLabel = "Apex Corp",
                revenue = BigDecimal("100000.0000"),
                cost = BigDecimal("60000.0000"),
                grossProfit = BigDecimal("40000.0000"),
                margin = BigDecimal("40.0000")
            ),
            DimensionInsight(
                insightId = "dim-2",
                snapshotId = "snap-1",
                tenantId = "TEN-001",
                periodId = "2026-M09",
                dimensionType = ProfitabilityDimensionType.PRODUCT,
                dimensionId = "PROD-002",
                dimensionLabel = "Loss Box",
                revenue = BigDecimal("20000.0000"),
                cost = BigDecimal("25000.0000"),
                grossProfit = BigDecimal("-5000.0000"),
                margin = BigDecimal("-25.0000")
            )
        )

        val drivers = driverEngine.evaluateDrivers(
            tenantId = "TEN-001",
            periodId = "2026-M09",
            totalRevenue = BigDecimal("120000.0000"),
            totalCost = BigDecimal("85000.0000"),
            dimensions = dims,
            relationships = emptyList()
        )

        assertTrue(drivers.isNotEmpty())
        val pos = drivers.filter { it.driverType == ProfitabilityDriverType.POSITIVE_DRIVER }
        val neg = drivers.filter { it.driverType == ProfitabilityDriverType.NEGATIVE_DRIVER }

        assertEquals(1, pos.size)
        assertEquals("CUST-001", pos.first().entityId)
        assertEquals(1, neg.size)
        assertEquals("PROD-002", neg.first().entityId)
    }
}
