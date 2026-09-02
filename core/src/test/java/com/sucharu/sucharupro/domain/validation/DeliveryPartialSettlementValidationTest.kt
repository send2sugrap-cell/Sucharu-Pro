package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryPartialSettlementValidationTest {

    private fun sampleLine(id: String = "SL-1", doLineId: String = "DOL-1") = DeliveryPartialSettlementLine(
        settlementLineId = id,
        projectId = "PRJ-01",
        settlementId = "SETTLE-01",
        deliveryOrderLineId = doLineId,
        productId = "PROD-01",
        orderedQuantity = 100.0,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun sampleSettlement(id: String = "SETTLE-01", projectId: String = "PRJ-01") = DeliveryPartialSettlement(
        settlementId = id,
        projectId = projectId,
        deliveryOrderId = "DO-01",
        status = DeliverySettlementStatus.OPEN,
        totalOrderedQuantity = 100.0,
        totalPendingQuantity = 100.0,
        createdBy = "user-1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun `valid settlement and lines pass validation`() {
        val settlement = sampleSettlement()
        val lines = listOf(sampleLine())
        assertTrue(DeliveryPartialSettlementValidator.validateSettlement(settlement, lines) is DomainResult.Success)
    }

    @Test
    fun `empty lines reject settlement`() {
        val settlement = sampleSettlement()
        val result = DeliveryPartialSettlementValidator.validateSettlement(settlement, emptyList())
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `duplicate delivery order line in settlement lines is rejected`() {
        val settlement = sampleSettlement()
        val lines = listOf(sampleLine("SL-1", "DOL-1"), sampleLine("SL-2", "DOL-1"))
        val result = DeliveryPartialSettlementValidator.validateSettlement(settlement, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("duplicate Delivery Order Line"))
    }

    @Test
    fun `cancelled delivery order cannot initialize settlement`() {
        val doOrder = DeliveryOrder(
            deliveryOrderId = "DO-CANCELLED",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-01",
            customerId = "CUST-01",
            sourceReferenceId = "SO-01",
            sourceReferenceType = "SALES_ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.CANCELLED,
            requestedDeliveryDate = 2000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val result = DeliveryPartialSettlementValidator.validateDeliveryOrderEligibility(doOrder, "PRJ-01")
        assertTrue(result is DomainResult.Error)
    }
}
