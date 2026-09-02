package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertStatus

/**
 * Validates structural invariants and data integrity of Delivery Governance Alerts.
 */
object DeliveryGovernanceValidator {

    fun validateAlert(alert: DeliveryGovernanceAlert, expectedProjectId: String): DomainResult<Unit> {
        if (alert.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (alert.projectId != expectedProjectId) {
            return DomainResult.Error(message = "Project ID mismatch: Alert has '${alert.projectId}' but context expects '$expectedProjectId'.")
        }
        if (alert.alertId.isBlank()) {
            return DomainResult.Error(message = "Alert ID cannot be blank.")
        }
        if (alert.title.isBlank()) {
            return DomainResult.Error(message = "Alert title cannot be blank.")
        }
        if (alert.description.isBlank()) {
            return DomainResult.Error(message = "Alert description cannot be blank.")
        }
        if (alert.referenceId.isBlank() || alert.referenceType.isBlank()) {
            return DomainResult.Error(message = "Alert reference type and reference ID cannot be blank.")
        }

        if (alert.status == DeliveryGovernanceAlertStatus.ACKNOWLEDGED) {
            if (alert.acknowledgedBy.isNullOrBlank()) {
                return DomainResult.Error(message = "Acknowledged By cannot be blank for ACKNOWLEDGED alert.")
            }
            if (alert.acknowledgedAt == null || alert.acknowledgedAt <= 0) {
                return DomainResult.Error(message = "Acknowledged At timestamp must be positive for ACKNOWLEDGED alert.")
            }
        }

        if (alert.status == DeliveryGovernanceAlertStatus.RESOLVED || alert.status == DeliveryGovernanceAlertStatus.DISMISSED) {
            if (alert.resolvedBy.isNullOrBlank()) {
                return DomainResult.Error(message = "Resolved/Dismissed By cannot be blank for terminal status.")
            }
            if (alert.resolvedAt == null || alert.resolvedAt <= 0) {
                return DomainResult.Error(message = "Resolved/Dismissed At timestamp must be positive for terminal status.")
            }
            if (alert.resolutionNotes.isNullOrBlank()) {
                return DomainResult.Error(message = "Resolution notes/reason cannot be blank for RESOLVED or DISMISSED alert.")
            }
        }

        return DomainResult.Success(Unit)
    }
}
