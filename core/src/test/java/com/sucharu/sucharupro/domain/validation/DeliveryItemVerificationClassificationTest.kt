package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import com.sucharu.sucharupro.domain.service.DeliveryItemVerificationClassificationService
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryItemVerificationClassificationTest {

    @Test
    fun `damaged goods classification`() {
        val result = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 50.0,
            verifiedQuantity = 50.0,
            isDamaged = true,
            damagedQuantity = 5.0
        )
        assertEquals(DeliveryItemVerificationResultType.DAMAGED, result.resultType)
        assertEquals(DeliveryItemVerificationIssueType.DAMAGED, result.issueType)
        assertEquals(5.0, result.issueQuantity, 0.001)
    }

    @Test
    fun `product mismatch classification`() {
        val result = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 50.0,
            verifiedQuantity = 50.0,
            isProductMismatch = true
        )
        assertEquals(DeliveryItemVerificationResultType.MISMATCH, result.resultType)
        assertEquals(DeliveryItemVerificationIssueType.PRODUCT_MISMATCH, result.issueType)
    }

    @Test
    fun `batch mismatch classification`() {
        val result = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 50.0,
            verifiedQuantity = 50.0,
            isBatchMismatch = true
        )
        assertEquals(DeliveryItemVerificationResultType.MISMATCH, result.resultType)
        assertEquals(DeliveryItemVerificationIssueType.BATCH_MISMATCH, result.issueType)
    }

    @Test
    fun `lot mismatch classification`() {
        val result = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 50.0,
            verifiedQuantity = 50.0,
            isLotMismatch = true
        )
        assertEquals(DeliveryItemVerificationResultType.MISMATCH, result.resultType)
        assertEquals(DeliveryItemVerificationIssueType.LOT_MISMATCH, result.issueType)
    }
}
