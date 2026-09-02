package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliverySplitDispatchLineValidationTest {

    @Test
    fun `valid split line within remaining bounds passes validation`() {
        val line = DeliverySplitDispatchLine(
            splitDispatchLineId = "SDL-1",
            projectId = "PRJ-01",
            splitDispatchId = "SD-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            quantity = 250.0,
            createdAt = 1000L
        )
        assertTrue(DeliverySplitDispatchLineValidator.validateLine(line, 500.0) is DomainResult.Success)
    }

    @Test
    fun `split line exceeding remaining authorized quantity is rejected`() {
        val line = DeliverySplitDispatchLine(
            splitDispatchLineId = "SDL-1",
            projectId = "PRJ-01",
            splitDispatchId = "SD-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            quantity = 600.0,
            createdAt = 1000L
        )
        val result = DeliverySplitDispatchLineValidator.validateLine(line, 500.0)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("exceeds remaining authorized quantity"))
    }
}
