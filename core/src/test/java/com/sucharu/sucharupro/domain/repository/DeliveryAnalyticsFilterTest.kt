package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryAnalyticsFilterTest {

    @Test
    fun `valid filter initializes properly`() {
        val filter = DeliveryAnalyticsFilter(
            projectId = "PRJ-01",
            period = DeliveryAnalyticsPeriod.THIS_MONTH,
            dateFrom = 1000L,
            dateTo = 2000L,
            customerId = "CUST-1"
        )
        assertEquals("PRJ-01", filter.projectId)
        assertEquals(DeliveryAnalyticsPeriod.THIS_MONTH, filter.period)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank project id throws exception`() {
        DeliveryAnalyticsFilter(projectId = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid date range throws exception`() {
        DeliveryAnalyticsFilter(
            projectId = "PRJ-01",
            dateFrom = 5000L,
            dateTo = 2000L
        )
    }
}
