package com.sucharu.sucharupro.ui.features.design.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.repository.DesignProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Design Project Details, Assignment and Status Control.
 */
class DesignProjectDetailsViewModel(
    private val projectId: String,
    private val designRepository: DesignProjectRepository
) : ViewModel() {

    private val _actionState = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))
    private val _uiState = MutableStateFlow<DesignProjectDetailsUiState>(DesignProjectDetailsUiState.Loading)
    val uiState: StateFlow<DesignProjectDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            combine(
                designRepository.getDesignProjectById(projectId),
                designRepository.getAssignmentsForProject(projectId),
                designRepository.getActivityEventsForProject(projectId),
                _actionState
            ) { project, assignments, events, action ->
                if (project == null) {
                    DesignProjectDetailsUiState.Error("Design project with ID '$projectId' not found.")
                } else {
                    DesignProjectDetailsUiState.Success(
                        project = project,
                        assignments = assignments,
                        activityEvents = events,
                        availableDesigners = designRepository.getAvailableDesigners(),
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

    fun assignDesigner(designerId: String, designerName: String, assignedBy: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Assigning designer...", null)
            val result = designRepository.assignDesigner(
                projectId = projectId,
                designerId = designerId,
                designerName = designerName,
                assignedBy = assignedBy,
                notes = notes,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Designer successfully assigned!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun reassignDesigner(newDesignerId: String, newDesignerName: String, reassignedBy: String?, reason: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Reassigning designer...", null)
            val result = designRepository.reassignDesigner(
                projectId = projectId,
                newDesignerId = newDesignerId,
                newDesignerName = newDesignerName,
                reassignedBy = reassignedBy,
                reason = reason,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Designer successfully reassigned!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun unassignDesigner(unassignedBy: String?, reason: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Removing assignment...", null)
            val result = designRepository.unassignDesigner(
                projectId = projectId,
                unassignedBy = unassignedBy,
                reason = reason,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Designer assignment removed!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun startDesign(actorId: String?, actorName: String?, notes: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Starting design work...", null)
            val result = designRepository.startDesign(
                projectId = projectId,
                actorId = actorId,
                actorName = actorName,
                notes = notes,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Design work in progress!", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }

    fun cancelProject(reason: String, cancelledBy: String?, timestamp: String) {
        viewModelScope.launch {
            _actionState.value = Triple(true, "Cancelling project...", null)
            val result = designRepository.cancelDesignProject(
                projectId = projectId,
                reason = reason,
                cancelledBy = cancelledBy,
                timestamp = timestamp
            )
            when (result) {
                is DomainResult.Success -> _actionState.value = Triple(false, "Project cancelled.", null)
                is DomainResult.Error -> _actionState.value = Triple(false, null, result.message)
                is DomainResult.Loading -> {}
            }
        }
    }
}
