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
 * ViewModel for Artwork list and creation under a Design Project.
 */
class ArtworkListViewModel(
    private val projectId: String,
    private val artworkRepository: DesignArtworkRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))
    private val _uiState = MutableStateFlow<ArtworkListUiState>(ArtworkListUiState.Loading)
    val uiState: StateFlow<ArtworkListUiState> = _uiState.asStateFlow()

    init {
        loadArtworks()
    }

    private fun loadArtworks() {
        viewModelScope.launch {
            combine(
                artworkRepository.getArtworksForProject(projectId),
                _searchQuery,
                _actionState
            ) { artworks, query, action ->
                if (artworks.isEmpty()) {
                    ArtworkListUiState.Empty()
                } else {
                    val filtered = if (query.isBlank()) {
                        artworks
                    } else {
                        artworks.filter {
                            it.name.contains(query, ignoreCase = true) ||
                                    (it.description?.contains(query, ignoreCase = true) == true)
                        }
                    }
                    ArtworkListUiState.Success(
                        artworks = filtered,
                        projectId = projectId,
                        searchQuery = query,
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

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun createArtwork(
        name: String,
        description: String?,
        initialFile: FileReference?,
        metadata: ArtworkMetadata?,
        createdBy: String?,
        timestamp: String
    ) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Creating artwork...", null)
            val result = artworkRepository.createArtwork(
                projectId = projectId,
                name = name,
                description = description,
                initialFile = initialFile,
                initialMetadata = metadata,
                createdBy = createdBy,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Artwork created successfully!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }
}
