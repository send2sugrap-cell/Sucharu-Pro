package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProfitabilityLeakageEngineTest {

    private val leakageEngine = ProfitabilityLeakageEngineImpl()

    @Test
    fun testDetectCustomerAndProductLeakages() {
        val dims = listOf(
            DimensionInsight(
                insightId = "dim-1",
                snapshotId = "snap-1",
                tenantId = "TEN-001",
                periodId = "2026-M09",
                dimensionType = ProfitabilityDimensionType.CUSTOMER,
                dimensionId = "CUST-LOSS",
                dimensionLabel = "Loss Customer",
                revenue = BigDecimal("10000.0000"),
                cost = BigDecimal("18000.0000"),
                grossProfit = BigDecimal("-8000.0000"),
                margin = BigDecimal("-80.0000")
            ),
            DimensionInsight(
                insightId = "dim-2",
                snapshotId = "snap-1",
                tenantId = "TEN-001",
                periodId = "2026-M09",
                dimensionType = ProfitabilityDimensionType.PRODUCT,
                dimensionId = "PROD-LOSS",
                dimensionLabel = "Loss Product",
                revenue = BigDecimal("5000.0000"),
                cost = BigDecimal("9000.0000"),
                grossProfit = BigDecimal("-4000.0000"),
                margin = BigDecimal("-80.0000")
            )
        )

        val leakages = leakageEngine.detectLeakages(
            tenantId = "TEN-001",
            periodId = "2026-M09",
            totalRevenue = BigDecimal("15000.0000"),
            totalCost = BigDecimal("27000.0000"),
            dimensions = dims,
            relationships = emptyList()
        )

        assertEquals(2, leakages.size)
        assertTrue(leakages.any { it.category == ProfitLeakageCategory.LOW_MARGIN_CUSTOMER && it.entityId == "CUST-LOSS" })
        assertTrue(leakages.any { it.category == ProfitLeakageCategory.LOW_MARGIN_PRODUCT && it.entityId == "PROD-LOSS" })
    }
}
