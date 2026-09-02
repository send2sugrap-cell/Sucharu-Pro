package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryItemVerificationValidationTest {

    private fun sampleVerification(
        verificationId: String = "VERIF-01",
        projectId: String = "PRJ-01",
        verificationNo: String = "V-2026-001",
        deliveryOrderId: String = "DO-01",
        deliveryChallanId: String = "CH-01",
        dispatchExecutionId: String = "DISP-01"
    ): DeliveryItemVerification {
        return DeliveryItemVerification(
            verificationId = verificationId,
            projectId = projectId,
            verificationNo = verificationNo,
            deliveryOrderId = deliveryOrderId,
            deliveryChallanId = deliveryChallanId,
            dispatchExecutionId = dispatchExecutionId,
            status = DeliveryItemVerificationStatus.DRAFT,
            remarks = "Sample remarks",
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }

    private fun sampleLine(
        lineId: String = "VLINE-01",
        verificationId: String = "VERIF-01",
        projectId: String = "PRJ-01",
        dispatchLineId: String = "DLINE-01",
        challanLineId: String = "CLINE-01",
        doLineId: String = "DOLINE-01",
        productId: String = "PROD-01",
        expectedQty: Double = 100.0,
        verifiedQty: Double = 100.0
    ): DeliveryItemVerificationLine {
        return DeliveryItemVerificationLine(
            verificationLineId = lineId,
            verificationId = verificationId,
            projectId = projectId,
            dispatchExecutionLineId = dispatchLineId,
            challanLineId = challanLineId,
            deliveryOrderLineId = doLineId,
            productId = productId,
            expectedQuantity = expectedQty,
            verifiedQuantity = verifiedQty,
            createdAt = 1000L
        )
    }

    @Test
    fun `valid verification and lines passes validation`() {
        val verification = sampleVerification()
        val lines = listOf(sampleLine())
        val result = DeliveryItemVerificationValidator.validateVerification(verification, lines)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `verification with empty lines fails validation`() {
        val verification = sampleVerification()
        val result = DeliveryItemVerificationValidator.validateVerification(verification, emptyList())
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("at least one line item"))
    }

    @Test
    fun `line with mismatched project fails validation`() {
        val verification = sampleVerification(projectId = "PRJ-01")
        val lines = listOf(sampleLine(projectId = "PRJ-99"))
        val result = DeliveryItemVerificationValidator.validateVerification(verification, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Project mismatch"))
    }

    @Test
    fun `line with mismatched verificationId fails validation`() {
        val verification = sampleVerification(verificationId = "VERIF-01")
        val lines = listOf(sampleLine(verificationId = "VERIF-99"))
        val result = DeliveryItemVerificationValidator.validateVerification(verification, lines)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Verification ID mismatch"))
    }

    @Test
    fun `dispatch eligibility passes for DISPATCHED status and same project`() {
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
        val result = DeliveryItemVerificationValidator.validateDispatchEligibility(dispatch, "PRJ-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `dispatch eligibility fails for non-dispatched status`() {
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
            status = DispatchExecutionStatus.READY_FOR_EXECUTION,
            dispatchDate = 1000L,
            notes = null,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val result = DeliveryItemVerificationValidator.validateDispatchEligibility(dispatch, "PRJ-01")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Status must be DISPATCHED"))
    }

    @Test
    fun `immutable identity fields alteration rejected`() {
        val original = sampleVerification()
        val alteredNo = original.copy(verificationNo = "NEW-NO")
        assertTrue(DeliveryItemVerificationValidator.validateImmutableIdentity(original, alteredNo) is DomainResult.Error)

        val alteredDispatch = original.copy(dispatchExecutionId = "NEW-DISP")
        assertTrue(DeliveryItemVerificationValidator.validateImmutableIdentity(original, alteredDispatch) is DomainResult.Error)
    }
}
