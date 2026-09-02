package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryOrderValidationTest {

    private fun createValidOrder(
        orderId: String = "DO-001",
        projectId: String = "PRJ-01",
        orderNo: String = "DEL-2026-001",
        createdBy: String = "user-1",
        createdAt: Long = 1000L,
        updatedAt: Long = 1000L,
        status: DeliveryOrderStatus = DeliveryOrderStatus.DRAFT
    ): DeliveryOrder {
        return DeliveryOrder(
            deliveryOrderId = orderId,
            projectId = projectId,
            deliveryOrderNo = orderNo,
            customerId = "CUST-101",
            sourceReferenceId = "SO-501",
            sourceReferenceType = "ORDER",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = status,
            requestedDeliveryDate = 2000L,
            notes = "Handle with care",
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun createValidLine(
        lineId: String = "LINE-001",
        orderId: String = "DO-001",
        projectId: String = "PRJ-01",
        productId: String = "PROD-A",
        qty: Double = 10.0
    ): DeliveryOrderLine {
        return DeliveryOrderLine(
            lineId = lineId,
            deliveryOrderId = orderId,
            projectId = projectId,
            productId = productId,
            requestedQuantity = qty,
            notes = null
        )
    }

    @Test
    fun `valid delivery order and lines passes validation`() {
        val order = createValidOrder()
        val lines = listOf(createValidLine())
        val result = DeliveryOrderValidator.validateDeliveryOrder(order, lines)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `order with empty lines fails validation`() {
        val order = createValidOrder()
        val result = DeliveryOrderValidator.validateDeliveryOrder(order, emptyList())
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("at least one line item"))
    }

    @Test
    fun `line with mismatched project fails validation`() {
        val order = createValidOrder(projectId = "PRJ-01")
        val lines = listOf(createValidLine(projectId = "PRJ-02"))
        val result = DeliveryOrderValidator.validateDeliveryOrder(order, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Project ID mismatch"))
    }

    @Test
    fun `line with mismatched deliveryOrderId fails validation`() {
        val order = createValidOrder(orderId = "DO-001")
        val lines = listOf(createValidLine(orderId = "DO-999"))
        val result = DeliveryOrderValidator.validateDeliveryOrder(order, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Delivery Order ID mismatch"))
    }

    @Test
    fun `immutable identity validation passes when identity fields unchanged`() {
        val original = createValidOrder()
        val updated = original.copy(notes = "New notes", updatedAt = 2000L)
        val result = DeliveryOrderValidator.validateImmutableIdentity(original, updated)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `immutable identity validation fails when deliveryOrderId altered`() {
        val original = createValidOrder(orderId = "DO-001")
        val updated = original.copy(deliveryOrderId = "DO-002")
        val result = DeliveryOrderValidator.validateImmutableIdentity(original, updated)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `immutable identity validation fails when deliveryOrderNo altered`() {
        val original = createValidOrder(orderNo = "DEL-001")
        val updated = original.copy(deliveryOrderNo = "DEL-002")
        val result = DeliveryOrderValidator.validateImmutableIdentity(original, updated)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `immutable identity validation fails when projectId altered`() {
        val original = createValidOrder(projectId = "PRJ-01")
        val updated = original.copy(projectId = "PRJ-02")
        val result = DeliveryOrderValidator.validateImmutableIdentity(original, updated)
        assertTrue(result is DomainResult.Error)
    }
}
