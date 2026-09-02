package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchExecutionLineValidationTest {

    @Test
    fun `valid dispatch execution line passes validation`() {
        val line = DispatchExecutionLine(
            dispatchExecutionLineId = "DLINE-1",
            projectId = "PRJ-1",
            dispatchExecutionId = "DISP-1",
            deliveryChallanLineId = "CLINE-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-1",
            requestedQuantity = 50.0,
            dispatchQuantity = 50.0,
            sourceLocationId = "LOC-1",
            createdAt = 1000L
        )
        val result = DispatchExecutionLineValidator.validateLine(line)
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `dispatch line with zero dispatch quantity throws exception on construction`() {
        DispatchExecutionLine(
            dispatchExecutionLineId = "DLINE-1",
            projectId = "PRJ-1",
            dispatchExecutionId = "DISP-1",
            deliveryChallanLineId = "CLINE-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-1",
            requestedQuantity = 50.0,
            dispatchQuantity = 0.0,
            sourceLocationId = "LOC-1",
            createdAt = 1000L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `dispatch line exceeding requested quantity throws exception on construction`() {
        DispatchExecutionLine(
            dispatchExecutionLineId = "DLINE-1",
            projectId = "PRJ-1",
            dispatchExecutionId = "DISP-1",
            deliveryChallanLineId = "CLINE-1",
            deliveryOrderLineId = "DOLINE-1",
            productId = "PROD-1",
            requestedQuantity = 50.0,
            dispatchQuantity = 51.0,
            sourceLocationId = "LOC-1",
            createdAt = 1000L
        )
    }
}
