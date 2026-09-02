package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DesignArtworkDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ArtworkMetadata
import com.sucharu.sucharupro.domain.model.design.ArtworkStatus
import com.sucharu.sucharupro.domain.model.design.DesignActivityType
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DesignArtworkRepository
import com.sucharu.sucharupro.domain.repository.DesignProjectRepository
import com.sucharu.sucharupro.domain.validation.DesignArtworkValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [DesignArtworkRepository] enforcing domain validation,
 * immutable historical versioning, deterministic version numbering, and activity logging.
 */
class DesignArtworkRepositoryImpl(
    private val dataSource: DesignArtworkDataSource,
    private val projectRepository: DesignProjectRepository
) : DesignArtworkRepository {

    private val repositoryMutex = Mutex()

    override fun observeArtworks(): Flow<List<DesignArtwork>> = dataSource.observeArtworks()

    override fun getArtworkById(artworkId: String): Flow<DesignArtwork?> {
        return dataSource.observeArtworks().map { artworks ->
            artworks.find { it.artworkId == artworkId }
        }
    }

    override suspend fun findArtworkById(artworkId: String): DomainResult<DesignArtwork> {
        return dataSource.fetchArtworkById(artworkId)
    }

    override fun getArtworksForProject(projectId: String): Flow<List<DesignArtwork>> {
        return dataSource.observeArtworks().map { artworks ->
            artworks.filter { it.projectId == projectId }
        }
    }

    override suspend fun createArtwork(
        projectId: String,
        name: String,
        description: String?,
        initialFile: FileReference?,
        initialMetadata: ArtworkMetadata?,
        createdBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignArtwork> = repositoryMutex.withLock {
        val project = when (val res = projectRepository.findDesignProjectById(projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Cannot create artwork: Design Project '$projectId' not found.")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val allArtworks = dataSource.observeArtworks().first()
        val creationValidation = DesignArtworkValidator.validateArtworkCreation(
            project = project,
            name = name,
            existingArtworks = allArtworks,
            callerRole = callerRole
        )
        if (creationValidation is DomainResult.Error) {
            return creationValidation
        }

        val artworkId = "art-" + UUID.randomUUID().toString()
        var initialVersions = emptyList<DesignArtworkVersion>()
        var currentVersionNum = 0

        if (initialFile != null) {
            val versionId = "ver-1-" + UUID.randomUUID().toString()
            val v1 = DesignArtworkVersion(
                versionId = versionId,
                artworkId = artworkId,
                versionNumber = 1,
                versionTag = "V1",
                fileReference = initialFile,
                metadata = initialMetadata ?: ArtworkMetadata(),
                status = ArtworkStatus.ACTIVE,
                notes = "Initial artwork upload",
                createdAt = timestamp,
                createdBy = createdBy
            )
            val v1Validation = DesignArtworkValidator.validateVersionCreation(
                artwork = DesignArtwork(
                    artworkId = artworkId,
                    projectId = project.projectId,
                    productionJobId = project.productionJobId,
                    name = name,
                    createdAt = timestamp,
                    updatedAt = timestamp
                ),
                project = project,
                versionNumber = 1,
                fileReference = initialFile,
                metadata = initialMetadata ?: ArtworkMetadata(),
                callerRole = callerRole
            )
            if (v1Validation is DomainResult.Error) {
                return v1Validation
            }
            initialVersions = listOf(v1)
            currentVersionNum = 1
        }

        val artwork = DesignArtwork(
            artworkId = artworkId,
            projectId = project.projectId,
            productionJobId = project.productionJobId,
            name = name.trim(),
            description = description,
            currentVersionNumber = currentVersionNum,
            status = ArtworkStatus.ACTIVE,
            versions = initialVersions,
            createdAt = timestamp,
            createdBy = createdBy,
            updatedAt = timestamp,
            updatedBy = createdBy
        )

        val insertResult = dataSource.insertArtwork(artwork)
        if (insertResult is DomainResult.Success && initialVersions.isNotEmpty()) {
            dataSource.insertVersion(initialVersions.first())
        }

        return insertResult
    }

    override suspend fun updateArtworkMetadata(
        artworkId: String,
        name: String,
        description: String?,
        updatedBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignArtwork> = repositoryMutex.withLock {
        val rbacResult = DesignArtworkValidator.validateArtworkPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        val currentArtwork = when (val res = dataSource.fetchArtworkById(artworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        if (currentArtwork.isArchived) {
            return DomainResult.Error(message = "Cannot update metadata for archived artwork '${currentArtwork.name}'.")
        }

        if (name.isBlank()) {
            return DomainResult.Error(message = "Artwork name cannot be blank.")
        }

        val updatedArtwork = currentArtwork.copy(
            name = name.trim(),
            description = description,
            updatedAt = timestamp,
            updatedBy = updatedBy
        )

        return dataSource.updateArtwork(updatedArtwork)
    }

    override suspend fun createArtworkVersion(
        artworkId: String,
        fileReference: FileReference,
        metadata: ArtworkMetadata,
        notes: String?,
        createdBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignArtworkVersion> = repositoryMutex.withLock {
        val currentArtwork = when (val res = dataSource.fetchArtworkById(artworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val project = when (val res = projectRepository.findDesignProjectById(currentArtwork.projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Design Project '${currentArtwork.projectId}' not found.")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        // Deterministic next version number calculation
        val nextVersionNumber = if (currentArtwork.versions.isEmpty()) 1 else (currentArtwork.versions.maxOf { it.versionNumber } + 1)

        val validation = DesignArtworkValidator.validateVersionCreation(
            artwork = currentArtwork,
            project = project,
            versionNumber = nextVersionNumber,
            fileReference = fileReference,
            metadata = metadata,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val versionId = "ver-$nextVersionNumber-" + UUID.randomUUID().toString()
        val version = DesignArtworkVersion(
            versionId = versionId,
            artworkId = artworkId,
            versionNumber = nextVersionNumber,
            versionTag = "V$nextVersionNumber",
            fileReference = fileReference,
            metadata = metadata,
            status = ArtworkStatus.ACTIVE,
            notes = notes,
            createdAt = timestamp,
            createdBy = createdBy
        )

        return dataSource.insertVersion(version)
    }

    override fun getArtworkVersions(artworkId: String): Flow<List<DesignArtworkVersion>> {
        return dataSource.observeVersions().map { versions ->
            versions.filter { it.artworkId == artworkId }.sortedBy { it.versionNumber }
        }
    }

    override fun getArtworkVersion(artworkId: String, versionNumber: Int): Flow<DesignArtworkVersion?> {
        return dataSource.observeVersions().map { versions ->
            versions.find { it.artworkId == artworkId && it.versionNumber == versionNumber }
        }
    }

    override suspend fun archiveArtwork(
        artworkId: String,
        archivedBy: String?,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignArtwork> = repositoryMutex.withLock {
        val currentArtwork = when (val res = dataSource.fetchArtworkById(artworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignArtworkValidator.validateArtworkArchival(currentArtwork, callerRole)
        if (validation is DomainResult.Error) {
            return validation
        }

        val updatedArtwork = currentArtwork.copy(
            status = ArtworkStatus.ARCHIVED,
            archivedAt = timestamp,
            archivedBy = archivedBy,
            updatedAt = timestamp,
            updatedBy = archivedBy
        )

        return dataSource.updateArtwork(updatedArtwork)
    }

    override suspend fun archiveArtworkVersion(
        artworkId: String,
        versionNumber: Int,
        archivedBy: String?,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignArtworkVersion> = repositoryMutex.withLock {
        val currentArtwork = when (val res = dataSource.fetchArtworkById(artworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val targetVersion = currentArtwork.versions.find { it.versionNumber == versionNumber }
            ?: return DomainResult.Error(message = "Version V$versionNumber not found for artwork '${currentArtwork.name}'.")

        val validation = DesignArtworkValidator.validateVersionArchival(targetVersion, callerRole)
        if (validation is DomainResult.Error) {
            return validation
        }

        val updatedVersion = targetVersion.copy(
            status = ArtworkStatus.ARCHIVED,
            archivedAt = timestamp,
            archivedBy = archivedBy
        )

        return dataSource.updateVersion(updatedVersion)
    }
}
