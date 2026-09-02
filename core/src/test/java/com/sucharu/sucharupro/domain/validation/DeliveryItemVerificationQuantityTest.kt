package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import com.sucharu.sucharupro.domain.service.DeliveryItemVerificationClassificationService
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryItemVerificationQuantityTest {

    @Test
    fun `exact matching quantity is classified as VERIFIED with no issue`() {
        val result = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 100.0,
            verifiedQuantity = 100.0
        )
        assertEquals(DeliveryItemVerificationResultType.VERIFIED, result.resultType)
        assertEquals(DeliveryItemVerificationIssueType.NONE, result.issueType)
        assertEquals(0.0, result.issueQuantity, 0.001)
    }

    @Test
    fun `short quantity is classified as SHORT with shortage quantity`() {
        val result = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 100.0,
            verifiedQuantity = 75.0
        )
        assertEquals(DeliveryItemVerificationResultType.SHORT, result.resultType)
        assertEquals(DeliveryItemVerificationIssueType.QUANTITY_SHORTAGE, result.issueType)
        assertEquals(25.0, result.issueQuantity, 0.001)
    }

    @Test
    fun `excess quantity is classified as EXCESS with excess quantity`() {
        val result = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 100.0,
            verifiedQuantity = 120.0
        )
        assertEquals(DeliveryItemVerificationResultType.EXCESS, result.resultType)
        assertEquals(DeliveryItemVerificationIssueType.QUANTITY_EXCESS, result.issueType)
        assertEquals(20.0, result.issueQuantity, 0.001)
    }

    @Test
    fun `zero verified quantity is classified as MISSING`() {
        val result = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 50.0,
            verifiedQuantity = 0.0
        )
        assertEquals(DeliveryItemVerificationResultType.MISSING, result.resultType)
        assertEquals(DeliveryItemVerificationIssueType.MISSING, result.issueType)
        assertEquals(50.0, result.issueQuantity, 0.001)
    }
}
