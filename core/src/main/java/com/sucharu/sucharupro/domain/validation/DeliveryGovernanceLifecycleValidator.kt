package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertStatus

/**
 * Validates lifecycle state transitions for Delivery Governance Alerts.
 */
object DeliveryGovernanceLifecycleValidator {

    private val ALLOWED_TRANSITIONS: Map<DeliveryGovernanceAlertStatus, Set<DeliveryGovernanceAlertStatus>> = mapOf(
        DeliveryGovernanceAlertStatus.OPEN to setOf(
            DeliveryGovernanceAlertStatus.ACKNOWLEDGED,
            DeliveryGovernanceAlertStatus.RESOLVED,
            DeliveryGovernanceAlertStatus.DISMISSED
        ),
        DeliveryGovernanceAlertStatus.ACKNOWLEDGED to setOf(
            DeliveryGovernanceAlertStatus.RESOLVED,
            DeliveryGovernanceAlertStatus.DISMISSED
        ),
        DeliveryGovernanceAlertStatus.RESOLVED to emptySet(),
        DeliveryGovernanceAlertStatus.DISMISSED to emptySet()
    )

    fun validateTransition(
        currentStatus: DeliveryGovernanceAlertStatus,
        targetStatus: DeliveryGovernanceAlertStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) {
            return DomainResult.Success(Unit)
        }

        val allowed = ALLOWED_TRANSITIONS[currentStatus] ?: emptySet()
        if (targetStatus !in allowed) {
            return DomainResult.Error(
                message = "Invalid governance alert transition: Cannot move from '${currentStatus.label}' to '${targetStatus.label}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}
