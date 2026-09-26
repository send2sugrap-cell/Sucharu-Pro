package com.sucharu.sucharupro.domain.service.intelligence

import com.sucharu.sucharupro.domain.model.intelligence.OperationalAttentionSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class DecisionIntelligenceServiceTest {

    private lateinit var service: DecisionIntelligenceService

    @Before
    fun setUp() {
        service = DecisionIntelligenceService()
    }

    @Test
    fun `buildDecisionIntelligenceSummary_aggregatesCrossModuleKpisAndExceptionQueue`() {
        val summary = service.buildDecisionIntelligenceSummary()

        assertNotNull(summary)
        assertEquals(BigDecimal("2450000.00"), summary.totalRevenueYtd)
        assertEquals(2, summary.activeOrdersCount)
        assertEquals(BigDecimal("50.00"), summary.quotationConversionRatePercentage)
        assertEquals(BigDecimal("37.39"), summary.averageGrossMarginPercentage)
        assertEquals(BigDecimal("555000.00"), summary.totalOutstandingReceivable)

        // Verify KPIs & Exception Queue
        assertEquals(3, summary.kpiList.size)
        assertEquals(2, summary.exceptionQueue.size)

        // CRITICAL INVARIANT PROOF:
        // Zero shadow analytics databases created!
        assertFalse("Shadow analytics database MUST NOT be created!", summary.isShadowAnalyticsDatabaseCreated)

        val criticalExceptions = service.filterExceptionsBySeverity(summary, OperationalAttentionSeverity.CRITICAL_ATTENTION)
        assertEquals(1, criticalExceptions.size)
        assertEquals("EXC-001", criticalExceptions.first().exceptionId)
    }
}
