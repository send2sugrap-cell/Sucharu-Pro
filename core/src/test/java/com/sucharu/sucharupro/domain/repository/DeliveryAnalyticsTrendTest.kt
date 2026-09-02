package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsPeriod
import com.sucharu.sucharupro.domain.service.delivery.DeliveryAnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryAnalyticsTrendTest {

    @Test
    fun `trends group points chronologically`() {
        val orders = listOf(
            DeliveryOrder(
                deliveryOrderId = "DO-1",
                projectId = "PRJ-01",
                deliveryOrderNo = "DON-1",
                customerId = "CUST-1",
                sourceReferenceId = "SO-1",
                sourceReferenceType = "SO",
                deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
                priority = DeliveryPriority.NORMAL,
                status = DeliveryOrderStatus.DELIVERED,
                requestedDeliveryDate = 2000L,
                notes = null,
                createdBy = "u1",
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L
            )
        )

        val trend = DeliveryAnalyticsCalculator.calculateTrends(
            projectId = "PRJ-01",
            period = DeliveryAnalyticsPeriod.ALL_TIME,
            orders = orders
        )

        assertEquals("PRJ-01", trend.projectId)
        assertTrue(trend.points.isNotEmpty())
        assertEquals(1, trend.points.first().orderCount)
    }
}
