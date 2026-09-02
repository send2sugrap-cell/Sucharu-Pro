package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DesignProofDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignRevisionRequest
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.design.RevisionRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DesignArtworkRepository
import com.sucharu.sucharupro.domain.repository.DesignProofRepository
import com.sucharu.sucharupro.domain.validation.DesignProofValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [DesignProofRepository] enforcing proof lifecycle state transitions,
 * revision cycles, immutable version histories, and atomic operations.
 */
class DesignProofRepositoryImpl(
    private val dataSource: DesignProofDataSource,
    private val artworkRepository: DesignArtworkRepository
) : DesignProofRepository {

    private val repositoryMutex = Mutex()

    override fun observeProofs(): Flow<List<DesignProof>> = dataSource.observeProofs()

    override fun getProofById(proofId: String): Flow<DesignProof?> {
        return dataSource.observeProofs().map { proofs ->
            proofs.find { it.proofId == proofId }
        }
    }

    override suspend fun findProofById(proofId: String): DomainResult<DesignProof> {
        return dataSource.fetchProofById(proofId)
    }

    override fun getProofsForArtwork(artworkId: String): Flow<List<DesignProof>> {
        return dataSource.observeProofs().map { proofs ->
            proofs.filter { it.artworkId == artworkId }
        }
    }

    override fun getProofsForProject(projectId: String): Flow<List<DesignProof>> {
        return dataSource.observeProofs().map { proofs ->
            proofs.filter { it.projectId == projectId }
        }
    }

    override suspend fun createProof(
        artworkId: String,
        title: String,
        initialArtworkVersionId: String?,
        initialFile: FileReference?,
        notes: String?,
        createdBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProof> = repositoryMutex.withLock {
        val artwork = when (val res = artworkRepository.findArtworkById(artworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Cannot create proof: Artwork '$artworkId' not found.")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val creationValidation = DesignProofValidator.validateProofCreation(
            artwork = artwork,
            title = title,
            callerRole = callerRole
        )
        if (creationValidation is DomainResult.Error) {
            return creationValidation
        }

        val proofId = "prf-" + UUID.randomUUID().toString()
        var initialVersions = emptyList<DesignProofVersion>()
        var currentVersionNum = 0
        val startingStatus = if (initialFile != null && initialArtworkVersionId != null) ProofStatus.READY_FOR_REVIEW else ProofStatus.DRAFT

        if (initialFile != null && initialArtworkVersionId != null) {
            val versionId = "prf-ver-1-" + UUID.randomUUID().toString()
            val v1 = DesignProofVersion(
                versionId = versionId,
                proofId = proofId,
                versionNumber = 1,
                versionTag = "V1",
                artworkVersionId = initialArtworkVersionId,
                fileReference = initialFile,
                status = ProofStatus.READY_FOR_REVIEW,
                notes = notes ?: "Initial proof generation",
                createdAt = timestamp,
                createdBy = createdBy
            )
            val v1Validation = DesignProofValidator.validateVersionCreation(
                proof = DesignProof(
                    proofId = proofId,
                    artworkId = artwork.artworkId,
                    projectId = artwork.projectId,
                    productionJobId = artwork.productionJobId,
                    title = title,
                    createdAt = timestamp,
                    updatedAt = timestamp
                ),
                versionNumber = 1,
                artworkVersionId = initialArtworkVersionId,
                fileReference = initialFile,
                callerRole = callerRole
            )
            if (v1Validation is DomainResult.Error) {
                return v1Validation
            }
            initialVersions = listOf(v1)
            currentVersionNum = 1
        }

        val proof = DesignProof(
            proofId = proofId,
            artworkId = artwork.artworkId,
            projectId = artwork.projectId,
            productionJobId = artwork.productionJobId,
            title = title.trim(),
            status = startingStatus,
            currentVersionNumber = currentVersionNum,
            versions = initialVersions,
            createdAt = timestamp,
            createdBy = createdBy,
            updatedAt = timestamp,
            updatedBy = createdBy
        )

        val insertResult = dataSource.insertProof(proof)
        if (insertResult is DomainResult.Success && initialVersions.isNotEmpty()) {
            dataSource.insertVersion(initialVersions.first())
        }

        return insertResult
    }

    override suspend fun submitProofForReview(
        proofId: String,
        actorId: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProof> = repositoryMutex.withLock {
        val currentProof = when (val res = dataSource.fetchProofById(proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val rbacResult = DesignProofValidator.validateProofManagementPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        val transitionValidation = DesignProofValidator.validateStatusTransition(currentProof, ProofStatus.READY_FOR_REVIEW)
        if (transitionValidation is DomainResult.Error) {
            return transitionValidation
        }

        val updatedProof = currentProof.copy(
            status = ProofStatus.READY_FOR_REVIEW,
            updatedAt = timestamp,
            updatedBy = actorId
        )

        return dataSource.updateProof(updatedProof)
    }

    override suspend fun requestRevision(
        proofId: String,
        targetVersionNumber: Int,
        reason: RevisionReason,
        notes: String,
        requesterId: String,
        requesterName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignRevisionRequest> = repositoryMutex.withLock {
        val currentProof = when (val res = dataSource.fetchProofById(proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignProofValidator.validateRevisionRequest(
            proof = currentProof,
            targetVersionNumber = targetVersionNumber,
            reason = reason,
            notes = notes,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val targetVersion = currentProof.versions.find { it.versionNumber == targetVersionNumber }
            ?: return DomainResult.Error(message = "Target version V$targetVersionNumber not found.")

        val requestId = "rev-" + UUID.randomUUID().toString()
        val revisionRequest = DesignRevisionRequest(
            requestId = requestId,
            proofId = proofId,
            proofVersionId = targetVersion.versionId,
            targetVersionNumber = targetVersionNumber,
            reason = reason,
            notes = notes.trim(),
            status = RevisionRequestStatus.OPEN,
            requestedBy = requesterId,
            requestedByName = requesterName,
            requestedAt = timestamp
        )

        val updatedProof = currentProof.copy(
            status = ProofStatus.REVISION_REQUESTED,
            revisions = currentProof.revisions + revisionRequest,
            updatedAt = timestamp,
            updatedBy = requesterName ?: requesterId
        )
        dataSource.updateProof(updatedProof)
        dataSource.insertRevision(revisionRequest)

        return DomainResult.Success(revisionRequest)
    }

    override suspend fun startRevision(
        proofId: String,
        actorId: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProof> = repositoryMutex.withLock {
        val currentProof = when (val res = dataSource.fetchProofById(proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignProofValidator.validateStartRevision(currentProof, callerRole)
        if (validation is DomainResult.Error) {
            return validation
        }

        val activeRevision = currentProof.activeRevisionRequest
        val updatedRevisions = if (activeRevision != null && activeRevision.isOpen) {
            val updatedRevision = activeRevision.copy(
                status = RevisionRequestStatus.IN_PROGRESS
            )
            dataSource.updateRevision(updatedRevision)
            currentProof.revisions.map { if (it.requestId == activeRevision.requestId) updatedRevision else it }
        } else {
            currentProof.revisions
        }

        val updatedProof = currentProof.copy(
            status = ProofStatus.REVISING,
            revisions = updatedRevisions,
            updatedAt = timestamp,
            updatedBy = actorId
        )

        return dataSource.updateProof(updatedProof)
    }

    override suspend fun resubmitProof(
        proofId: String,
        artworkVersionId: String,
        fileReference: FileReference,
        notes: String?,
        createdBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProofVersion> = repositoryMutex.withLock {
        val currentProof = when (val res = dataSource.fetchProofById(proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignProofValidator.validateResubmitProof(currentProof, callerRole)
        if (validation is DomainResult.Error) {
            return validation
        }

        val activeRevision = currentProof.activeRevisionRequest
            ?: return DomainResult.Error(message = "Cannot resubmit: No active revision request found to resolve.")

        val nextVersionNumber = if (currentProof.versions.isEmpty()) 1 else (currentProof.versions.maxOf { it.versionNumber } + 1)
        val versionValidation = DesignProofValidator.validateVersionCreation(
            proof = currentProof,
            versionNumber = nextVersionNumber,
            artworkVersionId = artworkVersionId,
            fileReference = fileReference,
            callerRole = callerRole
        )
        if (versionValidation is DomainResult.Error) {
            return versionValidation
        }

        val versionId = "prf-ver-$nextVersionNumber-" + UUID.randomUUID().toString()
        val version = DesignProofVersion(
            versionId = versionId,
            proofId = proofId,
            versionNumber = nextVersionNumber,
            versionTag = "V$nextVersionNumber",
            artworkVersionId = artworkVersionId,
            fileReference = fileReference,
            revisionRequestId = activeRevision.requestId,
            status = ProofStatus.READY_FOR_REVIEW,
            notes = notes ?: "Resolved revision (${activeRevision.reason.defaultLabel})",
            createdAt = timestamp,
            createdBy = createdBy
        )

        dataSource.insertVersion(version)

        // Atomically resolve active revision request
        val resolvedRevision = activeRevision.copy(
            status = RevisionRequestStatus.RESOLVED,
            resolvedBy = createdBy,
            resolvedAt = timestamp,
            resultingProofVersionId = version.versionId,
            resultingVersionNumber = nextVersionNumber
        )
        dataSource.updateRevision(resolvedRevision)

        val updatedVersions = (currentProof.versions.filterNot { it.versionId == version.versionId } + version).sortedBy { it.versionNumber }
        val updatedRevisions = currentProof.revisions.map { if (it.requestId == activeRevision.requestId) resolvedRevision else it }

        val updatedProof = currentProof.copy(
            status = ProofStatus.RESUBMITTED,
            currentVersionNumber = nextVersionNumber,
            versions = updatedVersions,
            revisions = updatedRevisions,
            updatedAt = timestamp,
            updatedBy = createdBy
        )
        dataSource.updateProof(updatedProof)

        return DomainResult.Success(version)
    }

    override suspend fun createProofVersion(
        proofId: String,
        artworkVersionId: String,
        fileReference: FileReference,
        notes: String?,
        createdBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProofVersion> = repositoryMutex.withLock {
        val currentProof = when (val res = dataSource.fetchProofById(proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val nextVersionNumber = if (currentProof.versions.isEmpty()) 1 else (currentProof.versions.maxOf { it.versionNumber } + 1)
        val validation = DesignProofValidator.validateVersionCreation(
            proof = currentProof,
            versionNumber = nextVersionNumber,
            artworkVersionId = artworkVersionId,
            fileReference = fileReference,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val versionId = "prf-ver-$nextVersionNumber-" + UUID.randomUUID().toString()
        val version = DesignProofVersion(
            versionId = versionId,
            proofId = proofId,
            versionNumber = nextVersionNumber,
            versionTag = "V$nextVersionNumber",
            artworkVersionId = artworkVersionId,
            fileReference = fileReference,
            status = ProofStatus.READY_FOR_REVIEW,
            notes = notes,
            createdAt = timestamp,
            createdBy = createdBy
        )

        dataSource.insertVersion(version)

        val updatedVersions = (currentProof.versions.filterNot { it.versionId == version.versionId } + version).sortedBy { it.versionNumber }
        val updatedProof = currentProof.copy(
            currentVersionNumber = nextVersionNumber,
            versions = updatedVersions,
            updatedAt = timestamp,
            updatedBy = createdBy
        )
        dataSource.updateProof(updatedProof)

        return DomainResult.Success(version)
    }

    override fun getProofVersions(proofId: String): Flow<List<DesignProofVersion>> {
        return dataSource.observeVersions().map { versions ->
            versions.filter { it.proofId == proofId }.sortedBy { it.versionNumber }
        }
    }

    override fun getProofVersion(proofId: String, versionNumber: Int): Flow<DesignProofVersion?> {
        return dataSource.observeVersions().map { versions ->
            versions.find { it.proofId == proofId && it.versionNumber == versionNumber }
        }
    }

    override fun getRevisionRequests(proofId: String): Flow<List<DesignRevisionRequest>> {
        return dataSource.observeRevisions().map { revisions ->
            revisions.filter { it.proofId == proofId }.sortedByDescending { it.requestedAt }
        }
    }

    override suspend fun archiveProof(
        proofId: String,
        archivedBy: String?,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProof> = repositoryMutex.withLock {
        val currentProof = when (val res = dataSource.fetchProofById(proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val rbacResult = DesignProofValidator.validateProofManagementPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (currentProof.isArchived) {
            return DomainResult.Error(message = "Proof '${currentProof.title}' is already archived.")
        }

        val updatedProof = currentProof.copy(
            status = ProofStatus.ARCHIVED,
            archivedAt = timestamp,
            archivedBy = archivedBy,
            updatedAt = timestamp,
            updatedBy = archivedBy
        )

        return dataSource.updateProof(updatedProof)
    }
}
