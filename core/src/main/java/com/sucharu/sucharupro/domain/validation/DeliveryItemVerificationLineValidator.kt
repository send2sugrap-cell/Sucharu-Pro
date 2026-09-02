package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType

/**
 * Validates verification line data integrity and result/issue consistency (Module 08 Step 04).
 */
object DeliveryItemVerificationLineValidator {

    fun validateLine(line: DeliveryItemVerificationLine): DomainResult<Unit> {
        if (line.verificationLineId.isBlank()) {
            return DomainResult.Error(message = "Verification line ID cannot be blank.")
        }
        if (line.verificationId.isBlank()) {
            return DomainResult.Error(message = "Verification ID cannot be blank.")
        }
        if (line.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (line.dispatchExecutionLineId.isBlank()) {
            return DomainResult.Error(message = "Dispatch execution line ID cannot be blank.")
        }
        if (line.challanLineId.isBlank()) {
            return DomainResult.Error(message = "Challan line ID cannot be blank.")
        }
        if (line.deliveryOrderLineId.isBlank()) {
            return DomainResult.Error(message = "Delivery order line ID cannot be blank.")
        }
        if (line.productId.isBlank()) {
            return DomainResult.Error(message = "Product ID cannot be blank.")
        }
        if (line.expectedQuantity <= 0.0) {
            return DomainResult.Error(message = "Expected quantity must be strictly positive (> 0). Found: ${line.expectedQuantity}")
        }
        if (line.verifiedQuantity < 0.0) {
            return DomainResult.Error(message = "Verified quantity cannot be negative. Found: ${line.verifiedQuantity}")
        }
        if (line.issueQuantity < 0.0) {
            return DomainResult.Error(message = "Issue quantity cannot be negative. Found: ${line.issueQuantity}")
        }

        // Result and Issue compatibility validation
        val compatibilityCheck = validateResultIssueCompatibility(line.resultType, line.issueType)
        if (compatibilityCheck is DomainResult.Error) return compatibilityCheck

        return DomainResult.Success(Unit)
    }

    fun validateResultIssueCompatibility(
        resultType: DeliveryItemVerificationResultType,
        issueType: DeliveryItemVerificationIssueType
    ): DomainResult<Unit> {
        return when (resultType) {
            DeliveryItemVerificationResultType.VERIFIED -> {
                if (issueType == DeliveryItemVerificationIssueType.NONE) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "A line marked as VERIFIED cannot have an active issue type '$issueType'.")
                }
            }
            DeliveryItemVerificationResultType.SHORT -> {
                if (issueType == DeliveryItemVerificationIssueType.QUANTITY_SHORTAGE || issueType == DeliveryItemVerificationIssueType.OTHER) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "A line marked as SHORT must have issue type QUANTITY_SHORTAGE or OTHER.")
                }
            }
            DeliveryItemVerificationResultType.EXCESS -> {
                if (issueType == DeliveryItemVerificationIssueType.QUANTITY_EXCESS || issueType == DeliveryItemVerificationIssueType.OTHER) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "A line marked as EXCESS must have issue type QUANTITY_EXCESS or OTHER.")
                }
            }
            DeliveryItemVerificationResultType.MISMATCH -> {
                if (issueType in listOf(
                        DeliveryItemVerificationIssueType.PRODUCT_MISMATCH,
                        DeliveryItemVerificationIssueType.BATCH_MISMATCH,
                        DeliveryItemVerificationIssueType.LOT_MISMATCH,
                        DeliveryItemVerificationIssueType.OTHER
                    )) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "A line marked as MISMATCH must have a mismatch issue type.")
                }
            }
            DeliveryItemVerificationResultType.DAMAGED -> {
                if (issueType == DeliveryItemVerificationIssueType.DAMAGED || issueType == DeliveryItemVerificationIssueType.OTHER) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "A line marked as DAMAGED must have issue type DAMAGED or OTHER.")
                }
            }
            DeliveryItemVerificationResultType.MISSING -> {
                if (issueType == DeliveryItemVerificationIssueType.MISSING || issueType == DeliveryItemVerificationIssueType.OTHER) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "A line marked as MISSING must have issue type MISSING or OTHER.")
                }
            }
        }
    }
}
