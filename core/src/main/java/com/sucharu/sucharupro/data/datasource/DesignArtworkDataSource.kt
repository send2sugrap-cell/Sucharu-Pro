package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Artwork & Version persistence in Sucharu Pro ERP.
 */
interface DesignArtworkDataSource {
    fun observeArtworks(): Flow<List<DesignArtwork>>
    suspend fun fetchArtworkById(artworkId: String): DomainResult<DesignArtwork>
    suspend fun insertArtwork(artwork: DesignArtwork): DomainResult<DesignArtwork>
    suspend fun updateArtwork(artwork: DesignArtwork): DomainResult<DesignArtwork>

    fun observeVersions(): Flow<List<DesignArtworkVersion>>
    suspend fun insertVersion(version: DesignArtworkVersion): DomainResult<DesignArtworkVersion>
    suspend fun updateVersion(version: DesignArtworkVersion): DomainResult<DesignArtworkVersion>
}
