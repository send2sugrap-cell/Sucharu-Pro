package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.capacity.CapacityMonitor
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Capacity metrics and drain time calculation test suite (INFRA-04 Step 09).
 */
class CapacityMetricsTest {

    private lateinit var capacityMonitor: CapacityMonitor

    @Before
    fun setUp() {
        capacityMonitor = CapacityMonitor()
    }

    @Test
    fun test01_normalQueues_isHealthy() {
        val snapshot = capacityMonitor.captureSnapshot(outboxDepth = 10, notificationQueueDepth = 5, jobQueueDepth = 5, providerThroughputPerSec = 100.0)
        assertEquals(OperationalHealthStatus.HEALTHY, snapshot.status)
        assertEquals(0L, snapshot.estimatedTimeToDrainSec)
    }

    @Test
    fun test02_massiveBacklog_isCritical() {
        val snapshot = capacityMonitor.captureSnapshot(outboxDepth = 3000, notificationQueueDepth = 2500, jobQueueDepth = 1000, providerThroughputPerSec = 10.0)
        assertEquals(OperationalHealthStatus.CRITICAL, snapshot.status)
        assertTrue(snapshot.estimatedTimeToDrainSec > 300)
    }
}
