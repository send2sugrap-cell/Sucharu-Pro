package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ArtworkMetadata
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Artwork & File Management in Sucharu Pro ERP (Module 05 Step 02).
 */
interface DesignArtworkRepository {

    /** Reactive stream observing all artworks across all design projects. */
    fun observeArtworks(): Flow<List<DesignArtwork>>

    /** Reactive stream observing a single artwork by [artworkId]. */
    fun getArtworkById(artworkId: String): Flow<DesignArtwork?>

    /** Direct one-shot lookup of an artwork by [artworkId]. */
    suspend fun findArtworkById(artworkId: String): DomainResult<DesignArtwork>

    /** Reactive stream of artworks associated with a specific [projectId]. */
    fun getArtworksForProject(projectId: String): Flow<List<DesignArtwork>>

    /**
     * Creates a new [DesignArtwork] aggregate. If [initialFile] is provided, atomically
     * initializes version V1.
     */
    suspend fun createArtwork(
        projectId: String,
        name: String,
        description: String? = null,
        initialFile: FileReference? = null,
        initialMetadata: ArtworkMetadata? = null,
        createdBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignArtwork>

    /**
     * Updates logical metadata (e.g. name, description) on the artwork aggregate.
     */
    suspend fun updateArtworkMetadata(
        artworkId: String,
        name: String,
        description: String? = null,
        updatedBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignArtwork>

    /**
     * Atomically creates a new immutable [DesignArtworkVersion] with deterministic version number sequencing.
     */
    suspend fun createArtworkVersion(
        artworkId: String,
        fileReference: FileReference,
        metadata: ArtworkMetadata = ArtworkMetadata(),
        notes: String? = null,
        createdBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignArtworkVersion>

    /** Reactive stream of all historical and active versions for an [artworkId]. */
    fun getArtworkVersions(artworkId: String): Flow<List<DesignArtworkVersion>>

    /** Reactive stream of a specific version by [artworkId] and [versionNumber]. */
    fun getArtworkVersion(artworkId: String, versionNumber: Int): Flow<DesignArtworkVersion?>

    /**
     * Archives an entire artwork aggregate, preserving all historical version records.
     */
    suspend fun archiveArtwork(
        artworkId: String,
        archivedBy: String? = null,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignArtwork>

    /**
     * Archives a single historical version.
     */
    suspend fun archiveArtworkVersion(
        artworkId: String,
        versionNumber: Int,
        archivedBy: String? = null,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignArtworkVersion>
}
