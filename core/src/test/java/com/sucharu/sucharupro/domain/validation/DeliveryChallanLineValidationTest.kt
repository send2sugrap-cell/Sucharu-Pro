package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryChallanLineValidationTest {

    @Test
    fun `valid challan line passes validation`() {
        val line = DeliveryChallanLine(
            lineId = "LINE-1",
            challanId = "CH-1",
            projectId = "PRJ-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-1",
            quantity = 15.0,
            notes = "Checked"
        )
        val result = DeliveryChallanLineValidator.validateLine(line)
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `challan line with zero quantity throws exception on construction`() {
        DeliveryChallanLine(
            lineId = "LINE-1",
            challanId = "CH-1",
            projectId = "PRJ-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-1",
            quantity = 0.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `challan line with negative quantity throws exception on construction`() {
        DeliveryChallanLine(
            lineId = "LINE-1",
            challanId = "CH-1",
            projectId = "PRJ-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-1",
            quantity = -10.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `challan line with blank lineId throws exception on construction`() {
        DeliveryChallanLine(
            lineId = "   ",
            challanId = "CH-1",
            projectId = "PRJ-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-1",
            quantity = 5.0
        )
    }
}
