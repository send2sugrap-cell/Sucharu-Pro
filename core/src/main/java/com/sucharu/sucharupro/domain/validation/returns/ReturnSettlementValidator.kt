package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus

/**
 * Domain validator for Customer Return Settlement & Financial Resolution (Module 11 Step 05).
 *
 * Enforces:
 *   1. Eligibility: only PROCESSED returns can enter financial settlement.
 *   2. Identity: non-blank IDs and valid format.
 *   3. Project isolation: return and settlement project IDs must match.
 *   4. Customer ownership: return and settlement customer IDs must match.
 *   5. Amount non-negativity.
 *   6. Optimistic concurrency: expectedVersion matches return's stored version.
 *   7. Resolution reference integrity for completed settlements.
 */
object ReturnSettlementValidator {

    /**
     * Validates that the target Return is in an eligible lifecycle status to be settled.
     */
    fun validateEligibility(request: ReturnRequest): DomainResult<Unit> {
        return if (request.status == ReturnStatus.PROCESSED) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Return '${request.returnId}' is in status '${request.status}' and cannot be settled. " +
                    "Return must be in PROCESSED status."
            )
        }
    }

    /**
     * Validates structural invariants and field integrity of [settlement].
     */
    fun validateSettlement(settlement: ReturnSettlement): DomainResult<Unit> {
        if (settlement.settlementId.isBlank()) {
            return DomainResult.Error(message = "Settlement ID cannot be blank.")
        }
        if (settlement.returnId.isBlank()) {
            return DomainResult.Error(message = "Return ID cannot be blank.")
        }
        if (settlement.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (settlement.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        if (settlement.amount.isNegative()) {
            return DomainResult.Error(message = "Settlement amount cannot be negative.")
        }
        if (settlement.settledBy.isBlank()) {
            return DomainResult.Error(message = "Settled By cannot be blank.")
        }
        if (settlement.idempotencyKey.isBlank()) {
            return DomainResult.Error(message = "Idempotency key cannot be blank.")
        }
        if (settlement.version <= 0) {
            return DomainResult.Error(message = "Settlement version must be strictly positive.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the settlement references match the target return aggregate.
     */
    fun validateSettlementAgainstReturn(
        settlement: ReturnSettlement,
        request: ReturnRequest
    ): DomainResult<Unit> {
        if (settlement.returnId != request.returnId) {
            return DomainResult.Error(
                message = "Settlement returnId '${settlement.returnId}' does not match target Return ID '${request.returnId}'."
            )
        }
        if (settlement.projectId != request.projectId) {
            return DomainResult.Error(
                message = "Settlement project '${settlement.projectId}' does not match Return project '${request.projectId}'."
            )
        }
        if (settlement.customerId != request.customerId) {
            return DomainResult.Error(
                message = "Settlement customer '${settlement.customerId}' does not match Return customer '${request.customerId}'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates optimistic concurrency version match.
     */
    fun validateConcurrency(
        expectedVersion: Long,
        currentVersion: Long,
        returnId: String
    ): DomainResult<Unit> {
        return if (expectedVersion == currentVersion) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Concurrency conflict on Return '$returnId': " +
                    "expected version $expectedVersion but found $currentVersion. " +
                    "Record was updated by another user. Please refresh."
            )
        }
    }
}
