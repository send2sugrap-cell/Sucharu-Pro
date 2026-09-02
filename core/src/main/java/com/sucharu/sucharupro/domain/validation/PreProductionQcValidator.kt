package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative domain validator for Pre-Production Quality Control inspection and version integrity (Module 06 Step 02).
 */
object PreProductionQcValidator {

    val AUTHORIZED_INSPECTION_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR)

    /**
     * Validates RBAC permissions for Pre-Production QC operations.
     */
    fun validateInspectionPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_INSPECTION_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to inspect or submit Pre-Production QC."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the QC aggregate is eligible for Pre-Production evaluation.
     */
    fun validateQcTypeAndStatus(qc: ProductionQc): DomainResult<Unit> {
        if (qc.qcType != QcType.PRE_PRODUCTION) {
            return DomainResult.Error(
                message = "QC record '${qc.qcId}' is not a PRE_PRODUCTION QC (Current Type: ${qc.qcType.defaultLabel})."
            )
        }
        if (qc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot perform operations on terminal QC record '${qc.qcId}'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates checklist items before final submission.
     */
    fun validateItemsCompletion(
        items: List<PreProductionQcItem>,
        targetDecision: QcDecision
    ): DomainResult<Unit> {
        if (items.isEmpty()) {
            return DomainResult.Error(message = "Pre-Production QC items checklist cannot be empty.")
        }

        val pendingRequiredItems = items.filter { it.isRequired && it.status == PreProductionItemStatus.PENDING }
        if (pendingRequiredItems.isNotEmpty()) {
            return DomainResult.Error(
                message = "Cannot submit Pre-Production QC with ${pendingRequiredItems.size} pending required check item(s)."
            )
        }

        val failedRequiredItems = items.filter { it.isRequired && it.status == PreProductionItemStatus.FAIL }

        if (targetDecision == QcDecision.PASS) {
            if (failedRequiredItems.isNotEmpty()) {
                return DomainResult.Error(
                    message = "Cannot PASS Pre-Production QC when ${failedRequiredItems.size} required check item(s) have failed."
                )
            }
        } else if (targetDecision == QcDecision.FAIL) {
            if (failedRequiredItems.isEmpty()) {
                return DomainResult.Error(
                    message = "Cannot FAIL Pre-Production QC without at least one failed required check item."
                )
            }
        } else {
            return DomainResult.Error(message = "Target decision must be PASS or FAIL.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates exact version alignment between design, artwork, proof, and final approval lock.
     */
    fun validateVersionIntegrity(
        project: DesignProject,
        artwork: DesignArtwork,
        inspectedArtworkVersionId: String,
        proof: DesignProof,
        inspectedProofVersionId: String,
        approval: DesignApproval,
        productionJobId: String
    ): DomainResult<Unit> {
        // 1. Cross-job and cross-project validation
        if (project.productionJobId != productionJobId) {
            return DomainResult.Error(
                message = "Design Project '${project.projectId}' is linked to Job '${project.productionJobId}', not '$productionJobId'."
            )
        }
        if (artwork.projectId != project.projectId) {
            return DomainResult.Error(
                message = "Artwork '${artwork.artworkId}' does not belong to Project '${project.projectId}'."
            )
        }
        if (proof.artworkId != artwork.artworkId) {
            return DomainResult.Error(
                message = "Proof '${proof.proofId}' does not belong to Artwork '${artwork.artworkId}'."
            )
        }
        if (approval.proofId != proof.proofId) {
            return DomainResult.Error(
                message = "Approval '${approval.approvalId}' does not belong to Proof '${proof.proofId}'."
            )
        }

        // 2. Lifecycle active state check
        if (project.status == DesignStatus.CANCELLED) {
            return DomainResult.Error(message = "Cannot perform Pre-Production QC on a cancelled Design Project.")
        }
        if (artwork.isArchived) {
            return DomainResult.Error(message = "Artwork '${artwork.artworkId}' is archived.")
        }
        if (proof.isArchived) {
            return DomainResult.Error(message = "Proof '${proof.proofId}' is archived.")
        }

        // 3. Version presence and matching
        val artVersion = artwork.versions.find { it.versionId == inspectedArtworkVersionId }
            ?: return DomainResult.Error(message = "Artwork version '$inspectedArtworkVersionId' not found in Artwork '${artwork.artworkId}'.")

        val proofVersion = proof.versions.find { it.versionId == inspectedProofVersionId }
            ?: return DomainResult.Error(message = "Proof version '$inspectedProofVersionId' not found in Proof '${proof.proofId}'.")

        if (proofVersion.artworkVersionId != artVersion.versionId) {
            return DomainResult.Error(
                message = "Proof version '$inspectedProofVersionId' references Artwork Version '${proofVersion.artworkVersionId}', not '$inspectedArtworkVersionId'."
            )
        }

        // 4. Approval lock and exact final locked version matching
        if (approval.status != ApprovalStatus.FINAL_LOCKED || !approval.isFinalLocked) {
            return DomainResult.Error(
                message = "Approval '${approval.approvalId}' is not FINAL_LOCKED (Current Status: ${approval.status.defaultLabel})."
            )
        }

        if (approval.finalApprovedArtworkVersionId != inspectedArtworkVersionId) {
            return DomainResult.Error(
                message = "Final locked Artwork Version ID '${approval.finalApprovedArtworkVersionId}' does not match inspected Artwork Version '$inspectedArtworkVersionId'."
            )
        }

        if (approval.finalApprovedProofVersionId != inspectedProofVersionId) {
            return DomainResult.Error(
                message = "Final locked Proof Version ID '${approval.finalApprovedProofVersionId}' does not match inspected Proof Version '$inspectedProofVersionId'."
            )
        }

        return DomainResult.Success(Unit)
    }
}
