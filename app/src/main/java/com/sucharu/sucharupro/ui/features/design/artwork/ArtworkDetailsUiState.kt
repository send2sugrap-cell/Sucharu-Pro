package com.sucharu.sucharupro.ui.features.design.artwork

import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion

/**
 * UI State for Artwork Details and Version History.
 */
sealed interface ArtworkDetailsUiState {
    data object Loading : ArtworkDetailsUiState

    data class Success(
        val artwork: DesignArtwork,
        val versions: List<DesignArtworkVersion>,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ArtworkDetailsUiState

    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : ArtworkDetailsUiState
}
