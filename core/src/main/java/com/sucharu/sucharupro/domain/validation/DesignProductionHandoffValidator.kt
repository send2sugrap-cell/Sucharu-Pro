package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative validator for controlled Production Handoff Authorization (Module 05 Step 05).
 */
object DesignProductionHandoffValidator {

    /** Roles authorized to authorize production handoff. */
    val AUTHORIZED_HANDOFF_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)

    /**
     * Validates whether a caller with [callerRole] can authorize production handoff.
     */
    fun validateHandoffPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_HANDOFF_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to authorize production handoff."
            )
        }

        if (callerRole == UserRole.DESIGNER) {
            return DomainResult.Error(
                message = "Designers are not authorized to self-authorize production handoff."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates the 15-point handoff authorization checklist.
     */
    fun validateProductionHandoff(
        project: DesignProject,
        artwork: DesignArtwork,
        artworkVersion: DesignArtworkVersion,
        proof: DesignProof,
        proofVersion: DesignProofVersion,
        approval: DesignApproval,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateHandoffPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        // 1. Project validity
        if (project.status == DesignStatus.CANCELLED) {
            return DomainResult.Error(message = "Cannot hand off cancelled Design Project '${project.projectNumber}'.")
        }

        // 2. Artwork association & non-archived
        if (artwork.projectId != project.projectId) {
            return DomainResult.Error(message = "Cross-project mismatch: Artwork '${artwork.name}' does not belong to Design Project '${project.projectNumber}'.")
        }
        if (artwork.isArchived) {
            return DomainResult.Error(message = "Cannot hand off archived Artwork '${artwork.name}'.")
        }

        // 3. ArtworkVersion association
        if (artworkVersion.artworkId != artwork.artworkId) {
            return DomainResult.Error(message = "Artwork version does not belong to Artwork '${artwork.name}'.")
        }

        // 4. Proof association & non-archived
        if (proof.artworkId != artwork.artworkId || proof.projectId != project.projectId) {
            return DomainResult.Error(message = "Cross-project mismatch: Proof '${proof.title}' does not belong to Project '${project.projectNumber}' or Artwork '${artwork.name}'.")
        }
        if (proof.isArchived) {
            return DomainResult.Error(message = "Cannot hand off archived Proof '${proof.title}'.")
        }

        // 5. ProofVersion association & non-archived
        if (proofVersion.proofId != proof.proofId) {
            return DomainResult.Error(message = "Proof version does not belong to Proof '${proof.title}'.")
        }
        if (proofVersion.isArchived) {
            return DomainResult.Error(message = "Cannot hand off archived Proof Version ${proofVersion.versionTag}.")
        }

        // 6. Traceability between ProofVersion and ArtworkVersion
        if (proofVersion.artworkVersionId != artworkVersion.versionId) {
            return DomainResult.Error(message = "Version mismatch: Proof Version ${proofVersion.versionTag} was not generated from Artwork Version ${artworkVersion.versionTag}.")
        }

        // 7. Approval association
        if (approval.proofId != proof.proofId || approval.artworkId != artwork.artworkId || approval.projectId != project.projectId) {
            return DomainResult.Error(message = "Cross-project mismatch: Approval does not match Project, Artwork, or Proof references.")
        }

        // 8. FINAL_LOCKED status verification
        if (approval.status != ApprovalStatus.FINAL_LOCKED || !approval.isFinalLocked) {
            return DomainResult.Error(
                message = "Production handoff blocked: Approval '${approval.approvalId}' is not Final Locked (Current Status: ${approval.status.defaultLabel})."
            )
        }

        // 9. Exact final version matching
        if (approval.finalApprovedProofVersionId != proofVersion.versionId) {
            return DomainResult.Error(
                message = "Version mismatch: Approval was final locked for proof version '${approval.finalApprovedProofVersionId}', not targeted '${proofVersion.versionId}'."
            )
        }
        if (approval.finalApprovedArtworkVersionId != artworkVersion.versionId) {
            return DomainResult.Error(
                message = "Version mismatch: Approval was final locked for artwork version '${approval.finalApprovedArtworkVersionId}', not targeted '${artworkVersion.versionId}'."
            )
        }

        // 10. No pending revisions blocking handoff
        if (proof.activeRevisionRequest != null) {
            return DomainResult.Error(
                message = "Production handoff blocked: Proof '${proof.title}' has an open revision request."
            )
        }

        return DomainResult.Success(Unit)
    }
}
