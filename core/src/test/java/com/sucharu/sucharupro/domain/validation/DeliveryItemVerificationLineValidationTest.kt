package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryItemVerificationLineValidationTest {

    @Test
    fun `valid line passes validation`() {
        val line = DeliveryItemVerificationLine(
            verificationLineId = "VL-1",
            verificationId = "V-1",
            projectId = "PRJ-1",
            dispatchExecutionLineId = "DL-1",
            challanLineId = "CL-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            expectedQuantity = 100.0,
            verifiedQuantity = 100.0,
            issueQuantity = 0.0,
            resultType = DeliveryItemVerificationResultType.VERIFIED,
            issueType = DeliveryItemVerificationIssueType.NONE,
            createdAt = 1000L
        )
        assertTrue(DeliveryItemVerificationLineValidator.validateLine(line) is DomainResult.Success)
    }

    @Test
    fun `incompatible verified result and damage issue fails validation`() {
        val line = DeliveryItemVerificationLine(
            verificationLineId = "VL-1",
            verificationId = "V-1",
            projectId = "PRJ-1",
            dispatchExecutionLineId = "DL-1",
            challanLineId = "CL-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            expectedQuantity = 100.0,
            verifiedQuantity = 100.0,
            issueQuantity = 5.0,
            resultType = DeliveryItemVerificationResultType.VERIFIED,
            issueType = DeliveryItemVerificationIssueType.DAMAGED,
            createdAt = 1000L
        )
        val result = DeliveryItemVerificationLineValidator.validateLine(line)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot have an active issue"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative verified quantity throws exception on construction`() {
        DeliveryItemVerificationLine(
            verificationLineId = "VL-1",
            verificationId = "V-1",
            projectId = "PRJ-1",
            dispatchExecutionLineId = "DL-1",
            challanLineId = "CL-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            expectedQuantity = 100.0,
            verifiedQuantity = -10.0,
            createdAt = 1000L
        )
    }
}
