package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.health.NotificationHealthEvaluator
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Provider-specific availability and circuit breaker tracking test suite (INFRA-04 Step 09).
 */
class ProviderHealthTest {

    private lateinit var evaluator: NotificationHealthEvaluator

    @Before
    fun setUp() {
        evaluator = NotificationHealthEvaluator()
    }

    @Test
    fun test01_providerSuccess_isHealthy() {
        evaluator.recordProviderResult("TwilioSMS", isSuccess = true, latencyMs = 200)
        val health = evaluator.evaluate()
        val provider = health.providerHealth.find { it.providerName == "TwilioSMS" }
        assertNotNull(provider)
        assertEquals(OperationalHealthStatus.HEALTHY, provider!!.status)
        assertEquals("CLOSED", provider.circuitState)
    }

    @Test
    fun test02_consecutiveFailures_tripsCircuitToOpen() {
        repeat(5) { evaluator.recordProviderResult("SendGridEmail", isSuccess = false, latencyMs = 500) }
        val health = evaluator.evaluate()
        val provider = health.providerHealth.find { it.providerName == "SendGridEmail" }
        assertNotNull(provider)
        assertEquals(OperationalHealthStatus.CRITICAL, provider!!.status)
        assertEquals("OPEN", provider.circuitState)
        assertEquals(5, provider.consecutiveFailures)
    }
}
