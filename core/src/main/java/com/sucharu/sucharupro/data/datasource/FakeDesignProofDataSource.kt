package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignRevisionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [DesignProofDataSource] with [Mutex] atomicity.
 */
class FakeDesignProofDataSource(
    initialProofs: List<DesignProof> = emptyList(),
    initialVersions: List<DesignProofVersion> = emptyList(),
    initialRevisions: List<DesignRevisionRequest> = emptyList()
) : DesignProofDataSource {

    private val mutex = Mutex()
    private val _proofs = MutableStateFlow<List<DesignProof>>(initialProofs)
    private val _versions = MutableStateFlow<List<DesignProofVersion>>(initialVersions)
    private val _revisions = MutableStateFlow<List<DesignRevisionRequest>>(initialRevisions)

    override fun observeProofs(): Flow<List<DesignProof>> = _proofs.asStateFlow()

    override suspend fun fetchProofById(proofId: String): DomainResult<DesignProof> = mutex.withLock {
        val proof = _proofs.value.find { it.proofId == proofId }
        return if (proof != null) {
            DomainResult.Success(proof)
        } else {
            DomainResult.Error(message = "Proof not found with ID: $proofId")
        }
    }

    override suspend fun insertProof(proof: DesignProof): DomainResult<DesignProof> = mutex.withLock {
        if (_proofs.value.any { it.proofId == proof.proofId }) {
            return DomainResult.Error(message = "Proof with ID '${proof.proofId}' already exists.")
        }
        _proofs.value = _proofs.value + proof
        DomainResult.Success(proof)
    }

    override suspend fun updateProof(proof: DesignProof): DomainResult<DesignProof> = mutex.withLock {
        val index = _proofs.value.indexOfFirst { it.proofId == proof.proofId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent Proof: ${proof.proofId}")
        }

        val currentList = _proofs.value.toMutableList()
        currentList[index] = proof
        _proofs.value = currentList.toList()
        DomainResult.Success(proof)
    }

    override fun observeVersions(): Flow<List<DesignProofVersion>> = _versions.asStateFlow()

    override suspend fun insertVersion(version: DesignProofVersion): DomainResult<DesignProofVersion> = mutex.withLock {
        if (_versions.value.any { it.versionId == version.versionId }) {
            return DomainResult.Error(message = "Proof version with ID '${version.versionId}' already exists.")
        }
        if (_versions.value.any { it.proofId == version.proofId && it.versionNumber == version.versionNumber }) {
            return DomainResult.Error(message = "Proof version ${version.versionTag} already exists.")
        }

        _versions.value = _versions.value + version

        // Synchronize parent proof versions list and currentVersionNumber
        val proofIndex = _proofs.value.indexOfFirst { it.proofId == version.proofId }
        if (proofIndex != -1) {
            val parent = _proofs.value[proofIndex]
            val updatedVersions = (parent.versions.filterNot { it.versionId == version.versionId } + version)
                .sortedBy { it.versionNumber }
            val updatedParent = parent.copy(
                versions = updatedVersions,
                currentVersionNumber = version.versionNumber,
                updatedAt = version.createdAt,
                updatedBy = version.createdBy
            )
            val currentProofs = _proofs.value.toMutableList()
            currentProofs[proofIndex] = updatedParent
            _proofs.value = currentProofs.toList()
        }

        DomainResult.Success(version)
    }

    override suspend fun updateVersion(version: DesignProofVersion): DomainResult<DesignProofVersion> = mutex.withLock {
        val index = _versions.value.indexOfFirst { it.versionId == version.versionId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent version: ${version.versionId}")
        }

        val currentVersions = _versions.value.toMutableList()
        currentVersions[index] = version
        _versions.value = currentVersions.toList()

        val proofIndex = _proofs.value.indexOfFirst { it.proofId == version.proofId }
        if (proofIndex != -1) {
            val parent = _proofs.value[proofIndex]
            val updatedVersions = parent.versions.map { if (it.versionId == version.versionId) version else it }
            val currentProofs = _proofs.value.toMutableList()
            currentProofs[proofIndex] = parent.copy(versions = updatedVersions)
            _proofs.value = currentProofs.toList()
        }

        DomainResult.Success(version)
    }

    override fun observeRevisions(): Flow<List<DesignRevisionRequest>> = _revisions.asStateFlow()

    override suspend fun insertRevision(revision: DesignRevisionRequest): DomainResult<DesignRevisionRequest> = mutex.withLock {
        if (_revisions.value.any { it.requestId == revision.requestId }) {
            return DomainResult.Error(message = "Revision request with ID '${revision.requestId}' already exists.")
        }
        _revisions.value = _revisions.value + revision

        // Synchronize parent proof revisions list
        val proofIndex = _proofs.value.indexOfFirst { it.proofId == revision.proofId }
        if (proofIndex != -1) {
            val parent = _proofs.value[proofIndex]
            val updatedRevisions = parent.revisions.filterNot { it.requestId == revision.requestId } + revision
            val currentProofs = _proofs.value.toMutableList()
            currentProofs[proofIndex] = parent.copy(revisions = updatedRevisions)
            _proofs.value = currentProofs.toList()
        }

        DomainResult.Success(revision)
    }

    override suspend fun updateRevision(revision: DesignRevisionRequest): DomainResult<DesignRevisionRequest> = mutex.withLock {
        val index = _revisions.value.indexOfFirst { it.requestId == revision.requestId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent revision request: ${revision.requestId}")
        }

        val currentRevisions = _revisions.value.toMutableList()
        currentRevisions[index] = revision
        _revisions.value = currentRevisions.toList()

        val proofIndex = _proofs.value.indexOfFirst { it.proofId == revision.proofId }
        if (proofIndex != -1) {
            val parent = _proofs.value[proofIndex]
            val updatedRevisions = parent.revisions.map { if (it.requestId == revision.requestId) revision else it }
            val currentProofs = _proofs.value.toMutableList()
            currentProofs[proofIndex] = parent.copy(revisions = updatedRevisions)
            _proofs.value = currentProofs.toList()
        }

        DomainResult.Success(revision)
    }
}
