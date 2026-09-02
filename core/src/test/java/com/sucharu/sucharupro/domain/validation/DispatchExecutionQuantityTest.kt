package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchExecutionQuantityTest {

    private val challanLine = DeliveryChallanLine(
        lineId = "CLINE-1",
        challanId = "CH-1",
        projectId = "PRJ-1",
        deliveryOrderLineId = "DOLINE-1",
        productId = "PROD-A",
        quantity = 100.0
    )

    @Test
    fun `dispatch line within authorized challan quantity passes validation`() {
        val dispatchLine = DispatchExecutionLine(
            dispatchExecutionLineId = "DLINE-1",
            projectId = "PRJ-1",
            dispatchExecutionId = "DISP-1",
            deliveryChallanLineId = "CLINE-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-A",
            requestedQuantity = 100.0,
            dispatchQuantity = 100.0,
            sourceLocationId = "LOC-1",
            createdAt = 1000L
        )

        val result = DispatchExecutionValidator.validateLinesAgainstChallan(
            challanLines = listOf(challanLine),
            dispatchLines = listOf(dispatchLine)
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `dispatch line exceeding challan line quantity fails validation`() {
        val dispatchLine = DispatchExecutionLine(
            dispatchExecutionLineId = "DLINE-1",
            projectId = "PRJ-1",
            dispatchExecutionId = "DISP-1",
            deliveryChallanLineId = "CLINE-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-A",
            requestedQuantity = 150.0,
            dispatchQuantity = 150.0,
            sourceLocationId = "LOC-1",
            createdAt = 1000L
        )

        val result = DispatchExecutionValidator.validateLinesAgainstChallan(
            challanLines = listOf(challanLine),
            dispatchLines = listOf(dispatchLine)
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("exceeds authorized Challan quantity"))
    }

    @Test
    fun `dispatch line with mismatched product fails validation`() {
        val dispatchLine = DispatchExecutionLine(
            dispatchExecutionLineId = "DLINE-1",
            projectId = "PRJ-1",
            dispatchExecutionId = "DISP-1",
            deliveryChallanLineId = "CLINE-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-WRONG",
            requestedQuantity = 50.0,
            dispatchQuantity = 50.0,
            sourceLocationId = "LOC-1",
            createdAt = 1000L
        )

        val result = DispatchExecutionValidator.validateLinesAgainstChallan(
            challanLines = listOf(challanLine),
            dispatchLines = listOf(dispatchLine)
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Product mismatch"))
    }
}
