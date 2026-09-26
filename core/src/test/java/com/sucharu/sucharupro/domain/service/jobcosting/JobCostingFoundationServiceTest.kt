package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.JobCostVarianceClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class JobCostingFoundationServiceTest {

    private lateinit var service: JobCostingFoundationService

    @Before
    fun setUp() {
        service = JobCostingFoundationService()
    }

    @Test
    fun `buildJobCostIntelligenceSummary_computesTotalsVarianceAndMarginsCorrectly`() {
        val summary = service.buildJobCostIntelligenceSummary()

        assertNotNull(summary)
        assertEquals(2, summary.totalJobsCount)
        assertEquals(1, summary.overEstimateJobsCount)
        assertEquals(1, summary.underEstimateJobsCount)

        assertEquals(BigDecimal("1450.00"), summary.totalEstimatedCostSum)
        assertEquals(BigDecimal("1415.00"), summary.totalActualCostSum)
        assertEquals(BigDecimal("-35.00"), summary.netCostVarianceSum)
        assertEquals(BigDecimal("2260.00"), summary.totalSellingPriceSnapshotSum)

        val overEstJobs = service.filterJobsByVarianceClassification(summary, JobCostVarianceClassification.COST_OVER_ESTIMATE)
        assertEquals(1, overEstJobs.size)
        assertEquals("JOB-2026-001", overEstJobs.first().jobId)
        assertEquals(BigDecimal("15.00"), overEstJobs.first().costVariance)
    }
}
