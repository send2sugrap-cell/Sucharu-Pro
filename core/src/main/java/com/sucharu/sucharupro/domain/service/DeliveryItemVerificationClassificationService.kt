package com.sucharu.sucharupro.domain.service

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType

/**
 * Service for deterministic classification of verification lines (Module 08 Step 04).
 */
object DeliveryItemVerificationClassificationService {

    data class ClassificationResult(
        val resultType: DeliveryItemVerificationResultType,
        val issueType: DeliveryItemVerificationIssueType,
        val issueQuantity: Double
    )

    fun classifyLine(
        expectedQuantity: Double,
        verifiedQuantity: Double,
        isDamaged: Boolean = false,
        damagedQuantity: Double = 0.0,
        isMissing: Boolean = false,
        isProductMismatch: Boolean = false,
        isBatchMismatch: Boolean = false,
        isLotMismatch: Boolean = false
    ): ClassificationResult {
        return when {
            isProductMismatch -> ClassificationResult(
                resultType = DeliveryItemVerificationResultType.MISMATCH,
                issueType = DeliveryItemVerificationIssueType.PRODUCT_MISMATCH,
                issueQuantity = verifiedQuantity
            )
            isBatchMismatch -> ClassificationResult(
                resultType = DeliveryItemVerificationResultType.MISMATCH,
                issueType = DeliveryItemVerificationIssueType.BATCH_MISMATCH,
                issueQuantity = verifiedQuantity
            )
            isLotMismatch -> ClassificationResult(
                resultType = DeliveryItemVerificationResultType.MISMATCH,
                issueType = DeliveryItemVerificationIssueType.LOT_MISMATCH,
                issueQuantity = verifiedQuantity
            )
            isMissing || verifiedQuantity == 0.0 -> ClassificationResult(
                resultType = DeliveryItemVerificationResultType.MISSING,
                issueType = DeliveryItemVerificationIssueType.MISSING,
                issueQuantity = expectedQuantity
            )
            isDamaged && damagedQuantity > 0.0 -> ClassificationResult(
                resultType = DeliveryItemVerificationResultType.DAMAGED,
                issueType = DeliveryItemVerificationIssueType.DAMAGED,
                issueQuantity = damagedQuantity
            )
            verifiedQuantity < expectedQuantity -> ClassificationResult(
                resultType = DeliveryItemVerificationResultType.SHORT,
                issueType = DeliveryItemVerificationIssueType.QUANTITY_SHORTAGE,
                issueQuantity = expectedQuantity - verifiedQuantity
            )
            verifiedQuantity > expectedQuantity -> ClassificationResult(
                resultType = DeliveryItemVerificationResultType.EXCESS,
                issueType = DeliveryItemVerificationIssueType.QUANTITY_EXCESS,
                issueQuantity = verifiedQuantity - expectedQuantity
            )
            else -> ClassificationResult(
                resultType = DeliveryItemVerificationResultType.VERIFIED,
                issueType = DeliveryItemVerificationIssueType.NONE,
                issueQuantity = 0.0
            )
        }
    }
}
