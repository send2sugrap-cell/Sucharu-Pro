package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Metric collection correctness, counters, gauges, and latencies test suite (INFRA-04 Step 09).
 */
class ObservabilityMetricsTest {

    private lateinit var registry: ObservabilityMetricsRegistry

    @Before
    fun setUp() {
        registry = ObservabilityMetricsRegistry()
    }

    @Test
    fun test01_counterIncrement_correctlyTracks() {
        registry.increment("test_counter", 1)
        registry.increment("test_counter", 5)
        assertEquals(6L, registry.getCounter("test_counter"))
    }

    @Test
    fun test02_gaugeSet_correctlyTracks() {
        registry.setGauge("active_queue_depth", 42)
        assertEquals(42L, registry.getGauge("active_queue_depth"))
        registry.setGauge("active_queue_depth", 10)
        assertEquals(10L, registry.getGauge("active_queue_depth"))
    }

    @Test
    fun test03_latencyAveraging_computesCorrectly() {
        registry.recordLatency("api_latency", 100)
        registry.recordLatency("api_latency", 200)
        registry.recordLatency("api_latency", 300)
        assertEquals(200.0, registry.getAverageLatency("api_latency"), 0.001)
    }

    @Test
    fun test04_domainEventConvenienceOperations() {
        registry.recordEventPublished("ORDER_CREATED", "p-001", 15)
        registry.recordEventConsumed("ORDER_CREATED", "p-001", 25)
        val pubCount = registry.getCounter("events_published_total", mapOf("event_type" to "ORDER_CREATED", "project" to "p-001"))
        val conCount = registry.getCounter("events_consumed_total", mapOf("event_type" to "ORDER_CREATED", "project" to "p-001"))
        assertEquals(1L, pubCount)
        assertEquals(1L, conCount)
    }

    @Test
    fun test05_notificationConvenienceOperations() {
        registry.recordNotificationDispatched(NotificationChannel.EMAIL, "p-001")
        registry.recordNotificationDelivered(NotificationChannel.EMAIL, "p-001", 120)
        val dispCount = registry.getCounter("notifications_dispatched_total", mapOf("notification_channel" to "EMAIL", "project" to "p-001"))
        val delivCount = registry.getCounter("notifications_delivered_total", mapOf("notification_channel" to "EMAIL", "project" to "p-001"))
        assertEquals(1L, dispCount)
        assertEquals(1L, delivCount)
    }
}
