package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryOrderLineValidationTest {

    @Test
    fun `valid delivery order line passes validation`() {
        val line = DeliveryOrderLine(
            lineId = "LINE-1",
            deliveryOrderId = "DO-1",
            projectId = "PRJ-1",
            productId = "PROD-1",
            requestedQuantity = 5.0,
            notes = "Standard packaging"
        )
        val result = DeliveryOrderLineValidator.validateLine(line)
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `line with zero quantity throws exception on construction`() {
        DeliveryOrderLine(
            lineId = "LINE-1",
            deliveryOrderId = "DO-1",
            projectId = "PRJ-1",
            productId = "PROD-1",
            requestedQuantity = 0.0,
            notes = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `line with negative quantity throws exception on construction`() {
        DeliveryOrderLine(
            lineId = "LINE-1",
            deliveryOrderId = "DO-1",
            projectId = "PRJ-1",
            productId = "PROD-1",
            requestedQuantity = -5.0,
            notes = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `line with blank lineId throws exception on construction`() {
        DeliveryOrderLine(
            lineId = "   ",
            deliveryOrderId = "DO-1",
            projectId = "PRJ-1",
            productId = "PROD-1",
            requestedQuantity = 1.0,
            notes = null
        )
    }
}
