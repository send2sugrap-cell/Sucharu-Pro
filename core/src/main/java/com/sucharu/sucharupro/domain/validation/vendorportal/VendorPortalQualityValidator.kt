package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

/**
 * Domain Validator for Vendor Portal Quality, CAPA & Dispute Workspace (Module 13 Step 07).
 */
object VendorPortalQualityValidator {

    fun validateQualityCase(case: VendorPortalQualityCase) {
        require(case.caseId.isNotBlank()) { "Quality case ID cannot be blank." }
        require(case.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(case.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(case.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(case.caseNumber.isNotBlank()) { "Case number cannot be blank." }
        require(case.title.isNotBlank()) { "Case title cannot be blank." }
        require(case.description.isNotBlank()) { "Case description cannot be blank." }
    }

    fun validateQualityCaseStatusTransition(
        currentStatus: VendorPortalQualityCaseStatus,
        newStatus: VendorPortalQualityCaseStatus
    ) {
        if (currentStatus == newStatus) return

        val allowed = when (currentStatus) {
            VendorPortalQualityCaseStatus.OPEN -> setOf(
                VendorPortalQualityCaseStatus.ACKNOWLEDGED,
                VendorPortalQualityCaseStatus.RESPONSE_REQUIRED,
                VendorPortalQualityCaseStatus.CLOSED
            )
            VendorPortalQualityCaseStatus.ACKNOWLEDGED -> setOf(
                VendorPortalQualityCaseStatus.RESPONSE_REQUIRED,
                VendorPortalQualityCaseStatus.RESPONSE_SUBMITTED,
                VendorPortalQualityCaseStatus.CAPA_REQUIRED,
                VendorPortalQualityCaseStatus.RESOLVED,
                VendorPortalQualityCaseStatus.CLOSED
            )
            VendorPortalQualityCaseStatus.RESPONSE_REQUIRED -> setOf(
                VendorPortalQualityCaseStatus.RESPONSE_SUBMITTED,
                VendorPortalQualityCaseStatus.CAPA_REQUIRED,
                VendorPortalQualityCaseStatus.CLOSED
            )
            VendorPortalQualityCaseStatus.RESPONSE_SUBMITTED -> setOf(
                VendorPortalQualityCaseStatus.UNDER_REVIEW,
                VendorPortalQualityCaseStatus.CAPA_REQUIRED,
                VendorPortalQualityCaseStatus.RESOLVED,
                VendorPortalQualityCaseStatus.CLOSED
            )
            VendorPortalQualityCaseStatus.CAPA_REQUIRED -> setOf(
                VendorPortalQualityCaseStatus.CAPA_SUBMITTED,
                VendorPortalQualityCaseStatus.CLOSED
            )
            VendorPortalQualityCaseStatus.CAPA_SUBMITTED -> setOf(
                VendorPortalQualityCaseStatus.UNDER_REVIEW,
                VendorPortalQualityCaseStatus.RESOLVED,
                VendorPortalQualityCaseStatus.CLOSED
            )
            VendorPortalQualityCaseStatus.UNDER_REVIEW -> setOf(
                VendorPortalQualityCaseStatus.RESPONSE_REQUIRED,
                VendorPortalQualityCaseStatus.CAPA_REQUIRED,
                VendorPortalQualityCaseStatus.RESOLVED,
                VendorPortalQualityCaseStatus.CLOSED
            )
            VendorPortalQualityCaseStatus.RESOLVED -> setOf(
                VendorPortalQualityCaseStatus.CLOSED,
                VendorPortalQualityCaseStatus.OPEN
            )
            VendorPortalQualityCaseStatus.CLOSED -> emptySet()
        }

        require(allowed.contains(newStatus)) {
            "Illegal quality case transition from $currentStatus to $newStatus."
        }
    }

    fun validateCapaPlan(capa: VendorPortalCapaPlan) {
        require(capa.capaId.isNotBlank()) { "CAPA ID cannot be blank." }
        require(capa.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(capa.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(capa.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(capa.title.isNotBlank()) { "CAPA title cannot be blank." }
        require(capa.rootCause.isNotBlank() && capa.rootCause.length >= 5) {
            "Root cause analysis must be at least 5 characters."
        }
        require(capa.correctiveAction.isNotBlank() && capa.correctiveAction.length >= 5) {
            "Corrective action description must be at least 5 characters."
        }
        require(capa.preventiveAction.isNotBlank() && capa.preventiveAction.length >= 5) {
            "Preventive action description must be at least 5 characters."
        }
        require(capa.responsiblePerson.isNotBlank()) { "Responsible person must be specified." }
        require(capa.targetCompletionDate > 0) { "Target completion date must be a valid timestamp." }
        require(capa.affectedQuantity >= BigDecimal.ZERO) { "Affected quantity cannot be negative." }
    }

    fun validateCapaStatusTransition(
        currentStatus: VendorPortalCapaStatus,
        newStatus: VendorPortalCapaStatus
    ) {
        if (currentStatus == newStatus) return

        val allowed = when (currentStatus) {
            VendorPortalCapaStatus.DRAFT -> setOf(
                VendorPortalCapaStatus.SUBMITTED,
                VendorPortalCapaStatus.CLOSED
            )
            VendorPortalCapaStatus.SUBMITTED -> setOf(
                VendorPortalCapaStatus.UNDER_REVIEW,
                VendorPortalCapaStatus.APPROVED,
                VendorPortalCapaStatus.REJECTED,
                VendorPortalCapaStatus.CLOSED
            )
            VendorPortalCapaStatus.UNDER_REVIEW -> setOf(
                VendorPortalCapaStatus.APPROVED,
                VendorPortalCapaStatus.REJECTED,
                VendorPortalCapaStatus.DRAFT,
                VendorPortalCapaStatus.CLOSED
            )
            VendorPortalCapaStatus.APPROVED -> setOf(
                VendorPortalCapaStatus.IN_PROGRESS,
                VendorPortalCapaStatus.COMPLETED,
                VendorPortalCapaStatus.OVERDUE,
                VendorPortalCapaStatus.CLOSED
            )
            VendorPortalCapaStatus.REJECTED -> setOf(
                VendorPortalCapaStatus.DRAFT,
                VendorPortalCapaStatus.SUBMITTED,
                VendorPortalCapaStatus.CLOSED
            )
            VendorPortalCapaStatus.IN_PROGRESS -> setOf(
                VendorPortalCapaStatus.COMPLETED,
                VendorPortalCapaStatus.OVERDUE,
                VendorPortalCapaStatus.CLOSED
            )
            VendorPortalCapaStatus.COMPLETED -> setOf(
                VendorPortalCapaStatus.CLOSED,
                VendorPortalCapaStatus.IN_PROGRESS
            )
            VendorPortalCapaStatus.OVERDUE -> setOf(
                VendorPortalCapaStatus.IN_PROGRESS,
                VendorPortalCapaStatus.COMPLETED,
                VendorPortalCapaStatus.CLOSED
            )
            VendorPortalCapaStatus.CLOSED -> emptySet()
        }

        require(allowed.contains(newStatus)) {
            "Illegal CAPA plan transition from $currentStatus to $newStatus."
        }
    }

    fun validateDisputeSubmission(dispute: VendorPortalDisputeSummary) {
        require(dispute.disputeId.isNotBlank()) { "Dispute ID cannot be blank." }
        require(dispute.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(dispute.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(dispute.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(dispute.sourceId.isNotBlank()) { "Source reference ID cannot be blank." }
        require(dispute.subject.isNotBlank() && dispute.subject.length >= 3) {
            "Dispute subject must be at least 3 characters."
        }
        require(dispute.description.isNotBlank() && dispute.description.length >= 10) {
            "Dispute description must be at least 10 characters."
        }
        require(dispute.disputedQuantity >= BigDecimal.ZERO) { "Disputed quantity cannot be negative." }
        require(dispute.disputedAmount.amount >= BigDecimal.ZERO) { "Disputed amount cannot be negative." }
    }

    fun validateResolutionResponse(response: VendorPortalResolutionResponse) {
        require(response.responseId.isNotBlank()) { "Response ID cannot be blank." }
        require(response.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(response.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(response.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(response.disputeId.isNotBlank()) { "Dispute ID cannot be blank." }
        require(response.rationale.isNotBlank() && response.rationale.length >= 5) {
            "Rationale for resolution proposal decision must be at least 5 characters."
        }
    }

    fun validateQualityEvidence(evidence: VendorPortalQualityEvidence) {
        require(evidence.evidenceId.isNotBlank()) { "Evidence ID cannot be blank." }
        require(evidence.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(evidence.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(evidence.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(evidence.entityId.isNotBlank()) { "Entity ID cannot be blank." }
        require(evidence.filename.isNotBlank()) { "Filename cannot be blank." }
        require(evidence.fileReference.isNotBlank()) { "File reference URI cannot be blank." }
    }
}
