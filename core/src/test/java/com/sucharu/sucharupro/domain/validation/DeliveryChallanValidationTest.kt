package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryChallanValidationTest {

    private fun sampleChallan(
        challanId: String = "CHAL-01",
        projectId: String = "PRJ-01",
        challanNo: String = "CH-2026-001",
        deliveryOrderId: String = "DO-01",
        status: DeliveryChallanStatus = DeliveryChallanStatus.DRAFT
    ): DeliveryChallan {
        return DeliveryChallan(
            challanId = challanId,
            projectId = projectId,
            challanNo = challanNo,
            deliveryOrderId = deliveryOrderId,
            customerId = "CUST-100",
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = status,
            issueDate = 10000L,
            notes = "Standard packaging",
            createdBy = "user-admin",
            createdAt = 10000L,
            updatedAt = 10000L
        )
    }

    private fun sampleLine(
        lineId: String = "CLINE-01",
        challanId: String = "CHAL-01",
        projectId: String = "PRJ-01",
        doLineId: String = "DOLINE-01",
        productId: String = "PROD-1",
        quantity: Double = 10.0
    ): DeliveryChallanLine {
        return DeliveryChallanLine(
            lineId = lineId,
            challanId = challanId,
            projectId = projectId,
            deliveryOrderLineId = doLineId,
            productId = productId,
            quantity = quantity,
            notes = null
        )
    }

    @Test
    fun `valid challan and lines passes validation`() {
        val challan = sampleChallan()
        val lines = listOf(sampleLine())
        val result = DeliveryChallanValidator.validateChallan(challan, lines)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `challan with empty lines fails validation`() {
        val challan = sampleChallan()
        val result = DeliveryChallanValidator.validateChallan(challan, emptyList())
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("at least one line item"))
    }

    @Test
    fun `line with mismatched project fails validation`() {
        val challan = sampleChallan(projectId = "PRJ-01")
        val lines = listOf(sampleLine(projectId = "PRJ-99"))
        val result = DeliveryChallanValidator.validateChallan(challan, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Project ID mismatch"))
    }

    @Test
    fun `line with mismatched challanId fails validation`() {
        val challan = sampleChallan(challanId = "CHAL-01")
        val lines = listOf(sampleLine(challanId = "CHAL-99"))
        val result = DeliveryChallanValidator.validateChallan(challan, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Challan ID mismatch"))
    }

    @Test
    fun `delivery order eligibility passes for APPROVED and READY_FOR_DISPATCH`() {
        val approvedOrder = DeliveryOrder(
            deliveryOrderId = "DO-01",
            projectId = "PRJ-01",
            deliveryOrderNo = "DO-NO-1",
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.APPROVED,
            requestedDeliveryDate = 20000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val result = DeliveryChallanValidator.validateDeliveryOrderEligibility(approvedOrder, "PRJ-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `delivery order eligibility fails for DRAFT and CANCELLED`() {
        val draftOrder = DeliveryOrder(
            deliveryOrderId = "DO-01",
            projectId = "PRJ-01",
            deliveryOrderNo = "DO-NO-1",
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.DRAFT,
            requestedDeliveryDate = 20000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val resultDraft = DeliveryChallanValidator.validateDeliveryOrderEligibility(draftOrder, "PRJ-01")
        assertTrue(resultDraft is DomainResult.Error)

        val cancelledOrder = draftOrder.copy(status = DeliveryOrderStatus.CANCELLED)
        val resultCancelled = DeliveryChallanValidator.validateDeliveryOrderEligibility(cancelledOrder, "PRJ-01")
        assertTrue(resultCancelled is DomainResult.Error)
    }

    @Test
    fun `immutable identity fields alteration rejected`() {
        val original = sampleChallan()
        val alteredNo = original.copy(challanNo = "NEW-NO")
        assertTrue(DeliveryChallanValidator.validateImmutableIdentity(original, alteredNo) is DomainResult.Error)

        val alteredDo = original.copy(deliveryOrderId = "NEW-DO")
        assertTrue(DeliveryChallanValidator.validateImmutableIdentity(original, alteredDo) is DomainResult.Error)

        val alteredProject = original.copy(projectId = "PRJ-99")
        assertTrue(DeliveryChallanValidator.validateImmutableIdentity(original, alteredProject) is DomainResult.Error)
    }
}
