package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryReturnValidationTest {

    private fun sampleReturn(id: String = "RET-1", projectId: String = "PRJ-01") = DeliveryReturn(
        returnId = id,
        projectId = projectId,
        returnNo = "RET-001",
        deliveryOrderId = "DO-01",
        status = DeliveryReturnStatus.DRAFT,
        requestedBy = "user-1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun sampleLine(id: String = "RL-1", returnId: String = "RET-1", projectId: String = "PRJ-01", doLineId: String = "DOL-1") = DeliveryReturnLine(
        returnLineId = id,
        returnId = returnId,
        projectId = projectId,
        deliveryOrderLineId = doLineId,
        productId = "PROD-1",
        returnedQuantity = 50.0,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun `valid return and lines pass validation`() {
        val ret = sampleReturn()
        val lines = listOf(sampleLine())
        assertTrue(DeliveryReturnValidator.validateReturn(ret, lines) is DomainResult.Success)
    }

    @Test
    fun `empty return lines list is rejected`() {
        val ret = sampleReturn()
        val result = DeliveryReturnValidator.validateReturn(ret, emptyList())
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `duplicate delivery order line in same return is rejected`() {
        val ret = sampleReturn()
        val lines = listOf(sampleLine("RL-1", doLineId = "DOL-1"), sampleLine("RL-2", doLineId = "DOL-1"))
        val result = DeliveryReturnValidator.validateReturn(ret, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Duplicate item line detected"))
    }

    @Test
    fun `cross project line inside return is rejected`() {
        val ret = sampleReturn("RET-1", "PRJ-01")
        val line = sampleLine("RL-1", "RET-1", "PRJ-02")
        val result = DeliveryReturnValidator.validateReturn(ret, listOf(line))
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Project ID mismatch"))
    }
}
