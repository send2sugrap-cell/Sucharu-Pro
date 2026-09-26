package com.sucharu.sucharupro.domain.service.customer360

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class Customer360MasterServiceTest {

    private lateinit var service: Customer360MasterService

    @Before
    fun setUp() {
        service = Customer360MasterService()
    }

    @Test
    fun `buildCustomer360View_aggregatesAllCanonicalModuleSummaries`() {
        val view = service.buildCustomer360View("CUST-1001")

        assertNotNull(view)
        assertEquals("CUST-1001", view.customer.customerId)
        assertEquals("Dhaka Printing Press & Media", view.customer.displayName)

        // Verify Financial 360 Integration (BI-01)
        assertEquals(BigDecimal("150000.00"), view.financial360.totalOutstandingDue)

        // Verify Quotation & Order Commercial Lock Summaries (BI-03)
        assertEquals(4, view.quotationSummary.totalQuotationsCount)
        assertEquals(2, view.quotationSummary.acceptedQuotationsCount)
        assertEquals(2, view.orderSummary.totalOrdersCount)
        assertEquals(BigDecimal("2260.00"), view.orderSummary.totalCommercialOrderValueSum)

        // Verify Production, Delivery, and Activity Timeline
        assertEquals(1, view.productionSummary.activeJobsCount)
        assertEquals(1, view.deliverySummary.deliveredOrdersCount)
        assertEquals(2, view.activityTimeline.size)
    }
}
