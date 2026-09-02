package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import com.sucharu.sucharupro.domain.service.DeliveryItemVerificationClassificationService
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryItemVerificationBatchLotTest {

    @Test
    fun `matching batch and lot retains VERIFIED result`() {
        val line = DeliveryItemVerificationLine(
            verificationLineId = "VL-1",
            verificationId = "V-1",
            projectId = "PRJ-1",
            dispatchExecutionLineId = "DL-1",
            challanLineId = "CL-1",
            deliveryOrderLineId = "DOL-1",
            productId = "PROD-1",
            batchId = "BATCH-2026-A",
            lotId = "LOT-2026-B",
            expectedQuantity = 100.0,
            verifiedQuantity = 100.0,
            issueQuantity = 0.0,
            resultType = DeliveryItemVerificationResultType.VERIFIED,
            issueType = DeliveryItemVerificationIssueType.NONE,
            createdAt = 1000L
        )

        val classification = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = line.expectedQuantity,
            verifiedQuantity = line.verifiedQuantity,
            isBatchMismatch = false,
            isLotMismatch = false
        )

        assertEquals(DeliveryItemVerificationResultType.VERIFIED, classification.resultType)
        assertEquals(DeliveryItemVerificationIssueType.NONE, classification.issueType)
    }

    @Test
    fun `mismatched batch is properly classified as BATCH_MISMATCH`() {
        val classification = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = 100.0,
            verifiedQuantity = 100.0,
            isBatchMismatch = true
        )

        assertEquals(DeliveryItemVerificationResultType.MISMATCH, classification.resultType)
        assertEquals(DeliveryItemVerificationIssueType.BATCH_MISMATCH, classification.issueType)
    }
}
