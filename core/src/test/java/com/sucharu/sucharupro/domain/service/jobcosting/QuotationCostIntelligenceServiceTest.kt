package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.CostIntelligenceConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class QuotationCostIntelligenceServiceTest {

    private lateinit var service: QuotationCostIntelligenceService

    @Before
    fun setUp() {
        service = QuotationCostIntelligenceService()
    }

    @Test
    fun `evaluateQuotationCostIntelligence_generatesHistoricalBenchmarkAndPreservesPriceImmutability`() {
        val intelligence = service.evaluateQuotationCostIntelligence("QUOTE-2026-001")

        assertNotNull(intelligence)
        assertEquals("QUOTE-2026-001", intelligence.quotationId)
        assertEquals(CostIntelligenceConfidence.SUFFICIENT_DATA, intelligence.confidenceState)

        // Verify Historical Benchmark
        val benchmark = intelligence.historicalBenchmark
        assertNotNull(benchmark)
        assertEquals(5, benchmark!!.comparableJobsCount)
        assertEquals(BigDecimal("0.2650"), benchmark.avgActualUnitCost)
        assertEquals(BigDecimal("6.00"), benchmark.avgCostVariancePercentage)

        // CRITICAL INVARIANT PROOF:
        // Evaluation MUST NEVER automatically mutate quotation or order selling prices!
        assertFalse("Cost intelligence evaluation MUST NOT apply automatic price mutation!", intelligence.isAutomaticPriceMutationApplied)
        assertEquals(BigDecimal("410.00"), intelligence.currentQuotationSellingPrice)
    }
}
