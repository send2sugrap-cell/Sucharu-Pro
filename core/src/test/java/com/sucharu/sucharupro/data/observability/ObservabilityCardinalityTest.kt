package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Metric cardinality explosion protection test suite (INFRA-04 Step 09).
 */
class ObservabilityCardinalityTest {

    private lateinit var registry: ObservabilityMetricsRegistry

    @Before
    fun setUp() {
        registry = ObservabilityMetricsRegistry()
    }

    @Test
    fun test01_highCardinalityKeys_areStrippedFromLabels() {
        val tagsWithOrderId = mapOf(
            "project" to "p-001",
            "order_id" to "ORD-123456789",
            "customer_id" to "CUST-987654321",
            "jwt" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        registry.increment("test_unrestricted", 1, tagsWithOrderId)

        // Only allowed dimension 'project' is retained in key
        val count = registry.getCounter("test_unrestricted", mapOf("project" to "p-001"))
        assertEquals(1L, count)
    }

    @Test
    fun test02_allowedDimensions_arePreserved() {
        val allowedTags = mapOf(
            "project" to "p-001",
            "event_type" to "ORDER_CREATED",
            "notification_channel" to "SMS",
            "failure_class" to "NETWORK"
        )
        registry.increment("test_allowed", 1, allowedTags)
        val count = registry.getCounter("test_allowed", allowedTags)
        assertEquals(1L, count)
    }
}
