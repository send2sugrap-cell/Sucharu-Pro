package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.health.N8nHealthEvaluator
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * n8n webhook automation health & signature failure detection test suite (INFRA-04 Step 09).
 */
class N8nObservabilitySecurityTest {

    private lateinit var evaluator: N8nHealthEvaluator

    @Before
    fun setUp() {
        evaluator = N8nHealthEvaluator()
    }

    @Test
    fun test01_successfulWebhooks_isHealthy() {
        repeat(10) { evaluator.recordWebhook(isSuccess = true, latencyMs = 80) }
        val health = evaluator.evaluate()
        assertEquals(OperationalHealthStatus.HEALTHY, health.status)
        assertEquals(0, health.signatureRejections)
    }

    @Test
    fun test02_signatureRejections_isCritical() {
        repeat(12) { evaluator.recordWebhook(isSuccess = false, latencyMs = 20, isSignatureRejected = true) }
        val health = evaluator.evaluate()
        assertEquals(OperationalHealthStatus.CRITICAL, health.status)
        assertTrue(health.issues.any { it.contains("High n8n webhook signature rejection rate") })
    }
}
