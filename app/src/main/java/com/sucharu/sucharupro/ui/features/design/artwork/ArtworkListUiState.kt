package com.sucharu.sucharupro.ui.features.design.artwork

import com.sucharu.sucharupro.domain.model.design.DesignArtwork

/**
 * UI State for Artwork List.
 */
sealed interface ArtworkListUiState {
    data object Loading : ArtworkListUiState

    data class Success(
        val artworks: List<DesignArtwork>,
        val projectId: String? = null,
        val searchQuery: String = "",
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ArtworkListUiState {
        val totalCount: Int get() = artworks.size
    }

    data class Empty(
        val message: String = "No artwork files found for this design project."
    ) : ArtworkListUiState

    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : ArtworkListUiState
}
