package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignRevisionRequest
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.design.RevisionRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative validator for Proof Management and Revision Workflows (Module 05 Step 03).
 */
object DesignProofValidator {

    /** Roles authorized to create proofs and upload proof versions. */
    val AUTHORIZED_PROOF_CREATOR_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.DESIGNER)

    /** Roles authorized to request proof revisions. */
    val AUTHORIZED_REVISION_REQUESTER_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)

    /**
     * Validates whether a caller with [callerRole] can manage proofs.
     */
    fun validateProofManagementPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_PROOF_CREATOR_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage proofs."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether a caller with [callerRole] can request a proof revision.
     */
    fun validateRevisionRequestPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_REVISION_REQUESTER_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to request proof revisions."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates proof creation eligibility.
     */
    fun validateProofCreation(
        artwork: DesignArtwork,
        title: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateProofManagementPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (artwork.isArchived) {
            return DomainResult.Error(
                message = "Cannot create proof for an archived artwork '${artwork.name}'."
            )
        }

        if (title.isBlank()) {
            return DomainResult.Error(message = "Proof title cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates proof lifecycle status transitions.
     */
    fun validateStatusTransition(
        proof: DesignProof,
        targetStatus: ProofStatus
    ): DomainResult<Unit> {
        val currentStatus = proof.status

        if (currentStatus == targetStatus) {
            return DomainResult.Error(
                message = "Proof '${proof.title}' is already in ${currentStatus.defaultLabel} state."
            )
        }

        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Archived proof cannot undergo status changes."
            )
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Cannot transition Proof from ${currentStatus.defaultLabel} to ${targetStatus.defaultLabel}."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates proof version creation.
     */
    fun validateVersionCreation(
        proof: DesignProof,
        versionNumber: Int,
        artworkVersionId: String,
        fileReference: FileReference,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateProofManagementPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (proof.isArchived) {
            return DomainResult.Error(
                message = "Cannot create version for an archived proof '${proof.title}'."
            )
        }

        if (versionNumber < 1) {
            return DomainResult.Error(message = "Version number must be at least 1.")
        }

        val duplicate = proof.versions.find { it.versionNumber == versionNumber }
        if (duplicate != null) {
            return DomainResult.Error(
                message = "Proof version V$versionNumber already exists. Proof versions are immutable."
            )
        }

        if (artworkVersionId.isBlank()) {
            return DomainResult.Error(message = "Originating artwork version ID cannot be blank.")
        }

        val fileValidation = DesignArtworkValidator.validateFileReference(fileReference)
        if (fileValidation is DomainResult.Error) {
            return fileValidation
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates submission of a revision request.
     */
    fun validateRevisionRequest(
        proof: DesignProof,
        targetVersionNumber: Int,
        reason: RevisionReason,
        notes: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateRevisionRequestPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (proof.isArchived) {
            return DomainResult.Error(
                message = "Cannot request revision on an archived proof."
            )
        }

        if (proof.status != ProofStatus.READY_FOR_REVIEW && proof.status != ProofStatus.RESUBMITTED) {
            return DomainResult.Error(
                message = "Cannot request revision on Proof in '${proof.status.defaultLabel}' state. Proof must be Ready for Review or Resubmitted."
            )
        }

        val targetVersion = proof.versions.find { it.versionNumber == targetVersionNumber }
        if (targetVersion == null) {
            return DomainResult.Error(
                message = "Target proof version V$targetVersionNumber not found on Proof '${proof.title}'."
            )
        }

        if (notes.isBlank()) {
            return DomainResult.Error(message = "Revision notes/instructions cannot be blank.")
        }

        // Prevent multiple simultaneous open revision requests
        if (proof.activeRevisionRequest != null) {
            return DomainResult.Error(
                message = "An active revision request is already open for Proof '${proof.title}'."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates start of revision work.
     */
    fun validateStartRevision(
        proof: DesignProof,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateProofManagementPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (proof.isArchived) {
            return DomainResult.Error(message = "Cannot start revision on an archived proof.")
        }

        if (proof.status != ProofStatus.REVISION_REQUESTED) {
            return DomainResult.Error(
                message = "Cannot start revision: Proof '${proof.title}' is currently in '${proof.status.defaultLabel}' state (Expected: Revision Requested)."
            )
        }

        if (proof.activeRevisionRequest == null) {
            return DomainResult.Error(
                message = "No active revision request found to start work on."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates resubmission of a proof.
     */
    fun validateResubmitProof(
        proof: DesignProof,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateProofManagementPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (proof.isArchived) {
            return DomainResult.Error(message = "Cannot resubmit an archived proof.")
        }

        if (proof.status != ProofStatus.REVISING) {
            return DomainResult.Error(
                message = "Cannot resubmit Proof: Proof '${proof.title}' is in '${proof.status.defaultLabel}' state (Expected: Revising)."
            )
        }

        if (proof.activeRevisionRequest == null) {
            return DomainResult.Error(
                message = "Cannot resubmit Proof: No active revision request found to resolve."
            )
        }

        return DomainResult.Success(Unit)
    }
}
