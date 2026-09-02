package com.sucharu.sucharupro.domain.validation.communication.vendor

import com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Validates lifecycle state transitions for VendorCommunication (Module 10 Step 05).
 *
 * Terminal states: ACKNOWLEDGED, DECLINED, CANCELLED — no further transitions allowed.
 * FAILED is NOT terminal — retry is permitted (FAILED → QUEUED).
 * Illegal backward transitions are strictly rejected.
 */
object VendorCommunicationLifecycleValidator {

    private val allowedTransitions: Map<VendorCommunicationStatus, Set<VendorCommunicationStatus>> = mapOf(
        VendorCommunicationStatus.DRAFT to setOf(
            VendorCommunicationStatus.SCHEDULED,
            VendorCommunicationStatus.QUEUED,
            VendorCommunicationStatus.CANCELLED
        ),
        VendorCommunicationStatus.SCHEDULED to setOf(
            VendorCommunicationStatus.QUEUED,
            VendorCommunicationStatus.CANCELLED
        ),
        VendorCommunicationStatus.QUEUED to setOf(
            VendorCommunicationStatus.SENT,
            VendorCommunicationStatus.FAILED,
            VendorCommunicationStatus.CANCELLED
        ),
        VendorCommunicationStatus.SENT to setOf(
            VendorCommunicationStatus.DELIVERED,
            VendorCommunicationStatus.FAILED
        ),
        VendorCommunicationStatus.DELIVERED to setOf(
            VendorCommunicationStatus.READ
        ),
        VendorCommunicationStatus.READ to setOf(
            VendorCommunicationStatus.ACKNOWLEDGED,
            VendorCommunicationStatus.DECLINED
        ),
        // FAILED is not terminal — retry re-queues the communication
        VendorCommunicationStatus.FAILED to setOf(
            VendorCommunicationStatus.QUEUED
        ),
        // Terminal states — no further transitions
        VendorCommunicationStatus.ACKNOWLEDGED to emptySet(),
        VendorCommunicationStatus.DECLINED to emptySet(),
        VendorCommunicationStatus.CANCELLED to emptySet()
    )

    fun validate(
        from: VendorCommunicationStatus,
        to: VendorCommunicationStatus
    ): DomainResult<Unit> {
        if (from.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition from terminal state '$from'. Communication is finalized."
            )
        }
        val permitted = allowedTransitions[from] ?: emptySet()
        return if (to in permitted) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal lifecycle transition: '$from' → '$to'. " +
                        "Allowed transitions from '$from': ${permitted.joinToString(", ")}."
            )
        }
    }

    fun canTransition(from: VendorCommunicationStatus, to: VendorCommunicationStatus): Boolean {
        if (from.isTerminal) return false
        return to in (allowedTransitions[from] ?: emptySet())
    }

    fun permittedNextStates(from: VendorCommunicationStatus): Set<VendorCommunicationStatus> {
        if (from.isTerminal) return emptySet()
        return allowedTransitions[from] ?: emptySet()
    }
}
