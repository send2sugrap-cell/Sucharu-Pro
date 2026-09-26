package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.JobCostVarianceClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CostComponentVarianceServiceTest {

    private lateinit var service: CostComponentVarianceService

    @Before
    fun setUp() {
        service = CostComponentVarianceService()
    }

    @Test
    fun `buildJobComponentVarianceDetail_computesComponentVariancesAndLeakageSummary`() {
        val detail = service.buildJobComponentVarianceDetail("JOB-2026-001")

        assertNotNull(detail)
        assertEquals("JOB-2026-001", detail.jobId)
        assertEquals(BigDecimal("200.00"), detail.totalEstimatedCost)
        assertEquals(BigDecimal("220.00"), detail.totalActualCost)
        assertEquals(BigDecimal("20.00"), detail.netCostVariance)
        assertTrue(detail.isReconciled)

        // Verify Component Breakdown & Leakage
        assertEquals(4, detail.componentVariances.size)
        assertEquals("Raw Paper Material Price & Setup Labor Downtime", detail.leakageSummary.primaryLeakageSource)
        assertEquals(BigDecimal("10.00"), detail.leakageSummary.materialPriceQuantityLeakage)
        assertEquals(BigDecimal("5.00"), detail.leakageSummary.laborEfficiencyLeakage)

        val overEstComponents = service.filterComponentVariancesByClassification(detail, JobCostVarianceClassification.COST_OVER_ESTIMATE)
        assertEquals(3, overEstComponents.size)
    }
}
