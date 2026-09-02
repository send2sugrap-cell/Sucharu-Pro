package com.sucharu.sucharupro.domain.service

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationSummary

/**
 * Service for calculating read-side delivery verification reconciliation summaries (Module 08 Step 04).
 */
object DeliveryItemVerificationReconciliationService {

    fun calculateSummary(
        verification: DeliveryItemVerification,
        lines: List<DeliveryItemVerificationLine>
    ): DeliveryItemVerificationSummary {
        var expectedTotal = 0.0
        var verifiedTotal = 0.0
        var shortageTotal = 0.0
        var excessTotal = 0.0
        var damagedTotal = 0.0
        var missingTotal = 0.0
        var mismatchCount = 0
        var verifiedCount = 0

        for (line in lines) {
            expectedTotal += line.expectedQuantity
            verifiedTotal += line.verifiedQuantity

            when (line.resultType) {
                DeliveryItemVerificationResultType.SHORT -> shortageTotal += line.issueQuantity
                DeliveryItemVerificationResultType.EXCESS -> excessTotal += line.issueQuantity
                DeliveryItemVerificationResultType.DAMAGED -> damagedTotal += line.issueQuantity
                DeliveryItemVerificationResultType.MISSING -> missingTotal += line.issueQuantity
                DeliveryItemVerificationResultType.MISMATCH -> mismatchCount++
                DeliveryItemVerificationResultType.VERIFIED -> verifiedCount++
            }
        }

        val hasDiscrepancies = shortageTotal > 0.0 ||
            excessTotal > 0.0 ||
            damagedTotal > 0.0 ||
            missingTotal > 0.0 ||
            mismatchCount > 0

        return DeliveryItemVerificationSummary(
            verificationId = verification.verificationId,
            projectId = verification.projectId,
            expectedTotalQuantity = expectedTotal,
            verifiedTotalQuantity = verifiedTotal,
            shortageTotalQuantity = shortageTotal,
            excessTotalQuantity = excessTotal,
            damagedTotalQuantity = damagedTotal,
            missingTotalQuantity = missingTotal,
            mismatchCount = mismatchCount,
            verifiedLineCount = verifiedCount,
            totalLineCount = lines.size,
            hasDiscrepancies = hasDiscrepancies
        )
    }
}
