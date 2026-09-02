package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryShipmentValidationTest {

    private fun sampleShipment(
        id: String = "SHP-01",
        projectId: String = "PRJ-01",
        no: String = "SHP-2026-001",
        doId: String = "DO-01",
        challanId: String = "CH-01",
        dispatchId: String = "DISP-01"
    ) = DeliveryShipment(
        shipmentId = id,
        projectId = projectId,
        shipmentNo = no,
        deliveryOrderId = doId,
        deliveryChallanId = challanId,
        dispatchExecutionId = dispatchId,
        currentStatus = DeliveryShipmentStatus.DRAFT,
        createdBy = "user-1",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Test
    fun `valid shipment passes validation`() {
        val shipment = sampleShipment()
        assertTrue(DeliveryShipmentValidator.validateShipment(shipment) is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank shipmentId fails at model construction`() {
        sampleShipment(id = "")
    }

    @Test
    fun `dispatch eligibility validation passes for matching DISPATCHED execution`() {
        val dispatch = DispatchExecution(
            dispatchExecutionId = "DISP-01",
            projectId = "PRJ-01",
            dispatchNo = "DN-01",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            customerId = null,
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.DISPATCHED,
            stockOutId = "SO-01",
            dispatchDate = 1000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L,
            dispatchedBy = "operator",
            dispatchedAt = 1000L
        )

        val result = DeliveryShipmentValidator.validateDispatchEligibility(
            dispatch = dispatch,
            targetProjectId = "PRJ-01",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `dispatch eligibility validation fails for mismatched order or challan`() {
        val dispatch = DispatchExecution(
            dispatchExecutionId = "DISP-01",
            projectId = "PRJ-01",
            dispatchNo = "DN-01",
            deliveryOrderId = "DO-01",
            deliveryChallanId = "CH-01",
            customerId = null,
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = DispatchExecutionStatus.DISPATCHED,
            stockOutId = "SO-01",
            dispatchDate = 1000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L,
            dispatchedBy = "operator",
            dispatchedAt = 1000L
        )

        val result = DeliveryShipmentValidator.validateDispatchEligibility(
            dispatch = dispatch,
            targetProjectId = "PRJ-01",
            deliveryOrderId = "DO-99",
            deliveryChallanId = "CH-01"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Delivery Order mismatch"))
    }

    @Test
    fun `immutable identity fields rejection`() {
        val original = sampleShipment()
        val alteredNo = original.copy(shipmentNo = "ALTERED-NO")
        assertTrue(DeliveryShipmentValidator.validateImmutableIdentity(original, alteredNo) is DomainResult.Error)

        val alteredDo = original.copy(deliveryOrderId = "ALTERED-DO")
        assertTrue(DeliveryShipmentValidator.validateImmutableIdentity(original, alteredDo) is DomainResult.Error)
    }
}
