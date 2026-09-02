package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.slo.SloEngine
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import com.sucharu.sucharupro.domain.observability.SloDefinition
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SLO/SLA evaluation and threshold testing (INFRA-04 Step 09).
 */
class SloEvaluationTest {

    private lateinit var sloEngine: SloEngine

    @Before
    fun setUp() {
        sloEngine = SloEngine()
    }

    @Test
    fun test01_meetingSlo_isHealthy() {
        val result = sloEngine.evaluateSlo("slo-notif-deliv", 99.5)
        assertNotNull(result)
        assertTrue(result!!.isMeetingSlo)
        assertEquals(OperationalHealthStatus.HEALTHY, result.status)
    }

    @Test
    fun test02_warningThreshold_isDegraded() {
        // Target 99.0, Warning 97.0 -> 97.5 is Degraded
        val result = sloEngine.evaluateSlo("slo-notif-deliv", 97.5)
        assertNotNull(result)
        assertFalse(result!!.isMeetingSlo)
        assertEquals(OperationalHealthStatus.DEGRADED, result.status)
    }

    @Test
    fun test03_criticalThreshold_isCritical() {
        // Target 99.0, Critical 90.0 -> 85.0 is Critical
        val result = sloEngine.evaluateSlo("slo-notif-deliv", 85.0)
        assertNotNull(result)
        assertFalse(result!!.isMeetingSlo)
        assertEquals(OperationalHealthStatus.CRITICAL, result.status)
    }
}
