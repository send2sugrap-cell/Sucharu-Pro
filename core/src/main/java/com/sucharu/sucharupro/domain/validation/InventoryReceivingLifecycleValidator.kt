package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLineStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus

/**
 * Domain validator for receiving lifecycle state-machine transitions (Module 07 Step 03).
 *
 * Enforces explicit, legal transitions to prevent arbitrary state jumps and
 * protect terminal states from mutation.
 *
 * All methods are pure and side-effect-free.
 */
object InventoryReceivingLifecycleValidator {

    /**
     * Validates a receiving-level status transition.
     *
     * Legal transitions:
     *   DRAFT      → PENDING, CANCELLED
     *   PENDING    → RECEIVING, CANCELLED
     *   RECEIVING  → PARTIALLY_ACCEPTED, ACCEPTED, PARTIALLY_REJECTED, REJECTED
     *   PARTIALLY_ACCEPTED / ACCEPTED / PARTIALLY_REJECTED / REJECTED → COMPLETED
     *   COMPLETED  → (terminal, no transitions)
     *   CANCELLED  → (terminal, no transitions)
     */
    fun validateReceivingTransition(
        current: InventoryReceivingStatus,
        target: InventoryReceivingStatus
    ): DomainResult<Unit> {
        if (current == target) return DomainResult.Success(Unit)

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Receiving is in terminal state '${current.defaultLabel}' and cannot transition to '${target.defaultLabel}'."
            )
        }

        val allowed = when (current) {
            InventoryReceivingStatus.DRAFT -> setOf(
                InventoryReceivingStatus.PENDING,
                InventoryReceivingStatus.CANCELLED
            )
            InventoryReceivingStatus.PENDING -> setOf(
                InventoryReceivingStatus.RECEIVING,
                InventoryReceivingStatus.CANCELLED
            )
            InventoryReceivingStatus.RECEIVING -> setOf(
                InventoryReceivingStatus.PARTIALLY_ACCEPTED,
                InventoryReceivingStatus.ACCEPTED,
                InventoryReceivingStatus.PARTIALLY_REJECTED,
                InventoryReceivingStatus.REJECTED
            )
            InventoryReceivingStatus.PARTIALLY_ACCEPTED,
            InventoryReceivingStatus.ACCEPTED,
            InventoryReceivingStatus.PARTIALLY_REJECTED,
            InventoryReceivingStatus.REJECTED -> setOf(InventoryReceivingStatus.COMPLETED)
            InventoryReceivingStatus.COMPLETED,
            InventoryReceivingStatus.CANCELLED -> emptySet()
        }

        return if (target in allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid receiving transition: '${current.defaultLabel}' → '${target.defaultLabel}'. " +
                    "Allowed from '${current.defaultLabel}': ${allowed.map { it.defaultLabel }}."
            )
        }
    }

    /**
     * Validates a receiving line status transition.
     *
     * Legal transitions:
     *   PENDING   → VERIFIED, CANCELLED
     *   VERIFIED  → ACCEPTED, PARTIALLY_ACCEPTED, REJECTED, CANCELLED
     *   ACCEPTED / PARTIALLY_ACCEPTED / REJECTED / CANCELLED → (terminal, no transitions)
     */
    fun validateLineTransition(
        current: InventoryReceivingLineStatus,
        target: InventoryReceivingLineStatus
    ): DomainResult<Unit> {
        if (current == target) return DomainResult.Success(Unit)

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Receiving line is in terminal state '${current.defaultLabel}' and cannot transition to '${target.defaultLabel}'."
            )
        }

        val allowed = when (current) {
            InventoryReceivingLineStatus.PENDING -> setOf(
                InventoryReceivingLineStatus.VERIFIED,
                InventoryReceivingLineStatus.CANCELLED
            )
            InventoryReceivingLineStatus.VERIFIED -> setOf(
                InventoryReceivingLineStatus.ACCEPTED,
                InventoryReceivingLineStatus.PARTIALLY_ACCEPTED,
                InventoryReceivingLineStatus.REJECTED,
                InventoryReceivingLineStatus.CANCELLED
            )
            InventoryReceivingLineStatus.ACCEPTED,
            InventoryReceivingLineStatus.PARTIALLY_ACCEPTED,
            InventoryReceivingLineStatus.REJECTED,
            InventoryReceivingLineStatus.CANCELLED -> emptySet()
        }

        return if (target in allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid line transition: '${current.defaultLabel}' → '${target.defaultLabel}'. " +
                    "Allowed from '${current.defaultLabel}': ${allowed.map { it.defaultLabel }}."
            )
        }
    }

    /**
     * Determines the correct receiving status based on finalized line results.
     * Called during receiving completion to derive the aggregate status.
     */
    fun deriveReceivingCompletionStatus(
        totalLines: Int,
        acceptedLines: Int,
        rejectedLines: Int
    ): InventoryReceivingStatus {
        return when {
            acceptedLines == totalLines -> InventoryReceivingStatus.ACCEPTED
            rejectedLines == totalLines -> InventoryReceivingStatus.REJECTED
            acceptedLines > 0 && rejectedLines > 0 -> InventoryReceivingStatus.PARTIALLY_ACCEPTED
            acceptedLines > 0 -> InventoryReceivingStatus.PARTIALLY_ACCEPTED
            else -> InventoryReceivingStatus.REJECTED
        }
    }
}
