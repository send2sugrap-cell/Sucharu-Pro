package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [DesignArtworkDataSource] with [Mutex] atomicity.
 */
class FakeDesignArtworkDataSource(
    initialArtworks: List<DesignArtwork> = emptyList(),
    initialVersions: List<DesignArtworkVersion> = emptyList()
) : DesignArtworkDataSource {

    private val mutex = Mutex()
    private val _artworks = MutableStateFlow<List<DesignArtwork>>(initialArtworks)
    private val _versions = MutableStateFlow<List<DesignArtworkVersion>>(initialVersions)

    override fun observeArtworks(): Flow<List<DesignArtwork>> = _artworks.asStateFlow()

    override suspend fun fetchArtworkById(artworkId: String): DomainResult<DesignArtwork> = mutex.withLock {
        val artwork = _artworks.value.find { it.artworkId == artworkId }
        return if (artwork != null) {
            DomainResult.Success(artwork)
        } else {
            DomainResult.Error(message = "Artwork not found with ID: $artworkId")
        }
    }

    override suspend fun insertArtwork(artwork: DesignArtwork): DomainResult<DesignArtwork> = mutex.withLock {
        if (_artworks.value.any { it.artworkId == artwork.artworkId }) {
            return DomainResult.Error(message = "Artwork with ID '${artwork.artworkId}' already exists.")
        }
        _artworks.value = _artworks.value + artwork
        DomainResult.Success(artwork)
    }

    override suspend fun updateArtwork(artwork: DesignArtwork): DomainResult<DesignArtwork> = mutex.withLock {
        val index = _artworks.value.indexOfFirst { it.artworkId == artwork.artworkId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent Artwork: ${artwork.artworkId}")
        }

        val currentList = _artworks.value.toMutableList()
        currentList[index] = artwork
        _artworks.value = currentList.toList()
        DomainResult.Success(artwork)
    }

    override fun observeVersions(): Flow<List<DesignArtworkVersion>> = _versions.asStateFlow()

    override suspend fun insertVersion(version: DesignArtworkVersion): DomainResult<DesignArtworkVersion> = mutex.withLock {
        if (_versions.value.any { it.versionId == version.versionId }) {
            return DomainResult.Error(message = "Version with ID '${version.versionId}' already exists.")
        }
        if (_versions.value.any { it.artworkId == version.artworkId && it.versionNumber == version.versionNumber }) {
            return DomainResult.Error(message = "Version ${version.versionTag} already exists for artwork '${version.artworkId}'.")
        }

        _versions.value = _versions.value + version

        // Synchronize parent artwork versions list and currentVersionNumber
        val artworkIndex = _artworks.value.indexOfFirst { it.artworkId == version.artworkId }
        if (artworkIndex != -1) {
            val parent = _artworks.value[artworkIndex]
            val updatedVersions = (parent.versions.filterNot { it.versionId == version.versionId } + version)
                .sortedBy { it.versionNumber }
            val updatedParent = parent.copy(
                versions = updatedVersions,
                currentVersionNumber = version.versionNumber,
                updatedAt = version.createdAt,
                updatedBy = version.createdBy
            )
            val currentArtworks = _artworks.value.toMutableList()
            currentArtworks[artworkIndex] = updatedParent
            _artworks.value = currentArtworks.toList()
        }

        DomainResult.Success(version)
    }

    override suspend fun updateVersion(version: DesignArtworkVersion): DomainResult<DesignArtworkVersion> = mutex.withLock {
        val index = _versions.value.indexOfFirst { it.versionId == version.versionId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent version: ${version.versionId}")
        }

        val currentVersions = _versions.value.toMutableList()
        currentVersions[index] = version
        _versions.value = currentVersions.toList()

        // Synchronize parent artwork
        val artworkIndex = _artworks.value.indexOfFirst { it.artworkId == version.artworkId }
        if (artworkIndex != -1) {
            val parent = _artworks.value[artworkIndex]
            val updatedVersions = parent.versions.map { if (it.versionId == version.versionId) version else it }
            val currentArtworks = _artworks.value.toMutableList()
            currentArtworks[artworkIndex] = parent.copy(versions = updatedVersions)
            _artworks.value = currentArtworks.toList()
        }

        DomainResult.Success(version)
    }
}
