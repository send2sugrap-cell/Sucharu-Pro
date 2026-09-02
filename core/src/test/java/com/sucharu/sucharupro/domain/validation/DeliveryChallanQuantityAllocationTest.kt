package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryChallanQuantityAllocationTest {

    private val doLine100 = DeliveryOrderLine(
        lineId = "DOLINE-1",
        deliveryOrderId = "DO-1",
        projectId = "PRJ-1",
        productId = "PROD-A",
        requestedQuantity = 100.0,
        notes = null
    )

    private val doLine50 = DeliveryOrderLine(
        lineId = "DOLINE-2",
        deliveryOrderId = "DO-1",
        projectId = "PRJ-1",
        productId = "PROD-B",
        requestedQuantity = 50.0,
        notes = null
    )

    @Test
    fun `split allocation matching exact quantity passes`() {
        val existingLines = listOf(
            DeliveryChallanLine(
                lineId = "CLINE-1",
                challanId = "CH-1",
                projectId = "PRJ-1",
                deliveryOrderLineId = "DOLINE-1",
                productId = "PROD-A",
                quantity = 40.0
            )
        )

        val newLines = listOf(
            DeliveryChallanLine(
                lineId = "CLINE-2",
                challanId = "CH-2",
                projectId = "PRJ-1",
                deliveryOrderLineId = "DOLINE-1",
                productId = "PROD-A",
                quantity = 60.0
            )
        )

        val result = DeliveryChallanValidator.validateQuantityAllocation(
            orderLines = listOf(doLine100),
            existingActiveChallanLines = existingLines,
            newChallanLines = newLines
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `over allocation by 1 unit fails validation`() {
        val existingLines = listOf(
            DeliveryChallanLine(
                lineId = "CLINE-1",
                challanId = "CH-1",
                projectId = "PRJ-1",
                deliveryOrderLineId = "DOLINE-1",
                productId = "PROD-A",
                quantity = 60.0
            )
        )

        val newLines = listOf(
            DeliveryChallanLine(
                lineId = "CLINE-2",
                challanId = "CH-2",
                projectId = "PRJ-1",
                deliveryOrderLineId = "DOLINE-1",
                productId = "PROD-A",
                quantity = 41.0
            )
        )

        val result = DeliveryChallanValidator.validateQuantityAllocation(
            orderLines = listOf(doLine100),
            existingActiveChallanLines = existingLines,
            newChallanLines = newLines
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Over-allocation"))
    }

    @Test
    fun `product mismatch between challan line and DO line fails validation`() {
        val newLines = listOf(
            DeliveryChallanLine(
                lineId = "CLINE-1",
                challanId = "CH-1",
                projectId = "PRJ-1",
                deliveryOrderLineId = "DOLINE-1",
                productId = "PROD-WRONG",
                quantity = 10.0
            )
        )

        val result = DeliveryChallanValidator.validateQuantityAllocation(
            orderLines = listOf(doLine100),
            existingActiveChallanLines = emptyList(),
            newChallanLines = newLines
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Product mismatch"))
    }
}
