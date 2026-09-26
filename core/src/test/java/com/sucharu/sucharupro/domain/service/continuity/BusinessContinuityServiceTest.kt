package com.sucharu.sucharupro.domain.service.continuity

import com.sucharu.sucharupro.domain.model.continuity.ContinuityHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BusinessContinuityServiceTest {

    private lateinit var service: BusinessContinuityService

    @Before
    fun setUp() {
        service = BusinessContinuityService()
    }

    @Test
    fun `buildBusinessContinuitySummary_computesHealthAndRpoRtoTargets`() {
        val summary = service.buildBusinessContinuitySummary()

        assertNotNull(summary)
        assertEquals(ContinuityHealthStatus.HEALTHY, summary.healthStatus)
        assertEquals(1, summary.rpoRtoTargets.targetRpoHours)
        assertEquals(2, summary.rpoRtoTargets.targetRtoHours)
        assertEquals("V20261219", summary.activeFlywayMigrationVersion)

        // CRITICAL INVARIANT PROOF:
        // PostgreSQL RLS Security MUST be active!
        assertTrue("PostgreSQL RLS Security MUST be active!", summary.isPostgresRlsSecurityActive)
        assertTrue("Backup MUST be verified!", summary.isBackupVerified)
    }
}
