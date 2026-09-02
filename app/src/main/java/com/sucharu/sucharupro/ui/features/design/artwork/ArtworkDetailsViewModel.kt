package com.sucharu.sucharupro.ui.features.design.artwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ArtworkMetadata
import com.sucharu.sucharupro.domain.repository.DesignArtworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Artwork Details, Version History, and New Version Upload.
 */
class ArtworkDetailsViewModel(
    private val artworkId: String,
    private val artworkRepository: DesignArtworkRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))
    private val _uiState = MutableStateFlow<ArtworkDetailsUiState>(ArtworkDetailsUiState.Loading)
    val uiState: StateFlow<ArtworkDetailsUiState> = _uiState.asStateFlow()

    init {
        loadArtworkDetails()
    }

    private fun loadArtworkDetails() {
        viewModelScope.launch {
            combine(
                artworkRepository.getArtworkById(artworkId),
                artworkRepository.getArtworkVersions(artworkId),
                _actionState
            ) { artwork, versions, action ->
                if (artwork == null) {
                    ArtworkDetailsUiState.Error("Artwork with ID '$artworkId' not found.")
                } else {
                    ArtworkDetailsUiState.Success(
                        artwork = artwork,
                        versions = versions,
                        isActionInProgress = action.first,
                        actionMessage = action.second,
                        actionError = action.third
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun uploadNewVersion(
        fileReference: FileReference,
        metadata: ArtworkMetadata,
        notes: String?,
        createdBy: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Creating new version...", null)
            val result = artworkRepository.createArtworkVersion(
                artworkId = artworkId,
                fileReference = fileReference,
                metadata = metadata,
                notes = notes,
                createdBy = createdBy,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Version ${result.data.versionTag} created!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun archiveArtwork(archivedBy: String?, reason: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Archiving artwork...", null)
            val result = artworkRepository.archiveArtwork(
                artworkId = artworkId,
                archivedBy = archivedBy,
                reason = reason,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Artwork archived successfully.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }
}
