package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchExecutionValidationTest {

    private fun sampleDispatch(
        dispatchId: String = "DISP-01",
        projectId: String = "PRJ-01",
        dispatchNo: String = "DN-2026-001",
        deliveryOrderId: String = "DO-01",
        deliveryChallanId: String = "CH-01",
        status: DispatchExecutionStatus = DispatchExecutionStatus.DRAFT
    ): DispatchExecution {
        return DispatchExecution(
            dispatchExecutionId = dispatchId,
            projectId = projectId,
            dispatchNo = dispatchNo,
            deliveryOrderId = deliveryOrderId,
            deliveryChallanId = deliveryChallanId,
            customerId = "CUST-01",
            sourceWarehouseId = "WH-01",
            sourceLocationId = "LOC-01",
            dispatchType = DispatchExecutionType.STANDARD,
            status = status,
            dispatchDate = 10000L,
            notes = "Handle with care",
            createdBy = "user-1",
            createdAt = 10000L,
            updatedAt = 10000L
        )
    }

    private fun sampleLine(
        lineId: String = "DLINE-01",
        dispatchId: String = "DISP-01",
        projectId: String = "PRJ-01",
        challanLineId: String = "CLINE-01",
        doLineId: String = "DOLINE-01",
        productId: String = "PROD-01",
        requestedQty: Double = 100.0,
        dispatchQty: Double = 100.0
    ): DispatchExecutionLine {
        return DispatchExecutionLine(
            dispatchExecutionLineId = lineId,
            projectId = projectId,
            dispatchExecutionId = dispatchId,
            deliveryChallanLineId = challanLineId,
            deliveryOrderLineId = doLineId,
            productId = productId,
            requestedQuantity = requestedQty,
            dispatchQuantity = dispatchQty,
            sourceLocationId = "LOC-01",
            createdAt = 10000L
        )
    }

    @Test
    fun `valid dispatch execution and lines passes validation`() {
        val dispatch = sampleDispatch()
        val lines = listOf(sampleLine())
        val result = DispatchExecutionValidator.validateDispatchExecution(dispatch, lines)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `dispatch with empty lines fails validation`() {
        val dispatch = sampleDispatch()
        val result = DispatchExecutionValidator.validateDispatchExecution(dispatch, emptyList())
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("at least one line item"))
    }

    @Test
    fun `line with mismatched project fails validation`() {
        val dispatch = sampleDispatch(projectId = "PRJ-01")
        val lines = listOf(sampleLine(projectId = "PRJ-99"))
        val result = DispatchExecutionValidator.validateDispatchExecution(dispatch, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Project mismatch"))
    }

    @Test
    fun `line with mismatched dispatchExecutionId fails validation`() {
        val dispatch = sampleDispatch(dispatchId = "DISP-01")
        val lines = listOf(sampleLine(dispatchId = "DISP-99"))
        val result = DispatchExecutionValidator.validateDispatchExecution(dispatch, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Dispatch ID mismatch"))
    }

    @Test
    fun `challan eligibility passes for APPROVED and READY_FOR_DISPATCH`() {
        val challan = DeliveryChallan(
            challanId = "CH-01",
            projectId = "PRJ-01",
            challanNo = "CH-NO-1",
            deliveryOrderId = "DO-01",
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.APPROVED,
            issueDate = 1000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val resApproved = DispatchExecutionValidator.validateChallanEligibility(challan, "PRJ-01")
        assertTrue(resApproved is DomainResult.Success)

        val resReady = DispatchExecutionValidator.validateChallanEligibility(
            challan.copy(status = DeliveryChallanStatus.READY_FOR_DISPATCH),
            "PRJ-01"
        )
        assertTrue(resReady is DomainResult.Success)
    }

    @Test
    fun `challan eligibility fails for DRAFT, CANCELLED, or DISPATCHED`() {
        val challan = DeliveryChallan(
            challanId = "CH-01",
            projectId = "PRJ-01",
            challanNo = "CH-NO-1",
            deliveryOrderId = "DO-01",
            customerId = null,
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = 1000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        assertTrue(DispatchExecutionValidator.validateChallanEligibility(challan, "PRJ-01") is DomainResult.Error)
        assertTrue(DispatchExecutionValidator.validateChallanEligibility(challan.copy(status = DeliveryChallanStatus.CANCELLED), "PRJ-01") is DomainResult.Error)
        assertTrue(DispatchExecutionValidator.validateChallanEligibility(challan.copy(status = DeliveryChallanStatus.DISPATCHED), "PRJ-01") is DomainResult.Error)
    }

    @Test
    fun `immutable identity fields alteration rejected`() {
        val original = sampleDispatch()
        val alteredNo = original.copy(dispatchNo = "NEW-NO")
        assertTrue(DispatchExecutionValidator.validateImmutableIdentity(original, alteredNo) is DomainResult.Error)

        val alteredChallan = original.copy(deliveryChallanId = "NEW-CH")
        assertTrue(DispatchExecutionValidator.validateImmutableIdentity(original, alteredChallan) is DomainResult.Error)
    }
}
