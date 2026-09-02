package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.health.NotificationHealthEvaluator
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Notification delivery health & channel evaluation test suite (INFRA-04 Step 09).
 */
class NotificationHealthTest {

    private lateinit var evaluator: NotificationHealthEvaluator

    @Before
    fun setUp() {
        evaluator = NotificationHealthEvaluator()
    }

    @Test
    fun test01_highDeliverySuccess_isHealthy() {
        repeat(20) { evaluator.recordDelivery(NotificationChannel.IN_APP, isSuccess = true, latencyMs = 50) }
        val health = evaluator.evaluate()
        assertEquals(OperationalHealthStatus.HEALTHY, health.status)
        assertEquals(100.0, health.overallDeliveryRate, 0.001)
    }

    @Test
    fun test02_channelFailures_isDegraded() {
        repeat(8) { evaluator.recordDelivery(NotificationChannel.EMAIL, isSuccess = true, latencyMs = 100) }
        repeat(2) { evaluator.recordDelivery(NotificationChannel.EMAIL, isSuccess = false, latencyMs = 100) }
        val health = evaluator.evaluate()
        // 80% delivery on EMAIL -> Degraded (<95%)
        assertEquals(OperationalHealthStatus.DEGRADED, health.status)
    }

    @Test
    fun test03_criticalFailureRate_isCritical() {
        repeat(3) { evaluator.recordDelivery(NotificationChannel.SMS, isSuccess = true, latencyMs = 100) }
        repeat(12) { evaluator.recordDelivery(NotificationChannel.SMS, isSuccess = false, latencyMs = 100) }
        val health = evaluator.evaluate()
        assertEquals(OperationalHealthStatus.CRITICAL, health.status)
    }
}
