package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine

/**
 * Validates aggregate structure, dispatch eligibility, and immutability for Delivery Item Verification (Module 08 Step 04).
 */
object DeliveryItemVerificationValidator {

    fun validateVerification(
        verification: DeliveryItemVerification,
        lines: List<DeliveryItemVerificationLine>
    ): DomainResult<Unit> {
        if (verification.verificationId.isBlank()) {
            return DomainResult.Error(message = "Verification ID cannot be blank.")
        }
        if (verification.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (verification.verificationNo.isBlank()) {
            return DomainResult.Error(message = "Verification number cannot be blank.")
        }
        if (verification.deliveryOrderId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        }
        if (verification.deliveryChallanId.isBlank()) {
            return DomainResult.Error(message = "Delivery Challan ID cannot be blank.")
        }
        if (verification.dispatchExecutionId.isBlank()) {
            return DomainResult.Error(message = "Dispatch Execution ID cannot be blank.")
        }
        if (verification.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created by user ID cannot be blank.")
        }
        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Delivery verification must contain at least one line item.")
        }

        for (line in lines) {
            if (line.projectId != verification.projectId) {
                return DomainResult.Error(
                    message = "Project mismatch: Line '${line.verificationLineId}' has project '${line.projectId}' but verification has '${verification.projectId}'."
                )
            }
            if (line.verificationId != verification.verificationId) {
                return DomainResult.Error(
                    message = "Verification ID mismatch: Line '${line.verificationLineId}' references verification '${line.verificationId}' but parent is '${verification.verificationId}'."
                )
            }
            val lineValidation = DeliveryItemVerificationLineValidator.validateLine(line)
            if (lineValidation is DomainResult.Error) return lineValidation
        }

        return DomainResult.Success(Unit)
    }

    fun validateDispatchEligibility(
        dispatch: DispatchExecution,
        targetProjectId: String
    ): DomainResult<Unit> {
        if (dispatch.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Referenced dispatch execution belongs to '${dispatch.projectId}', not '$targetProjectId'."
            )
        }
        if (dispatch.status != DispatchExecutionStatus.DISPATCHED) {
            return DomainResult.Error(
                message = "Dispatch execution '${dispatch.dispatchNo}' is not eligible for delivery verification. Status must be DISPATCHED (current: '${dispatch.status}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateLinesAgainstDispatch(
        dispatchLines: List<DispatchExecutionLine>,
        verificationLines: List<DeliveryItemVerificationLine>
    ): DomainResult<Unit> {
        val dispatchMap = dispatchLines.associateBy { it.dispatchExecutionLineId }

        for (vLine in verificationLines) {
            val dLine = dispatchMap[vLine.dispatchExecutionLineId]
                ?: return DomainResult.Error(
                    message = "Verification line '${vLine.verificationLineId}' references non-existent dispatch line '${vLine.dispatchExecutionLineId}'."
                )

            if (dLine.productId != vLine.productId) {
                return DomainResult.Error(
                    message = "Product mismatch on line '${vLine.verificationLineId}': Dispatch product is '${dLine.productId}', verification specifies '${vLine.productId}'."
                )
            }

            if (dLine.dispatchQuantity != vLine.expectedQuantity) {
                return DomainResult.Error(
                    message = "Expected quantity mismatch on line '${vLine.verificationLineId}': Dispatch qty is ${dLine.dispatchQuantity}, verification expected is ${vLine.expectedQuantity}."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateImmutableIdentity(
        original: DeliveryItemVerification,
        updated: DeliveryItemVerification
    ): DomainResult<Unit> {
        if (original.verificationId != updated.verificationId) {
            return DomainResult.Error(message = "Verification ID is immutable and cannot be changed.")
        }
        if (original.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable and cannot be changed.")
        }
        if (original.verificationNo != updated.verificationNo) {
            return DomainResult.Error(message = "Verification Number is immutable and cannot be changed.")
        }
        if (original.deliveryOrderId != updated.deliveryOrderId) {
            return DomainResult.Error(message = "Delivery Order ID is immutable and cannot be changed.")
        }
        if (original.deliveryChallanId != updated.deliveryChallanId) {
            return DomainResult.Error(message = "Delivery Challan ID is immutable and cannot be changed.")
        }
        if (original.dispatchExecutionId != updated.dispatchExecutionId) {
            return DomainResult.Error(message = "Dispatch Execution ID is immutable and cannot be changed.")
        }
        if (original.createdBy != updated.createdBy) {
            return DomainResult.Error(message = "Creator is immutable and cannot be changed.")
        }
        if (original.createdAt != updated.createdAt) {
            return DomainResult.Error(message = "Created timestamp is immutable and cannot be changed.")
        }
        return DomainResult.Success(Unit)
    }
}
