package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.returns.ReturnDomainValidator
import com.sucharu.sucharupro.domain.validation.returns.ReturnLifecycleValidator

/**
 * Validator for the physical receiving of an APPROVED Return Request (Module 11 Step 04).
 *
 * All validation follows the same `DomainResult<Unit>` convention used throughout the
 * validation layer. The implementation re‑uses existing validators where applicable to
 * avoid duplication of business logic.
 */
object ReturnReceivingValidator {

    /**
     * Validates that a [ReturnReceivingInfo] can be persisted for the given [ReturnRequest].
     *
     * The validation rules are:
     * 1. The Return must be in `APPROVED` status.
     * 2. `returnId` and `projectId` in the receiving record must match the Return.
     * 3. Customer ownership (if provided) must be respected.
     * 4. All quantity invariants are satisfied:
     *    - `approvedQty` must be non‑negative.
     *    - `actualQty` must be non‑negative and ≤ `approvedQty`.
     *    - `acceptedQty + rejectedQty + damagedQty` must equal `actualQty`.
     * 5. `receiverId` must be non‑blank.
     * 6. `receivedAt` must be a positive timestamp.
     * 7. `idempotencyKey` must be non‑blank.
     * 8. `version` must be strictly positive.
     * 9. The lifecycle transition `APPROVED → RETURN_RECEIVED` must be allowed.
     *
     * If any rule fails, a `DomainResult.Error` with an appropriate message is returned.
     */
    fun validate(
        request: ReturnRequest,
        receivingInfo: ReturnReceivingInfo,
        callerCustomerId: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<Unit> {
        // 1. Return must be APPROVED.
        if (request.status != ReturnStatus.APPROVED) {
            return DomainResult.Error(message = "Return ${request.returnId} is not APPROVED; current status is ${request.status}.")
        }

        // 2. Matching identifiers.
        if (receivingInfo.returnId != request.returnId) {
            return DomainResult.Error(message = "ReturnReceivingInfo.returnId (${receivingInfo.returnId}) does not match Return.id (${request.returnId}).")
        }
        if (receivingInfo.projectId != request.projectId) {
            return DomainResult.Error(message = "ReturnReceivingInfo.projectId (${receivingInfo.projectId}) does not match Return.projectId (${request.projectId}).")
        }

        // 3. Customer ownership – optional guard.
        if (callerCustomerId != null) {
            val ownershipResult = ReturnDomainValidator.validateCustomerOwnership(request, callerCustomerId)
            if (ownershipResult is DomainResult.Error) return ownershipResult
        }

        // 4. Quantity invariants.
        if (receivingInfo.approvedQty < 0) {
            return DomainResult.Error(message = "Approved quantity cannot be negative (got ${receivingInfo.approvedQty}).")
        }
        if (receivingInfo.actualQty < 0) {
            return DomainResult.Error(message = "Actual quantity cannot be negative (got ${receivingInfo.actualQty}).")
        }
        if (receivingInfo.actualQty > receivingInfo.approvedQty) {
            return DomainResult.Error(message = "Actual quantity (${receivingInfo.actualQty}) cannot exceed approved quantity (${receivingInfo.approvedQty}).")
        }
        val sum = receivingInfo.acceptedQty + receivingInfo.rejectedQty + receivingInfo.damagedQty
        if (sum != receivingInfo.actualQty) {
            return DomainResult.Error(
                message = "Quantity breakdown mismatch: accepted (${receivingInfo.acceptedQty}) + rejected (${receivingInfo.rejectedQty}) + damaged (${receivingInfo.damagedQty}) = $sum, but actualQty is ${receivingInfo.actualQty}."
            )
        }

        // 5. Receiver id.
        if (receivingInfo.receiverId.isBlank()) {
            return DomainResult.Error(message = "Receiver ID cannot be blank.")
        }

        // 6. Received timestamp.
        if (receivingInfo.receivedAt <= 0) {
            return DomainResult.Error(message = "Received timestamp must be positive.")
        }

        // 7. Idempotency key.
        if (receivingInfo.idempotencyKey.isBlank()) {
            return DomainResult.Error(message = "Idempotency key cannot be blank.")
        }

        // 8. Version.
        if (receivingInfo.version <= 0) {
            return DomainResult.Error(message = "Version must be strictly positive.")
        }

        // 9. Lifecycle transition validation.
        val transitionResult = ReturnLifecycleValidator.validateTransition(
            current = ReturnStatus.APPROVED,
            target = ReturnStatus.RETURN_RECEIVED
        )
        if (transitionResult is DomainResult.Error) return transitionResult

        // All checks passed.
        return DomainResult.Success(Unit)
    }
}
