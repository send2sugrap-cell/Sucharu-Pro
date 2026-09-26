package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.JobProfitabilityHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class JobProfitabilityManagementServiceTest {

    private lateinit var service: JobProfitabilityManagementService

    @Before
    fun setUp() {
        service = JobProfitabilityManagementService()
    }

    @Test
    fun `buildJobProfitabilityManagementSummary_computesTotalsMarginsAndEstimateAccuracy`() {
        val summary = service.buildJobProfitabilityManagementSummary()

        assertNotNull(summary)
        assertEquals(2, summary.totalJobsEvaluated)
        assertEquals(2, summary.profitableJobsCount)
        assertEquals(0, summary.lossJobsCount)

        assertEquals(BigDecimal("2260.00"), summary.totalSellingRevenueSum)
        assertEquals(BigDecimal("1450.00"), summary.totalEstimatedCostSum)
        assertEquals(BigDecimal("1415.00"), summary.totalActualCostSum)
        assertEquals(BigDecimal("845.00"), summary.totalGrossProfitSum)
        assertEquals(BigDecimal("37.39"), summary.averageGrossMarginPercentage)
        assertEquals(BigDecimal("95.80"), summary.averageEstimateAccuracyPercentage)

        val profitableJobs = service.filterJobsByHealthStatus(summary, JobProfitabilityHealthStatus.PROFITABLE)
        assertEquals(2, profitableJobs.size)
        assertTrue(profitableJobs.first().isFullyReconciled)
    }
}
