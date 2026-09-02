package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignRevisionRequest
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Proof Management and Revision Workflows (Module 05 Step 03).
 */
interface DesignProofRepository {

    /** Reactive stream observing all proofs across all projects. */
    fun observeProofs(): Flow<List<DesignProof>>

    /** Reactive stream observing a single proof by [proofId]. */
    fun getProofById(proofId: String): Flow<DesignProof?>

    /** Direct one-shot lookup of a proof by [proofId]. */
    suspend fun findProofById(proofId: String): DomainResult<DesignProof>

    /** Reactive stream of proofs belonging to an [artworkId]. */
    fun getProofsForArtwork(artworkId: String): Flow<List<DesignProof>>

    /** Reactive stream of proofs associated with a [projectId]. */
    fun getProofsForProject(projectId: String): Flow<List<DesignProof>>

    /**
     * Initializes a new [DesignProof]. If [initialFile] and [initialArtworkVersionId] are provided,
     * atomically creates version V1 and places proof in DRAFT or READY_FOR_REVIEW.
     */
    suspend fun createProof(
        artworkId: String,
        title: String,
        initialArtworkVersionId: String? = null,
        initialFile: FileReference? = null,
        notes: String? = null,
        createdBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProof>

    /**
     * Advances a DRAFT proof into READY_FOR_REVIEW state.
     */
    suspend fun submitProofForReview(
        proofId: String,
        actorId: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProof>

    /**
     * Records a revision request on a specific proof version and transitions proof to REVISION_REQUESTED.
     */
    suspend fun requestRevision(
        proofId: String,
        targetVersionNumber: Int,
        reason: RevisionReason,
        notes: String,
        requesterId: String,
        requesterName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignRevisionRequest>

    /**
     * Transitions proof to REVISING and marks the active revision request as IN_PROGRESS.
     */
    suspend fun startRevision(
        proofId: String,
        actorId: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProof>

    /**
     * Atomically creates the next sequential [DesignProofVersion], resolves the active revision request,
     * and transitions proof status to RESUBMITTED.
     */
    suspend fun resubmitProof(
        proofId: String,
        artworkVersionId: String,
        fileReference: FileReference,
        notes: String? = null,
        createdBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProofVersion>

    /**
     * Directly creates a new proof version.
     */
    suspend fun createProofVersion(
        proofId: String,
        artworkVersionId: String,
        fileReference: FileReference,
        notes: String? = null,
        createdBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProofVersion>

    /** Reactive stream of all historical versions for a [proofId]. */
    fun getProofVersions(proofId: String): Flow<List<DesignProofVersion>>

    /** Reactive stream of a single proof version by [versionNumber]. */
    fun getProofVersion(proofId: String, versionNumber: Int): Flow<DesignProofVersion?>

    /** Reactive stream of all revision requests for a [proofId]. */
    fun getRevisionRequests(proofId: String): Flow<List<DesignRevisionRequest>>

    /**
     * Archives a proof record.
     */
    suspend fun archiveProof(
        proofId: String,
        archivedBy: String? = null,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProof>
}
