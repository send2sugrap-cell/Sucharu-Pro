package com.sucharu.sucharupro.domain.service.ai.ops

import com.sucharu.sucharupro.domain.model.ai.ops.OpsExceptionCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionOpsAgentServiceTest {

    private lateinit var service: ProductionOpsAgentService

    @Before
    fun setUp() {
        service = ProductionOpsAgentService()
    }

    @Test
    fun `buildDailyOperationsBrief_assemblesAuthorizedBriefAndEnforcesFactSeparation`() = runBlocking {
        val brief = service.buildDailyOperationsBrief(userRole = "MANAGER", tenantId = "TENANT-001")

        assertNotNull(brief)
        assertEquals(2, brief.totalActiveJobsCount)
        assertEquals(1, brief.delayedJobsCount)
        assertTrue("13-stage canonical pipeline MUST be preserved!", brief.isCanonicalPipelinePreserved)

        assertEquals(2, brief.observations.size)
        val obs1 = brief.observations.first()
        assertEquals(OpsExceptionCategory.SLA_RISK, obs1.category)
        assertTrue(obs1.factSummary.startsWith("FACT:"))
        assertTrue(obs1.interpretation.startsWith("INTERPRETATION:"))
        assertTrue(obs1.recommendation.startsWith("RECOMMENDATION:"))
    }

    @Test(expected = IllegalAccessException::class)
    fun `buildDailyOperationsBrief_deniesAccessToCustomerRole`() {
        runBlocking {
            // Customer Role attempting to access internal daily brief MUST be denied!
            service.buildDailyOperationsBrief(userRole = "CUSTOMER", tenantId = "TENANT-001")
        }
    }

    @Test
    fun `evaluateJobOperationalStatus_returnsCustomerSafeObservationForCustomerRole`() = runBlocking {
        val custObs = service.evaluateJobOperationalStatus("JOB-2026-001", userRole = "CUSTOMER")

        assertNotNull(custObs)
        assertEquals("JOB-2026-001", custObs.targetEntityId)
        assertTrue(custObs.factSummary.contains("PRINTING stage"))
        // Customer-safe observation does NOT expose internal telemetry or machine delays
        assertTrue(custObs.recommendation.contains("SMS notification"))
    }
}
