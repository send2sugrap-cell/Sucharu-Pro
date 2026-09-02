package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.event.dispatcher.OutboxMetrics
import com.sucharu.sucharupro.data.observability.health.EventInfrastructureHealthEvaluator
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Event Infrastructure & Outbox health evaluator test suite (INFRA-04 Step 09).
 */
class EventInfrastructureHealthTest {

    private lateinit var metrics: OutboxMetrics
    private lateinit var evaluator: EventInfrastructureHealthEvaluator

    @Before
    fun setUp() {
        metrics = OutboxMetrics()
        evaluator = EventInfrastructureHealthEvaluator(metrics)
    }

    @Test
    fun test01_cleanOutbox_isHealthy() {
        val health = evaluator.evaluate(pendingOutboxCount = 5, processingOutboxCount = 2, deadLetterCount = 0)
        assertEquals(OperationalHealthStatus.HEALTHY, health.status)
        assertEquals(0, health.issues.size)
    }

    @Test
    fun test02_highBacklog_isDegraded() {
        val health = evaluator.evaluate(pendingOutboxCount = 600, processingOutboxCount = 50, deadLetterCount = 0)
        assertEquals(OperationalHealthStatus.DEGRADED, health.status)
        assertTrue(health.issues.any { it.contains("Outbox backlog high") })
    }

    @Test
    fun test03_criticalDeadLetters_isCritical() {
        val health = evaluator.evaluate(pendingOutboxCount = 10, processingOutboxCount = 2, deadLetterCount = 55)
        assertEquals(OperationalHealthStatus.CRITICAL, health.status)
        assertTrue(health.issues.any { it.contains("High dead-letter queue count") })
    }

    @Test
    fun test04_oldPendingAge_isDegraded() {
        val health = evaluator.evaluate(pendingOutboxCount = 10, oldestPendingAgeMs = 90_000)
        assertEquals(OperationalHealthStatus.DEGRADED, health.status)
        assertTrue(health.issues.any { it.contains("exceeds 60s") })
    }
}
